package com.group2.concord_messenger

import android.media.MediaPlayer
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

class ChatAudioPlayer(view: View) {
    private var mp: MediaPlayer? = null
    private val seekBarUpdateJob: CoroutineScope? = null

    fun onStart() {
        mp = MediaPlayer()
    }

    fun onPause() {
        mp?.pause()
    }

    fun onResume() {
        mp?.start()
    }

    fun onComplete() {
        mp?.release()
    }

    fun updateSeekBar() {
        try {
            while (true) {
//                audioProgressBar.progress = mp.currentPosition
//                messageText.text = "${durationMs.toInt() - mp.currentPosition} ms"
//                delay(25)
            }
        } catch(e: IllegalStateException) {
            // bad
        }
    }
}