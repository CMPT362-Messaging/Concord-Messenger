package com.group2.concord_messenger

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.group2.concord_messenger.dialogs.AudioDialog
import java.io.IOException


class MainActivity : AppCompatActivity(), AudioDialog.AudioDialogListener {
    private lateinit var attachmentButton: Button
    private lateinit var playButton: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        attachmentButton = findViewById(R.id.attachment_button)
        playButton = findViewById(R.id.play_demo_button)

        attachmentButton.setOnClickListener {
            val dur = AudioDialog()
            dur.show(supportFragmentManager, "audioPicker")
            checkPermissions(this)
        }
    }

    // https://developer.android.com/training/location/permissions
    private fun checkPermissions(activity: Activity?) {
        if (Build.VERSION.SDK_INT < 23) return
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO), 0)
        }
    }

    override fun onComplete(dialog: DialogFragment, fileName: String) {
        playButton.visibility = View.VISIBLE
        playButton.setOnClickListener {
            MediaPlayer().apply {
                try {
                    setDataSource(fileName)
                    prepare()
                    start()
                } catch (e: IOException) {
                    println( "prepare() failed")
                }
            }
        }
    }
}