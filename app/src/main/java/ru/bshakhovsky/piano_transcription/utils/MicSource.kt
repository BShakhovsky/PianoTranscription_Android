package ru.bshakhovsky.piano_transcription.utils

import android.media.MediaRecorder

/* https://stackoverflow.com/a/60636870
https://stackoverflow.com/questions/60338135
    /recorded-audio-using-mediarecorder-audiosource-voice-communication-is-empty-on-s
    /60636870#60636870
I ended up using MediaRecorder.AudioSource.VOICE_RECOGNITION instead of
    MediaRecorder.AudioSource.VOICE_COMMUNICATION on all android versions.
We ended up taking samples on 15+ different devices and found out that
    MediaRecorder.AudioSource.VOICE_RECOGNITION works best with the majority devices
including high-end and mid-range phones. */

object MicSource {
    const val micSource: Int = MediaRecorder.AudioSource.VOICE_RECOGNITION
}