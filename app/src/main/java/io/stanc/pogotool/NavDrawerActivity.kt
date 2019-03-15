package io.stanc.pogotool

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import io.stanc.pogotool.firebase.FirebaseServer
import io.stanc.pogotool.firebase.data.FirebaseUserLocal
import io.stanc.pogotool.utils.SystemUtils
import io.stanc.pogotool.utils.WaitingSpinner
import kotlinx.android.synthetic.main.layout_activity_appbar.*
import kotlinx.android.synthetic.main.layout_activity_drawer.*
import kotlinx.android.synthetic.main.layout_nav_header.*
import kotlinx.android.synthetic.main.layout_progress.*
import java.lang.ref.WeakReference


class NavDrawerActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // content
        setContentView(R.layout.layout_activity_drawer)

        // appbar
        setSupportActionBar(toolbar)

        // navigation drawer
        val weakActivity = WeakReference(this)
        val toggle = object : ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                weakActivity.get()?.let { SystemUtils.hideKeyboard(activity = it) }
                removeAuthFragment()
            }
        }

        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)

        // progress view
        WaitingSpinner.initialize(layout_progress, window)

        // first fragment
        showMapFragment()
    }

    override fun onResume() {
        super.onResume()
        FirebaseServer.start()
        FirebaseServer.addUserProfileObserver(userProfileObserver)
        FirebaseServer.reloadUserData(baseContext)
    }

    override fun onPause() {
        FirebaseServer.removeUserProfileObserver(userProfileObserver)
        FirebaseServer.stop()
        super.onPause()
    }

    /**
     * Fragments
     */

    private fun showMapFragment() {

        supportFragmentManager.beginTransaction().replace(R.id.activity_content_framelayout,
            MapFragment(), MapFragment::class.java.name).commit()
    }

    private fun showAuthFragment() {

        val fragmentTag = AuthenticationFragment::class.java.name
        var fragment = supportFragmentManager?.findFragmentByTag(fragmentTag)

        if (fragment == null) {
            fragment = AuthenticationFragment()
        }

        supportFragmentManager.beginTransaction().add(R.id.activity_content_framelayout, fragment, fragmentTag).commit()
    }

    private fun removeAuthFragment() {

        val fragmentTag = AuthenticationFragment::class.java.name
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_appbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    /**
     * Navigation drawer
     */

    private val userProfileObserver = object : FirebaseServer.UserProfileObserver {
        override fun userProfileChanged(user: FirebaseUserLocal?) {
            Log.d(TAG, "Debug:: userProfileChanged(${user?.name})")
            updateNavText()
        }
    }

    private fun updateNavText() {
        nav_header_subtitle?.text = FirebaseServer.usersAuthenticationStateText(baseContext)
        nav_info?.text = getString(R.string.user_name, FirebaseServer.currentUser?.name)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_authentication -> {
                showAuthFragment()
            }
            R.id.nav_share -> {
                // TODO
            }
            R.id.nav_send -> {
                // TODO
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    companion object {
        private val TAG = this::class.java.name
    }
}
