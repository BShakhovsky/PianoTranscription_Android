@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL.Geometry

import android.content.Context
import android.opengl.GLES32
import ru.BShakhovsky.Piano_Transcription.OpenGL.Texture
import ru.BShakhovsky.Piano_Transcription.OpenGL.Shader.DepthShader
import ru.BShakhovsky.Piano_Transcription.OpenGL.Shader.Light
import ru.BShakhovsky.Piano_Transcription.OpenGL.Shader.MainShader
import ru.BShakhovsky.Piano_Transcription.OpenGL.Shader.TextureShader
import ru.BShakhovsky.Piano_Transcription.OpenGL.Shader.StencilShader

class Model(context: Context) {

    private val geom = Geometry()

    val mats = Matrices()

    private val    mainShader =    MainShader(context)
    private val   depthShader =   DepthShader(context)
    private val stencilShader = StencilShader(context)
    private val textureShader = TextureShader(context)

    private val lights = arrayOf(Light(floatArrayOf(-.5265408f, -.5735765f, -.6275069f), 0, mainShader),
                                 Light(floatArrayOf( .7198464f,  .3420201f, -.6040227f), 1, mainShader), // z reversed
                                 Light(floatArrayOf( .4545195f, -.7660444f,  .4545195f), 2, mainShader))
    val textures = Texture(context, lights)

    fun draw(width: Int, height: Int) {
        for (lightNo in 0..2) {
            depthShader.prepare(textures, lights, lightNo)
            /*// I do not like horizontal shadow-line from desk
            GLES31.glDisable(GLES31.GL_CULL_FACE)
            depthShader.draw(geom.desk, 0f, lights[lightNo].lightOrtho)
            GLES31.glEnable(GLES31.GL_CULL_FACE)*/
            geom.drawKeyboard { key, offset -> run { depthShader.draw(key, offset, lights[lightNo].lightOrtho) }}
        }

        GLES32.glViewport(0, 0, width, height)

        mainShader.initReflectBuff(textures, lights)
        geom.drawKeyboard { key, offset -> run { mainShader.drawKey(key, offset, geom.isBlack(key),
            mats.reflectView, mats.reflectVP, mats.refInvTransView, lights) }}

        mainShader.initMainScreen(textures, lights)
        stencilShader.draw(geom.desk, mats.viewProjection)
        textureShader.draw(textures, lights.size, true)

        mainShader.initMainScreen(textures, lights, true)
        mainShader.drawDesk(geom.desk, mats.view, mats.viewProjection, mats.invTransView, textures, lights.size + 1)
        geom.drawKeyboard { key, offset -> run { mainShader.drawKey(key, offset, geom.isBlack(key),
            mats.view, mats.viewProjection, mats.invTransView, lights) }}
    }
}