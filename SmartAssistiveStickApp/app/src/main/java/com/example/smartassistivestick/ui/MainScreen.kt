package com.example.smartassistivestick.ui

import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.example.smartassistivestick.ui.theme.*
import com.example.smartassistivestick.bluetooth.BluetoothManager
import com.example.smartassistivestick.caregiver.CaregiverManager
import com.example.smartassistivestick.language.LanguageManager
import com.example.smartassistivestick.viewmodel.MainViewModel

/**
 * Live Assist Dashboard - Premium voice-first UI for visually impaired users
 * Accessibility-first design with large touch targets and voice feedback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, hasLocationPermission: Boolean, onConnectClicked: () -> Unit) {
    val currentAlert by viewModel.currentAlert.collectAsState()
    val subtext by viewModel.currentAlertSubtext.collectAsState()
    val isConnected by viewModel.isBluetoothConnected.collectAsState()
    val btStatus by viewModel.bluetoothConnectionStatus.collectAsState()
    val locationText by viewModel.locationText.collectAsState()
    val currentLatLng by viewModel.currentLatLng.collectAsState()
    val linkedCaregivers by viewModel.linkedCaregivers.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val sosActive by viewModel.sosActive.collectAsState()
    val accessCode by viewModel.accessCode.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()

    var showCaregiverMenu by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showAccessCode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Text(
                        "Smart Assistive Stick",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Language selector
                    IconButton(onClick = { showLanguageMenu = true }) {
                        Icon(Icons.Filled.Language, contentDescription = "Change Language", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = SurfaceContainerLowest)
            )
            
            if (showLanguageMenu) {
                LanguageSelectorDialog(
                    currentLanguage = selectedLanguage,
                    onLanguageSelected = { language ->
                        viewModel.setLanguage(language)
                        showLanguageMenu = false
                    },
                    onDismiss = { showLanguageMenu = false }
                )
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                // Voice/SOS Button
                FloatingActionButton(
                    onClick = {
                        if (sosActive) {
                            viewModel.cancelSOS()
                        } else if (voiceState == MainViewModel.VoiceAssistantState.IDLE) {
                            viewModel.startVoiceCommand()
                        }
                    },
                    containerColor = if (sosActive) Color(0xFF93000A) else Primary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(90.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (sosActive) Icons.Filled.CallEnd else Icons.Filled.Mic,
                            contentDescription = if (sosActive) "Cancel SOS" else "Voice Command",
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            if (sosActive) "CANCEL" else "VOICE",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                    }
                }

                // Emergency SOS Button
                FloatingActionButton(
                    onClick = {
                        viewModel.processMessage("SOS")
                    },
                    containerColor = Color(0xFF93000A),
                    contentColor = Color.White,
                    modifier = Modifier.size(70.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Emergency,
                            contentDescription = "Emergency SOS",
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            "SOS",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        },
        containerColor = SurfaceContainerLowest
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. AWARENESS BANNER
            AwarenessCard(currentAlert, subtext, sosActive)

            // 2. VOICE STATE INDICATOR
            VoiceStateIndicator(voiceState)

            // 3. BLUETOOTH STATUS
            BluetoothStatusCard(isConnected, btStatus, onConnectClicked)

            // 4. LIVE LOCATION
            LocationMapCard(
                locationText = locationText,
                currentLatLng = currentLatLng,
                hasLocationPermission = hasLocationPermission
            )

            // 5. CONTEXT-AWARE ACTIONS
            ContextualActionsCard(viewModel = viewModel)

            // 6. CAREGIVER QUICK ACCESS
            CaregiverAccessCard(
                linkedCaregivers = linkedCaregivers,
                onShowCaregiverMenu = { showCaregiverMenu = true },
                onShowAccessCode = { showAccessCode = true },
                onRevokeAccess = { caregiverId ->
                    viewModel.revokeCaregiverAccess(caregiverId)
                }
            )

            // 7. ACCESS CODE DISPLAY
            if (showAccessCode && accessCode.isNotEmpty()) {
                AccessCodeCard(
                    code = accessCode,
                    onDismiss = { showAccessCode = false }
                )
            } else if (showAccessCode && accessCode.isEmpty()) {
                Button(
                    onClick = { viewModel.generateAccessCode() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Generate Access Code", fontWeight = FontWeight.Bold)
                }
            }

            if (showCaregiverMenu) {
                CaregiverManagementDialog(
                    onDismiss = { showCaregiverMenu = false },
                    linkedCaregivers = linkedCaregivers,
                    onRevokeAccess = { caregiverId ->
                        viewModel.revokeCaregiverAccess(caregiverId)
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Awareness Banner showing Safe/Warning/Danger status
 */
