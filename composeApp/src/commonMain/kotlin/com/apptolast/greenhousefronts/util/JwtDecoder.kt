package com.apptolast.greenhousefronts.util

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Utility to decode JWT token payloads and extract claims.
 * Used to retrieve tenantId, user info and expiration from the access token.
 */
@OptIn(ExperimentalEncodingApi::class)
object JwtDecoder {

    private val json = Json { ignoreUnknownKeys = true }

    fun extractStringClaim(token: String, claim: String): String? {
        return try {
            val payload = decodePayload(token) ?: return null
            val jsonObj = json.parseToJsonElement(payload).jsonObject
            jsonObj[claim]?.jsonPrimitive?.contentOrNull
        } catch (_: Exception) {
            null
        }
    }

    fun extractLongClaim(token: String, claim: String): Long? {
        return try {
            val payload = decodePayload(token) ?: return null
            val jsonObj = json.parseToJsonElement(payload).jsonObject
            jsonObj[claim]?.jsonPrimitive?.longOrNull
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Returns the JWT `exp` claim (RFC 7519) as Unix epoch seconds, or null if absent /
     * malformed. Reuses [extractLongClaim] so any non-numeric encoding falls through to null.
     */
    fun extractExpiration(token: String): Long? = extractLongClaim(token, "exp")

    /**
     * True if the token has no `exp` claim (defensive: treat missing expiry as expired) or
     * if `exp <= now + skewSeconds`. The skew protects against tokens that expire mid-request
     * — by treating them as already expired we avoid firing a request that the server will
     * 401 milliseconds later.
     *
     * @param skewSeconds positive grace window before the actual expiry, default 30 s.
     */
    @OptIn(ExperimentalTime::class)
    fun isTokenExpired(token: String, skewSeconds: Long = 30): Boolean {
        val exp = extractExpiration(token) ?: return true
        val nowSec = Clock.System.now().epochSeconds
        return exp <= nowSec + skewSeconds
    }

    private fun decodePayload(token: String): String? {
        val parts = token.split(".")
        if (parts.size != 3) return null
        val payload = parts[1]
        // JWT uses base64url without padding — add padding if needed
        val padded = when (payload.length % 4) {
            2 -> "${payload}=="
            3 -> "${payload}="
            else -> payload
        }
        return Base64.UrlSafe.decode(padded).decodeToString()
    }
}
