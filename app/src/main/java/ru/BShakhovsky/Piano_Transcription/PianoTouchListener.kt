@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription

import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.View
import kotlin.math.sign

class PianoTouchListener(private val render: PianoRenderer) : View.OnTouchListener {

    private var prevX = 0f
    private var prevY = 0f

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        v?.let {
            when (event?.action) {
                MotionEvent.ACTION_UP -> v.performClick()
                MotionEvent.ACTION_MOVE -> {
                    render.angle += ((event.x - prevX) * sign(v.height / 2 - event.y) +
                            (event.y - prevY) * sign(event.x - v.width / 2)) * 180 / 320
                    (v as GLSurfaceView).requestRender()
                }
                else -> {}
            }
        }
        event?.let {
            prevX = event.x
            prevY = event.y
        }
        return true
    }
}