@Composable
fun AwarenessCard(alert: String, subtext: String, sosActive: Boolean) {
    val backgroundColor = when {
        sosActive -> Color(0xFF93000A).copy(alpha = 0.2f)
        alert.contains("EMERGENCY") -> Color(0xFF93000A).copy(alpha = 0.2f)
        alert.contains("DANGER") || alert.contains("WARNING") -> Color(0xFFFFB81C).copy(alpha = 0.2f)
        else -> Color(0xFF4CAF50).copy(alpha = 0.2f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = alert,
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 36.sp),
                color = Primary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Voice State Indicator
 */
@Composable
fun VoiceStateIndicator(voiceState: MainViewModel.VoiceAssistantState) {
    val stateText = when (voiceState) {
        MainViewModel.VoiceAssistantState.IDLE -> "Ready"
        MainViewModel.VoiceAssistantState.LISTENING -> "Listening..."
        MainViewModel.VoiceAssistantState.PROCESSING -> "Processing..."
        MainViewModel.VoiceAssistantState.SPEAKING -> "Speaking..."
        MainViewModel.VoiceAssistantState.ERROR -> "Error"
    }

    val indicatorColor = when (voiceState) {
        MainViewModel.VoiceAssistantState.IDLE -> Color(0xFF4CAF50)
        MainViewModel.VoiceAssistantState.LISTENING -> Color(0xFF2196F3)
        MainViewModel.VoiceAssistantState.PROCESSING -> Color(0xFFFF9800)
        MainViewModel.VoiceAssistantState.SPEAKING -> Color(0xFF9C27B0)
        MainViewModel.VoiceAssistantState.ERROR -> Color.Red
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = indicatorColor.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color = indicatorColor, shape = CircleShape)
        )
        Text(
            text = "Voice: $stateText",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Bluetooth Status Card
 */
@Composable
fun BluetoothStatusCard(isConnected: Boolean, btStatus: BluetoothManager.ConnectionStatus, onConnectClicked: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            color = when (btStatus) {
                                BluetoothManager.ConnectionStatus.CONNECTED -> Color(0xFF4CAF50)
                                BluetoothManager.ConnectionStatus.CONNECTING -> Color(0xFF2196F3)
                                else -> Color.Red
                            },
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Bluetooth",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                    Text(
                        text = when (btStatus) {
                            BluetoothManager.ConnectionStatus.CONNECTED -> "Connected"
                            BluetoothManager.ConnectionStatus.CONNECTING -> "Connecting..."
                            BluetoothManager.ConnectionStatus.FAILED -> "Failed"
                            BluetoothManager.ConnectionStatus.DISCONNECTED -> "Disconnected"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!isConnected) {
                Button(
                    onClick = onConnectClicked,
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Connect", fontWeight = FontWeight.Bold)
                }
            } else {
                Icon(Icons.Filled.BluetoothConnected, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/**
 * Live Location Card
 */
@Composable
fun LocationMapCard(
    locationText: String,
    currentLatLng: Pair<Double, Double>?,
    hasLocationPermission: Boolean
) {
    val context = LocalContext.current
    val fallback = LatLng(20.5937, 78.9629)
    val userLatLng = currentLatLng?.let { LatLng(it.first, it.second) } ?: fallback
    val hasValidApiKey = remember(context) { hasValidMapsApiKey(context) }
    val cameraPositionState = remember {
        CameraPositionState(
            position = CameraPosition.fromLatLngZoom(userLatLng, if (currentLatLng == null) 4f else 16f)
        )
    }

    LaunchedEffect(currentLatLng) {
        if (currentLatLng != null) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(userLatLng, 16f),
                durationMs = 1000
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Live Location", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            Text(locationText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = OnSurface)

            if (!hasValidApiKey) {
                Text(
                    text = "Google Maps API key missing or invalid. Please configure API key.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF93000A)
                )
            } else if (!hasLocationPermission) {
                Text(
                    text = "Location permission is denied. Enable location permission to view live map.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF93000A)
                )
            } else {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = true),
                    uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false)
                ) {
                    Marker(
                        state = MarkerState(position = userLatLng),
                        title = "You",
                        snippet = "Current Location"
                    )
                }
            }
        }
    }
}

private fun hasValidMapsApiKey(context: android.content.Context): Boolean {
    return try {
        val appInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        val apiKey = appInfo.metaData?.getString("com.google.android.geo.API_KEY")
        !apiKey.isNullOrBlank()
    } catch (e: Exception) {
        Log.e("APP_DEBUG", "Failed to read maps API key from manifest", e)
        false
    }
}

/**
 * Contextual Actions
 */
@Composable
fun ContextualActionsCard(viewModel: MainViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Quick Actions", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = OnSurfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(
                    icon = Icons.Filled.Map,
                    label = "Navigate",
                    onClick = { },
                    modifier = Modifier.weight(1f)
                )

                ActionButton(
                    icon = Icons.Filled.Phone,
                    label = "Call Caregiver",
                    onClick = { },
                    modifier = Modifier.weight(1f)
                )

                ActionButton(
                    icon = Icons.Filled.LocationOn,
                    label = "Track",
                    onClick = { },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Primary.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.Gray)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = Primary, modifier = Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = OnSurface)
        }
    }
}

