@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription

object Assert {
    fun argument(b: Boolean, msg: String = "") = if (BuildConfig.DEBUG) require(b){msg} else Unit
    fun    state(b: Boolean, msg: String = "") = if (BuildConfig.DEBUG)   check(b){msg} else Unit
}