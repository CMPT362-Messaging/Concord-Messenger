package com.group2.concord_messenger

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity(), View.OnClickListener
{
    private lateinit var usernameView: TextView
    private lateinit var emailView: TextView
    private lateinit var uidView: TextView
    private lateinit var logoutButtonView: TextView

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        usernameView = findViewById(R.id.home_textview_username)
        emailView = findViewById(R.id.home_textview_email)
        uidView = findViewById(R.id.home_textview_uid)
        logoutButtonView = findViewById(R.id.home_button_logout)

        firebaseAuth = FirebaseAuth.getInstance()

        usernameView.text = firebaseAuth.currentUser?.displayName
        emailView.text = firebaseAuth.currentUser?.email
        uidView.text = firebaseAuth.currentUser?.uid

        logoutButtonView.setOnClickListener(this)
    }

    override fun onClick(view: View?)
    {
        firebaseAuth.signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(this, gso).signOut()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}