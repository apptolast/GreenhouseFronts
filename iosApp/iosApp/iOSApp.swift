import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

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