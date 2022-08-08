package com.group2.concord_messenger.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

// "GLOBALS"
const val FIREBASE_STORAGE_AUDIO_REPO = "gs://concord-messenger.appspot.com/audio/"

// https://developer.android.com/training/location/permissions
fun checkPermissions(activity: Activity?) {
    if (Build.VERSION.SDK_INT < 23) return
    if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA), 0)
    }
}

fun checkAudioFilePaths(context: Context) {
    // check that audio directory and file do not exist
    val audioDir = File("${context.filesDir}/audio/")
    if (!audioDir.exists()) {
        audioDir.mkdir()
    }
}