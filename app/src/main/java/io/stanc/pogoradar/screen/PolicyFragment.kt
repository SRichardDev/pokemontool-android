package io.stanc.pogoradar.screen

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import io.stanc.pogoradar.App
import io.stanc.pogoradar.PrivacyPolicyActivity
import io.stanc.pogoradar.R
import io.stanc.pogoradar.appbar.AppbarManager

class PolicyFragment: Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(R.layout.fragment_policy, container, false)

        // TODO:...
//        rootLayout.findViewById<Button>(R.id.policy_terms_conditions)?.setOnClickListener {
//        }

        rootLayout.findViewById<Button>(R.id.policy_privacy)?.setOnClickListener {
            startActivity(Intent(context, PrivacyPolicyActivity::class.java))
        }

        rootLayout.findViewById<Button>(R.id.policy_license)?.setOnClickListener {
            startActivity(Intent(context, OssLicensesMenuActivity::class.java))
        }

        return rootLayout
    }

    override fun onResume() {
        super.onResume()
        AppbarManager.setTitle(App.geString(R.string.policy_app_title))
    }

    override fun onPause() {
        AppbarManager.reset()
        super.onPause()
    }
}