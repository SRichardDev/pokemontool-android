package io.stanc.pogoradar

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.stanc.pogoradar.appbar.AppbarManager
import io.stanc.pogoradar.appbar.PoGoToolbar
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_BODY
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_FLAG
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_LATITUDE
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_LONGITUDE
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TITLE
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.firebase.NotificationContent
import io.stanc.pogoradar.firebase.NotificationHolder
import io.stanc.pogoradar.subscreen.AppInfoLabelController
import io.stanc.pogoradar.utils.PermissionManager
import io.stanc.pogoradar.utils.WaitingSpinner
import kotlinx.android.synthetic.main.layout_progress.*


class StartActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private var appInfoLabelController: AppInfoLabelController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // content
        setContentView(R.layout.activity_start)

        // app bar
        setupToolbar()

        // app info label
        setupAppLabel()

        // navigation drawer
        setupNavigationView()

        // progress view
        WaitingSpinner.initialize(layout_progress, progressbar_text, window)

        // first screen
        showMapFragment()

        // notification check
        onNotification(intent)
    }

    override fun onStart() {
        super.onStart()
        showPopupIfUserIsLoggedOut()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        onNotification(intent)
    }

    private fun onNotification(intent: Intent) {
        intent.extras?.let { extras ->

            if (extras.containsKey(NOTIFICATION_FLAG)) {
                NotificationContent.new(extras.getString(NOTIFICATION_TITLE),
                                        extras.getString(NOTIFICATION_BODY),
                                        extras.getDouble(NOTIFICATION_LATITUDE),
                                        extras.getDouble(NOTIFICATION_LONGITUDE))?.let { notification ->

                    NotificationHolder.reportNotification(notification)
                    showMapFragment()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        FirebaseUser.startAuthentication()
        appInfoLabelController?.start()
    }

    override fun onPause() {
        FirebaseUser.stopAuthentication()
        appInfoLabelController?.stop()
        super.onPause()
    }

    private fun showPopupIfUserIsLoggedOut() {
        if(FirebaseUser.authState() == FirebaseUser.AuthState.UserLoggedOut) {
            Popup.showInfo(this, title = R.string.authentication_state_signed_out, description = R.string.dialog_user_logged_out_message)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionManager.onRequestPermissionsResult(requestCode, this)
    }

    private fun setupToolbar() {

        this.onBackPressed()

        (findViewById(R.id.activity_toolbar) as? PoGoToolbar)?.let { toolbar ->

            AppbarManager.setup(toolbar, resources.getString(R.string.default_app_title), defaultOnNavigationIconClicked = {

//                Log.i(TAG, "Debug:: OnNavigationIconClicked -> onBackPressed()")
//                this.onBackPressed()
            })
        }
    }

    private fun setupAppLabel() {
        findViewById<View>(R.id.app_info_label)?.let {
            appInfoLabelController = AppInfoLabelController(it)
        }
    }

    private fun setupNavigationView() {
        findViewById<BottomNavigationView>(R.id.navigationView)?.setOnNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.nav_account -> {
                showAccountFragment()
            }
            R.id.nav_map -> {
                showMapFragment()
            }
            R.id.nav_policy -> {
                showPolicyFragment()
            }
        }
        return true
    }

    /**
     * Screens
     */

    private fun showMapFragment() {

        if (this.findNavController(R.id.nav_host_fragment).currentDestination?.id != R.id.mapInteractionFragment) {
            this.findNavController(R.id.nav_host_fragment).popBackStack(R.id.mapInteractionFragment, false)
        }
    }

    private fun showAccountFragment() {

        this.findNavController(R.id.nav_host_fragment).popBackStack(R.id.mapInteractionFragment, false)
        this.findNavController(R.id.nav_host_fragment).navigate(R.id.action_mapInteractionFragment_to_accountFragment)
    }

    private fun showPolicyFragment() {

        this.findNavController(R.id.nav_host_fragment).popBackStack(R.id.mapInteractionFragment, false)
        this.findNavController(R.id.nav_host_fragment).navigate(R.id.action_mapInteractionFragment_to_policyFragment)
    }

    companion object {
        private val TAG = this::class.java.name
    }
}
