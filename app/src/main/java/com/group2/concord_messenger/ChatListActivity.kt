package com.group2.concord_messenger

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.group2.concord_messenger.model.UserProfile


class ChatListActivity : AppCompatActivity() {
    // The Firestore database where all messages will be added
    private lateinit var fsDb: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private var fromUser: UserProfile? = null
    private var userId: String? = null
    private var name: String? = null
    private var email: String? = null
    private lateinit var chatsListView: ListView
    private var chatListAdapter: ArrayAdapter<String>? = null
    private lateinit var fab: FloatingActionButton
    private lateinit var profileButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)
        firebaseAuth = FirebaseAuth.getInstance()
        val toolbar = findViewById<Toolbar>(R.id.toolbar_chat_list)
        setSupportActionBar(toolbar)
        fab = findViewById(R.id.fab)
        profileButton = findViewById(R.id.profile_button)

        // Get the Firestore instance, which holds all users, messages, and groups
        fsDb = FirebaseFirestore.getInstance()

        chatsListView = findViewById(R.id.current_chats_listView)

        ConcordDatabase.getCurrentUser {
            if (it != null) {
                fromUser = it
                userId = fromUser!!.uId
                email = fromUser!!.email
                name = fromUser!!.userName

                // User info gathered, show chats associated with the current user
                showCurrentChats()
                // Now safe to enable the FAB to create a new chat
                fab.setOnClickListener {
                    val intent = Intent(this, ContactsActivity::class.java)
                    startActivity(intent)
                }
            } else {
                // Temporary solution to insert the logged-in user who is authenticated with
                // Firebase but not in the Concord Messenger database
                ConcordDatabase.insertCurrentUser { result ->
                    Log.println(Log.DEBUG, "ChatList", "New user result code: $result")
                    ConcordDatabase.getCurrentUser { newUser ->
                        if (newUser != null) {
                            fromUser = newUser
                            userId = fromUser!!.uId
                            email = fromUser!!.email
                            name = fromUser!!.userName

                            // User info gathered, show chats associated with the current user
                            showCurrentChats()
                            // Now safe to enable the FAB to create a new chat
                            fab.setOnClickListener {
                                val intent = Intent(this, ContactsActivity::class.java)
                                startActivity(intent)
                            }
                        } else {
                            Log.d(TAG, "Critical user login failure")
                        }
                    }
                }
            }
            profileButton.setOnClickListener{
                val intent = Intent(this, UserProfileActivity::class.java)
                intent.putExtra("user", fromUser)
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
                        println("Contact name: ${contact.userName}")
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.chat_list_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        firebaseAuth.signOut()
        // Remove the user's instance token
        if (fromUser != null) {
            fsDb.collection("users").document(fromUser!!.uId)
                .update(mapOf("tokenId" to ""))
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(this, gso).signOut()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        return true
    }
}
