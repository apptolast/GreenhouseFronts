package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.domain.model.Alert
import com.apptolast.greenhousefronts.domain.model.AlertSeverity
import com.apptolast.greenhousefronts.domain.model.AlertTransition
import com.apptolast.greenhousefronts.domain.repository.AlertRepository
import com.apptolast.greenhousefronts.presentation.navigation.PendingAlertSelectionBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AlertsTab { ACTIVE, HISTORY }

data class AlertsUiState(
    val tab: AlertsTab = AlertsTab.ACTIVE,
    /** Severities the user wants to see (chips). Defaults to all selected. */
    val severityFilter: Set<AlertSeverity> = AlertSeverity.entries.toSet(),

    // Active tab — driven by the WebSocket snapshot.
    val activeAlerts: List<Alert> = emptyList(),
    /** True until the first WS frame arrives. */
    val activeIsLoading: Boolean = true,
    val expandedActiveId: Long? = null,

    // History tab — REST-paginated transitions.
    val historyTransitions: List<AlertTransition> = emptyList(),
    val historyIsLoading: Boolean = false,
    val historyHasMore: Boolean = false,
    val expandedTransitionId: Long? = null,

    val error: String? = null,
)

/**
 * Two independent data sources behind one screen:
 *  - **Active tab** collects [AlertRepository.observeActiveAlerts] from the WS snapshot —
 *    real-time, no race with REST.
 *  - **History tab** lazily fetches the first page of `/alert-events` whenever the user
 *    enters the tab (or pull-to-refreshes). Each row is a state transition, not an alert.
 *
 * Deep links (FCM) use [AlertRepository.getById] to decide which tab to land on.
 */
class AlertsViewModel(
    private val repository: AlertRepository,
    private val pendingSelection: PendingAlertSelectionBus,
) : ViewModel() {

    val uiState: StateFlow<AlertsUiState>
        field = MutableStateFlow(AlertsUiState())

    init {
        observeActiveAlerts()
        viewModelScope.launch { handleDeepLinkOnStart() }
    }

    fun selectTab(tab: AlertsTab) {
        if (uiState.value.tab == tab) return
        uiState.update {
            it.copy(
                tab = tab,
                expandedActiveId = if (tab == AlertsTab.ACTIVE) it.expandedActiveId else null,
                expandedTransitionId = if (tab == AlertsTab.HISTORY) it.expandedTransitionId else null,
            )
        }
        if (tab == AlertsTab.HISTORY && uiState.value.historyTransitions.isEmpty()) {
            loadHistory()
        }
    }

    fun toggleSeverity(severity: AlertSeverity) {
        uiState.update { state ->
            val next = state.severityFilter.toMutableSet().apply {
                if (!add(severity)) remove(severity)
            }
            state.copy(severityFilter = next)
        }
    }

    fun toggleExpandActive(alertId: Long) {
        uiState.update {
            it.copy(expandedActiveId = if (it.expandedActiveId == alertId) null else alertId)
        }
    }

    fun toggleExpandTransition(transitionId: Long) {
        uiState.update {
            it.copy(expandedTransitionId = if (it.expandedTransitionId == transitionId) null else transitionId)
        }
    }

    /** Pull-to-refresh on the History tab. */
    fun refreshHistory() = loadHistory()

    fun clearError() {
        uiState.update { it.copy(error = null) }
    }

    // --- Internals ---

    private fun observeActiveAlerts() {
        viewModelScope.launch {
            repository.observeActiveAlerts().collect { list ->
                uiState.update { it.copy(activeAlerts = list, activeIsLoading = false) }
            }
        }
    }

    private fun loadHistory() {
        if (uiState.value.historyIsLoading) return
        viewModelScope.launch {
            uiState.update { it.copy(historyIsLoading = true, error = null) }
            repository.getTransitionHistory(page = 0, size = HISTORY_PAGE_SIZE)
                .onSuccess { paged ->
                    uiState.update {
                        it.copy(
                            historyIsLoading = false,
                            historyTransitions = paged.items,
                            historyHasMore = paged.hasMore,
                        )
                    }
                }
                .onFailure { error ->
                    uiState.update {
                        it.copy(
                            historyIsLoading = false,
                            error = error.message ?: "No se pudo cargar el histórico",
                        )
                    }
                }
        }
    }

    /**
     * If the user arrived from a notification tap, decide which tab to show and (for the
     * Active tab) pre-expand the target alert. For deep links into resolved alerts we just
     * land on History — auto-finding the right transition row is left to the user, since
     * the same alert may have several rows.
     */
    private suspend fun handleDeepLinkOnStart() {
        val pendingId = pendingSelection.consume() ?: return
        val alert = repository.getById(pendingId).getOrNull() ?: return
        if (alert.isResolved) {
            selectTab(AlertsTab.HISTORY)
        } else {
            uiState.update {
                it.copy(tab = AlertsTab.ACTIVE, expandedActiveId = alert.id)
            }
        }
    }

    private companion object {
        const val HISTORY_PAGE_SIZE = 50
    }
}
