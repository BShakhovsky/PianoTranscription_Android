@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.MainUI

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.view.GestureDetectorCompat
import ru.BShakhovsky.Piano_Transcription.Utils.DebugMode
import ru.BShakhovsky.Piano_Transcription.OpenGL.Render

class Touch(private val render: Render) : View.OnTouchListener {

    private var zoom: ScaleGestureDetector? = null
    private var gesture: GestureDetectorCompat? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View?, event: MotionEvent?): Boolean = true.also {
        DebugMode.assertArgument((view != null) and (event != null))
        with(view as GLSurfaceView) {
            if (zoom == null) zoom = ScaleGestureDetector(context, Zoom(render))
            if (gesture == null) gesture = GestureDetectorCompat(context, Gesture(render))

            DebugMode.assertArgument((zoom != null) and (gesture != null))

            zoom?.onTouchEvent(event)
            gesture?.onTouchEvent(event)

            requestRender()
        }
    }
}