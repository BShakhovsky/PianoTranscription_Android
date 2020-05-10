@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.MainUI

import android.view.GestureDetector
import android.view.MotionEvent
import ru.BShakhovsky.Piano_Transcription.Utils.DebugMode
import ru.BShakhovsky.Piano_Transcription.OpenGL.Render

class Gesture(private val render: Render) : GestureDetector.SimpleOnGestureListener() {

    override fun onSingleTapUp(event: MotionEvent?): Boolean = super.onSingleTapUp(event).also {
        DebugMode.assertArgument(event != null)
        event?.run {
            DebugMode.assertArgument(action == MotionEvent.ACTION_UP)
            render.tap(x, y)
        }
    }

    override fun onLongPress(event: MotionEvent?) {
        DebugMode.assertArgument(event != null)
        event?.run {
            DebugMode.assertArgument(action == MotionEvent.ACTION_DOWN)
            render.longTap(x, y)
        }
    }

    override fun onScroll(
        event1: MotionEvent?, event2: MotionEvent?, distanceX: Float, distanceY: Float
    ): Boolean = super.onScroll(event1, event2, distanceX, distanceY).also {
        DebugMode.assertArgument((event1 != null) and (event2 != null))
        event2?.run {
            DebugMode.assertArgument(
                (event1?.action == MotionEvent.ACTION_DOWN) and (action == MotionEvent.ACTION_MOVE)
            )
            render.move(x + distanceX, x, y + distanceY)
        }
    }
}