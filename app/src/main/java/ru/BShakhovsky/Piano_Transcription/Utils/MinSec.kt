@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.Utils

import kotlin.math.roundToInt

object MinSec {

    fun minutes(milSec: Long): Int = totalSecs(milSec) / 60
    fun seconds(milSec: Long): Int = totalSecs(milSec) % 60

    private fun totalSecs(milSec: Long) = (milSec / 1_000f).roundToInt()
}