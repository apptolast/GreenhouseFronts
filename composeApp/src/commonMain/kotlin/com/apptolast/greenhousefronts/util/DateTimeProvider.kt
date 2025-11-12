package com.apptolast.greenhousefronts.util

/**
 * Multiplatform date/time provider using expect/actual pattern
 *
 * This pattern enables platform-specific implementations when there is no
 * multiplatform library available or when direct access to native APIs is required.
 */
expect fun getCurrentTimestamp(): String
