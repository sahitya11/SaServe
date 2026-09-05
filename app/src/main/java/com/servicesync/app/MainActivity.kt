package com.servicesync.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.servicesync.app.data.model.Booking
import com.servicesync.app.data.model.ServiceCategory
import com.servicesync.app.data.model.ServiceProvider
import com.servicesync.app.data.repository.ServiceSyncRepository
import com.servicesync.app.ui.screens.auth.AuthScreen
import com.servicesync.app.ui.screens.customer.*
import com.servicesync.app.ui.theme.*
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
    object Splash : Screen()
    object Auth : Screen()
    object CustomerHome : Screen()
    data class ProviderList(val category: ServiceCategory) : Screen()
    data class ProviderDetail(val provider: ServiceProvider) : Screen()
    object CustomerBookings : Screen()
    object Notifications : Screen()
    data class BookingStatus(val booking: Booking) : Screen()
    object Wallet : Screen()
    object HelpSupport : Screen()
    object ManageAddresses : Screen()
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppHost(initialNavTarget: String?) {
    val context = LocalContext.current
    val repository = remember { ServiceSyncRepository.getInstance(context) }
    val notifications by repository.notifications.collectAsState()
    val isLoggedIn by repository.isLoggedIn.collectAsState()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }

    // 2-Second Fullscreen SaServe Splash Screen Transition
    LaunchedEffect(Unit) {
        delay(2000)
        currentScreen = if (!repository.isUserLoggedIn()) {
            Screen.Auth
        } else {
            when (initialNavTarget) {
                "customer_bookings" -> Screen.CustomerBookings
                else -> Screen.CustomerHome
            }
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn && currentScreen !is Screen.Auth && currentScreen !is Screen.Splash) {
            currentScreen = Screen.Auth
        }
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
        containerColor = BackgroundLight
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is Screen.Splash -> {
                    SplashScreen()
                }

                is Screen.Auth -> {
                    AuthScreen(
                        repository = repository,
                        onAuthSuccess = {
                            currentScreen = Screen.CustomerHome
                        }
                    )
                }

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
                        onOpenBookings = {
                            currentScreen = Screen.CustomerBookings
                        },
                        onOpenWallet = {
                            currentScreen = Screen.Wallet
                        },
                        onOpenHelp = {
                            currentScreen = Screen.HelpSupport
                        },
                        onOpenAddresses = {
                            currentScreen = Screen.ManageAddresses
                        },
                        onAddProviderClick = {
                            addProviderCategory = null
                            showAddProviderDialog = true
                        },
                        onLogoutClick = {
                            repository.logoutCustomer()
                            currentScreen = Screen.Auth
                        },
                        onBookingSelected = { booking ->
                            currentScreen = Screen.BookingStatus(booking)
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

                is Screen.Wallet -> {
                    WalletScreen(
                        repository = repository,
                        onBackClick = { currentScreen = Screen.CustomerHome }
                    )
                }

                is Screen.HelpSupport -> {
                    HelpSupportScreen(
                        onBackClick = { currentScreen = Screen.CustomerHome }
                    )
                }

                is Screen.ManageAddresses -> {
                    ManageAddressesScreen(
                        repository = repository,
                        onBackClick = { currentScreen = Screen.CustomerHome }
                    )
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            // Enhanced Crayon Logo Image (Seamlessly feathered into background)
            Image(
                painter = painterResource(id = R.drawable.saserve_seamless_logo),
                contentDescription = "SaServe Logo",
                modifier = Modifier.fillMaxWidth(0.92f),
                contentScale = ContentScale.Fit
            )

            Text(
                text = "Specialist Home Services at Your Doorstep",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            CircularProgressIndicator(
                color = PrimaryBlue, // Radiant Cyan Blue
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
