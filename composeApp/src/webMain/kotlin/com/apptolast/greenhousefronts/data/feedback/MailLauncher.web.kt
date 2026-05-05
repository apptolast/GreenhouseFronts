package com.apptolast.greenhousefronts.data.feedback

import kotlinx.browser.window

/**
 * Web (`jsMain` + `wasmJsMain`) `mailto:` launcher. Sets `window.location.href` so the
 * browser hands the URL to whichever protocol handler the user has registered for
 * `mailto:` — typically the OS default mail client. If none is registered the click is
 * a no-op from the browser's side; we return `true` regardless because there's no API
 * to detect handler presence on the web.
 */
class WebMailLauncher : MailLauncher {

    override fun launch(recipients: List<String>, subject: String, body: String): Boolean {
        return runCatching {
            window.location.href = buildMailtoUri(recipients, subject, body)
            true
        }.getOrDefault(false)
    }
}
