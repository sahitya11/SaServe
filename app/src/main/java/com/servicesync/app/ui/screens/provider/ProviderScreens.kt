package com.servicesync.app.ui.screens.provider

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.servicesync.app.data.model.Booking
import com.servicesync.app.data.model.BookingStatus
import com.servicesync.app.data.repository.ServiceSyncRepository
import com.servicesync.app.ui.components.RatingDisplay
import com.servicesync.app.ui.components.StatusBadge
import com.servicesync.app.ui.components.getCategoryIcon
import com.servicesync.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDashboardScreen(
    repository: ServiceSyncRepository,
    onViewIncomingRequests: () -> Unit,
    onViewActiveJobs: () -> Unit,
    onViewProfile: () -> Unit,
    onSwitchRoleClick: () -> Unit
) {
    val currentUser by repository.currentUser.collectAsState()
    val bookings by repository.bookings.collectAsState()
    val providers by repository.providers.collectAsState()

    val currentProvider = providers.firstOrNull { it.id == currentUser?.providerId }
        ?: providers.firstOrNull { it.id == "prov_marcus" }
        ?: providers.firstOrNull()

    // Count bookings assigned to this provider or matching category
    val myBookings = bookings.filter {
        it.providerId == currentProvider?.id || currentProvider == null
    }

    val pendingRequests = myBookings.filter { it.status == BookingStatus.PENDING }
    val activeJobs = myBookings.filter { it.status == BookingStatus.ACCEPTED || it.status == BookingStatus.IN_PROGRESS }
    val completedJobs = myBookings.filter { it.status == BookingStatus.COMPLETED }

    var isOnline by remember { mutableStateOf(currentProvider?.isAvailable ?: true) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Provider Portal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Text(
                            text = currentProvider?.name ?: "Service Partner",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    // Switch back to Customer mode button
                    FilledTonalButton(
                        onClick = onSwitchRoleClick,
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = StatusAcceptedBg,
                            contentColor = PrimaryBlue
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Provider", style = MaterialTheme.typography.labelSmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        snackbarHost = {
            if (snackbarMessage != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("OK", color = Color.White)
                        }
                    }
                ) {
                    Text(snackbarMessage ?: "")
                }
            }
        },
        containerColor = BackgroundLight
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile & Status Header Card
            item {
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (currentProvider?.name ?: "Pro").split(" ")
                                        .mapNotNull { it.firstOrNull()?.toString() }.joinToString(""),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = currentProvider?.name ?: "Service Specialist",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = "Verified",
                                        tint = AccentSky,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "${currentProvider?.category?.displayName ?: "General Service"} Specialist",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                RatingDisplay(
                                    rating = currentProvider?.rating ?: 4.9f,
                                    reviewCount = currentProvider?.reviewCount ?: 120
                                )
                            }
                        }

                        Divider(color = CardBorder)

                        // Online/Available Toggle
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
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) StatusCompleted else Color.Gray)
                                )
                                Text(
                                    text = if (isOnline) "Accepting Booking Requests" else "Currently Offline",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Switch(
                                checked = isOnline,
                                onCheckedChange = { isOnline = it }
                            )
                        }
                    }
                }
            }

            // Quick Stats Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pending Requests Stat Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onViewIncomingRequests() },
                        colors = CardDefaults.cardColors(
                            containerColor = if (pendingRequests.isNotEmpty()) StatusPendingBg else SurfaceLight
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.HourglassEmpty,
                                contentDescription = null,
                                tint = if (pendingRequests.isNotEmpty()) StatusPending else TextSecondary
                            )
                            Text(
                                text = "${pendingRequests.size}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (pendingRequests.isNotEmpty()) StatusPending else TextPrimary
                            )
                            Text(
                                text = "Pending Requests",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    // Active Jobs Stat Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onViewActiveJobs() },
                        colors = CardDefaults.cardColors(
                            containerColor = if (activeJobs.isNotEmpty()) StatusAcceptedBg else SurfaceLight
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Handyman,
                                contentDescription = null,
                                tint = if (activeJobs.isNotEmpty()) StatusAccepted else TextSecondary
                            )
                            Text(
                                text = "${activeJobs.size}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (activeJobs.isNotEmpty()) StatusAccepted else TextPrimary
                            )
                            Text(
                                text = "Active Jobs",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Pending Booking Requests Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Customer Requests",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (pendingRequests.isNotEmpty()) {
                        TextButton(onClick = onViewIncomingRequests) {
                            Text("See All (${pendingRequests.size})")
                        }
                    }
                }
            }

            if (pendingRequests.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Inbox,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "No pending requests right now.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Switch to Customer mode to book a new service slot!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PrimaryBlue
                                )
                            }
                        }
                    }
                }
            } else {
                items(pendingRequests.take(3)) { booking ->
                    ProviderRequestCard(
                        booking = booking,
                        onAccept = {
                            repository.acceptBooking(booking.id)
                            snackbarMessage = "🎉 Booking accepted! Customer notified via system alert."
                        },
                        onDecline = {
                            repository.declineBooking(booking.id)
                            snackbarMessage = "Booking request declined."
                        }
                    )
                }
            }

            // Active / In-Progress Jobs Preview
            if (activeJobs.isNotEmpty()) {
                item {
                    Text(
                        text = "Scheduled & In-Progress Jobs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(activeJobs) { job ->
                    ProviderJobCard(
                        job = job,
                        onStatusChange = { newStatus ->
                            repository.updateBookingStatus(job.id, newStatus)
                            snackbarMessage = "Job status updated to ${newStatus.label}"
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProviderRequestCard(
    booking: Booking,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StatusPendingBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = StatusPending,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = booking.customerName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = booking.customerPhone,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                StatusBadge(status = booking.status)
            }

            Divider(color = CardBorder)

            // Booking slot and location
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${booking.scheduledDate} at ${booking.scheduledSlot}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Text(
                        text = booking.customerAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Description, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Problem: ${booking.issueDescription}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }

            // Action Buttons: Accept or Decline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCancelled)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Decline")
                }

                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1.4f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCompleted)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Accept Booking", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProviderJobCard(
    job: Booking,
    onStatusChange: (BookingStatus) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${job.customerName} (${job.category.displayName})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(status = job.status)
            }

            Text(
                text = "${job.scheduledDate} • ${job.scheduledSlot}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Location: ${job.customerAddress}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Text(
                text = "Issue: ${job.issueDescription}",
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
            )

            Divider(color = CardBorder)

            // Status Transitions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (job.status == BookingStatus.ACCEPTED) {
                    Button(
                        onClick = { onStatusChange(BookingStatus.IN_PROGRESS) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusInProgress)
                    ) {
                        Text("Start Job (In Progress)")
                    }
                } else if (job.status == BookingStatus.IN_PROGRESS) {
                    Button(
                        onClick = { onStatusChange(BookingStatus.COMPLETED) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusCompleted)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark Job Completed")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingRequestsScreen(
    repository: ServiceSyncRepository,
    onBackClick: () -> Unit
) {
    val bookings by repository.bookings.collectAsState()
    val pendingRequests = bookings.filter { it.status == BookingStatus.PENDING }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incoming Booking Requests") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        snackbarHost = {
            if (snackbarMessage != null) {
                Snackbar(modifier = Modifier.padding(16.dp)) {
                    Text(snackbarMessage ?: "")
                }
            }
        },
        containerColor = BackgroundLight
    ) { padding ->
        if (pendingRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircleOutline,
                        contentDescription = null,
                        tint = StatusCompleted,
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = "All caught up! No pending requests.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(pendingRequests) { booking ->
                    ProviderRequestCard(
                        booking = booking,
                        onAccept = {
                            repository.acceptBooking(booking.id)
                            snackbarMessage = "🎉 Booking accepted! Customer notified via system alert."
                        },
                        onDecline = {
                            repository.declineBooking(booking.id)
                            snackbarMessage = "Booking declined."
                        }
                    )
                }
            }
        }
    }
}
