package com.apptolast.greenhousefronts.data.feedback

/**
 * Cross-platform "open the user's mail composer" hook. Builds a `mailto:` URI with the
 * provided recipients/subject/body and asks the OS (or browser) to open it; on Android
 * this is `Intent.ACTION_SENDTO`, on iOS `UIApplication.openURL`, on JVM `Desktop.mail`,
 * on the web `window.location.href`.
 *
 * Returns `true` if the OS could resolve a handler — `false` (or a thrown exception
 * caught upstream) means there's no mail client installed, in which case the caller
 * should surface a friendly message.
 *
 * Note: this is **not** an automatic background send. The user still has to tap "send"
 * in their mail client. The pre-filled subject/body keeps it nearly one-tap.
 */
interface MailLauncher {
    fun launch(recipients: List<String>, subject: String, body: String): Boolean
}

/**
 * Builds a properly-encoded `mailto:` URI. Shared across platforms so encoding rules
 * stay consistent (RFC 6068 — query parameters are percent-encoded, recipients go in
 * the path).
 */
internal fun buildMailtoUri(recipients: List<String>, subject: String, body: String): String {
    val to = recipients.joinToString(",") { encodeMailtoComponent(it) }
    val params = buildList {
        if (subject.isNotEmpty()) add("subject=" + encodeMailtoComponent(subject))
        if (body.isNotEmpty()) add("body=" + encodeMailtoComponent(body))
    }
    val query = if (params.isEmpty()) "" else "?" + params.joinToString("&")
    return "mailto:$to$query"
}

/**
 * Minimal RFC 3986 percent-encoder for `mailto:` query components. Kept here (instead of
 * pulling in a multiplatform URL library) because we only need a tiny subset: alnum +
 * `-_.~` pass through, everything else becomes `%XX`. Spaces become `%20`, not `+` —
 * `+` is reserved in `mailto:` query components.
 */
private fun encodeMailtoComponent(value: String): String = buildString(value.length) {
    for (b in value.encodeToByteArray()) {
        val c = b.toInt() and 0xFF
        val isUnreserved = (c in 'A'.code..'Z'.code) ||
                (c in 'a'.code..'z'.code) ||
                (c in '0'.code..'9'.code) ||
                c == '-'.code || c == '_'.code || c == '.'.code || c == '~'.code
        if (isUnreserved) {
            append(c.toChar())
        } else {
            append('%')
            append(HEX[c ushr 4])
            append(HEX[c and 0x0F])
        }
    }
}

private val HEX = "0123456789ABCDEF".toCharArray()
