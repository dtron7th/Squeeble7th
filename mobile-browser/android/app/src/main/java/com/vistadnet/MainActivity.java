package com.vistadnet;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;

public class MainActivity extends Activity {

    private WebView webView;
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private static final int REFRESH_INTERVAL = 30000; // 30 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create WebView
        webView = new WebView(this);
        setContentView(webView);
        
        // Enable JavaScript
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        // Enable debugging for troubleshooting
        WebView.setWebContentsDebuggingEnabled(true);
        
        // Set WebViewClient to stay within app
        webView.setWebViewClient(new WebViewClient());
        
        // Load your website
        webView.loadUrl("http://vista-d-net.world/");
        
        // Initialize auto-refresh
        initializeAutoRefresh();
    }
    
    @Override
    public void onBackPressed() {
        // Keep user in app
    }
    
    private void initializeAutoRefresh() {
        refreshHandler = new Handler();
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (webView != null) {
                    webView.reload();
                }
                refreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
        
        // Start auto-refresh after initial load
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop auto-refresh when app is destroyed
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }
}
