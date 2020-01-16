@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.MainUI

import android.view.GestureDetector
import android.view.MotionEvent
import ru.BShakhovsky.Piano_Transcription.Assert
import ru.BShakhovsky.Piano_Transcription.OpenGL.Render

class Gesture(private val render: Render) : GestureDetector.SimpleOnGestureListener() {

    override fun onSingleTapUp(event: MotionEvent?): Boolean { event?.let {
        Assert.argument(event.action == MotionEvent.ACTION_UP)
        with(event) { render.tap(x, y) } }
        return true
    }

    override fun onLongPress(event: MotionEvent?) { event?.let {
        Assert.argument(event.action == MotionEvent.ACTION_DOWN)
        with(event) { render.longTap(x, y) } }
    }

    override fun onScroll(event1: MotionEvent?, event2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean { event2?.let {
        Assert.argument((event1?.action == MotionEvent.ACTION_DOWN) and (event2.action == MotionEvent.ACTION_MOVE))
        with(event2) { render.move(x + distanceX, x, y + distanceY) } }
        return true
    }
}