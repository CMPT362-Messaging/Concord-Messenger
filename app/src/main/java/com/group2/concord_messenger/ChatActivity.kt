package com.group2.concord_messenger

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.core.QueryListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.group2.concord_messenger.model.ChatMessageListAdapter
import com.group2.concord_messenger.model.ConcordMessage
import com.group2.concord_messenger.model.MessageContent
import com.group2.concord_messenger.model.UserProfile


class ChatActivity : AppCompatActivity() {
    private lateinit var editText: EditText
    private lateinit var sendBtn: Button
    // The Firestore database where all messages will be added
    private lateinit var fsDb: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private var user: FirebaseUser? = null
    private var userId: String? = null
    private var idToken: String? = null
    private var name: String? = null
    private var email: String? = null
    private lateinit var credential: AuthCredential


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        editText = findViewById(R.id.edit_gchat_message)
        sendBtn = findViewById(R.id.button_gchat_send)

        intent.extras?.let {
            for (key in it.keySet()) {
                val value = intent.extras!!.get(key)
                Log.d(TAG, "Key: $key Value: $value")
            }
        }

        val messageList = ArrayList<MessageContent>()
        val messageRecycler: RecyclerView = findViewById(R.id.recycler_gchat)
        val messageAdapter = ChatMessageListAdapter(messageList)
        messageRecycler.layoutManager = LinearLayoutManager(this)
        messageRecycler.adapter = messageAdapter

        sendBtn.setOnClickListener {
            val message = editText.text.toString()
            messageList.add(MessageContent(message, null, 0))
            editText.text.clear()
            messageAdapter.notifyItemInserted(messageList.size - 1)
            messageRecycler.scrollToPosition(messageAdapter.itemCount - 1)

            sendMessage(message)
        }
    }

    fun sendMessage(msg: String) {

    }

    override fun onStart() {
        super.onStart()
        val account = GoogleSignIn.getLastSignedInAccount(this)
    }
}