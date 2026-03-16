package com.apptolast.greenhousefronts.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Utility to decode JWT token payloads and extract claims.
 * Used to retrieve tenantId and user info from the access token.
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
