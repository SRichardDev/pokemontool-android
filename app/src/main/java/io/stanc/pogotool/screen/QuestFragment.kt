package io.stanc.pogotool.screen

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SearchView
import androidx.appcompat.app.AlertDialog
import androidx.databinding.adapters.SearchViewBindingAdapter
import io.stanc.pogotool.App
import io.stanc.pogotool.FirebaseImageMapper
import io.stanc.pogotool.R
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.FirebaseDefinitions
import io.stanc.pogotool.firebase.FirebaseUser
import io.stanc.pogotool.firebase.node.FirebasePokestop
import io.stanc.pogotool.firebase.node.FirebaseQuest
import io.stanc.pogotool.firebase.node.FirebaseQuestDefinition
import io.stanc.pogotool.recyclerview.RecyclerViewAdapter
import io.stanc.pogotool.recyclerview.RecyclerViewFragment
import io.stanc.pogotool.recyclerviewadapter.QuestAdapter


class QuestFragment: RecyclerViewFragment<FirebaseQuestDefinition>() {
    private val TAG = javaClass.name

    private var pokestop: FirebasePokestop? = null
    private val firebase = FirebaseDatabase()
    private var adapter: QuestAdapter? = null

    override val fragmentLayoutRes: Int
        get() = R.layout.fragment_quest

    override val recyclerViewIdRes: Int
        get() = R.id.quest_recyclerview

    override val orientation: Orientation
        get() = Orientation.VERTICAL

    override val initItemList: List<FirebaseQuestDefinition>
        get() = FirebaseDefinitions.quests

    override fun onCreateListAdapter(context: Context, list: List<FirebaseQuestDefinition>): QuestAdapter {
        val adapter = QuestAdapter(context, list)

        adapter.onItemClickListener = object : RecyclerViewAdapter.OnItemClickListener {
            override fun onClick(id: Any) {
                // TODO: PopupFragment/AlertFragment
                showPopup(list, id)
            }
        }

        this.adapter = adapter
        return adapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupQueryTextView(view)
    }

    override fun onResume() {
        super.onResume()
        pokestop?.let { AppbarManager.setTitle(it.name) }
    }

    override fun onPause() {
        AppbarManager.setTitle(getString(R.string.default_app_title))
        super.onPause()
    }

    private fun setupQueryTextView(rootLayout: View) {

        rootLayout.findViewById<SearchView>(R.id.quest_searchview)?.setOnQueryTextListener(object: SearchViewBindingAdapter.OnQueryTextChange, SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(p0: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                adapter?.filter(newText)
                return false
            }

        }) ?: kotlin.run { Log.e(TAG, "no quest_searchview found in view: $view") }
    }

    @Throws(Exception::class)
    private fun tryToSendNewQuest(questDefinition: FirebaseQuestDefinition) {

        pokestop?.let { pokestop ->

            FirebaseUser.userData?.id?.let { userId ->

                sendNewQuest(pokestop, questDefinition, userId)

            } ?: kotlin.run {
                throw Exception("could not send quest because user is not logged in. FirebaseUser.userData?: ${FirebaseUser.userData}")
            }

        } ?: kotlin.run {
            throw Exception("could not send quest, because pokestop: $pokestop!")
        }
    }

    private fun sendNewQuest(pokestop: FirebasePokestop, questDefinition: FirebaseQuestDefinition, userId: String) {
        val quest = FirebaseQuest.new(pokestop.id, pokestop.geoHash, questDefinition.id, userId)
        firebase.pushQuest(quest)
    }

    private fun closeScreen() {
        fragmentManager?.findFragmentByTag(this::class.java.name)?.let {
            fragmentManager?.beginTransaction()?.remove(it)?.commit()
        }
    }

    private fun showPopup(list: List<FirebaseQuestDefinition>, itemId: Any) {

        list.find { it.id == itemId }?.let { questDefinition ->

            context?.let { context ->

                val alertDialogBuilder = AlertDialog.Builder(context)
                    .setTitle(App.geString(R.string.pokestop_quest_popup_title))
                    .setMessage(questDefinition.questDescription)
                    .setPositiveButton(R.string.pokestop_quest_popup_button_send) { dialog, which ->

                        try {
                            tryToSendNewQuest(questDefinition)
                            closeScreen()

                        } catch (e: Exception) {
                            Log.e(TAG, e.message)
                        }
                    }
                    .setNegativeButton(R.string.pokestop_quest_popup_button_cancel, null)

                FirebaseImageMapper.questDrawable(context, questDefinition.imageName)?.let {
                    alertDialogBuilder.setIcon(it)
                }

                alertDialogBuilder.show()
            }

        } ?: kotlin.run {
            Log.e(TAG, "could not find quest definition with id: $id in list: $list!")
        }
    }

    companion object {

        fun newInstance(pokestop: FirebasePokestop): QuestFragment {
            val fragment = QuestFragment()
            fragment.pokestop = pokestop
            return fragment
        }
    }
}