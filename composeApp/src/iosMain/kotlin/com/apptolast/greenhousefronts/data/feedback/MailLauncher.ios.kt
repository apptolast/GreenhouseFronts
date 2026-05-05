package com.apptolast.greenhousefronts.data.feedback

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * iOS `mailto:` launcher. Hands the URL to `UIApplication.openURL` — iOS resolves it
 * to whichever mail composer is registered (Mail.app, Gmail, Outlook…). If no mail
 * app is installed and configured the open call returns `false` synchronously and
 * we surface that upstream.
 */
class IosMailLauncher : MailLauncher {

    override fun launch(recipients: List<String>, subject: String, body: String): Boolean {
        val url = NSURL.URLWithString(buildMailtoUri(recipients, subject, body)) ?: return false
        if (!UIApplication.sharedApplication.canOpenURL(url)) return false
        UIApplication.sharedApplication.openURL(url)
        return true
    }
}
