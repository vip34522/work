package com.example.schedulewidget

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val assetLoader = WebViewAssetLoader.Builder().addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            addJavascriptInterface(WebBridge(), "AndroidBridge")
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)
                override fun onPageFinished(view: WebView, url: String) { injectHook(view) }
            }
            loadUrl("https://appassets.androidplatform.net/assets/schedule.html")
        }
        setContentView(webView)
    }

    private fun injectHook(view: WebView) {
        val js = """
            (function(){
              if (window.__bridgeHooked) return;
              window.__bridgeHooked = true;
              const KEY = 'multi_shift_v34';
              const orig = Storage.prototype.setItem;
              Storage.prototype.setItem = function(k, v){
                orig.call(this, k, v);
                try { if (k === KEY && window.AndroidBridge) window.AndroidBridge.syncData(v); } catch(e){}
              };
              try {
                const d = localStorage.getItem(KEY);
                if (d && window.AndroidBridge) window.AndroidBridge.syncData(d);
              } catch(e){}
            })();
        """.trimIndent()
        view.evaluateJavascript(js, null)
    }

    inner class WebBridge {
        @JavascriptInterface
        fun syncData(json: String) { runOnUiThread { WidgetDataStore.save(applicationContext, json) } }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
