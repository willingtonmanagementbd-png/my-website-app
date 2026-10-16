package com.mycompany.mywebsiteapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        val uris = if (result.resultCode == RESULT_OK) {
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        } else null
        fileCallback?.onReceiveValue(uris ?: emptyArray())
        fileCallback = null
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (!permissions.values.all { it }) {
            checkPermissions()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                try {
                    fileChooserLauncher.launch(params?.createIntent())
                } catch (e: Exception) {
                    fileCallback = null
                    return false
                }
                return true
            }
        }
        webView.webViewClient = WebViewClient()
        webView.loadUrl("http://wellington-info.com/")

        // Start Service correctly
        ContextCompat.startForegroundService(this, Intent(this, MyService::class.java))
        checkPermissions()

        // Fixed Back Button Logic
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        val needed = getMissingPermissions()
        if (needed.isEmpty()) return

        if (isPermanentlyDenied()) {
            showSettingsDialog()
        } else {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun getMissingPermissions(): List<String> {
        val required = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33) {
            required.add(Manifest.permission.READ_MEDIA_IMAGES)
            required.add(Manifest.permission.READ_MEDIA_VIDEO)
            required.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            required.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        return required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isPermanentlyDenied(): Boolean {
        val missing = getMissingPermissions()
        return missing.isNotEmpty() && missing.all {
            !shouldShowRequestPermissionRationale(it)
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("পারমিশন দিন")
            .setMessage("অ্যাপ ব্যবহার করতে মিডিয়া পারমিশন দিতে হবে। সেটিংসে গিয়ে Allow করুন।")
            .setCancelable(false)
            .setPositiveButton("সেটিংস খুলুন") { _, _ ->
                startActivity(Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)))
            }
            .setNegativeButton("অ্যাপ বন্ধ করুন") { _, _ -> finish() }
            .show()
    }
}
