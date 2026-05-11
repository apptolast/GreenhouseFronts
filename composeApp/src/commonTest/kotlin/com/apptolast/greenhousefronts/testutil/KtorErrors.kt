package com.apptolast.greenhousefronts.testutil

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode

/**
 * Fires a one-shot HTTP request through a [MockEngine] that responds with [status], so the
 * `expectSuccess = true` Ktor client surfaces a real [ClientRequestException] /
 * [ServerResponseException] — the same exception type production code catches in
 * `AuthRepositoryImpl.tryRefreshOrInvalidate`.
 *
 * Constructing those exceptions directly is awkward because `HttpResponse` is abstract
 * with many platform-specific members; this is the path of least resistance.
 */
suspend fun throwKtorHttpError(status: HttpStatusCode): Nothing {
    val client = HttpClient(MockEngine { respondError(status) }) {
        expectSuccess = true
    }
    try {
        client.post("http://test.local/dummy")
    } finally {
        client.close()
    }
    // Either ClientRequestException (4xx) or ServerResponseException (5xx) was thrown above;
    // execution never reaches this line.
    error("unreachable: $status should have thrown")
}
