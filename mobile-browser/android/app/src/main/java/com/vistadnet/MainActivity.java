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
        
        // Request runtime permission for phone state
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            requestPermissions(new String[]{android.Manifest.permission.READ_PHONE_STATE}, 1);
        }
        
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
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Phone state permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Phone state permission denied - IMEI access unavailable", Toast.LENGTH_LONG).show();
            }
        }
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
                // Check if we have permission
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    if (context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                        return "Permission Not Granted";
                    }
                }
                
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (telephonyManager == null) {
                    return "TelephonyManager Not Available";
                }
                
                // Try different methods based on Android version
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    // Android 8.0+ - try getImei() first
                    try {
                        return telephonyManager.getImei(0);
                    } catch (Exception e) {
                        // Fallback to getMeid for CDMA devices
                        try {
                            return telephonyManager.getMeid(0);
                        } catch (Exception e2) {
                            // Fallback to getDeviceId for older methods
                            try {
                                return telephonyManager.getDeviceId();
                            } catch (Exception e3) {
                                return "IMEI Access Restricted (Android 10+)";
                            }
                        }
                    }
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    // Android 6.0-7.1 - try getDeviceId
                    try {
                        String deviceId = telephonyManager.getDeviceId();
                        return deviceId != null ? deviceId : "Device ID Null";
                    } catch (Exception e) {
                        return "Device ID Access Failed";
                    }
                } else {
                    // Pre-Marshmallow - getDeviceId should work
                    try {
                        String deviceId = telephonyManager.getDeviceId();
                        return deviceId != null ? deviceId : "Device ID Null";
                    } catch (Exception e) {
                        return "Legacy Device ID Failed";
                    }
                }
            } catch (SecurityException e) {
                return "Security Exception: " + e.getMessage();
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }
        
        @JavascriptInterface
        public String getDeviceSerial() {
            try {
                // Try to get serial number (less restricted than IMEI)
                String serial = android.os.Build.SERIAL;
                return (serial != null && !serial.equals("unknown")) ? serial : "Serial Unknown";
            } catch (Exception e) {
                return "Serial Access Failed";
            }
        }
        
        @JavascriptInterface
        public String getAndroidId() {
            try {
                // Get Android ID (unique to device, less sensitive than IMEI)
                android.provider.Settings.Secure secureSettings = android.provider.Settings.Secure.getContentResolver(context);
                String androidId = android.provider.Settings.Secure.getString(secureSettings, android.provider.Settings.Secure.ANDROID_ID);
                return androidId != null ? androidId : "Android ID Null";
            } catch (Exception e) {
                return "Android ID Access Failed";
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
                return "Android " + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + ")";
            } catch (Exception e) {
                return "Unknown Version";
            }
        }
        
        @JavascriptInterface
        public String getAllDeviceInfo() {
            StringBuilder info = new StringBuilder();
            info.append("Device: ").append(getDeviceName()).append("\n");
            info.append("Version: ").append(getAndroidVersion()).append("\n");
            info.append("IMEI: ").append(getDeviceIMEI()).append("\n");
            info.append("Serial: ").append(getDeviceSerial()).append("\n");
            info.append("Android ID: ").append(getAndroidId());
            return info.toString();
        }
    }
}
