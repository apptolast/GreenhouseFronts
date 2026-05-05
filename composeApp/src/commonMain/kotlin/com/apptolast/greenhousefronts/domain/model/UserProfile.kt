package com.apptolast.greenhousefronts.domain.model

/**
 * Domain model representing the current user's profile,
 * enriched with tenant (company) information.
 */
data class UserProfile(
    // User fields
    val id: Long?,
    val code: String?,
    val username: String,
    val email: String,
    val role: String,
    val isActive: Boolean,
    val lastLogin: String?,
    val createdAt: String?,
    // Tenant fields
    val companyName: String? = null,
    val companyPhone: String? = null,
    val province: String? = null,
    val country: String? = null,
)
