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
        ) { granted, error in
            if let error = error {
                print("[Push] Notification permission error: \(error.localizedDescription)")
            } else {
                print("[Push] Notification permission granted=\(granted)")
            }
            DispatchQueue.main.async {
                application.registerForRemoteNotifications()
            }
        }

        return true
    }

    // MARK: - APNs token forwarding (FCM uses this internally)

    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let apnsToken = deviceToken.map {
            String(format: "%02.2hhx", $0)
        }
        .joined()
        print("[Push] APNs token received prefix=\(apnsToken.prefix(16)) length=\(apnsToken.count)")
        Messaging.messaging().apnsToken = deviceToken

        Messaging.messaging().token { token, error in
            if let error = error {
                print("[Push] FCM token fetch failed: \(error.localizedDescription)")
                return
            }
            guard let token = token, !token.isEmpty else {
                print("[Push] FCM token fetch returned empty token")
                return
            }
            print("[Push] FCM token fetched prefix=\(token.prefix(16))")
            IOSPushBridge.shared.pushNewToken(token: token)
        }
    }

    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("[Push] APNs registration failed: \(error.localizedDescription)")
    }

    // MARK: - FCM token rotation → Kotlin

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else {
            print("[Push] Messaging delegate returned empty FCM token")
            return
        }
        print("[Push] Messaging delegate FCM token prefix=\(token.prefix(16))")
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
