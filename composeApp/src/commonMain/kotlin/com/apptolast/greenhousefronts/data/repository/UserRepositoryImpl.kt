package com.apptolast.greenhousefronts.data.repository

import com.apptolast.greenhousefronts.data.local.auth.TokenStorage
import com.apptolast.greenhousefronts.data.model.tenant.TenantResponse
import com.apptolast.greenhousefronts.data.remote.api.UserApiService
import com.apptolast.greenhousefronts.domain.model.UserProfile
import com.apptolast.greenhousefronts.domain.repository.UserRepository
import com.apptolast.greenhousefronts.util.JwtDecoder

/**
 * Implementation of UserRepository.
 * Fetches user profile by listing tenant users and matching the current JWT subject.
 * Also fetches tenant info for company details.
 *
 * @param userApiService API service for user and tenant endpoints
 * @param tokenStorage Token storage for tenantId and username
 */
class UserRepositoryImpl(
    private val userApiService: UserApiService,
    private val tokenStorage: TokenStorage,
) : UserRepository {

    override suspend fun getCurrentUserProfile(): Result<UserProfile> {
        val tenantId = tokenStorage.getTenantId()
            ?: return Result.failure(Exception("No se encontró el ID del tenant"))
        val currentEmail = tokenStorage.getUsername()
            ?: return Result.failure(Exception("No se encontró el email del usuario"))

        return try {
            val users = userApiService.getUsers(tenantId)
            val tenant = runCatching { userApiService.getTenant(tenantId) }.getOrNull()
            val currentUser = users.find { it.email.equals(currentEmail, ignoreCase = true) }

            if (currentUser != null) {
                Result.success(
                    UserProfile(
                        id = currentUser.id,
                        code = currentUser.code,
                        username = currentUser.username,
                        email = currentUser.email,
                        role = currentUser.role,
                        isActive = currentUser.isActive,
                        lastLogin = currentUser.lastLogin,
                        createdAt = currentUser.createdAt,
                        companyName = tenant?.name,
                        companyPhone = tenant?.phone,
                        province = tenant?.province,
                        country = tenant?.country,
                    ),
                )
            } else {
                buildProfileFromJwt(currentEmail, tenant)
            }
        } catch (_: Exception) {
            buildProfileFromJwt(currentEmail, tenant = null)
        }
    }

    private suspend fun buildProfileFromJwt(
        email: String,
        tenant: TenantResponse?,
    ): Result<UserProfile> {
        val token = tokenStorage.getToken() ?: return Result.failure(Exception("No token"))
        val role = JwtDecoder.extractStringClaim(token, "role") ?: "UNKNOWN"

        return Result.success(
            UserProfile(
                id = null,
                code = null,
                username = email.substringBefore("@"),
                email = email,
                role = role,
                isActive = true,
                lastLogin = null,
                createdAt = null,
                companyName = tenant?.name,
                companyPhone = tenant?.phone,
                province = tenant?.province,
                country = tenant?.country,
            ),
        )
    }
}
