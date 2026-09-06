package com.servicesync.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Vibrant Accents
val PrimaryBlue = Color(0xFF00D2EE)       // Radiant Electric Cyan Blue
val PrimaryBlueDark = Color(0xFF0097A7)   // Deep Sky Cyan
val SecondaryTeal = Color(0xFF00BFA5)     // Aqua Mint Teal
val AccentSky = Color(0xFF38BDF8)         // Light Sky
val AccentGold = Color(0xFFFFD54F)        // Amber Gold

// Black & Light High Contrast Mode (Deep Black background + light card surfaces)
val DarkBackground = Color(0xFF000000)    // Pure Obsidian Pitch Black
val DarkSurface = Color(0xFF14171E)       // Dark Slate Surface
val DarkSurfaceVariant = Color(0xFF1E222B)// Elevated Slate Card
val DarkTextPrimary = Color(0xFFF8FAFC)   // Crisp White
val DarkTextSecondary = Color(0xFF94A3B8) // Cool Silver
val DarkTextMuted = Color(0xFF64748B)     // Muted Gray
val DarkCardBorder = Color(0xFF262C38)    // Border for dark cards

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
val StatusPendingBg = Color(0xFFFEF3C7)
val StatusAccepted = Color(0xFF0284C7)
val StatusAcceptedBg = Color(0xFFE0F2FE)
val StatusInProgress = Color(0xFF8B5CF6)
val StatusInProgressBg = Color(0xFFEDE9FE)
val StatusCompleted = Color(0xFF10B981)
val StatusCompletedBg = Color(0xFFD1FAE5)
val StatusCancelled = Color(0xFFEF4444)
val StatusCancelledBg = Color(0xFFFEE2E2)

