@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.MainUI

import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import ru.BShakhovsky.Piano_Transcription.OpenGL.Render

class Gesture(private val render: Render) : GestureDetector.SimpleOnGestureListener() {

    override fun onSingleTapUp(event: MotionEvent?): Boolean {
        Log.d("Gestures", "Tap: ${event?.x}, ${event?.y}")
        return true
    }

    override fun onLongPress(event: MotionEvent?) {
        Log.d("Gestures", "Long Press: ${event?.x}, ${event?.y}")
    }

    override fun onScroll(event1: MotionEvent?, event2: MotionEvent?,
                          distanceX: Float, distanceY: Float): Boolean {
        event2?.let {
            assert((event1?.action == MotionEvent.ACTION_DOWN) and
                    (event2.action == MotionEvent.ACTION_MOVE))
            render.move(event2.x + distanceX, event2.x, event2.y + distanceY)
        }
        return true
    }
}