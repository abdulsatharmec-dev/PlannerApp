package com.dailycurator.ui.security

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.security.AppPinHasher

@Composable
fun AppLockOverlay(
    visible: Boolean,
    activity: FragmentActivity,
    prefs: AppPreferences,
    onUnlocked: () -> Unit,
) {
    if (!visible) return

    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }

    fun tryUnlock(entered: String) {
        val salt = prefs.getAppLockPinSalt()
        val hash = prefs.getAppLockPinHash()
        if (salt.isBlank() || hash.isBlank()) {
            onUnlocked()
            return
        }
        if (AppPinHasher.sha256Hex(salt, entered) == hash) {
            pin = ""
            onUnlocked()
        } else {
            Toast.makeText(context, "Incorrect PIN", Toast.LENGTH_SHORT).show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("App locked", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) pin = it },
                label = { Text("PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { tryUnlock(pin) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Unlock") }
            if (prefs.isAppLockBiometricAllowed()) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val mgr = BiometricManager.from(activity)
                        val can = mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
                            Toast.makeText(
                                context,
                                "Biometric unlock not available on this device.",
                                Toast.LENGTH_LONG,
                            ).show()
                            return@Button
                        }
                        val executor = ContextCompat.getMainExecutor(activity)
                        val prompt = BiometricPrompt(
                            activity,
                            executor,
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    onUnlocked()
                                }
                            },
                        )
                        val info = BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Unlock DayRoute")
                            .setSubtitle("Confirm it’s you")
                            .setNegativeButtonText("Cancel")
                            .build()
                        prompt.authenticate(info)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Use fingerprint or screen lock") }
            }
        }
    }
}
