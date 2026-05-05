package com.apptolast.greenhousefronts.presentation.navigation

import com.apptolast.greenhousefronts.presentation.ui.components.BottomNavTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cross-screen bus for forcing a tab selection on `MainScreen`.
 *
 * Backed by a [MutableStateFlow] (instead of a `Channel`) so that the latest pending
 * selection survives screens that are not yet composed. This matters specifically for the
 * FCM deep-link → Login → MainScreen flow: the deep-link handler in App.kt may emit
 * [BottomNavTab.ALERTS] while the user is still in Login; with a Channel that emit would
 * be lost when MainScreen finally subscribes. With a StateFlow the value is replayed once
 * MainScreen attaches its collector, the collector calls [consume] to clear it, and
 * subsequent unrelated recompositions don't re-trigger the navigation.
 */
class BottomNavSelectionBus {
    private val _pending = MutableStateFlow<BottomNavTab?>(null)
    val pending: StateFlow<BottomNavTab?> = _pending.asStateFlow()

    /** Request a tab selection. Overwrites any previous pending selection. */
    fun select(tab: BottomNavTab) {
        _pending.value = tab
    }

    /** Marks the current pending selection as consumed. Idempotent. */
    fun consume() {
        _pending.value = null
    }
}
