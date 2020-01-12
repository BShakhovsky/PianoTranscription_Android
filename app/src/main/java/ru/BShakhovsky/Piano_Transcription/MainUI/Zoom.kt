@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.MainUI

import android.view.ScaleGestureDetector
import ru.BShakhovsky.Piano_Transcription.OpenGL.Render

class Zoom(private val render: Render) : ScaleGestureDetector.OnScaleGestureListener {

    override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean = true
    override fun onScaleEnd(p0: ScaleGestureDetector?) {}

    override fun onScale(detector: ScaleGestureDetector?): Boolean {
        detector?.let { render.zoom(detector.scaleFactor) }
        return true
    }
}