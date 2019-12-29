@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription

import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.opengl.GLU
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Render : GLSurfaceView.Renderer {

    private lateinit var model : Geometry
    private var x = 0f
    private var y = 0f
    private var z = 0f
    private var width  = 0
    private var height = 0
    private val view           = FloatArray(16)
    private val projection     = FloatArray(16)
    private val viewProjection = FloatArray(16)

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES31.glClearColor(70 / 255f, 130 / 255f, 180 / 255f, 1f)  // Steel blue
        GLES31.glEnable(GLES31.GL_DEPTH_TEST)
        model = Geometry()
        x = model.whiteWid * 52 / 2
        y = model.whiteLen * 5
        z = model.whiteLen * 5
    }
    override fun onSurfaceChanged(unused: GL10?, newWidth: Int, newHeight: Int) {
        width = newWidth
        height = newHeight
        GLES31.glViewport(0, 0, width, height)
        (width.toFloat() / height.toFloat()).also { ratio ->
            Matrix.frustumM(projection, 0, -ratio, ratio,
                -1f, 1f, 3f, model.whiteLen * 7)
        }
        calcViewProjection()
    }
    override fun onDrawFrame(unused: GL10?) {
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT or GLES31.GL_DEPTH_BUFFER_BIT)
        model.draw(viewProjection)
    }

    fun move(xStart : Float, xEnd : Float, yStart : Float, yEnd : Float) {
        fun worldX(xWin : Float, yWin : Float) : Float {
            fun worldCords(zDepth : Float) = floatArrayOf(0f, 0f, 0f, 0f).also { obj ->
                GLU.gluUnProject(xWin, height - yWin, zDepth,
                    view, 0, projection, 0,
                    intArrayOf(0, 0, width, height), 0, obj, 0)
                for (i in 0 .. obj.size - 2) obj[i] /= obj.last()
            }
            val (xNear, yNear, zNear) = worldCords(0f)
            val (xFar, yFar, zFar) = worldCords(1f)

            return if (zFar - yFar * (zNear - zFar) / (yNear - yFar) > 0)
                (xFar - yFar * (xNear - xFar) / (yNear - yFar))
            else (xFar - zFar * (xNear - xFar) / (zNear - zFar))
        }
        x += worldX(xStart, yStart) - worldX(xEnd, yEnd)
        calcViewProjection()
    }
    private fun calcViewProjection() {
        Matrix.setLookAtM(view, 0, x, y, z,
            x, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0)
    }
}