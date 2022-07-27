package com.group2.concord_messenger

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
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
    private var googleAccount: GoogleSignInAccount? = null
    private var fromUser: UserProfile? = null
    private var userId: String? = null
    private var idToken: String? = null
    private var name: String? = null
    private var email: String? = null
    private lateinit var credential: AuthCredential
    private lateinit var chatsListView: ListView
    private var chatListAdapter: ArrayAdapter<String>? = null

    private lateinit var binding: ActivityChatListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()

        // TODO: Notifications
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

        // Get the Firestore instance, which holds all users, messages, and groups
        fsDb = FirebaseFirestore.getInstance()

        // Get Firebase Cloud Messaging registration token
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(ContentValues.TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Token is unique to current phone and will be reset once the user uninstalls the app
            val token = task.result
            Log.println(Log.DEBUG, "Chat", "token: $token")
            /* TODO: send to backend so that token can be used to target this device
                with messages using the FCM send API */
        })

        chatsListView = findViewById(R.id.current_chats_listView)

        // Now begins a chain of asynchronous calls
        // Authenticate user -> insert user into db/update user ->
        // -> show current chats associated with this user -> enable ability to make new chats

        // Sign the user in using their Google account
        authenticate()
    }

    private fun authenticate() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Callback
        val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { it ->
            if (it.resultCode == Activity.RESULT_OK) {
                val data: Intent? = it.data
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                if (task.isSuccessful) {
                    googleAccount = task.result
                    userId = googleAccount!!.id
                    idToken = googleAccount!!.idToken
                    name = googleAccount!!.displayName
                    email = googleAccount!!.email
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
                                // User has been authenticated, now safe to insert this user
                                // into the Firestore db
                                insertThisUser()
                            } else {
                                Log.println(Log.DEBUG, "Chat", "resultLauncher: Credential login failed")
                                authResult.exception?.printStackTrace()
                            }
                        })
                }
            }
        }

        // Execute callback
        resultLauncher.launch(googleSignInClient.signInIntent)
        userId = GoogleSignIn.getLastSignedInAccount(this)?.id
    }

    private fun insertThisUser() {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(userId!!)
        userRef.get().addOnCompleteListener {
            if (it.isSuccessful && it.result.exists()) {
                val doc = it.result
                fromUser = doc.toObject(UserProfile::class.java)
                println("retrieved current user from db")
                if (fromUser!!.groups != null) {
                    println("There was historical data: ${fromUser!!.groups!!.size}")
                }
            } else {
                // User does not already exist, make a new user and insert them
                println("No historical data to retrieve")
                fromUser = UserProfile(
                    userId!!,
                    name!!,
                    idToken!!,
                    null,
                    email!!
                )
                db.collection("users").document(fromUser!!.uId)
                    .set(fromUser!!, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d(ContentValues.TAG, "DocumentSnapshot successfully written!")
                    }
                    .addOnFailureListener { e -> Log.w(ContentValues.TAG, "Error writing document", e) }
            }
            // Now safe to show chats associated with the current user
            showCurrentChats()

            // Now safe to enable the FAB to create a new chat
            binding.fab.setOnClickListener { view ->
                val intent = Intent(this, ContactsActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun showCurrentChats() {
        val currentChatNames = ArrayList<String>()
        val userList = ArrayList<UserProfile>()
        val groupsList = ArrayList<String>()
        chatListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
            android.R.id.text1, currentChatNames)

        chatsListView.adapter = chatListAdapter

        val ref = fsDb.collection("groups")
            .document(userId!!).collection("userGroups")

        // Listen for changes in current chats user is a part of
        ref.addSnapshotListener {snapshot, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed.", e)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                Log.d(TAG, "Current data size: ${snapshot.size()}")
                // Clear adapter and all lists so there is no possibility
                // of duplicate entries
                chatListAdapter!!.clear()
                currentChatNames.clear()
                userList.clear()
                groupsList.clear()
                // Add all new data
                for (i in snapshot.documents) {
                    val contact = i.toObject(UserProfile::class.java)
                    if (contact!!.uId != fromUser!!.uId) {
                        currentChatNames.add(contact.userName)
                        userList.add(contact)
                        // Document id works as the group id
                        groupsList.add(i.id)
                    }
                }
                chatListAdapter!!.notifyDataSetChanged()
            } else {
                Log.d(TAG, "Current data: null")
            }
        }
        chatsListView.setOnItemClickListener { parent, view, position, id ->
            val intent = Intent(this@ChatListActivity, ChatActivity::class.java)
            intent.putExtra("fromUser", fromUser)
            intent.putExtra("toUser", userList[position])
            intent.putExtra("roomId", groupsList[position])
            startActivity(intent)
        }
    }
}
