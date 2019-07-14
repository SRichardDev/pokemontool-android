package io.stanc.pogoradar.utils

import android.util.Log
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

object ShowFragmentManager {
    private val TAG = javaClass.name

    fun showFragment(fragment: Fragment?, fragmentManager: FragmentManager?, @IdRes layoutResId: Int) {

        if (fragment == null || fragmentManager == null) {
            Log.e(TAG, "could not showInfo fragment: $fragment, fragmentManager: $fragmentManager!")
            return
        }

        val fragmentTag = fragment::class.java.name

        if(!fragmentAlreadyExists(fragmentTag, fragmentManager)) {
            fragmentManager.beginTransaction().add(layoutResId, fragment, fragmentTag).addToBackStack(null).commit()
        }
    }

    fun replaceFragment(fragment: Fragment?, fragmentManager: FragmentManager?, @IdRes layoutResId: Int) {

        if (fragment == null || fragmentManager == null) {
            Log.e(TAG, "could not showInfo fragment: $fragment, fragmentManager: $fragmentManager!")
            return
        }

        val fragmentTag = fragment::class.java.name

        if(!fragmentAlreadyExists(fragmentTag, fragmentManager)) {
            fragmentManager.beginTransaction().replace(layoutResId, fragment, fragmentTag).commit()
        }
    }

    private fun fragmentAlreadyExists(fragmentTag: String, fragmentManager: FragmentManager): Boolean {
        val fragment = fragmentManager.findFragmentByTag(fragmentTag)
        return fragment?.isAdded ?: false
    }
}