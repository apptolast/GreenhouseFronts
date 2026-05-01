package com.apptolast.greenhousefronts.presentation.navigation

import com.apptolast.greenhousefronts.presentation.ui.components.BottomNavTab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * One-shot bus for forcing a tab selection on `MainScreen` from outside the composition,
 * e.g. when the deep-link handler in `App.kt` wants to switch the user to the Alerts tab
 * after they tap a push notification.
 *
 * The screen collects [requests] and applies the value to its local `selectedTab` state.
 */
class BottomNavSelectionBus {
    private val channel = Channel<BottomNavTab>(capacity = Channel.BUFFERED)
    val requests: Flow<BottomNavTab> = channel.receiveAsFlow()

    fun select(tab: BottomNavTab) {
        channel.trySend(tab)
    }
}
