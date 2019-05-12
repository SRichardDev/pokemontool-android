package io.stanc.pogotool

import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.firebase.FirebaseUser
import io.stanc.pogotool.firebase.node.FirebaseUserNode
import io.stanc.pogotool.utils.KotlinUtils
import io.stanc.pogotool.utils.SystemUtils
import io.stanc.pogotool.utils.WaitingSpinner
import kotlinx.android.synthetic.main.fragment_authentication.*

class AccountFragment: Fragment(), View.OnClickListener {

    private var rootView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_authentication, container, false) as ViewGroup

        rootView?.setOnClickListener { activity?.let { SystemUtils.hideKeyboard(it) } }

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authentication_button_sign_in.setOnClickListener(this)
        authentication_button_sign_up.setOnClickListener(this)
        authentication_button_sign_out.setOnClickListener(this)
        authentication_button_verify_email.setOnClickListener(this)
        authentication_button_user_name.setOnClickListener(this)

        updateUI()
    }

    override fun onResume() {
        super.onResume()
        AppbarManager.setTitle(getString(R.string.authentication_app_title))
        FirebaseUser.addAuthStateObserver(authStateObserver)
        FirebaseUser.addUserDataObserver(userDataObserver)
    }

    override fun onPause() {
        FirebaseUser.removeAuthStateObserver(authStateObserver)
        FirebaseUser.removeUserDataObserver(userDataObserver)
        super.onPause()
    }

    override fun onClick(v: View) {
        when (v.id) {

            R.id.authentication_button_sign_up -> {
                if (checkEditTextsInputsIsValid()) {

                    FirebaseUser.signUp(
                        userConfigForAuthentication(),
                        onCompletedSignedUpRequest
                    )
                }
            }

            R.id.authentication_button_sign_in -> {
                if (checkEditTextsInputsIsValid()) {
                    FirebaseUser.signIn(
                        userConfigForAuthentication(),
                        onCompletedSignedInRequest
                    )
                }
            }

            R.id.authentication_button_sign_out -> FirebaseUser.signOut()

            R.id.authentication_button_verify_email -> {
                FirebaseUser.sendEmailVerification(onCompletedVerificationRequest)
            }

            R.id.authentication_button_user_name -> {

                if (authentication_edittext_user_name.visibility == View.VISIBLE) {

                    FirebaseUser.changeUserName(authentication_edittext_user_name.text.toString())

                    authentication_edittext_user_name.visibility = View.GONE
                    authentication_button_user_name.text = getString(R.string.authentication_button_open_user_name)
                    KotlinUtils.safeLet(context, rootView) { _context, view -> SystemUtils.hideKeyboardFrom(_context, view) }

                } else {
                    authentication_edittext_user_name.visibility = View.VISIBLE
                    authentication_button_user_name.text = getString(R.string.authentication_button_send_user_name)
                    context?.let { SystemUtils.showKeyboard(it) }
                }
            }
        }
    }

    private fun userConfigForAuthentication(): FirebaseUser.UserConfig {

        val email = authentication_edittext_email.text.toString()
        val password = authentication_edittext_password.text.toString()
//        TODO: ....
//        val name = ?
//        val team = ?

        return FirebaseUser.UserConfig(email, password)
    }

    /**
     * firebase observer
     */

    private val authStateObserver = object: FirebaseUser.AuthStateObserver {
        override fun authStateChanged(newAuthState: FirebaseUser.AuthState) {
            updateUI()
        }
    }

    private val userDataObserver = object: FirebaseUser.UserDataObserver {
        override fun userDataChanged(user: FirebaseUserNode?) {
            updateUI()
        }
    }

