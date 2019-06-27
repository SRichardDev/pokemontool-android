package io.stanc.pogotool

import android.app.Activity
import android.webkit.WebView
import android.os.Bundle


class PrivacyPolicyActivity: Activity() {

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.activity_privay_policy)

        val wv = findViewById<WebView>(R.id.webview)
        wv.loadUrl("file:///android_asset/pp_android.html")
    }
}