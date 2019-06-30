package io.stanc.pogotool.appbar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

interface Toolbar {

    enum class MenuType {
        Icon,
        Button
    }

    fun setVisibility(visible: Boolean)

    fun setNavigationIcon(@DrawableRes navigationIconResID: Int)
    fun setNavigationIconClickListener(onNavigationIconClicked: () -> Unit)
    fun showNavigationIcon()
    fun hideNavigationIcon()

    fun setTitle(text: String)
    fun setTitle(@StringRes stringResID: Int)
    fun setTitleLongClickListener(onTitleLongClicked: () -> Unit)

    fun showMenu(type: MenuType)
    fun hideMenu(type: MenuType)

    fun setMenuItemIcon(@DrawableRes menuIconResID: Int, onMenuItemClicked: () -> Unit)
    fun setMenuItemButton(@StringRes stringResID: Int, onMenuItemClicked: () -> Unit)
}