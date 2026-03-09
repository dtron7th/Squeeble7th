package com.vistadnet;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView webView;
    private static final String TARGET_URL = "http://vista-d-net.world/";
    private static final String TARGET_URL_HTTPS = "https://vista-d-net.world/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize WebView
        webView = new WebView(this);
        setContentView(webView);
        
        // Configure WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        
        // Enable debugging
        WebView.setWebContentsDebuggingEnabled(true);
        
        // Add JavaScript interface for device info
        webView.addJavascriptInterface(new DeviceInfoInterface(this), "AndroidDevice");
        
        // Set WebViewClient with navigation filtering
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleNavigation(url);
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                return handleNavigation(request.getUrl().toString());
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                if (!isAllowedHost(url)) {
                    view.loadUrl(TARGET_URL);
                }
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(MainActivity.this, "Load error: " + description, Toast.LENGTH_LONG).show();
                maybeTryHttpsFallback(failingUrl);
            }
            
            @Override
            public void onReceivedError(WebView view, android.webkit.WebResourceRequest request, android.webkit.WebResourceError error) {
                if (request.isForMainFrame()) {
                    Toast.makeText(MainActivity.this, "Load error: " + error.getDescription(), Toast.LENGTH_LONG).show();
                    maybeTryHttpsFallback(request.getUrl().toString());
                }
            }
            
            @Override
            public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
                handler.cancel();
                maybeTryHttpFromHttps(error.getUrl());
            }
        });
        
        // Load initial URL
        webView.loadUrl(TARGET_URL);
        
        // Set fullscreen immersive mode
        setFullscreenMode();
    }
    
    private boolean handleNavigation(String url) {
        if (isAllowedHost(url)) {
            return false; // Allow navigation
        } else {
            // Block navigation to external sites
            Toast.makeText(this, "Navigation to external sites is not allowed", Toast.LENGTH_SHORT).show();
            webView.loadUrl(TARGET_URL); // Redirect back to allowed site
            return true; // Override navigation
        }
    }
    
    private boolean isAllowedHost(String url) {
        return url.contains("vista-d-net.world");
    }
    
    private void maybeTryHttpsFallback(String failingUrl) {
        if (failingUrl.startsWith("http://") && failingUrl.contains("vista-d-net.world")) {
            String httpsUrl = failingUrl.replace("http://", "https://");
            webView.loadUrl(httpsUrl);
        }
    }
    
    private void maybeTryHttpFromHttps(String failingUrl) {
        if (failingUrl.startsWith("https://") && failingUrl.contains("vista-d-net.world")) {
            String httpUrl = failingUrl.replace("https://", "http://");
            webView.loadUrl(httpUrl);
        }
    }
    
    private void setFullscreenMode() {
        getWindow().getDecorView().setSystemUiVisibility(
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN |
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }
    
    @Override
    public void onBackPressed() {
        // Keep user in app - don't allow back button to exit
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        setFullscreenMode(); // Reapply fullscreen mode
    }
    
    /**
     * JavaScript interface class to expose device information to web content
     */
    public class DeviceInfoInterface {
        private Context context;
        
        public DeviceInfoInterface(Context context) {
            this.context = context;
        }
        
        @JavascriptInterface
        public String getDeviceIMEI() {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    if (context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                        if (telephonyManager != null) {
                            return telephonyManager.getImei();
                        }
                    }
                }
                return "Permission Denied";
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }
        
        @JavascriptInterface
        public String getDeviceName() {
            try {
                return android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
            } catch (Exception e) {
                return "Unknown Device";
            }
        }
        
        @JavascriptInterface
        public String getAndroidVersion() {
            try {
                return "Android " + android.os.Build.VERSION.RELEASE;
            } catch (Exception e) {
                return "Unknown Version";
            }
        }
    }
}