/**
 * Caregiver Access Card
 */
@Composable
fun CaregiverAccessCard(
    linkedCaregivers: List<CaregiverManager.CaregiverAccess>,
    onShowCaregiverMenu: () -> Unit,
    onShowAccessCode: () -> Unit,
    onRevokeAccess: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Caregivers", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = OnSurface)
                Button(
                    onClick = onShowAccessCode,
                    modifier = Modifier.height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Share Code", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            if (linkedCaregivers.isEmpty()) {
                Text("No caregivers linked yet.", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(linkedCaregivers) { caregiver ->
                        CaregiverItem(caregiver, onRevokeAccess)
                    }
                }
            }
        }
    }
}

@Composable
fun CaregiverItem(caregiver: CaregiverManager.CaregiverAccess, onRevokeAccess: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(caregiver.caregiverName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Text(caregiver.caregiverPhone, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
            IconButton(onClick = { onRevokeAccess(caregiver.caregiverId) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Revoke", tint = Color.Red)
            }
        }
    }
}

/**
 * Access Code Display Card
 */
@Composable
fun AccessCodeCard(code: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(Secondary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Secondary.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Share this code with your caregiver", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = OnSurface)
            Text(
                text = code,
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 48.sp),
                color = Secondary,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text("This code will expire in 24 hours", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant, textAlign = TextAlign.Center)
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Caregiver Management Dialog
 */
@Composable
fun CaregiverManagementDialog(
    onDismiss: () -> Unit,
    linkedCaregivers: List<CaregiverManager.CaregiverAccess>,
    onRevokeAccess: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Caregivers") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(linkedCaregivers) { caregiver ->
                    CaregiverItem(caregiver, onRevokeAccess)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Language Selector Dialog
 */
@Composable
fun LanguageSelectorDialog(
    currentLanguage: LanguageManager.Language,
    onLanguageSelected: (LanguageManager.Language) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Language") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LanguageManager.Language.values()) { language ->
                    Button(
                        onClick = { onLanguageSelected(language) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (language == currentLanguage) Primary else SurfaceContainerHigh
                        )
                    ) {
                        Text(language.displayName, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
