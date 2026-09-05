package com.servicesync.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.servicesync.app.data.model.Booking
import com.servicesync.app.data.model.ServiceCategory
import com.servicesync.app.data.model.ServiceProvider
import com.servicesync.app.data.repository.ServiceSyncRepository
import com.servicesync.app.ui.screens.customer.*
import com.servicesync.app.ui.theme.BackgroundLight
import com.servicesync.app.ui.theme.ServiceSyncTheme
import com.servicesync.app.ui.theme.StatusCancelled

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialNavTarget = intent?.getStringExtra("EXTRA_NAV_TARGET")

        setContent {
            ServiceSyncTheme {
                MainAppHost(initialNavTarget = initialNavTarget)
            }
        }
    }
}

sealed class Screen {
    object CustomerHome : Screen()
    data class ProviderList(val category: ServiceCategory) : Screen()
    data class ProviderDetail(val provider: ServiceProvider) : Screen()
    object CustomerBookings : Screen()
    object Notifications : Screen()
    data class BookingStatus(val booking: Booking) : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppHost(initialNavTarget: String?) {
    val context = LocalContext.current
    val repository = remember { ServiceSyncRepository.getInstance(context) }
    val notifications by repository.notifications.collectAsState()

    var currentScreen by remember {
        mutableStateOf<Screen>(
            when (initialNavTarget) {
                "customer_bookings" -> Screen.CustomerBookings
                else -> Screen.CustomerHome
            }
        )
    }

    var showAddProviderDialog by remember { mutableStateOf(false) }
    var addProviderCategory by remember { mutableStateOf<ServiceCategory?>(null) }

    // Request notification permission for Android 13+ (Tiramisu)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* Granted or denied */ }

        LaunchedEffect(Unit) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Add Provider Dialog
    if (showAddProviderDialog) {
        AddProviderDialog(
            repository = repository,
            initialCategory = addProviderCategory,
            onDismiss = {
                showAddProviderDialog = false
                addProviderCategory = null
            },
            onProviderAdded = {
                showAddProviderDialog = false
                addProviderCategory = null
            }
        )
    }

    val unreadNotifs = notifications.count { !it.isRead }

    Scaffold(
        bottomBar = {
            if (currentScreen !is Screen.ProviderDetail && currentScreen !is Screen.ProviderList && currentScreen !is Screen.BookingStatus) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen is Screen.CustomerHome,
                        onClick = { currentScreen = Screen.CustomerHome },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Services") },
                        label = { Text("Services") }
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.CustomerBookings,
                        onClick = { currentScreen = Screen.CustomerBookings },
                        icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Bookings") },
                        label = { Text("Bookings") }
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Notifications,
                        onClick = { currentScreen = Screen.Notifications },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifs > 0) {
                                        Badge(containerColor = StatusCancelled) {
                                            Text("$unreadNotifs")
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                            }
                        },
                        label = { Text("Alerts") }
                    )
                }
            }
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is Screen.CustomerHome -> {
                    CustomerHomeScreen(
                        repository = repository,
                        onCategorySelected = { category ->
                            currentScreen = Screen.ProviderList(category)
                        },
                        onProviderSelected = { provider ->
                            currentScreen = Screen.ProviderDetail(provider)
                        },
                        onBookProvider = { provider ->
                            currentScreen = Screen.ProviderDetail(provider)
                        },
                        onOpenNotifications = {
                            currentScreen = Screen.Notifications
                        },
                        onAddProviderClick = {
                            addProviderCategory = null
                            showAddProviderDialog = true
                        }
                    )
                }

                is Screen.ProviderList -> {
                    ProviderListScreen(
                        category = screen.category,
                        repository = repository,
                        onBackClick = { currentScreen = Screen.CustomerHome },
                        onProviderSelected = { provider ->
                            currentScreen = Screen.ProviderDetail(provider)
                        },
                        onBookProvider = { provider ->
                            currentScreen = Screen.ProviderDetail(provider)
                        },
                        onAddProviderClick = { cat ->
                            addProviderCategory = cat
                            showAddProviderDialog = true
                        }
                    )
                }

                is Screen.ProviderDetail -> {
                    ProviderDetailScreen(
                        provider = screen.provider,
                        repository = repository,
                        onBackClick = { currentScreen = Screen.CustomerHome },
                        onBookingConfirmed = { booking ->
                            currentScreen = Screen.BookingStatus(booking)
                        }
                    )
                }

                is Screen.CustomerBookings -> {
                    CustomerBookingsScreen(
                        repository = repository,
                        onBackClick = { currentScreen = Screen.CustomerHome },
                        onBookingClick = { booking ->
                            currentScreen = Screen.BookingStatus(booking)
                        }
                    )
                }

                is Screen.BookingStatus -> {
                    BookingStatusScreen(
                        initialBooking = screen.booking,
                        repository = repository,
                        onBackToHome = { currentScreen = Screen.CustomerHome },
                        onViewAllBookings = { currentScreen = Screen.CustomerBookings }
                    )
                }

                is Screen.Notifications -> {
                    NotificationsScreen(
                        repository = repository,
                        onBackClick = { currentScreen = Screen.CustomerHome },
                        onNotificationClick = {
                            currentScreen = Screen.CustomerBookings
                        }
                    )
                }
            }
        }
    }
}
