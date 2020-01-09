@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL

import android.opengl.Matrix
import ru.BShakhovsky.Piano_Transcription.OpenGL.Shader.Shader

class Shadow(val lightDir: FloatArray, i: Int, mainShader: Shader) {

    val depthBuff: Int; val light: Int; val lightMVO: Int
    val lightOrtho = FloatArray(16)

    init { with (mainShader) {
        depthBuff = uniform("depthBuff$i")
        light     = uniform("light$i")
        lightMVO  = uniform("lightMVO$i")
    } }

    fun ortho(ortho: FloatArray) { FloatArray(16).also { view -> Matrix.setLookAtM(
        view, 0, Geometry.overallLen / 2 - lightDir[0], -lightDir[1], -lightDir[2],
        Geometry.overallLen / 2, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(lightOrtho, 0, ortho, 0, view, 0)
    } }
}