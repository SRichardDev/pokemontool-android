package io.stanc.pogoradar.appbar

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.stanc.pogoradar.R

class PoGoToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ConstraintLayout(context, attrs, defStyleAttr), Toolbar {

    private val toolbarLayout: ViewGroup

    private val navigationIcon: ImageView
    private val title: TextView
    private val menuIcon: ImageView
    private val menuButton: TextView

    init {
        View.inflate(context, R.layout.layout_toolbar, this)
        toolbarLayout = findViewById(R.id.toolbar_layout)
        navigationIcon = findViewById(R.id.toolbar_icon_navigation)
        title = findViewById(R.id.toolbar_title)
        menuIcon = findViewById(R.id.toolbar_icon_menu)
        menuButton = findViewById(R.id.toolbar_button_menu)
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
        navigationIcon.visibility = View.INVISIBLE
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

    override fun showMenu(type: Toolbar.MenuType) {
        when(type) {
            Toolbar.MenuType.Icon -> menuIcon.visibility = View.VISIBLE
            Toolbar.MenuType.Button -> menuButton.visibility = View.VISIBLE
        }
    }

    override fun hideMenu(type: Toolbar.MenuType) {
        when(type) {
            Toolbar.MenuType.Icon -> menuIcon.visibility = View.INVISIBLE
            Toolbar.MenuType.Button -> menuButton.visibility = View.INVISIBLE
        }

    }

    override fun setMenuItemIcon(menuIconResID: Int, onMenuItemClicked: () -> Unit) {
        menuIcon.setImageResource(menuIconResID)
        menuIcon.setOnClickListener { onMenuItemClicked() }
    }

    override fun setMenuItemButton(stringResID: Int, onMenuItemClicked: () -> Unit) {
        (resources.getText(stringResID) as String?)?.let { menuButton.text = it }
        menuButton.setOnClickListener { onMenuItemClicked() }
    }
}