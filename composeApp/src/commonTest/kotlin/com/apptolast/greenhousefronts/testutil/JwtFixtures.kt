package com.apptolast.greenhousefronts.testutil

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Builds a syntactically valid JWT (header + payload + dummy signature) with arbitrary
 * `exp` and custom claims. The signature is NOT cryptographically valid — fine for
 * client-side decoder tests, which only base64url-decode and JSON-parse the payload.
 *
 * Note the encoding contract `JwtDecoder` expects: base64url without padding.
 */
@OptIn(ExperimentalEncodingApi::class)
fun fakeJwt(
    expEpochSec: Long,
    tenantId: Long? = 42L,
    firstName: String? = "Test",
    extraClaims: Map<String, JsonElement> = emptyMap(),
): String {
    val header = """{"alg":"HS256","typ":"JWT"}"""
    val payload = buildJsonObject {
        put("exp", expEpochSec)
        if (tenantId != null) put("tenantId", tenantId)
        if (firstName != null) put("firstName", firstName)
        extraClaims.forEach { (k, v) -> put(k, v) }
    }.toString()
    val signature = "fake-signature-bytes"
    return "${b64Url(header)}.${b64Url(payload)}.${b64Url(signature)}"
}

/** Builds a JWT whose payload has NO `exp` claim — should be treated as expired. */
@OptIn(ExperimentalEncodingApi::class)
fun jwtWithoutExp(): String {
    val header = """{"alg":"HS256","typ":"JWT"}"""
    val payload = buildJsonObject { put("sub", JsonPrimitive("test-user")) }.toString()
    return "${b64Url(header)}.${b64Url(payload)}.${b64Url("sig")}"
}

@OptIn(ExperimentalEncodingApi::class)
private fun b64Url(s: String): String =
    Base64.UrlSafe.encode(s.encodeToByteArray()).trimEnd('=')
