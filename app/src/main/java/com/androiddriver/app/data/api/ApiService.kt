package com.androiddriver.app.data.api

import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // ─── AUTH ──────────────────────────────────────────────
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @GET("api/auth/profile")
    suspend fun profile(): Response<AuthResponse>

    // ─── BOOKINGS ──────────────────────────────────────────
    @POST("api/bookings")
    suspend fun createBooking(@Body request: BookingRequest): Response<BookingResponse>

    @GET("api/bookings")
    suspend fun myBookings(): Response<BookingResponse>

    @GET("api/bookings/{id}")
    suspend fun getBooking(@Path("id") id: Int): Response<BookingResponse>

    @PATCH("api/bookings/{id}/cancel")
    suspend fun cancelBooking(@Path("id") id: Int): Response<StatusResponse>

    @POST("api/bookings/estimate")
    suspend fun estimateFare(@Body request: EstimateRequest): Response<EstimateResponse>

    // ─── DRIVERS ───────────────────────────────────────────
    @POST("api/drivers/nearby")
    suspend fun nearbyDrivers(@Body request: NearbyRequest): Response<NearbyResponse>

    @POST("api/drivers/register")
    suspend fun registerDriver(@Body body: Map<String, String>): Response<Unit>

    @POST("api/drivers/vehicle")
    suspend fun registerVehicle(@Body body: Map<String, String>): Response<Unit>

    @POST("api/drivers/location")
    suspend fun updateLocation(@Body request: LocationRequest): Response<StatusResponse>

    @POST("api/drivers/status")
    suspend fun setStatus(@Body request: StatusRequest): Response<StatusResponse>
}
