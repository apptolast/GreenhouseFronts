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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.time.ExperimentalTime

/**
 * Covers the two coalescing invariants that broke before:
 *  1. Concurrent callers (Ktor 401 + WS reconnect) must collapse to a single
 *     `/auth/refresh` round-trip.
 *  2. A second caller that arrives AFTER another has already failed-and-completed must
 *     itself issue a new refresh — even if storage still holds a "valid-looking" token —
 *     because that token may be the same one the backend just rejected (root cause of
 *     the 24h-403 WebSocket loop).
 */
@OptIn(ExperimentalTime::class)
class TryRefreshOrInvalidateCoalescingTest {

    private val accessTtl = 3_600L
    private val refreshTtl = 30L * 24 * 3_600L

    @Test
    fun concurrentCallers_collapseToASingleRefresh() = runTest {
        val staleAccess = fakeJwt(expEpochSec = DEFAULT_NOW - 1_000)
        val freshAccess = fakeJwt(expEpochSec = DEFAULT_NOW + accessTtl)
        val storage = InMemoryTokenStorage(
            initialAccessToken = staleAccess,
            initialRefreshToken = "refresh-1",
            initialRefreshExpiry = DEFAULT_NOW + refreshTtl,
        )

        // A gate that lets the test fire BOTH callers before the refresh API responds.
        val gate = CompletableDeferred<Unit>()
        val api = FakeAuthApiService(
            refreshHandler = {
                gate.await()
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

        val a = async { repo.tryRefreshOrInvalidate() }
        val b = async { repo.tryRefreshOrInvalidate() }
        // Yield enough for both coroutines to enter refreshMutex (one waits inside, the
        // other queues at the mutex).
        testScheduler.runCurrent()
        gate.complete(Unit)
        val results = awaitAll(a, b)

        assertEquals(1, api.refreshCallCount, "expected exactly one HTTP refresh")
        assertEquals(freshAccess, results[0])
        assertEquals(freshAccess, results[1])
    }

    @Test
    fun sequentialCaller_afterRefreshWithSameInputToken_triggersAnotherRefresh() = runTest {
        // This is the heart of the H1 fix: the first refresh succeeds and stores T2; later
        // a 401 comes in still carrying T2 (because the backend now rejects T2 too — clock
        // drift, kill-switch, whatever). The second `tryRefreshOrInvalidate` call MUST NOT
        // simply return T2 from cache — it must call /auth/refresh again.
        val t1 = "stored-stale"
        val t2 = fakeJwt(expEpochSec = DEFAULT_NOW + accessTtl)
        val t3 = fakeJwt(expEpochSec = DEFAULT_NOW + accessTtl, firstName = "Three")

        val storage = InMemoryTokenStorage(
            initialAccessToken = t1,
            initialRefreshToken = "refresh-1",
            initialRefreshExpiry = DEFAULT_NOW + refreshTtl,
        )
        val responses = mutableListOf<JwtResponse>(
            JwtResponse(token = t2, username = "u", refreshToken = "refresh-2", refreshExpiresIn = refreshTtl),
            JwtResponse(token = t3, username = "u", refreshToken = "refresh-3", refreshExpiresIn = refreshTtl),
        )
        val api = FakeAuthApiService(refreshHandler = { responses.removeAt(0) })
        val repo = AuthRepositoryImpl(api, storage, TestClock())

        val first = repo.tryRefreshOrInvalidate()
        // After this call, storage holds t2. Now the WS comes back and says "the bearer
        // I just tried (t2) was rejected — please refresh". The contract: we MUST hit the
        // backend again, NOT silently return t2.
        val second = repo.tryRefreshOrInvalidate()

        assertEquals(t2, first)
        assertEquals(t3, second)
        assertEquals(2, api.refreshCallCount)
        assertNotEquals(first, second)
    }

    @Test
    fun coalescing_secondCallerThatLostTheMutexRace_reusesFreshToken() = runTest {
        // The OTHER side of the coalescing contract: if caller A is mid-refresh and caller
        // B arrives before A's refresh completes, B should wait for the mutex and then
        // notice the counter advanced — reusing A's token without a second HTTP round-trip.
        val staleAccess = fakeJwt(expEpochSec = DEFAULT_NOW - 1_000)
        val freshAccess = fakeJwt(expEpochSec = DEFAULT_NOW + accessTtl)
        val storage = InMemoryTokenStorage(
            initialAccessToken = staleAccess,
            initialRefreshToken = "refresh-1",
            initialRefreshExpiry = DEFAULT_NOW + refreshTtl,
        )

        val firstRefreshGate = CompletableDeferred<Unit>()
        var calls = 0
        val api = FakeAuthApiService(
            refreshHandler = {
                calls++
                if (calls == 1) firstRefreshGate.await()
                JwtResponse(
                    token = freshAccess,
                    username = "u",
                    refreshToken = "refresh-2",
                    refreshExpiresIn = refreshTtl,
                )
            },
        )
        val repo = AuthRepositoryImpl(api, storage, TestClock())

        val a = async { repo.tryRefreshOrInvalidate() }
        testScheduler.runCurrent() // A enters refreshMutex and parks on the gate
        val b = async { repo.tryRefreshOrInvalidate() }
        testScheduler.runCurrent() // B's seenCounter snapshot is BEFORE A increments

        firstRefreshGate.complete(Unit)
        val tokens = awaitAll(a, b)

        assertEquals(freshAccess, tokens[0])
        assertEquals(freshAccess, tokens[1])
        assertEquals(1, api.refreshCallCount, "coalesced — only one HTTP round-trip")
    }

    @Test
    fun caller_whenStoredRefreshIsBlank_invalidatesSession() = runTest {
        val storage = InMemoryTokenStorage(
            initialAccessToken = "anything",
            initialRefreshToken = null,
        )
        val api = FakeAuthApiService()
        val repo = AuthRepositoryImpl(api, storage, TestClock())

        val result = repo.tryRefreshOrInvalidate()

        assertEquals(null, result)
        assertEquals(0, api.refreshCallCount)
        val state = repo.authState.value
        val unauth = assertIs<AuthState.Unauthenticated>(state)
        assertEquals(AuthState.Reason.EXPIRED, unauth.reason)
    }

    @Test
    fun terminal4xxFailure_clearsRefreshAndInvalidates() = runTest {
        val storage = InMemoryTokenStorage(
            initialAccessToken = "stale",
            initialRefreshToken = "refresh-1",
            initialRefreshExpiry = DEFAULT_NOW + refreshTtl,
        )
        val api = FakeAuthApiService(refreshHandler = { throwKtorHttpError(HttpStatusCode.Unauthorized) })
        val repo = AuthRepositoryImpl(api, storage, TestClock())

        val result = repo.tryRefreshOrInvalidate()

        assertEquals(null, result)
        assertEquals(1, storage.clearRefreshTokenCount)
        val state = repo.authState.value
        assertIs<AuthState.Unauthenticated>(state)
    }
}
