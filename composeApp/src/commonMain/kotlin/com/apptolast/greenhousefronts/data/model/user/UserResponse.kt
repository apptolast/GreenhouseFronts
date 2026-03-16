package com.apptolast.greenhousefronts.data.model.user

import kotlinx.serialization.Serializable

/**
 * Response from GET /api/v1/tenants/{tenantId}/users
 */
@Serializable
data class UserResponse(
    val id: Long,
    val code: String,
    val username: String,
    val email: String,
    val role: String,
    val tenantId: Long,
    val isActive: Boolean = true,
    val lastLogin: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
)
