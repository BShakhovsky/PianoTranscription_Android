@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription

import android.view.ScaleGestureDetector

class Zoom(private val render : Render) : ScaleGestureDetector.OnScaleGestureListener {

    override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean = true
    override fun onScaleEnd(p0: ScaleGestureDetector?) {}

    override fun onScale(detector: ScaleGestureDetector?): Boolean {
//        mScaleFactor *= detector.scaleFactor
//        mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f))
        return true
    }
}