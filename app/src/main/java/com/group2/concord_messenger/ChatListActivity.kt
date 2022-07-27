package com.group2.concord_messenger

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.group2.concord_messenger.databinding.ActivityChatListBinding
import com.group2.concord_messenger.model.ChatListAdapter
import com.group2.concord_messenger.model.UserProfile


class ChatListActivity : AppCompatActivity() {
    // The Firestore database where all messages will be added
    private lateinit var fsDb: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private var user: FirebaseUser? = null
    private var googleAccount: GoogleSignInAccount? = null
    private var fromUser: UserProfile? = null
    private var userId: String? = null
    private var idToken: String? = null
    private var name: String? = null
    private var email: String? = null
    private lateinit var credential: AuthCredential
    private lateinit var chatsListView: ListView
    private lateinit var chatsRecyclerView: RecyclerView
    private var chatListAdapter: ChatListAdapter? = null

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

            // Token is unique to current phone will be reset once the user uninstalls the app
            val token = task.result
            Log.println(Log.DEBUG, "Chat", "token: $token")
            /* TODO: send to backend so that token can be used to target this device
                with messages using the FCM send API */
        })

        chatsRecyclerView = findViewById(R.id.current_chats_recyclerView)

        // Now begin a chain of asynchronous calls
        // Authenticate user -> insert user into db ->
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
        val query = fsDb.collection("groups")
            .document(userId!!).collection("userGroups")
        val options = FirestoreRecyclerOptions.Builder<UserProfile>()
            .setQuery(query, UserProfile::class.java).build()

        chatListAdapter = ChatListAdapter(options)
        chatsRecyclerView.layoutManager = LinearLayoutManager(this)
        // The RecyclerView itemAnimator needs to be set to null otherwise it
        // will throw an out of bounds exception
        chatsRecyclerView.itemAnimator = null
        chatListAdapter!!.startListening()
        chatsRecyclerView.adapter = chatListAdapter

        val uIRef = fsDb.collection("users").document(userId!!)
        uIRef.get().addOnCompleteListener {
            if (it.isSuccessful) {
                val doc = it.result
                if (doc.exists()) {
                    val fromUser = doc.toObject(UserProfile::class.java)!!

                    val groupsRef = fsDb.collection("groups").document(fromUser.uId)
                        .collection("userGroups")

                    groupsRef.get().addOnCompleteListener { p ->
                        if (p.isSuccessful) {
                            val currentChatNames = ArrayList<String>()
                            val userList = ArrayList<UserProfile>()
                            val groupsList = ArrayList<String>()
                            for (i in p.result) {
                                val contact = i.toObject(UserProfile::class.java)
                                if (contact.uId != fromUser.uId) {
                                    currentChatNames.add(contact.userName)
                                    userList.add(contact)
                                    // Document id works as the group id
                                    groupsList.add(i.id)
                                }
                            }

                            // This is how an onItemClickListener is implemented for a RecyclerView
                            // it is a pain and uses a custom interface in the Adapter
                            chatListAdapter!!.setOnItemClickListener(object : ChatListAdapter.ClickListener{
                                override fun onItemClicked(position: Int) {
                                    val intent = Intent(this@ChatListActivity, ChatActivity::class.java)
                                    intent.putExtra("fromUser", fromUser)
                                    intent.putExtra("toUser", userList[position])
                                    intent.putExtra("roomId", groupsList[position])
                                    startActivity(intent)
                                }
                            })

                        } else {
                            println("Getting list of current chats was unsuccessful")
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (chatListAdapter != null) {
            chatListAdapter!!.startListening()
        }
    }

    override fun onResume() {
        super.onResume()
        if (chatListAdapter != null) {
            chatListAdapter!!.startListening()
        }
    }

    override fun onPause() {
        super.onPause()
        if (chatListAdapter != null) {
            chatListAdapter!!.stopListening()
        }
    }
}
