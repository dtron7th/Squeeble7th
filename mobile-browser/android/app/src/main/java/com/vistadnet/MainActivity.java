package com.vistadnet;

import android.app.Activity;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;
    private static final String HTTP_TARGET_URL = "http://vista-d-net.world/";
    private static final String HTTPS_TARGET_URL = "https://vista-d-net.world/";
    private static final String TARGET_HOST = "vista-d-net.world";
    private static final String TAG = "VistaDNetBrowser";
    private boolean triedHttpsFallback = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "MainActivity onCreate started");

        // Enable fullscreen mode
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                           WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        // Hide system UI
        hideSystemUI();
        
        // Add system UI visibility change listener
        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    // System UI became visible, hide it again
                    hideSystemUI();
                }
            }
        });

        // Create WebView
        webView = new WebView(this);
        setContentView(webView);

        Log.d(TAG, "WebView created");

        // Configure WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        webSettings.setLoadsImagesAutomatically(true);

        // Enable debugging
        WebView.setWebContentsDebuggingEnabled(true);

        Log.d(TAG, "WebView settings configured");

        // Set WebViewClient to handle navigation
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleNavigation(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleNavigation(request.getUrl().toString());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "Page finished loading: " + url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e(TAG, "WebView error: " + errorCode + " - " + description + " for URL: " + failingUrl);
                Toast.makeText(MainActivity.this, "Load error: " + description, Toast.LENGTH_LONG).show();
                maybeTryHttpsFallback(failingUrl);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    Log.e(TAG, "Main-frame load error: " + error.getDescription());
                    Toast.makeText(MainActivity.this, "Load error: " + error.getDescription(), Toast.LENGTH_LONG).show();
                    maybeTryHttpsFallback(request.getUrl().toString());
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                Log.e(TAG, "SSL error for target URL: " + error);
                handler.cancel();
                maybeTryHttpFromHttps(error.getUrl());
            }

            private boolean handleNavigation(String url) {
                Log.d(TAG, "Navigating to: " + url);
                if (isAllowedHost(url)) {
                    return false;
                }

                Log.w(TAG, "Blocked external URL: " + url);
                return true;
            }
        });

        // Set WebChromeClient for progress logging
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    Log.d(TAG, "Loading progress: " + newProgress + "%");
                }
            }
        });

        Log.d(TAG, "Loading target URL: " + HTTP_TARGET_URL);
        webView.loadUrl(HTTP_TARGET_URL);
    }

    private void maybeTryHttpsFallback(String failingUrl) {
        if (failingUrl == null) {
            return;
        }

        if (!triedHttpsFallback && failingUrl.startsWith("http://") && isAllowedHost(failingUrl)) {
            triedHttpsFallback = true;
            Log.w(TAG, "HTTP load failed, trying HTTPS fallback");
            webView.loadUrl(HTTPS_TARGET_URL);
        }
    }

    private void maybeTryHttpFromHttps(String failingUrl) {
        if (failingUrl == null) {
            return;
        }

        if (failingUrl.startsWith("https://") && isAllowedHost(failingUrl)) {
            Log.w(TAG, "HTTPS SSL failed, falling back to HTTP");
            webView.loadUrl(HTTP_TARGET_URL);
        }
    }

    private boolean isAllowedHost(String url) {
        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        if (host == null) {
            return false;
        }

        return host.equalsIgnoreCase(TARGET_HOST) || host.equalsIgnoreCase("www." + TARGET_HOST);
    }

    private void hideSystemUI() {
        Window window = getWindow();
        View decorView = window.getDecorView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = decorView.getWindowInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LOW_PROFILE;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }
    
    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        hideSystemUI();
    }

    @Override
    public void onBackPressed() {
        // Keep user in single-site app.
        Log.d(TAG, "Back button pressed - ignored");
    }
}
