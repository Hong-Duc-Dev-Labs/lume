package labs.hongducdev.lume.domain.diy

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class Automation(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String = "",
    @SerializedName("type") val type: Type = Type.TRIGGER,
    @SerializedName("trigger") val trigger: Trigger? = null,
    @SerializedName("state") val state: DiyState? = null,
    @SerializedName("actions") val actions: List<Action> = emptyList(),
    @SerializedName("entryActions") val entryActions: List<Action>? = null,
    @SerializedName("exitActions") val exitActions: List<Action>? = null,
    @SerializedName("isEnabled") val isEnabled: Boolean = true,
    @SerializedName("selectedApps") val selectedApps: List<String> = emptyList(),
) {
    val actionList: List<Action> get() = actions

    val entryActionList: List<Action> get() = entryActions ?: actions

    val exitActionList: List<Action> get() = exitActions ?: emptyList()

    @Keep
    enum class Type {
        @SerializedName("TRIGGER")
        TRIGGER,

        @SerializedName("STATE")
        STATE,

        @SerializedName("APP")
        APP,

        @SerializedName("ACTION_SHORTCUT")
        ACTION_SHORTCUT,
    }
}
