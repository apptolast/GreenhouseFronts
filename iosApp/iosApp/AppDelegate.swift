//
//  AppDelegate.swift
//  iosApp
//
//  Created by Alberto Hidalgo on 03/12/2025.
//
//  IMPORTANT: Before this file compiles you MUST add the Firebase iOS SDK to the
//  Xcode project via Swift Package Manager:
//      File → Add Package Dependencies… → https://github.com/firebase/firebase-ios-sdk
//      Add the products: FirebaseMessaging (and FirebaseAnalytics if you want).
//  Also add the capabilities to the iosApp target:
//      Signing & Capabilities → + Capability → "Push Notifications"
//      Signing & Capabilities → + Capability → "Background Modes" → ☑ "Remote notifications"
//  And drop GoogleService-Info.plist into the iosApp target (Copy items if needed).

import UIKit
import FirebaseCore
import FirebaseMessaging
import UserNotifications
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate, MessagingDelegate, UNUserNotificationCenterDelegate {

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        FirebaseApp.configure()

        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self

        UNUserNotificationCenter.current().requestAuthorization(
            options: [.alert, .badge, .sound]
        ) { _, _ in
            /* result ignored: best effort */
        }
        application.registerForRemoteNotifications()

        return true
    }

    // MARK: - APNs token forwarding (FCM uses this internally)

    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    // MARK: - FCM token rotation → Kotlin

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else {
            return
        }
        IOSPushBridge.shared.pushNewToken(token: token)
    }

    // MARK: - Notification tap → Kotlin deep link

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        let severity = notification.request.content.userInfo["severity"] as? String
        guard IOSPushBridge.shared.shouldShowNotification(severity: severity) else {
            completionHandler([])
            return
        }
        completionHandler([.banner, .sound, .badge])
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        let userInfo = response.notification.request.content.userInfo
        var stringPayload: [String: String] = [:]
        for (key, value) in userInfo {
            if let k = key as? String {
                stringPayload[k] = (value as? String) ?? "\(value)"
            }
        }
        IOSPushBridge.shared.handleAlertDeepLink(payload: stringPayload)
        completionHandler()
    }

    // MARK: - Existing Universal Link handling for password reset (preserved)

    func application(_ application: UIApplication,
                     continue userActivity: NSUserActivity,
                     restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void) -> Bool {

        if userActivity.activityType == NSUserActivityTypeBrowsingWeb,
           let url = userActivity.webpageURL {
            print("Universal Link recibido: \(url)")

            if let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
               let token = components.queryItems?.first(where: { $0.name == "token" })?.value {
                AppLinkHandlerKt.HandleResetPassword(token: token)
            }
        }
        return true
    }
}
