package com.group2.concord_messenger

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.group2.concord_messenger.model.ChatMessageListAdapter
import com.group2.concord_messenger.model.ConcordMessage
import com.group2.concord_messenger.model.UserProfile
import kotlin.collections.ArrayList


class ChatActivity : AppCompatActivity() {
    private lateinit var editText: EditText
    private lateinit var sendBtn: Button
    // The Firestore database where all messages will be added
    private lateinit var fsDb: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var fromUser: UserProfile // i.e., you
    private lateinit var toUser: UserProfile
    // Groups the users are a part of (may need to update if this is a new chat)
    private var fromGroups: MutableMap<String, Any>? = null
    private var toGroups: MutableMap<String, Any>? = null
    // Id of this chat group (for now this group only has two people)
    private var groupId: String = ""
    private var messageAdapter: ChatMessageListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        fsDb = FirebaseFirestore.getInstance()
        val toolBar = findViewById<Toolbar>(R.id.toolbar_gchannel)

        editText = findViewById(R.id.edit_gchat_message)
        sendBtn = findViewById(R.id.button_gchat_send)

        fromUser = intent.extras?.get("fromUser") as UserProfile
        toUser = intent.extras?.get("toUser") as UserProfile
        // Set the title of the chat to the toUser's name
        toolBar.title = toUser.userName
        fromGroups = fromUser.groups
        toGroups = toUser.groups
        groupId = intent.extras?.get("roomId") as String
        if (groupId == "none") {
            groupId = fsDb.collection("messages").document().id
            println("New groupId: $groupId")
            if (fromGroups != null) {
                for ((key, v) in fromGroups!!) {
                    if (toGroups != null) {
                        if (toGroups!!.contains(key)) {
                            groupId = key
                        }
                    }
                }
            }
        }

        // Get all messages associated with this group from the remote db
        val query = fsDb.collection("messages")
            .document(groupId).collection("groupMessages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
        val options = FirestoreRecyclerOptions.Builder<ConcordMessage>()
            .setQuery(query, ConcordMessage::class.java).build()



        val messageRecycler: RecyclerView = findViewById(R.id.recycler_gchat)
        messageAdapter = ChatMessageListAdapter(fromUser.uId,
            messageRecycler, options)
        // Add adapter (with messages) to the RecyclerView
        messageRecycler.layoutManager = LinearLayoutManager(this)
        messageRecycler.adapter = messageAdapter

        sendBtn.setOnClickListener {
            val message = ConcordMessage(fromUser.uId, fromUser.userName,editText.text.toString())
            editText.text.clear()

            sendMessage(message)
        }
    }

    private fun sendMessage(msg: ConcordMessage) {
        if (fromGroups == null) {
            fromGroups = mutableMapOf()
        }
        // Indicate that this user is apart of this chat group
        fromGroups!![groupId] = true
        fromUser.groups = fromGroups
        // Write this new data to the db
        fsDb.collection("users").document(fromUser.uId)
            .set(fromUser, SetOptions.merge())
        fsDb.collection("groups").document(fromUser.uId)
            .collection("userGroups").document(groupId).set(toUser, SetOptions.merge())

        // Do the same process for toUser
        if (toGroups == null) {
            toGroups = mutableMapOf()
        }
        // Indicate that this user is apart of this chat group
        toGroups!![groupId] = true
        toUser.groups = toGroups
        // Write this new data to the db
        fsDb.collection("users").document(toUser.uId)
            .set(toUser, SetOptions.merge())
        fsDb.collection("groups").document(toUser.uId)
            .collection("userGroups").document(groupId).set(fromUser, SetOptions.merge())

        // Add the message to the db
        // The actual message will be nested within
        // messages
        //      |-> groupMessages
        //          |-> [groupId]
        //              |-> message_1
        //              . . .
        //              |-> message_n
        // Currently a group only holds 2 people
        fsDb.collection("messages").document(groupId)
            .collection("groupMessages").add(msg)
    }

    override fun onStart() {
        super.onStart()
        if (messageAdapter != null) {
            messageAdapter!!.startListening()
        }
    }

    override fun onStop() {
        super.onStop()
        if (messageAdapter != null) {
            messageAdapter!!.stopListening()
        }
    }
}