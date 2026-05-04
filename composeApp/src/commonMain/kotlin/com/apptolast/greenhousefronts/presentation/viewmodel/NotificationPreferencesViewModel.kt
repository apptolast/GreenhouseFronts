package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.domain.model.AlertSeverity
import com.apptolast.greenhousefronts.domain.model.NotificationPreferences
import com.apptolast.greenhousefronts.domain.model.PreferredChannel
import com.apptolast.greenhousefronts.domain.model.QuietHours
import com.apptolast.greenhousefronts.domain.repository.NotificationPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

data class NotificationPreferencesUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    /** Last-loaded server snapshot. Used for the "dirty" check. */
    val initial: NotificationPreferences? = null,
    /** Editable copy. Equals [initial] until the user touches anything. */
    val draft: NotificationPreferences? = null,
    /** One-shot flag for the success snackbar. */
    val saveSuccess: Boolean = false,
    val error: String? = null,
) {
    val canSave: Boolean
        get() = draft != null && initial != null && draft != initial && !isSaving && !isLoading
}

/**
 * Loads the user's notification preferences on entry, exposes setters that mutate a
 * draft copy, and ships the full draft back via PUT on save (the backend rejects partial
 * updates — every field must be present).
 */
class NotificationPreferencesViewModel(
    private val repository: NotificationPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<NotificationPreferencesUiState>
        field = MutableStateFlow(NotificationPreferencesUiState())

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, error = null) }
            repository.getPreferences()
                .onSuccess { prefs ->
                    uiState.update {
                        it.copy(
                            isLoading = false,
                            initial = prefs,
                            draft = prefs,
                        )
                    }
                }
                .onFailure { error ->
                    uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "No se pudieron cargar las preferencias",
                        )
                    }
                }
        }
    }

    // --- Edits (mutate draft) ---

    fun setCategoryAlerts(value: Boolean) = mutateDraft { it.copy(categoryAlerts = value) }
    fun setCategoryDevices(value: Boolean) = mutateDraft { it.copy(categoryDevices = value) }
    fun setCategorySubscription(value: Boolean) = mutateDraft { it.copy(categorySubscription = value) }
    fun setMinSeverity(severity: AlertSeverity) = mutateDraft { it.copy(minSeverity = severity) }
    fun setChannel(channel: PreferredChannel) = mutateDraft { it.copy(channel = channel) }
    fun setLocale(locale: String) = mutateDraft { it.copy(locale = locale) }

    /**
     * Toggles quiet hours on/off. Turning on inserts a default 22:00–07:00 window so the
     * user has something visible to adjust; turning off clears the times entirely.
     */
    fun setQuietHoursEnabled(enabled: Boolean) {
        mutateDraft {
            if (enabled) {
                if (it.quietHours == null) {
                    val tz = it.timezone.takeIf { tz -> tz.isNotBlank() }
                        ?: TimeZone.currentSystemDefault().id
                    it.copy(
                        quietHours = QuietHours(start = DEFAULT_QH_START, end = DEFAULT_QH_END),
                        timezone = tz,
                    )
                } else {
                    it
                }
            } else {
                it.copy(quietHours = null)
            }
        }
    }

    fun setQuietHoursStart(hhmm: String) = mutateDraft { state ->
        val current = state.quietHours ?: return@mutateDraft state
        state.copy(quietHours = current.copy(start = hhmm))
    }

    fun setQuietHoursEnd(hhmm: String) = mutateDraft { state ->
        val current = state.quietHours ?: return@mutateDraft state
        state.copy(quietHours = current.copy(end = hhmm))
    }

    fun setTimezone(tz: String) = mutateDraft { it.copy(timezone = tz) }

    // --- Save ---

    fun save() {
        val draft = uiState.value.draft ?: return
        if (uiState.value.isSaving) return
        viewModelScope.launch {
            uiState.update { it.copy(isSaving = true, error = null) }
            repository.updatePreferences(draft)
                .onSuccess { saved ->
                    uiState.update {
                        it.copy(
                            isSaving = false,
                            initial = saved,
                            draft = saved,
                            saveSuccess = true,
                        )
                    }
                }
                .onFailure { error ->
                    uiState.update {
                        it.copy(
                            isSaving = false,
                            error = error.message ?: "No se pudieron guardar las preferencias",
                        )
                    }
                }
        }
    }

    fun consumeSaveSuccess() {
        uiState.update { it.copy(saveSuccess = false) }
    }

    fun clearError() {
        uiState.update { it.copy(error = null) }
    }

    private inline fun mutateDraft(transform: (NotificationPreferences) -> NotificationPreferences) {
        uiState.update { state ->
            val draft = state.draft ?: return@update state
            state.copy(draft = transform(draft))
        }
    }

    private companion object {
        const val DEFAULT_QH_START = "22:00"
        const val DEFAULT_QH_END = "07:00"
    }
}
