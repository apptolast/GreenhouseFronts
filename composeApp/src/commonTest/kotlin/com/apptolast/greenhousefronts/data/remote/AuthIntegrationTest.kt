package com.apptolast.greenhousefronts.data.remote

import com.apptolast.greenhousefronts.data.remote.api.AuthApiService
import com.apptolast.greenhousefronts.data.repository.AuthRepositoryImpl
import com.apptolast.greenhousefronts.testutil.InMemoryTokenStorage
import com.apptolast.greenhousefronts.testutil.TestClock
import com.apptolast.greenhousefronts.testutil.TestClock.Companion.DEFAULT_NOW
import com.apptolast.greenhousefronts.testutil.fakeJwt
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

/**
 * End-to-end exercise of the bearer-refresh pipeline using Ktor MockEngine on BOTH the
 * authenticated and unauthenticated clients. This is the closest we can get to "24h after
 * login" without standing up the real backend.
 *
 * The scenario:
 *  1. Storage has a stale access token + a usable refresh.
 *  2. A protected GET fires with the stale bearer → MockEngine returns 401.
 *  3. Ktor's bearer plugin invokes `refreshTokens` → AuthRepositoryImpl POSTs /auth/refresh
 *     on the unauthenticated client → MockEngine returns 200 with a fresh token pair.
 *  4. Ktor transparently retries the original GET with the new bearer → 200 OK.
 *
 * Coalescing is also exercised: two parallel GETs should produce a single /auth/refresh.
 */
