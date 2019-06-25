package io.stanc.pogotool.screen

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import io.stanc.pogotool.R

class PolicyFragment: Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(R.layout.fragment_policy, container, false)

        rootLayout.findViewById<Button>(R.id.policy_license)?.setOnClickListener {
            startActivity(Intent(context, OssLicensesMenuActivity::class.java))
        }

        rootLayout.findViewById<Button>(R.id.policy_privacy)?.setOnClickListener {

        }

        return rootLayout
    }
}