package com.example.schedulewidget

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // 用于接收网页点击“上传头像”后返回的文件
    private val fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (filePathCallback == null) return@registerForActivityResult
        var results: Array<Uri>? = null
        if (result.resultCode == RESULT_OK && result.data != null) {
            val dataString = result.data?.dataString
            if (dataString != null) {
                results = arrayOf(Uri.parse(dataString))
            }
        }
        filePathCallback?.onReceiveValue(results)
        filePathCallback = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // 允许长按
            isLongClickable = true
            setOnLongClickListener { 
                val result = hitTestResult
                if (result.type == HitTestResult.IMAGE_TYPE || result.type == HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                    // 如果长按的是图片，触发保存逻辑
                    saveImage(result.extra)
                    true
                } else {
                    false
                }
            }

            addJavascriptInterface(WebBridge(), "AndroidBridge")
            
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)
                override fun onPageFinished(view: WebView, url: String) { injectHook(view) }
            }

            // 【核心修复1】重写 WebChromeClient，解决弹窗、选文件等问题
            webChromeClient = object : WebChromeClient() {
                // 1. 允许网页弹出 confirm 提示框（解决清空数据、删除点击无效）
                override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("提示")
                        .setMessage(message)
                        .setPositiveButton("确定") { _, _ -> result?.confirm() }
                        .setNegativeButton("取消") { _, _ -> result?.cancel() }
                        .setOnCancelListener { result?.cancel() }
                        .show()
                    return true
                }

                // 2. 允许网页弹出 alert 提示
                override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("提示")
                        .setMessage(message)
                        .setPositiveButton("知道了") { _, _ -> result?.confirm() }
                        .setOnCancelListener { result?.cancel() }
                        .show()
                    return true
                }

                // 3. 解决“上传头像”点击没反应
                override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                    this@MainActivity.filePathCallback?.onReceiveValue(null)
                    this@MainActivity.filePathCallback = filePathCallback
                    
                    val intent = fileChooserParams?.createIntent()
                    if (intent != null) {
                        try {
                            fileChooserLauncher.launch(intent)
                            return true
                        } catch (e: Exception) {
                            this@MainActivity.filePathCallback = null
                            return false
                        }
                    }
                    return false
                }
            }

            loadUrl("https://appassets.androidplatform.net/assets/schedule.html")
        }
        setContentView(webView)
    }

    // 【核心修复2】长按保存图片功能（兼容 Base64 图片）
    private fun saveImage(imageUrl: String?) {
        if (imageUrl == null) return
        try {
            var bitmap: Bitmap? = null
            if (imageUrl.startsWith("data:image")) {
                // 处理网页生成的 Base64 图片
                val base64Data = imageUrl.substringAfter(",")
                val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            }
            
            if (bitmap != null) {
                val filename = "Export_${System.currentTimeMillis()}.png"
                val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val file = File(path, filename)
                val out: OutputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
                out.close()
                
                // 通知相册刷新
                MediaStore.Images.Media.insertImage(contentResolver, file.absolutePath, filename, "排班导出图片")
                Toast.makeText(this, "图片已保存到相册！", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "保存失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
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
}
