package io.stanc.pogoradar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import com.google.android.material.navigation.NavigationView
import io.stanc.pogoradar.appbar.AppbarManager
import io.stanc.pogoradar.appbar.PoGoToolbar
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_BODY
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_FLAG
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_LATITUDE
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_LONGITUDE
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TITLE
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.firebase.NotificationContent
import io.stanc.pogoradar.firebase.node.FirebaseUserNode
import io.stanc.pogoradar.subscreen.AppInfoLabelController
import io.stanc.pogoradar.firebase.NotificationHolder
import io.stanc.pogoradar.utils.PermissionManager
import io.stanc.pogoradar.utils.SystemUtils
import io.stanc.pogoradar.utils.WaitingSpinner
import kotlinx.android.synthetic.main.layout_navigation_header.*
import kotlinx.android.synthetic.main.layout_progress.*
import java.lang.ref.WeakReference


class NavDrawerActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var drawerLayout: DrawerLayout? = null
    private var appInfoLabelController: AppInfoLabelController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // content
        setContentView(R.layout.activity_drawer)

        // app bar
        setupToolbar()

        // app info label
        setupAppLabel()

        // navigation drawer
        setupDrawer()

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
        FirebaseUser.addAuthStateObserver(authStateObserver)
        FirebaseUser.addUserDataObserver(userDataObserver)
        FirebaseUser.startAuthentication()
        appInfoLabelController?.start()
    }

    override fun onPause() {
        FirebaseUser.stopAuthentication()
        FirebaseUser.removeAuthStateObserver(authStateObserver)
        FirebaseUser.removeUserDataObserver(userDataObserver)
        appInfoLabelController?.stop()
        super.onPause()
    }

    private fun showPopupIfUserIsLoggedOut() {
        if(FirebaseUser.authState() == FirebaseUser.AuthState.UserLoggedOut) {
            Popup.showInfo(this, title = R.string.authentication_state_signed_out, description = R.string.dialog_user_logged_out_message)
        }
    }

    override fun onBackPressed() {

        drawerLayout?.let { drawerLayout ->

            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                super.onBackPressed()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionManager.onRequestPermissionsResult(requestCode, this)
    }

    private fun setupToolbar() {

        (findViewById(R.id.activity_toolbar) as? PoGoToolbar)?.let { toolbar ->

            AppbarManager.setup(toolbar, resources.getString(R.string.default_app_title), defaultOnNavigationIconClicked = {

                findViewById<View>(R.id.nav_view)?.let { navView ->
                    drawerLayout?.openDrawer(navView)
                }
            })
        }
    }

    private fun setupAppLabel() {
        findViewById<View>(R.id.app_info_label)?.let {
            appInfoLabelController = AppInfoLabelController(it)
        }
    }

    private fun setupDrawer() {

        drawerLayout = findViewById(R.id.drawer_layout)

        drawerLayout?.let { drawerLayout ->

            val weakActivity = WeakReference(this)
            val toggle = object : ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

                override fun onDrawerOpened(drawerView: View) {
                    super.onDrawerOpened(drawerView)
                    weakActivity.get()?.let { SystemUtils.hideKeyboard(activity = it) }
                    updateNavText()
                }
            }

            drawerLayout.addDrawerListener(toggle)
            findViewById<NavigationView>(R.id.nav_view)?.setNavigationItemSelectedListener(this)

//            findViewById<NavigationView>(R.id.nav_view)?.let { navigationView ->
//                NavigationUI.setupWithNavController(navigationView, this.findNavController(R.id.nav_host_fragment))
//            }


            toggle.syncState()
        }
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

    /**
     * Navigation drawer
     */

    private val authStateObserver = object : FirebaseUser.AuthStateObserver {
        override fun authStateChanged(newAuthState: FirebaseUser.AuthState) {
            updateNavText()
        }
    }

    private val userDataObserver = object : FirebaseUser.UserDataObserver {
        override fun userDataChanged(user: FirebaseUserNode?) {
            updateNavText()
        }
    }

    private fun updateNavText() {
        nav_header_subtitle?.text = FirebaseUser.authStateText(baseContext)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.i(TAG, "Debug:: currentDestination: ${this.findNavController(R.id.nav_host_fragment).currentDestination?.id}")
        Log.d(TAG, "Debug:: mapInteractionFragment: ${R.id.mapInteractionFragment}")
        Log.d(TAG, "Debug:: accountFragment: ${R.id.accountFragment}")
        Log.d(TAG, "Debug:: policyFragment: ${R.id.policyFragment}")

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val backStackEntryCount = navHostFragment?.childFragmentManager?.backStackEntryCount
        Log.w(TAG, "Debug:: backStackEntryCount: $backStackEntryCount: ${navHostFragment?.childFragmentManager?.fragments}")

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
        drawerLayout?.closeDrawer(GravityCompat.START)
        return true
    }

    companion object {
        private val TAG = this::class.java.name
    }
}
