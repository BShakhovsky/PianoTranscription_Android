@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.MainUI

import android.view.ScaleGestureDetector
import ru.BShakhovsky.Piano_Transcription.Utils.DebugMode
import ru.BShakhovsky.Piano_Transcription.OpenGL.Render

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