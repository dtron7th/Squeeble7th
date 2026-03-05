import UIKit
import WebKit

class ViewController: UIViewController, WKNavigationDelegate {
    
    private var webView: WKWebView!
    private let targetURL = "https://vista-d-net.world/"
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Setup WebView
        let webConfiguration = WKWebViewConfiguration()
        webConfiguration.allowsInlineMediaPlayback = true
        webConfiguration.mediaTypesRequiringUserActionForPlayback = []
        
        webView = WKWebView(frame: view.bounds, configuration: webConfiguration)
        webView.navigationDelegate = self
        webView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(webView)
        
        // Enable fullscreen
        setupFullscreen()
        
        // Load target URL
        if let url = URL(string: targetURL) {
            let request = URLRequest(url: url)
            webView.load(request)
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        hideStatusBar()
    }
    
    private func setupFullscreen() {
        // Hide navigation bar
        navigationController?.setNavigationBarHidden(true, animated: false)
        
        // Hide tab bar
        tabBarController?.tabBar.isHidden = true
        
        // Set constraints to fill entire screen
        webView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            webView.topAnchor.constraint(equalTo: view.topAnchor),
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            webView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
    }
    
    private func hideStatusBar() {
        UIApplication.shared.isStatusBarHidden = true
        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene {
            windowScene.statusBarManager?.statusBarFrame = .zero
        }
    }
    
    // MARK: - WKNavigationDelegate
    
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        
        guard let url = navigationAction.request.url else {
            decisionHandler(.cancel)
            return
        }
        
        let urlString = url.absoluteString
        
        // Only allow vista-d-net.world URLs
        if urlString.starts(with: "https://vista-d-net.world/") {
            decisionHandler(.allow)
        } else {
            decisionHandler(.cancel)
        }
    }
    
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        // Ensure we're always on the target URL
        if let currentURL = webView.url?.absoluteString {
            if !currentURL.starts(with: "https://vista-d-net.world/") {
                if let url = URL(string: targetURL) {
                    let request = URLRequest(url: url)
                    webView.load(request)
                }
            }
        }
    }
    
    // MARK: - Device Orientation
    
    override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return .portrait
    }
    
    override var preferredInterfaceOrientationForPresentation: UIInterfaceOrientation {
        return .portrait
    }
    
    // MARK: - Status Bar
    
    override var prefersStatusBarHidden: Bool {
        return true
    }
    
    override var preferredStatusBarUpdateAnimation: UIStatusBarAnimation {
        return .none
    }
}
