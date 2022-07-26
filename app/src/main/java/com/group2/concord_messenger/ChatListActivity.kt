package com.group2.concord_messenger

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.group2.concord_messenger.databinding.ActivityChatListBinding
import com.group2.concord_messenger.model.UserProfile

class ChatListActivity : AppCompatActivity() {
    // The Firestore database where all messages will be added
    private lateinit var fsDb: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private var user: FirebaseUser? = null
    private var userId: String? = null
    private var idToken: String? = null
    private var name: String? = null
    private var email: String? = null
    private lateinit var credential: AuthCredential

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityChatListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Creating channel to show notifications.
            val channelId = "1"
            val channelName = "default channel"
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(
                NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW)
            )
        }

        authenticate()

        fsDb = FirebaseFirestore.getInstance()

        // Get the user's Firebase token
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(ContentValues.TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.println(Log.DEBUG, "Chat", "token: $token")
            /* TODO: send to backend so that token can be used to target this device
                with messages using the FCM send API */
        })

        binding.fab.setOnClickListener { view ->
            val intent = Intent(this, ContactsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun insertThisUser() {
        val profile = UserProfile(
            userId!!,
            name!!,
            idToken!!,
            null,
            email!!
        )
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(profile.uId)
            .set(profile, SetOptions.merge())
            .addOnSuccessListener { Log.d(ContentValues.TAG, "DocumentSnapshot successfully written!") }
            .addOnFailureListener { e -> Log.w(ContentValues.TAG, "Error writing document", e) }
    }

    private fun authenticate() {
        firebaseAuth = FirebaseAuth.getInstance()
        val authStateListener = FirebaseAuth.AuthStateListener {
            user = firebaseAuth.currentUser
            if (user != null) {
                // User is signed in
                Log.println(Log.DEBUG, "Chat", "User signed in: ${user!!.uid}")
            } else {
                Log.println(Log.DEBUG, "Chat", "User is signed out")
            }
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { it ->
            if (it.resultCode == Activity.RESULT_OK) {
                val data: Intent? = it.data
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                if (task.isSuccessful) {
                    val account = task.result
                    userId = account.id
                    idToken = account.idToken
                    name = account.displayName
                    email = account.email
                    println(idToken)
                    println(name)
                    println(email)
                    credential = GoogleAuthProvider.getCredential(idToken, null)
                    firebaseAuth.signInWithCredential(credential)
                        .addOnCompleteListener(OnCompleteListener { authResult ->
                            if (authResult.isSuccessful) {
                                Log.println(Log.DEBUG, "Chat", "resultLauncher: Credential login successful")
                                val user = hashMapOf(
                                    "tokenId" to idToken,
                                    "name" to name,
                                    "email" to email
                                )
                                // Insert this user into the firestore db
                                insertThisUser()
                            } else {
                                Log.println(Log.DEBUG, "Chat", "resultLauncher: Credential login failed")
                                authResult.exception?.printStackTrace()
                            }
                        })
                } else {
                    // Sign in failed
                    Log.println(Log.DEBUG, "Chat", "Login failed")
                }
            } else {
                Log.println(Log.DEBUG, "Chat", "Something went wrong ${it.resultCode}")
            }
        }

        resultLauncher.launch(googleSignInClient.signInIntent)
    }
}