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
            if (booking.status == BookingStatus.PENDING) {
                // OTPs locked until specialist accepts
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = StatusPending)
                            Text(
                                text = "Security OTPs (Locked)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StatusPendingBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = StatusPending,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Awaiting Specialist Acceptance",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = StatusPending
                                    )
                                    Text(
                                        text = "Your Start and Completion OTPs will unlock immediately once ${booking.providerName} accepts your booking.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // OTPs UNLOCKED (status is ACCEPTED, IN_PROGRESS, or COMPLETED)
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
                            text = "Share each 4-digit code with your specialist only at the designated step.",
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
                                        Icon(
                                            Icons.Default.PlayCircle,
                                            null,
                                            tint = if (booking.status == BookingStatus.IN_PROGRESS || booking.status == BookingStatus.COMPLETED) StatusCompleted else StatusAccepted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "1. Start Service OTP",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (booking.status == BookingStatus.IN_PROGRESS || booking.status == BookingStatus.COMPLETED) StatusCompletedBg else StatusAcceptedBg
                                    ) {
                                        Text(
                                            text = booking.startOtp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (booking.status == BookingStatus.IN_PROGRESS || booking.status == BookingStatus.COMPLETED) StatusCompleted else StatusAccepted,
                                            letterSpacing = 2.sp
                                        )
                                    }
                                }

                                Text(
                                    text = if (booking.status == BookingStatus.IN_PROGRESS || booking.status == BookingStatus.COMPLETED)
                                        "✓ Start OTP verified! Specialist has begun the work."
                                    else
                                        "Share with specialist when they arrive at your doorstep to start work.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (booking.status == BookingStatus.IN_PROGRESS || booking.status == BookingStatus.COMPLETED) StatusCompleted else TextSecondary
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
                                        Icon(Icons.Default.VpnKey, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Enter Start OTP (Begin Service)")
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
                                    text = if (booking.status == BookingStatus.COMPLETED)
                                        "✓ Completion OTP verified! Booking successfully finalized."
                                    else
                                        "Share with specialist ONLY after the job is fully done and inspected to finalize.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (booking.status == BookingStatus.COMPLETED) StatusCompleted else TextSecondary
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
                                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Enter Completion OTP (Finish Job)", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Celebratory Happy Message & Review Section when COMPLETED
            if (booking.status == BookingStatus.COMPLETED) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusCompletedBg),
                    border = BorderStroke(1.dp, StatusCompleted.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(StatusCompleted),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        Text(
                            text = "🎉 Service Completed Successfully!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = StatusCompleted,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Thank you for choosing SaServe! We hope you had a wonderful experience with ${booking.providerName}. Your satisfaction is our top priority.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Interactive Rating & Review Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B))
                            Text(
                                text = "Rate & Review Specialist",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (booking.customerRating != null) {
                            // Review already submitted
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceVariantLight,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        repeat(5) { starIndex ->
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (starIndex < (booking.customerRating ?: 5f).toInt()) Color(0xFFF59E0B) else TextMuted,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${booking.customerRating?.toInt() ?: 5} / 5 Stars", fontWeight = FontWeight.Bold)
                                    }
                                    if (!booking.customerReview.isNullOrBlank()) {
                                        Text(
                                            text = "\"${booking.customerReview}\"",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    }
                                    Text(
                                        text = "✓ Review saved to ${booking.providerName}'s profile. Thank you for your feedback!",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusCompleted,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            var selectedRating by remember { mutableStateOf(5f) }
                            var reviewText by remember { mutableStateOf("") }
                            var reviewSubmitted by remember { mutableStateOf(false) }

                            if (reviewSubmitted) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = StatusCompletedBg,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "✓ Thank you for your review!",
                                        color = StatusCompleted,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "How was your experience with ${booking.providerName}?",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )

                                    // 5 Stars row
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        (1..5).forEach { star ->
                                            IconButton(
                                                onClick = { selectedRating = star.toFloat() },
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "$star Stars",
                                                    tint = if (star <= selectedRating) Color(0xFFF59E0B) else TextMuted,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${selectedRating.toInt()} / 5",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF59E0B),
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }

                                    OutlinedTextField(
                                        value = reviewText,
                                        onValueChange = { reviewText = it },
                                        label = { Text("Write your review") },
                                        placeholder = { Text("e.g. Prompt arrival, polite specialist, problem resolved quickly!") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        maxLines = 3,
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Button(
                                        onClick = {
                                            repository.addReviewForBooking(
                                                bookingId = booking.id,
                                                rating = selectedRating,
                                                comment = reviewText.ifBlank { "Great service by ${booking.providerName}!" }
                                            )
                                            reviewSubmitted = true
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Submit Specialist Review", fontWeight = FontWeight.Bold)
                                    }
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
                                val success = repository.startBookingWithOtp(booking.id, otpInput.trim())
                                if (success) {
                                    showOtpDialogFor = null
                                } else {
                                    otpError = "Failed to start service."
                                }
                            } else {
                                val success = repository.completeBookingWithOtp(booking.id, otpInput.trim())
                                if (success) {
                                    showOtpDialogFor = null
                                } else {
                                    otpError = "Failed to complete service."
                                }
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
