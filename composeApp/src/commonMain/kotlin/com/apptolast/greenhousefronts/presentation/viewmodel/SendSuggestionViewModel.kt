package com.apptolast.greenhousefronts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.greenhousefronts.BuildKonfig
import com.apptolast.greenhousefronts.data.feedback.FeedbackCategory
import com.apptolast.greenhousefronts.data.feedback.FeedbackDraft
import com.apptolast.greenhousefronts.data.feedback.FeedbackDraftStorage
import com.apptolast.greenhousefronts.data.feedback.MailLauncher
import com.apptolast.greenhousefronts.domain.repository.AuthRepository
import com.apptolast.greenhousefronts.domain.repository.UserRepository
import com.apptolast.greenhousefronts.getPlatform
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class SendSuggestionUiState(
    val draft: FeedbackDraft = FeedbackDraft(),
    val isSending: Boolean = false,
    val isLoadingProfile: Boolean = true,
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val showExitConfirm: Boolean = false,
) {
    val canSend: Boolean
        get() = draft.title.isNotBlank() && draft.description.isNotBlank() && !isSending
}

sealed interface SendSuggestionEvent {
    data object SentSuccessfully : SendSuggestionEvent
    data object NoMailClientAvailable : SendSuggestionEvent
    data object NavigateBack : SendSuggestionEvent
}

/**
 * Drives the in-app suggestion/feedback form.
 *
 * - Loads the persisted draft from [FeedbackDraftStorage] so the user picks up where
 *   they left off after a process death or backgrounded session.
 * - Mirrors the user profile (display name, email, …) into UI state, used both to
 *   build the email subject and to attach a "Información técnica" footer to the body
 *   so the receiving inbox can triage without playing 20 questions.
 * - Send opens the OS mail composer with a pre-filled `mailto:` URI; on success we
 *   clear the draft and emit [SendSuggestionEvent.SentSuccessfully] so the screen
 *   can pop the back stack. If no mail handler is registered we surface
 *   [SendSuggestionEvent.NoMailClientAvailable] so the UI can show a snackbar instead
 *   of silently looking broken.
 */
class SendSuggestionViewModel(
    private val draftStorage: FeedbackDraftStorage,
    private val mailLauncher: MailLauncher,
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
            // displayName is best-effort from the cached token; falls back to the
            // username if the backend never returned one. Both go into the email
            // subject and the technical-context footer.
            val cachedDisplayName = authRepository.getDisplayName().orEmpty()
            val cachedUsername = authRepository.getUsername().orEmpty()
            uiState.update {
                it.copy(displayName = cachedDisplayName, username = cachedUsername)
            }
            userRepository.getCurrentUserProfile()
                .onSuccess { profile ->
                    uiState.update {
                        it.copy(
                            isLoadingProfile = false,
                            displayName = profile.username.ifBlank { cachedDisplayName },
                            username = cachedUsername.ifBlank { profile.email },
                            email = profile.email,
                        )
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

        val recipients = BuildKonfig.FEEDBACK_RECIPIENTS
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val signer = state.displayName.ifBlank { state.username.ifBlank { "Usuario" } }
        val subject = "Kropia - ${state.draft.category.display} de $signer"
        val body = buildEmailBody(state)

        val launched = mailLauncher.launch(recipients, subject, body)
        if (launched) {
            // Wipe the draft only after the mail client was successfully invoked. If
            // the user cancels the compose UI later, they still have to re-type — this
            // is the trade-off of mailto: vs a backend endpoint.
            draftStorage.clear()
            viewModelScope.launch { _events.send(SendSuggestionEvent.SentSuccessfully) }
        } else {
            uiState.update { it.copy(isSending = false) }
            viewModelScope.launch { _events.send(SendSuggestionEvent.NoMailClientAvailable) }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun buildEmailBody(state: SendSuggestionUiState): String {
        val draft = state.draft
        return buildString {
            appendLine("Categoría: ${draft.category.display}")
            appendLine()
            appendLine("Título:")
            appendLine(draft.title)
            appendLine()
            appendLine("Descripción:")
            appendLine(draft.description)
            appendLine()
            appendLine("---")
            appendLine("Información técnica (añadida automáticamente)")
            appendLine("App: Kropia v${getPlatform().versionName}")
            appendLine("Plataforma: ${getPlatform().name}")
            if (state.username.isNotBlank()) appendLine("Usuario: ${state.username}")
            if (state.email.isNotBlank()) appendLine("Email: ${state.email}")
            appendLine("Fecha: ${Clock.System.now()}")
        }
    }
}
