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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.servicesync.app.data.model.Booking
import com.servicesync.app.data.model.BookingStatus
import com.servicesync.app.data.repository.ServiceSyncRepository
import com.servicesync.app.ui.components.StatusBadge
import com.servicesync.app.ui.components.getCategoryIcon
import com.servicesync.app.ui.components.getSpecialistAvatarDrawable
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
            // 1. SaServe Hero Status Card
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
                            BookingStatus.PENDING -> if (booking.providerId.isBlank() || booking.providerName.contains("Searching", ignoreCase = true)) "Broadcasting to Nearby Specialists 📡" else "Request Sent to Specialist"
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
                            BookingStatus.PENDING -> if (booking.providerId.isBlank() || booking.providerName.contains("Searching", ignoreCase = true)) {
                                "Broadcasting your service request to all nearby verified ${booking.category.displayName} specialists. Whoever accepts the service first will be assigned and shown here immediately."
                            } else {
                                "Waiting for ${booking.providerName} to accept your request. You will receive an instant notification once confirmed."
                            }
                            BookingStatus.ACCEPTED -> "${booking.providerName} has accepted your booking for ${booking.scheduledDate} at ${booking.scheduledSlot}."
                            BookingStatus.IN_PROGRESS -> "The specialist has started your service. Share the Completion OTP when work finishes."
                            BookingStatus.COMPLETED -> "Thank you! The job was completed and closed with the verified OTP."
                            BookingStatus.CANCELLED -> "This booking was cancelled."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = TextPrimary
                    )
                }
            }

            // 2. SASERVE TWO-OTP SECURITY SECTION
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
                                        text = if (booking.providerId.isBlank() || booking.providerName.contains("Searching", ignoreCase = true)) "Broadcasting to Nearby Specialists" else "Awaiting Specialist Acceptance",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = StatusPending
                                    )
                                    Text(
                                        text = if (booking.providerId.isBlank() || booking.providerName.contains("Searching", ignoreCase = true))
                                            "Your Start and Completion OTPs will unlock immediately once a nearby specialist accepts your booking."
                                        else
                                            "Your Start and Completion OTPs will unlock immediately once ${booking.providerName} accepts your booking.",
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
                            }
                        }

                        // OTP 2: Service Completion OTP (Only revealed once service starts and Start OTP is entered)
                        if (booking.status == BookingStatus.IN_PROGRESS || booking.status == BookingStatus.COMPLETED) {
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
                                            "Share with specialist ONLY after the job is fully completed and inspected to finalize.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (booking.status == BookingStatus.COMPLETED) StatusCompleted else TextSecondary
                                    )
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
                    if (booking.status == BookingStatus.PENDING && (booking.providerId.isBlank() || booking.providerName.contains("Searching", ignoreCase = true))) {
                        Text(
                            text = "Broadcast Status",
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
                                    .background(StatusPending.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.NearMe, null, tint = StatusPending, modifier = Modifier.size(28.dp))
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Broadcasting to Nearby Specialists 📡",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Request sent to all nearby verified ${booking.category.displayName} specialists. Whoever accepts the service first will be displayed here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
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
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, PrimaryBlue, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = getSpecialistAvatarDrawable(booking.category)),
                                    contentDescription = booking.providerName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
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
            }

            // Google Maps Live GPS Tracking View (Shown when specialist is assigned and on the way / in progress)
            if (booking.status == BookingStatus.ACCEPTED || booking.status == BookingStatus.IN_PROGRESS) {
                GoogleMapsLiveTrackingCard(booking = booking)
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
                        val formattedTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(booking.createdAt))
                        val displaySchedule = if (booking.scheduledSlot.contains("Immediate", ignoreCase = true)) {
                            "${booking.scheduledDate} at $formattedTime"
                        } else {
                            "${booking.scheduledDate} (${booking.scheduledSlot})"
                        }
                        Text(text = displaySchedule, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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
                        Text(text = "#${booking.displayBookingId}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // 5. Specialist Tipping Card (Beneath Appointment Summary)
            SpecialistTippingCard(
                booking = booking,
                onTipSelected = { tip ->
                    repository.addTipToBooking(booking.id, tip)
                }
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

/**
 * High-fidelity Google Maps Live Specialist Tracking Card
 */
@Composable
fun GoogleMapsLiveTrackingCard(booking: Booking) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Live Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(StatusAccepted)
                    )
                    Text(
                        text = "Live Google Maps Tracking",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryBlue.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(12.dp))
                        Text(
                            text = "GPS Active",
                            color = PrimaryBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Stylized Dark Google Maps Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1B2333))
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            ) {
                // Map Background Grid & Polyline Route Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    val streetColor = Color(0xFF253147)
                    val highwayColor = Color(0xFF334260)

                    // Draw secondary streets
                    var y = 25f
                    while (y < h) {
                        drawLine(streetColor, Offset(0f, y), Offset(w, y), strokeWidth = 2.5f)
                        y += 35f
                    }
                    var x = 30f
                    while (x < w) {
                        drawLine(streetColor, Offset(x, 0f), Offset(x, h), strokeWidth = 2.5f)
                        x += 45f
                    }

                    // Main boulevard highways
                    drawLine(highwayColor, Offset(0f, h * 0.6f), Offset(w, h * 0.45f), strokeWidth = 8f)
                    drawLine(highwayColor, Offset(w * 0.45f, 0f), Offset(w * 0.55f, h), strokeWidth = 8f)

                    // Navigation Route Path (Blue Glowing Polyline)
                    val path = Path().apply {
                        moveTo(w * 0.2f, h * 0.72f) // Specialist start
                        cubicTo(
                            w * 0.35f, h * 0.62f,
                            w * 0.5f, h * 0.5f,
                            w * 0.78f, h * 0.3f // Customer home
                        )
                    }

                    // Route outer glow
                    drawPath(
                        path = path,
                        color = PrimaryBlue.copy(alpha = 0.35f),
                        style = Stroke(width = 12f)
                    )
                    // Route dash polyline
                    drawPath(
                        path = path,
                        color = PrimaryBlue,
                        style = Stroke(
                            width = 5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f)
                        )
                    )
                }

                // Specialist Marker (Left Bottom on Route)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 26.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryBlue,
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                text = "${booking.providerName} (En Route)",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TwoWheeler,
                                contentDescription = "Specialist en route",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Customer Home Pin (Top Right on Route)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 20.dp, end = 32.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StatusCompleted,
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                text = "Your Address",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(StatusCompleted)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Google Watermark badge at bottom right
                Text(
                    text = "Google Maps Live",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }

            // ETA and Distance Status Bar
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceVariantLight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        Column {
                            Text(
                                text = "Expected Arrival: 15–25 mins",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Distance: 2.1 km away • Moving towards your location",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StatusAcceptedBg
                    ) {
                        Text(
                            text = "EN ROUTE",
                            color = StatusAccepted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Specialist Tipping Card with selectable tip options updating final bill
 */
@Composable
fun SpecialistTippingCard(
    booking: Booking,
    onTipSelected: (Double) -> Unit
) {
    val tipOptions = listOf(10.0, 20.0, 50.0, 100.0)

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
                Icon(Icons.Default.Favorite, contentDescription = null, tint = AccentGold)
                Text(
                    text = "Tip Your Specialist",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Show appreciation for ${booking.providerName}'s prompt and dedicated service. 100% of your tip goes directly to the specialist.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            // Tip Option Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tipOptions.forEach { tip ->
                    val isSelected = booking.tipAmount == tip
                    OutlinedButton(
                        onClick = {
                            if (isSelected) {
                                onTipSelected(0.0)
                            } else {
                                onTipSelected(tip)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = if (isSelected) ButtonDefaults.buttonColors(containerColor = AccentGold) else ButtonDefaults.outlinedButtonColors(),
                        border = BorderStroke(1.dp, if (isSelected) AccentGold else CardBorder),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text(
                            text = "+₹${tip.toInt()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSelected) Color.White else TextPrimary
                        )
                    }
                }
            }

            // Summary of price with tip
            if (booking.tipAmount > 0.0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AccentGold.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "✓ ₹${booking.tipAmount.toInt()} Tip Added for ${booking.providerName}",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Base Rate: ₹${booking.hourlyRate.toInt()} + Tip: ₹${booking.tipAmount.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        Text(
                            text = "₹${(booking.hourlyRate + booking.tipAmount).toInt()}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = PrimaryBlue
                        )
                    }
                }
            }
        }
    }
}


