package com.apptolast.greenhousefronts.data.feedback

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Android `mailto:` launcher. Uses `Intent.ACTION_SENDTO` (not `ACTION_SEND`) so only
 * email apps see it — chooser dialogs filtered to compose-mail apps only. The
 * `FLAG_ACTIVITY_NEW_TASK` flag is required because we hold an `applicationContext`,
 * not an `Activity` (the latter would tie us to a specific lifecycle).
 */
class AndroidMailLauncher(private val context: Context) : MailLauncher {

    override fun launch(recipients: List<String>, subject: String, body: String): Boolean {
        val uri = Uri.parse(buildMailtoUri(recipients, subject, body))
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
