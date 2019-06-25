package io.stanc.pogotool.appbar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

interface Toolbar {

    fun setVisibility(visible: Boolean)

    fun setNavigationIcon(@DrawableRes navigationIconResID: Int)
    fun setNavigationIconClickListener(onNavigationIconClicked: () -> Unit)
    fun showNavigationIcon()
    fun hideNavigationIcon()

    fun setTitle(text: String)
    fun setTitle(@StringRes stringResID: Int)
    fun setTitleLongClickListener(onTitleLongClicked: () -> Unit)

    fun showMenu()
    fun hideMenu()

    fun setMenuItemIcon(@DrawableRes menuIconResID: Int)
    fun setMenuItemClickListener(onMenuItemClicked: () -> Unit)
}