package labs.hongducdev.lume.ui.diy.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import labs.hongducdev.lume.domain.diy.Action
import labs.hongducdev.lume.ui.core.containers.RoundedCardContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundModeSettingsSheet(
    initialAction: Action.SoundMode,
    onDismiss: () -> Unit,
    onSave: (Action.SoundMode) -> Unit,
) {
    var selectedMode by remember { mutableStateOf(initialAction.mode) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Chọn chế độ âm thanh",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            RoundedCardContainer {
                val modes =
                    listOf(
                        Action.SoundModeType.SOUND to "Chuông (Sound)",
                        Action.SoundModeType.VIBRATE to "Chỉ rung (Vibrate)",
                        Action.SoundModeType.SILENT to "Im lặng (Silent)",
                    )
                modes.forEach { (mode, label) ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { selectedMode = mode }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Hủy")
                }
                Button(
                    onClick = {
                        onSave(Action.SoundMode(mode = selectedMode))
                        onDismiss()
                    },
                ) {
                    Text("Lưu")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
