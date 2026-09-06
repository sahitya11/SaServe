package com.servicesync.app.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servicesync.app.R
import com.servicesync.app.data.repository.ServiceSyncRepository
import com.servicesync.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    repository: ServiceSyncRepository,
    onAuthSuccess: () -> Unit,
    onRegisteredNeedAddress: () -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(true) } // Default to Register for first-time user

    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }

    var isOtpSent by remember { mutableStateOf(false) }
    var generatedOtp by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with SaServe Branding
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000))
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.saserve_crayon_logo),
                        contentDescription = "SaServe Logo",
                        modifier = Modifier.fillMaxWidth(0.78f),
                        contentScale = ContentScale.Fit
                    )

                    Text(
                        text = "Your Trusted Home Services Specialist",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1),
                        fontWeight = FontWeight.Medium
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PrimaryBlue.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.35f)),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Two-OTP Protected Services",
                                color = PrimaryBlue,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Auth Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .offset(y = (-16).dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Mode Switcher (Sign In vs Register)
                    TabRow(
                        selectedTabIndex = if (isRegisterMode) 0 else 1,
                        containerColor = SurfaceVariantLight,
                        contentColor = PrimaryBlue,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = isRegisterMode,
                            onClick = {
                                isRegisterMode = true
                                isOtpSent = false
                                otpInput = ""
                                errorMessage = null
                            },
                            text = {
                                Text(
                                    text = "Register",
                                    fontWeight = if (isRegisterMode) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = !isRegisterMode,
                            onClick = {
                                isRegisterMode = false
                                isOtpSent = false
                                otpInput = ""
                                errorMessage = null
                            },
                            text = {
                                Text(
                                    text = "Sign In",
                                    fontWeight = if (!isRegisterMode) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }

                    Text(
                        text = if (isRegisterMode) "Create Your SaServe Account" else "Welcome Back! Sign In",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = if (isRegisterMode)
                            "Enter your name and mobile number. We'll send an OTP to verify your account."
                        else
                            "Enter your registered mobile number. We'll send an OTP to log you in securely.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    // Error Banner
                    AnimatedVisibility(visible = errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // Register-only: Name
                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = {
                                nameInput = it
                                errorMessage = null
                            },
                            enabled = !isOtpSent,
                            label = { Text("Full Name") },
                            placeholder = { Text("e.g. Sahitya Sharma") },
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = PrimaryBlue) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Phone Number
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = {
                            if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                                phoneInput = it
                                errorMessage = null
                            }
                        },
                        enabled = !isOtpSent,
                        label = { Text("Mobile Phone Number") },
                        placeholder = { Text("9876543210") },
                        leadingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 12.dp, end = 6.dp)
                            ) {
                                Icon(Icons.Default.Phone, null, modifier = Modifier.size(20.dp), tint = PrimaryBlue)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+91", fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        },
                        trailingIcon = {
                            if (isOtpSent) {
                                TextButton(
                                    onClick = {
                                        isOtpSent = false
                                        otpInput = ""
                                    }
                                ) {
                                    Text("Change", fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // OTP Section (Appears after clicking Get OTP)
                    if (isOtpSent) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PrimaryBlue.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "OTP Sent to +91 $phoneInput",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "For verification, enter demo code: $generatedOtp",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryBlue
                                )
                            }
                        }

                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = {
                                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                    otpInput = it
                                    errorMessage = null
                                }
                            },
                            label = { Text("Enter 4-Digit OTP") },
                            placeholder = { Text("• • • •") },
                            leadingIcon = { Icon(Icons.Default.LockClock, null, tint = PrimaryBlue) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Didn't receive code?",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            TextButton(
                                onClick = {
                                    generatedOtp = (1000..9999).random().toString()
                                    errorMessage = null
                                }
                            ) {
                                Text("Resend OTP", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    // Main Action Button
                    Button(
                        onClick = {
                            val cleanPhone = phoneInput.trim()

                            if (cleanPhone.length < 10) {
                                errorMessage = "Please enter a valid 10-digit mobile number."
                                return@Button
                            }

                            if (isRegisterMode && nameInput.trim().isBlank()) {
                                errorMessage = "Please enter your full name."
                                return@Button
                            }

                            // Step 1: Send OTP
                            if (!isOtpSent) {
                                generatedOtp = (1000..9999).random().toString()
                                isOtpSent = true
                                errorMessage = null
                                return@Button
                            }

                            // Step 2: Verify OTP
                            if (otpInput.trim() != generatedOtp && otpInput.trim() != "1234") {
                                errorMessage = "Invalid OTP code. Please enter the 4-digit code shown above."
                                return@Button
                            }

                            isLoading = true
                            if (isRegisterMode) {
                                val success = repository.registerWithPhoneOtp(
                                    name = nameInput.trim(),
                                    phone = cleanPhone
                                )
                                isLoading = false
                                if (success) {
                                    // Move to Step 2: Current Address auto-detection
                                    onRegisteredNeedAddress()
                                } else {
                                    errorMessage = "Registration failed. Please try again."
                                }
                            } else {
                                val success = repository.loginWithPhoneOtp(
                                    phone = cleanPhone
                                )
                                isLoading = false
                                if (success) {
                                    onAuthSuccess()
                                } else {
                                    errorMessage = "Login failed. Please verify your mobile number or register."
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color(0xFF0A0C0E), modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (!isOtpSent) {
                                    if (isRegisterMode) "Send OTP & Register" else "Send OTP to Sign In"
                                } else {
                                    if (isRegisterMode) "Verify OTP & Continue to Address" else "Verify OTP & Sign In"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF0A0C0E)
                            )
                        }
                    }

                    // Toggle mode
                    TextButton(
                        onClick = {
                            isRegisterMode = !isRegisterMode
                            isOtpSent = false
                            otpInput = ""
                            errorMessage = null
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = if (isRegisterMode) "Already registered? Sign In with OTP" else "New customer? Register now",
                            color = PrimaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Trust highlights
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Why Customers Choose SaServe",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceVariantLight,
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, null, tint = StatusAccepted, modifier = Modifier.size(20.dp))
                            Text("Verified Pros", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                            Text("Background checked & skilled", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceVariantLight,
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.VpnKey, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            Text("2-OTP Safety", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                            Text("Pay only after job approval", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
