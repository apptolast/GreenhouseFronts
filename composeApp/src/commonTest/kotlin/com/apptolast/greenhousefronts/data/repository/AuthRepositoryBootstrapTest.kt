package com.apptolast.greenhousefronts.data.repository

import com.apptolast.greenhousefronts.data.model.auth.JwtResponse
import com.apptolast.greenhousefronts.domain.model.AuthState
import com.apptolast.greenhousefronts.testutil.FakeAuthApiService
import com.apptolast.greenhousefronts.testutil.InMemoryTokenStorage
import com.apptolast.greenhousefronts.testutil.TestClock
import com.apptolast.greenhousefronts.testutil.TestClock.Companion.DEFAULT_NOW
import com.apptolast.greenhousefronts.testutil.fakeJwt
import com.apptolast.greenhousefronts.testutil.throwKtorHttpError
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

/**
 * Exercises every branch of [AuthRepositoryImpl.bootstrap] using a [TestClock] so we can
 * simulate "X hours after login" without sleeping. The 24-hour regression scenario the
 * user reported is covered by [bootstrap_expiredAccess_refreshSucceeds_emitsAuthenticated].
 */
@OptIn(ExperimentalTime::class)
class AuthRepositoryBootstrapTest {

    private val accessTtl = 3_600L
    private val refreshTtl = 30L * 24 * 3_600L

    @Test
    fun bootstrap_noToken_emitsInitial() = runTest {
        val storage = InMemoryTokenStorage()
        val api = FakeAuthApiService()
        val clock = TestClock()
        val repo = AuthRepositoryImpl(api, storage, clock)

        repo.bootstrap()

        val state = repo.authState.first { it !is AuthState.Loading }
        val unauthenticated = assertIs<AuthState.Unauthenticated>(state)
        assertEquals(AuthState.Reason.INITIAL, unauthenticated.reason)
        assertEquals(0, api.refreshCallCount)
    }

    @Test
    fun bootstrap_healthyToken_emitsAuthenticated_withoutRefresh() = runTest {
        val accessToken = fakeJwt(expEpochSec = DEFAULT_NOW + accessTtl)
        val storage = InMemoryTokenStorage(
            initialAccessToken = accessToken,
            initialRefreshToken = "refresh-1",
            initialRefreshExpiry = DEFAULT_NOW + refreshTtl,
        )
        val api = FakeAuthApiService()
        val repo = AuthRepositoryImpl(api, storage, TestClock())

        repo.bootstrap()

        val state = repo.authState.first { it !is AuthState.Loading }
        val authed = assertIs<AuthState.Authenticated>(state)
        assertEquals(accessToken, authed.token)
        assertEquals(0, api.refreshCallCount)
    }

    @Test
    fun bootstrap_tokenNearExpiry_forcesRefresh() = runTest {
        // Token has 4 min left → inside the 5-min NEAR_EXPIRY window. Bootstrap MUST refresh
        // proactively rather than emitting Authenticated with a soon-dead bearer.
        val oldAccess = fakeJwt(expEpochSec = DEFAULT_NOW + 240)
        val newAccess = fakeJwt(expEpochSec = DEFAULT_NOW + accessTtl)
        val storage = InMemoryTokenStorage(
            initialAccessToken = oldAccess,
            initialRefreshToken = "refresh-1",
            initialRefreshExpiry = DEFAULT_NOW + refreshTtl,
        )
        val api = FakeAuthApiService(
            refreshHandler = {
                JwtResponse(
                    token = newAccess,
                    username = "user@example.com",
                    refreshToken = "refresh-2",
                    expiresIn = accessTtl,
                    refreshExpiresIn = refreshTtl,
                )
            },
        )
        val repo = AuthRepositoryImpl(api, storage, TestClock())

        repo.bootstrap()

        assertEquals(1, api.refreshCallCount)
        val state = repo.authState.first { it !is AuthState.Loading }
        val authed = assertIs<AuthState.Authenticated>(state)
        assertEquals(newAccess, authed.token)
        assertEquals(newAccess, storage.getToken())
        assertEquals("refresh-2", storage.getRefreshToken())
    }

