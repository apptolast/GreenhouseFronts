import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

    // Conecta el AppDelegate
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    
    init() {
        // Initialize Koin dependency injection
        KoinInitializerKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
