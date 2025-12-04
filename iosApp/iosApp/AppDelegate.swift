//
//  AppDelegate.swift
//  iosApp
//
//  Created by Alberto Hidalgo on 03/12/2025.
//

import UIKit
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate {
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
