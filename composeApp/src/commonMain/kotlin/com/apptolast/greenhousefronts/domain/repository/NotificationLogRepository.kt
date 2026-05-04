package com.apptolast.greenhousefronts.domain.repository

import com.apptolast.greenhousefronts.domain.model.NotificationLogPage

/**
 * History of push notifications dispatched to the authenticated user.
 *
 * Backed by `GET /api/v1/users/me/notifications` — cursor-paginated (NOT Spring Pageable).
 * The first call passes `cursor = null`; subsequent pages pass `nextCursor` from the
 * previous response.
 */
interface NotificationLogRepository {
    suspend fun getPage(
        cursor: Long? = null,
        limit: Int = 50,
    ): Result<NotificationLogPage>
}
