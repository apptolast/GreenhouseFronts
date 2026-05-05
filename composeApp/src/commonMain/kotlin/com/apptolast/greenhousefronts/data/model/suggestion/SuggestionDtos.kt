package com.apptolast.greenhousefronts.data.model.suggestion

import kotlinx.serialization.Serializable

/**
 * Request body for `POST /api/v1/suggestions`. Mirrors the OpenAPI schema at
 * `https://inverapi-dev.apptolast.com/v3/api-docs` (verified contract):
 *
 * - All six fields are required by the backend.
 * - JSON keys are camelCase already, so no `@SerialName` is needed.
 * - Validation enforced server-side; we mirror the limits client-side in the
 *   ViewModel so we fail fast before issuing the request.
 *     · category    1..50    chars
 *     · title       3..200   chars
 *     · description 1..5000  chars
 *     · appVersion  0..50    chars  (always satisfied by [Platform.versionName])
 *     · platform    0..100   chars  (always satisfied by [Platform.name])
 *     · userEmail   valid RFC 5322 email
 */
@Serializable
data class CreateSuggestionRequest(
    val category: String,
    val title: String,
    val description: String,
    val appVersion: String,
    val platform: String,
    val userEmail: String,
)

/**
 * Response from `POST /api/v1/suggestions` on HTTP 201. The OpenAPI marks the
 * body as a generic `object`, but the curl-confirmed shape returns the GitHub
 * issue identifiers and a flag indicating whether the email side-effect
 * succeeded. All fields are nullable + defaulted for two reasons:
 *
 *   1. The schema is informal; future fields shouldn't crash deserialization
 *      (and `Json { ignoreUnknownKeys = true }` is already configured).
 *   2. `emailSent = false` means the issue was created but the email failed —
 *      from the user's perspective the call still succeeded.
 */
@Serializable
data class CreateSuggestionResponse(
    val issueNumber: Int? = null,
    val issueUrl: String? = null,
    val emailSent: Boolean? = null,
)
