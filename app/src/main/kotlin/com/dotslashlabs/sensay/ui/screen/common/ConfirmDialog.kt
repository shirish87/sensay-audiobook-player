package com.dotslashlabs.sensay.ui.screen.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember


@Composable
fun <T> ConfirmDialog(
    data: MutableState<T?> = remember { mutableStateOf(null) },
    title: (data: T?) -> String,
    message: (data: T?) -> String,
    confirmLabel: String = "Confirm",
    cancelLabel: String = "Dismiss",
    next: (t: T?) -> Unit,
) {

    if (data.value == null) return

    AlertDialog(
        title = { Text(title(data.value)) },
        text = { Text(message(data.value)) },
        confirmButton = {
            TextButton(
                onClick = {
                    next(data.value)
                    data.value = null
                },
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    data.value = null
                },
            ) {
                Text(cancelLabel)
            }
        },
        onDismissRequest = {
            // Dismiss the dialog when the user clicks outside the dialog or on the back
            // button. If you want to disable that functionality, simply use an empty
            // onDismissRequest.
            data.value = null
        },
    )
}
