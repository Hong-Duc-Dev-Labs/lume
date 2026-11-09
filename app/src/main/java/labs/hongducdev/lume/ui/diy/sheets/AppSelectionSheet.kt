package labs.hongducdev.lume.ui.diy.sheets

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(
    val appName: String,
    val packageName: String,
    val iconBitmap: android.graphics.Bitmap?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionSheet(
    initialSelected: List<String> = emptyList(),
    multiSelect: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmed: (List<InstalledApp>) -> Unit,
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPackages by remember { mutableStateOf(initialSelected.toSet()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val installed =
                pm
                    .getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || pm.getLaunchIntentForPackage(it.packageName) != null }
                    .map { appInfo ->
                        val name = pm.getApplicationLabel(appInfo).toString()
                        val icon =
                            try {
                                pm.getApplicationIcon(appInfo).toBitmap(96, 96)
                            } catch (_: Exception) {
                                null
                            }
                        InstalledApp(name, appInfo.packageName, icon)
                    }.sortedBy { it.appName.lowercase() }
            withContext(Dispatchers.Main) {
                apps = installed
            }
        }
    }

    val filteredApps =
        remember(apps, searchQuery) {
            if (searchQuery.isBlank()) {
                apps
            } else {
                apps.filter {
                    it.appName.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
                }
            }
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (multiSelect) "Chọn ứng dụng kích hoạt" else "Chọn ứng dụng cần mở",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Tìm kiếm ứng dụng...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(380.dp),
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (multiSelect) {
                                        selectedPackages =
                                            if (selectedPackages.contains(app.packageName)) {
                                                selectedPackages - app.packageName
                                            } else {
                                                selectedPackages + app.packageName
                                            }
                                    } else {
                                        onConfirmed(listOf(app))
                                        onDismiss()
                                    }
                                }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (multiSelect) {
                            Checkbox(
                                checked = selectedPackages.contains(app.packageName),
                                onCheckedChange = { isChecked ->
                                    selectedPackages =
                                        if (isChecked) {
                                            selectedPackages + app.packageName
                                        } else {
                                            selectedPackages - app.packageName
                                        }
                                },
                            )
                        }

                        app.iconBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                            )
                        } ?: Spacer(modifier = Modifier.size(40.dp))

                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (multiSelect) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Hủy") }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val selectedApps = apps.filter { selectedPackages.contains(it.packageName) }
                            onConfirmed(selectedApps)
                            onDismiss()
                        },
                    ) {
                        Text("Xác nhận (${selectedPackages.size})")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
