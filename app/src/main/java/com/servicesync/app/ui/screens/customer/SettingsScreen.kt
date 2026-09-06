package com.servicesync.app.ui.screens.customer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servicesync.app.data.model.AppThemeMode
import com.servicesync.app.data.repository.ServiceSyncRepository
import com.servicesync.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: ServiceSyncRepository,
    onBackClick: () -> Unit
) {
    val currentUser by repository.currentUser.collectAsState()
    val activeThemeMode by repository.themeMode.collectAsState()

    var nameInput by remember(currentUser) { mutableStateOf(currentUser?.name ?: "") }
    var phoneInput by remember(currentUser) { mutableStateOf(currentUser?.phone ?: "") }
    var emailInput by remember(currentUser) { mutableStateOf(currentUser?.email ?: "") }
    var addressInput by remember(currentUser) { mutableStateOf(currentUser?.address ?: "") }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundLight,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Preferences",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Theme Customization Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "App Theme & Appearance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Choose between high-contrast Black, Light, or System",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

                    // 3 Theme Options: Dark, Light, System
                    ThemeOptionRow(
                        title = "Dark Mode (Pure Black & Cyan)",
                        subtitle = "High contrast pitch-black with radiant cyan accents",
                        icon = Icons.Default.DarkMode,
                        isSelected = activeThemeMode == AppThemeMode.DARK,
                        onClick = { repository.setThemeMode(AppThemeMode.DARK) }
                    )

                    ThemeOptionRow(
                        title = "Light Mode (Crisp White & Slate)",
                        subtitle = "Bright clean daylight theme with deep slate text",
                        icon = Icons.Default.LightMode,
                        isSelected = activeThemeMode == AppThemeMode.LIGHT,
                        onClick = { repository.setThemeMode(AppThemeMode.LIGHT) }
                    )

                    ThemeOptionRow(
                        title = "Follow System (Automatic)",
                        subtitle = "Automatically adapts to device system theme",
                        icon = Icons.Default.SettingsBrightness,
                        isSelected = activeThemeMode == AppThemeMode.SYSTEM,
                        onClick = { repository.setThemeMode(AppThemeMode.SYSTEM) }
                    )
                }
            }

            // Profile Editing Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ManageAccounts,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Edit Customer Profile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Update your name, contact phone, email, and address",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

                    // Full Name
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Full Name") },
                        placeholder = { Text("Customer Name") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = PrimaryBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Phone Number
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Registered Mobile Number") },
                        placeholder = { Text("9876543210") },
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = PrimaryBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Email Address
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address (Optional)") },
                        placeholder = { Text("customer@example.com") },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = PrimaryBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Delivery Address
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("Primary Delivery Address") },
                        placeholder = { Text("Enter address line") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = PrimaryBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            isSaving = true
                            repository.updateCustomerProfile(
                                name = nameInput.trim().ifBlank { "Customer" },
                                phone = phoneInput.trim(),
                                email = emailInput.trim(),
                                address = addressInput.trim().ifBlank { "Home Address" }
                            )
                            isSaving = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Profile details saved successfully! ✅")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color(0xFF0A0C0E), modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color(0xFF0A0C0E))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save Profile Changes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0A0C0E)
                            )
                        }
                    }
                }
            }

            // About SaServe App Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SaServe Platform",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "Version 2.4.0 • Built with Two-OTP Safety & Realtime Service Tracking",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "Customer Support: 📞 7488274632 • ✉️ sahaditya1804@gmail.com",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) PrimaryBlue.copy(alpha = 0.12f) else SurfaceVariantLight,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) PrimaryBlue else CardBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) PrimaryBlue else TextSecondary,
                modifier = Modifier.size(22.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) PrimaryBlue else TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = PrimaryBlue,
                    unselectedColor = TextSecondary
                )
            )
        }
    }
}
