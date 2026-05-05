package com.apptolast.greenhousefronts.data.feedback

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Write-through cache for the in-progress feedback/suggestion draft.
 *
 * Multiplatform-settings is the source of truth on disk; the in-memory
 * [MutableStateFlow] is initialised from disk at construction. Every setter writes
 * to both, so collectors get the new value immediately and the form survives both
 * a process death (read-from-disk on next launch) and a screen recomposition.
 */
class FeedbackDraftStorage {

    private val settings: Settings = Settings()

    private val _draft = MutableStateFlow(loadFromDisk())
    val draft: StateFlow<FeedbackDraft> = _draft.asStateFlow()

    fun setCategory(category: FeedbackCategory) {
        settings.putString(KEY_CATEGORY, category.name)
        _draft.value = _draft.value.copy(category = category)
    }

    fun setTitle(title: String) {
        settings.putString(KEY_TITLE, title)
        _draft.value = _draft.value.copy(title = title)
    }

    fun setDescription(description: String) {
        settings.putString(KEY_DESCRIPTION, description)
        _draft.value = _draft.value.copy(description = description)
    }

    fun clear() {
        settings.remove(KEY_CATEGORY)
        settings.remove(KEY_TITLE)
        settings.remove(KEY_DESCRIPTION)
        _draft.value = FeedbackDraft()
    }

    private fun loadFromDisk(): FeedbackDraft {
        val storedCategory = settings.getStringOrNull(KEY_CATEGORY)
        val category = FeedbackCategory.entries.firstOrNull { it.name == storedCategory }
            ?: FeedbackCategory.SUGGESTION
        return FeedbackDraft(
            category = category,
            title = settings.getString(KEY_TITLE, ""),
            description = settings.getString(KEY_DESCRIPTION, ""),
        )
    }

    companion object {
        private const val KEY_CATEGORY = "feedback_draft_category"
        private const val KEY_TITLE = "feedback_draft_title"
        private const val KEY_DESCRIPTION = "feedback_draft_description"
    }
}
