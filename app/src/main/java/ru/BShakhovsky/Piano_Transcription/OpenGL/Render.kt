@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL

import android.content.Context
import android.graphics.PointF
import android.opengl.GLES32
import android.opengl.GLSurfaceView
import android.opengl.GLU
import android.os.SystemClock
import ru.BShakhovsky.Piano_Transcription.Assert
import ru.BShakhovsky.Piano_Transcription.OpenGL.Geometry.Geometry
import ru.BShakhovsky.Piano_Transcription.OpenGL.Geometry.Model
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Render(private val context: Context) : GLSurfaceView.Renderer {

    private lateinit var model: Model; private val sound = Sound(context)

    private var width = 0; private var height = 0
    private var x = Geometry.overallLen / 2; private var yz = Geometry.whiteLen
    private var zoomOut = true; private var zoomTime = 0L; private var prevTime = 0L

    override fun onSurfaceCreated(glUnused: GL10?, config: EGLConfig?) {
        GLES32.glEnable(GLES32.GL_DEPTH_TEST)
        GLES32.glEnable(GLES32.GL_CULL_FACE)
        GLES32.glBlendFunc(GLES32.GL_SRC_ALPHA, GLES32.GL_ONE_MINUS_SRC_ALPHA)
        model = Model(context)
    }
    override fun onSurfaceChanged(glUnused: GL10?, newWidth: Int, newHeight: Int) {
        width  = newWidth; height = newHeight
        with(model) { with(mats) { project(width, height); viewProject(x, yz, yz) }
            textures.resizeReflection(width, height) }
    }
    override fun onDrawFrame(glUnused: GL10?) {
        if (zoomOut and (SystemClock.uptimeMillis() > zoomTime)) {
            if (zoomTime != 0L) zoom(1 - .0006f * (SystemClock.uptimeMillis() - zoomTime))
            zoomTime = SystemClock.uptimeMillis()
        }
        with(model) { draw(width, height)
            geom.keys.forEach { it.rotate(SystemClock.uptimeMillis() - prevTime) }
            prevTime = SystemClock.uptimeMillis()
        }
    }

    fun move(xStart: Float, xEnd: Float, yStart: Float) {
        if (((xStart < xEnd) and (winX(0f).x > 0)) or
            ((xStart > xEnd) and (winX(Geometry.overallLen).x < width))) return
        moveUnlimited(xStart, xEnd, yStart)
        fit()
    }
    fun zoom(scale: Float) { (winX(0f) to winX(Geometry.overallLen)).also { (winLeft, winRight) ->
        if ((scale > 1) or (winLeft.x < 0) or (winRight.x > width)) zoomUnlimited(scale)
        if (scale < 1) fit()
    } }

    private fun fit() { (winX(0f) to winX(Geometry.overallLen)).also { (winLeft, winRight) -> when {
        (winLeft.x > 0) and (winRight.x < width) -> {
            Assert.state(winLeft.y == winRight.y)
            x  = Geometry.overallLen / 2; yz = Geometry.overallLen * .425f // 1 / 2 / sqrt(2) = 0.354
            model.mats.viewProject(x, yz, yz)
            zoomOut = false
        }
        winLeft.x  > 0     -> moveUnlimited(winLeft.x, 0f, winLeft.y)
        winRight.x < width -> moveUnlimited(winRight.x, width.toFloat(), winRight.y)
    } } }
    private fun moveUnlimited(xStart: Float, xEnd: Float, yStart: Float) {
        x = (x + worldXZ(xStart, yStart).x - worldXZ(xEnd, yStart).x).coerceAtLeast(0f).coerceAtMost(Geometry.overallLen)
        model.mats.viewProject(x, yz, yz)
    }
    private fun zoomUnlimited(scale: Float) {
        yz = (yz / scale).coerceAtLeast(Geometry.whiteLen).coerceAtMost(Geometry.overallLen)
        model.mats.viewProject(x, yz, yz)
    }

    private fun winX(worldX: Float): PointF { floatArrayOf(0f, 0f, 0f).also { GLU.gluProject(
        worldX, Geometry.whiteWid, Geometry.whiteLen, model.mats.view, 0, model.mats.projection, 0,
        intArrayOf(0, 0, width, height), 0, it, 0)
        return PointF(it[0], it[1])
    } }
    private fun worldXZ(xWin: Float, yWin: Float): PointF {
        fun worldCords(zDepth: Float) = floatArrayOf(0f, 0f, 0f, 0f).also { obj -> with(model.mats) {
            GLU.gluUnProject(xWin, height - yWin, zDepth, view, 0, projection,
                0, intArrayOf(0, 0, width, height), 0, obj, 0) }
            for (i in 0 until obj.lastIndex) obj[i] /= obj.last()
        }
        worldCords(0f).also { (xNear, yNear, zNear) -> worldCords(1f).also { (xFar, yFar, zFar) ->
            Assert.state((yNear > 0) and (zNear > 0))
            (zFar + (Geometry.whiteWid - yFar) * (zNear - zFar) / (yNear - yFar)).also { z ->
                return if (yWin > height / 2) // (zFar - yFar * (zNear - zFar) / (yNear - yFar) > 0)
                    if (z > Geometry.blackLen) PointF(xFar + (Geometry.whiteWid - yFar) * (xNear - xFar) / (yNear - yFar), z)
                    else PointF(xFar + (Geometry.whiteWid + Geometry.blackWid - yFar) * (xNear - xFar) / (yNear - yFar),
                        zFar + (Geometry.whiteWid + Geometry.blackWid - yFar) * (zNear - zFar) / (yNear - yFar))
                else PointF(xFar - zFar * (xNear - xFar) / (zNear - zFar), 0f) } } }
    }
    private fun winToKey(xWin: Float, yWin: Float): Int { worldXZ(xWin, yWin).also { xz ->
        if ((xz.y == 0f) or (xz.x !in 0f..Geometry.overallLen) or (xz.y !in 0f..Geometry.whiteLen)) return -1
        ((xz.x / Geometry.whiteWid + 5).toInt() / 7).also { octave ->
            if (xz.y > Geometry.blackLen) { return when { xz.x < Geometry.whiteWid -> 0; xz.x < Geometry.whiteWid * 2 -> 2
                else -> 3 + (octave - 1) * 12 + when ((xz.x / Geometry.whiteWid - 2).toInt() % 7) {
                    0 -> 0 1 -> 2 2 -> 4 3 -> 5 4 -> 7 5 -> 9 6 -> 11 else -> { Assert.argument(false); -1 } } }
            } else (Geometry.whiteWid / 2).also { blackW -> var cord = xz.x; when {
                cord - Geometry.whiteWid     < -blackW             -> return -1//0
                cord - Geometry.whiteWid     <  blackW             -> return  1
                cord - Geometry.whiteWid     < Geometry.whiteWid   -> return -1//2
                cord + Geometry.whiteWid     > Geometry.overallLen -> return 87
              else -> { cord = (cord - Geometry.whiteWid * 2) % (Geometry.whiteWid * 7) - Geometry.whiteWid; when {
                cord                         < -blackW             -> return -1//(octave - 1) * 12 + 3
                cord                         <  blackW             -> return     (octave - 1) * 12 + 4
                cord - Geometry.whiteWid     < -blackW             -> return -1//(octave - 1) * 12 + 5
                cord - Geometry.whiteWid     <  blackW             -> return     (octave - 1) * 12 + 6
                cord - Geometry.whiteWid     <  Geometry.whiteWid  -> return -1//(octave - 1) * 12 + 7
                cord - Geometry.whiteWid * 3 < -blackW             -> return -1//(octave - 1) * 12 + 8
                cord - Geometry.whiteWid * 3 <  blackW             -> return     (octave - 1) * 12 + 9
                cord - Geometry.whiteWid * 4 < -blackW             -> return -1//(octave - 1) * 12 + 10
                cord - Geometry.whiteWid * 4 <  blackW             -> return     (octave - 1) * 12 + 11
                cord - Geometry.whiteWid * 5 < -blackW             -> return -1//(octave - 1) * 12 + 12
                cord - Geometry.whiteWid * 5 <  blackW             -> return     (octave - 1) * 12 + 13
                cord - Geometry.whiteWid * 5 <  Geometry.whiteWid  -> return -1//(octave - 1) * 12 + 14
            } } } } }
        Assert.state(false)
        return -1
    } }

    fun     tap(xWin: Float, yWin: Float) { winToKey(xWin, yWin).also { if (it != -1) {
             model.geom.keys[it]   .isTapped = true;          sound.play(it) } } }
    fun longTap(xWin: Float, yWin: Float) { winToKey(xWin, yWin).also { if (it != -1) {
        with(model.geom.keys[it]) { isPressed = !isPressed }; sound.play(it) } } }

//  fun   pressKey(note: Int, velocity: Float) { if (check(note)) { model.geom.keys[note]       .isPressed = true;  sound.play(note, velocity) } }
//  fun releaseKey(note: Int)                  { if (check(note))   model.geom.keys[note]       .isPressed = false; sound.stop(note) }
//  fun releaseAllKeys()                       { if (check())       model.geom.keys.forEach { it.isPressed = false } }

//    private fun check(note: Int): Boolean { Assert.argument(note in 0..87); return ::model.isInitialized }
}