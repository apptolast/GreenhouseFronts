package com.apptolast.greenhousefronts.data.feedback

import java.awt.Desktop
import java.net.URI

/**
 * Desktop (JVM) `mailto:` launcher. Goes through `java.awt.Desktop.mail(URI)` — which
 * delegates to whichever app the OS has registered for the `mailto:` scheme (Mail on
 * macOS, the default chooser on Linux, Outlook/Mail on Windows).
 *
 * `Desktop.isDesktopSupported()` and `isSupported(MAIL)` short-circuit the call on
 * headless environments or stripped-down Linux setups where AWT can't find a mail
 * handler — surface that as `false` so the UI can show a friendly fallback.
 */
class JvmMailLauncher : MailLauncher {

    override fun launch(recipients: List<String>, subject: String, body: String): Boolean {
        if (!Desktop.isDesktopSupported()) return false
        val desktop = Desktop.getDesktop()
        if (!desktop.isSupported(Desktop.Action.MAIL)) return false
        return runCatching {
            desktop.mail(URI(buildMailtoUri(recipients, subject, body)))
            true
        }.getOrDefault(false)
    }
}
