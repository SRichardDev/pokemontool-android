package io.stanc.pogoradar.appbar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import java.lang.ref.WeakReference


class AppbarManager {

    companion object {
        private val TAG = javaClass.name

        private var toolbar: WeakReference<Toolbar>? = null
        private val EMPTY_LAMBDA: () -> Unit = {}
        private var defaultOnNavigationIconClicked: () -> Unit = EMPTY_LAMBDA
        private var defaultTitle: String? = null


        fun setup(toolbar: Toolbar, defaultTitle: String, defaultOnNavigationIconClicked: () -> Unit) {
            toolbar.setNavigationIconClickListener { defaultOnNavigationIconClicked() }

            Companion.toolbar = WeakReference(toolbar)
            Companion.defaultTitle = defaultTitle
            Companion.defaultOnNavigationIconClicked = defaultOnNavigationIconClicked

            setTitle(defaultTitle)
            hideNavigationIcon()
        }


        fun showAppbar() {
            toolbar?.get()?.setVisibility(true)
        }

        fun hideAppbar() {
            toolbar?.get()?.setVisibility(false)
        }

        fun reset() {
            defaultTitle?.let { setTitle(it) }
            hideNavigationIcon()
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

        fun showNavigationIcon() {
            toolbar?.get()?.showNavigationIcon()
        }

        fun hideNavigationIcon() {
            toolbar?.get()?.hideNavigationIcon()
        }


        /**
         * Title
         */

        fun setTitle(title: String?, onTitleLongClicked: () -> Unit = EMPTY_LAMBDA) {

            title?.let { toolbar?.get()?.setTitle(it) }

            if (onTitleLongClicked != EMPTY_LAMBDA) {
                toolbar?.get()?.setTitleLongClickListener { onTitleLongClicked() }
            }
            showNavigationIcon()
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