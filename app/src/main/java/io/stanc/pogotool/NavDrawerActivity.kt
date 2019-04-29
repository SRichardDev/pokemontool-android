package io.stanc.pogotool

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.appbar.PoGoToolbar
import io.stanc.pogotool.firebase.FirebaseServer
import io.stanc.pogotool.firebase.FirebaseUser
import io.stanc.pogotool.firebase.node.FirebaseUserNode
import io.stanc.pogotool.utils.SystemUtils
import io.stanc.pogotool.utils.WaitingSpinner
import kotlinx.android.synthetic.main.activity_drawer.*
import kotlinx.android.synthetic.main.layout_drawer_navigationview.*
import kotlinx.android.synthetic.main.layout_navigation_header.*
import kotlinx.android.synthetic.main.layout_progress.*
import java.lang.ref.WeakReference


class NavDrawerActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // content
        setContentView(R.layout.activity_drawer)

        // app bar
        setupToolbar()

        // navigation drawer
        setupDrawer()

        // progress view
        WaitingSpinner.initialize(layout_progress, progressbar_text, window)

        // first screen
        showMapFragment()
    }

    override fun onResume() {
        super.onResume()
        FirebaseUser.startAuthentication()
        FirebaseUser.addUserProfileObserver(userProfileObserver)
//        FirebaseUser.reloadUserData(baseContext)
    }

    override fun onPause() {
        FirebaseUser.removeUserProfileObserver(userProfileObserver)
        FirebaseUser.stopAuthentication()
        super.onPause()
    }

    private fun setupToolbar() {

        (findViewById(R.id.activity_toolbar) as? PoGoToolbar)?.let { toolbar ->

            AppbarManager.setup(toolbar, defaultOnNavigationIconClicked = {
                drawer_layout?.openDrawer(nav_view)
            })
        }
    }

    private fun setupDrawer() {

        val weakActivity = WeakReference(this)
        val toggle = object : ActionBarDrawerToggle(this, drawer_layout, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                weakActivity.get()?.let { SystemUtils.hideKeyboard(activity = it) }
            }
        }

        drawer_layout?.addDrawerListener(toggle)
        nav_view?.setNavigationItemSelectedListener(this)

        toggle.syncState()
    }

    /**
     * Screens
     */

    private fun showMapFragment() {

        val fragmentTag = MapInteractionFragment::class.java.name
        var fragment = supportFragmentManager?.findFragmentByTag(fragmentTag)

        if (fragment == null) {
            fragment = MapInteractionFragment()
        }

        supportFragmentManager.beginTransaction().replace(R.id.activity_content_layout, fragment, fragmentTag).commit()
    }

    private fun showAuthFragment() {

        val fragmentTag = AccountFragment::class.java.name
        var fragment = supportFragmentManager?.findFragmentByTag(fragmentTag)

        if (fragment == null) {
            fragment = AccountFragment()
        }

        supportFragmentManager.beginTransaction().replace(R.id.activity_content_layout, fragment, fragmentTag).commit()
    }

    private fun removeAuthFragment() {

        val fragmentTag = AccountFragment::class.java.name
        supportFragmentManager?.findFragmentByTag(fragmentTag)?.let { fragment ->
            supportFragmentManager?.beginTransaction()?.remove(fragment)?.commit()
        }
    }

    /**
     * backpress, options, menu
     */

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Navigation drawer
     */

    private val userProfileObserver = object : FirebaseUser.UserProfileObserver {
        override fun userProfileChanged(user: FirebaseUserNode?) {
            updateNavText()
        }
    }

    private fun updateNavText() {
        nav_header_subtitle?.text = FirebaseUser.authStateText(baseContext)
        nav_info?.text = getString(R.string.user_name, FirebaseUser.currentUser?.trainerName)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_account -> {
                showAuthFragment()
            }
            R.id.nav_map -> {
                showMapFragment()
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    companion object {
        private val TAG = this::class.java.name
    }
}
