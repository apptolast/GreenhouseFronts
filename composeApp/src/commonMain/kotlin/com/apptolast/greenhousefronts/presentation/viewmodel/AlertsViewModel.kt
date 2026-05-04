package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.domain.model.Alert
import com.apptolast.greenhousefronts.domain.model.AlertSeverity
import com.apptolast.greenhousefronts.domain.model.AlertTransition
import com.apptolast.greenhousefronts.domain.repository.AlertRepository
import com.apptolast.greenhousefronts.presentation.navigation.PendingAlertSelectionBus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    // History tab — REST-paginated transitions with infinite scroll.
    val historyTransitions: List<AlertTransition> = emptyList(),
    /** True for the first page or pull-to-refresh. */
    val historyIsLoading: Boolean = false,
    /** True while a subsequent page is being appended. */
    val historyIsAppending: Boolean = false,
    val historyHasMore: Boolean = false,
    /** Set when the last page returned `hasMore = false`. */
    val historyEndReached: Boolean = false,
    /** Latest page index loaded (0-based). */
    val historyCurrentPage: Int = 0,
    val expandedTransitionId: Long? = null,
    /** Append-specific error so a transient pagination failure doesn't blank the list. */
    val historyAppendError: String? = null,

    val error: String? = null,
)

/**
 * Two independent data sources behind one screen:
 *  - **Active tab** collects [AlertRepository.observeActiveAlerts] from the WS snapshot —
 *    real-time, no race with REST.
 *  - **History tab** lazily fetches `/alert-events` on entry; subsequent pages are loaded
 *    via infinite scroll. The severity filter is applied server-side; toggling chips
 *    debounces 250 ms and reloads from page 0.
 *
 * Deep links (FCM) use [AlertRepository.getById] to decide which tab to land on.
 */
class AlertsViewModel(
    private val repository: AlertRepository,
    private val pendingSelection: PendingAlertSelectionBus,
) : ViewModel() {

    val uiState: StateFlow<AlertsUiState>
        field = MutableStateFlow(AlertsUiState())

    /** Cancelled and replaced on every refresh / next-page call to keep responses ordered. */
    private var historyJob: Job? = null
    private var historyFilterDebounceJob: Job? = null

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
        if (tab == AlertsTab.HISTORY && uiState.value.historyTransitions.isEmpty() && !uiState.value.historyIsLoading) {
            refreshHistory()
        }
    }

    fun toggleSeverity(severity: AlertSeverity) {
        uiState.update { state ->
            val next = state.severityFilter.toMutableSet().apply {
                if (!add(severity)) remove(severity)
            }
            state.copy(severityFilter = next)
        }
        // Re-issue the request only on the History tab. ACTIVE is WS-driven; client
        // filtering of the active list is fine and avoids extra round-trips.
        if (uiState.value.tab == AlertsTab.HISTORY) {
            historyFilterDebounceJob?.cancel()
            historyFilterDebounceJob = viewModelScope.launch {
                delay(FILTER_DEBOUNCE_MS)
                refreshHistory()
            }
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

    /** Pull-to-refresh on the History tab. Resets to page 0. */
    fun refreshHistory() {
        val severities = currentSeverityIds()
        if (severities == null) {
            // No severities selected — short-circuit to empty state without hitting the API.
            historyJob?.cancel()
            uiState.update {
                it.copy(
                    historyIsLoading = false,
                    historyIsAppending = false,
                    historyTransitions = emptyList(),
                    historyHasMore = false,
                    historyEndReached = true,
                    historyCurrentPage = 0,
                    historyAppendError = null,
                )
            }
            return
        }
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            uiState.update {
                it.copy(
                    historyIsLoading = true,
                    historyIsAppending = false,
                    historyAppendError = null,
                    error = null,
                )
            }
            repository.getTransitionHistory(page = 0, size = HISTORY_PAGE_SIZE, severityIds = severities)
                .onSuccess { paged ->
                    uiState.update {
                        it.copy(
                            historyIsLoading = false,
                            historyTransitions = paged.items,
                            historyHasMore = paged.hasMore,
                            historyEndReached = !paged.hasMore,
                            historyCurrentPage = 0,
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

    /** Triggered by the LazyColumn when the user scrolls near the bottom. */
    fun loadNextHistoryPage() {
        val s = uiState.value
        if (s.historyIsLoading || s.historyIsAppending || !s.historyHasMore || s.historyEndReached) return
        val severities = currentSeverityIds() ?: return
        val nextPage = s.historyCurrentPage + 1
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            uiState.update { it.copy(historyIsAppending = true, historyAppendError = null) }
            repository.getTransitionHistory(page = nextPage, size = HISTORY_PAGE_SIZE, severityIds = severities)
                .onSuccess { paged ->
                    uiState.update {
                        it.copy(
                            historyIsAppending = false,
                            historyTransitions = it.historyTransitions + paged.items,
                            historyHasMore = paged.hasMore,
                            historyEndReached = !paged.hasMore,
                            historyCurrentPage = nextPage,
                        )
                    }
                }
                .onFailure { error ->
                    uiState.update {
                        it.copy(
                            historyIsAppending = false,
                            historyAppendError = error.message ?: "No se pudo cargar más",
                        )
                    }
                }
        }
    }

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

    /**
     * Returns the severity-ids list to send to the backend, or `null` if the user
     * deselected every chip (caller should short-circuit to empty state — sending zero
     * `severityId` params would be interpreted as "no filter").
     */
    private fun currentSeverityIds(): List<Short>? {
        val sel = uiState.value.severityFilter
        if (sel.isEmpty()) return null
        // All severities selected = default behavior; pass empty to keep URLs clean.
        return if (sel.size == AlertSeverity.entries.size) emptyList() else sel.map { it.level }
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
        const val FILTER_DEBOUNCE_MS = 250L
    }
}
