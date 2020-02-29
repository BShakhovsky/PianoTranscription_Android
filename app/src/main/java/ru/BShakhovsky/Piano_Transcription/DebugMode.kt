@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription

object DebugMode {

    var debug: Boolean private set // otherwise condition is always true

    init {
        debug = true // BuildConfig.DEBUG
    }

    fun assertArgument(b: Boolean, msg: String = ""): Unit =
        if (debug) require(b) { msg } else Unit

    fun assertState(b: Boolean, msg: String = ""): Unit =
        if (debug) check(b) { msg } else Unit
}