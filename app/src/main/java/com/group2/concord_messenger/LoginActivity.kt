package com.group2.concord_messenger

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity(), View.OnClickListener
{
    private lateinit var inputViews: List<TextInputLayout>
    private lateinit var interactableViews: List<View>

    private lateinit var emailInputLayoutView: TextInputLayout
    private lateinit var passInputLayoutView: TextInputLayout
    private lateinit var emailInputView: EditText
    private lateinit var passInputView: EditText
    private lateinit var loginButtonView: Button
    private lateinit var registerButtonView: Button
    private lateinit var progressBarView: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Get views
        emailInputLayoutView = findViewById(R.id.login_textlayout_email)
        passInputLayoutView = findViewById(R.id.login_textlayout_password)
        emailInputView = findViewById(R.id.login_edittext_email)
        passInputView = findViewById(R.id.login_edittext_password)
        loginButtonView = findViewById(R.id.login_button_login)
        registerButtonView = findViewById(R.id.login_button_register)
        progressBarView = findViewById(R.id.login_progress_bar)

        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        loginButtonView.setOnClickListener(this)
        registerButtonView.setOnClickListener(this)

        inputViews = listOf(
            emailInputLayoutView,
            passInputLayoutView,
        )
        interactableViews = listOf(
            emailInputLayoutView,
            passInputLayoutView,
            loginButtonView,
            registerButtonView
        )

        if(Firebase.auth.currentUser != null)
        {
            startActivityClear(this, HomeActivity::class.java)
        }
    }

    override fun onClick(view: View?)
    {
        when(view?.id)
        {
            // LOGIN BUTTON
            R.id.login_button_login ->
            {
                val email = emailInputView.text.toString()
                val password = passInputView.text.toString()

                // Clear old errors from input fields
                clearFieldErrors(inputViews)
                // Clear focused fields
                clearFocuses(interactableViews)
                // Close the keyboard
                closeKeyboard(this)
                // Prevent input and show progress bar
                setLoading(this, progressBarView, interactableViews)

                login(email, password)
            }

            // REGISTER BUTTON
            R.id.login_button_register ->
            {
                startActivity(Intent(this, RegisterActivity::class.java))
                overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
            }
        }
    }

    private fun login(email: String, password: String)
    {
        setLoading(this, progressBarView, interactableViews)

        // No blank fields or FireBase will throw an IllegalArgument
        if(email.isBlank() || password.isBlank())
        {
            if(email.isBlank())
            {
                showFieldError(emailInputLayoutView, resources.getString(R.string.login_error_empty_field))
            }
            else if(password.isBlank())
            {
                showFieldError(passInputLayoutView, resources.getString(R.string.login_error_empty_field))
            }
            clearLoading(this, progressBarView, interactableViews)
        }
        else
        {
            Firebase.auth.signInWithEmailAndPassword(email, password).addOnCompleteListener()
            {
                authResult ->
                clearLoading(this, progressBarView, interactableViews)
                // Login successful
                if(authResult.isSuccessful)
                {
                    startActivityClear(this, HomeActivity::class.java)
                }
                else
                {
                    // Login failed
                    when(authResult.exception!!::class)
                    {
                        // https://firebase.google.com/docs/reference/kotlin/com/google/firebase/auth/FirebaseAuth#exceptions_6
                        // Invalid email
                        FirebaseAuthInvalidUserException::class ->
                            showFieldError(passInputLayoutView, resources.getString(R.string.login_error_wrong_password))
                        // Invalid password
                        FirebaseAuthInvalidCredentialsException::class ->
                            showFieldError(passInputLayoutView, resources.getString(R.string.login_error_wrong_password))

                        else ->
                        {
                            authResult.exception!!.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}