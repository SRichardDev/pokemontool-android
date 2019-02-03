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

        if (!validateForm()) {
            return
        }

        WaitingSpinner.showProgress()

        // [START create_user_with_email]
        auth?.createUserWithEmailAndPassword(email, password)?.addOnCompleteListener {task ->
            if (task.isSuccessful) {
                // Sign in success, update UI with the signed-in user's information
                Log.d(this.javaClass.name, "createUserWithEmail:success")
                updateUI()
            } else {
                // If sign in fails, display a message to the user.
                Log.w(this.javaClass.name, "createUserWithEmail:failure", task.exception)
                Toast.makeText(
                    context, "Authentication failed.",
                    Toast.LENGTH_SHORT
                ).show()
                updateUI()
            }

            // [START_EXCLUDE]
            WaitingSpinner.hideProgress()
            // [END_EXCLUDE]
        }
        // [END create_user_with_email]
    }

    private fun signIn(email: String, password: String) {
        Log.d(this.javaClass.name, "signIn:$email")
        if (!validateForm()) {
            return
        }

        WaitingSpinner.showProgress()

        // [START sign_in_with_email]
        auth?.signInWithEmailAndPassword(email, password)?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(this.javaClass.name, "signInWithEmail:success")
                    updateUI()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(this.javaClass.name, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        context, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUI()
                }

                // [START_EXCLUDE]
                if (!task.isSuccessful) {
                    authentication_textview_status.setText(R.string.authentication_state_failed)
                }
            WaitingSpinner.hideProgress()
                // [END_EXCLUDE]
            }
        // [END sign_in_with_email]
    }

    private fun signOut() {
        auth?.signOut()
        updateUI()
    }

    private fun sendEmailVerification() {
        // Disable button
        authentication_button_verify_email.isEnabled = false
        Log.d(this.javaClass.name, "Debug:: sendEmailVerification()")

        WaitingSpinner.showProgress()

        // Send verification email
        // [START send_email_verification]
        val user = auth?.currentUser
        Log.d(this.javaClass.name, "Debug:: sendEmailVerification() for user: ${user?.email}")
        user?.sendEmailVerification()?.addOnCompleteListener { task ->
                // [START_EXCLUDE]
                // Re-enable button
            authentication_button_verify_email.isEnabled = true

            WaitingSpinner.hideProgress()

            Log.d(this.javaClass.name, "Debug:: sendEmailVerification() onCompleteListener, task.isSuccessful: ${task.isSuccessful}")

                if (task.isSuccessful) {
                    Toast.makeText(context, "Verification email sent to ${user.email} ", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(this.javaClass.name, "sendEmailVerification", task.exception)
                    Toast.makeText(context, "Failed to send verification email.", Toast.LENGTH_SHORT).show()
                }
                // [END_EXCLUDE]
            }
        // [END send_email_verification]
    }

    private fun validateForm(): Boolean {
        var valid = true

        val email = authentication_edittext_email.text.toString()
        if (TextUtils.isEmpty(email)) {
            authentication_edittext_email.error = "Required."
            valid = false
        } else {
            authentication_edittext_email.error = null
        }

        val password = authentication_edittext_password.text.toString()
        if (TextUtils.isEmpty(password)) {
            authentication_edittext_password.error = "Required."
            valid = false
        } else {
            authentication_edittext_password.error = null
        }

        return valid
    }

    private fun updateUI() {

        WaitingSpinner.hideProgress()

        auth?.currentUser?.let { user ->

            updateSignButtons(userHasToSignInOrUp = false)
            authentication_button_verify_email.isEnabled = !user.isEmailVerified

        } ?: kotlin.run {
            updateSignButtons(userHasToSignInOrUp = true)
        }

        updateAuthenticationState()
    }

    private fun updateAuthenticationState() {

        val statusText = auth?.currentUser?.let { user ->

            if (user.isEmailVerified) {
                getString(R.string.authentication_state_signed_in, user.email)
            } else {
                getString(R.string.authentication_state_signed_in_but_missing_verification, user.email)
            }

        } ?: kotlin.run {
            getString(R.string.authentication_state_signed_out)
        }

        authentication_textview_status.text = statusText
        delegate?.changeSubTitle(statusText)
    }


    private fun updateSignButtons(userHasToSignInOrUp: Boolean) {

        authentication_button_sign_in.visibility = if (userHasToSignInOrUp) View.VISIBLE else View.GONE
        authentication_button_sign_up.visibility = if (userHasToSignInOrUp) View.VISIBLE else View.GONE

        authentication_edittext_email.visibility = if (userHasToSignInOrUp) View.VISIBLE else View.GONE
        authentication_edittext_password.visibility = if (userHasToSignInOrUp) View.VISIBLE else View.GONE

        authentication_button_sign_out.visibility = if (userHasToSignInOrUp) View.GONE else View.VISIBLE
        authentication_button_verify_email.visibility = if (userHasToSignInOrUp) View.GONE else View.VISIBLE
    }

    companion object {

        fun newInstance(delegate: NavDrawerDelegate): AuthenticationFragment {
            val fragment = AuthenticationFragment()
            fragment.delegate = delegate
            return fragment
        }
    }
}