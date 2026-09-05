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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.servicesync.app.data.model.Booking
import com.servicesync.app.data.model.ServiceCategory
import com.servicesync.app.data.model.ServiceProvider
import com.servicesync.app.data.model.UserRole
import com.servicesync.app.data.repository.ServiceSyncRepository
import com.servicesync.app.ui.screens.auth.AuthScreen
import com.servicesync.app.ui.screens.customer.*
import com.servicesync.app.ui.screens.provider.IncomingRequestsScreen
import com.servicesync.app.ui.screens.provider.ProviderDashboardScreen
import com.servicesync.app.ui.theme.BackgroundLight
import com.servicesync.app.ui.theme.PrimaryBlue
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

    object ProviderDashboard : Screen()
    object ProviderIncomingRequests : Screen()

    object Auth : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppHost(initialNavTarget: String?) {
    val context = LocalContext.current
    val repository = remember { ServiceSyncRepository.getInstance(context) }
    val currentUser by repository.currentUser.collectAsState()
    val notifications by repository.notifications.collectAsState()
    val bookings by repository.bookings.collectAsState()

    var currentScreen by remember {
        mutableStateOf<Screen>(
            when (initialNavTarget) {
                "customer_bookings" -> Screen.CustomerBookings
                "provider_requests" -> Screen.ProviderIncomingRequests
                else -> if (currentUser?.role == UserRole.PROVIDER) Screen.ProviderDashboard else Screen.CustomerHome
            }
        )
    }

    var showRoleSwitchDialog by remember { mutableStateOf(false) }

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

    // Role Switch Dialog Modal
    if (showRoleSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showRoleSwitchDialog = false },
            title = { Text("Switch Active Role") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select a perspective to test both sides of the booking lifecycle:")

                    FilledTonalButton(
                        onClick = {
                            repository.switchUserRole(UserRole.CUSTOMER)
                            currentScreen = Screen.CustomerHome
                            showRoleSwitchDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Customer Mode (Sarah Jenkins)")
                    }

                    FilledTonalButton(
                        onClick = {
                            repository.switchUserRole(UserRole.PROVIDER)
                            currentScreen = Screen.ProviderDashboard
                            showRoleSwitchDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Handyman, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Provider Mode (Marcus Vance - Plumber)")
                    }

                    OutlinedButton(
                        onClick = {
                            currentScreen = Screen.Auth
                            showRoleSwitchDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create / Register New Account")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleSwitchDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val unreadNotifs = notifications.count { !it.isRead }
    val pendingCount = bookings.count { it.status == com.servicesync.app.data.model.BookingStatus.PENDING }

    Scaffold(
        bottomBar = {
            if (currentScreen !is Screen.Auth && currentScreen !is Screen.ProviderDetail && currentScreen !is Screen.ProviderList) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    if (currentUser?.role == UserRole.PROVIDER) {
                        NavigationBarItem(
                            selected = currentScreen is Screen.ProviderDashboard,
                            onClick = { currentScreen = Screen.ProviderDashboard },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                            label = { Text("Dashboard") }
                        )
                        NavigationBarItem(
                            selected = currentScreen is Screen.ProviderIncomingRequests,
                            onClick = { currentScreen = Screen.ProviderIncomingRequests },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (pendingCount > 0) {
                                            Badge(containerColor = StatusCancelled) {
                                                Text("$pendingCount")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Inbox, contentDescription = "Requests")
                                }
                            },
                            label = { Text("Requests") }
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = { showRoleSwitchDialog = true },
                            icon = { Icon(Icons.Default.SwapHoriz, contentDescription = "Switch Role") },
                            label = { Text("Switch") }
                        )
                    } else {
                        NavigationBarItem(
                            selected = currentScreen is Screen.CustomerHome,
                            onClick = { currentScreen = Screen.CustomerHome },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
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
                        onSwitchRoleClick = {
                            showRoleSwitchDialog = true
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
                        }
                    )
                }

                is Screen.ProviderDetail -> {
                    ProviderDetailScreen(
                        provider = screen.provider,
                        repository = repository,
                        onBackClick = { currentScreen = Screen.CustomerHome },
                        onBookingConfirmed = {
                            currentScreen = Screen.CustomerBookings
                        }
                    )
                }

                is Screen.CustomerBookings -> {
                    CustomerBookingsScreen(
                        repository = repository,
                        onBackClick = { currentScreen = Screen.CustomerHome }
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

                is Screen.ProviderDashboard -> {
                    ProviderDashboardScreen(
                        repository = repository,
                        onViewIncomingRequests = {
                            currentScreen = Screen.ProviderIncomingRequests
                        },
                        onViewActiveJobs = {
                            currentScreen = Screen.ProviderIncomingRequests
                        },
                        onViewProfile = {
                            showRoleSwitchDialog = true
                        },
                        onSwitchRoleClick = {
                            showRoleSwitchDialog = true
                        }
                    )
                }

                is Screen.ProviderIncomingRequests -> {
                    IncomingRequestsScreen(
                        repository = repository,
                        onBackClick = { currentScreen = Screen.ProviderDashboard }
                    )
                }

                is Screen.Auth -> {
                    AuthScreen(
                        repository = repository,
                        onAuthSuccess = {
                            currentScreen = if (repository.currentUser.value?.role == UserRole.PROVIDER) {
                                Screen.ProviderDashboard
                            } else {
                                Screen.CustomerHome
                            }
                        }
                    )
                }
            }
        }
    }
}
