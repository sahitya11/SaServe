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
    val activeThemeMode by repository.themeMode.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Preferences states
    var smsNotifications by remember { mutableStateOf(true) }
    var pushNotifications by remember { mutableStateOf(true) }
    var arrivalSoundAlerts by remember { mutableStateOf(true) }
    var instantSpecialistAutoMatch by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf("English (Default)") }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showResetCacheDialog by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Theme Customization Card
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

            // 2. Booking & Service Preferences
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
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Service & Booking Preferences",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Customize how specialists are dispatched to you",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

                    SettingToggleRow(
                        title = "Fast Auto-Dispatch",
                        subtitle = "Notify top-rated nearby specialists immediately when booking",
                        icon = Icons.Default.Speed,
                        checked = instantSpecialistAutoMatch,
                        onCheckedChange = {
                            instantSpecialistAutoMatch = it
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    if (it) "Fast Auto-Dispatch enabled" else "Manual specialist broadcast mode selected"
                                )
                            }
                        }
                    )

                    SettingToggleRow(
                        title = "Arrival Chime Alerts",
                        subtitle = "Play high-clarity notification chime when specialist is nearby",
                        icon = Icons.Default.VolumeUp,
                        checked = arrivalSoundAlerts,
                        onCheckedChange = { arrivalSoundAlerts = it }
                    )

                    // Language Preference
                    Surface(
                        onClick = { showLanguageDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceVariantLight,
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                                Column {
                                    Text("App Language", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    Text(selectedLanguage, style = MaterialTheme.typography.labelSmall, color = PrimaryBlue)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                        }
                    }
                }
            }

            // 3. Push Notifications & SMS Alerts
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
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Notifications & Live Updates",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Keep track of OTPs, arrival times, and job completions",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

                    SettingToggleRow(
                        title = "Push Notifications",
                        subtitle = "Instant status alerts when specialist accepts or arrives",
                        icon = Icons.Default.NotificationsActive,
                        checked = pushNotifications,
                        onCheckedChange = { pushNotifications = it }
                    )

                    SettingToggleRow(
                        title = "SMS Booking Updates",
                        subtitle = "Receive booking confirmation and Safety OTP via SMS",
                        icon = Icons.Default.Sms,
                        checked = smsNotifications,
                        onCheckedChange = { smsNotifications = it }
                    )
                }
            }

            // 4. Privacy, Security & Cache Management
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
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Privacy & Local Data",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Manage app cache and local storage",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Two-Step Safety Verification",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Start OTP and Completion OTP are strictly required before payment",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StatusAcceptedBg
                        ) {
                            Text(
                                text = "ACTIVE",
                                color = StatusAccepted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showResetCacheDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCancelled),
                        border = BorderStroke(1.dp, StatusCancelled.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = StatusCancelled)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear App Cache & Refresh Data", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 5. About SaServe Platform
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
                        text = "Version 2.5.0 • India's On-Demand Home Appliance & Repair Network",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "Customer Care: 📞 7488274632 • ✉️ sahaditya1804@gmail.com",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryBlue
                    )
                    Text(
                        text = "All service providers are ID-verified and background-checked.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Language Selector Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select App Language", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val languages = listOf("English (Default)", "हिन्दी (Hindi)", "বাংলা (Bengali)", "मराठी (Marathi)")
                    languages.forEach { lang ->
                        Surface(
                            onClick = {
                                selectedLanguage = lang
                                showLanguageDialog = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Language set to " + lang)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedLanguage == lang) PrimaryBlue.copy(alpha = 0.15f) else SurfaceVariantLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(lang, fontWeight = if (selectedLanguage == lang) FontWeight.Bold else FontWeight.Normal, color = TextPrimary)
                                if (selectedLanguage == lang) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryBlue)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Close", color = PrimaryBlue)
                }
            }
        )
    }

    // Clear Cache Confirmation Dialog
    if (showResetCacheDialog) {
        AlertDialog(
            onDismissRequest = { showResetCacheDialog = false },
            title = { Text("Clear Local Cache?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "This will clear temporary cache and reset service provider mock listings to latest Indian specialists. Your active account profile remains safe.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetCacheDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Cache cleared and provider index refreshed! ✨")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCancelled)
                ) {
                    Text("Clear Cache", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetCacheDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
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

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceVariantLight,
        border = BorderStroke(1.dp, CardBorder),
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
                tint = PrimaryBlue,
                modifier = Modifier.size(22.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryBlue,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = CardBorder
                )
            )
        }
    }
}
