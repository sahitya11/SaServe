package com.servicesync.app.ui.screens.customer

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servicesync.app.data.model.*
import com.servicesync.app.data.repository.ServiceSyncRepository
import com.servicesync.app.ui.components.getCategoryIcon
import com.servicesync.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceCatalogScreen(
    initialCategory: ServiceCategory,
    preselectedItemId: String? = null,
    repository: ServiceSyncRepository,
    onBackClick: () -> Unit,
    onBookingSelected: (Booking) -> Unit
) {
    val currentUser by repository.currentUser.collectAsState()
    val savedAddresses by repository.savedAddresses.collectAsState()
    val pendingCancellationFee by repository.pendingCancellationFee.collectAsState()

    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var catalogSearchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // State for booking modal
    var activeBookingItem by remember {
        val initialList = getSubServicesForCategory(initialCategory)
        mutableStateOf(initialList.firstOrNull { it.id == preselectedItemId } ?: if (preselectedItemId != null) initialList.firstOrNull() else null)
    }
    var showBookingModal by remember { mutableStateOf(preselectedItemId != null) }

    // Booking configuration state
    var selectedDate by remember { mutableStateOf("Today") }
    var selectedSlot by remember { mutableStateOf("Immediate (in 30 mins)") }
    var serviceAddress by remember(currentUser, savedAddresses) {
        mutableStateOf(savedAddresses.firstOrNull()?.formattedDisplayAddress ?: currentUser?.address ?: "Current Location")
    }
    var issueNotes by remember { mutableStateOf("") }
    var isSearchingSpecialist by remember { mutableStateOf(false) }
    var assignedBooking by remember { mutableStateOf<Booking?>(null) }

    val categorySubServices = remember(selectedCategory, catalogSearchQuery) {
        val all = getSubServicesForCategory(selectedCategory)
        if (catalogSearchQuery.isBlank()) all
        else all.filter {
            it.name.contains(catalogSearchQuery, ignoreCase = true) ||
            it.description.contains(catalogSearchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${selectedCategory.displayName} Services",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "SaServe Assured Warranty • Verified Experts",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
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
            // Category Switcher Chips (switch between categories on the same page)
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ServiceCategory.values()) { cat ->
                        val isCatActive = selectedCategory == cat
                        FilterChip(
                            selected = isCatActive,
                            onClick = {
                                selectedCategory = cat
                                catalogSearchQuery = ""
                            },
                            label = {
                                Text(
                                    text = cat.displayName,
                                    fontWeight = if (isCatActive) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = getCategoryIcon(cat),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isCatActive) Color.White else PrimaryBlue
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SurfaceLight,
                                labelColor = TextPrimary,
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isCatActive,
                                borderColor = CardBorder,
                                selectedBorderColor = PrimaryBlue
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // SaServe Assured Hero Category Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.2.dp, PrimaryBlue.copy(alpha = 0.35f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        PrimaryBlue.copy(alpha = 0.12f),
                                        SecondaryTeal.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PrimaryBlue
                                ) {
                                    Text(
                                        text = "SASERVE ASSURED STANDARDS",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Star, null, tint = StarGold, modifier = Modifier.size(16.dp))
                                    Text("4.88 ★", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                    Text("(25k+ booked)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }

                            Text(
                                text = "Professional ${selectedCategory.displayName} Repair & Setup",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )

                            // 3 Key Badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Shield, null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                                    Text("30-Day Warranty", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Verified, null, tint = SecondaryTeal, modifier = Modifier.size(14.dp))
                                    Text("Verified Pros", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Lock, null, tint = AccentGold, modifier = Modifier.size(14.dp))
                                    Text("2-OTP Security", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // In-Catalog Search Bar
            item {
                OutlinedTextField(
                    value = catalogSearchQuery,
                    onValueChange = { catalogSearchQuery = it },
                    placeholder = { Text("Search ${selectedCategory.displayName} appliances & services...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                    trailingIcon = {
                        if (catalogSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { catalogSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceLight,
                        unfocusedContainerColor = SurfaceLight,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = CardBorder
                    ),
                    singleLine = true
                )
            }

            // Catalog Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available Appliances & Services (${categorySubServices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Fixed Upfront Rates",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // List of Appliances with Images & Inclusions
            items(categorySubServices) { item ->
                ApplianceCatalogCard(
                    item = item,
                    category = selectedCategory,
                    onBookClick = {
                        activeBookingItem = item
                        showBookingModal = true
                        assignedBooking = null
                        isSearchingSpecialist = false
                    }
                )
            }
        }
    }

    // Interactive Booking & Dispatch Modal
    if (showBookingModal && activeBookingItem != null) {
        val currentItem = activeBookingItem!!

        AlertDialog(
            onDismissRequest = {
                if (!isSearchingSpecialist) {
                    showBookingModal = false
                    assignedBooking = null
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when {
                            assignedBooking != null -> Icons.Default.CheckCircle
                            isSearchingSpecialist -> Icons.Default.Radio
                            else -> Icons.Default.CalendarMonth
                        },
                        contentDescription = null,
                        tint = when {
                            assignedBooking != null -> StatusCompleted
                            else -> PrimaryBlue
                        }
                    )
                    Text(
                        text = when {
                            assignedBooking != null -> "Specialist Assigned!"
                            isSearchingSpecialist -> "Finding Your Specialist"
                            else -> "Schedule & Location"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (isSearchingSpecialist) {
                        // Uber / Ola Style Dynamic Radar Search View
                        ProviderSearchingRadarView(
                            category = selectedCategory,
                            subItemName = currentItem.name,
                            date = selectedDate,
                            slot = selectedSlot
                        )
                    } else if (assignedBooking != null) {
                        // Assigned Specialist Card with FIXED Scheduled Arrival Time
                        AssignedSpecialistCard(
                            booking = assignedBooking!!,
                            selectedItem = currentItem
                        )
                    } else {
                        // Selected Item Summary
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = PrimaryBlue.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceVariantLight)
                                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(
                                            id = com.servicesync.app.ui.components.getApplianceClayDrawable(currentItem.id)
                                        ),
                                        contentDescription = currentItem.name,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentItem.name,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (currentItem.price.equals("Unknown", ignoreCase = true))
                                            "${selectedCategory.displayName} • On-Site Custom Quote"
                                        else
                                            "${selectedCategory.displayName} • ${currentItem.price}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrimaryBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (currentItem.price.equals("Unknown", ignoreCase = true)) Color(0xFFF59E0B).copy(alpha = 0.15f) else PrimaryBlue.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (currentItem.price.equals("Unknown", ignoreCase = true)) "Unknown" else currentItem.price,
                                        color = if (currentItem.price.equals("Unknown", ignoreCase = true)) Color(0xFFD97706) else PrimaryBlue,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // Date Selection
                        Text(
                            text = "Select Service Date",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Today", "Tomorrow", "In 2 Days").forEach { dateOpt ->
                                val isSelectedDate = selectedDate == dateOpt
                                FilterChip(
                                    selected = isSelectedDate,
                                    onClick = { selectedDate = dateOpt },
                                    label = { Text(dateOpt, fontWeight = if (isSelectedDate) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        // Preferred Time Slot
                        Text(
                            text = "Preferred Time Slot",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        val slots = listOf(
                            "Immediate (in 30 mins) ⚡",
                            "10:00 AM - 12:00 PM",
                            "02:00 PM - 04:00 PM",
                            "05:00 PM - 07:00 PM",
                            "07:00 PM - 09:00 PM"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            slots.forEach { slot ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedSlot == slot) PrimaryBlue.copy(alpha = 0.12f) else Color.Transparent)
                                        .clickable { selectedSlot = slot }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedSlot == slot,
                                        onClick = { selectedSlot = slot },
                                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = slot,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                        fontWeight = if (selectedSlot == slot) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // Location
                        OutlinedTextField(
                            value = serviceAddress,
                            onValueChange = { serviceAddress = it },
                            label = { Text("Service Location") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = PrimaryBlue) },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Notes
                        OutlinedTextField(
                            value = issueNotes,
                            onValueChange = { issueNotes = it },
                            label = { Text("Problem Details / Instructions (Optional)") },
                            placeholder = { Text("e.g. Needs immediate check, spare parts on site") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Previous Cancellation Penalty Alert (if applicable)
                        if (pendingCancellationFee > 0.0) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StatusCancelledBg,
                                border = BorderStroke(1.dp, StatusCancelled.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, null, tint = StatusCancelled, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "Previous Late Cancellation Fine",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = StatusCancelled
                                        )
                                    }
                                    Text(
                                        text = "A fine of ₹${pendingCancellationFee.toInt()} from your previously cancelled in-progress service is applied to this booking.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                    HorizontalDivider(color = StatusCancelled.copy(alpha = 0.2f))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Service Base:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        Text(currentItem.price, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Late Cancellation Fine:", style = MaterialTheme.typography.bodySmall, color = StatusCancelled)
                                        Text("+ ₹${pendingCancellationFee.toInt()}", fontWeight = FontWeight.Bold, color = StatusCancelled)
                                    }
                                }
                            }
                        }

                        // Security Reassurance
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceVariantLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Shield, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "⚡ Price Lock • 2-OTP Verified Security • 30-Day Guarantee",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (assignedBooking != null) {
                    Button(
                        onClick = {
                            val target = assignedBooking!!
                            showBookingModal = false
                            assignedBooking = null
                            onBookingSelected(target)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Track Live Service 🚀", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else if (!isSearchingSpecialist) {
                    Button(
                        onClick = {
                            isSearchingSpecialist = true
                            coroutineScope.launch {
                                val fullDesc = "${currentItem.name}${if (issueNotes.isNotBlank()) " - $issueNotes" else ""}"
                                val booked = repository.broadcastServiceDispatch(
                                    category = selectedCategory,
                                    date = selectedDate,
                                    timeSlot = selectedSlot,
                                    address = serviceAddress,
                                    issueDescription = fullDesc
                                )
                                isSearchingSpecialist = false
                                assignedBooking = booked
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        val ctaLabel = if (pendingCancellationFee > 0.0) {
                            "Find Specialist (incl. ₹${pendingCancellationFee.toInt()} fine) 🚀"
                        } else {
                            "Find Specialist Now 🚀"
                        }
                        Text(ctaLabel, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isSearchingSpecialist) {
                    TextButton(onClick = {
                        showBookingModal = false
                        assignedBooking = null
                    }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

/**
 * SaServe Assured Appliance Card with Visual Image Box, Ratings, Inclusions, and Price.
 */
@Composable
fun ApplianceCatalogCard(
    item: SubServiceItem,
    category: ServiceCategory,
    onBookClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Visual Appliance Image Box
                ApplianceVisualBadge(item = item, category = category)

                // Main Info Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.rating,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Text(
                            text = "(${item.reviewCount} reviews)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = item.duration,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Inclusions Preview
                    item.includes.take(2).forEach { inc ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = StatusAccepted,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = inc,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

            // Price & Action Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.price.equals("Unknown", ignoreCase = true)) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Unknown",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "ON-SITE QUOTE",
                                        color = Color(0xFFD97706),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Decided by specialist on visit",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Text(
                            text = item.price,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        if (item.originalPrice.isNotBlank()) {
                            Text(
                                text = item.originalPrice,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                textDecoration = TextDecoration.LineThrough
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = StatusAccepted.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "SAVE 33%",
                                    color = StatusAccepted,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onBookClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Book Now ➔", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * Visual illustration image box for each appliance with authentic 3D claymorphic image, smooth rounded container, and badges.
 */
@Composable
fun ApplianceVisualBadge(
    item: SubServiceItem,
    category: ServiceCategory
) {
    val clayResId = com.servicesync.app.ui.components.getApplianceClayDrawable(item.id)

    Box(
        modifier = Modifier
            .size(105.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceVariantLight)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        // High-Quality 3D Claymorphic Appliance Image
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = clayResId),
            contentDescription = item.name,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Tag Ribbon (e.g. BESTSELLER / POPULAR)
        if (item.tag != null) {
            Surface(
                shape = RoundedCornerShape(bottomEnd = 8.dp, topStart = 14.dp),
                color = if (item.isPopular) PrimaryBlue else SecondaryTeal,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = item.tag,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 8.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Uber / Ola Style High-Tech Dynamic Radar Searching View.
 */
@Composable
fun ProviderSearchingRadarView(
    category: ServiceCategory,
    subItemName: String,
    date: String,
    slot: String
) {
    val infiniteTransition = rememberInfiniteTransition()

    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 450, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 450, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    var currentStep by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1100)
            currentStep = (currentStep + 1) % 4
        }
    }

    val stepText = when (currentStep) {
        0 -> "Scanning nearby verified ${category.displayName} specialists... 📍"
        1 -> "Connecting with top-rated pros within 5 km... ⚡"
        2 -> "Confirming priority dispatch for $subItemName... 🤝"
        else -> "Assigning your top-rated specialist... ✨"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Radar Rings Container
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer pulsing ring
            Box(
                modifier = Modifier
                    .size((130 * pulseScale1).dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = pulseAlpha1))
            )

            // Inner pulsing ring
            Box(
                modifier = Modifier
                    .size((110 * pulseScale2).dp)
                    .clip(CircleShape)
                    .background(SecondaryTeal.copy(alpha = pulseAlpha2))
            )

            // Radar Scanning Sweep
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .rotate(radarAngle)
                    .clip(CircleShape)
                    .border(2.dp, PrimaryBlue.copy(alpha = 0.5f), CircleShape)
            )

            // Center Beacon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NearMe,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Catchy Uber/Ola Header
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Finding Your Expert Specialist ⚡",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = TextPrimary
            )
            Text(
                text = "Connecting you with highest-rated nearby verified pros, just like Uber & Ola. Best available specialist will accept shortly.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        // Dynamic Stage Ticker Badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PrimaryBlue.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
        ) {
            Text(
                text = stepText,
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        // Summary Preview Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SurfaceVariantLight,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(subItemName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                    Text("$date • $slot", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Text("Rate Locked", fontWeight = FontWeight.Bold, color = StatusAccepted, fontSize = 12.sp)
            }
        }
    }
}

/**
 * Assigned Specialist Card with Accurate Arrival Time Handling (Fixes the 15-20 min scheduled bug).
 */
@Composable
fun AssignedSpecialistCard(
    booking: Booking,
    selectedItem: SubServiceItem
) {
    val isImmediate = booking.scheduledSlot.contains("Immediate", ignoreCase = true)
    val isToday = booking.scheduledDate.equals("Today", ignoreCase = true)

    val (arrivalLabel, arrivalValue, arrivalDescription) = if (isToday && isImmediate) {
        Triple(
            "Estimated Arrival",
            "In 15–30 mins (Today)",
            "⚡ Specialist is dispatched and on the way to your location for immediate service."
        )
    } else {
        Triple(
            "Scheduled Arrival",
            "${booking.scheduledDate} at ${booking.scheduledSlot}",
            "Specialist will arrive at your location on ${booking.scheduledDate} during your selected slot (${booking.scheduledSlot}). Please ensure someone is available at the address."
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StatusAcceptedBg)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.providerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${booking.category.displayName} Specialist • 4.9 ★ (Verified Pro)",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusAccepted
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Service Item", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(selectedItem.name, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            // Accurate Arrival Time Section
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = PrimaryBlue.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(arrivalLabel, style = MaterialTheme.typography.labelMedium, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isToday && isImmediate) PrimaryBlue else StatusAccepted
                        ) {
                            Text(
                                text = if (isToday && isImmediate) "DISPATCHED" else "SCHEDULED",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = arrivalValue,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = arrivalDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            // Two-OTP Security
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Start OTP", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("Share only when specialist arrives", fontSize = 10.sp, color = TextMuted)
                }
                Text(
                    text = booking.startOtp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryBlue,
                    fontSize = 18.sp
                )
            }
        }
    }
}
