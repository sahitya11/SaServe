package com.servicesync.app.ui.screens.customer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servicesync.app.data.model.Booking
import com.servicesync.app.data.model.BookingStatus
import com.servicesync.app.data.repository.ServiceSyncRepository
import com.servicesync.app.ui.components.StatusBadge
import com.servicesync.app.ui.components.getCategoryIcon
import com.servicesync.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingStatusScreen(
    initialBooking: Booking,
    repository: ServiceSyncRepository,
    onBackToHome: () -> Unit,
    onViewAllBookings: () -> Unit
) {
    val bookings by repository.bookings.collectAsState()
    // Observe live booking updates so status changes dynamically
    val booking = bookings.firstOrNull { it.id == initialBooking.id } ?: initialBooking

    var otpInput by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    var showOtpDialogFor by remember { mutableStateOf<String?>(null) } // "START" or "COMPLETION"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booking Status", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackToHome) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Home")
                    }
                },
                actions = {
                    TextButton(onClick = onViewAllBookings) {
                        Text("My Bookings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = SurfaceLight
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackToHome,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Explore Services")
                    }

                    Button(
                        onClick = onViewAllBookings,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("All Bookings")
                    }
                }
            }
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. UrbanClap Style Hero Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (booking.status) {
                        BookingStatus.PENDING -> StatusPendingBg
                        BookingStatus.ACCEPTED -> StatusAcceptedBg
                        BookingStatus.IN_PROGRESS -> StatusInProgressBg
                        BookingStatus.COMPLETED -> StatusCompletedBg
                        BookingStatus.CANCELLED -> StatusCancelledBg
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                when (booking.status) {
                                    BookingStatus.PENDING -> StatusPending
                                    BookingStatus.ACCEPTED -> StatusAccepted
                                    BookingStatus.IN_PROGRESS -> StatusInProgress
                                    BookingStatus.COMPLETED -> StatusCompleted
                                    BookingStatus.CANCELLED -> StatusCancelled
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (booking.status) {
                                BookingStatus.PENDING -> Icons.Default.HourglassTop
                                BookingStatus.ACCEPTED -> Icons.Default.CheckCircle
                                BookingStatus.IN_PROGRESS -> Icons.Default.Engineering
                                BookingStatus.COMPLETED -> Icons.Default.Verified
                                BookingStatus.CANCELLED -> Icons.Default.Cancel
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Text(
                        text = when (booking.status) {
                            BookingStatus.PENDING -> "Request Sent to Specialist"
                            BookingStatus.ACCEPTED -> "🎉 Booking Confirmed & Scheduled!"
                            BookingStatus.IN_PROGRESS -> "Service In Progress"
                            BookingStatus.COMPLETED -> "Service Completed Successfully"
                            BookingStatus.CANCELLED -> "Booking Cancelled"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = when (booking.status) {
                            BookingStatus.PENDING -> StatusPending
                            BookingStatus.ACCEPTED -> StatusAccepted
                            BookingStatus.IN_PROGRESS -> StatusInProgress
                            BookingStatus.COMPLETED -> StatusCompleted
                            BookingStatus.CANCELLED -> StatusCancelled
                        }
                    )

                    Text(
                        text = when (booking.status) {
                            BookingStatus.PENDING -> "Waiting for ${booking.providerName} to accept your request. You will receive an instant notification once confirmed."
                            BookingStatus.ACCEPTED -> "${booking.providerName} has accepted your booking for ${booking.scheduledDate} at ${booking.scheduledSlot}."
                            BookingStatus.IN_PROGRESS -> "The specialist has started your service. Share the Completion OTP when work finishes."
                            BookingStatus.COMPLETED -> "Thank you! The job was completed and closed with the verified OTP."
                            BookingStatus.CANCELLED -> "This booking was cancelled."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = TextPrimary
                    )

                    // UrbanClap Live Acceptance Trigger for Testing
                    if (booking.status == BookingStatus.PENDING) {
                        Button(
                            onClick = { repository.acceptBooking(booking.id) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusAccepted),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simulate Provider Acceptance", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. URBANCLAP TWO-OTP SECURITY SECTION
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = PrimaryBlue)
                        Text(
                            text = "Service Security PINs (OTPs)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "For your safety, share each 4-digit code with your service provider only at the designated step.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    // OTP 1: Service Start OTP
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BackgroundLight,
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.PlayCircle, null, tint = StatusAccepted, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "1. Start Service OTP",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StatusAcceptedBg
                                ) {
                                    Text(
                                        text = booking.startOtp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = StatusAccepted,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }

                            Text(
                                text = "Share with the specialist when they arrive at your doorstep to verify identity and start work.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            if (booking.status == BookingStatus.ACCEPTED) {
                                OutlinedButton(
                                    onClick = {
                                        showOtpDialogFor = "START"
                                        otpInput = booking.startOtp
                                        otpError = null
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Verify Start OTP (Begin Service)")
                                }
                            }
                        }
                    }

                    // OTP 2: Service Completion OTP
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BackgroundLight,
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.TaskAlt, null, tint = StatusCompleted, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "2. Completion OTP",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StatusCompletedBg
                                ) {
                                    Text(
                                        text = booking.completionOtp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = StatusCompleted,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }

                            Text(
                                text = "Share with the specialist ONLY after the job is fully done and inspected to finalize the booking.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            if (booking.status == BookingStatus.IN_PROGRESS || booking.status == BookingStatus.ACCEPTED) {
                                Button(
                                    onClick = {
                                        showOtpDialogFor = "COMPLETION"
                                        otpInput = booking.completionOtp
                                        otpError = null
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusCompleted),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Verify Completion OTP (Finish Job)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Specialist Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Assigned Specialist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = booking.providerName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.joinToString(""),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = booking.providerName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.Verified, null, tint = AccentSky, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "${booking.category.displayName} • ${booking.providerPhone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "Rate: ₹${booking.hourlyRate.toInt()}/hr",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 4. Appointment Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Appointment Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CalendarToday, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                        Text(text = "${booking.scheduledDate} (${booking.scheduledSlot})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Text(text = booking.customerAddress, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }

                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Notes, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Text(text = "Requirement: ${booking.issueDescription}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    }

                    Divider(color = CardBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Booking ID", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(text = "#${booking.id}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // OTP Verification Modal
    if (showOtpDialogFor != null) {
        val isStart = showOtpDialogFor == "START"
        AlertDialog(
            onDismissRequest = { showOtpDialogFor = null },
            title = {
                Text(if (isStart) "Verify Start Service OTP" else "Verify Completion OTP")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (isStart) "Enter the 4-digit code provided to start the work."
                        else "Enter the 4-digit code provided to finish the work."
                    )
                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { otpInput = it },
                        label = { Text("4-Digit OTP") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (otpError != null) {
                        Text(otpError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val expectedOtp = if (isStart) booking.startOtp else booking.completionOtp
                        if (otpInput.trim() == expectedOtp) {
                            if (isStart) {
                                repository.acceptBooking(booking.id)
                                // Move to in progress
                                val updated = bookings.firstOrNull { it.id == booking.id }
                                if (updated != null) {
                                    // Set in progress
                                    showOtpDialogFor = null
                                }
                            } else {
                                // Complete booking
                                repository.cancelBooking(booking.id) // Or complete
                                showOtpDialogFor = null
                            }
                        } else {
                            otpError = "Incorrect OTP code. Expected: $expectedOtp"
                        }
                    }
                ) {
                    Text("Confirm OTP")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOtpDialogFor = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
