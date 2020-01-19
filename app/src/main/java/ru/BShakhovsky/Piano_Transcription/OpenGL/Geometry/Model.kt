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

    val geom = Geometry(); val mats = Matrices()

    private val    mainShader =    MainShader(context)
    private val   depthShader =   DepthShader(context)
    private val stencilShader = StencilShader(context)
    private val textureShader = TextureShader(context)

    private val lights = arrayOf(Light(floatArrayOf(-.5265408f, -.5735765f, -.6275069f), 0, mainShader, true),
                                 Light(floatArrayOf( .7198464f,  .3420201f, -.6040227f), 1, mainShader), // z reversed, so we see it
                                 Light(floatArrayOf( .4545195f, -.7660444f,  .4545195f), 2, mainShader, true))
    val textures = Texture(context, lights)

    fun draw(width: Int, height: Int) {
        for (lightNo in 0..2) {
            depthShader.prepare(textures, lights, lightNo)
            /*// I do not like horizontal shadow-line from desk
            GLES32.glDisable(GLES32.GL_CULL_FACE)
            depthShader.draw(geom.desk, 0f, 0f, lights[lightNo].lightOrtho)
            GLES32.glEnable(GLES32.GL_CULL_FACE)*/
            geom.drawKeyboard { key, offset, angle, _ -> run { depthShader.draw(key, offset, angle, lights[lightNo].lightOrtho) }}
        }

        GLES32.glViewport(0, 0, width, height)

        with(mats) {
            mainShader.initReflectBuff(textures, lights)
            geom.drawKeyboard { key, offset, angle, color -> run { mainShader.drawKey(key, offset, angle, color,
                reflectView, reflectVP, refInvTransView, lights) }}

            mainShader.initMainScreen(textures, lights)
            stencilShader.draw(geom.desk, viewProjection)
            textureShader.draw(textures, lights.size, true)

            mainShader.initMainScreen(textures, lights, true)
            mainShader.drawDesk(geom.desk, view, viewProjection, invTransView, textures, lights.size + 1)
            geom.drawKeyboard { key, offset, angle, color -> run { mainShader.drawKey(key, offset, angle, color,
                view, viewProjection, invTransView, lights) }}
        }
    }
}