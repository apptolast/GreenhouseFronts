package com.apptolast.greenhousefronts.domain.repository

import com.apptolast.greenhousefronts.domain.model.Alert

interface AlertRepository {
    /** Active alerts (`isResolved=false`), most recent first per backend ordering. */
    suspend fun getActive(): Result<List<Alert>>

    /** Full alert feed (active + resolved), ordered by createdAt DESC. Limited to the latest 100. */
    suspend fun getHistory(): Result<List<Alert>>

    /** Fetch a single alert. Used for deep-links that point to an alert not in the current list. */
    suspend fun getById(alertId: Long): Result<Alert>
}
