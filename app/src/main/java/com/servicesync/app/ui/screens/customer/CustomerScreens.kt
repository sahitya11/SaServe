package com.servicesync.app.ui.screens.customer

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servicesync.app.data.model.*
import com.servicesync.app.data.repository.ServiceSyncRepository
import com.servicesync.app.ui.components.*
import com.servicesync.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    repository: ServiceSyncRepository,
    onCategorySelected: (ServiceCategory) -> Unit,
    onProviderSelected: (ServiceProvider) -> Unit,
    onBookProvider: (ServiceProvider) -> Unit,
    onOpenNotifications: () -> Unit,
    onAddProviderClick: () -> Unit,
    onLogoutClick: () -> Unit = {}
) {
    val currentUser by repository.currentUser.collectAsState()
    val providers by repository.providers.collectAsState()
    val notifications by repository.notifications.collectAsState()
    val bookings by repository.bookings.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val unreadNotifCount = notifications.count { !it.isRead }
    val activeBookingsCount = bookings.count {
        it.status == BookingStatus.PENDING || it.status == BookingStatus.ACCEPTED || it.status == BookingStatus.IN_PROGRESS
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = currentUser?.address?.ifBlank { "Current Location" } ?: "Current Location",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = "Hi, ${currentUser?.name ?: "Customer"} 👋",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    // Add Provider Button in Top Bar
                    FilledTonalButton(
                        onClick = onAddProviderClick,
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = StatusAcceptedBg,
                            contentColor = PrimaryBlue
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Provider",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Pro", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    // Notification Bell with Badge
                    IconButton(onClick = onOpenNotifications) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifCount > 0) {
                                    Badge(containerColor = StatusCancelled) {
                                        Text("$unreadNotifCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = TextPrimary
                            )
                        }
                    }

                    // Logout Button
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = TextSecondary
                        )
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
            // Active Bookings Alert Banner
            if (activeBookingsCount > 0) {
                item {
                    val latestActive = bookings.firstOrNull {
                        it.status == BookingStatus.PENDING || it.status == BookingStatus.ACCEPTED
                    }
                    if (latestActive != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (latestActive.status == BookingStatus.ACCEPTED) StatusAcceptedBg else StatusPendingBg
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (latestActive.status == BookingStatus.ACCEPTED) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (latestActive.status == BookingStatus.ACCEPTED) StatusAccepted else StatusPending,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (latestActive.status == BookingStatus.ACCEPTED)
                                            "🎉 ${latestActive.providerName} accepted your request!"
                                        else "Request Pending: ${latestActive.category.displayName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (latestActive.status == BookingStatus.ACCEPTED) StatusAccepted else StatusPending
                                    )
                                    Text(
                                        text = "${latestActive.scheduledDate} • ${latestActive.scheduledSlot}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                                StatusBadge(status = latestActive.status)
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search electrician, plumber, mechanic...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceLight,
                        unfocusedContainerColor = SurfaceLight,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = CardBorder
                    ),
                    singleLine = true
                )
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
                            color = PrimaryBlue,
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
                                    Icon(Icons.Default.Shield, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Text("Two-OTP Security", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Text("Urban Safety Protocol", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Text("Start & Completion OTPs ensure verified specialist and payment safety.", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0D9488), // Emerald Teal
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
                                    Icon(Icons.Default.Bolt, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Text("Express Dispatch", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Text("Specialists at Your Doorstep", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Text("Quick response for electricians, plumbers, and home appliance repairs.", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF7C3AED), // Deep Violet
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
                                    Icon(Icons.Default.VerifiedUser, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Text("Transparent INR Rates", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Text("Affordable ₹ Pricing", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Text("Clear hourly pricing in Indian Rupees with 100% verified specialists.", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
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
                            text = "${ServiceCategory.values().size} categories",
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
                }
            }

            // Quick Filter Chips
            if (providers.isNotEmpty()) {
                item {
                    val filters = listOf("All", "Top Rated (4.9+)", "Budget (< ₹400)", "Available Now")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filters) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Available Service Providers Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Service Providers",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${filteredProviders.size} added",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Providers List or Empty State
            if (providers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(StatusAcceptedBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Text(
                                text = "No Service Providers Added Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Your service catalog is empty. Tap '+ Add Pro' to add electricians, plumbers, carpenters, or mechanics.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = onAddProviderClick,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Service Provider", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (filteredProviders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No providers match your filter criteria.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(filteredProviders) { provider ->
                    ProviderCard(
                        provider = provider,
                        onBookClick = { onBookProvider(provider) },
                        onViewDetail = { onProviderSelected(provider) }
                    )
                }
            }
        }
    }
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
    onBookProvider: (ServiceProvider) -> Unit,
    onAddProviderClick: (ServiceCategory) -> Unit
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
                actions = {
                    FilledTonalButton(
                        onClick = { onAddProviderClick(category) },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = StatusAcceptedBg,
                            contentColor = PrimaryBlue
                        )
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
                                text = "No ${category.displayName}s Added Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "You haven't added any specialists for this service. Tap below to register one.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { onAddProviderClick(category) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add ${category.displayName}")
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
                            text = "3. Job Details & Location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = addressText,
                            onValueChange = { addressText = it },
                            label = { Text("Service Location Address") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = issueText,
                            onValueChange = { issueText = it },
                            label = { Text("Describe the Issue / Requirement") },
                            placeholder = { Text("e.g. Water leak under sink, need tap replacement...") },
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
    val bookings by repository.bookings.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredBookings = remember(bookings, selectedFilter) {
        when (selectedFilter) {
            "Pending" -> bookings.filter { it.status == BookingStatus.PENDING }
            "Accepted" -> bookings.filter { it.status == BookingStatus.ACCEPTED || it.status == BookingStatus.IN_PROGRESS }
            "Completed" -> bookings.filter { it.status == BookingStatus.COMPLETED }
            else -> bookings
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Service Bookings") },
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
            // Filter tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Pending", "Accepted", "Completed").forEach { statusLabel ->
                    FilterChip(
                        selected = selectedFilter == statusLabel,
                        onClick = { selectedFilter = statusLabel },
                        label = { Text(statusLabel) },
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
                                repository.cancelBooking(booking.id)
                            }
                        )
                    }
                }
            }
        }
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

            // UrbanClap Two-OTP Badge preview (Locked when PENDING)
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
            } else if (booking.status != BookingStatus.CANCELLED) {
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
                        Column {
                            Text(
                                text = "START OTP",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = booking.startOtp,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryBlue,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Divider(
                            modifier = Modifier
                                .height(26.dp)
                                .width(1.dp),
                            color = CardBorder
                        )

                        Column {
                            Text(
                                text = "COMPLETION OTP",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusCompleted,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = booking.completionOtp,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusCompleted,
                                fontFamily = FontFamily.Monospace
                            )
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
                        text = booking.customerAddress,
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
                        text = "Issue: ${booking.issueDescription}",
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
                    text = "ID: #${booking.id.takeLast(5)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            // Quick Simulate Acceptance for testing notifications on device
            if (booking.status == BookingStatus.PENDING) {
                Divider(color = CardBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCancelClick,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

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
