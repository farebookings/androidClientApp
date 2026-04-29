package com.androiddriver.app.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    // Sample history data (real app fetches from API)
    val sampleBookings = remember {
        listOf(
            SampleBooking(1, "Calle Mayor 10", "Aeropuerto MAD", "completed", "25.50€", "Today, 10:30"),
            SampleBooking(2, "Gran Vía 23", "Plaza Sol 5", "completed", "8.20€", "Today, 08:15"),
            SampleBooking(3, "Calle Alcalá 40", "Estación Atocha", "cancelled", "—", "Yesterday"),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📋 Ride History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (sampleBookings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No rides yet", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sampleBookings) { booking ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            // Status icon
                            Icon(
                                imageVector = when (booking.status) {
                                    "completed" -> Icons.Default.CheckCircle
                                    "cancelled" -> Icons.Default.Cancel
                                    else -> Icons.Default.HourglassEmpty
                                },
                                contentDescription = null,
                                tint = when (booking.status) {
                                    "completed" -> MaterialTheme.colorScheme.primary
                                    "cancelled" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    booking.from + " → " + booking.to,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    booking.date,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    booking.fare,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    booking.status.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (booking.status) {
                                        "completed" -> MaterialTheme.colorScheme.primary
                                        "cancelled" -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class SampleBooking(
    val id: Int,
    val from: String,
    val to: String,
    val status: String,
    val fare: String,
    val date: String
)
