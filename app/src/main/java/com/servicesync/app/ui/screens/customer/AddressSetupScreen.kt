package com.servicesync.app.ui.screens.customer

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.servicesync.app.data.repository.ServiceSyncRepository
import com.servicesync.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSetupScreen(
    repository: ServiceSyncRepository,
    onAddressSaved: () -> Unit
) {
    val context = LocalContext.current

    var flatHouseInput by remember { mutableStateOf("") }
    var streetAreaInput by remember { mutableStateOf("") }
    var landmarkInput by remember { mutableStateOf("") }
    var instructionsInput by remember { mutableStateOf("") }

    var isDetectingLocation by remember { mutableStateOf(false) }
    var gpsDetectedSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            isDetectingLocation = true
            detectCurrentGpsLocation(context) { detected ->
                isDetectingLocation = false
                if (!detected.isNullOrBlank()) {
                    streetAreaInput = detected
                    gpsDetectedSuccess = true
                }
            }
        } else {
            errorMessage = "Location permission is needed to auto-detect your area."
        }
    }

    fun triggerGpsDetection() {
        errorMessage = null
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            isDetectingLocation = true
            detectCurrentGpsLocation(context) { detected ->
                isDetectingLocation = false
                if (!detected.isNullOrBlank()) {
                    streetAreaInput = detected
                    gpsDetectedSuccess = true
                }
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Auto-trigger GPS detection once on entering screen
    LaunchedEffect(Unit) {
        triggerGpsDetection()
    }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Step 2: Service Location",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Where should specialists visit?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Auto-detect or fill your flat & street so service specialists arrive without delay.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // GPS Auto-detect Action Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceLight,
                border = BorderStroke(1.dp, if (gpsDetectedSuccess) PrimaryBlue else CardBorder),
                modifier = Modifier.fillMaxWidth()
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
                                Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "GPS Auto-Detection",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        if (gpsDetectedSuccess) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StatusAcceptedBg
                            ) {
                                Text(
                                    text = "✓ Detected",
                                    color = StatusAccepted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { triggerGpsDetection() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue.copy(alpha = 0.15f),
                            contentColor = PrimaryBlue
                        ),
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isDetectingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Detecting Current GPS Area...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (gpsDetectedSuccess) "Re-detect GPS Location" else "📍 Detect Current GPS Location",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Error Banner
            AnimatedVisibility(visible = errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Address Details Form Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Precise Address Details",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )

                    // Flat / House / Apartment
                    OutlinedTextField(
                        value = flatHouseInput,
                        onValueChange = {
                            flatHouseInput = it
                            errorMessage = null
                        },
                        label = { Text("Flat / House / Apartment / Floor No.") },
                        placeholder = { Text("e.g. Flat 302, Green Valley Apartments") },
                        leadingIcon = { Icon(Icons.Default.Apartment, null, tint = PrimaryBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Street / Road / Area
                    OutlinedTextField(
                        value = streetAreaInput,
                        onValueChange = {
                            streetAreaInput = it
                            errorMessage = null
                        },
                        label = { Text("Street / Road / Sector / Area") },
                        placeholder = { Text("e.g. 100 Feet Rd, Indiranagar") },
                        leadingIcon = { Icon(Icons.Default.Signpost, null, tint = PrimaryBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Landmark
                    OutlinedTextField(
                        value = landmarkInput,
                        onValueChange = {
                            landmarkInput = it
                            errorMessage = null
                        },
                        label = { Text("Nearby Landmark (Optional)") },
                        placeholder = { Text("e.g. Near Metro Station / City Park") },
                        leadingIcon = { Icon(Icons.Default.NearMe, null, tint = PrimaryBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Reach Instructions
                    OutlinedTextField(
                        value = instructionsInput,
                        onValueChange = {
                            instructionsInput = it
                            errorMessage = null
                        },
                        label = { Text("Instructions to Reach (Optional)") },
                        placeholder = { Text("e.g. Ring bell #3, take elevator to 3rd floor") },
                        leadingIcon = { Icon(Icons.Default.Directions, null, tint = PrimaryBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Save & Continue Button
            Button(
                onClick = {
                    val cleanStreet = streetAreaInput.trim()
                    val cleanFlat = flatHouseInput.trim()

                    if (cleanStreet.isBlank() && cleanFlat.isBlank()) {
                        errorMessage = "Please enter your address or tap auto-detect GPS location."
                        return@Button
                    }

                    isSaving = true
                    repository.updateUserAddress(
                        flatHouseNo = cleanFlat,
                        streetArea = if (cleanStreet.isNotBlank()) cleanStreet else "Current Location",
                        landmark = landmarkInput.trim(),
                        reachInstructions = instructionsInput.trim(),
                        isDefault = true
                    )
                    isSaving = false
                    onAddressSaved()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color(0xFF0A0C0E), modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF0A0C0E))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Address & Go to Home",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0A0C0E)
                    )
                }
            }

            // Skip Option
            TextButton(
                onClick = {
                    // Set minimal default address and continue to home
                    repository.updateUserAddress(
                        flatHouseNo = "",
                        streetArea = if (streetAreaInput.isNotBlank()) streetAreaInput.trim() else "Home Address",
                        landmark = "",
                        reachInstructions = "",
                        isDefault = true
                    )
                    onAddressSaved()
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Skip for now, set address later",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
