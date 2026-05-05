package com.apptolast.greenhousefronts.data.feedback

/**
 * Auto-saved state of the suggestion/feedback form. Persisted by [FeedbackDraftStorage]
 * so the user does not lose what they typed if the app is backgrounded or the process is
 * killed by the OS. Cleared explicitly after a successful send or when the user confirms
 * abandoning the form via the back-arrow dialog.
 */
data class FeedbackDraft(
    val category: FeedbackCategory = FeedbackCategory.SUGGESTION,
    val title: String = "",
    val description: String = "",
) {
    val isBlank: Boolean get() = title.isBlank() && description.isBlank()
}
