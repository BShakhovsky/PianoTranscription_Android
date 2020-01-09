@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.MainUI

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.view.GestureDetectorCompat
import ru.BShakhovsky.Piano_Transcription.OpenGL.Render

class Touch(private val render : Render) : View.OnTouchListener {

    private var zoom : ScaleGestureDetector? = null
    private var gesture : GestureDetectorCompat? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        with (v as GLSurfaceView) {
            if (zoom == null) zoom = ScaleGestureDetector(context, Zoom(render))
            if (gesture == null) gesture = GestureDetectorCompat(context, Gesture(render))
            zoom!!.onTouchEvent(event); gesture!!.onTouchEvent(event)
            requestRender()
        }
        return true
    }
}