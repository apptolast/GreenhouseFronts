package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.data.remote.api.AlertApiService
import com.apptolast.greenhousefronts.domain.model.Alert
import com.apptolast.greenhousefronts.domain.model.AlertSeverity
import com.apptolast.greenhousefronts.domain.repository.AlertRepository
import com.apptolast.greenhousefronts.presentation.navigation.PendingAlertSelectionBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AlertsTab { ACTIVE, HISTORY }

data class AlertsUiState(
    val tab: AlertsTab = AlertsTab.ACTIVE,
    /** Severities the user wants to see. By default all four are selected. */
    val severityFilter: Set<AlertSeverity> = AlertSeverity.entries.toSet(),
    val isLoading: Boolean = false,
    val alerts: List<Alert> = emptyList(),
    val expandedAlertId: Long? = null,
    val error: String? = null,
    /** True when the backend response hit the page limit — the UI shows a footer hint. */
    val truncated: Boolean = false,
)

class AlertsViewModel(
    private val repository: AlertRepository,
    private val pendingSelection: PendingAlertSelectionBus,
) : ViewModel() {

    val uiState: StateFlow<AlertsUiState>
        field = MutableStateFlow(AlertsUiState())

    init {
        // Trigger the first load and consume any pending deep-link selection.
        refresh(consumeDeepLink = true)
    }

    fun selectTab(tab: AlertsTab) {
        if (uiState.value.tab == tab) return
        uiState.update { it.copy(tab = tab, expandedAlertId = null) }
        refresh(consumeDeepLink = false)
    }

    fun toggleSeverity(severity: AlertSeverity) {
        uiState.update { state ->
            val next = state.severityFilter.toMutableSet().apply {
                if (!add(severity)) remove(severity)
            }
            state.copy(severityFilter = next)
        }
    }

    fun toggleExpand(alertId: Long) {
        uiState.update { state ->
            state.copy(expandedAlertId = if (state.expandedAlertId == alertId) null else alertId)
        }
    }

    fun refresh() {
        refresh(consumeDeepLink = false)
    }

    fun clearError() {
        uiState.update { it.copy(error = null) }
    }

    /**
     * Load the current tab's data and, if [consumeDeepLink] is true, also handle a pending
     * alert selection: switch tab if the alert lives in History, fetch it on demand if
     * it's not already in the loaded list, and expand it.
     */
    private fun refresh(consumeDeepLink: Boolean) {
        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, error = null) }

            val pending = if (consumeDeepLink) pendingSelection.consume() else null

            // If there's a deep-link target, fetch it first so we can decide which tab to show.
            val deepLinkAlert = pending?.let { id ->
                repository.getById(id).getOrNull()
            }
            val targetTab = when {
                deepLinkAlert != null && deepLinkAlert.isResolved -> AlertsTab.HISTORY
                deepLinkAlert != null && !deepLinkAlert.isResolved -> AlertsTab.ACTIVE
                else -> uiState.value.tab
            }

            val listResult = when (targetTab) {
                AlertsTab.ACTIVE -> repository.getActive()
                AlertsTab.HISTORY -> repository.getHistory()
            }

            listResult
                .onSuccess { list ->
                    val alerts = ensureContains(list, deepLinkAlert)
                    uiState.update {
                        it.copy(
                            tab = targetTab,
                            isLoading = false,
                            alerts = alerts,
                            expandedAlertId = deepLinkAlert?.id ?: it.expandedAlertId,
                            truncated = list.size >= AlertApiService.DEFAULT_LIMIT,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    uiState.update {
                        it.copy(
                            tab = targetTab,
                            isLoading = false,
                            error = error.message ?: "No se pudieron cargar las alertas",
                        )
                    }
                }
        }
    }

    /**
     * If the deep-link target alert isn't included in the freshly fetched list (e.g. it's
     * older than the 100-row window), prepend it so the UI can still expand it.
     */
    private fun ensureContains(list: List<Alert>, target: Alert?): List<Alert> {
        if (target == null) return list
        if (list.any { it.id == target.id }) return list
        return listOf(target) + list
    }
}
