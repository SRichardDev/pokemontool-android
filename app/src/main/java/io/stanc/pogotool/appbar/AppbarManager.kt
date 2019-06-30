package io.stanc.pogotool.appbar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import java.lang.ref.WeakReference


class AppbarManager {

    companion object {
        private val TAG = javaClass.name

        internal var toolbar: WeakReference<Toolbar>? = null
             private set
        internal val EMPTY_LAMBDA: () -> Unit = {}
        internal var defaultOnNavigationIconClicked: () -> Unit = EMPTY_LAMBDA
            private set

        fun setup(toolbar: Toolbar, defaultOnNavigationIconClicked: () -> Unit) {
            toolbar.setNavigationIconClickListener { defaultOnNavigationIconClicked() }

            Companion.toolbar = WeakReference(toolbar)
            Companion.defaultOnNavigationIconClicked = defaultOnNavigationIconClicked
        }


        fun showAppbar() {
            toolbar?.get()?.setVisibility(true)
        }

        fun hideAppbar() {
            toolbar?.get()?.setVisibility(false)
        }

        /**
         * Navigation Icon
         */

        fun setNavigationIcon(@DrawableRes navigationIconResID: Int) {
            setNavigationIcon(navigationIconResID, onNavigationIconClicked = defaultOnNavigationIconClicked)
        }

        fun setNavigationIcon(@DrawableRes navigationIconResID: Int, onNavigationIconClicked: () -> Unit) {
            toolbar?.get()?.setNavigationIcon(navigationIconResID)
            toolbar?.get()?.setNavigationIconClickListener { onNavigationIconClicked() }
        }

        /**
         * Title
         */

        fun setTitle(title: String?, onTitleLongClicked: () -> Unit = EMPTY_LAMBDA) {

            title?.let { toolbar?.get()?.setTitle(it) }

            if (onTitleLongClicked != EMPTY_LAMBDA) {
                toolbar?.get()?.setTitleLongClickListener { onTitleLongClicked() }
            }
        }

        /**
         * Menu Icon
         */

        fun resetMenu() {
            toolbar?.get()?.hideMenu(Toolbar.MenuType.Button)
            toolbar?.get()?.hideMenu(Toolbar.MenuType.Icon)
        }

        fun setMenuIcon(@DrawableRes menuIconResID: Int, onMenuIconClicked: () -> Unit) {
            toolbar?.get()?.setMenuItemIcon(menuIconResID, onMenuIconClicked)
            toolbar?.get()?.showMenu(Toolbar.MenuType.Icon)
            toolbar?.get()?.hideMenu(Toolbar.MenuType.Button)
        }

        fun setMenuButton(@StringRes menuButtonStringResID: Int, onMenuIconClicked: () -> Unit) {
            toolbar?.get()?.setMenuItemButton(menuButtonStringResID, onMenuIconClicked)
            toolbar?.get()?.showMenu(Toolbar.MenuType.Button)
            toolbar?.get()?.hideMenu(Toolbar.MenuType.Icon)
        }
    }
}