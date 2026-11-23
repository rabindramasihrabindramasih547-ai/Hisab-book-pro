package com.example.ui.screens

import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.GoldBright
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyPrimary
import com.example.util.PinSecurity

@Composable
fun PinLockScreen(
    businessName: String,
    biometricEnabled: Boolean,
    onVerifyPin: (String) -> Boolean,
    onBiometricSuccess: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val biometricSupported = remember { PinSecurity.isBiometricHardwareSupported(context) }

    fun launchBiometric() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt.Builder(context)
                .setTitle(strings.enterPinTitle)
                .setSubtitle(businessName)
                .setNegativeButton(strings.cancel, executor) { _, _ -> }
                .build()
            prompt.authenticate(
                CancellationSignal(),
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        onBiometricSuccess()
                    }
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDeep)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(NavyPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = GoldBright, modifier = Modifier.size(30.dp))
            }

            Text(businessName, style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text(strings.enter4DigitPin, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { idx ->
                    val filled = idx < pin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (filled) GoldBright else Color.Transparent)
                            .border(1.5.dp, if (filled) GoldBright else Color.White.copy(alpha = 0.5f), CircleShape)
                    )
                }
            }

            if (showError) {
                Text(strings.wrongPinError, color = CrimsonBright, style = MaterialTheme.typography.bodySmall)
            }

            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "DEL")
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        row.forEach { key ->
                            if (key.isEmpty()) {
                                Spacer(modifier = Modifier.size(68.dp))
                            } else {
                                Surface(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            showError = false
                                            if (key == "DEL") {
                                                if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                            } else if (pin.length < 4) {
                                                pin += key
                                                if (pin.length == 4) {
                                                    val ok = onVerifyPin(pin)
                                                    if (!ok) {
                                                        showError = true
                                                        pin = ""
                                                    }
                                                }
                                            }
                                        },
                                    shape = CircleShape,
                                    color = NavyPrimary
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (key == "DEL") {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = strings.delete,
                                                tint = Color.White
                                            )
                                        } else {
                                            Text(
                                                key,
                                                style = MaterialTheme.typography.headlineSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (biometricEnabled && biometricSupported) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { launchBiometric() },
                    border = BorderStroke(1.dp, GoldBright)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = GoldBright)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.unlockWithBiometric, color = Color.White)
                }
            }
        }
    }
}
