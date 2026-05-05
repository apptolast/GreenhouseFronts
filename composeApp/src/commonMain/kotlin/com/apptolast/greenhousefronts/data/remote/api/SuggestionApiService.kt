package com.apptolast.greenhousefronts.data.remote.api

import com.apptolast.greenhousefronts.data.model.suggestion.CreateSuggestionRequest
import com.apptolast.greenhousefronts.data.model.suggestion.CreateSuggestionResponse
import com.apptolast.greenhousefronts.data.remote.baseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * In-app suggestion endpoint. Wired to the AUTHENTICATED client — the bearer
 * plugin auto-injects the JWT and transparently refreshes on 401, so this
 * service stays a thin Ktor wrapper.
 */
class SuggestionApiService(private val httpClient: HttpClient) {

    /**
     * `POST /api/v1/suggestions`. Creates a GitHub issue server-side and
     * triggers the recipient email pipeline managed by backend env vars.
     */
    suspend fun create(request: CreateSuggestionRequest): CreateSuggestionResponse =
        httpClient.post("$baseUrl/suggestions") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
