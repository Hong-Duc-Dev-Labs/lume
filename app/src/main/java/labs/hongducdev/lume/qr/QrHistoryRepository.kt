package labs.hongducdev.lume.qr

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.qrDataStore by preferencesDataStore(name = "qr_scanner_settings")
private val KEY_HISTORY = stringPreferencesKey("qr_history_json")

data class QrHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val rawText: String,
    val typeName: String,
    val title: String,
    val subtitle: String,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val type: QrType
        get() =
            try {
                QrType.valueOf(typeName)
            } catch (_: Exception) {
                QrType.TEXT
            }

    companion object {
        fun fromResult(result: QrResult): QrHistoryItem {
            val title =
                when (val data = result.data) {
                    is QrParsedData.Url -> data.host.ifBlank { data.url }
                    is QrParsedData.Wifi -> "Wi-Fi: ${data.ssid}"
                    is QrParsedData.Phone -> "Gọi: ${data.number}"
                    is QrParsedData.Sms -> "SMS: ${data.number}"
                    is QrParsedData.Email -> "Email: ${data.email}"
                    is QrParsedData.Geo -> "Tọa độ: ${data.latitude}, ${data.longitude}"
                    is QrParsedData.Text -> data.content.take(40)
                }
            val subtitle =
                when (val data = result.data) {
                    is QrParsedData.Url -> data.url
                    is QrParsedData.Wifi -> if (data.password.isNotBlank()) "Mật khẩu: ${data.password}" else "Mạng mở (Open)"
                    is QrParsedData.Phone -> data.number
                    is QrParsedData.Sms -> data.message.ifBlank { data.number }
                    is QrParsedData.Email -> data.subject.ifBlank { data.email }
                    is QrParsedData.Geo -> data.query ?: "${data.latitude}, ${data.longitude}"
                    is QrParsedData.Text -> data.content
                }
            return QrHistoryItem(
                rawText = result.rawText,
                typeName = result.type.name,
                title = title,
                subtitle = subtitle,
                timestamp = result.timestamp,
            )
        }
    }
}

class QrHistoryRepository(
    private val context: Context,
    private val gson: Gson = Gson(),
) {
    val history: Flow<List<QrHistoryItem>> =
        context.qrDataStore.data.map { prefs ->
            val json = prefs[KEY_HISTORY]
            if (json.isNullOrBlank()) {
                emptyList()
            } else {
                try {
                    val type = object : TypeToken<List<QrHistoryItem>>() {}.type
                    gson.fromJson<List<QrHistoryItem>>(json, type) ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }

    suspend fun add(
        result: QrResult,
        maxItems: Int = 50,
    ) {
        val newItem = QrHistoryItem.fromResult(result)
        context.qrDataStore.edit { prefs ->
            val json = prefs[KEY_HISTORY]
            val currentList: MutableList<QrHistoryItem> =
                if (json.isNullOrBlank()) {
                    mutableListOf()
                } else {
                    try {
                        val type = object : TypeToken<List<QrHistoryItem>>() {}.type
                        gson.fromJson<List<QrHistoryItem>>(json, type)?.toMutableList() ?: mutableListOf()
                    } catch (_: Exception) {
                        mutableListOf()
                    }
                }

            // Remove existing duplicate with the exact same rawText so the latest scan jumps to the top
            currentList.removeAll { it.rawText == newItem.rawText }
            currentList.add(0, newItem)

            if (currentList.size > maxItems) {
                val trimmed = currentList.take(maxItems)
                prefs[KEY_HISTORY] = gson.toJson(trimmed)
            } else {
                prefs[KEY_HISTORY] = gson.toJson(currentList)
            }
        }
    }

    suspend fun delete(id: String) {
        context.qrDataStore.edit { prefs ->
            val json = prefs[KEY_HISTORY] ?: return@edit
            try {
                val type = object : TypeToken<List<QrHistoryItem>>() {}.type
                val currentList: MutableList<QrHistoryItem> = gson.fromJson(json, type) ?: return@edit
                currentList.removeAll { it.id == id }
                prefs[KEY_HISTORY] = gson.toJson(currentList)
            } catch (_: Exception) {
            }
        }
    }

    suspend fun clear() {
        context.qrDataStore.edit { prefs ->
            prefs.remove(KEY_HISTORY)
        }
    }
}
