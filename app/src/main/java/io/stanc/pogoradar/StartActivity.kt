package io.stanc.pogoradar

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.stanc.pogoradar.UpdateManager.showVersionInfoIfNotAlreadyShown
import io.stanc.pogoradar.appbar.AppbarManager
import io.stanc.pogoradar.appbar.PoGoToolbar
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_BODY
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_LATITUDE
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_LONGITUDE
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TITLE
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TYPE
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TYPE_QUEST
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TYPE_RAID
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.firebase.notification.NotificationContent
import io.stanc.pogoradar.firebase.notification.NotificationHolder
import io.stanc.pogoradar.firebase.notification.NotificationSettings
import io.stanc.pogoradar.screen.MapInteractionFragment
import io.stanc.pogoradar.utils.*
import kotlinx.android.synthetic.main.layout_progress.*


class StartActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private val TAG = this::class.java.name

    private var bottomNavigationView: BottomNavigationView? = null

    private val systemObserver = object: SystemUtils.SystemObserver {
        override fun onKeyboardVisibilityDidChange(isKeyboardVisible: Boolean) {
            bottomNavigationView?.visibility = if (isKeyboardVisible) View.GONE else View.VISIBLE
        }
    }

    private val delayedInfoLabelStart = DelayedTrigger(5000) {
        // TODO: add info banner for connection lost after several seconds after start
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // content
        setContentView(R.layout.activity_start)

        // app bar
        setupToolbar()

        // navigation drawer
        setupNavigationView()

        // progress view
        WaitingSpinner.initialize(layout_progress, progressbar_text, window)

        // notification check
        onNotification(intent)

        // map screen
        setupMapScreen()
    }

    override fun onStart() {
        super.onStart()
        FirebaseUser.startAuthentication()
        delayedInfoLabelStart.trigger()
        SystemUtils.addObserver(systemObserver, this)
        showPopupIfUserIsLoggedOut()
        UpdateManager.start()
    }

    override fun onResume() {
        super.onResume()
        FirebaseUser.refresh()
        showVersionInfoIfNotAlreadyShown(this)
    }

    override fun onStop() {
        UpdateManager.stop()
        FirebaseUser.stopAuthentication()
        SystemUtils.removeObserver(systemObserver)
        NotificationSettings.informUserAboutDisabledNotificationsIfNeeded()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        onNotification(intent)
    }

    private fun onNotification(intent: Intent) {

        intent.extras?.let { extras ->

            // TODO: NOTIFICATION_TYPE == NOTIFICATION_TYPE_CHAT
            if (extras.containsKey(NOTIFICATION_TYPE) && (extras.getString(NOTIFICATION_TYPE) == NOTIFICATION_TYPE_RAID || extras.getString(NOTIFICATION_TYPE) == NOTIFICATION_TYPE_QUEST)) {

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

        (findViewById(R.id.activity_toolbar) as? PoGoToolbar)?.let { toolbar ->

            AppbarManager.setup(toolbar, resources.getString(R.string.default_app_title), defaultOnNavigationIconClicked = {
                this.onBackPressed()
            })
        }
    }

    private fun setupNavigationView() {
        bottomNavigationView = findViewById(R.id.navigationView)
        bottomNavigationView?.setOnNavigationItemSelectedListener(this)
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

    private fun setupMapScreen() {
        ShowFragmentManager.replaceFragment(MapInteractionFragment(), supportFragmentManager, R.id.fragment_map)
    }

    private fun showMapFragment() {

        if (this.findNavController(R.id.nav_host_fragment).currentDestination?.id != R.id.blankFragment) {
            this.findNavController(R.id.nav_host_fragment).popBackStack(R.id.blankFragment, false)
        }
    }

    private fun showAccountFragment() {

        if (this.findNavController(R.id.nav_host_fragment).currentDestination?.id != R.id.accountFragment) {
            this.findNavController(R.id.nav_host_fragment).popBackStack(R.id.blankFragment, false)
            this.findNavController(R.id.nav_host_fragment).navigate(R.id.action_blankFragment_to_accountFragment)
        }
    }

    private fun showPolicyFragment() {

        if (this.findNavController(R.id.nav_host_fragment).currentDestination?.id != R.id.policyFragment) {
            this.findNavController(R.id.nav_host_fragment).popBackStack(R.id.blankFragment, false)
            this.findNavController(R.id.nav_host_fragment).navigate(R.id.action_blankFragment_to_policyFragment)
        }
    }
}
