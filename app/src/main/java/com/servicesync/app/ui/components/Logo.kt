package com.servicesync.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servicesync.app.R
import com.servicesync.app.ui.theme.PrimaryBlue
import com.servicesync.app.ui.theme.SecondaryTeal

/**
 * Official SaServe Logo Component
 *
 * Displays the glowing spiral S-loop (#2563EB to #06B6D4) and the SaServe typography
 * with the "Connecting You with Trusted Services" subtitle.
 */
@Composable
fun SaServeLogo(
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    showText: Boolean = true
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.saserve_logo),
            contentDescription = "SaServe Logo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Spiral Emblem only (from the SaServe logo)
 */
@Composable
fun SaServeSpiralIcon(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    Image(
        painter = painterResource(id = R.drawable.ic_launcher_foreground),
        contentDescription = "SaServe Spiral",
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}
