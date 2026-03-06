package com.vistadnet;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create WebView
        webView = new WebView(this);
        setContentView(webView);
        
        // Enable JavaScript
        webView.getSettings().setJavaScriptEnabled(true);
        
        // Set WebViewClient to stay within app
        webView.setWebViewClient(new WebViewClient());
        
        // Load your website
        webView.loadUrl("http://vista-d-net.world/");
    }
    
    @Override
    public void onBackPressed() {
        // Keep user in app
    }
}
