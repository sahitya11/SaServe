package com.servicesync.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Vibrant Accents (from SaServe Logo: Royal Blue #2563eb to Cyan #06b6d4)
val PrimaryBlue = Color(0xFF2563EB)       // Royal Blue (Logo Gradient Start)
val PrimaryBlueDark = Color(0xFF1D4ED8)   // Deep Royal Blue
val SecondaryTeal = Color(0xFF06B6D4)     // Vibrant Electric Cyan (Logo Gradient End)
val AccentSky = Color(0xFF38BDF8)         // Light Sky Blue
val AccentGold = Color(0xFFFFD54F)        // Amber Gold

// Blue & Black Theme Palette (Matching Logo Canvas #090d16)
val DarkBackground = Color(0xFF090D16)    // Deep Obsidian Black Canvas
val DarkSurface = Color(0xFF111726)       // Deep Slate Blue Surface
val DarkSurfaceVariant = Color(0xFF1B2236)// Elevated Slate Blue Card
val DarkTextPrimary = Color(0xFFFFFFFF)   // Crisp Pure White
val DarkTextSecondary = Color(0xFFE2E8F0) // Bright High-Contrast Cool Silver
val DarkTextMuted = Color(0xFFA0AEC0)     // Legible Muted Slate
val DarkCardBorder = Color(0xFF2E3A52)    // Clear Border for dark cards

// Light Mode (Crisp White / Light Minimalist Palette)
val LightBackground = Color(0xFFF8FAFC)   // Crisp Off-White
val LightSurface = Color(0xFFFFFFFF)      // Pure White Card
val LightSurfaceVariant = Color(0xFFF1F5F9)// Soft Gray
val LightTextPrimary = Color(0xFF0F172A)  // Deep Obsidian Text
val LightTextSecondary = Color(0xFF475569)// Slate Gray
val LightTextMuted = Color(0xFF94A3B8)    // Soft Muted
val LightCardBorder = Color(0xFFE2E8F0)   // Border for light cards

// Default tokens referencing active palette dynamically
val BackgroundLight: Color @Composable get() = MaterialTheme.colorScheme.background
val SurfaceLight: Color @Composable get() = MaterialTheme.colorScheme.surface
val SurfaceVariantLight: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val TextPrimary: Color @Composable get() = MaterialTheme.colorScheme.onBackground
val TextSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val TextMuted: Color @Composable get() = MaterialTheme.colorScheme.outline
val CardBorder: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant

// Ratings & Status Colors (Optimized for both dark & light backgrounds)
val StarGold = Color(0xFFFFB300)
val StatusPending = Color(0xFFF59E0B)
val StatusAccepted = Color(0xFF0284C7)
val StatusInProgress = Color(0xFF8B5CF6)
val StatusCompleted = Color(0xFF10B981)
val StatusCancelled = Color(0xFFEF4444)

// Adaptive Status Backgrounds: deep luminous tints in Dark Mode, soft pastels in Light Mode
val StatusPendingBg: Color @Composable get() =
    if (MaterialTheme.colorScheme.background == DarkBackground) Color(0xFF382305) else Color(0xFFFEF3C7)

val StatusAcceptedBg: Color @Composable get() =
    if (MaterialTheme.colorScheme.background == DarkBackground) Color(0xFF0C2740) else Color(0xFFE0F2FE)

val StatusInProgressBg: Color @Composable get() =
    if (MaterialTheme.colorScheme.background == DarkBackground) Color(0xFF241544) else Color(0xFFEDE9FE)

val StatusCompletedBg: Color @Composable get() =
    if (MaterialTheme.colorScheme.background == DarkBackground) Color(0xFF09331E) else Color(0xFFD1FAE5)

val StatusCancelledBg: Color @Composable get() =
    if (MaterialTheme.colorScheme.background == DarkBackground) Color(0xFF3F1114) else Color(0xFFFEE2E2)

