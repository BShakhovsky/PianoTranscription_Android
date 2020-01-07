@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription

import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.opengl.GLU
import android.opengl.Matrix
import android.os.SystemClock
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Render : GLSurfaceView.Renderer {

    private lateinit var model : Geometry

    private var x = Geometry.overallLen / 2
    private var yz = Geometry.whiteLen
    private var width  = 0
    private var height = 0

    private var zoomOut = true
    private var zoomTime = 0L

    private val view           = FloatArray(16)
    private val projection     = FloatArray(16)
    private val viewProjection = FloatArray(16)

    override fun onSurfaceCreated(glUnused: GL10?, config: EGLConfig?) {
        GLES31.glClearColor(70 / 255f, 130 / 255f, 180 / 255f, 1f)  // Steel blue
        GLES31.glEnable(GLES31.GL_DEPTH_TEST)
        model = Geometry()
    }
    override fun onSurfaceChanged(glUnused: GL10?, newWidth: Int, newHeight: Int) {
        width = newWidth
        height = newHeight
        GLES31.glViewport(0, 0, width, height)
        (height.toFloat() / width.toFloat()).also { ratio -> Matrix.frustumM(projection,
            0, -1f, 1f, -ratio, ratio, 1f, Geometry.overallLen) }
        calcViewProjection()
    }
    override fun onDrawFrame(glUnused: GL10?) {
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT or GLES31.GL_DEPTH_BUFFER_BIT)
        if (zoomOut and (SystemClock.uptimeMillis() > zoomTime)) {
            if (zoomTime != 0L) zoom(1 - .0006f * (SystemClock.uptimeMillis() - zoomTime))
            zoomTime = SystemClock.uptimeMillis()
        }
        model.draw(view, viewProjection)
    }

    fun move(xStart : Float, xEnd : Float, yStart : Float, yEnd : Float) {
        if (((xStart < xEnd) and (winX(0f) > 0)) or
            ((xStart > xEnd) and (winX(Geometry.overallLen) < width))) return

        fun worldX(xWin : Float, yWin : Float) : Float {
            fun worldCords(zDepth : Float) = floatArrayOf(0f, 0f, 0f, 0f).also { obj ->
                GLU.gluUnProject(xWin, height - yWin, zDepth, view, 0, projection,
                    0, intArrayOf(0, 0, width, height), 0, obj, 0)
                for (i in 0 .. obj.size - 2) obj[i] /= obj.last()
            }
            worldCords(0f).also { (xNear, yNear, zNear) ->
                worldCords(1f).also { (xFar, yFar, zFar) ->
                    assert((yNear > 0) and (zNear > 0))
                    return if (yStart > height / 2) // (zFar - yFar * (zNear - zFar) / (yNear - yFar) > 0)
                        (xFar - yFar * (xNear - xFar) / (yNear - yFar))
                    else (xFar - zFar * (xNear - xFar) / (zNear - zFar))
                }
            }
        }
        x = 0f.coerceAtLeast(Geometry.overallLen.coerceAtMost(x
                + worldX(xStart, yStart) - worldX(xEnd, yEnd)))
        calcViewProjection()
    }
    fun zoom(scale : Float) {
        if (scale < 1) (winX(0f) to winX(Geometry.overallLen)).also { (xLeft, xRight) ->
            if ((xLeft > 0) and (xRight < width)) {
                zoomOut = false
                return
            }
            if (xLeft > 0) move(xLeft, 0f, height.toFloat(), height.toFloat())
            if (xRight < width) move(xRight, width.toFloat(), height.toFloat(), height.toFloat())
        }
        yz = Geometry.whiteLen.coerceAtLeast(Geometry.overallLen.coerceAtMost(yz / scale))
        calcViewProjection()
    }

    private fun winX(worldX : Float) : Float { floatArrayOf(0f, 0f, 0f).also { GLU.gluProject(
        worldX, Geometry.whiteWid, Geometry.whiteLen, view, 0, projection, 0,
        intArrayOf(0, 0, width, height), 0, it, 0)
        return it[0]
    } }
    private fun calcViewProjection() {
        Matrix.setLookAtM(view, 0, x, yz, yz, x, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0)
    }
}