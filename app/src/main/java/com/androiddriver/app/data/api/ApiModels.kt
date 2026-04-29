package com.androiddriver.app.data.api

import com.google.gson.annotations.SerializedName

// ─── Auth ───────────────────────────────────────────────────
data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(
    val name: String,
    val email: String,
    val phone: String,
    val password: String,
    val role: String = "client"
)

data class AuthResponse(
    val user: UserDto?,
    val token: String?,
    val error: String?
)

data class UserDto(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String,
    val role: String
)

// ─── Bookings ───────────────────────────────────────────────
data class BookingRequest(
    @SerializedName("pickup_address") val pickupAddress: String,
    @SerializedName("pickup_lat") val pickupLat: Double,
    @SerializedName("pickup_lng") val pickupLng: Double,
    @SerializedName("dropoff_address") val dropoffAddress: String,
    @SerializedName("dropoff_lat") val dropoffLat: Double,
    @SerializedName("dropoff_lng") val dropoffLng: Double,
    val type: String,
    @SerializedName("scheduled_date") val scheduledDate: String? = null,
    val notes: String? = null
)

data class EstimateRequest(
    @SerializedName("pickup_lat") val pickupLat: Double,
    @SerializedName("pickup_lng") val pickupLng: Double,
    @SerializedName("dropoff_lat") val dropoffLat: Double,
    @SerializedName("dropoff_lng") val dropoffLng: Double
)

data class BookingResponse(val booking: BookingDto?, val bookings: List<BookingDto>?, val error: String?)
data class EstimateResponse(val fare: Double?, val error: String?)
data class StatusResponse(val message: String?)

data class BookingDto(
    val id: Int,
    @SerializedName("pickup_address") val pickupAddress: String,
    @SerializedName("pickup_lat") val pickupLat: String,
    @SerializedName("pickup_lng") val pickupLng: String,
    @SerializedName("dropoff_address") val dropoffAddress: String,
    @SerializedName("dropoff_lat") val dropoffLat: String,
    @SerializedName("dropoff_lng") val dropoffLng: String,
    val type: String,
    @SerializedName("scheduled_date") val scheduledDate: String?,
    val status: String,
    val fare: String?,
    @SerializedName("client_name") val clientName: String?,
    @SerializedName("driver_name") val driverName: String?,
    @SerializedName("created_at") val createdAt: String
)

// ─── Drivers ────────────────────────────────────────────────
data class NearbyRequest(val lat: Double, val lng: Double)
data class LocationRequest(val lat: Double, val lng: Double)
data class StatusRequest(val status: String)

data class NearbyResponse(val drivers: List<DriverDto>?, val error: String?)

data class DriverDto(
    val id: Int,
    @SerializedName("driver_name") val driverName: String,
    val phone: String,
    val distance: String?,
    val status: String,
    @SerializedName("current_lat") val currentLat: String?,
    @SerializedName("current_lng") val currentLng: String?,
    val make: String?,
    val model: String?,
    val plate: String?,
    val color: String?,
    val seats: Int?
)

data class DriverProfileResponse(
    val driver: DriverProfileDto?,
    val vehicle: VehicleDto?,
    val error: String?
)

data class DriverProfileDto(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("license_number") val licenseNumber: String,
    val status: String,
    @SerializedName("driver_name") val driverName: String?
)

data class VehicleDto(
    @SerializedName("driver_id") val driverId: Int,
    val make: String,
    val model: String,
    val plate: String,
    val color: String,
    val seats: Int
)
