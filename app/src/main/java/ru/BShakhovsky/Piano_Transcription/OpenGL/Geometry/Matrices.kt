@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL.Geometry

import android.opengl.Matrix

class Matrices {

    val projection      = FloatArray(16)

    val view            = FloatArray(16)
    val viewProjection  = FloatArray(16)
    val invTransView    = FloatArray(16)

    val reflectView     = FloatArray(16)
    val reflectVP       = FloatArray(16)
    val refInvTransView = FloatArray(16)

    fun project(width: Int, height: Int) { (height.toFloat() / width.toFloat()).also { ratio ->
        Matrix.frustumM(projection, 0, -1f, 1f, -ratio, ratio, 1f, Geometry.overallLen * .7f) } } // .67

    fun viewProject(x: Float, y: Float, z: Float) {
        Matrix.setLookAtM(view, 0, x, y, z, x, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0)
        Matrix.setLookAtM(reflectView, 0, x, y, -z, x, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(reflectVP, 0, projection, 0, reflectView, 0)
        FloatArray(16).also { inv ->
            Matrix.invertM(inv, 0, view, 0)
            Matrix.transposeM(invTransView, 0, inv, 0)
            Matrix.invertM(inv, 0, reflectView, 0)
            Matrix.transposeM(refInvTransView, 0, inv, 0)
        }
    }
}