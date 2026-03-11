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
        // No phone permissions needed anymore - only device name
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
        public String getDeviceName() {
            return Build.MANUFACTURER + " " + Build.MODEL;
        }
    }
}
