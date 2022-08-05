package com.group2.concord_messenger.model

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.group2.concord_messenger.R
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

const val FIREBASE_STORAGE_AUDIO_REPO = "gs://concord-messenger.appspot.com/audio/"

// TODO: update the "Enter Message" size to account for the new attachment button
// TODO: update audio player styling
class ChatMessageListAdapter(private val fromUid: String, private val recyclerView: RecyclerView,
                             options: FirestoreRecyclerOptions<ConcordMessage>
) : FirestoreRecyclerAdapter<ConcordMessage, RecyclerView.ViewHolder>(options) {
    companion object {
        const val VIEW_TYPE_MESSAGE_SENT = 0
        const val VIEW_TYPE_MESSAGE_RECEIVED = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        print("Layout id: $viewType")
        return if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.message_me,
                parent, false
            )
            println("SentMessageHolder")
            SentMessageHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.message_other,
                parent, false
            )
            println("ReceivedMessageHolder")
            ReceivedMessageHolder(view)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).fromUid == fromUid) {
            VIEW_TYPE_MESSAGE_SENT
        } else {
            VIEW_TYPE_MESSAGE_RECEIVED
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, model: ConcordMessage) {
        val message: ConcordMessage = getItem(position)
        val messageId: String = snapshots.getSnapshot(position).id
        if (message.fromUid == fromUid) {
            (holder as SentMessageHolder).bind(message, messageId)
        } else {
            (holder as ReceivedMessageHolder).bind(message, messageId)
        }
    }

    override fun onDataChanged() {
        recyclerView.layoutManager?.scrollToPosition(itemCount - 1)
    }
}

class SentMessageHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    var messageText: TextView
    var dateText: TextView
    var timeText: TextView
    var playAudio: ImageButton

    init {
        messageText = itemView.findViewById<View>(R.id.text_gchat_message_me) as TextView
        dateText = itemView.findViewById<View>(R.id.text_gchat_date_me) as TextView
        timeText = itemView.findViewById<View>(R.id.text_gchat_timestamp_me) as TextView
        playAudio = itemView.findViewById(R.id.play_audio_button_me)
    }

    fun bind(message: ConcordMessage, messageId: String) {
        messageText.text = message.text
        // Format time
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("MMM dd")
        val timeString = "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}"
        val dateString = sdf.format(calendar.time)
        timeText.text = timeString
        dateText.text = dateString
        // TODO: possibly add this to the another thread so it doesn't lag
        if (message.audio) {
            // check for audio file locally
            val audioFile = File("${itemView.context.filesDir}/audio/$messageId.3gp")
            println(audioFile.toString())
            if (!audioFile.exists()) {
                val audioDir = File("${itemView.context.filesDir}/audio/")
                if (!audioDir.exists()) {
                    audioDir.mkdir()
                }

                // if audio file is not saved locally fetch from Firebase and save locally
                audioFile.createNewFile()
                val storage = Firebase.storage
                val gsReference = storage.getReferenceFromUrl("$FIREBASE_STORAGE_AUDIO_REPO$messageId.3gp")
                gsReference.getFile(audioFile.toUri()).addOnSuccessListener {
                    println("Audio File Downloaded")
                }
            }
            playAudio.setOnClickListener {
                println("Playing message $messageId")
                // TODO: make it pausable and replayable
                // TODO: there is a loading time that needs to be tracked to stop unpredicatable behaviour
                MediaPlayer().apply {
                    try {
                        setDataSource(audioFile.toString())
                        prepare()
                        start()
                    } catch (e: IOException) {
                        println( "prepare() failed:$e")
                    }
                }
            }
            playAudio.visibility = View.VISIBLE
        } else {
            playAudio.visibility = View.GONE
        }
    }
}

class ReceivedMessageHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    var messageText: TextView
    var nameText: TextView
    var dateText: TextView
    var timeText: TextView
    var playAudio: ImageButton

    init {
        messageText = itemView.findViewById<View>(R.id.text_gchat_message_other) as TextView
        nameText = itemView.findViewById<View>(R.id.text_gchat_user_other) as TextView
        dateText = itemView.findViewById<View>(R.id.text_gchat_date_other) as TextView
        timeText = itemView.findViewById<View>(R.id.text_gchat_timestamp_other) as TextView
        playAudio = itemView.findViewById(R.id.play_audio_button_other)
    }

    fun bind(message: ConcordMessage, messageId: String) {
        messageText.text = message.text
        nameText.text = message.fromName
        // Format time
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("MMM dd")
        val timeString = "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}"
        val dateString = sdf.format(calendar.time)
        timeText.text = timeString
        dateText.text = dateString

        // TODO: possibly add this to the another thread so it doesn't lag
        if (message.audio) {
            val audioFile = File("${itemView.context.filesDir}/$messageId.3gp")
            if (!audioFile.exists()) {
                val audioDir = File("${itemView.context.filesDir}/audio/")
                if (!audioDir.exists()) {
                    audioDir.mkdir()
                }
                // if audio file is not saved locally fetch from Firebase and save locally
                audioFile.createNewFile()
                val storage = Firebase.storage
                val gsReference = storage.getReferenceFromUrl("$FIREBASE_STORAGE_AUDIO_REPO$messageId.3gp")
                gsReference.getFile(audioFile.toUri()).addOnSuccessListener {
                    println("Audio File Downloaded")
                }
            }
            playAudio.setOnClickListener {
                println("Playing message $messageId")
                // TODO: make it pausable and replayable, extend MediaPlayer class
                // TODO: there is loading time associated with it, if it is still downloading
                MediaPlayer().apply {
                    try {
                        setDataSource(audioFile.toString())
                        prepare()
                        start()
                    } catch (e: IOException) {
                        println( "prepare() failed:$e")
                    }
                }
            }
            playAudio.visibility = View.VISIBLE
        } else {
            playAudio.visibility = View.GONE
        }
    }
}