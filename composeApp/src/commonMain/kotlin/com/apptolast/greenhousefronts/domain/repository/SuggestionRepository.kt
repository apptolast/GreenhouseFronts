package com.apptolast.greenhousefronts.domain.repository

import com.apptolast.greenhousefronts.data.model.suggestion.CreateSuggestionRequest
import com.apptolast.greenhousefronts.data.model.suggestion.CreateSuggestionResponse

/**
 * Submits in-app suggestions to the backend, which creates a GitHub issue
 * and emails the configured recipients.
 */
interface SuggestionRepository {
    suspend fun create(request: CreateSuggestionRequest): Result<CreateSuggestionResponse>
}
