package com.servicesync.app.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servicesync.app.data.repository.ServiceSyncRepository
import com.servicesync.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    repository: ServiceSyncRepository,
    onAuthSuccess: () -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(true) } // Default to Register for first-time user

    var phoneInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf("") }
    var flatHouseInput by remember { mutableStateOf("") }
    var streetAreaInput by remember { mutableStateOf("") }
    var landmarkInput by remember { mutableStateOf("") }
    var instructionsInput by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
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
                    .background(PrimaryBlue)
                    .padding(horizontal = 24.dp, vertical = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Handyman,
                            contentDescription = "SaServe Logo",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Text(
                        text = "SaServe",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "Your Trusted Home Services Specialist",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Two-OTP Protected Services",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
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
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = isRegisterMode,
                            onClick = {
                                isRegisterMode = true
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
                            onValueChange = { nameInput = it; errorMessage = null },
                            label = { Text("Full Name") },
                            placeholder = { Text("e.g. Sahitya Sharma") },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Phone Number
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = {
                            phoneInput = it
                            errorMessage = null
                        },
                        label = { Text("Phone Number") },
                        placeholder = { Text("9876543210") },
                        leadingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 12.dp, end = 6.dp)
                            ) {
                                Icon(Icons.Default.Phone, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+91", fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Password
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it; errorMessage = null },
                        label = { Text("Password") },
                        placeholder = { Text("Enter your password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Register-only: Address Form
                    if (isRegisterMode) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        var isDetectingLocation by remember { mutableStateOf(false) }

                        val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
                        ) { permissions ->
                            val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
                            val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
                            if (fineGranted || coarseGranted) {
                                isDetectingLocation = true
                                com.servicesync.app.ui.screens.customer.detectCurrentGpsLocation(context) { detected ->
                                    isDetectingLocation = false
                                    if (!detected.isNullOrBlank()) {
                                        streetAreaInput = detected
                                        addressInput = detected
                                    }
                                }
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Service Address Details",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            // GPS Auto-detect button
                            OutlinedButton(
                                onClick = {
                                    val fineGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    val coarseGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                    if (fineGranted || coarseGranted) {
                                        isDetectingLocation = true
                                        com.servicesync.app.ui.screens.customer.detectCurrentGpsLocation(context) { detected ->
                                            isDetectingLocation = false
                                            if (!detected.isNullOrBlank()) {
                                                streetAreaInput = detected
                                                addressInput = detected
                                            }
                                        }
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                            ) {
                                if (isDetectingLocation) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = PrimaryBlue
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Detecting GPS Street / Area...", fontSize = 13.sp)
                                } else {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("📍 Auto-detect GPS Street / Area", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Flat / House No
                            OutlinedTextField(
                                value = flatHouseInput,
                                onValueChange = { flatHouseInput = it; errorMessage = null },
                                label = { Text("Flat / House / Building No.") },
                                placeholder = { Text("e.g. Flat 301, Sunshine Heights") },
                                leadingIcon = { Icon(Icons.Default.Apartment, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Street / Road / Area
                            OutlinedTextField(
                                value = streetAreaInput,
                                onValueChange = { streetAreaInput = it; addressInput = it; errorMessage = null },
                                label = { Text("Street / Road / Area / Sector") },
                                placeholder = { Text("e.g. 14th Main Rd, Indiranagar") },
                                leadingIcon = { Icon(Icons.Default.Signpost, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Landmark
                            OutlinedTextField(
                                value = landmarkInput,
                                onValueChange = { landmarkInput = it; errorMessage = null },
                                label = { Text("Nearby Landmark (Optional)") },
                                placeholder = { Text("e.g. Opposite Metro Station, Near Park") },
                                leadingIcon = { Icon(Icons.Default.NearMe, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Reach Instructions
                            OutlinedTextField(
                                value = instructionsInput,
                                onValueChange = { instructionsInput = it; errorMessage = null },
                                label = { Text("Instructions to Reach (Optional)") },
                                placeholder = { Text("e.g. Ring doorbell #3, take lift to 3rd floor") },
                                leadingIcon = { Icon(Icons.Default.Directions, null) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 3,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Action Button
                    Button(
                        onClick = {
                            val cleanPhone = phoneInput.trim()
                            val cleanPassword = passwordInput.trim()

                            if (cleanPhone.isBlank()) {
                                errorMessage = "Please enter your phone number."
                                return@Button
                            }
                            if (cleanPhone.replace(Regex("[^0-9]"), "").length < 10) {
                                errorMessage = "Please enter a valid 10-digit phone number."
                                return@Button
                            }
                            if (cleanPassword.length < 4) {
                                errorMessage = "Password must be at least 4 characters long."
                                return@Button
                            }

                            isLoading = true
                            if (isRegisterMode) {
                                val fullAddr = if (streetAreaInput.isNotBlank()) streetAreaInput else addressInput
                                val success = repository.registerCustomer(
                                    phone = cleanPhone,
                                    password = cleanPassword,
                                    name = nameInput.ifBlank { "Customer" },
                                    address = fullAddr.ifBlank { "Home Address" },
                                    flatHouseNo = flatHouseInput,
                                    streetArea = streetAreaInput,
                                    landmark = landmarkInput,
                                    reachInstructions = instructionsInput
                                )
                                isLoading = false
                                if (success) {
                                    onAuthSuccess()
                                } else {
                                    errorMessage = "Registration failed. Please try again."
                                }
                            } else {
                                val success = repository.loginCustomer(
                                    phone = cleanPhone,
                                    password = cleanPassword
                                )
                                isLoading = false
                                if (success) {
                                    onAuthSuccess()
                                } else {
                                    errorMessage = "Incorrect phone number or password. If new, please Register."
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
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Text(
                                text = if (isRegisterMode) "Register & Start Exploring" else "Sign In to SaServe",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Toggle bottom text
                    TextButton(
                        onClick = {
                            isRegisterMode = !isRegisterMode
                            errorMessage = null
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = if (isRegisterMode) "Already have an account? Sign In" else "New customer? Register now",
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
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, null, tint = StatusAccepted, modifier = Modifier.size(20.dp))
                            Text("Verified Pros", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Background checked & skilled", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceVariantLight,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.VpnKey, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            Text("2-OTP Safety", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Pay only after job approval", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