@OptIn(ExperimentalTime::class)
class AuthIntegrationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }
    private val accessTtl = 3_600L
    private val refreshTtl = 30L * 24 * 3_600L

    private fun freshAccessJwt() = fakeJwt(expEpochSec = DEFAULT_NOW + accessTtl)

    private val staleAccess = fakeJwt(expEpochSec = DEFAULT_NOW - 1_000)

    /** Counts how many times the unauthenticated MockEngine saw `/auth/refresh`. */
    private var refreshHits = 0

    /** Counts how many times the authenticated MockEngine saw a protected request. */
    private var protectedHits = 0

    @Test
    fun expiredBearer_triggersTransparentRefresh_andRetriesOriginalRequest() = runTest {
        val newAccess = freshAccessJwt()

        val authEngine = MockEngine { request ->
            // Simulates POST /auth/refresh — only path the unauthenticated client should hit.
            assertTrue(request.url.encodedPath.endsWith("/auth/refresh"))
            refreshHits++
            respondJson(
                """
                {
                    "token": "$newAccess",
                    "type": "Bearer",
                    "username": "user@example.com",
                    "refreshToken": "refresh-2",
                    "expiresIn": $accessTtl,
                    "refreshExpiresIn": $refreshTtl
                }
                """.trimIndent(),
            )
        }
        val unauthClient = createUnauthenticatedHttpClient(jsonConfig = json, engine = authEngine)
        val authApi = AuthApiService(unauthClient)

        val storage = InMemoryTokenStorage(
            initialAccessToken = staleAccess,
            initialRefreshToken = "refresh-1",
            initialRefreshExpiry = DEFAULT_NOW + refreshTtl,
        )
        val repo = AuthRepositoryImpl(authApi, storage, TestClock())

        val protectedEngine = MockEngine { request ->
            protectedHits++
            val bearer = request.headers[HttpHeaders.Authorization]
            if (bearer == "Bearer $newAccess") {
                respondJson("""{"ok":true}""")
            } else {
                respondError(HttpStatusCode.Unauthorized)
            }
        }
        val protectedClient = createAuthenticatedHttpClient(
            jsonConfig = json,
            tokenStorage = storage,
            sessionInvalidator = repo,
            engine = protectedEngine,
            sendWithoutRequestHostMatch = "test.local",
        )

        val body = protectedClient.get("http://test.local/api/v1/greenhouses").bodyAsText()

        assertEquals("""{"ok":true}""", body)
        assertEquals(1, refreshHits)
        // Initial 401 + retried 200 = 2 hits on the protected mock.
        assertEquals(2, protectedHits)
        assertEquals(newAccess, storage.getToken())
        assertEquals("refresh-2", storage.getRefreshToken())
    }

    @Test
    fun refresh401_propagatesAsUnauthorized_andInvalidatesSession() = runTest {
        val authEngine = MockEngine { _ ->
            refreshHits++
            respondError(HttpStatusCode.Unauthorized)
        }
        val unauthClient = createUnauthenticatedHttpClient(jsonConfig = json, engine = authEngine)
        val authApi = AuthApiService(unauthClient)

        val storage = InMemoryTokenStorage(
            initialAccessToken = staleAccess,
            initialRefreshToken = "refresh-1",
            initialRefreshExpiry = DEFAULT_NOW + refreshTtl,
        )
        val repo = AuthRepositoryImpl(authApi, storage, TestClock())

        val protectedEngine = MockEngine { _ ->
            protectedHits++
            respondError(HttpStatusCode.Unauthorized)
        }
        val protectedClient = createAuthenticatedHttpClient(
            jsonConfig = json,
            tokenStorage = storage,
            sessionInvalidator = repo,
            engine = protectedEngine,
            sendWithoutRequestHostMatch = "test.local",
        )

        assertFailsWith<ClientRequestException> {
            protectedClient.get("http://test.local/api/v1/greenhouses")
        }
        assertEquals(1, refreshHits, "/auth/refresh fired once and was rejected")
        // Storage should now be clear (terminal 4xx → clearRefreshToken + invalidateSession→clearAll)
        assertEquals(null, storage.getRefreshToken())
        assertEquals(null, storage.getToken())
    }

    @Test
    fun twoConcurrentProtectedRequests_coalesceIntoASingleRefresh() = runTest {
        val newAccess = freshAccessJwt()
        val authEngine = MockEngine { _ ->
            refreshHits++
            respondJson(
                """
                {
                    "token": "$newAccess",
                    "type": "Bearer",
                    "username": "user@example.com",
                    "refreshToken": "refresh-2",
                    "expiresIn": $accessTtl,
                    "refreshExpiresIn": $refreshTtl
                }
                """.trimIndent(),
            )
        }
        val unauthClient = createUnauthenticatedHttpClient(jsonConfig = json, engine = authEngine)
        val authApi = AuthApiService(unauthClient)

        val storage = InMemoryTokenStorage(
            initialAccessToken = staleAccess,
            initialRefreshToken = "refresh-1",
            initialRefreshExpiry = DEFAULT_NOW + refreshTtl,
        )
        val repo = AuthRepositoryImpl(authApi, storage, TestClock())

        val protectedEngine = MockEngine { request ->
            protectedHits++
            val bearer = request.headers[HttpHeaders.Authorization]
            if (bearer == "Bearer $newAccess") respondJson("""{"ok":true}""")
            else respondError(HttpStatusCode.Unauthorized)
        }
        val protectedClient = createAuthenticatedHttpClient(
            jsonConfig = json,
            tokenStorage = storage,
            sessionInvalidator = repo,
            engine = protectedEngine,
            sendWithoutRequestHostMatch = "test.local",
        )

        val (bodyA, bodyB) = awaitAll(
            async { protectedClient.get("http://test.local/api/v1/a").bodyAsText() },
            async { protectedClient.get("http://test.local/api/v1/b").bodyAsText() },
        )

        assertEquals("""{"ok":true}""", bodyA)
        assertEquals("""{"ok":true}""", bodyB)
        // Both concurrent callers must collapse onto a single /auth/refresh round-trip.
        assertEquals(1, refreshHits)
    }

    @Test
    fun staleBearer_returning403_triggersRefresh_andRetries() = runTest {
        // The production bug: backend (Spring Security) returns 403 (NOT 401) for an
        // expired/invalid JWT. Ktor's stock bearer plugin only listens for 401 — so the
        // old setup never noticed and kept sending the stale bearer forever. The HttpSend
        // interceptor must treat 403 as a refresh-trigger too.
        val newAccess = freshAccessJwt()
        val authEngine = MockEngine { _ ->
            refreshHits++
            respondJson(
                """
                {
                    "token": "$newAccess",
                    "type": "Bearer",
                    "username": "user@example.com",
                    "refreshToken": "refresh-2",
                    "expiresIn": $accessTtl,
                    "refreshExpiresIn": $refreshTtl
                }
                """.trimIndent(),
            )
        }
        val unauthClient = createUnauthenticatedHttpClient(jsonConfig = json, engine = authEngine)
        val authApi = AuthApiService(unauthClient)

        val storage = InMemoryTokenStorage(
            initialAccessToken = staleAccess,
            initialRefreshToken = "refresh-1",
            initialRefreshExpiry = DEFAULT_NOW + refreshTtl,
        )
        val repo = AuthRepositoryImpl(authApi, storage, TestClock())

        val protectedEngine = MockEngine { request ->
            protectedHits++
            val bearer = request.headers[HttpHeaders.Authorization]
            if (bearer == "Bearer $newAccess") respondJson("""{"ok":true}""")
            else respondError(HttpStatusCode.Forbidden) // ← backend's behaviour on expired JWT
        }
        val protectedClient = createAuthenticatedHttpClient(
            jsonConfig = json,
            tokenStorage = storage,
            sessionInvalidator = repo,
            engine = protectedEngine,
            sendWithoutRequestHostMatch = "test.local",
        )

        val body = protectedClient.get("http://test.local/api/v1/greenhouses").bodyAsText()

        assertEquals("""{"ok":true}""", body)
        assertEquals(1, refreshHits, "403 should have triggered exactly one refresh")
        assertEquals(
            2,
            protectedHits,
            "request issued twice: once with stale bearer (403), once retried with fresh (200)"
        )
        assertEquals(newAccess, storage.getToken())
    }

    @Test
    fun bearerRotatedExternallyInStorage_isPickedUpWithoutInternalCacheStaleness() = runTest {
        // Regression for the bug we hit in production: the old bearer plugin cached the
        // result of `loadTokens` and ignored any later writes to TokenStorage. The new
        // HttpSend interceptor reads storage on every request, so an external rotation
        // (e.g. `bootstrap()` silent-refresh) is reflected on the very next call without
        // needing a 401/403 round-trip.
        val tokenV1 = fakeJwt(expEpochSec = DEFAULT_NOW + accessTtl, firstName = "v1")
        val tokenV2 = fakeJwt(expEpochSec = DEFAULT_NOW + accessTtl, firstName = "v2")

        val authEngine = MockEngine { _ -> error("Auth endpoints should not be hit in this test") }
        val unauthClient = createUnauthenticatedHttpClient(jsonConfig = json, engine = authEngine)
        val authApi = AuthApiService(unauthClient)

        val storage = InMemoryTokenStorage(initialAccessToken = tokenV1)
        val repo = AuthRepositoryImpl(authApi, storage, TestClock())

        val seenBearers = mutableListOf<String?>()
        val protectedEngine = MockEngine { request ->
            seenBearers += request.headers[HttpHeaders.Authorization]
            respondJson("""{"ok":true}""")
        }
        val protectedClient = createAuthenticatedHttpClient(
            jsonConfig = json,
            tokenStorage = storage,
            sessionInvalidator = repo,
            engine = protectedEngine,
            sendWithoutRequestHostMatch = "test.local",
        )

        protectedClient.get("http://test.local/api/v1/greenhouses")
        // External rotation — exactly what `persistSuccessfulAuth` does after a bootstrap refresh.
        storage.saveToken(tokenV2)
        protectedClient.get("http://test.local/api/v1/greenhouses")

        assertEquals(listOf("Bearer $tokenV1", "Bearer $tokenV2"), seenBearers.toList())
    }

    private fun MockRequestHandleScope.respondJson(content: String): HttpResponseData = respond(
        content = content,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )
}
