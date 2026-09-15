package com.servicesync.app.ui.screens.customer

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servicesync.app.data.model.*
import com.servicesync.app.data.repository.ServiceSyncRepository
import com.servicesync.app.ui.components.*
import com.servicesync.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    repository: ServiceSyncRepository,
    onCategorySelected: (ServiceCategory) -> Unit,
    onCategorySelectedWithItem: (ServiceCategory, String) -> Unit = { cat, _ -> onCategorySelected(cat) },
    onProviderSelected: (ServiceProvider) -> Unit,
    onBookProvider: (ServiceProvider) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenBookings: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenFeedback: () -> Unit = {},
    onOpenAddresses: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onBookingSelected: (Booking) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val currentUser by repository.currentUser.collectAsState()
    val providers by repository.providers.collectAsState()
    val notifications by repository.notifications.collectAsState()
    val bookings by repository.bookings.collectAsState()
    val walletBalance by repository.walletBalance.collectAsState()
    val savedAddresses by repository.savedAddresses.collectAsState()
    val dismissedHomeIds by repository.dismissedHomeBookingIds.collectAsState()
    val dismissedRatingIds by repository.dismissedRatingBookingIds.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var showProfileDialog by remember { mutableStateOf(false) }

    val unreadNotifCount = notifications.count { !it.isRead }
    val activeBookingsCount = bookings.count {
        it.status == BookingStatus.PENDING || it.status == BookingStatus.ACCEPTED || it.status == BookingStatus.IN_PROGRESS
    }

    // 1. Completed service awaiting rating/feedback: appears on home screen if not filled in booking section and not cut/dismissed
    val completedUnratedBooking = bookings.firstOrNull { b ->
        b.status == BookingStatus.COMPLETED &&
        b.customerRating == null &&
        b.id !in dismissedRatingIds
    }

    // 2. Active ongoing booking (pending, accepted, in progress)
    val ongoingHomeBooking = bookings.firstOrNull { b ->
        b.id !in dismissedHomeIds && (
            b.status == BookingStatus.PENDING ||
            b.status == BookingStatus.ACCEPTED ||
            b.status == BookingStatus.IN_PROGRESS
        )
    }

    val filteredProviders = remember(providers, searchQuery, selectedFilter) {
        var list = providers
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.category.displayName.contains(searchQuery, ignoreCase = true) ||
                        it.skills.any { s -> s.contains(searchQuery, ignoreCase = true) }
            }
        }
        when (selectedFilter) {
            "Top Rated (4.9+)" -> list.filter { it.rating >= 4.9f }
            "Budget (< ₹400)" -> list.filter { it.hourlyRate <= 400.0 }
            "Available Now" -> list.filter { it.isAvailable }
            else -> list
        }.sortedByDescending { it.rating }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SurfaceLight,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                // Drawer Header with adaptive background for perfect contrast in both Light & Dark modes
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceVariantLight)
                        .padding(horizontal = 20.dp, vertical = 22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = currentUser?.name?.ifBlank { "Customer" } ?: "Customer",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = currentUser?.phone?.ifBlank { "+91 Registered User" } ?: "+91 User",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // Clean Verified Customer Badge in Header
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PrimaryBlue.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "VERIFIED",
                                    color = PrimaryBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Wallet Mini-Card in Drawer Header
                        Surface(
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onOpenWallet()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = PrimaryBlue.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "SaServe Wallet",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "₹${walletBalance.toInt()}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Items - My Profile first
                NavigationDrawerItem(
                    label = { Text("My Profile", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        showProfileDialog = true
                    },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PrimaryBlue) },
                    badge = {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryBlue.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Edit",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
                NavigationDrawerItem(
                    label = { Text("My Bookings", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onOpenBookings()
                    },
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryBlue) },
                    badge = {
                        if (activeBookingsCount > 0) {
                            Surface(
                                shape = CircleShape,
                                color = StatusAcceptedBg
                            ) {
                                Text(
                                    text = "$activeBookingsCount active",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusAccepted
                                )
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Payment & Wallet", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onOpenWallet()
                    },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PrimaryBlue) },
                    badge = {
                        Text(
                            text = "₹${walletBalance.toInt()}",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Manage Addresses", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onOpenAddresses()
                    },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryBlue) },
                    badge = {
                        if (savedAddresses.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = StatusAcceptedBg
                            ) {
                                Text(
                                    text = "${savedAddresses.size}",
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Help & Support", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onOpenHelp()
                    },
                    icon = { Icon(Icons.Default.SupportAgent, contentDescription = null, tint = PrimaryBlue) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Rate Us on Play Store", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        val appPackage = context.packageName
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackage")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackage")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(webIntent)
                        }
                    },
                    icon = { Icon(Icons.Default.StarRate, contentDescription = null, tint = Color(0xFFFFA000)) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Feedback & Suggestions", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onOpenFeedback()
                    },
                    icon = { Icon(Icons.Default.Feedback, contentDescription = null, tint = PrimaryBlue) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Settings", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onOpenSettings()
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = PrimaryBlue) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = CardBorder)

                // Contact info footer preview
                Column(
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 4.dp)
                ) {
                    Text("Direct Support Line", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("📞 7488274632", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text("✉️ sahaditya1804@gmail.com", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }

                Spacer(modifier = Modifier.height(16.dp))

                NavigationDrawerItem(
                    label = { Text("Logout", fontSize = 13.sp, color = StatusCancelled, fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onLogoutClick()
                    },
                    icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = StatusCancelled) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        // Top-left Navigation Menu Button (Hamburger)
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Navigation Menu",
                                tint = TextPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    },
                    title = {
                        Column {
                            // User Name in place of the logo
                            val customerGreeting = if (!currentUser?.name.isNullOrBlank()) {
                                "Hi, ${currentUser?.name}"
                            } else {
                                "Hi, Customer"
                            }
                            Text(
                                text = customerGreeting,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                maxLines = 1
                            )

                            // Current default address subtitle
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.clickable { onOpenAddresses() }
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = currentUser?.address?.ifBlank { "Add address" }?.take(22) ?: "Add address",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Switch address",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        // Top-Right Ring-Shaped Notification Bell (with +Pro removed as requested)
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceLight)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (unreadNotifCount > 0) PrimaryBlue else CardBorder,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsNone,
                                    contentDescription = "Notifications",
                                    tint = if (unreadNotifCount > 0) PrimaryBlue else TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )

                                if (unreadNotifCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 4.dp, end = 4.dp)
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(StatusCancelled)
                                            .border(1.dp, Color.White, CircleShape)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
                )
            },
            containerColor = BackgroundLight
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
            // Active / Recent Booking Notification Card (Dismissable via 'X' or once review submitted)
            // 1. Completed Service Rating & Feedback Form (Appears if not filled in booking section; if user cuts/dismisses it, it will not pop up again)
            if (completedUnratedBooking != null) {
                item {
                    HomeScreenCompletedRatingCard(
                        booking = completedUnratedBooking,
                        onDismiss = {
                            repository.dismissRatingForm(completedUnratedBooking.id)
                            Toast.makeText(context, "Rating dismissed", Toast.LENGTH_SHORT).show()
                        },
                        onSubmitRating = { rating, review ->
                            repository.addReviewForBooking(
                                bookingId = completedUnratedBooking.id,
                                rating = rating,
                                comment = review
                            )
                            Toast.makeText(context, "Thank you! Your feedback has been submitted.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // 2. Ongoing Active Booking Notification Card (Pending, Accepted, In Progress)
            if (ongoingHomeBooking != null) {
                item {
                    val isAccepted = ongoingHomeBooking.status == BookingStatus.ACCEPTED
                    val isInProgress = ongoingHomeBooking.status == BookingStatus.IN_PROGRESS

                    val cardBg = if (isAccepted || isInProgress) StatusAcceptedBg else StatusPendingBg
                    val primaryColor = if (isAccepted || isInProgress) StatusAccepted else StatusPending

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBookingSelected(ongoingHomeBooking) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isAccepted || isInProgress) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(26.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        isAccepted -> "🎉 ${ongoingHomeBooking.providerName} accepted your request!"
                                        isInProgress -> "⚡ Service in progress with ${ongoingHomeBooking.providerName}"
                                        else -> "Request Pending: ${ongoingHomeBooking.category.displayName}"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                                Text(
                                    text = "${ongoingHomeBooking.scheduledDate} • ${ongoingHomeBooking.scheduledSlot}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }

                            // Dismiss / Cut-out button (X) removes ongoing booking card from home
                            IconButton(
                                onClick = { repository.dismissBookingFromHome(ongoingHomeBooking.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss from Home",
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Dispersed Edge-to-Edge Human Clay Specialists Showcase Banner (No text on image or below, pure visual art)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.2.dp, CardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(205.dp)
                            .clip(RoundedCornerShape(22.dp))
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.servicesync.app.R.drawable.clay_specialists_team),
                            contentDescription = "SaServe Specialists Team",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }



            // Promotional Highlights & Security Carousel
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceLight,
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.4f)),
                            modifier = Modifier.width(280.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Shield, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                    Text("Two-OTP Security", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Text("SaServe Safety Protocol", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Text("Start & Completion OTPs ensure verified specialist and payment safety.", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceLight,
                            border = BorderStroke(1.dp, SecondaryTeal.copy(alpha = 0.4f)),
                            modifier = Modifier.width(280.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Bolt, null, tint = SecondaryTeal, modifier = Modifier.size(18.dp))
                                    Text("Express Dispatch", color = SecondaryTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Text("Specialists at Your Doorstep", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Text("Quick response for electricians, plumbers, and home appliance repairs.", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceLight,
                            border = BorderStroke(1.dp, StatusInProgress.copy(alpha = 0.4f)),
                            modifier = Modifier.width(280.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.VerifiedUser, null, tint = StatusInProgress, modifier = Modifier.size(18.dp))
                                    Text("Transparent INR Rates", color = StatusInProgress, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Text("Affordable ₹ Pricing", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Text("Clear hourly pricing in Indian Rupees with 100% verified specialists.", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Categories Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Explore Services",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "9 categories",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CategoryGridItem(
                            category = ServiceCategory.ELECTRICIAN,
                            modifier = Modifier.weight(1f),
                            onClick = { onCategorySelected(ServiceCategory.ELECTRICIAN) }
                        )
                        CategoryGridItem(
                            category = ServiceCategory.PLUMBER,
                            modifier = Modifier.weight(1f),
                            onClick = { onCategorySelected(ServiceCategory.PLUMBER) }
                        )
                        CategoryGridItem(
                            category = ServiceCategory.CARPENTER,
                            modifier = Modifier.weight(1f),
                            onClick = { onCategorySelected(ServiceCategory.CARPENTER) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CategoryGridItem(
                            category = ServiceCategory.MECHANIC,
                            modifier = Modifier.weight(1f),
                            onClick = { onCategorySelected(ServiceCategory.MECHANIC) }
                        )
                        CategoryGridItem(
                            category = ServiceCategory.APPLIANCE_REPAIR,
                            modifier = Modifier.weight(1f),
                            onClick = { onCategorySelected(ServiceCategory.APPLIANCE_REPAIR) }
                        )
                        CategoryGridItem(
                            category = ServiceCategory.PAINTER,
                            modifier = Modifier.weight(1f),
                            onClick = { onCategorySelected(ServiceCategory.PAINTER) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CategoryGridItem(
                            category = ServiceCategory.MASON,
                            modifier = Modifier.weight(1f),
                            onClick = { onCategorySelected(ServiceCategory.MASON) }
                        )
                        CategoryGridItem(
                            category = ServiceCategory.GARDENER,
                            modifier = Modifier.weight(1f),
                            onClick = { onCategorySelected(ServiceCategory.GARDENER) }
                        )
                        CategoryGridItem(
                            category = ServiceCategory.HOUSE_CLEANING,
                            modifier = Modifier.weight(1f),
                            onClick = { onCategorySelected(ServiceCategory.HOUSE_CLEANING) }
                        )
                    }
                }
            }

            // Most Requested Appliances & Quick Fixes (SaServe Assured)
            item {
                val commonFixes = remember {
                    listOf(
                        Pair(ServiceCategory.ELECTRICIAN, getSubServicesForCategory(ServiceCategory.ELECTRICIAN).first { it.id == "elec_fan" }),
                        Pair(ServiceCategory.PLUMBER, getSubServicesForCategory(ServiceCategory.PLUMBER).first { it.id == "plumb_tap" }),
                        Pair(ServiceCategory.APPLIANCE_REPAIR, getSubServicesForCategory(ServiceCategory.APPLIANCE_REPAIR).first { it.id == "app_wm" }),
                        Pair(ServiceCategory.CARPENTER, getSubServicesForCategory(ServiceCategory.CARPENTER).first { it.id == "carp_locks" }),
                        Pair(ServiceCategory.HOUSE_CLEANING, getSubServicesForCategory(ServiceCategory.HOUSE_CLEANING).first { it.id == "clean_deep_home" }),
                        Pair(ServiceCategory.APPLIANCE_REPAIR, getSubServicesForCategory(ServiceCategory.APPLIANCE_REPAIR).first { it.id == "app_fridge" }),
                        Pair(ServiceCategory.ELECTRICIAN, getSubServicesForCategory(ServiceCategory.ELECTRICIAN).first { it.id == "elec_switch" }),
                        Pair(ServiceCategory.GARDENER, getSubServicesForCategory(ServiceCategory.GARDENER).first { it.id == "garden_lawn" }),
                        Pair(ServiceCategory.MASON, getSubServicesForCategory(ServiceCategory.MASON).first { it.id == "mason_tile" }),
                        Pair(ServiceCategory.MECHANIC, getSubServicesForCategory(ServiceCategory.MECHANIC).first { it.id == "mech_bike" }),
                        Pair(ServiceCategory.PAINTER, getSubServicesForCategory(ServiceCategory.PAINTER).first { it.id == "paint_patch" }),
                        Pair(ServiceCategory.HOUSE_CLEANING, getSubServicesForCategory(ServiceCategory.HOUSE_CLEANING).first { it.id == "clean_kitchen" })
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Most Requested Fixes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = PrimaryBlue.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "POPULAR",
                                    color = PrimaryBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Quick 1-tap bookings for top everyday household repairs",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    commonFixes.chunked(2).forEach { pairRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pairRow.forEach { (cat, subItem) ->
                                CommonApplianceFixCard(
                                    category = cat,
                                    item = subItem,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        onCategorySelectedWithItem(cat, subItem.id)
                                    }
                                )
                            }
                            if (pairRow.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Profile Edit Dialog
    if (showProfileDialog) {
        var editName by remember(currentUser) { mutableStateOf(currentUser?.name ?: "") }
        var editPhone by remember(currentUser) { mutableStateOf(currentUser?.phone ?: "") }
        var editEmail by remember(currentUser) { mutableStateOf(currentUser?.email ?: "") }
        var editAddress by remember(currentUser) { mutableStateOf(currentUser?.address ?: "") }

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue)
                    Text("My Profile", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Mobile Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAddress,
                        onValueChange = { editAddress = it },
                        label = { Text("Service Address") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.updateUserProfile(editName, editPhone, editEmail, editAddress)
                        showProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    }
}

@Composable
fun CommonApplianceFixCard(
    category: ServiceCategory,
    item: SubServiceItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val gradientColors = remember(category) {
        when (category) {
            ServiceCategory.ELECTRICIAN -> listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF06B6D4))
            ServiceCategory.PLUMBER -> listOf(Color(0xFF0C4A6E), Color(0xFF0284C7), Color(0xFF38BDF8))
            ServiceCategory.CARPENTER -> listOf(Color(0xFF78350F), Color(0xFFB45309), Color(0xFFF59E0B))
            ServiceCategory.MECHANIC -> listOf(Color(0xFF881337), Color(0xFFE11D48), Color(0xFFFB7185))
            ServiceCategory.APPLIANCE_REPAIR -> listOf(Color(0xFF064E3B), Color(0xFF059669), Color(0xFF34D399))
            ServiceCategory.PAINTER -> listOf(Color(0xFF581C87), Color(0xFF9333EA), Color(0xFFC084FC))
            ServiceCategory.MASON -> listOf(Color(0xFF7C2D12), Color(0xFFC2410C), Color(0xFFFB923C))
            ServiceCategory.GARDENER -> listOf(Color(0xFF14532D), Color(0xFF16A34A), Color(0xFF4ADE80))
            ServiceCategory.HOUSE_CLEANING -> listOf(Color(0xFF0F766E), Color(0xFF0D9488), Color(0xFF2DD4BF))
            ServiceCategory.OTHER -> listOf(Color(0xFF334155), Color(0xFF475569), Color(0xFF64748B))
        }
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Visual Graphic Illustration Image Banner with Authentic 3D Clay Art
            val clayResId = com.servicesync.app.ui.components.getApplianceClayDrawable(item.id)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .background(SurfaceVariantLight),
                contentAlignment = Alignment.Center
            ) {
                // High-Resolution 3D Clay Appliance Image
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = clayResId),
                    contentDescription = item.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Category Tag Ribbon
                Surface(
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = category.displayName.uppercase(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Card Body Details
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    fontSize = 13.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                        Text(
                            text = item.rating.replace(" ★", ""),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = item.price,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PrimaryBlue.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onClick() }
                ) {
                    Text(
                        text = "Book Now ➔",
                        color = PrimaryBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CancellationReasonDialog(
    booking: Booking,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val reasons = listOf(
        "Booked by mistake / wrong service selected",
        "Specialist delayed / taking too long to arrive",
        "Issue resolved by myself / found alternative",
        "Change of plans / not available at home",
        "Price or scope of work higher than expected",
        "Emergency / need to reschedule for later",
        "Other reason"
    )
    var selectedReasonIndex by remember { mutableStateOf(0) }
    var customReason by remember { mutableStateOf("") }

    val isServiceStarted = booking.status == BookingStatus.IN_PROGRESS
    val lateFine = 99.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Cancel, null, tint = StatusCancelled, modifier = Modifier.size(24.dp))
                Text("Cancel Service Booking", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Please select a reason for cancelling #${if (booking.id.length >= 5) booking.id.takeLast(5) else booking.id} with ${booking.providerName}:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                // Late Cancellation Fine Warning Banner
                if (isServiceStarted) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StatusCancelledBg,
                        border = BorderStroke(1.dp, StatusCancelled.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Warning, null, tint = StatusCancelled, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Service Has Started (Late Cancellation)",
                                    fontWeight = FontWeight.Bold,
                                    color = StatusCancelled,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            Text(
                                text = "Because the specialist has already commenced work on site, a cancellation fee of ₹${lateFine.toInt()} will be added to your next service booking for technician mobilization.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StatusAcceptedBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = StatusAccepted, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Free cancellation: No charges will be applied.",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusAccepted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Reasons List
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    reasons.forEachIndexed { index, reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedReasonIndex = index }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedReasonIndex == index,
                                onClick = { selectedReasonIndex = index },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                fontWeight = if (selectedReasonIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (selectedReasonIndex == reasons.lastIndex) {
                    OutlinedTextField(
                        value = customReason,
                        onValueChange = { customReason = it },
                        label = { Text("Specify Reason") },
                        placeholder = { Text("Describe the issue...") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = if (selectedReasonIndex == reasons.lastIndex && customReason.isNotBlank()) {
                        customReason.trim()
                    } else {
                        reasons[selectedReasonIndex]
                    }
                    onConfirm(finalReason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusCancelled)
            ) {
                Text("Confirm Cancellation", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Booking")
            }
        }
    )
}

@Composable
fun CategoryGridItem(
    category: ServiceCategory,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(category),
                    contentDescription = category.displayName,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderListScreen(
    category: ServiceCategory,
    repository: ServiceSyncRepository,
    onBackClick: () -> Unit,
    onProviderSelected: (ServiceProvider) -> Unit,
    onBookProvider: (ServiceProvider) -> Unit
) {
    val providers by repository.providers.collectAsState()
    var sortBy by remember { mutableStateOf("Rating") }

    val categoryProviders = remember(providers, category, sortBy) {
        val filtered = providers.filter { it.category == category }
        when (sortBy) {
            "Rating" -> filtered.sortedByDescending { it.rating }
            "Experience" -> filtered.sortedByDescending { it.experienceYears }
            "Price" -> filtered.sortedBy { it.hourlyRate }
            else -> filtered
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = category.displayName + "s",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${categoryProviders.size} registered specialists",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
        ) {
            if (categoryProviders.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Sort by:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    listOf("Rating", "Experience", "Price").forEach { option ->
                        FilterChip(
                            selected = sortBy == option,
                            onClick = { sortBy = option },
                            label = { Text(option) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(categoryProviders) { provider ->
                        ProviderCard(
                            provider = provider,
                            onBookClick = { onBookProvider(provider) },
                            onViewDetail = { onProviderSelected(provider) }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(StatusAcceptedBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(category),
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "No ${category.displayName}s Available",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "There are currently no specialists available for this service in your area. Please check back shortly!",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDetailScreen(
    provider: ServiceProvider,
    repository: ServiceSyncRepository,
    onBackClick: () -> Unit,
    onBookingConfirmed: (Booking) -> Unit
) {
    val currentUser by repository.currentUser.collectAsState()

    val availableDates = listOf(
        "Today, Sep 5",
        "Tomorrow, Sep 6",
        "Sunday, Sep 7",
        "Monday, Sep 8"
    )
    var selectedDate by remember { mutableStateOf(availableDates[1]) }
    var selectedSlot by remember { mutableStateOf(provider.availableSlots.firstOrNull()?.timeRange ?: "11:00 AM - 01:00 PM") }
    var issueText by remember { mutableStateOf("") }
    var addressText by remember { mutableStateOf(currentUser?.address ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Provider Profile & Booking") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Estimated Cost",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "₹${provider.hourlyRate.toInt()} / hr",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }

                    Button(
                        onClick = {
                            val booking = repository.createBooking(
                                provider = provider,
                                date = selectedDate,
                                timeSlot = selectedSlot,
                                address = addressText.ifBlank { "Customer Location (Home)" },
                                issueDescription = issueText.ifBlank { "General maintenance and inspection" }
                            )
                            onBookingConfirmed(booking)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirm & Book Slot", fontWeight = FontWeight.Bold)
                    }
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = provider.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.joinToString(""),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = provider.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (provider.isVerified) {
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = "Verified",
                                            tint = AccentSky,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "${provider.category.displayName} • ${provider.experienceYears} Years Experience",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )

                                RatingDisplay(rating = provider.rating, reviewCount = provider.reviewCount)
                            }
                        }

                        Divider(color = CardBorder)

                        Text(
                            text = "About",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = provider.bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        Text(
                            text = "Specialties",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            provider.skills.forEach { skill ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StatusAcceptedBg
                                ) {
                                    Text(
                                        text = skill,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrimaryBlue,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Date Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "1. Select Service Date",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(availableDates) { date ->
                                val isSelected = selectedDate == date
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedDate = date },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) PrimaryBlue else SurfaceVariantLight,
                                    border = if (isSelected) null else BorderStroke(1.dp, CardBorder)
                                ) {
                                    Text(
                                        text = date,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Time Slot Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "2. Select Available Time Slot",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            provider.availableSlots.forEach { slot ->
                                val isSelected = selectedSlot == slot.timeRange
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedSlot = slot.timeRange },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) StatusAcceptedBg else SurfaceVariantLight,
                                    border = if (isSelected) BorderStroke(1.5.dp, PrimaryBlue) else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = if (isSelected) PrimaryBlue else TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = slot.timeRange,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) PrimaryBlue else TextPrimary
                                            )
                                        }

                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = PrimaryBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Booking Details Form
            item {
                val savedAddresses by repository.savedAddresses.collectAsState()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "3. Precise Service Location & Instructions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Saved Addresses Selector Chips
                        if (savedAddresses.isNotEmpty()) {
                            Text(
                                text = "Select from Saved Addresses:",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(savedAddresses) { addr ->
                                    val isSelected = addressText == addr.formattedDisplayAddress || addressText == addr.addressLine
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            addressText = addr.formattedDisplayAddress
                                        },
                                        label = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    when (addr.label) {
                                                        "Home" -> Icons.Default.Home
                                                        "Office" -> Icons.Default.Work
                                                        else -> Icons.Default.LocationOn
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text("${addr.label}: ${(addr.flatHouseNo.takeIf { it.isNotBlank() } ?: addr.addressLine).take(18)}...")
                                            }
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = StatusAcceptedBg,
                                            selectedLabelColor = PrimaryBlue
                                        )
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = addressText,
                            onValueChange = { addressText = it },
                            label = { Text("Service Location (Flat, Street, Landmark, City)") },
                            placeholder = { Text("e.g. Flat 304, Tower B, 14th Main, Near Apollo Pharmacy") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3
                        )

                        OutlinedTextField(
                            value = issueText,
                            onValueChange = { issueText = it },
                            label = { Text("Describe the Issue / Job Requirements") },
                            placeholder = { Text("e.g. Water leak under kitchen sink, switchboard sparked...") },
                            leadingIcon = { Icon(Icons.Default.Description, null) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 4
                        )

                        if (validationError != null) {
                            Text(
                                text = validationError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Customer Reviews Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Customer Reviews (${provider.reviews.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        provider.reviews.forEach { review ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = review.author,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    RatingDisplay(rating = review.rating)
                                }
                                Text(
                                    text = review.comment,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = review.date,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                Divider(color = CardBorder, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerBookingsScreen(
    repository: ServiceSyncRepository,
    onBackClick: () -> Unit,
    onBookingClick: (Booking) -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser by repository.currentUser.collectAsState()
    val allBookings by repository.bookings.collectAsState()

    // Filter strictly by current logged-in customer account
    val bookings = remember(allBookings, currentUser) {
        val uid = currentUser?.id ?: "cust_user"
        allBookings.filter { it.customerId == uid }
    }

    var selectedFilter by remember { mutableStateOf("All") }
    var bookingToCancel by remember { mutableStateOf<Booking?>(null) }

    val filteredBookings = remember(bookings, selectedFilter) {
        when (selectedFilter) {
            "Pending" -> bookings.filter { it.status == BookingStatus.PENDING }
            "Accepted" -> bookings.filter { it.status == BookingStatus.ACCEPTED || it.status == BookingStatus.IN_PROGRESS }
            "Completed" -> bookings.filter { it.status == BookingStatus.COMPLETED }
            "Cancelled" -> bookings.filter { it.status == BookingStatus.CANCELLED }
            else -> bookings
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Service Bookings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
        ) {
            // Responsive scrollable filter tabs (prevents "Completed" from clipping on any screen)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filterList = listOf("All", "Pending", "Accepted", "Completed", "Cancelled")
                items(filterList) { statusLabel ->
                    val isSelected = selectedFilter == statusLabel
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = statusLabel },
                        label = {
                            Text(
                                text = statusLabel,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (filteredBookings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = "No bookings found in this category.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredBookings) { booking ->
                        BookingItemCard(
                            booking = booking,
                            onBookingClick = { onBookingClick(booking) },
                            onAcceptClick = {
                                repository.acceptBooking(booking.id)
                            },
                            onCancelClick = {
                                bookingToCancel = booking
                            }
                        )
                    }
                }
            }
        }
    }

    if (bookingToCancel != null) {
        val target = bookingToCancel!!
        CancellationReasonDialog(
            booking = target,
            onDismiss = { bookingToCancel = null },
            onConfirm = { reason ->
                val (_, feeApplied) = repository.cancelBookingWithDetails(target.id, reason)
                bookingToCancel = null
                val msg = if (feeApplied > 0.0) {
                    "Booking cancelled. A ₹${feeApplied.toInt()} late fee was recorded for your next booking."
                } else {
                    "Booking successfully cancelled."
                }
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        )
    }
}

@Composable
fun BookingItemCard(
    booking: Booking,
    onBookingClick: () -> Unit = {},
    onAcceptClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBookingClick() },
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = getCategoryIcon(booking.category),
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = booking.providerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                StatusBadge(status = booking.status)
            }

            // Notification note if Accepted
            if (booking.status == BookingStatus.ACCEPTED) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusAcceptedBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = StatusAccepted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Provider accepted your request! Service confirmed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusAccepted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // SaServe Two-OTP Badge preview (Locked when PENDING)
            if (booking.status == BookingStatus.PENDING) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StatusPendingBg,
                    border = BorderStroke(1.dp, StatusPending.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Lock, null, tint = StatusPending, modifier = Modifier.size(16.dp))
                            Text(
                                text = "OTPs unlock after acceptance",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusPending,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = onBookingClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Track", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            } else if (booking.status == BookingStatus.ACCEPTED) {
                // ACCEPTED: ONLY Start OTP is visible
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceVariantLight,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "START SERVICE OTP",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue,
                                letterSpacing = 0.5.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = booking.startOtp.ifBlank { "------" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryBlue,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "• Share on arrival",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = onBookingClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Track", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            } else if (booking.status == BookingStatus.IN_PROGRESS) {
                // IN_PROGRESS: ONLY Completion OTP is visible
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceVariantLight,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "COMPLETION OTP (Work in Progress)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusCompleted,
                                letterSpacing = 0.5.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = booking.completionOtp.ifBlank { "------" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StatusCompleted,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "• Share to finish job",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = onBookingClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusCompleted),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Track", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            } else if (booking.status == BookingStatus.COMPLETED) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StatusCompletedBg,
                    border = BorderStroke(1.dp, StatusCompleted.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = StatusCompleted, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Service Successfully Completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusCompleted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = onBookingClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusCompleted),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Invoice", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (booking.status == BookingStatus.CANCELLED) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusCancelledBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Cancel, null, tint = StatusCancelled, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (booking.cancellationFee > 0)
                                "Cancelled: ${booking.cancellationReason ?: "User requested"} (Late fine: ₹${booking.cancellationFee.toInt()})"
                            else
                                "Cancelled: ${booking.cancellationReason ?: "User requested"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusCancelled,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Divider(color = CardBorder)

            // Booking details
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Event, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${booking.scheduledDate} • ${booking.scheduledSlot}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Text(
                        text = booking.customerAddress.ifBlank { "Customer Location" },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Description, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Issue: ${booking.issueDescription.ifBlank { "General maintenance" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rate: ₹${booking.hourlyRate.toInt()}/hr",
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "ID: #${if (booking.id.length >= 5) booking.id.takeLast(5) else booking.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            // Action Buttons (Cancel button available on all active bookings)
            val isActiveBooking = booking.status == BookingStatus.PENDING ||
                    booking.status == BookingStatus.ACCEPTED ||
                    booking.status == BookingStatus.IN_PROGRESS

            if (isActiveBooking) {
                Divider(color = CardBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCancelClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCancelled),
                        border = BorderStroke(1.dp, StatusCancelled.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Cancel, null, modifier = Modifier.size(14.dp), tint = StatusCancelled)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StatusCancelled)
                    }

                    if (booking.status == BookingStatus.PENDING) {
                        Button(
                            onClick = onAcceptClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusAccepted),
                            modifier = Modifier.weight(1.8f)
                        ) {
                            Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simulate Acceptance", fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = onBookingClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier.weight(1.4f)
                        ) {
                            Icon(Icons.Default.Navigation, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Live Track", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    repository: ServiceSyncRepository,
    onBackClick: () -> Unit,
    onNotificationClick: (AppNotification) -> Unit
) {
    val notifications by repository.notifications.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        TextButton(onClick = { repository.clearAllNotifications() }) {
                            Text("Clear All")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        if (notifications.isEmpty()) {
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
                        Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(54.dp)
                    )
                    Text("No notifications yet", color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications) { notif ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                repository.markNotificationRead(notif.id)
                                onNotificationClick(notif)
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!notif.isRead) StatusAcceptedBg.copy(alpha = 0.5f) else SurfaceLight
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (notif.title.contains("Accepted")) Icons.Default.CheckCircle else Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = notif.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = notif.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            if (!notif.isRead) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBlue)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    repository: ServiceSyncRepository,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val walletBalance by repository.walletBalance.collectAsState()
    val transactions by repository.walletTransactions.collectAsState()

    var customAmountText by remember { mutableStateOf("") }
    var selectedUpiApp by remember { mutableStateOf("Google Pay") }
    var upiIdInput by remember { mutableStateOf("") }
    var showDepositDialog by remember { mutableStateOf(false) }
    var showSuccessSnackbar by remember { mutableStateOf(false) }
    var lastAddedAmount by remember { mutableStateOf(0.0) }

    val quickAmounts = listOf(200, 500, 1000, 2000)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SaServe Wallet & UPI") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
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
            // Balance Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.5.dp, PrimaryBlue.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AVAILABLE BALANCE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PrimaryBlue.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "UPI Verified",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrimaryBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = "₹${String.format(java.util.Locale.US, "%,.2f", walletBalance)}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryBlue
                        )

                        Text(
                            text = "Use wallet balance for instant booking confirmations with 0% extra gateway charges.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Quick Add Money Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Add Money to Wallet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Quick Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickAmounts.forEach { amt ->
                                FilterChip(
                                    selected = customAmountText == amt.toString(),
                                    onClick = { customAmountText = amt.toString() },
                                    label = { Text("+ ₹$amt", fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StatusAcceptedBg,
                                        selectedLabelColor = PrimaryBlue
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Custom Amount Field
                        OutlinedTextField(
                            value = customAmountText,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) customAmountText = input
                            },
                            label = { Text("Enter Amount (₹)") },
                            placeholder = { Text("e.g. 500") },
                            leadingIcon = {
                                Text("₹", fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 18.sp)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // UPI Provider Selector
                        Text(
                            text = "Pay via Preferred UPI App:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Google Pay", "PhonePe", "Paytm", "BHIM").forEach { appName ->
                                FilterChip(
                                    selected = selectedUpiApp == appName,
                                    onClick = { selectedUpiApp = appName },
                                    label = { Text(appName, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        // Optional UPI ID / VPA
                        OutlinedTextField(
                            value = upiIdInput,
                            onValueChange = { upiIdInput = it },
                            label = { Text("UPI ID (Optional, e.g. user@okaxis)") },
                            placeholder = { Text("user@okhdfcbank") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                val amt = customAmountText.toDoubleOrNull() ?: 0.0
                                if (amt > 0) {
                                    repository.addMoneyToWallet(
                                        amount = amt,
                                        upiApp = selectedUpiApp,
                                        upiId = upiIdInput
                                    )
                                    lastAddedAmount = amt
                                    customAmountText = ""
                                    upiIdInput = ""
                                    showSuccessSnackbar = true
                                    Toast.makeText(context, "₹${amt.toInt()} added to SaServe Wallet via $selectedUpiApp!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Proceed with $selectedUpiApp", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            // Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaction History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${transactions.size} records",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Transactions List
            if (transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No transactions yet.", color = TextSecondary)
                    }
                }
            } else {
                items(transactions) { tx ->
                    val isDeposit = tx.type == "DEPOSIT"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isDeposit) StatusAcceptedBg else StatusCancelledBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDeposit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isDeposit) StatusAccepted else StatusCancelled,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Ref: ${tx.upiRefId} • ${tx.timestamp}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }

                            Text(
                                text = "${if (isDeposit) "+" else "-"}₹${tx.amount.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDeposit) StatusAccepted else StatusCancelled
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var contactQuerySubject by remember { mutableStateOf("") }
    var contactQueryMessage by remember { mutableStateOf("") }

    val supportPhone = "7488274632"
    val supportEmail = "sahaditya1804@gmail.com"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & Customer Support") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
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
            // Hero Help Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.5.dp, PrimaryBlue.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = "How can we assist you today?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = "Our SaServe support team is ready to assist you with booking queries, service guarantee, OTP issues, or provider questions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Direct Contact Cards (Call & Email)
            item {
                Text(
                    text = "Direct Contact Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Phone Contact Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(StatusAcceptedBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = StatusAccepted,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Call Support Directly",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = supportPhone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Available 8:00 AM - 10:00 PM (Daily)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$supportPhone")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusAccepted),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Call", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Email Contact Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(StatusAcceptedBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Email Support Team",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = supportEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Fast response within 2 hours",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        Button(
                            onClick = {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:$supportEmail")
                                    putExtra(Intent.EXTRA_SUBJECT, "SaServe Customer Support Request")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(emailIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Email: $supportEmail", Toast.LENGTH_LONG).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Email", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Query Form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Send Message to SaServe Helpdesk",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = contactQuerySubject,
                            onValueChange = { contactQuerySubject = it },
                            label = { Text("Subject / Issue Type") },
                            placeholder = { Text("e.g. Booking rescheduled, OTP issue, payment query") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = contactQueryMessage,
                            onValueChange = { contactQueryMessage = it },
                            label = { Text("Describe your query") },
                            placeholder = { Text("Provide details so we can assist you quickly...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = {
                                if (contactQueryMessage.isNotBlank()) {
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:$supportEmail")
                                        putExtra(Intent.EXTRA_SUBJECT, contactQuerySubject.ifBlank { "Support Ticket from Customer" })
                                        putExtra(Intent.EXTRA_TEXT, contactQueryMessage)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try {
                                        context.startActivity(emailIntent)
                                        contactQuerySubject = ""
                                        contactQueryMessage = ""
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Please email: $supportEmail", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Please write a message first", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send Help Message", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Frequently Asked Questions
            item {
                Text(
                    text = "Frequently Asked Questions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            val faqs = listOf(
                "How do Two-Factor OTPs work?" to "When the service pro arrives, give them the Start OTP to begin the job. Once work is completed to your satisfaction, share the Completion OTP so the pro can finish the task.",
                "How do I add money using UPI?" to "Open 'Payment & Wallet' from the menu, pick an amount or enter custom rupees, select your UPI app (Google Pay, PhonePe, Paytm, or BHIM), and proceed.",
                "Can I cancel or reschedule a booking?" to "Yes, navigate to 'My Bookings' from the top-left menu and select your booking. Pending bookings can be canceled at any time free of charge.",
                "What if the service specialist does not arrive?" to "Call our emergency helpline directly at 7488274632 or write to sahaditya1804@gmail.com. We will reassign a verified specialist immediately."
            )

            items(faqs) { (question, answer) ->
                var expanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = question,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = PrimaryBlue
                            )
                        }
                        if (expanded) {
                            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = answer,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAddressesScreen(
    repository: ServiceSyncRepository,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val savedAddresses by repository.savedAddresses.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingAddressId by remember { mutableStateOf<String?>(null) }
    var labelInput by remember { mutableStateOf("Home") }
    var addressInput by remember { mutableStateOf("") }
    var flatHouseInput by remember { mutableStateOf("") }
    var streetAreaInput by remember { mutableStateOf("") }
    var landmarkInput by remember { mutableStateOf("") }
    var instructionsInput by remember { mutableStateOf("") }
    var isDefaultInput by remember { mutableStateOf(false) }
    var isDetectingGps by remember { mutableStateOf(false) }

    // GPS Location Launcher
    val locationLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isDetectingGps = true
            detectCurrentGpsLocation(context) { detected ->
                isDetectingGps = false
                if (!detected.isNullOrBlank()) {
                    streetAreaInput = detected
                    addressInput = detected
                    Toast.makeText(context, "Location detected via GPS!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Could not detect GPS location. Please check location settings.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Location permission is required to detect GPS location.", Toast.LENGTH_SHORT).show()
        }
    }

    if (showAddDialog) {
        val isEditing = editingAddressId != null
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingAddressId = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (isEditing) Icons.Default.EditLocationAlt else Icons.Default.AddLocationAlt,
                        contentDescription = null,
                        tint = PrimaryBlue
                    )
                    Text(
                        if (isEditing) "Edit Service Address" else "Add Precise Service Address",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("1. Address Tag", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Home", "Office", "Other").forEach { tag ->
                            FilterChip(
                                selected = labelInput == tag,
                                onClick = { labelInput = tag },
                                label = { Text(tag) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // GPS Auto-detect Button
                    OutlinedButton(
                        onClick = {
                            locationLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isDetectingGps) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Detecting GPS...")
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📍 Auto-detect GPS Street / Area", color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Text("2. Precise Location Details", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)

                    // Flat / House / Building
                    OutlinedTextField(
                        value = flatHouseInput,
                        onValueChange = { flatHouseInput = it },
                        label = { Text("House / Flat / Floor / Building No.") },
                        placeholder = { Text("e.g. Flat 304, Tower B, Palm Meadows") },
                        leadingIcon = { Icon(Icons.Default.Apartment, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Street / Area / Sector
                    OutlinedTextField(
                        value = streetAreaInput,
                        onValueChange = { streetAreaInput = it; addressInput = it },
                        label = { Text("Street / Road / Area / Sector") },
                        placeholder = { Text("e.g. 14th Main Rd, Indiranagar, 2nd Stage") },
                        leadingIcon = { Icon(Icons.Default.Signpost, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Landmark
                    OutlinedTextField(
                        value = landmarkInput,
                        onValueChange = { landmarkInput = it },
                        label = { Text("Nearby Landmark (Optional)") },
                        placeholder = { Text("e.g. Behind Apollo Pharmacy, opposite Metro Pillar 42") },
                        leadingIcon = { Icon(Icons.Default.NearMe, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Instructions to Reach Location
                    OutlinedTextField(
                        value = instructionsInput,
                        onValueChange = { instructionsInput = it },
                        label = { Text("Instructions to Reach Location (Optional)") },
                        placeholder = { Text("e.g. Ring doorbell #3, take lift to 3rd floor, gate code 1234") },
                        leadingIcon = { Icon(Icons.Default.Directions, null) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = isDefaultInput,
                            onCheckedChange = { isDefaultInput = it }
                        )
                        Text("Set as default service address", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val baseLine = if (streetAreaInput.isNotBlank()) streetAreaInput else addressInput
                        if (baseLine.isNotBlank() || flatHouseInput.isNotBlank()) {
                            if (isEditing && editingAddressId != null) {
                                repository.updateSavedAddress(
                                    id = editingAddressId!!,
                                    label = labelInput,
                                    addressLine = baseLine,
                                    flatHouseNo = flatHouseInput,
                                    streetArea = streetAreaInput,
                                    landmark = landmarkInput,
                                    reachInstructions = instructionsInput,
                                    isDefault = isDefaultInput
                                )
                                Toast.makeText(context, "Address updated successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                repository.addSavedAddress(
                                    label = labelInput,
                                    addressLine = baseLine,
                                    flatHouseNo = flatHouseInput,
                                    streetArea = streetAreaInput,
                                    landmark = landmarkInput,
                                    reachInstructions = instructionsInput,
                                    isDefault = isDefaultInput
                                )
                                Toast.makeText(context, "Precise address added successfully!", Toast.LENGTH_SHORT).show()
                            }
                            showAddDialog = false
                            editingAddressId = null
                            addressInput = ""
                            flatHouseInput = ""
                            streetAreaInput = ""
                            landmarkInput = ""
                            instructionsInput = ""
                            isDefaultInput = false
                        } else {
                            Toast.makeText(context, "Please enter at least house/flat or street details", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(if (isEditing) "Update Address" else "Save Address")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    editingAddressId = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Saved Addresses") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingAddressId = null
                    labelInput = "Home"
                    addressInput = ""
                    flatHouseInput = ""
                    streetAreaInput = ""
                    landmarkInput = ""
                    instructionsInput = ""
                    isDefaultInput = savedAddresses.isEmpty()
                    showAddDialog = true
                },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Precise Address", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        if (savedAddresses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        text = "No saved addresses yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add your home, office, or other locations with flat number, street, landmark, and directions so specialists reach without confusion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = {
                            editingAddressId = null
                            labelInput = "Home"
                            addressInput = ""
                            flatHouseInput = ""
                            streetAreaInput = ""
                            landmarkInput = ""
                            instructionsInput = ""
                            isDefaultInput = true
                            showAddDialog = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.AddLocationAlt, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add First Address")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Your Saved Locations (${savedAddresses.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(savedAddresses) { addr ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        border = if (addr.isDefault) BorderStroke(1.5.dp, PrimaryBlue) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (addr.isDefault) StatusAcceptedBg else SurfaceVariantLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (addr.label) {
                                        "Home" -> Icons.Default.Home
                                        "Office" -> Icons.Default.Work
                                        else -> Icons.Default.LocationOn
                                    },
                                    contentDescription = null,
                                    tint = if (addr.isDefault) PrimaryBlue else TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = addr.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (addr.isDefault) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = StatusAcceptedBg
                                        ) {
                                            Text(
                                                text = "DEFAULT",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryBlue,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }

                                // Flat & Street
                                if (addr.flatHouseNo.isNotBlank() || addr.streetArea.isNotBlank()) {
                                    Text(
                                        text = listOfNotNull(addr.flatHouseNo.takeIf { it.isNotBlank() }, addr.streetArea.takeIf { it.isNotBlank() }).joinToString(", "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }

                                // Full address line or city
                                Text(
                                    text = addr.addressLine,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )

                                // Landmark
                                if (addr.landmark.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Icon(Icons.Default.NearMe, null, tint = AccentSky, modifier = Modifier.size(13.dp))
                                        Text(
                                            text = "Near ${addr.landmark}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AccentSky,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Reach instructions
                                if (addr.reachInstructions.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Directions, null, tint = TextMuted, modifier = Modifier.size(13.dp))
                                        Text(
                                            text = "Instructions: ${addr.reachInstructions}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    if (!addr.isDefault) {
                                        TextButton(
                                            onClick = { repository.setDefaultAddress(addr.id) },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Set as Default", fontSize = 12.sp, color = PrimaryBlue)
                                        }
                                    }
                                    TextButton(
                                        onClick = {
                                            editingAddressId = addr.id
                                            labelInput = addr.label
                                            flatHouseInput = addr.flatHouseNo
                                            streetAreaInput = addr.streetArea
                                            landmarkInput = addr.landmark
                                            instructionsInput = addr.reachInstructions
                                            addressInput = addr.addressLine
                                            isDefaultInput = addr.isDefault
                                            showAddDialog = true
                                        },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp), tint = PrimaryBlue)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Edit", fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            IconButton(onClick = { repository.deleteSavedAddress(addr.id) }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Address",
                                    tint = StatusCancelled,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper to retrieve device GPS coordinates and reverse geocode to a human-readable street address.
 */
fun detectCurrentGpsLocation(context: android.content.Context, onResult: (String?) -> Unit) {
    try {
        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
        if (locationManager == null) {
            onResult(null)
            return
        }

        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            onResult(null)
            return
        }

        // Check GPS or Network last known location
        val location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            ?: locationManager.getLastKnownLocation(android.location.LocationManager.PASSIVE_PROVIDER)

        if (location != null) {
            try {
                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val feature = addr.featureName
                    val subLocality = addr.subLocality ?: addr.locality
                    val city = addr.adminArea ?: addr.subAdminArea
                    val postal = addr.postalCode
                    val fullAddress = listOfNotNull(feature, subLocality, city, postal)
                        .filter { it.isNotBlank() }
                        .joinToString(", ")
                    onResult(if (fullAddress.isNotBlank()) fullAddress else "Lat: ${location.latitude}, Lng: ${location.longitude}")
                } else {
                    onResult("GPS Coordinates: ${String.format(java.util.Locale.US, "%.4f, %.4f", location.latitude, location.longitude)}")
                }
            } catch (e: Exception) {
                onResult("GPS Location: ${String.format(java.util.Locale.US, "%.4f, %.4f", location.latitude, location.longitude)}")
            }
        } else {
            onResult("Current GPS Location (Simulated City Center, Karnataka)")
        }
    } catch (e: Exception) {
        onResult("Current GPS Location (Location Provider)")
    }
}

@Composable
fun HomeScreenCompletedRatingCard(
    booking: Booking,
    onDismiss: () -> Unit,
    onSubmitRating: (Float, String) -> Unit
) {
    var selectedStars by remember(booking.id) { mutableStateOf(5f) }
    var reviewComment by remember(booking.id) { mutableStateOf("") }
    var selectedQuickTags by remember(booking.id) { mutableStateOf(setOf<String>()) }

    val positiveTags = listOf("Punctual ⏱️", "Clean Work 🧼", "Polite Specialist 🤝", "Expert Skills 🛠️", "Fair Price 💰")
    val constructiveTags = listOf("Delayed Arrival ⏳", "Unprofessional ⚠️", "Messy Work ❌", "Pricing Dispute 💸", "Incomplete ⚠️")
    val activeTags = if (selectedStars >= 4f) positiveTags else constructiveTags

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.5.dp, PrimaryBlue.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Specialist Info, Completion Badge & Cut (X) button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(StatusCompletedBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = null,
                            tint = StatusCompleted,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Service Completed! 🎉",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = StatusCompletedBg
                            ) {
                                Text(
                                    text = "RATE SERVICE",
                                    color = StatusCompleted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "How was ${booking.providerName} (${booking.category.displayName})?",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }

                // The Cut / Dismiss (X) button: permanently removes this rating form from home screen
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantLight)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cut rating form",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

            // Star Rating Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { selectedStars = star.toFloat() },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = if (star <= selectedStars) Icons.Default.Star else Icons.Default.StarOutline,
                                contentDescription = "$star Stars",
                                tint = if (star <= selectedStars) StarGold else TextMuted,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                val starLabel = when (selectedStars.toInt()) {
                    1 -> "😞 1 / 5 - Poor Experience"
                    2 -> "😐 2 / 5 - Fair Experience"
                    3 -> "🙂 3 / 5 - Good Service"
                    4 -> "😊 4 / 5 - Very Good Service"
                    else -> "🤩 5 / 5 - Excellent Service"
                }
                Text(
                    text = starLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedStars >= 4f) PrimaryBlue else StatusPending
                )
            }

            // Quick Feedback Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(activeTags) { tag ->
                    val isTagSelected = tag in selectedQuickTags
                    Surface(
                        onClick = {
                            selectedQuickTags = if (isTagSelected) {
                                selectedQuickTags - tag
                            } else {
                                selectedQuickTags + tag
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isTagSelected) PrimaryBlue.copy(alpha = 0.15f) else SurfaceVariantLight,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isTagSelected) PrimaryBlue else CardBorder
                        )
                    ) {
                        Text(
                            text = tag,
                            fontSize = 11.sp,
                            fontWeight = if (isTagSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isTagSelected) PrimaryBlue else TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Optional Feedback / Review Text Field
            OutlinedTextField(
                value = reviewComment,
                onValueChange = { reviewComment = it },
                placeholder = {
                    Text(
                        text = "Add feedback or review for ${booking.providerName} (optional)...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Action Buttons: "Not Now" (cuts form without filling) & "Submit Rating"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Text(
                        text = "Not Now",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = {
                        val tagsPart = if (selectedQuickTags.isNotEmpty()) selectedQuickTags.joinToString(", ") else ""
                        val combinedReview = when {
                            reviewComment.isNotBlank() && tagsPart.isNotBlank() -> "$tagsPart - $reviewComment"
                            reviewComment.isNotBlank() -> reviewComment
                            tagsPart.isNotBlank() -> tagsPart
                            else -> "Service completed with ${booking.providerName}. Rated ${selectedStars.toInt()}/5 stars."
                        }
                        onSubmitRating(selectedStars, combinedReview)
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Submit Rating",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}



