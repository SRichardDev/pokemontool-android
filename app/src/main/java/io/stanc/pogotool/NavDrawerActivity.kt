package io.stanc.pogotool

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import io.stanc.pogotool.firebase.AuthenticationFragment
import io.stanc.pogotool.geohash.MapFragment
import kotlinx.android.synthetic.main.layout_activity_drawer.*
import kotlinx.android.synthetic.main.layout_activity_appbar.*
import kotlinx.android.synthetic.main.layout_nav_header.*
import kotlinx.android.synthetic.main.layout_progress.*

class NavDrawerActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // content
        setContentView(R.layout.layout_activity_drawer)

        // appbar
        setSupportActionBar(toolbar)

        // floating action buttons
        fab_update.setOnClickListener {
            (supportFragmentManager.findFragmentByTag(MapFragment::class.java.name) as MapFragment).updateData()
        }

        // navigation drawer
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        // progress view
        WaitingSpinner.initialize(layout_progress, window)

        // first fragment
        showMapFragment()
    }

    /**
     * Fragments
     */

    private fun showMapFragment() {

        supportFragmentManager.beginTransaction().replace(R.id.activity_content_framelayout,
            MapFragment(), MapFragment::class.java.name).commit()
    }

    private fun toggleAuthenticationFragment() {
        val fragmentTag = AuthenticationFragment::class.java.name

        supportFragmentManager?.findFragmentByTag(fragmentTag)?.let { fragment ->
            supportFragmentManager?.beginTransaction()?.remove(fragment)?.commit()

        } ?: kotlin.run {

            val fragment = AuthenticationFragment.newInstance(delegate)
            supportFragmentManager.beginTransaction().add(R.id.activity_content_framelayout,
                fragment, fragmentTag).commit()
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

    private val delegate = object: NavDrawerDelegate {

//        TODO: at the start of activity: get current authentication state -> FirebaseServer.class::~updateAuthenticationStateText(see AuthenticationFragment)
        override fun changeSubTitle(text: String) {
            Log.d(this.javaClass.name, "Debug:: changeSubTitle(text: $text) for nav_header_subtitle: $nav_header_subtitle")
            nav_header_subtitle.text = text
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_authentication -> {
                toggleAuthenticationFragment()
            }
            R.id.nav_share -> {

            }
            R.id.nav_send -> {

            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}
