package com.example.geogeusserclone.data.network

import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<AuthResponse>

    @GET("api/game/location")
    suspend fun getRandomLocation(): Response<LocationResponse>

    @POST("api/game/guess")
    suspend fun submitGuess(@Body guessRequest: GuessRequest): Response<GuessResponse>
}

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val username: String, val email: String, val password: String)
data class AuthResponse(val token: String, val user: UserDto)
data class LocationResponse(val id: String, val imageUrl: String, val latitude: Double, val longitude: Double)
data class GuessRequest(val gameId: String, val latitude: Double, val longitude: Double)
data class GuessResponse(val score: Int, val distance: Double, val correct: Boolean)
data class UserDto(val id: String, val username: String, val email: String)