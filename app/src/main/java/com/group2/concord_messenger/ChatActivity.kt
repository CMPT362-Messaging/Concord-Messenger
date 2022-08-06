package com.group2.concord_messenger

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.group2.concord_messenger.dialogs.AudioDialog
import com.group2.concord_messenger.model.ChatMessageListAdapter
import com.group2.concord_messenger.model.ConcordMessage
import com.group2.concord_messenger.model.UserProfile
import com.group2.concord_messenger.utils.checkPermissions
import java.io.File

class ChatActivity : AppCompatActivity(), AudioDialog.AudioDialogListener {
    private lateinit var editText: EditText
    private lateinit var sendBtn: Button
    private lateinit var attachButton: ImageButton
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
    private lateinit var messageRecycler: RecyclerView

    private lateinit var profileButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        fsDb = FirebaseFirestore.getInstance()
        val toolBar = findViewById<Toolbar>(R.id.toolbar_gchannel)

        editText = findViewById(R.id.edit_gchat_message)
        sendBtn = findViewById(R.id.button_gchat_send)
        attachButton = findViewById(R.id.attachment_button)

        fromUser = intent.extras?.get("fromUser") as UserProfile
        toUser = intent.extras?.get("toUser") as UserProfile
        // Set the title of the chat to the toUser's name
        toolBar.title = toUser.userName
        fromGroups = fromUser.groups
        toGroups = toUser.groups
        groupId = intent.extras?.get("roomId") as String

        attachButton.setOnClickListener {
            checkPermissions(this)
            val dia = AudioDialog()
            dia.show(supportFragmentManager, "audioPicker")
        }

        updateCurrentUser()

        profileButton = findViewById(R.id.profile_button)
        profileButton.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            intent.putExtra("user", toUser)
            startActivity(intent)
        }
    }

    private fun updateCurrentUser() {
        // Update user groups if they've been added to any
        val uIRef = fsDb.collection("users").document(fromUser.uId)
        uIRef.get().addOnCompleteListener {
            if (it.isSuccessful && it.result.exists()) {
                val doc = it.result
                val userSnapshot = doc.toObject(UserProfile::class.java)
                fromUser.groups = userSnapshot!!.groups
                fromGroups = fromUser.groups
                if (userSnapshot.groups != null) {
                    println("There was historical data: ${userSnapshot.groups!!.size}")
                    println("New user group size: ${fromUser.groups!!.size}")
                }
            } else {
                println("No historical data to retrieve")
            }

            // Now safe to look for existing chats
            // Look first in the easy place, in the groups associated with the current user
            if (fromGroups != null) {
                for ((key, v) in fromGroups!!) {
                    if (toGroups != null) {
                        if (toGroups!!.contains(key)) {
                            groupId = key
                            println("Already existing chat found in fromUser's groups")
                        }
                    }
                }
            }
            // Chat still wasn't found so we can be assured this is
            // a new chat and generate a new group id
            if (groupId == "none") {
                groupId = fsDb.collection("messages").document().id
                Log.w(TAG, "Chat was not found, generating a new id")
            }

            // Get all messages associated with this group from the remote db
            val query = fsDb.collection("messages")
                .document(groupId).collection("groupMessages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
            val options = FirestoreRecyclerOptions.Builder<ConcordMessage>()
                .setQuery(query, ConcordMessage::class.java).build()

            messageRecycler = findViewById(R.id.recycler_gchat)
            messageAdapter = ChatMessageListAdapter(fromUser.uId,
                messageRecycler, options)
            // Add adapter (with messages) to the RecyclerView
            messageRecycler.layoutManager = LinearLayoutManager(this)
            messageRecycler.adapter = messageAdapter
//            messageAdapter!!.setMediaPlayer(mp)
            messageAdapter!!.startListening()

            sendBtn.setOnClickListener {
                val message = ConcordMessage(fromUser.uId, fromUser.userName,editText.text.toString())
                editText.text.clear()
                sendMessage(message)
            }
        }
    }

    private fun sendMessage(msg: ConcordMessage, audioFile: File? = null) {
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
        println("sendMessage: toUser: ${toUser.userName}, ${toUser.uId}")
        println("sendMessage: toUser groups size ${toUser.groups!!.size}")
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
            .collection("groupMessages").add(msg).addOnSuccessListener {
                // Upload the audio file to Firebase Storage
                if (msg.audio) {
                    val audioFileName = it.id + ".3gp"  // each message is limited to one audio recording
                    val storage = Firebase.storage
                    val imageRef = storage.reference.child("audio/${audioFileName}")
                    imageRef.putFile(audioFile?.toUri()!!)
                        .addOnSuccessListener { taskSnapshot ->
                        }
                    // save the audio file in permanent local storage so it doesn't need to be fetched from Firebase everytime the chat is opened
                    val persistentAudioFile = File(this.filesDir, "audio/${audioFileName}")
                    // check that audio directory and file do not exist
                    if (!persistentAudioFile.exists()) {
                        persistentAudioFile.createNewFile()
                    }
                    // check that audio directory and file do not exist
                    val audioDir = File("${this.filesDir}/audio/")
                    if (!audioDir.exists()) {
                        audioDir.mkdir()
                    }
                    if (!persistentAudioFile.exists()) {
                        persistentAudioFile.createNewFile()
                    }
                    audioFile.copyTo(persistentAudioFile, overwrite = true)
                }
            }
    }

    override fun onStart() {
        super.onStart()
        if (messageAdapter != null) {
            messageRecycler.recycledViewPool.clear()
            messageAdapter!!.notifyDataSetChanged()
            messageAdapter!!.startListening()
        }
    }

    override fun onStop() {
        super.onStop()
        if (messageAdapter != null) {
            messageAdapter!!.stopListening()
        }
    }



    override fun onAudioComplete(dialog: DialogFragment, filename: String) {
        val message = ConcordMessage(fromUser.uId, fromUser.userName, editText.text.toString(), true)
        editText.text.clear()
        sendMessage(message, File(filename))
    }
}