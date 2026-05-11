package com.apptolast.greenhousefronts.testutil

import com.apptolast.greenhousefronts.data.model.auth.ForgotPasswordRequest
import com.apptolast.greenhousefronts.data.model.auth.JwtResponse
import com.apptolast.greenhousefronts.data.model.auth.LoginRequest
import com.apptolast.greenhousefronts.data.model.auth.RefreshRequest
import com.apptolast.greenhousefronts.data.model.auth.RegisterRequest
import com.apptolast.greenhousefronts.data.model.auth.ResetPasswordRequest
import com.apptolast.greenhousefronts.data.remote.api.AuthApiService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode

/**
 * Programmable fake for [AuthApiService]. Overrides each suspend endpoint with a slot
 * that the test can set per scenario. Tracks call counts so concurrency tests can assert
 * coalescing behaviour.
 *
 * Subclassing is possible because `AuthApiService` exposes its endpoints as `open suspend
 * fun`. The base class still requires an HttpClient — we pass one backed by a no-op
 * MockEngine that throws on any unexpected request.
 */
class FakeAuthApiService(
    var refreshHandler: suspend (RefreshRequest) -> JwtResponse = { error("refreshHandler not set") },
    var loginHandler: suspend (LoginRequest) -> JwtResponse = { error("loginHandler not set") },
) : AuthApiService(httpClient = unusedClient()) {

    var refreshCallCount: Int = 0
        private set
    var loginCallCount: Int = 0
        private set

    override suspend fun refresh(request: RefreshRequest): JwtResponse {
        refreshCallCount++
        return refreshHandler(request)
    }

    override suspend fun login(request: LoginRequest): JwtResponse {
        loginCallCount++
        return loginHandler(request)
    }

    override suspend fun register(request: RegisterRequest): JwtResponse =
        error("register not stubbed")

    override suspend fun forgotPassword(request: ForgotPasswordRequest) {
        error("forgotPassword not stubbed")
    }

    override suspend fun resetPassword(request: ResetPasswordRequest) {
        error("resetPassword not stubbed")
    }

    override suspend fun logout() {
        // Tests don't care about logout HTTP; no-op silently.
    }

    companion object {
        /** Backing HttpClient that throws if any request is attempted — failsafe for fakes. */
        private fun unusedClient(): HttpClient = HttpClient(
            MockEngine { _ ->
                respondError(
                    status = HttpStatusCode.NotImplemented,
                    content = "Fake AuthApiService should not perform HTTP",
                )
            },
        )
    }
}
