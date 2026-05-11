package com.apptolast.greenhousefronts.data.remote.api

import com.apptolast.greenhousefronts.data.model.auth.ForgotPasswordRequest
import com.apptolast.greenhousefronts.data.model.auth.JwtResponse
import com.apptolast.greenhousefronts.data.model.auth.LoginRequest
import com.apptolast.greenhousefronts.data.model.auth.RefreshRequest
import com.apptolast.greenhousefronts.data.model.auth.RegisterRequest
import com.apptolast.greenhousefronts.data.model.auth.ResetPasswordRequest
import com.apptolast.greenhousefronts.data.remote.baseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Auth endpoints. MUST be wired with the UNAUTHENTICATED HttpClient — `/refresh` would
 * otherwise recurse through the bearer plugin's 401 handler.
 */
open class AuthApiService(private val httpClient: HttpClient) {

    open suspend fun login(request: LoginRequest): JwtResponse =
        httpClient.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    open suspend fun register(request: RegisterRequest): JwtResponse =
        httpClient.post("$baseUrl/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    open suspend fun forgotPassword(request: ForgotPasswordRequest) {
        httpClient.post("$baseUrl/auth/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    open suspend fun resetPassword(request: ResetPasswordRequest) {
        httpClient.post("$baseUrl/auth/reset-password") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    open suspend fun logout() {
        httpClient.post("$baseUrl/auth/logout")
    }

    /**
     * Rotates the refresh token. Identifies the user by the opaque token in the body —
     * no `Authorization` header. Returns a new access + refresh pair; the caller MUST
     * replace the stored refresh or reuse-detection will revoke the family on the next call.
     *
     * Failure modes (Ktor `expectSuccess = true` raises these):
     *  - `ClientRequestException` 400 (malformed) or 401 (invalid/expired/revoked/reused).
     *  - `ServerResponseException` 503 if `REFRESH_TOKEN_ENABLED=false` on the backend.
     */
    open suspend fun refresh(request: RefreshRequest): JwtResponse =
        httpClient.post("$baseUrl/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
