@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription

import android.view.ScaleGestureDetector

class Zoom(private val render : Render) : ScaleGestureDetector.OnScaleGestureListener {

    override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean = true
    override fun onScaleEnd(p0: ScaleGestureDetector?) {}

    override fun onScale(detector: ScaleGestureDetector?): Boolean {
        detector?.let {
            render.zoom(0.1f.coerceAtLeast(detector.scaleFactor.coerceAtMost(5.0f)))
        }
        return true
    }
}