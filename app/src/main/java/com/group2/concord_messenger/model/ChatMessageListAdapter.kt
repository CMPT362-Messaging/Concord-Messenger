package com.group2.concord_messenger.model

import android.media.MediaMetadataRetriever
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.group2.concord_messenger.ChatAudioPlayer
import com.group2.concord_messenger.R
import java.io.File
import java.text.SimpleDateFormat

const val FIREBASE_STORAGE_AUDIO_REPO = "gs://concord-messenger.appspot.com/audio/"

// TODO: update the "Enter Message" size to account for the new attachment button
// TODO: update audio player styling
// TODO: when the binding happens stuff gets weird if scrolled
class ChatMessageListAdapter(private val fromUid: String, private val recyclerView: RecyclerView,
                             options: FirestoreRecyclerOptions<ConcordMessage>
) : FirestoreRecyclerAdapter<ConcordMessage, RecyclerView.ViewHolder>(options) {
    companion object {
        const val VIEW_TYPE_MESSAGE_SENT = 0
        const val VIEW_TYPE_MESSAGE_RECEIVED = 1
    }
    private val audioPlayer:ChatAudioPlayer = ChatAudioPlayer()

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
            (holder as SentMessageHolder).bind(message)
        } else {
            (holder as ReceivedMessageHolder).bind(message, messageId, audioPlayer, position)
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
//        (holder as SentMessageHolder).unbind()
//        println("This got rejected")
    }

    override fun stopListening() {
        // stop all audio streams
        audioPlayer.onComplete()
        super.stopListening()
    }

    override fun startListening() {
        audioPlayer.onStart()
        super.startListening()
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
    var pauseAudio: ImageButton
    var audioSeekBar: SeekBar
    var progressBar: ProgressBar

    init {
        messageText = itemView.findViewById<View>(R.id.text_gchat_message_me) as TextView
        dateText = itemView.findViewById<View>(R.id.text_gchat_date_me) as TextView
        timeText = itemView.findViewById<View>(R.id.text_gchat_timestamp_me) as TextView
        playAudio = itemView.findViewById(R.id.play_audio_button_me)
        pauseAudio = itemView.findViewById(R.id.pause_audio_button_me)
        audioSeekBar = itemView.findViewById(R.id.audio_seek_me)
        progressBar = itemView.findViewById(R.id.progress_bar_me)
    }

    fun bind(message: ConcordMessage, messageId: String, ap: ChatAudioPlayer, position:Int) {
        println("HOLDERPOSITION$position")
        messageText.text = message.text
        if (message.createdAt != null) {
            // Format time
            val sdf = SimpleDateFormat("MMM dd")
            val timeFormatter = SimpleDateFormat("HH:mm")
            val timeString = timeFormatter.format(message.createdAt!!)
            val dateString = sdf.format(message.createdAt!!)
            timeText.text = timeString
            dateText.text = dateString
        }
        // TODO: extract this into its own helper function?
        if (message.audio) {
            // if audio file is not saved locally fetch from Firebase and save locally
            val audioFile = File("${itemView.context.filesDir}/audio/$messageId.3gp")
            if (!audioFile.exists() || audioFile.length() <= 0) {
                progressBar.visibility = View.VISIBLE
                val audioDir = File("${itemView.context.filesDir}/audio/")
                if (!audioDir.exists()) {
                    audioDir.mkdir()
                }
                audioFile.createNewFile()
                val storage = Firebase.storage
                val gsReference = storage.getReferenceFromUrl("$FIREBASE_STORAGE_AUDIO_REPO$messageId.3gp")
                gsReference.getFile(audioFile.toUri()).addOnSuccessListener {
                    println("Audio File Downloaded")
                    progressBar.visibility = View.GONE
                    playAudio.visibility = View.VISIBLE
                    audioSeekBar.visibility = View.VISIBLE
                    // Set duration
                    val metaData  = MediaMetadataRetriever()
                    metaData.setDataSource(audioFile.toString())
                    val durationMs = metaData.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    messageText.text = "$durationMs ms"
                    metaData.release()
                    audioSeekBar.max = durationMs!!.toInt()
                }
            } else {
                playAudio.visibility = View.VISIBLE
                audioSeekBar.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                val metaData  = MediaMetadataRetriever()
                metaData.setDataSource(audioFile.toString())
                val durationMs = metaData.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                messageText.text = "$durationMs ms"
                metaData.release()
                audioSeekBar.max = durationMs!!.toInt()
            }
            playAudio.setOnClickListener {
                ap.claim(messageText, playAudio, pauseAudio, audioSeekBar, position)
                ap.play(audioFile)
            }
            pauseAudio.setOnClickListener {
                ap.pause()
            }
        } else {
            // needed since the view might get recycled and shown again
            playAudio.visibility = View.GONE
            pauseAudio.visibility = View.GONE
            audioSeekBar.visibility = View.GONE
            progressBar.visibility = View.GONE
        }
    }

    fun unbind() {
        // possibly needed to fix the view being recycled and playing in a different area
//        playAudio.visibility = View.GONE
//        pauseAudio.visibility = View.GONE
//        audioSeekBar.visibility = View.GONE
//        progressBar.visibility = View.GONE
////        seekBarUpdateJob?.cancel()
//        audioSeekBar.progress = 0
    }
}

class ReceivedMessageHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    var messageText: TextView
    var nameText: TextView
    var dateText: TextView
    var timeText: TextView
    var playAudio: ImageButton
    var pauseAudio: ImageButton
    var audioSeekBar: SeekBar
    var progressBar: ProgressBar

    init {
        messageText = itemView.findViewById<View>(R.id.text_gchat_message_other) as TextView
        nameText = itemView.findViewById<View>(R.id.text_gchat_user_other) as TextView
        dateText = itemView.findViewById<View>(R.id.text_gchat_date_other) as TextView
        timeText = itemView.findViewById<View>(R.id.text_gchat_timestamp_other) as TextView
        playAudio = itemView.findViewById(R.id.play_audio_button_other)
        pauseAudio = itemView.findViewById(R.id.pause_audio_button_other)
        audioSeekBar = itemView.findViewById(R.id.audio_seek_other)
        progressBar = itemView.findViewById(R.id.progress_bar_other)
    }

    fun bind(message: ConcordMessage, messageId: String, ap: ChatAudioPlayer, position: Int) {
        messageText.text = message.text
        nameText.text = message.fromName
        if (message.createdAt != null) {
            // Format time
            val sdf = SimpleDateFormat("MMM dd")
            val timeFormatter = SimpleDateFormat("HH:mm")
            val timeString = timeFormatter.format(message.createdAt!!)
            val dateString = sdf.format(message.createdAt!!)
            timeText.text = timeString
            dateText.text = dateString
        }

        if (message.audio) {
            // if audio file is not saved locally fetch from Firebase and save locally
            val audioFile = File("${itemView.context.filesDir}/audio/$messageId.3gp")
            if (!audioFile.exists() || audioFile.length() <= 0) {
                progressBar.visibility = View.VISIBLE
                val audioDir = File("${itemView.context.filesDir}/audio/")
                if (!audioDir.exists()) {
                    audioDir.mkdir()
                }
                audioFile.createNewFile()
                val storage = Firebase.storage
                val gsReference = storage.getReferenceFromUrl("$FIREBASE_STORAGE_AUDIO_REPO$messageId.3gp")
                gsReference.getFile(audioFile.toUri()).addOnSuccessListener {
                    println("Audio File Downloaded")
                    progressBar.visibility = View.GONE
                    playAudio.visibility = View.VISIBLE
                    audioSeekBar.visibility = View.VISIBLE
                    // Set duration
                    val metaData  = MediaMetadataRetriever()
                    metaData.setDataSource(audioFile.toString())
                    val durationMs = metaData.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    messageText.text = "$durationMs ms"
                    metaData.release()
                    audioSeekBar.max = durationMs!!.toInt()
                }
            } else {
                playAudio.visibility = View.VISIBLE
                audioSeekBar.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                val metaData  = MediaMetadataRetriever()
                metaData.setDataSource(audioFile.toString())
                val durationMs = metaData.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                messageText.text = "$durationMs ms"
                metaData.release()
                audioSeekBar.max = durationMs!!.toInt()
            }
            playAudio.setOnClickListener {
                ap.claim(messageText, playAudio, pauseAudio, audioSeekBar, position)
                ap.play(audioFile)
            }
            pauseAudio.setOnClickListener {
                ap.pause()
            }
        } else {
            // needed since the view might get recycled and shown again
            playAudio.visibility = View.GONE
            pauseAudio.visibility = View.GONE
            audioSeekBar.visibility = View.GONE
            progressBar.visibility = View.GONE
        }
    }
}