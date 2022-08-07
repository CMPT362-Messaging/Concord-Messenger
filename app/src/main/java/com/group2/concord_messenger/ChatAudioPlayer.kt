package com.group2.concord_messenger

import android.media.MediaPlayer
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import kotlinx.coroutines.*
import java.io.File

class ChatAudioPlayer: SeekBar.OnSeekBarChangeListener {
    private var mp: MediaPlayer = MediaPlayer()
    private var seekBarUpdateJob: Job? = null
    private var claimed: Boolean = false

    lateinit var messageText: TextView
    lateinit var playAudio: ImageButton
    lateinit var pauseAudio: ImageButton
    lateinit var audioSeekBar: SeekBar
    private var duration: Int = 0
    private var position: Int = 0

    fun claim(text: TextView, play:ImageButton, pause:ImageButton, seek:SeekBar, pos:Int) {
        // if the same position just resume
        if (claimed && pos != position) {
            onComplete()
        }
        mp.reset()
        messageText = text
        playAudio = play
        pauseAudio = pause
        audioSeekBar = seek
        audioSeekBar.setOnSeekBarChangeListener(this)
        claimed = true
        position = pos
    }

    fun play(file: File) {
        println("PLAY")
        if (claimed) {
            mp.setDataSource(file.toString())
            mp.prepare()
            mp.seekTo(audioSeekBar.progress)
            println(audioSeekBar.progress)
            mp.start()
            duration = mp.duration
            seekBarUpdateJob = CoroutineScope(Dispatchers.Main).launch {
                try {
                    while (true) {
                        yield()
                        audioSeekBar.progress = mp.currentPosition
                        messageText.text = "${duration.minus(mp.currentPosition)} ms"
                        delay(25)
                    }
                } catch (e: IllegalStateException) {
                    // bad
                }
            }
            mp.setOnCompletionListener {
                onComplete()
            }
            pauseAudio.visibility = View.VISIBLE
            playAudio.visibility = View.GONE
        }
    }

    fun pause() {
        println("PAUSE")
        mp.pause()
        if (claimed) {
            pauseAudio.visibility = View.GONE
            playAudio.visibility = View.VISIBLE
            seekBarUpdateJob?.cancel()
        }
    }

    fun onStart() {
        mp = MediaPlayer()
    }

    fun onComplete() {
        println("ONCOMPLETE")
        mp.stop()
        if (claimed) {
            playAudio.visibility = View.VISIBLE
            pauseAudio.visibility = View.GONE
            audioSeekBar.progress = 0
            seekBarUpdateJob?.cancel()
            messageText.text = "$duration ms"
            audioSeekBar.setOnSeekBarChangeListener(null)
        }
    }

    fun onUnbind() {
        onComplete()
    }

    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
        if (p2) {
            messageText.text = "${p0?.max?.minus(p1)} ms"
        }
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {
        if (claimed) {
            pause()
        }
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
        mp.seekTo(p0?.progress!!)
    }
}