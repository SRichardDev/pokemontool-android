package io.stanc.pogotool.firebase

import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import io.stanc.pogotool.R
import io.stanc.pogotool.WaitingSpinner
import kotlinx.android.synthetic.main.layout_fragment_authentication.*

class AuthenticationFragment: Fragment(), View.OnClickListener {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_fragment_authentication, container, false) as ViewGroup
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
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        context?.let { FirebaseServer.updateUserData(it, onCompletedReloading) }
    }

    override fun onClick(v: View) {
        when (v.id) {

            R.id.authentication_button_sign_up -> {
                if (checkEditTextsInputsIsValid()) {
                    FirebaseServer.signUp(authentication_edittext_email.text.toString(), authentication_edittext_password.text.toString(), onCompletedSignedUp)
                }
            }

            R.id.authentication_button_sign_in -> {
                if (checkEditTextsInputsIsValid()) {
                    FirebaseServer.signIn(authentication_edittext_email.text.toString(), authentication_edittext_password.text.toString(), onCompletedSignedIn)
                }
            }

            R.id.authentication_button_sign_out -> {
                FirebaseServer.signOut()
                updateUI()
            }

            R.id.authentication_button_verify_email -> {
                authentication_button_verify_email?.isEnabled = false
                FirebaseServer.sendEmailVerification(onCompletedVerificationRequest)
            }
        }
    }

    /**
     * authentication completed callbacks
     */

    private val onCompletedSignedUp = { taskSuccessful: Boolean ->
        if (taskSuccessful) {
            updateUI()
        } else {
            Toast.makeText(context, getString(R.string.authentication_state_authentication_failed), Toast.LENGTH_LONG).show()
        }
    }

    private val onCompletedSignedIn = { taskSuccessful: Boolean ->
        if (taskSuccessful) {
            updateUI()
        } else {
            Toast.makeText(context, getString(R.string.authentication_state_authentication_failed), Toast.LENGTH_LONG).show()
        }
    }

    private val onCompletedReloading = { taskSuccessful: Boolean ->
        if (taskSuccessful) {
            updateUI()
        } else {
            Toast.makeText(context, getString(R.string.authentication_state_synchronization_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private val onCompletedVerificationRequest = { taskSuccessful: Boolean ->
        if (taskSuccessful) {
            Toast.makeText(context, getString(R.string.authentication_state_verification_successful, FirebaseServer.user()?.email), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, getString(R.string.authentication_state_verification_failed), Toast.LENGTH_LONG).show()
        }

        authentication_button_verify_email?.isEnabled = true
    }


    /**
     * authentication methods
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

        FirebaseServer.user()?.let { user ->
            updateSignButtons(userHasToSignInOrUp = false, isEmailVerified = user.isEmailVerified)

        } ?: kotlin.run {
            updateSignButtons(userHasToSignInOrUp = true)
        }

        updateAuthenticationStateText()
    }

    private fun updateAuthenticationStateText() {
        context?.let { authentication_textview_status?.text = FirebaseServer.usersAuthenticationStateText(it) }
    }

    private fun updateSignButtons(userHasToSignInOrUp: Boolean, isEmailVerified: Boolean = false) {

        authentication_button_sign_in?.visibility = if (userHasToSignInOrUp) View.VISIBLE else View.GONE
        authentication_button_sign_up?.visibility = if (userHasToSignInOrUp) View.VISIBLE else View.GONE

        authentication_edittext_email?.visibility = if (userHasToSignInOrUp) View.VISIBLE else View.GONE
        authentication_edittext_password?.visibility = if (userHasToSignInOrUp) View.VISIBLE else View.GONE

        authentication_button_sign_out?.visibility = if (userHasToSignInOrUp) View.GONE else View.VISIBLE
        authentication_button_verify_email?.visibility = if (!userHasToSignInOrUp && !isEmailVerified) View.VISIBLE else View.GONE
    }
}