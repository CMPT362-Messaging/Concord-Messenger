package com.group2.concord_messenger

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity(), View.OnClickListener
{
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var usernameInputLayoutView: TextInputLayout
    private lateinit var emailInputLayoutView: TextInputLayout
    private lateinit var passInputLayoutView: TextInputLayout
    private lateinit var passConfirmInputLayoutView: TextInputLayout
    private lateinit var usernameInputView: EditText
    private lateinit var emailInputView: EditText
    private lateinit var passInputView: EditText
    private lateinit var passConfirmInputView: EditText
    private lateinit var createAccountButtonView: Button
    private lateinit var backButtonView: Button
    private lateinit var progressBarView: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Get views
        usernameInputLayoutView = findViewById(R.id.register_textlayout_username)
        emailInputLayoutView = findViewById(R.id.register_textlayout_email)
        passInputLayoutView = findViewById(R.id.register_textlayout_password)
        passConfirmInputLayoutView = findViewById(R.id.register_textlayout_confirm_password)
        usernameInputView = findViewById(R.id.register_edittext_username)
        emailInputView = findViewById(R.id.register_edittext_email)
        passInputView = findViewById(R.id.register_edittext_password)
        passConfirmInputView = findViewById(R.id.register_edittext_confirm_password)
        createAccountButtonView = findViewById(R.id.register_button_create_account)
        backButtonView = findViewById(R.id.register_button_back)
        progressBarView = findViewById(R.id.register_progress_bar)

        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        createAccountButtonView.setOnClickListener(this)
        backButtonView.setOnClickListener(this)

        firebaseAuth = Firebase.auth
    }

    override fun onClick(view: View?)
    {
        when(view?.id)
        {
            // CREATE ACCOUNT BUTTON
            R.id.register_button_create_account ->
            {
                val username = usernameInputView.text.toString()
                val email = emailInputView.text.toString()
                val password = passInputView.text.toString()
                val passwordConfirm = passConfirmInputView.text.toString()

                createAccount(username, email, password, passwordConfirm)
            }

            // BACK BUTTON
            R.id.register_button_back ->
            {
                println("DEBUG ${Firebase.auth.currentUser?.email} ${Firebase.auth.currentUser?.displayName}")
            }
        }
    }

    private fun createAccount(username: String, email: String, password: String, password_confirm: String)
    {
        val fieldViews = listOf(
            usernameInputLayoutView,
            emailInputLayoutView,
            passInputLayoutView,
            passConfirmInputLayoutView,
        )
        // Clear old errors from input fields
        clearFieldErrors(fieldViews)
        // Clear focused fields
        clearFieldFocuses(fieldViews)

        // No blank fields or FireBase will throw an IllegalArgument
        // Passwords must match
        if(username.isBlank() || email.isBlank() || password.isBlank() ||
            password_confirm.isBlank() || password != password_confirm)
        {
            if(username.isBlank())
            {
                showFieldError(usernameInputLayoutView, resources.getString(R.string.login_error_empty_field))
            }
            else if(email.isBlank())
            {
                showFieldError(emailInputLayoutView, resources.getString(R.string.login_error_empty_field))
            }
            else if(password.isBlank())
            {
                showFieldError(passInputLayoutView, resources.getString(R.string.login_error_empty_field))
            }
            else if(password_confirm.isBlank())
            {
                showFieldError(passConfirmInputLayoutView, resources.getString(R.string.login_error_empty_field))
            }
            else if(password != password_confirm)
            {
                showFieldError(passConfirmInputLayoutView, resources.getString(R.string.login_error_password_not_match))
            }
            setLoading(false)
        }
        else
        {
            setLoading(true)

            // Create account
            firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener()
            {
                authResult ->
                setLoading(false)

                // Account created successfully
                if(authResult.isSuccessful)
                {
                    // Set account's username
                    val displayNameChange = UserProfileChangeRequest.Builder()
                        .setDisplayName(username)
                        .build()
                    firebaseAuth.currentUser?.updateProfile(displayNameChange)
                }
                else
                {
                    // Failed to create account
                    when(authResult.exception!!::class)
                    {
                        // https://firebase.google.com/docs/reference/kotlin/com/google/firebase/auth/FirebaseAuth#exceptions_1
                        // Invalid email
                        FirebaseAuthInvalidCredentialsException::class ->
                            showFieldError(emailInputLayoutView, resources.getString(R.string.login_error_invalid_email))
                        // Email already in use
                        FirebaseAuthUserCollisionException::class ->
                            showFieldError(emailInputLayoutView, resources.getString(R.string.login_error_account_exists))
                        // Weak password
                        FirebaseAuthWeakPasswordException::class ->
                            showFieldError(passInputLayoutView, resources.getString(R.string.login_error_invalid_password))

                        else -> {authResult.exception!!.printStackTrace()}
                    }
                }
            }
        }
    }

    private fun showFieldError(view: TextInputLayout, message: String)
    {
        view.error = message
    }

    private fun clearFieldErrors(views: List<TextInputLayout>)
    {
        for(view in views)
        {
            view.error = null
        }
    }

    private fun clearFieldFocuses(views: List<TextInputLayout>)
    {
        for(view in views)
        {
            view.clearFocus()
        }
    }

    private fun setLoading(loading: Boolean)
    {
        if(loading)
        {
            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            progressBarView.visibility = View.VISIBLE
        }
        else
        {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            progressBarView.visibility = View.INVISIBLE
        }
    }
}