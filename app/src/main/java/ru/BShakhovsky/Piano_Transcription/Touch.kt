@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.view.GestureDetectorCompat

class Touch(render : Render, context : Context) : View.OnTouchListener {

    private val zoom = ScaleGestureDetector(context, Zoom(render))
    private val gesture = GestureDetectorCompat(context, Gesture(render))

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        zoom.onTouchEvent(event)
        gesture.onTouchEvent(event)
        (v as GLSurfaceView).requestRender()
        return true
    }
}