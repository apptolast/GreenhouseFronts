package com.apptolast.greenhousefronts.data.remote.api

import com.apptolast.greenhousefronts.data.model.auth.ForgotPasswordRequest
import com.apptolast.greenhousefronts.data.model.auth.JwtResponse
import com.apptolast.greenhousefronts.data.model.auth.LoginRequest
import com.apptolast.greenhousefronts.data.model.auth.RegisterRequest
import com.apptolast.greenhousefronts.data.remote.baseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * API service for authentication operations.
 * Handles login and registration endpoints.
 *
 * Note: This service uses the unauthenticated HttpClient since
 * auth endpoints don't require Bearer tokens.
 *
 * @param httpClient Injected HTTP client (should be unauthenticated client)
 */
class AuthApiService(
    private val httpClient: HttpClient
) {

    /**
     * Authenticates a user with email and password.
     * POST /api/auth/login
     *
     * @param request Login credentials
     * @return JWT response containing access token and user info
     * @throws ClientRequestException 401 if credentials are invalid
     */
    suspend fun login(request: LoginRequest): JwtResponse {
        return httpClient.post("$baseUrl/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    /**
     * Registers a new tenant and admin user.
     * POST /api/auth/register
     *
     * Auto-logs in the user after successful registration.
     *
     * @param request Registration data including company and user info
     * @return JWT response containing access token and user info
     * @throws ClientRequestException 400 if validation fails or email is already in use
     */
    suspend fun register(request: RegisterRequest): JwtResponse {
        return httpClient.post("$baseUrl/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    /**
     * Requests a password reset email.
     * POST /api/auth/forgot-password
     *
     * @param request Request containing the user's email
     */
    suspend fun forgotPassword(request: ForgotPasswordRequest) {
        httpClient.post("$baseUrl/api/auth/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
