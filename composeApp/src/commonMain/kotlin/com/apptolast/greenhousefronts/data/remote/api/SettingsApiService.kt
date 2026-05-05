package com.apptolast.greenhousefronts.data.remote.api

import com.apptolast.greenhousefronts.data.remote.baseUrl
import io.ktor.client.HttpClient
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/**
 * API service for updating individual settings.
 * Uses the authenticated HttpClient (Bearer token auto-injected).
 *
 * @param httpClient Injected authenticated HTTP client
 */
class SettingsApiService(
    private val httpClient: HttpClient,
) {

    /**
     * Updates a single setting's value.
     * PUT /api/v1/tenants/{tenantId}/settings/{settingId}
     *
     * @param tenantId The tenant ID
     * @param settingId The setting ID to update
     * @param value The new value as String
     */
    suspend fun updateSettingValue(tenantId: Long, settingId: Long, value: String) {
        httpClient.put("$baseUrl/tenants/$tenantId/settings/$settingId") {
            contentType(ContentType.Application.Json)
            setBody(SettingUpdateRequest(value = value))
        }
    }
}

@Serializable
private data class SettingUpdateRequest(
    val value: String? = null,
)
