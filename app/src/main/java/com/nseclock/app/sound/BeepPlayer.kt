package com.nseclock.app.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.nseclock.app.model.BeepType

/** Plays a synthesized beep on the ALARM stream so it sounds through silent + DND. */
object BeepPlayer {

    fun play(type: BeepType) {
        val data = ToneSynth.pcm16(type)
        val sizeBytes = data.size * 2

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(ToneSynth.SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val track = AudioTrack(
            attrs, format, sizeBytes,
            AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        track.write(data, 0, data.size)
        track.setNotificationMarkerPosition(data.size)
        track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(t: AudioTrack?) {
                runCatching { t?.stop() }
                runCatching { t?.release() }
            }
            override fun onPeriodicNotification(t: AudioTrack?) {}
        })
        track.play()
    }
}
