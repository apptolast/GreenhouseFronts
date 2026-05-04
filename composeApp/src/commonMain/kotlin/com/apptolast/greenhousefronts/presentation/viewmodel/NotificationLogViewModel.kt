package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.domain.model.NotificationLogEntry
import com.apptolast.greenhousefronts.domain.repository.NotificationLogRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationLogUiState(
    val items: List<NotificationLogEntry> = emptyList(),
    /** True for the first page or pull-to-refresh. */
    val isLoading: Boolean = true,
    /** True while a subsequent page is being appended. */
    val isAppending: Boolean = false,
    val hasMore: Boolean = false,
    val nextCursor: Long? = null,
    val endReached: Boolean = false,
    val error: String? = null,
    val appendError: String? = null,
)

/**
 * Cursor-paginated history of push notifications delivered to the authenticated user.
 * Mirrors the AlertsViewModel.history pattern (refresh = cursor null, loadMore = cursor
 * from previous response, re-entry guards on isAppending).
 */
class NotificationLogViewModel(
    private val repository: NotificationLogRepository,
) : ViewModel() {

    val uiState: StateFlow<NotificationLogUiState>
        field = MutableStateFlow(NotificationLogUiState())

    private var inFlightJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        inFlightJob?.cancel()
        inFlightJob = viewModelScope.launch {
            uiState.update {
                it.copy(
                    isLoading = true,
                    isAppending = false,
                    appendError = null,
                    error = null,
                )
            }
            repository.getPage(cursor = null, limit = PAGE_LIMIT)
                .onSuccess { page ->
                    uiState.update {
                        it.copy(
                            isLoading = false,
                            items = page.items,
                            hasMore = page.hasMore,
                            nextCursor = page.nextCursor,
                            endReached = !page.hasMore,
                        )
                    }
                }
                .onFailure { error ->
                    uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "No se pudieron cargar las notificaciones",
                        )
                    }
                }
        }
    }

    fun loadMore() {
        val s = uiState.value
        if (s.isLoading || s.isAppending || !s.hasMore || s.endReached) return
        val cursor = s.nextCursor ?: return
        inFlightJob?.cancel()
        inFlightJob = viewModelScope.launch {
            uiState.update { it.copy(isAppending = true, appendError = null) }
            repository.getPage(cursor = cursor, limit = PAGE_LIMIT)
                .onSuccess { page ->
                    uiState.update {
                        it.copy(
                            isAppending = false,
                            items = it.items + page.items,
                            hasMore = page.hasMore,
                            nextCursor = page.nextCursor,
                            endReached = !page.hasMore,
                        )
                    }
                }
                .onFailure { error ->
                    uiState.update {
                        it.copy(
                            isAppending = false,
                            appendError = error.message ?: "No se pudo cargar más",
                        )
                    }
                }
        }
    }

    fun clearError() {
        uiState.update { it.copy(error = null) }
    }

    private companion object {
        const val PAGE_LIMIT = 50
    }
}
