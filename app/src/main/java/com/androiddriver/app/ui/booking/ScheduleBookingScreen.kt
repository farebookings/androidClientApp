package com.androiddriver.app.ui.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleBookingScreen(onBack: () -> Unit) {
    var pickupAddress by remember { mutableStateOf("") }
    var dropoffAddress by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var selectedTime by remember { mutableStateOf("09:00") }
    var notes by remember { mutableStateOf("") }
    var bookingConfirmed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📅 Schedule a Ride", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Book a ride in advance. The taxi company will confirm your booking.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pickup
            Text("Pickup Location", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = pickupAddress,
                onValueChange = { pickupAddress = it },
                placeholder = { Text("Enter pickup address") },
                leadingIcon = { Icon(Icons.Default.MyLocation, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Dropoff
            Text("Destination", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = dropoffAddress,
                onValueChange = { dropoffAddress = it },
                placeholder = { Text("Enter destination") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Date
            Text("Date", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = selectedDate,
                onValueChange = { selectedDate = it },
                placeholder = { Text("YYYY-MM-DD") },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("Format: YYYY-MM-DD (e.g. 2026-04-30)") }
            )

            // Time
            Text("Time", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = selectedTime,
                onValueChange = { selectedTime = it },
                placeholder = { Text("HH:MM") },
                leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("Format: HH:MM (e.g. 09:00 or 14:30)") }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Notes
            Text("Notes (optional)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text("Any special requests...") },
                modifier = Modifier.fillMaxWidth().height(100.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Result
            if (bookingConfirmed) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "✅ Booking Request Sent!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            "Pickup on $selectedDate at $selectedTime.\nThe taxi company will confirm shortly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        bookingConfirmed = true
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    enabled = pickupAddress.isNotBlank() && dropoffAddress.isNotBlank()
                            && selectedDate.isNotBlank() && selectedTime.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SCHEDULE BOOKING", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
