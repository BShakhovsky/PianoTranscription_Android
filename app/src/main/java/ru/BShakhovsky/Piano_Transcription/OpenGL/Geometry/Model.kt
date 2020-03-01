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

    val geom: Geometry = Geometry()
    val mats: Matrices = Matrices()

    private val mainShader = MainShader(context)
    private val depthShader = DepthShader(context)
    private val stencilShader = StencilShader(context)
    private val textureShader = TextureShader(context)

    private val lights = arrayOf(
        Light(floatArrayOf(-.5265408f, -.5735765f, -.6275069f), 0, mainShader, true),
        // Z reversed, so we see it:
        Light(floatArrayOf(.7198464f, .3420201f, -.6040227f), 1, mainShader),
        Light(floatArrayOf(.4545195f, -.7660444f, .4545195f), 2, mainShader, true)
    )
    val textures: Texture = Texture(context, lights)

    fun draw(width: Int, height: Int): Unit = with(geom) {
        for (lightNo in 0..2) with(depthShader) {
            prepare(textures, lights, lightNo)
            /*// I do not like horizontal shadow-line from desk
            GLES32.glDisable(GLES32.GL_CULL_FACE)
            depthShader.draw(geom.desk, 0f, 0f, lights[lightNo].lightOrtho)
            GLES32.glEnable(GLES32.GL_CULL_FACE)*/
            drawKeyboard { key, offset, angle, _ ->
                draw(key, offset, angle, lights[lightNo].lightOrtho)
            }
        }

        GLES32.glViewport(0, 0, width, height)

        with(mainShader) {
            initReflectBuff(textures, lights)
            with(mats) {
                drawCotton(cotton, reflectView, reflectVP, refInvTransView, lights)
                drawKeyboard { key, offset, angle, color ->
                    drawKey(
                        key, offset, angle, color, reflectView, reflectVP, refInvTransView, lights
                    )
                }

                initMainScreen(textures, lights)
                stencilShader.draw(desk, viewProjection)
                with(lights) {
                    textureShader.draw(textures, size, true)

                    initMainScreen(textures, lights, true)
                    drawDesk(desk, view, viewProjection, invTransView, textures, size + 1)
                }
                drawCotton(cotton, view, viewProjection, invTransView, lights)
                drawKeyboard { key, offset, angle, color ->
                    drawKey(key, offset, angle, color, view, viewProjection, invTransView, lights)
                }
            }
        }
    }
}