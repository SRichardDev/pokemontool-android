package io.stanc.pogotool.appbar

import android.content.Context
import android.content.res.ColorStateList
import android.support.annotation.StringRes
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v4.widget.ImageViewCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import io.stanc.pogotool.R

class PoGoToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ConstraintLayout(context, attrs, defStyleAttr), Toolbar {

    private val toolbarLayout: ViewGroup

    private val navigationIcon: ImageView
    private val title: TextView
    private val menuIcon: ImageView

    init {
        View.inflate(context, R.layout.layout_toolbar, this)
        toolbarLayout = findViewById(R.id.toolbar_layout)
        navigationIcon = findViewById(R.id.toolbar_icon_navigation)
        title = findViewById(R.id.toolbar_title)
        menuIcon = findViewById(R.id.toolbar_icon_menu)
    }

    /**
     * Toolbar
     */

    override fun setVisibility(visible: Boolean) {
        this.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /**
     * Navigation Icon
     */

    override fun showNavigationIcon() {
        navigationIcon.visibility = View.VISIBLE
    }

    override fun hideNavigationIcon() {
        navigationIcon.visibility = View.GONE
    }

    override fun setNavigationIcon(navigationIconResID: Int) {
        navigationIcon.setImageResource(navigationIconResID)
    }

    override fun setNavigationIconClickListener(onNavigationIconClicked: () -> Unit) {
        navigationIcon.setOnClickListener { onNavigationIconClicked() }
    }

    /**
     * Title
     */

    override fun setTitle(text: String) {
        title.text = text
    }

    override fun setTitle(@StringRes stringResID: Int) {
        (resources.getText(stringResID) as String?)?.let { setTitle(it) }
    }

    override fun setTitleLongClickListener(onTitleLongClicked: () -> Unit) {
        title.setOnLongClickListener { onTitleLongClicked(); true }
    }

    /**
     * Menu
     */

    override fun showMenu() {
        menuIcon.visibility = View.VISIBLE
    }

    override fun hideMenu() {
        menuIcon.visibility = View.GONE
    }

    override fun setMenuItemIcon(menuIconResID: Int) {
        menuIcon.setImageResource(menuIconResID)
    }

    override fun setMenuItemClickListener(onMenuItemClicked: () -> Unit) {
        menuIcon.setOnClickListener { onMenuItemClicked() }
    }
}