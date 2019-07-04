package io.stanc.pogoradar

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import io.stanc.pogoradar.appbar.AppbarManager
import io.stanc.pogoradar.appbar.PoGoToolbar
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.firebase.node.FirebaseUserNode
import io.stanc.pogoradar.screen.AccountFragment
import io.stanc.pogoradar.screen.MapInteractionFragment
import io.stanc.pogoradar.screen.PolicyFragment
import io.stanc.pogoradar.subscreen.AppInfoLabelController
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

    override fun onBackPressed() {

        drawerLayout?.let { drawerLayout ->

            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                super.onBackPressed()
            }
        }
    }

    private fun setupToolbar() {

        (findViewById(R.id.activity_toolbar) as? PoGoToolbar)?.let { toolbar ->

            AppbarManager.setup(toolbar, defaultOnNavigationIconClicked = {

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

            toggle.syncState()
        }
    }

    /**
     * Screens
     */

    private fun showMapFragment() {

        val fragmentTag = MapInteractionFragment::class.java.name
        var fragment = supportFragmentManager.findFragmentByTag(fragmentTag)

        if (fragment == null) {
            fragment = MapInteractionFragment()
        }

        supportFragmentManager.beginTransaction().replace(R.id.activity_content_layout, fragment, fragmentTag).commit()
    }

    private fun showAccountFragment() {

        val fragmentTag = AccountFragment::class.java.name
        var fragment = supportFragmentManager.findFragmentByTag(fragmentTag)

        if (fragment == null) {
            fragment = AccountFragment()
        }

        supportFragmentManager.beginTransaction().replace(R.id.activity_content_layout, fragment, fragmentTag).commit()
    }

    private fun showPolicyFragment() {

        val fragmentTag = PolicyFragment::class.java.name
        var fragment = supportFragmentManager.findFragmentByTag(fragmentTag)

        if (fragment == null) {
            fragment = PolicyFragment()
        }

        supportFragmentManager.beginTransaction().replace(R.id.activity_content_layout, fragment, fragmentTag).commit()
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
        // Handle navigation view item clicks here.
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
