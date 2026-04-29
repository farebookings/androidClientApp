package com.androiddriver.app.ui.map

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    onBookingCreated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ─── State ─────────────────────────────────────────────
    var pickupAddress by remember { mutableStateOf("Getting location...") }
    var dropoffAddress by remember { mutableStateOf("") }
    var pickupLat by remember { mutableStateOf(0.0) }
    var pickupLng by remember { mutableStateOf(0.0) }
    var dropoffLat by remember { mutableStateOf<Double?>(null) }
    var dropoffLng by remember { mutableStateOf<Double?>(null) }
    var distanceKm by remember { mutableStateOf<Double?>(null) }
    var fare by remember { mutableStateOf<Double?>(null) }
    var durationMin by remember { mutableStateOf<Long?>(null) }
    var routePoints by remember { mutableStateOf<List<Pair<Double, Double>>?>(null) }
    var isRouting by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(true) }
    var isBooking by remember { mutableStateOf(false) }
    var bookingResult by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var eventsOverlayRef by remember { mutableStateOf<org.osmdroid.views.overlay.MapEventsOverlay?>(null) }
    var isLocating by remember { mutableStateOf(true) }

    // ─── Location Permission ───────────────────────────────
    var locationGranted by remember { mutableStateOf(false) }
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        locationGranted = perms.values.any { it }
        if (locationGranted) {
            isLocating = true
            getCurrentLocation(context, onLocation = { lat, lng ->
                pickupLat = lat; pickupLng = lng
                reverseGeocode(context, lat, lng) { addr ->
                    pickupAddress = addr
                    isLocating = false
                }
                updateMap(mapView, lat, lng, dropoffLat, dropoffLng, pickupAddress, null)
            })
        } else {
            isLocating = false
            pickupAddress = "Location permission denied"
        }
    }

    // Request permission on first composition
    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = context.cacheDir
            osmdroidTileCache = context.cacheDir.resolve("tiles")
        }
        locationLauncher.launch(arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    // ─── Geocode dropoff (debounced 1.5s) ──────────────
    var geocodingDropoff by remember { mutableStateOf(false) }
    val geocodeKey = remember { mutableStateOf(0L) }
    LaunchedEffect(geocodeKey.value) {
        if (dropoffAddress.length > 5 && geocodeKey.value > 0L) {
            kotlinx.coroutines.delay(1500) // debounce: wait 1.5s after last keystroke
            if (dropoffAddress.length <= 5) return@LaunchedEffect
            geocodingDropoff = true
            geocodeAddress(context, dropoffAddress) { lat, lng ->
                if (lat != null && lng != null) {
                    dropoffLat = lat; dropoffLng = lng
                    distanceKm = null  // Wait for OSRM road route
                    fare = null
                }
                geocodingDropoff = false
            }
        }
    }

    // ─── Geocode pickup (debounced 1.5s) ──────────────
    var geocodingPickup by remember { mutableStateOf(false) }
    val geocodePickupKey = remember { mutableStateOf(0L) }
    LaunchedEffect(geocodePickupKey.value) {
        if (pickupAddress.length > 5 && pickupAddress != "Getting location..." && geocodePickupKey.value > 0L) {
            kotlinx.coroutines.delay(1500)
            if (pickupAddress.length <= 5) return@LaunchedEffect
            geocodingPickup = true
            geocodeAddress(context, pickupAddress) { lat, lng ->
                if (lat != null && lng != null) {
                    pickupLat = lat; pickupLng = lng
                    distanceKm = null
                    fare = null
                }
                geocodingPickup = false
            }
        }
    }

    fun triggerPickupGeocode() {
        geocodePickupKey.value = System.currentTimeMillis()
    }

    // Geocode trigger (no comma required, 1.5s delay)
    fun triggerGeocode() {
        geocodeKey.value = System.currentTimeMillis()
    }

    // --- Get road route via OSRM ---
    LaunchedEffect(pickupLat, pickupLng, dropoffLat, dropoffLng) {
        if (pickupLat != 0.0 && dropoffLat != null) {
            isRouting = true
            val route = RoadRouter.getRoute(pickupLat, pickupLng, dropoffLat!!, dropoffLng!!)
            if (route != null) {
                distanceKm = route.distanceKm
                durationMin = route.durationMinutes
                fare = Math.round(route.distanceKm * 100.0) / 100.0
                routePoints = route.routePoints
                updateMap(mapView, pickupLat, pickupLng, dropoffLat, dropoffLng, pickupAddress, dropoffAddress, route.routePoints)
            }
            isRouting = false
        }
    }

    // ─── UI ──────────────────────────────────────────────────
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
                    label = { Text("Ride") }, selected = true, onClick = {}
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    label = { Text("Schedule") }, selected = false, onClick = onNavigateSchedule
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("History") }, selected = false, onClick = onNavigateHistory
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Profile") }, selected = false, onClick = onNavigateProfile
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ─── MAP ─────────────────────────────────────────
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setTileSource(org.osmdroid.tileprovider.tilesource.XYTileSource(
                            "Satellite",
                            0, 19, 256,
                            ".png",
                            arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
                        ))
                        setMultiTouchControls(true)
                        controller.setZoom(16.0)
                        try {
                            zoomController.setVisibility(
                                org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT
                            )
                        } catch (e: Exception) {
                            setBuiltInZoomControls(true)
                        }
                        controller.setCenter(GeoPoint(pickupLat, pickupLng))
                        isTilesScaledToDpi = true
                        mapView = this

                        val mv = this
                        // ─── Long-press on map to set pickup ──
                        mv.apply {
                            val eo = org.osmdroid.views.overlay.MapEventsOverlay(object : org.osmdroid.events.MapEventsReceiver {
                                override fun longPressHelper(p: GeoPoint): Boolean {
                                    pickupLat = p.latitude
                                    pickupLng = p.longitude
                                    isLocating = true
                                    reverseGeocode(ctx, p.latitude, p.longitude) { addr ->
                                        pickupAddress = addr
                                        isLocating = false
                                    }
                                    if (dropoffLat != null) {
                                        val dist = haversine(p.latitude, p.longitude, dropoffLat!!, dropoffLng!!)
                                        distanceKm = dist
                                        fare = kotlin.math.round(dist * 100.0) / 100.0
                                    }
                                    updateMap(mv, pickupLat, pickupLng, dropoffLat, dropoffLng, pickupAddress, dropoffAddress)
                                    return true
                                }
                                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean { return false }
                            })
                            eventsOverlayRef = eo
                            overlays.add(0, eo)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // ─── COLLAPSIBLE BOTTOM CARD ────────────────────────
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column {
                        // Summary bar (always visible)
                        Surface(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (distanceKm != null && fare != null && !isRouting) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            durationMin?.let {
                                                Text("🚗 ${it} min", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                                Text("  ")
                                            }
                                            Text("🛣️ ${"%.1f".format(distanceKm)} km", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Text("${"%.2f".format(fare)}€", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    } else if (isRouting) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Calculating route...")
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                    } else {
                                        Text("Set pickup and destination", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { isExpanded = !isExpanded }) {
                                        Icon(
                                            if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                                        )
                                    }
                                }
                                if (!isExpanded) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Expand", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                        androidx.compose.animation.AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                                OutlinedTextField(
                                    value = pickupAddress,
                                    onValueChange = { newVal ->
                                        pickupAddress = newVal
                                        if (newVal.length > 5 && newVal != "Getting location...") triggerPickupGeocode()
                                    },
                                    label = { Text("Pickup") },
                                    leadingIcon = { Icon(Icons.Default.MyLocation, contentDescription = null) },
                                    trailingIcon = {
                                        if (geocodingPickup) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                        } else {
                                            IconButton(onClick = { triggerPickupGeocode() }) { Icon(Icons.Default.Search, contentDescription = "Find") }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                                    enabled = !isLocating && !geocodingPickup,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { triggerPickupGeocode() })
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = dropoffAddress,
                                    onValueChange = { newVal ->
                                        dropoffAddress = newVal
                                        if (newVal.length > 5) triggerGeocode()
                                    },
                                    label = { Text("Where to?") },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                    trailingIcon = {
                                        if (geocodingDropoff) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                        } else {
                                            IconButton(onClick = { triggerGeocode() }) { Icon(Icons.Default.Search, contentDescription = "Find") }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                                    enabled = !geocodingDropoff,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { triggerGeocode() })
                                )
                                errorMsg?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                                bookingResult?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(it, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (dropoffLat == null) { errorMsg = "Please enter a valid destination address"; return@Button }
                                        isBooking = true; errorMsg = null; bookingResult = null
                                        scope.launch {
                                            try {
                                                val roadFare = fare ?: 0.0; val roadDistance = distanceKm ?: 0.0; val roadMinutes = durationMin ?: 0
                                                val response = RetrofitClient.api.createBooking(BookingRequest(
                                                    pickupAddress = pickupAddress, pickupLat = pickupLat, pickupLng = pickupLng,
                                                    dropoffAddress = dropoffAddress, dropoffLat = dropoffLat!!, dropoffLng = dropoffLng!!,
                                                    type = "immediate", fare = roadFare, notes = "${roadDistance}km ${roadMinutes}min"
                                                ))
                                                if (response.isSuccessful && response.body() != null) {
                                                    val body = response.body()!!
                                                    if (body.booking != null) { bookingResult = "✅ Ride requested! Booking #${body.booking.id}"; onBookingCreated() }
                                                    else { errorMsg = body.error ?: "Booking failed" }
                                                } else { errorMsg = "Server error (${response.code()})" }
                                            } catch (e: Exception) { errorMsg = e.message ?: "Connection error" }
                                            finally { isBooking = false }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    enabled = !isBooking && dropoffAddress.isNotBlank() && dropoffLat != null,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    if (isBooking) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                                    else { Icon(Icons.Default.DirectionsCar, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("REQUEST RIDE", fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getCurrentLocation(
    context: Context,
    onLocation: (Double, Double) -> Unit
) {
    try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        for (provider in providers) {
            try {
                val loc = lm.getLastKnownLocation(provider)
                if (loc != null) {
                    onLocation(loc.latitude, loc.longitude)
                    return
                }
            } catch (_: Exception) {}
        }
        // Fallback Madrid
        onLocation(40.4168, -3.7038)
    } catch (e: Exception) {
        onLocation(40.4168, -3.7038)
    }
}

private fun reverseGeocode(
    context: Context,
    lat: Double,
    lng: Double,
    onResult: (String) -> Unit
) {
    try {
        val geocoder = Geocoder(context)
        val addresses = geocoder.getFromLocation(lat, lng, 1)
        if (!addresses.isNullOrEmpty()) {
            val addr = addresses[0]
            val lines = mutableListOf<String>()
            if (addr.thoroughfare != null) lines.add(addr.thoroughfare)
            if (addr.subThoroughfare != null) lines.add(addr.subThoroughfare)
            if (addr.locality != null) lines.add(addr.locality)
            val street = lines.joinToString(", ")
            onResult(street.ifEmpty {
                "${"%.4f".format(lat)}, ${"%.4f".format(lng)}"
            })
            return
        }
    } catch (_: Exception) {}
    onResult("${"%.4f".format(lat)}, ${"%.4f".format(lng)}")
}

private fun geocodeAddress(
    context: Context,
    address: String,
    onResult: (Double?, Double?) -> Unit
) {
    try {
        val geocoder = Geocoder(context)
        val results = geocoder.getFromLocationName(address, 1)
        if (!results.isNullOrEmpty()) {
            val loc = results[0]
            onResult(loc.latitude, loc.longitude)
            return
        }
    } catch (_: Exception) {}
    onResult(null, null)
}

private fun updateMap(
    mapView: MapView?,
    pickupLat: Double, pickupLng: Double,
    dropoffLat: Double?, dropoffLng: Double?,
    pickupAddr: String, dropoffAddr: String?,
    route: List<Pair<Double, Double>>? = null
) {
    mapView ?: return

    // Preserve events overlay, remove everything else
    val eventsOverlays = mapView.overlays.filter {
        it is org.osmdroid.views.overlay.MapEventsOverlay
    }
    mapView.overlays.clear()
    mapView.overlays.addAll(eventsOverlays)

    // Pickup marker
    val pickupMarker = Marker(mapView).apply {
        position = GeoPoint(pickupLat, pickupLng)
        title = "📍 Pickup"
        snippet = pickupAddr
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    }
    mapView.overlays.add(pickupMarker)

    // Dropoff marker
    if (dropoffLat != null && dropoffLng != null) {
        val dropMarker = Marker(mapView).apply {
            position = GeoPoint(dropoffLat, dropoffLng)
            title = "📍 Dropoff"
            snippet = dropoffAddr ?: ""
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(dropMarker)

        // Road route or straight line
        if (route != null && route.size >= 2) {
            val routeLine = Polyline().apply {
                for (pt in route) addPoint(GeoPoint(pt.first, pt.second))
                outlinePaint.strokeWidth = 6f
                outlinePaint.color = 0xFF1A73E8.toInt()
                outlinePaint.isAntiAlias = true
            }
            mapView.overlays.add(routeLine)
        } else {
            val straightLine = Polyline().apply {
                addPoint(GeoPoint(pickupLat, pickupLng))
                addPoint(GeoPoint(dropoffLat, dropoffLng))
                outlinePaint.strokeWidth = 3f
                outlinePaint.color = 0x88667788.toInt()
                // dotted effect not available on this Paint type
            }
            mapView.overlays.add(straightLine)
        }

        // Zoom to fit
        val points = if (route != null && route.size >= 2) {
            route.map { GeoPoint(it.first, it.second) }
        } else {
            listOf(GeoPoint(pickupLat, pickupLng), GeoPoint(dropoffLat, dropoffLng))
        }
        val bounds = org.osmdroid.util.BoundingBox.fromGeoPoints(points)
        mapView.zoomToBoundingBox(bounds.increaseByScale(1.3f), true)
    } else {
        mapView.controller.setCenter(GeoPoint(pickupLat, pickupLng))
        mapView.controller.setZoom(16.0)
    }

    mapView.invalidate()
}

private fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val sindLat = Math.sin(dLat / 2.0)
    val sindLng = Math.sin(dLng / 2.0)
    val a = sindLat * sindLat +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * sindLng * sindLng
    val c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a))
    return r * c
}
