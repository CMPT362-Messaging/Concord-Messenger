package com.group2.concord_messenger

import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity(), View.OnClickListener
{
    private lateinit var inputViews: List<TextInputLayout>
    private lateinit var interactableViews: List<View>

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

        inputViews = listOf(
            usernameInputLayoutView,
            emailInputLayoutView,
            passInputLayoutView,
            passConfirmInputLayoutView
        )
        interactableViews = listOf(
            usernameInputLayoutView,
            emailInputLayoutView,
            passInputLayoutView,
            passConfirmInputLayoutView,
            createAccountButtonView,
            backButtonView
        )

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

                // Clear old errors from input fields
                clearFieldErrors(inputViews)
                // Clear focused fields
                clearFocuses(interactableViews)
                // Close the keyboard
                closeKeyboard(this)
                // Prevent input and show progress bar
                setLoading(this, progressBarView, interactableViews)

                createAccount(username, email, password, passwordConfirm)
            }

            // BACK BUTTON
            R.id.register_button_back ->
            {
                onBackPressed()
            }
        }
    }

    override fun onBackPressed()
    {
        finish()
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
    }

    private fun createAccount(username: String, email: String, password: String, password_confirm: String)
    {
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
            clearLoading(this, progressBarView, interactableViews)
        }
        else
        {
            setLoading(this, progressBarView, interactableViews)

            // Create account
            firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener()
            {
                authResult ->
                // Account created successfully
                if(authResult.isSuccessful)
                {
                    // Set account's username
                    val displayNameChange = UserProfileChangeRequest.Builder()
                        .setDisplayName(username)
                        .build()
                    firebaseAuth.currentUser!!.updateProfile(displayNameChange).addOnCompleteListener()
                    {
                        clearLoading(this, progressBarView, interactableViews)
                        startActivityClear(this, HomeActivity::class.java)
                    }
                }
                else
                {
                    // Failed to create account
                    clearLoading(this, progressBarView, interactableViews)
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
}