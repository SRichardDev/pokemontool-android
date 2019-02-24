package io.stanc.pogotool

import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import io.stanc.pogotool.firebase.FirebaseServer
import io.stanc.pogotool.firebase.data.FirebaseUserLocal
import io.stanc.pogotool.utils.KotlinUtils
import io.stanc.pogotool.utils.SystemUtils
import io.stanc.pogotool.utils.WaitingSpinner
import kotlinx.android.synthetic.main.layout_fragment_authentication.*

class AuthenticationFragment: Fragment(), View.OnClickListener {

    private var rootView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.layout_fragment_authentication, container, false) as ViewGroup
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authentication_imageButton_close.setOnClickListener {
            activity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit()
        }

        authentication_button_sign_in.setOnClickListener(this)
        authentication_button_sign_up.setOnClickListener(this)
        authentication_button_sign_out.setOnClickListener(this)
        authentication_button_verify_email.setOnClickListener(this)
        authentication_button_user_name.setOnClickListener(this)

        updateUI(FirebaseServer.authState())
    }

    override fun onResume() {
        super.onResume()
        FirebaseServer.addAuthStateObserver(authStateObserver)
        FirebaseServer.addUserProfileObserver(userProfileObserver)
        context?.let { FirebaseServer.reloadUserData(it, onCompletedReloadingRequest) }
    }

    override fun onPause() {
        FirebaseServer.removeAuthStateObserver(authStateObserver)
        FirebaseServer.removeUserProfileObserver(userProfileObserver)
        super.onPause()
    }

    override fun onClick(v: View) {
        when (v.id) {

            R.id.authentication_button_sign_up -> {
                if (checkEditTextsInputsIsValid()) {
                    FirebaseServer.signUp(
                        authentication_edittext_email.text.toString(),
                        authentication_edittext_password.text.toString(),
                        onCompletedSignedUpRequest
                    )
                }
            }

            R.id.authentication_button_sign_in -> {
                if (checkEditTextsInputsIsValid()) {
                    FirebaseServer.signIn(
                        authentication_edittext_email.text.toString(),
                        authentication_edittext_password.text.toString(),
                        onCompletedSignedInRequest
                    )
                }
            }

            R.id.authentication_button_sign_out -> FirebaseServer.signOut()

            R.id.authentication_button_verify_email -> {
                authentication_button_verify_email?.isEnabled = false
                FirebaseServer.sendEmailVerification(onCompletedVerificationRequest)
            }

            R.id.authentication_button_user_name -> {

                if (authentication_edittext_user_name.visibility == View.VISIBLE) {

                    FirebaseServer.changeUserName(authentication_edittext_user_name.text.toString())

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

    /**
     * firebase observer
     */

    private val authStateObserver = object: FirebaseServer.AuthStateObserver {
        override fun authStateChanged(newAuthState: FirebaseServer.AuthState) {
            authentication_button_verify_email?.isEnabled = true
            updateUI(newAuthState)
        }
    }

    private val userProfileObserver = object: FirebaseServer.UserProfileObserver {
        override fun userProfileChanged(user: FirebaseUserLocal) {
            updateAuthenticationStateText()
        }
    }

//    TODO: move onCompleted<XY>Request to FirebaseServer as default callbacks
    private val onCompletedSignedUpRequest = { taskSuccessful: Boolean ->
        if (!taskSuccessful) {
            Toast.makeText(context, getString(R.string.authentication_state_authentication_failed), Toast.LENGTH_LONG).show()
        }
    }
    private val onCompletedSignedInRequest = { taskSuccessful: Boolean ->
        if (!taskSuccessful) {
            Toast.makeText(context, getString(R.string.authentication_state_authentication_failed), Toast.LENGTH_LONG).show()
        }
    }
    private val onCompletedReloadingRequest = { taskSuccessful: Boolean ->
        if (!taskSuccessful) {
            Toast.makeText(context, getString(R.string.authentication_state_synchronization_failed), Toast.LENGTH_LONG).show()
        }
    }
    private val onCompletedVerificationRequest = { taskSuccessful: Boolean ->
        if (taskSuccessful) {
            Toast.makeText(context, getString(R.string.authentication_state_verification_successful, FirebaseServer.user().email), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, getString(R.string.authentication_state_verification_failed), Toast.LENGTH_LONG).show()
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

    private fun updateUI(newAuthState: FirebaseServer.AuthState) {

        WaitingSpinner.hideProgress()

        when (newAuthState) {
            FirebaseServer.AuthState.UserLoggedOut -> {
                updateSignButtons(userHasToSignInOrUp = true)
            }
            FirebaseServer.AuthState.UserLoggedInButUnverified -> {
                updateSignButtons(userHasToSignInOrUp = false, isEmailVerified = false)
            }
            FirebaseServer.AuthState.UserLoggedIn -> {
                updateSignButtons(userHasToSignInOrUp = false, isEmailVerified = true)
            }
        }

        updateAuthenticationStateText()
    }

    private fun updateAuthenticationStateText() {
        context?.let { authentication_textview_status?.text =
            FirebaseServer.usersAuthenticationStateText(it)
        }
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