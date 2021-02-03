package ru.bshakhovsky.piano_transcription.utils

import androidx.annotation.CheckResult
import kotlin.math.roundToInt

object MinSec {

    fun minutes(milSec: Long): Int = totalSecs(milSec) / 60
    fun seconds(milSec: Long): Int = totalSecs(milSec) % 60

    @CheckResult
    private fun totalSecs(milSec: Long) = (milSec / 1_000f).roundToInt()
}