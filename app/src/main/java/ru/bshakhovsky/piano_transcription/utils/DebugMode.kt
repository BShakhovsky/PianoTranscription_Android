package ru.bshakhovsky.piano_transcription.utils

import ru.bshakhovsky.piano_transcription.BuildConfig

object DebugMode {

    var debug: Boolean private set // otherwise condition is always true

    init {
        debug = BuildConfig.DEBUG
    }

    fun assertArgument(b: Boolean, msg: String = ""): Unit = if (debug) require(b) { msg } else Unit
    fun assertState(b: Boolean, msg: String = ""): Unit = if (debug) check(b) { msg } else Unit
}