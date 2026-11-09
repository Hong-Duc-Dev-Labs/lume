package com.hongducdev.toolkit.ui.diy.sheets

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
import androidx.compose.material3.OutlinedTextField
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
import com.hongducdev.toolkit.domain.diy.Action

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendNotificationSettingsSheet(
    initialAction: Action.SendNotification,
    onDismiss: () -> Unit,
    onSave: (Action.SendNotification) -> Unit,
) {
    var title by remember { mutableStateOf(initialAction.notifTitle) }
    var message by remember { mutableStateOf(initialAction.notifMessage) }

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
                text = "Gửi thông báo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tiêu đề thông báo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Nội dung thông báo") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
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
                        onSave(Action.SendNotification(notifTitle = title, notifMessage = message))
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
