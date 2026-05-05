package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.data.feedback.FeedbackCategory
import com.apptolast.greenhousefronts.data.feedback.FeedbackDraft
import com.apptolast.greenhousefronts.data.feedback.FeedbackDraftStorage
import com.apptolast.greenhousefronts.data.model.suggestion.CreateSuggestionRequest
import com.apptolast.greenhousefronts.domain.repository.AuthRepository
import com.apptolast.greenhousefronts.domain.repository.SuggestionRepository
import com.apptolast.greenhousefronts.domain.repository.UserRepository
import com.apptolast.greenhousefronts.getPlatform
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SendSuggestionUiState(
    val draft: FeedbackDraft = FeedbackDraft(),
    val isSending: Boolean = false,
    val isLoadingProfile: Boolean = true,
    val email: String = "",
    val showExitConfirm: Boolean = false,
) {
    /**
     * Backend enforces title 3..200 chars and description 1..5000. Mirror the
     * lower bounds client-side so the user doesn't submit only to be told no.
     */
    val canSend: Boolean
        get() = draft.title.length >= MIN_TITLE_LENGTH &&
            draft.description.isNotBlank() &&
            email.isNotBlank() &&
            !isSending

    companion object {
        const val MIN_TITLE_LENGTH = 3
    }
}

sealed interface SendSuggestionEvent {
    data object SentSuccessfully : SendSuggestionEvent
    data class SendFailed(val message: String) : SendSuggestionEvent
    data object NavigateBack : SendSuggestionEvent
}

/**
 * Drives the in-app suggestion/feedback form.
 *
 * - Persists the draft via [FeedbackDraftStorage] so the user picks up where
 *   they left off after a process death or backgrounded session.
 * - Mirrors the user's email into UI state so we can attach it to the request
 *   payload (the backend uses it as both attribution and a CC recipient).
 * - On submit, calls [SuggestionRepository.create]. The backend creates a
 *   GitHub issue and emails the configured recipients server-side. On
 *   success we clear the draft and emit [SendSuggestionEvent.SentSuccessfully]
 *   so the screen can show a success snackbar and pop the back stack. On
 *   failure we surface the (already-translated) repository error message
 *   through [SendSuggestionEvent.SendFailed].
 */
class SendSuggestionViewModel(
    private val draftStorage: FeedbackDraftStorage,
    private val suggestionRepository: SuggestionRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    val uiState: StateFlow<SendSuggestionUiState>
        field = MutableStateFlow(SendSuggestionUiState(draft = draftStorage.draft.value))

    private val _events = Channel<SendSuggestionEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        // Mirror the persisted draft into UI state so any external write reflects
        // instantly. Pairs with the setters below which write through to disk first.
        viewModelScope.launch {
            draftStorage.draft.collect { draft ->
                uiState.update { it.copy(draft = draft) }
            }
        }
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            // The cached username (from TokenStorage) is usually an email or a
            // login name — used as a fallback while the profile fetch is in
            // flight. The fresh profile then overrides it with the canonical
            // email returned by the backend.
            val cached = authRepository.getUsername().orEmpty()
            uiState.update { it.copy(email = cached) }
            userRepository.getCurrentUserProfile()
                .onSuccess { profile ->
                    uiState.update {
                        it.copy(isLoadingProfile = false, email = profile.email)
                    }
                }
                .onFailure {
                    uiState.update { it.copy(isLoadingProfile = false) }
                }
        }
    }

    fun setCategory(category: FeedbackCategory) {
        draftStorage.setCategory(category)
    }

    fun setTitle(title: String) {
        draftStorage.setTitle(title)
    }

    fun setDescription(description: String) {
        draftStorage.setDescription(description)
    }

    /**
     * Called when the user taps the back arrow. If the form has any content we ask for
     * confirmation before discarding it; if it's empty we just navigate back.
     */
    fun onBackPressed() {
        if (uiState.value.draft.isBlank) {
            viewModelScope.launch { _events.send(SendSuggestionEvent.NavigateBack) }
        } else {
            uiState.update { it.copy(showExitConfirm = true) }
        }
    }

    fun dismissExitConfirm() {
        uiState.update { it.copy(showExitConfirm = false) }
    }

    /** User confirmed they want to abandon the draft — wipe it and pop. */
    fun confirmExit() {
        draftStorage.clear()
        uiState.update { it.copy(showExitConfirm = false) }
        viewModelScope.launch { _events.send(SendSuggestionEvent.NavigateBack) }
    }

    fun send() {
        val state = uiState.value
        if (!state.canSend) return
        uiState.update { it.copy(isSending = true) }

        viewModelScope.launch {
            val request = state.toRequest()
            suggestionRepository.create(request)
                .onSuccess {
                    draftStorage.clear()
                    _events.send(SendSuggestionEvent.SentSuccessfully)
                }
                .onFailure { error ->
                    uiState.update { it.copy(isSending = false) }
                    _events.send(
                        SendSuggestionEvent.SendFailed(
                            error.message ?: "No se pudo enviar tu sugerencia.",
                        ),
                    )
                }
        }
    }

    /**
     * Build the API payload from the current UI state plus host-platform
     * metadata. Kept inline because the mapping is one-shot — no other lane
     * needs it.
     */
    private fun SendSuggestionUiState.toRequest(): CreateSuggestionRequest =
        CreateSuggestionRequest(
            category = draft.category.display,
            title = draft.title.trim(),
            description = draft.description.trim(),
            appVersion = getPlatform().versionName,
            platform = getPlatform().name,
            userEmail = email,
        )
}
