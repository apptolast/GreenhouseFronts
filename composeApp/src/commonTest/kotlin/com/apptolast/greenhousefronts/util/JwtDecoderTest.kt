package com.apptolast.greenhousefronts.util

import com.apptolast.greenhousefronts.testutil.TestClock
import com.apptolast.greenhousefronts.testutil.TestClock.Companion.DEFAULT_NOW
import com.apptolast.greenhousefronts.testutil.fakeJwt
import com.apptolast.greenhousefronts.testutil.jwtWithoutExp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class JwtDecoderTest {

    @Test
    fun isTokenExpired_returnsTrue_whenNoExpClaim() {
        val token = jwtWithoutExp()
        assertTrue(JwtDecoder.isTokenExpired(token, clock = TestClock()))
    }

    @Test
    fun isTokenExpired_returnsTrue_forMalformedToken() {
        assertTrue(JwtDecoder.isTokenExpired("not.a.jwt", clock = TestClock()))
        assertTrue(JwtDecoder.isTokenExpired("plain-string", clock = TestClock()))
        assertTrue(JwtDecoder.isTokenExpired("", clock = TestClock()))
    }

    @Test
    fun isTokenExpired_returnsTrue_whenExpIsInThePast() {
        val clock = TestClock()
        val token = fakeJwt(expEpochSec = DEFAULT_NOW - 1)
        assertTrue(JwtDecoder.isTokenExpired(token, clock = clock))
    }

    @Test
    fun isTokenExpired_returnsTrue_withinSkewWindow() {
        // exp = now + 30s, skew defaults to 30 → "exp <= now + 30" is true.
        val clock = TestClock()
        val token = fakeJwt(expEpochSec = DEFAULT_NOW + 30)
        assertTrue(JwtDecoder.isTokenExpired(token, clock = clock))
    }

    @Test
    fun isTokenExpired_returnsFalse_justOutsideSkewWindow() {
        // exp = now + 31s, skew = 30 → not expired.
        val clock = TestClock()
        val token = fakeJwt(expEpochSec = DEFAULT_NOW + 31)
        assertFalse(JwtDecoder.isTokenExpired(token, clock = clock))
    }

    @Test
    fun isTokenExpired_followsAdvancingClock() {
        val clock = TestClock()
        val token = fakeJwt(expEpochSec = DEFAULT_NOW + 3_600) // 1h ahead

        assertFalse(JwtDecoder.isTokenExpired(token, clock = clock))

        clock.setNow(DEFAULT_NOW + 3_570) // 30s left
        assertTrue(JwtDecoder.isTokenExpired(token, clock = clock))

        clock.setNow(DEFAULT_NOW + 3_700) // 100s past expiry
        assertTrue(JwtDecoder.isTokenExpired(token, clock = clock))
    }

    @Test
    fun extractExpiration_returnsExp_whenPresent() {
        val token = fakeJwt(expEpochSec = 1_700_000_000L)
        assertEquals(1_700_000_000L, JwtDecoder.extractExpiration(token))
    }

    @Test
    fun extractExpiration_returnsNull_whenAbsent() {
        assertNull(JwtDecoder.extractExpiration(jwtWithoutExp()))
    }

    @Test
    fun extractStringClaim_andExtractLongClaim_readKnownClaims() {
        val token = fakeJwt(expEpochSec = DEFAULT_NOW + 60, tenantId = 7L, firstName = "Ana")
        assertEquals(7L, JwtDecoder.extractLongClaim(token, "tenantId"))
        assertEquals("Ana", JwtDecoder.extractStringClaim(token, "firstName"))
        assertNull(JwtDecoder.extractStringClaim(token, "nonexistent"))
    }

    @Test
    fun customSkew_isHonored() {
        val clock = TestClock()
        val token = fakeJwt(expEpochSec = DEFAULT_NOW + 60)
        // With skew=120, a token 60s away from expiry is "already expired".
        assertTrue(JwtDecoder.isTokenExpired(token, skewSeconds = 120, clock = clock))
        // With skew=0, the same token is fresh.
        assertFalse(JwtDecoder.isTokenExpired(token, skewSeconds = 0, clock = clock))
    }
}
