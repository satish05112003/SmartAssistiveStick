package com.satis.caregiver.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.satis.caregiver.model.AlertEvent
import com.satis.caregiver.ui.theme.*
import com.satis.caregiver.utils.AlertManager
import com.satis.caregiver.utils.MapManager

@Composable
fun CaregiverAppScreen(
    viewModel: CaregiverViewModel,
    modifier: Modifier = Modifier
) {
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val alertManager = remember { AlertManager(context) }
    val mapManager = remember { MapManager(context) }

    Surface(
        color = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) {
        when (val state = appState) {
            is AppState.Idle -> LinkScreen(
                onLink = { accessKey -> viewModel.linkWithAccessKey(accessKey) }
            )
            is AppState.Loading -> LoadingScreen()
            is AppState.Error -> ErrorScreen(
                message = state.message,
                onRetry = { viewModel.unlink() }
            )
            is AppState.Linked -> DashboardScreen(
                viewModel = viewModel,
                mapManager = mapManager,
                alertManager = alertManager,
                onUnlink = { viewModel.unlink() }
            )
        }
    }
}

@Composable
fun LinkScreen(onLink: (String) -> Unit) {
    var accessKey by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Connect to SAS User",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = accessKey,
            onValueChange = { accessKey = it },
            label = { Text("Access Key") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonGreen,
                focusedLabelColor = NeonGreen,
                unfocusedBorderColor = TextSecondary
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { onLink(accessKey) },
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DarkBackground),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Link Caregiver", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = NeonGreen)
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Error", color = DangerRed, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message, color = TextPrimary)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = WarningYellow, contentColor = DarkBackground)
        ) {
            Text("Go Back")
        }
    }
}

@Composable
fun DashboardScreen(
    viewModel: CaregiverViewModel,
    mapManager: MapManager,
    alertManager: AlertManager,
    onUnlink: () -> Unit
) {
    val userData by viewModel.userData.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()

    val isSosActive = userData?.sosActive == true
    
    LaunchedEffect(isSosActive) {
        if (isSosActive) {
            alertManager.triggerVibration()
            alertManager.playSosAlert()
        }
    }

    Scaffold(
        topBar = {
            DashboardHeader(
                isSosActive = isSosActive,
                onUnlink = onUnlink
            )
        },
        floatingActionButton = {
            DashboardFAB(
                userData = userData,
                mapManager = mapManager
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Live Map
            if (userData != null) {
                Box(modifier = Modifier.weight(1f)) {
                    val latLng = LatLng(userData!!.latitude, userData!!.longitude)
                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(latLng, 16f)
                    }

                    // Auto-follow map logic can be enhanced, here we basic update position
                    LaunchedEffect(latLng) {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, cameraPositionState.position.zoom)
                    }

                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(zoomControlsEnabled = false)
                    ) {
                        Marker(
                            state = MarkerState(position = latLng),
                            title = "User Location"
                        )
                    }
                }

                // Stats and Feed
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .padding(16.dp)
                ) {
                    StatusCardsRow(mode = userData!!.mode, batteryLevel = userData!!.batteryLevel)
                    Spacer(modifier = Modifier.height(16.dp))
                    AlertFeed(alerts = alerts)
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardHeader(isSosActive: Boolean, onUnlink: () -> Unit) {
    val titleColor = if (isSosActive) DangerRed else NeonGreen
    val statusText = if (isSosActive) "SOS ACTIVE!" else "Status: Safe"

    TopAppBar(
        title = {
            Column {
                Text("SAS User", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(statusText, color = titleColor, style = MaterialTheme.typography.bodySmall)
            }
        },
        actions = {
            TextButton(onClick = onUnlink) {
                Text("Unlink", color = TextSecondary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
    )
}

@Composable
fun DashboardFAB(
    userData: com.satis.caregiver.model.UserData?,
    mapManager: MapManager
) {
    userData ?: return
    Column {
        FloatingActionButton(
            onClick = { mapManager.makePhoneCall("SOS_NUMBER_HERE") },
            containerColor = WarningYellow,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(Icons.Default.Call, contentDescription = "Call", tint = DarkBackground)
        }
        FloatingActionButton(
            onClick = { mapManager.openNavigation(userData.latitude, userData.longitude) },
            containerColor = NeonGreen
        ) {
            Icon(Icons.Default.Directions, contentDescription = "Navigate", tint = DarkBackground)
        }
    }
}

@Composable
fun StatusCardsRow(mode: String, batteryLevel: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatusCard(title = "Mode", value = mode, color = if (mode == "INDOOR") NeonGreen else WarningYellow)
        StatusCard(title = "Battery", value = "$batteryLevel%", color = if (batteryLevel > 20) NeonGreen else DangerRed)
    }
}

@Composable
fun StatusCard(title: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun AlertFeed(alerts: List<AlertEvent>) {
    Text("Alert History", color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
    LazyColumn(modifier = Modifier.height(200.dp)) {
        items(alerts) { alert ->
            val color = when (alert.severity) {
                "DANGER" -> DangerRed
                "WARNING" -> WarningYellow
                else -> NeonGreen
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (alert.severity == "DANGER") {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = color)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column {
                        Text(alert.type, color = color, fontWeight = FontWeight.Bold)
                        Text(alert.message, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
