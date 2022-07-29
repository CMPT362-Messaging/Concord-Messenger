package com.group2.concord_messenger

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity(), View.OnClickListener
{
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var usernameInputView: EditText
    private lateinit var emailInputView: EditText
    private lateinit var passInputView: EditText
    private lateinit var passConfirmationInputView: EditText
    private lateinit var createAccountButtonView: Button
    private lateinit var backButtonView: Button

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Get views
        usernameInputView = findViewById(R.id.register_edittext_username)
        emailInputView = findViewById(R.id.register_edittext_email)
        passInputView = findViewById(R.id.register_edittext_password)
        passConfirmationInputView = findViewById(R.id.register_edittext_confirm_password)
        createAccountButtonView = findViewById(R.id.register_button_create_account)
        backButtonView = findViewById(R.id.register_button_back)

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
                val passwordConfirm = passConfirmationInputView.text.toString()

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
        // Clear old errors from input fields
        clearFieldErrors(listOf<EditText>(
            usernameInputView,
            emailInputView,
            passInputView,
            passConfirmationInputView,
        ))

        // No blank fields or FireBase will throw an IllegalArgument
        // Passwords must match
        if(username.isBlank() || email.isBlank() || password.isBlank() ||
            password_confirm.isBlank() || password != password_confirm)
        {
            if(username.isBlank())
            {
                showFieldError(usernameInputView, resources.getString(R.string.login_error_empty_field))
            }
            else if(email.isBlank())
            {
                showFieldError(emailInputView, resources.getString(R.string.login_error_empty_field))
            }
            else if(password.isBlank())
            {
                showFieldError(passInputView, resources.getString(R.string.login_error_empty_field))
            }
            else if(password_confirm.isBlank())
            {
                showFieldError(passConfirmationInputView, resources.getString(R.string.login_error_empty_field))
            }
            else if(password != password_confirm)
            {
                showFieldError(passConfirmationInputView, resources.getString(R.string.login_error_password_not_match))
            }
        }
        else
        {
            // Create account
            firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener()
            {
                // Account created successfully
                authResult ->
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
                            showFieldError(emailInputView, resources.getString(R.string.login_error_invalid_email))
                        // Email already in use
                        FirebaseAuthUserCollisionException::class ->
                            showFieldError(emailInputView, resources.getString(R.string.login_error_account_exists))
                        // Weak password
                        FirebaseAuthWeakPasswordException::class ->
                            showFieldError(passInputView, resources.getString(R.string.login_error_invalid_password))
                        // Something broke
                        else -> {authResult.exception!!.printStackTrace()}
                    }
                }
            }
        }
    }

    private fun showFieldError(view: EditText, message: String)
    {
        view.error = message
    }

    private fun clearFieldErrors(views: List<EditText>)
    {
        for(view in views)
        {
            view.error = null
        }
    }
}