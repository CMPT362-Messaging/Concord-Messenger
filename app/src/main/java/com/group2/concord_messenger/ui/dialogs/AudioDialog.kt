package com.group2.concord_messenger.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.fragment.app.DialogFragment
import com.group2.concord_messenger.R
import java.io.File
import java.io.IOException

// TODO: there is no pause.
class AudioDialog : DialogFragment() {
    internal lateinit var listener: AudioDialogListener

    private var fileName: String = ""

    private var recordButton: ImageButton? = null
    private var recorder: MediaRecorder? = null

    private var playButton: ImageButton? = null
    private var stopButton: ImageButton? = null
    private var stopPlayButton: ImageButton? = null
    private var player: MediaPlayer? = null


    interface AudioDialogListener {
        fun onComplete(dialog: DialogFragment, filename: String)
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
                // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = context as AudioDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException((context.toString() +
                    " must implement PickPhotoListener"))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)

            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.audio_dialog, null)
            builder.setTitle("Record Audio Message")
                .setView(view)
                .setPositiveButton("Send Recording",
                    DialogInterface.OnClickListener { dialog, id ->
                        recorder?.release()
                        recorder = null
                        player?.release()
                        player = null
                        listener.onComplete(this, fileName)
                    })
                    .setNeutralButton("Cancel",
                        DialogInterface.OnClickListener { dialog, id ->
                            // Do nothing
                            recorder?.release()
                            recorder = null
                            player?.release()
                            player = null
                        })

            val outputDir: File = requireContext().cacheDir
            fileName = "${outputDir}/testaudiofile.3gp"

            recordButton = view?.findViewById(R.id.record_button)
            playButton = view?.findViewById(R.id.play_button)
            stopButton = view?.findViewById(R.id.stop_record_button)
            stopPlayButton = view?.findViewById(R.id.stop_play_button)
            recordButton?.setOnClickListener {
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
                    recordButton?.visibility = View.GONE
                    stopButton?.visibility = View.VISIBLE
                    playButton?.visibility = View.GONE
                    start()
                }
            }
            playButton?.setOnClickListener {
                player = MediaPlayer().apply {
                    try {
                        setDataSource(fileName)
                        prepare()
                        playButton?.visibility = View.GONE
                        stopPlayButton?.visibility = View.VISIBLE
                        start()
                    } catch (e: IOException) {
                        println("prepare() failed")
                    }
                }
                player?.setOnCompletionListener {
                    playButton?.visibility = View.VISIBLE
                    stopPlayButton?.visibility = View.GONE
                }

            }
            stopPlayButton?.setOnClickListener {
                player?.release()
                player = null
                stopPlayButton?.visibility = View.GONE
                playButton?.visibility = View.VISIBLE
            }
            stopButton?.setOnClickListener {
                recorder?.apply {
                    stop()
                    release()
                }
                recordButton?.visibility = View.VISIBLE
                stopButton?.visibility = View.GONE
                playButton?.visibility = View.VISIBLE
                recorder = null
            }

            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
