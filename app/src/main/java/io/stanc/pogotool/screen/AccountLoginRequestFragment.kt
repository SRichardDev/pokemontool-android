package io.stanc.pogotool.screen

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import io.stanc.pogotool.App
import io.stanc.pogotool.R
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.firebase.node.Team
import io.stanc.pogotool.utils.ShowFragmentManager
import io.stanc.pogotool.viewmodel.AccountViewModel

class AccountLoginRequestFragment: Fragment() {
    private val TAG = javaClass.name

    private val viewModel = AccountViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(R.layout.fragment_account_login, container, false)

        AppbarManager.setTitle(App.geString(R.string.authentication_app_title))

        setupTeamImages(rootLayout)

        rootLayout.findViewById<Button>(R.id.account_button_signin)?.setOnClickListener {
            // TODO...
        }

        rootLayout.findViewById<Button>(R.id.account_button_signup)?.setOnClickListener {
            val fragment = AccountLoginFragment.newInstance(viewModel)
            ShowFragmentManager.showFragment(fragment, fragmentManager, R.id.activity_content_layout)
            // TODO: if successful, after Button:Send/ close -> show AccountInfoFragment
        }

        rootLayout.findViewById<Button>(R.id.account_button_signout)?.setOnClickListener {
            // TODO...
        }

        return rootLayout
    }

    private fun setupTeamImages(rootLayout: View) {

        val teamImageMap = mapOf(Team.INSTINCT to R.drawable.icon_instinct_512dp,
                                                Team.MYSTIC to R.drawable.mystic,
                                                Team.VALOR to R.drawable.icon_valor_512dp)

        viewModel.teamOrder.get()?.let { teamOrder ->

            if (teamOrder.size != 3) {
                Log.e(TAG, "viewModel.teamOrder contains not 3 entries! teamOrder: $teamOrder")
                return
            }

            teamImageMap[teamOrder[0]]?.let { rootLayout.findViewById<ImageView>(R.id.account_imageView_0).setImageResource(it) }
            teamImageMap[teamOrder[1]]?.let { rootLayout.findViewById<ImageView>(R.id.account_imageView_1).setImageResource(it) }
            teamImageMap[teamOrder[2]]?.let { rootLayout.findViewById<ImageView>(R.id.account_imageView_2).setImageResource(it) }
        }
    }

}