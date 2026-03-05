import UIKit

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        
        // Setup window
        window = UIWindow(frame: UIScreen.main.bounds)
        
        let viewController = ViewController()
        let navigationController = UINavigationController(rootViewController: viewController)
        
        window?.rootViewController = navigationController
        window?.makeKeyAndVisible()
        
        return true
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        // Keep app running in background if needed
    }

    func applicationWillEnterForeground(_ application: UIApplication) {
        // Refresh when app comes to foreground
    }
}