//    TODO: move onCompleted<XY>Request to FirebaseServer as default callbacks
    private val onCompletedSignedUpRequest = { taskSuccessful: Boolean, exception: String? ->
        if (!taskSuccessful) {
            Toast.makeText(context, getString(R.string.authentication_state_authentication_failed, exception), Toast.LENGTH_LONG).show()
        }
    }
    private val onCompletedSignedInRequest = { taskSuccessful: Boolean, exception: String? ->
        if (!taskSuccessful) {
            Toast.makeText(context, getString(R.string.authentication_state_authentication_failed, exception), Toast.LENGTH_LONG).show()
        }
    }
    private val onCompletedReloadingRequest = { taskSuccessful: Boolean ->
        if (!taskSuccessful) {
            Toast.makeText(context, getString(R.string.authentication_state_synchronization_failed), Toast.LENGTH_LONG).show()
        }
    }
    private val onCompletedVerificationRequest = { taskSuccessful: Boolean, exception: String? ->
        if (taskSuccessful) {
            Toast.makeText(context, getString(R.string.authentication_state_verification_successful, FirebaseUser.userData?.email), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, getString(R.string.authentication_state_verification_failed, exception), Toast.LENGTH_LONG).show()
        }
    }


    /**
     * ui methods
     */

    private fun checkEditTextsInputsIsValid(): Boolean {
        
        var valid = true

        val email = authentication_edittext_email?.text.toString()
        if (TextUtils.isEmpty(email)) {
            authentication_edittext_email?.error = getString(R.string.authentication_edittext_email_error)
            valid = true
        } else {
            authentication_edittext_email?.error = null
        }

        val password = authentication_edittext_password?.text.toString()
        if (TextUtils.isEmpty(password)) {
            authentication_edittext_password?.error = getString(R.string.authentication_edittext_password_error)
            valid = true
        } else {
            authentication_edittext_password?.error = null
        }
        
        return valid
    }

    private fun updateUI() {

        WaitingSpinner.hideProgress()

        when (FirebaseUser.authState()) {
            FirebaseUser.AuthState.UserLoggedOut -> {
                updateSignButtons(userHasToSignInOrUp = true)
            }
            FirebaseUser.AuthState.UserLoggedInButUnverified -> {
                updateSignButtons(userHasToSignInOrUp = false, isEmailVerified = false)
            }
            FirebaseUser.AuthState.UserLoggedIn -> {
                updateSignButtons(userHasToSignInOrUp = false, isEmailVerified = true)
            }
        }

        updateUserStateTexts()
    }

    private fun updateUserStateTexts() {
        Log.d(TAG, "Debug:: updateUserStateTexts(), authentication_layout_user_name: $authentication_layout_user_name, authentication_layout_user_email: $authentication_layout_user_email")

        FirebaseUser.userData?.photoURL?.let { authentication_imageview_user?.setImageURI(it) }

        context?.let {
            authentication_layout_user_status?.text = getString(R.string.authentication_user_state, FirebaseUser.authStateText(it))
        }

        authentication_layout_user_name?.text = getString(R.string.authentication_user_name, FirebaseUser.userData?.trainerName)
        authentication_layout_user_email?.text = getString(R.string.authentication_user_email, FirebaseUser.userData?.email)
    }

    private fun updateSignButtons(userHasToSignInOrUp: Boolean, isEmailVerified: Boolean = false) {

        authentication_button_sign_in?.visibility = if (userHasToSignInOrUp) View.VISIBLE else View.GONE
        authentication_button_sign_up?.visibility = if (userHasToSignInOrUp) View.VISIBLE else View.GONE

        authentication_edittext_email?.visibility = if (userHasToSignInOrUp) View.VISIBLE else View.GONE
        authentication_edittext_password?.visibility = if (userHasToSignInOrUp) View.VISIBLE else View.GONE

        authentication_button_sign_out?.visibility = if (userHasToSignInOrUp) View.GONE else View.VISIBLE
        authentication_button_verify_email?.visibility = if (!userHasToSignInOrUp && !isEmailVerified) View.VISIBLE else View.GONE

        authentication_button_user_name?.visibility = if (!userHasToSignInOrUp && isEmailVerified) View.VISIBLE else View.GONE
    }

    companion object {

        private val TAG = this::class.java.name
    }
}