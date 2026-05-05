package com.apptolast.greenhousefronts.data.feedback

/**
 * User-facing classification of an in-app feedback message. The Spanish [display] name
 * is what shows up both in the dropdown and in the email subject ("Kropia - {display} de
 * {usuario}"), so changes here affect what the receiving inbox sees in the subject line.
 */
enum class FeedbackCategory(val display: String) {
    BUG("Bug"),
    SUGGESTION("Sugerencia"),
    IMPROVEMENT("Mejora"),
    OTHER("Otro"),
}
