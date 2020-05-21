package ru.bshakhovsky.piano_transcription.mainUI

import android.view.ScaleGestureDetector
import ru.bshakhovsky.piano_transcription.openGL.Render
import ru.bshakhovsky.piano_transcription.utils.DebugMode

class Zoom(private val render: Render) : ScaleGestureDetector.OnScaleGestureListener {

    override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean =
        true.also { DebugMode.assertArgument(detector != null) }

    override fun onScaleEnd(detector: ScaleGestureDetector?): Unit =
        DebugMode.assertArgument(detector != null)

    override fun onScale(detector: ScaleGestureDetector?): Boolean = true.also {
        DebugMode.assertArgument(detector != null)
        detector?.run { render.zoom(scaleFactor) }
    }
}