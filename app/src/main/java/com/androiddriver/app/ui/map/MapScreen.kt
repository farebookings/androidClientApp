package com.androiddriver.app.ui.map

import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.androiddriver.app.data.api.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateSchedule: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateProfile: () -> Unit,
    onBookingCreated: (BookingDto) -> Unit
) {
    val context = LocalContext.current
    var pickupAddress by remember { mutableStateOf("") }
    var dropoffAddress by remember { mutableStateOf("") }
    var showMap by remember { mutableStateOf(true) }
    var estimatedFare by remember { mutableStateOf<Double?>(null) }
    var bookingResult by remember { mutableStateOf<String?>(null) }

    // OSM Configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = context.cacheDir
            osmdroidTileCache = context.cacheDir.resolve("tiles")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🚖 Book a Ride", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                    label = { Text("Ride") },
                    selected = true,
                    onClick = {}
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    label = { Text("Schedule") },
                    selected = false,
                    onClick = onNavigateSchedule
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("History") },
                    selected = false,
                    onClick = onNavigateHistory
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Profile") },
                    selected = false,
                    onClick = onNavigateProfile
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Map (full screen background)
            if (showMap) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(15.0)
                                controller.setCenter(GeoPoint(40.4168, -3.7038)) // Madrid
                                isTilesScaledToDpi = true

                                // Sample marker for pickup
                                val marker = Marker(this)
                                marker.position = GeoPoint(40.4168, -3.7038)
                                marker.title = "📍 Your location"
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                overlays.add(marker)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay floating button for booking
                    Column(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Address card
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                OutlinedTextField(
                                    value = pickupAddress,
                                    onValueChange = { pickupAddress = it },
                                    label = { Text("Pickup location") },
                                    leadingIcon = { Icon(Icons.Default.MyLocation, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = dropoffAddress,
                                    onValueChange = { dropoffAddress = it },
                                    label = { Text("Where to?") },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Estimated fare
                                estimatedFare?.let { fare ->
                                    Text(
                                        "Estimated: ${"%.2f".format(fare)}€",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                // Booking result
                                bookingResult?.let {
                                    Text(
                                        it,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Button(
                                    onClick = {
                                        // Simulate booking (real app calls API)
                                        estimatedFare = 12.50
                                        bookingResult = "✅ Driver assigned! Coming in ~5 min"
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    enabled = pickupAddress.isNotBlank() && dropoffAddress.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.DirectionsCar, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("REQUEST RIDE", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
