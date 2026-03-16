package com.apptolast.greenhousefronts.domain.repository

import com.apptolast.greenhousefronts.domain.model.UserProfile

/**
 * Repository interface for user profile operations.
 */
interface UserRepository {

    /**
     * Fetches the current user's profile.
     * Combines JWT claims with API data from the users endpoint.
     */
    suspend fun getCurrentUserProfile(): Result<UserProfile>
}
