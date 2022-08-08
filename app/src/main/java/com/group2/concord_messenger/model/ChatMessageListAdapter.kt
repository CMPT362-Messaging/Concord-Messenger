package com.group2.concord_messenger.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.group2.concord_messenger.ChatAudioPlayer
import com.group2.concord_messenger.R
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat

const val FIREBASE_STORAGE_AUDIO_REPO = "gs://concord-messenger.appspot.com/audio/"
const val FIREBASE_STORAGE_PHOTO_SHARING_REPO = "gs://concord-messenger.appspot.com/photo-sharing/"

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
            (holder as SentMessageHolder).bind(message, messageId, audioPlayer, position)
        } else {
            (holder as ReceivedMessageHolder).bind(message, messageId, audioPlayer, position)
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder.itemViewType == VIEW_TYPE_MESSAGE_SENT) {
            (holder as SentMessageHolder).unbind()
        } else {
            (holder as ReceivedMessageHolder).unbind()
        }
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
    var image: ImageView
    lateinit var ap:ChatAudioPlayer

    init {
        messageText = itemView.findViewById<View>(R.id.text_gchat_message_me) as TextView
        dateText = itemView.findViewById<View>(R.id.text_gchat_date_me) as TextView
        timeText = itemView.findViewById<View>(R.id.text_gchat_timestamp_me) as TextView
        playAudio = itemView.findViewById(R.id.play_audio_button_me)
        pauseAudio = itemView.findViewById(R.id.pause_audio_button_me)
        audioSeekBar = itemView.findViewById(R.id.audio_seek_me)
        progressBar = itemView.findViewById(R.id.progress_bar_me)
        image = itemView.findViewById(R.id.shared_image)
    }

    fun bind(message: ConcordMessage, messageId: String, ap: ChatAudioPlayer, position:Int) {
        this.ap = ap
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
        } else if (message.image) {
            val imageFile = File("${itemView.context.filesDir}/photo-sharing/${message.imageName}.jpg")
            progressBar.visibility = View.VISIBLE
            val imageDir = File("${itemView.context.filesDir}/photo-sharing/")
            if (!imageDir.exists()) {
                imageDir.mkdir()
            }
            imageFile.createNewFile()
            val storage = Firebase.storage
            val gsReference = storage.getReferenceFromUrl("$FIREBASE_STORAGE_PHOTO_SHARING_REPO${message.imageName}.jpg")
            gsReference.getFile(imageFile.toUri()).addOnSuccessListener {
                println("Image File Downloaded")
                println("${messageId}.jpg")
                val bitmap = getBitmap(itemView.context, imageFile.toUri(), imageFile.path)
                image.setImageBitmap(bitmap)
                image.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
            }
        } else {
            // needed since the view might get recycled and shown again
            playAudio.visibility = View.GONE
            pauseAudio.visibility = View.GONE
            audioSeekBar.visibility = View.GONE
            progressBar.visibility = View.GONE
            image.visibility = View.GONE
        }
    }

    fun unbind() {
        // For now if, to eliminate the error of a audio message playing that gets recycled acting
        // weird, stop the player on unbind
        playAudio.visibility = View.GONE
        pauseAudio.visibility = View.GONE
        audioSeekBar.visibility = View.GONE
        progressBar.visibility = View.GONE
        audioSeekBar.progress = 0
        ap.onUnbind()
    }

    fun getBitmap(context: Context, imgUri: Uri, imgPath: String): Bitmap {
        val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(imgUri))
        val matrix = Matrix()
        var rotate = 0f
        try {
            val exif = ExifInterface(imgPath)
            val orientation: String? = exif.getAttribute(ExifInterface.TAG_ORIENTATION)
            when(orientation) {
                "6" -> rotate = 90f
                "8" -> rotate = 270f
                "3" -> rotate = 180f
            }
        } catch(e: IOException) {
            e.printStackTrace()
        }
        matrix.setRotate(rotate)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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
    var image: ImageView

    init {
        messageText = itemView.findViewById<View>(R.id.text_gchat_message_other) as TextView
        nameText = itemView.findViewById<View>(R.id.text_gchat_user_other) as TextView
        dateText = itemView.findViewById<View>(R.id.text_gchat_date_other) as TextView
        timeText = itemView.findViewById<View>(R.id.text_gchat_timestamp_other) as TextView
        playAudio = itemView.findViewById(R.id.play_audio_button_other)
        pauseAudio = itemView.findViewById(R.id.pause_audio_button_other)
        audioSeekBar = itemView.findViewById(R.id.audio_seek_other)
        progressBar = itemView.findViewById(R.id.progress_bar_other)
        image = itemView.findViewById(R.id.shared_image_other)
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
        } else if (message.image) {
            val imageFile = File("${itemView.context.filesDir}/photo-sharing/${message.imageName}.jpg")
            progressBar.visibility = View.VISIBLE
            val imageDir = File("${itemView.context.filesDir}/photo-sharing/")
            if (!imageDir.exists()) {
                imageDir.mkdir()
            }
            imageFile.createNewFile()
            val storage = Firebase.storage
            val gsReference =
                storage.getReferenceFromUrl("$FIREBASE_STORAGE_PHOTO_SHARING_REPO${message.imageName}.jpg")

            gsReference.getFile(imageFile.toUri()).addOnSuccessListener {
                println("Image File Downloaded")
                println("${messageId}.jpg")
                val bitmap = getBitmap(itemView.context, imageFile.toUri(), imageFile.path)
                image.setImageBitmap(bitmap)
                image.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
            }

        } else {
            // needed since the view might get recycled and shown again
            playAudio.visibility = View.GONE
            pauseAudio.visibility = View.GONE
            audioSeekBar.visibility = View.GONE
            progressBar.visibility = View.GONE
            image.visibility = View.GONE
        }
    }
    fun unbind() {
        // For now if, to eliminate the error of a audio message playing that gets recycled acting
        // weird, stop the player on unbind
        playAudio.visibility = View.GONE
        pauseAudio.visibility = View.GONE
        audioSeekBar.visibility = View.GONE
        progressBar.visibility = View.GONE
        audioSeekBar.progress = 0
    }

    fun getBitmap(context: Context, imgUri: Uri, imgPath: String): Bitmap {
        val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(imgUri))
        val matrix = Matrix()
        var rotate = 0f
        try {
            val exif = ExifInterface(imgPath)
            val orientation: String? = exif.getAttribute(ExifInterface.TAG_ORIENTATION)
            when(orientation) {
                "6" -> rotate = 90f
                "8" -> rotate = 270f
                "3" -> rotate = 180f
            }
        } catch(e: IOException) {
            e.printStackTrace()
        }
        matrix.setRotate(rotate)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}