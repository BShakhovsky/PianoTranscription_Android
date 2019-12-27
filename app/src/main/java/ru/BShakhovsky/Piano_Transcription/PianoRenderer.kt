@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription

import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class PianoRenderer : GLSurfaceView.Renderer {

    var angle = 0f
    private val projection = FloatArray(16)
    private lateinit var model: PianoGeometry

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES31.glClearColor(70 / 255f, 130 / 255f, 180 / 255f, 1f)  // Steel blue
        model = PianoGeometry()
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        GLES31.glViewport(0, 0, width, height)
        (width.toFloat() / height.toFloat()).also { ratio ->
            Matrix.frustumM(projection, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
        }
    }

    override fun onDrawFrame(unused: GL10?) {
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        FloatArray(16).also { vp ->
            Matrix.setLookAtM(vp, 0, 3f, 3f, 5f,
                0f, 0f, 0f, 0f, 1f, 0f)
            FloatArray(16).also { mvp ->
                Matrix.multiplyMM(mvp, 0, projection, 0, vp, 0)
                Matrix.rotateM(mvp, 0, angle, 0f, 0f, -1f)
                model.draw(mvp)
            }
        }
    }
}