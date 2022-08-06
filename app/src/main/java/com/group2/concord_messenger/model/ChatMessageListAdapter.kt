package com.group2.concord_messenger.model

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
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
import com.group2.concord_messenger.R
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

const val FIREBASE_STORAGE_AUDIO_REPO = "gs://concord-messenger.appspot.com/audio/"

// TODO: update the "Enter Message" size to account for the new attachment button
// TODO: update audio player styling
// TODO: release the media player
// TODO: when the binding happens stuff gets weird if scrolled
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

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        // TODO: temporarily just stop all playback
        (holder as SentMessageHolder).unbind()
        println("This got rejected")
    }

    override fun stopListening() {
        // stop all audio streams
        super.stopListening()
    }
//
//    override fun startListening() {
//        super.startListening()
//    }


    override fun onDataChanged() {
        recyclerView.layoutManager?.scrollToPosition(itemCount - 1)
    }
}

class SentMessageHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView), SeekBar.OnSeekBarChangeListener {
    var messageText: TextView
    var dateText: TextView
    var timeText: TextView
    var playAudio: ImageButton
    var pauseAudio: ImageButton
    var audioSeekBar: SeekBar
    var progressBar: ProgressBar
    var mp: MediaPlayer? = null
    var seekBarUpdateJob: Job? = null

    init {
        messageText = itemView.findViewById<View>(R.id.text_gchat_message_me) as TextView
        dateText = itemView.findViewById<View>(R.id.text_gchat_date_me) as TextView
        timeText = itemView.findViewById<View>(R.id.text_gchat_timestamp_me) as TextView
        playAudio = itemView.findViewById(R.id.play_audio_button_me)
        pauseAudio = itemView.findViewById(R.id.pause_audio_button_me)
        audioSeekBar = itemView.findViewById(R.id.audio_seek_me)
        progressBar = itemView.findViewById(R.id.progress_bar_me)
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

        // TODO: extract this into its own class
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
                }.addOnFailureListener {
                    println("THis is the total length ${audioFile.length()}$it")
                }
            } else {
                playAudio.visibility = View.VISIBLE
                audioSeekBar.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                // Set duration
                audioSeekBar.setOnSeekBarChangeListener(this)
                val metaData  = MediaMetadataRetriever()
                println(audioFile.exists() && audioFile.length() > 0)
                metaData.setDataSource(audioFile.toString())
                val durationMs = metaData.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                messageText.text = "$durationMs ms"
                metaData.release()
                audioSeekBar.max = durationMs!!.toInt()
            }
            playAudio.setOnClickListener {
                println("Audio message $messageId")
                playAudio.visibility = View.GONE
                pauseAudio.visibility = View.VISIBLE
                if (mp == null) {
                    mp = MediaPlayer()
                    mp?.setDataSource(audioFile.toString())
                    mp?.prepare()
                    mp?.seekTo(audioSeekBar.progress)
                } else {
                    mp?.seekTo(audioSeekBar.progress)
                    mp?.start()
                    seekBarUpdateJob = CoroutineScope(Dispatchers.Main).launch {
                        try {
                            while (true) {
                                yield()
                                audioSeekBar.progress = mp?.currentPosition!!
                                messageText.text = "${mp?.duration?.minus(mp?.currentPosition!!)} ms"
                                delay(25)
                            }
                        } catch(e: IllegalStateException) {
                            // bad
                        }
                    }
                    return@setOnClickListener
                }

                try {
                    mp?.start()
                    // handle updating the progress bar
                    seekBarUpdateJob = CoroutineScope(Dispatchers.Main).launch {
                        try {
                            while (true) {
                                yield()
                                audioSeekBar.progress = mp?.currentPosition!!
                                messageText.text = "${mp?.duration?.minus(mp?.currentPosition!!)} ms"
                                delay(25)
                            }
                        } catch(e: IllegalStateException) {
                            // bad
                        }
                    }
                    mp?.setOnCompletionListener {
                        messageText.text = "${mp?.duration} ms"
                        seekBarUpdateJob?.cancel()
                        audioSeekBar.progress = 0
                        it.release()
                        mp = null
                        playAudio.visibility = View.VISIBLE
                        pauseAudio.visibility = View.GONE
                    }
                    pauseAudio.setOnClickListener {
                        mp?.pause()
                        playAudio.visibility = View.VISIBLE
                        pauseAudio.visibility = View.GONE
                        if (seekBarUpdateJob?.isActive!!) {
                            seekBarUpdateJob?.cancel()
                        }
                    }

                } catch (e: IOException) {
                    println( "prepare() failed:$e")
                    mp?.release()
                    mp = null
                }
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
        if (mp!=null) {
            mp?.release()
            mp = null
            playAudio.visibility = View.GONE
            pauseAudio.visibility = View.GONE
            audioSeekBar.visibility = View.GONE
            progressBar.visibility = View.GONE
            seekBarUpdateJob?.cancel()
            audioSeekBar.progress = 0
        }
    }

    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
        if (p2) {
            messageText.text = "${p0?.max?.minus(p1)} ms"
        }
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {
        // Pause playback
        pauseAudio.callOnClick()
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
        mp?.seekTo(p0?.progress!!)
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
            val audioFile = File("${itemView.context.filesDir}/audio/$messageId.3gp")
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
                        println(audioFile.toString())
                        setDataSource(audioFile.toString())
                        prepare()
                        start()
                    } catch (e: IOException) {
                        println( "prepare() failed:$e")
//                        MediaPlayer().release()
                        // Download the file
                    }
                }
            }
            playAudio.visibility = View.VISIBLE
        } else {
            playAudio.visibility = View.GONE
        }
    }

    fun unbind() {
        // TODO: if audio is playing don't let it die/start updating random stuff
    }
}