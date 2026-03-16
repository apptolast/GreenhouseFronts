package com.apptolast.greenhousefronts.data.model.tenant

import kotlinx.serialization.Serializable

/**
 * Response from GET /api/v1/tenants/{tenantId}
 */
@Serializable
data class TenantResponse(
    val id: Long,
    val code: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val province: String? = null,
    val country: String? = null,
    val isActive: Boolean? = null,
    val status: String? = null,
)
