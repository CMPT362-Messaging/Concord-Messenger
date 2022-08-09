package com.group2.concord_messenger

import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
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
import com.group2.concord_messenger.ContactsActivityFolder.ContactsActivityAdapter
import com.group2.concord_messenger.dialogs.AttachPictureDialog
import com.group2.concord_messenger.dialogs.AudioDialog
import com.group2.concord_messenger.model.ChatMessageListAdapter
import com.group2.concord_messenger.model.ConcordMessage
import com.group2.concord_messenger.model.UserProfile
import com.group2.concord_messenger.utils.checkAudioFilePaths
import com.group2.concord_messenger.utils.checkPermissions
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


class GroupChatActivity : AppCompatActivity(), AudioDialog.AudioDialogListener, AttachPictureDialog.ProfilePicturePickDialogListener {
    private lateinit var editText: EditText
    private lateinit var sendBtn: Button

    // The Firestore database where all messages will be added
    private lateinit var fsDb: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var fromUser: UserProfile // i.e., you
    private lateinit var toUser: ArrayList<UserProfile>

    // Groups the users are a part of (may need to update if this is a new chat)
    private var fromGroups: MutableMap<String, Any>? = null
    private var toGroups: MutableMap<String, Any>? = null

    // Id of this chat group (for now this group only has two people)
    private var groupId: String = ""
    private var messageAdapter: ChatMessageListAdapter? = null

    private lateinit var profileButton: Button
    private lateinit var attachButton: ImageButton
    private lateinit var messageRecycler: RecyclerView
    private lateinit var takePicture: ActivityResultLauncher<Uri>
    private lateinit var photoButton: ImageButton





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        fsDb = FirebaseFirestore.getInstance()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        editText = findViewById(R.id.edit_gchat_message)
        sendBtn = findViewById(R.id.button_gchat_send)
        attachButton = findViewById(R.id.attachment_button)
        photoButton = findViewById(R.id.photo_button)

        fromUser = intent.extras?.get("fromUser") as UserProfile
        groupId = intent.extras?.get("roomId") as String
        setTitleFromGroup(groupId)

        updateCurrentUser()

        attachButton.setOnClickListener {
            checkPermissions(this)
            val dia = AudioDialog()
            dia.show(supportFragmentManager, "audioPicker")
        }
        photoButton.setOnClickListener {
            checkPermissions(this)
            val dialog = AttachPictureDialog()
            dialog.show(supportFragmentManager, "picturePicker")
        }

        takePicture =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
                if (success) {
                    val file = File(this.getExternalFilesDir(null), "pictureToShare.jpg")
                    val message = ConcordMessage(fromUser.uId, fromUser.userName, editText.text.toString(),
                        audio = false,
                        image = true,
                        imageName = UUID.randomUUID().toString()
                    )
                    sendMessage(message, file)
                }
            }

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
            messageAdapter = ChatMessageListAdapter(
                fromUser.uId,
                messageRecycler, options
            )
            // Add adapter (with messages) to the RecyclerView
            messageRecycler.layoutManager = LinearLayoutManager(this)
            messageRecycler.adapter = messageAdapter
            messageAdapter!!.startListening()

            sendBtn.setOnClickListener {
                val message =
                    ConcordMessage(fromUser.uId, fromUser.userName, editText.text.toString())
                editText.text.clear()
                sendMessage(message)
            }
        }
    }

    private fun setTitleFromGroup(groupId : String) {
        val fsDb = FirebaseFirestore.getInstance()
        val uIRef = fsDb.collection("MultipleUserGroup").document(groupId)
        var groupName = ""
        uIRef.get().addOnCompleteListener {
            if (it.isSuccessful && it.result.exists()) {
                val doc = it.result
                val users: java.util.ArrayList<MutableMap<String, String>> =
                    doc.get("Users") as java.util.ArrayList<MutableMap<String, String>>

                for (userInd in 0 until users.size - 1) {
                    val curUserMap = users[userInd]
                    val mName = curUserMap["userName"]
                    groupName = "$groupName $mName"
                }
                val toolBar = findViewById<Toolbar>(R.id.contacts_toolbar)
                toolBar.title = groupName


            } else {

            }
        }
    }


    private fun sendMessage(msg: ConcordMessage,  fileToSend: File? = null) {

        // Add the message to the db
        // The actual message will be nested within
        // messages
        //      |-> groupMessages
        //          |-> [groupId]
        //              |-> message_1
        //              . . .
        //              |-> message_n
        // Currently a group only holds 2 people
        if (msg.image) {
            val imageFileName = msg.imageName + ".jpg"  // one image per message
            val storage = Firebase.storage
            val imageRef = storage.reference.child("photo-sharing/${imageFileName}")
            val uri = FileProvider.getUriForFile(this, "com.group2.concord_messenger", fileToSend!!)
            imageRef.putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    fsDb.collection("messages").document(groupId)
                        .collection("groupMessages").add(msg)
                }
        } else if (msg.audio) {
            // Upload the audio file to Firebase Storage
            msg.audioId = UUID.randomUUID().toString()
            val audioFileName = msg.audioId + ".3gp"  // each message is limited to one audio recording
            val storage = Firebase.storage
            val audioRef = storage.reference.child("audio/${audioFileName}")
            // save the audio file in permanent local storage so it doesn't need to be fetched from Firebase everytime the chat is opened
            checkAudioFilePaths(this)
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
            fileToSend?.copyTo(persistentAudioFile, overwrite = true)
            audioRef.putFile(persistentAudioFile.toUri())
                .addOnSuccessListener { taskSnapshot ->
                    fsDb.collection("messages").document(groupId)
                        .collection("groupMessages").add(msg)
                }
        } else {
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
        // Add the message to the receiving user's inbox
        // A user's inbox is a collection of every message sent to them, used for notifications
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

    override fun onOpenCameraClick(dialog: DialogFragment) {
        val uri = FileProvider.getUriForFile(this, "com.group2.concord_messenger", File(this.getExternalFilesDir(null), "pictureToShare.jpg"))
        takePicture.launch(uri)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}