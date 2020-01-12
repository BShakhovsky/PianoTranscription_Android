@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL.Shader

import android.opengl.Matrix
import ru.BShakhovsky.Piano_Transcription.OpenGL.Geometry.Geometry

class Shadow(val lightDir: FloatArray, i: Int, mainShader: Shader) {

    val depthBuff  = mainShader.uniform("depthBuff$i")
    val light      = mainShader.uniform("light$i")
    val lightMVO   = mainShader.uniform("lightMVO$i")
    val lightOrtho = FloatArray(16)

    fun ortho(ortho: FloatArray) { FloatArray(16).also { view -> Matrix.setLookAtM(
        view, 0, Geometry.overallLen / 2 - lightDir[0], -lightDir[1], -lightDir[2],
        Geometry.overallLen / 2, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(lightOrtho, 0, ortho, 0, view, 0)
    } }
}