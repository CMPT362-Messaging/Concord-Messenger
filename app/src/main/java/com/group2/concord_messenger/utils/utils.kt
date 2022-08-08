package com.group2.concord_messenger.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

// "GLOBALS"
const val FIREBASE_STORAGE_AUDIO_REPO = "gs://concord-messenger.appspot.com/audio/"
import java.io.IOException

// https://developer.android.com/training/location/permissions
fun checkPermissions(activity: Activity?) {
    if (Build.VERSION.SDK_INT < 23) return
    if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA), 0)
    }
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

fun checkAudioFilePaths(context: Context) {
    // check that audio directory and file do not exist
    val audioDir = File("${context.filesDir}/audio/")
    if (!audioDir.exists()) {
        audioDir.mkdir()
    }
}