@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL.Geometry

import android.content.Context
import android.opengl.GLES31
import android.opengl.Matrix
import ru.BShakhovsky.Piano_Transcription.OpenGL.FrameBuffs
import ru.BShakhovsky.Piano_Transcription.OpenGL.Shader.*

class Model(context: Context) {

    private val geom = Geometry()

    val mats = Matrices(); val frames = FrameBuffs()

    private val    mainShader =    MainShader  (context)
    private val   depthShader =   DepthShader  (context)
    private val stencilShader = StencilShader  (context)
    private val   orthoShader =        Shader2D(context)

    private val shadows = arrayOf(Shadow(floatArrayOf(-.5265408f, -.5735765f, -.6275069f), 0, mainShader),
                                  Shadow(floatArrayOf( .7198464f,  .3420201f,  .6040227f), 1, mainShader),
                                  Shadow(floatArrayOf( .4545195f, -.7660444f,  .4545195f), 2, mainShader))

    init { FloatArray(16).also { ortho -> Matrix.orthoM(ortho, 0, -Geometry.overallLen / 2, Geometry.overallLen / 2,
        -Geometry.overallLen / 2, Geometry.overallLen / 2, -Geometry.overallLen, Geometry.overallLen * 2)
        shadows.forEach { it.ortho(ortho) } } }

    fun draw(width: Int, height: Int) {
        for (lightNo in 0..2) {
            depthShader.prepare(frames, lightNo)
            geom.drawKeyboard { key, offset -> run { depthShader.draw(key, offset, shadows[lightNo].lightOrtho) }}
        }

        GLES31.glViewport(0, 0, width, height)

        mainShader.initReflectBuff(frames, shadows)
        geom.drawKeyboard { key, offset -> run { mainShader.drawKey(key, offset, geom.isBlack(key),
            mats.reflectView, mats.reflectVP, mats.refInvTransView, shadows) }}

        mainShader.initMainScreen(frames, shadows)
        stencilShader.draw(geom.desk, mats.viewProjection)
        orthoShader.draw(frames, shadows.size, true)

        mainShader.initMainScreen(frames, shadows, true)
        mainShader.drawDesk(geom.desk, mats.view, mats.viewProjection, mats.invTransView)
        geom.drawKeyboard { key, offset -> run { mainShader.drawKey(key, offset, geom.isBlack(key),
            mats.view, mats.viewProjection, mats.invTransView, shadows) }}
    }
}