    @Test
    fun bootstrap_expiredAccess_refreshSucceeds_emitsAuthenticated() = runTest {
        // The 24h regression scenario: access expired ~23h ago, refresh still valid.
        val staleAccess = fakeJwt(expEpochSec = DEFAULT_NOW - 23 * 3_600)
        val freshAccess = fakeJwt(expEpochSec = DEFAULT_NOW + accessTtl)
        val storage = InMemoryTokenStorage(
            initialAccessToken = staleAccess,
            initialRefreshToken = "refresh-1",
            initialRefreshExpiry = DEFAULT_NOW + 7 * 24 * 3_600, // still 7 days left
        )
        val api = FakeAuthApiService(
            refreshHandler = {
                assertEquals("refresh-1", it.refreshToken)
                JwtResponse(
                    token = freshAccess,
                    username = "user@example.com",
                    refreshToken = "refresh-2",
                    expiresIn = accessTtl,
                    refreshExpiresIn = refreshTtl,
                )
            },
        )
        val repo = AuthRepositoryImpl(api, storage, TestClock())

        repo.bootstrap()

        assertEquals(1, api.refreshCallCount)
        val state = repo.authState.first { it !is AuthState.Loading }
        val authed = assertIs<AuthState.Authenticated>(state)
        assertEquals(freshAccess, authed.token)
        assertNotNull(authed.expiresAtEpochSec)
    }

    @Test
    fun bootstrap_expiredAccess_refreshAlsoExpired_emitsExpired() = runTest {
        val staleAccess = fakeJwt(expEpochSec = DEFAULT_NOW - 1_000)
        val storage = InMemoryTokenStorage(
            initialAccessToken = staleAccess,
            initialRefreshToken = "refresh-1",
            initialRefreshExpiry = DEFAULT_NOW - 100, // refresh ALSO expired
        )
        val api = FakeAuthApiService()
        val repo = AuthRepositoryImpl(api, storage, TestClock())

        repo.bootstrap()

        val state = repo.authState.first { it !is AuthState.Loading }
        val unauth = assertIs<AuthState.Unauthenticated>(state)
        assertEquals(AuthState.Reason.EXPIRED, unauth.reason)
        assertEquals(0, api.refreshCallCount)
        assertEquals(1, storage.clearAllCount)
    }

    @Test
    fun bootstrap_expiredAccess_refresh401_invalidatesAndClearsRefresh() = runTest {
        val staleAccess = fakeJwt(expEpochSec = DEFAULT_NOW - 1_000)
        val storage = InMemoryTokenStorage(
            initialAccessToken = staleAccess,
            initialRefreshToken = "refresh-1",
            initialRefreshExpiry = DEFAULT_NOW + refreshTtl,
        )
        val api = FakeAuthApiService(refreshHandler = { throwKtorHttpError(HttpStatusCode.Unauthorized) })
        val repo = AuthRepositoryImpl(api, storage, TestClock())

        repo.bootstrap()

        val state = repo.authState.first { it !is AuthState.Loading }
        val unauth = assertIs<AuthState.Unauthenticated>(state)
        assertEquals(AuthState.Reason.EXPIRED, unauth.reason)
        assertEquals(1, api.refreshCallCount)
        // clearRefreshToken was called before clearAll (invalidateSession), so both fire.
        assertTrue(storage.clearRefreshTokenCount >= 1)
    }

    @Test
    fun bootstrap_expiredAccess_refreshIoException_keepsTokensAndStaysLoading() = runTest {
        val staleAccess = fakeJwt(expEpochSec = DEFAULT_NOW - 1_000)
        val storage = InMemoryTokenStorage(
            initialAccessToken = staleAccess,
            initialRefreshToken = "refresh-1",
            initialRefreshExpiry = DEFAULT_NOW + refreshTtl,
        )
        val api = FakeAuthApiService(refreshHandler = { throw IOException("network down") })
        val repo = AuthRepositoryImpl(api, storage, TestClock())

        repo.bootstrap()

        // Transient I/O failure: authState stays Loading so the next caller can retry.
        assertEquals(AuthState.Loading, repo.authState.value)
        assertEquals(1, api.refreshCallCount)
        assertEquals("refresh-1", storage.getRefreshToken()) // refresh preserved
        assertEquals(staleAccess, storage.getToken()) // access also preserved
    }

    @Test
    fun bootstrap_expiredAccess_refresh503_invalidatesButKeepsRefresh() = runTest {
        val staleAccess = fakeJwt(expEpochSec = DEFAULT_NOW - 1_000)
        val storage = InMemoryTokenStorage(
            initialAccessToken = staleAccess,
            initialRefreshToken = "refresh-1",
            initialRefreshExpiry = DEFAULT_NOW + refreshTtl,
        )
        val api = FakeAuthApiService(refreshHandler = { throwKtorHttpError(HttpStatusCode.ServiceUnavailable) })
        val repo = AuthRepositoryImpl(api, storage, TestClock())

        repo.bootstrap()

        val state = repo.authState.first { it !is AuthState.Loading }
        val unauth = assertIs<AuthState.Unauthenticated>(state)
        assertEquals(AuthState.Reason.EXPIRED, unauth.reason)
        // 5xx keeps refresh: clearRefreshToken NOT called (only clearAll via invalidate).
        assertEquals(0, storage.clearRefreshTokenCount)
    }
}
