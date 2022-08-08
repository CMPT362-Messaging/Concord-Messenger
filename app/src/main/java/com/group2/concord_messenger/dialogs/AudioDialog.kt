package com.group2.concord_messenger.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.group2.concord_messenger.ChatAudioPlayer
import com.group2.concord_messenger.R
import java.io.File
import java.io.IOException

class AudioDialog : DialogFragment() {
    internal lateinit var listener: AudioDialogListener

    private var fileName: String = ""

    private lateinit var recordButton: ImageButton
    private var recorder: MediaRecorder? = null

    private lateinit var playButton: ImageButton
    private lateinit var stopButton: ImageButton
    private lateinit var pauseButton: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var durationText: TextView
    private lateinit var ap: ChatAudioPlayer


    interface AudioDialogListener {
        fun onAudioComplete(dialog: DialogFragment, filename: String)
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
                // Verify that the host activity implements the callback interface
        try {
            // Instantiate the AudioDialogListener so we can send events to the host
            listener = context as AudioDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException((context.toString() +
                    " must implement AudioDialogListener"))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)

            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.audio_dialog, null)
            builder.setTitle("Record Audio Message")
                .setView(view)
                .setPositiveButton("Send Recording",
                        DialogInterface.OnClickListener { dialog, id ->
                            recorder?.release()
                            recorder = null
                            listener.onAudioComplete(this, fileName)
                        })
                .setNeutralButton("Cancel",
                    DialogInterface.OnClickListener { dialog, id ->
                        // Do nothing
                        recorder?.release()
                    })

            val outputDir: File = requireContext().cacheDir
            fileName = "${outputDir}/temp-audio.3gp"

            recordButton = view.findViewById(R.id.record_button)
            playButton = view.findViewById(R.id.play_button)
            stopButton = view.findViewById(R.id.stop_record_button)
            pauseButton = view.findViewById(R.id.pause_button)
            durationText = view.findViewById(R.id.duration_text)
            seekBar = view.findViewById(R.id.audio_seek)
            ap = ChatAudioPlayer()

            recordButton.setOnClickListener {
                recorder = MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setOutputFile(fileName)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    try {
                        prepare()
                    } catch (e: IOException) {
                        println("ERROR")
                        println(e)
                    }
                    recordButton.visibility = View.GONE
                    stopButton.visibility = View.VISIBLE
                    playButton.visibility = View.GONE
                    start()
                }
            }
            playButton.setOnClickListener {
                ap.claim(durationText, playButton, pauseButton,seekBar, 0)
                ap.play(File(fileName))
            }
            pauseButton.setOnClickListener {
                ap.pause()
            }
            stopButton.setOnClickListener {
                recorder?.apply {
                    stop()
                    release()
                }
                recordButton.visibility = View.VISIBLE
                stopButton.visibility = View.GONE
                playButton.visibility = View.VISIBLE
                seekBar.visibility = View.VISIBLE
                durationText.visibility = View.VISIBLE
                // Set duration
                val metaData  = MediaMetadataRetriever()
                metaData.setDataSource(fileName)
                val durationMs = metaData.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationText.text = "%.2f s".format((durationMs?.toInt()?.div(1000.0)))
                metaData.release()
                seekBar.max = durationMs!!.toInt()
                recorder = null
            }

            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onStop() {
        super.onStop()
        recorder?.release()
        recorder = null
        ap.onDestroy()
    }
}
