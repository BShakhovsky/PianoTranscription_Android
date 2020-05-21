package ru.bshakhovsky.piano_transcription.openGL.geometry

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import kotlin.math.atan

class Key(note: Int) {

    @Suppress("Reformat") companion object {
        enum class KeyType { WHITE_LEFT, WHITE_RIGHT, WHITE_MID, WHITE_FULL, BLACK }

        private val aliceBlue   = floatArrayOf(240 / 255f, 248 / 255f, 255 / 255f, 1f)
        private val lightSilver = floatArrayOf(211 / 255f, 211 / 255f, 211 / 255f, 1f)
        private val slateGray   = floatArrayOf(112 / 255f, 128 / 255f, 144 / 255f, 1f)
        private val black       = floatArrayOf(.15f, .15f, .15f, 1f)
    }

    @Suppress("Reformat") val key: KeyType = when (note) {
            0               ->  KeyType.WHITE_LEFT
            87              ->  KeyType.WHITE_FULL
        else -> when ((note + 9) % 12) {
            0, 5            ->  KeyType.WHITE_LEFT
            4, 11           ->  KeyType.WHITE_RIGHT
            2, 7, 9         ->  KeyType.WHITE_MID
            1, 3, 6, 8, 10  ->  KeyType.BLACK
            else            ->  KeyType.WHITE_FULL.also { DebugMode.assertArgument(false) }
        }
    }

    @Suppress("Reformat") val offset: Float = when (note) {
            0, 1    ->  0
            2       ->  1
        else -> when ((note - 3) % 12) {
            0, 1    ->  2
            2, 3    ->  3
            4       ->  4
            5, 6    ->  5
            7, 8    ->  6
            9, 10   ->  7
            11      ->  8
            else    ->  0.also { DebugMode.assertArgument(false) }
        } + (note - 3) / 12 * 7
    } * Geometry.whiteWid

    var isPressed: Boolean = false
    var isTapped: Boolean = false
    var angle: Float = 0f

    private val maxAngle = if (key == KeyType.BLACK)
        Math.toDegrees(atan(1.1 * Geometry.blackWid / Geometry.blackLen)).toFloat()
    else Math.toDegrees(atan(.6 * Geometry.whiteWid / Geometry.whiteLen)).toFloat()

    fun color(): FloatArray =
        if (isPressed or isTapped) if (key == KeyType.BLACK) slateGray else lightSilver
        else if (key == KeyType.BLACK) black
        else aliceBlue

    fun rotate(deltaTime: Long): Unit = @Suppress("Reformat") when {
            isPressed   -> when {
                isTapped -> when {
                    angle >  0          -> decAngle(deltaTime)
                    else                -> isTapped = false
                }
                    angle <  maxAngle   -> incAngle(deltaTime)
                    else                -> {}
            }
            else        -> when {
                isTapped -> when {
                    angle <  maxAngle   -> incAngle(deltaTime)
                    else                -> isTapped = false
                }
                    angle >  0          -> decAngle(deltaTime)
                    else                -> {}
            }
        }

    private fun incAngle(deltaTime: Long) {
        angle = (angle + .15f * deltaTime).coerceAtMost(maxAngle)
    }

    private fun decAngle(deltaTime: Long) {
        angle = (angle - .015f * deltaTime).coerceAtLeast(0f)
    }
}