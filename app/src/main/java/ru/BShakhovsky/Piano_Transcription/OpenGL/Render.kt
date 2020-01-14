@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL

import android.content.Context
import android.graphics.PointF
import android.opengl.GLES32
import android.opengl.GLSurfaceView
import android.opengl.GLU
import android.os.SystemClock
import ru.BShakhovsky.Piano_Transcription.OpenGL.Geometry.Geometry
import ru.BShakhovsky.Piano_Transcription.OpenGL.Geometry.Model
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Render(private val context: Context) : GLSurfaceView.Renderer {

    private var width = 0; private var height = 0

    private lateinit var model: Model

    private var x = Geometry.overallLen / 2; private var yz = Geometry.whiteLen
    private var zoomOut = true; private var zoomTime = 0L

    override fun onSurfaceCreated(glUnused: GL10?, config: EGLConfig?) {
        GLES32.glEnable(GLES32.GL_DEPTH_TEST)
        GLES32.glEnable(GLES32.GL_CULL_FACE)
        GLES32.glBlendFunc(GLES32.GL_SRC_ALPHA, GLES32.GL_ONE_MINUS_SRC_ALPHA)
        model = Model(context)
    }
    override fun onSurfaceChanged(glUnused: GL10?, newWidth: Int, newHeight: Int) {
        width  = newWidth; height = newHeight
        model.mats.project(width, height)
        model.mats.viewProject(x, yz, yz)
        model.textures.resizeReflection(width, height)
    }
    override fun onDrawFrame(glUnused: GL10?) {
        if (zoomOut and (SystemClock.uptimeMillis() > zoomTime)) {
            if (zoomTime != 0L) zoom(1 - .0006f * (SystemClock.uptimeMillis() - zoomTime))
            zoomTime = SystemClock.uptimeMillis()
        }
        model.draw(width, height)
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
            assert(winLeft.y == winRight.y)
            x  = Geometry.overallLen / 2; yz = Geometry.overallLen * .425f // 1 / 2 / sqrt(2) = 0.354
            model.mats.viewProject(x, yz, yz)
            zoomOut = false
        }
        winLeft.x  > 0     -> moveUnlimited(winLeft.x, 0f, winLeft.y)
        winRight.x < width -> moveUnlimited(winRight.x, width.toFloat(), winRight.y)
    } } }
    private fun moveUnlimited(xStart: Float, xEnd: Float, yStart: Float) {
        fun worldX(xWin: Float, yWin: Float): Float {
            fun worldCords(zDepth: Float) = floatArrayOf(0f, 0f, 0f, 0f).also { obj ->
                GLU.gluUnProject(xWin, height - yWin, zDepth, model.mats.view, 0, model.mats.projection,
                    0, intArrayOf(0, 0, width, height), 0, obj, 0)
                for (i in 0 until obj.lastIndex) obj[i] /= obj.last()
            }
            worldCords(0f).also { (xNear, yNear, zNear) -> worldCords(1f).also { (xFar, yFar, zFar) ->
                assert((yNear > 0) and (zNear > 0))
                return if (yWin > height / 2) // (zFar - yFar * (zNear - zFar) / (yNear - yFar) > 0)
                     (xFar - yFar * (xNear - xFar) / (yNear - yFar))
                else (xFar - zFar * (xNear - xFar) / (zNear - zFar))
            } }
        }
        x = (x + worldX(xStart, yStart) - worldX(xEnd, yStart)).coerceAtLeast(0f).coerceAtMost(Geometry.overallLen)
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
}