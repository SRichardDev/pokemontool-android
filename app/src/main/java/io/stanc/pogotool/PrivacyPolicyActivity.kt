package io.stanc.pogotool

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.appbar.PoGoToolbar


class PrivacyPolicyActivity: Activity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privay_policy)

        setupWebView()
        setupToolbar()
    }

    private fun setupWebView() {

        findViewById<WebView>(R.id.webview)?.let { webview ->

            webview.settings.javaScriptEnabled = true // enable javascript

            val context = applicationContext
            webview.webViewClient = object : WebViewClient() {
                override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                    Toast.makeText(context, description, Toast.LENGTH_SHORT).show()
                }

                @TargetApi(android.os.Build.VERSION_CODES.M)
                override fun onReceivedError(view: WebView, req: WebResourceRequest, rerr: WebResourceError) {
                    // Redirect to deprecated method, so you can use it in all SDK versions
                    onReceivedError(view, rerr.errorCode, rerr.description.toString(), req.url.toString())
                }
            }

            webview.loadUrl("https://sites.google.com/view/pogoradar-privacypolicy")
        }
    }

    private fun setupToolbar() {

        (findViewById(R.id.activity_toolbar) as? PoGoToolbar)?.let { toolbar ->

            AppbarManager.setup(toolbar, defaultOnNavigationIconClicked = {
                // TODO...
            })
        }
    }

    override fun onResume() {
        super.onResume()
        AppbarManager.setTitle(App.geString(R.string.policy_button_privacy_policy))
    }

    override fun onPause() {
        AppbarManager.setTitle(getString(R.string.default_app_title))
        super.onPause()
    }
}