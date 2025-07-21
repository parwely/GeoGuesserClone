package com.example.geogeusserclone.data.network

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<AuthResponse>

    @GET("games/locations")
    suspend fun getRandomLocation(): Response<LocationResponse>

    // Weitere API Endpoints hier hinzufügen
}

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val username: String, val email: String, val password: String)
data class AuthResponse(val token: String, val user: User)
data class LocationResponse(val latitude: Double, val longitude: Double, val imageUrl: String)
data class User(val id: String, val username: String, val email: String)