package io.stanc.pogoradar

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import io.stanc.pogoradar.appbar.AppbarManager
import io.stanc.pogoradar.appbar.PoGoToolbar


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
                    Popup.showToast(context, description)
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

            AppbarManager.setup(toolbar, resources.getString(R.string.policy_button_privacy_policy), defaultOnNavigationIconClicked = {
                // TODO...
            })
        }
    }
}