package labs.hongducdev.lume.ui.diy.sheets

import android.media.AudioManager
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import labs.hongducdev.lume.domain.diy.Action

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeSettingsSheet(
    initialAction: Action.SetVolume,
    onDismiss: () -> Unit,
    onSave: (Action.SetVolume) -> Unit,
) {
    var volume by remember { mutableFloatStateOf(initialAction.volumePercent.toFloat()) }

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
                text = "Cài đặt âm lượng",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Mức âm lượng đa phương tiện: ${volume.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Slider(
                value = volume,
                onValueChange = { volume = it },
                valueRange = 0f..100f,
                steps = 20,
            )

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
                        onSave(
                            Action.SetVolume(
                                streamType = AudioManager.STREAM_MUSIC,
                                volumePercent = volume.toInt(),
                            ),
                        )
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
