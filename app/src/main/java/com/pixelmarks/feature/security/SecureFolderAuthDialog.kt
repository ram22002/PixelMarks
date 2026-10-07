package com.pixelmarks.feature.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.security.CustomPasswordManager
import com.pixelmarks.core.security.PasswordValidationResult
import com.pixelmarks.core.security.model.PrivateAuthMethod
import com.pixelmarks.ui.theme.Spacing
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Dialog prompting for Custom Secure Folder Password with support for visibility toggle,
 * hint recovery, and fallback.
 */
@Composable
fun SecureFolderPasswordDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onFallbackAuth: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as PixelMarksApplication
    val coroutineScope = rememberCoroutineScope()

    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var failedAttempts by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var lockoutSecondsRemaining by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    var showHintDialog by remember { mutableStateOf(false) }
    var hintText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lockoutSecondsRemaining) {
        if (lockoutSecondsRemaining > 0) {
            kotlinx.coroutines.delay(1000L)
            lockoutSecondsRemaining -= 1
        }
    }

    fun submit() {
        if (lockoutSecondsRemaining > 0) {
            errorMessage = "Too many failed attempts. Please wait $lockoutSecondsRemaining seconds."
            return
        }
        if (password.isEmpty()) {
            errorMessage = "Please enter your password"
            return
        }
        isVerifying = true
        errorMessage = null

        coroutineScope.launch {
            val charArray = password.toCharArray()
            val valid = app.container.privatePreferencesRepository.verifyCustomPassword(charArray)
            charArray.fill('0')
            isVerifying = false

            if (valid) {
                failedAttempts = 0
                onSuccess()
            } else {
                failedAttempts += 1
                if (failedAttempts >= 5) {
                    lockoutSecondsRemaining = 30
                    errorMessage = "Too many failed attempts. Please wait 30 seconds."
                } else {
                    errorMessage = "Incorrect password"
                }
            }
        }
    }

    if (showHintDialog) {
        AlertDialog(
            onDismissRequest = { showHintDialog = false },
            title = {
                Text("Password Hint", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    if (!hintText.isNullOrBlank()) {
                        Text("Your hint is:")
                        Text(
                            text = "\"$hintText\"",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text("No password hint was configured when setting up this password.")
                    }

                    if (onFallbackAuth != null) {
                        Spacer(modifier = Modifier.height(Spacing.small))
                        Text(
                            text = "You can also verify identity with your phone's biometric or screen lock.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showHintDialog = false
                        onFallbackAuth?.invoke()
                    }
                ) {
                    Text(if (onFallbackAuth != null) "Use Device Unlock" else "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHintDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Secure Folder",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Text(
                    text = "Enter your Secure Folder password to enter Private Mode.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    isError = errorMessage != null
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                hintText = app.container.privatePreferencesRepository.getPasswordHint()
                                showHintDialog = true
                            }
                        }
                    ) {
                        Text("Forgot Password?")
                    }

                    if (onFallbackAuth != null) {
                        TextButton(onClick = onFallbackAuth) {
                            Text("Biometric / PIN")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { submit() },
                enabled = !isVerifying
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for setting or changing custom password for Secure Folder.
 */
@Composable
fun SetupSecureFolderPasswordDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as PixelMarksApplication
    val coroutineScope = rememberCoroutineScope()

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    fun submit() {
        val validation = CustomPasswordManager.validatePassword(newPassword)
        if (validation is PasswordValidationResult.Invalid) {
            errorMessage = validation.message
            return
        }

        if (newPassword != confirmPassword) {
            errorMessage = "Passwords do not match."
            return
        }

        if (hint.isNotBlank() && !CustomPasswordManager.isHintAcceptable(newPassword.toCharArray(), hint)) {
            errorMessage = "Password hint cannot be identical to your password."
            return
        }

        isSaving = true
        errorMessage = null

        coroutineScope.launch {
            val charArray = newPassword.toCharArray()
            app.container.privatePreferencesRepository.setCustomPassword(
                password = charArray,
                hint = hint.takeIf { it.isNotBlank() }
            )
            charArray.fill('0')
            isSaving = false
            onSuccess()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Set Secure Folder Password", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Text(
                    text = "Create a custom password for your Secure Folder (minimum 8 characters).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        errorMessage = null
                    },
                    label = { Text("Password (min 8 chars)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    }
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                    },
                    label = { Text("Confirm Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Password Hint (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() })
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { submit() },
                enabled = !isSaving
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
