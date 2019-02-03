package io.stanc.pogotool.firebase

import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import io.stanc.pogotool.NavDrawerDelegate
import io.stanc.pogotool.R
import io.stanc.pogotool.WaitingSpinner
import kotlinx.android.synthetic.main.layout_fragment_authentication.*

class AuthenticationFragment: Fragment(), View.OnClickListener {

    private var auth: FirebaseAuth? = null
    private var delegate: NavDrawerDelegate? = null

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

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
    }

    override fun onResume() {
        super.onResume()

        updateUI()

        auth?.currentUser?.reload()?.addOnCompleteListener { task ->

            Log.i(this.javaClass.name, "Debug:: reload.onCompleteListener, isSuccessful: ${task.isSuccessful}")
            if (task.isSuccessful) {
                updateUI()
            } else {
                Toast.makeText(context, getString(R.string.authentication_state_synchronization_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.authentication_button_sign_up -> createAccount(authentication_edittext_email.text.toString(), authentication_edittext_password.text.toString())
            R.id.authentication_button_sign_in -> signIn(authentication_edittext_email.text.toString(), authentication_edittext_password.text.toString())
            R.id.authentication_button_sign_out -> signOut()
            R.id.authentication_button_verify_email -> sendEmailVerification()
        }
    }

    /**
     * authentication methods
     */

    private fun createAccount(email: String, password: String) {
        Log.d(this.javaClass.name, "createAccount:$email")

        if (!validateEditTextsInput()) {
            return
        }

        WaitingSpinner.showProgress()

        auth?.createUserWithEmailAndPassword(email, password)?.addOnCompleteListener {task ->

            if (task.isSuccessful) {
                updateUI()
            } else {
                Toast.makeText(context, getString(R.string.authentication_state_authentication_failed), Toast.LENGTH_LONG).show()
            }

            WaitingSpinner.hideProgress()
        }
    }

    private fun signIn(email: String, password: String) {

        Log.d(this.javaClass.name, "signIn:$email")
        if (!validateEditTextsInput()) {
            return
        }

        WaitingSpinner.showProgress()

        auth?.signInWithEmailAndPassword(email, password)?.addOnCompleteListener { task ->

            if (task.isSuccessful) {
                updateUI()

            } else {
                Toast.makeText(context, "Authentication failed.", Toast.LENGTH_LONG).show()
            }

            WaitingSpinner.hideProgress()
        }
    }

    private fun signOut() {
        auth?.signOut()
        updateUI()
    }

    private fun sendEmailVerification() {

        // Send verification email
        auth?.currentUser?.let { user ->

            Log.d(this.javaClass.name, "Debug:: sendEmailVerification() for user: ${user.email}")
            authentication_button_verify_email?.isEnabled = false
            WaitingSpinner.showProgress()

            user.sendEmailVerification().addOnCompleteListener { task ->

                authentication_button_verify_email?.isEnabled = true

                WaitingSpinner.hideProgress()

                Log.d(this.javaClass.name, "Debug:: sendEmailVerification() onCompleteListener, task.isSuccessful: ${task.isSuccessful}")

                if (task.isSuccessful) {
                    Toast.makeText(context, getString(R.string.authentication_state_verification_successful, user.email), Toast.LENGTH_LONG).show()
                } else {
                    Log.e(this.javaClass.name, "sendEmailVerification", task.exception)
                    Toast.makeText(context, getString(R.string.authentication_state_verification_failed), Toast.LENGTH_LONG).show()
                }
            }
        } ?: kotlin.run { Log.w(this.javaClass.name, "can not send email verification because current user is null!") }
    }

    private fun validateEditTextsInput(): Boolean {

        var valid = true

        val email = authentication_edittext_email?.text.toString()
        if (TextUtils.isEmpty(email)) {
            authentication_edittext_email?.error = getString(R.string.authentication_edittext_email_error)
            valid = false
        } else {
            authentication_edittext_email?.error = null
        }

        val password = authentication_edittext_password?.text.toString()
        if (TextUtils.isEmpty(password)) {
            authentication_edittext_password?.error = getString(R.string.authentication_edittext_password_error)
            valid = false
        } else {
            authentication_edittext_password?.error = null
        }

        return valid
    }

    private fun updateUI() {

        WaitingSpinner.hideProgress()

        auth?.currentUser?.let { user ->

            updateSignButtons(userHasToSignInOrUp = false, isEmailVerified = user.isEmailVerified)

        } ?: kotlin.run {
            updateSignButtons(userHasToSignInOrUp = true)
        }

        updateAuthenticationStateText()
    }

    private fun updateAuthenticationStateText() {

        val statusText = auth?.currentUser?.let { user ->

            if (user.isEmailVerified) {
                getString(R.string.authentication_state_signed_in, user.email)
            } else {
                getString(R.string.authentication_state_signed_in_but_missing_verification, user.email)
            }

        } ?: kotlin.run {
            getString(R.string.authentication_state_signed_out)
        }

        authentication_textview_status?.text = statusText
        delegate?.changeSubTitle(statusText)
    }


    private fun updateSignButtons(userHasToSignInOrUp: Boolean, isEmailVerified: Boolean = false) {

        authentication_button_sign_in?.visibility = if (userHasToSignInOrUp) View.VISIBLE else View.GONE
        authentication_button_sign_up?.visibility = if (userHasToSignInOrUp) View.VISIBLE else View.GONE

        authentication_edittext_email?.visibility = if (userHasToSignInOrUp) View.VISIBLE else View.GONE
        authentication_edittext_password?.visibility = if (userHasToSignInOrUp) View.VISIBLE else View.GONE

        authentication_button_sign_out?.visibility = if (userHasToSignInOrUp) View.GONE else View.VISIBLE
        authentication_button_verify_email?.visibility = if (!userHasToSignInOrUp && !isEmailVerified) View.VISIBLE else View.GONE
    }

    companion object {

        fun newInstance(delegate: NavDrawerDelegate): AuthenticationFragment {
            val fragment = AuthenticationFragment()
            fragment.delegate = delegate
            return fragment
        }
    }
}