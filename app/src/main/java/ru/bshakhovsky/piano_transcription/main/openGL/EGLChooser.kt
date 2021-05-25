package ru.bshakhovsky.piano_transcription.main.openGL

import android.opengl.EGL14
import android.opengl.EGLExt.EGL_OPENGL_ES3_BIT_KHR
import android.opengl.GLException
import android.opengl.GLSurfaceView

import ru.bshakhovsky.piano_transcription.utils.DebugMode

import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

typealias EGL = EGL14

class EGLChooser : GLSurfaceView.EGLConfigChooser {

    override fun chooseConfig(egl: EGL10?, display: EGLDisplay?): EGLConfig {
        val attributes = intArrayOf( /* https://developer.qualcomm.com/qfile/28557/
                                        80-nu141-1_b_adreno_opengl_es_developer_guide.pdf,
                                        Page 25, 2.3.1 Create an ES 2.0 context on Android */

            EGL.EGL_SURFACE_TYPE, EGL.EGL_WINDOW_BIT, // not sorted

//            EGL.EGL_COLOR_BUFFER_TYPE, EGL.EGL_RGB_BUFFER,
            // Special: by larger total color bits (R + G + B + A),
            // deeper color buffers > shallower colors, which may be counter-intuitive:
            EGL.EGL_RED_SIZE, 5, EGL.EGL_GREEN_SIZE, 6, EGL.EGL_BLUE_SIZE, 5,
            EGL.EGL_DEPTH_SIZE, 16, EGL.EGL_STENCIL_SIZE, 8, // smaller

            /* https://stackoverflow.com/a/64637011
            https://stackoverflow.com/questions/64636428/
                glcreateshader-crashes-on-android-11-pixel-5/64637011#64637011 */
            EGL.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT_KHR, // not sorted
//            EGL.EGL_CONFORMANT,      EGL_OPENGL_ES3_BIT_KHR, // not sorted

            EGL.EGL_NONE
        )

        DebugMode.assertArgument(egl != null)
        egl?.run {
            IntArray(1).let { arg ->
                /* https://www.khronos.org/registry/EGL/sdk/docs/man/html/eglChooseConfig.xhtml
                EGL implementors are strongly discouraged, but not proscribed,
                from changing the selection algorithm used by eglChooseConfig.
                Therefore, selections may change from release to release of the client-side library.

                Also from https://developer.qualcomm.com/qfile/28557/
                    80-nu141-1_b_adreno_opengl_es_developer_guide.pdf,
                    Page 25, 2.3.1 Create an ES 2.0 context on Android:
                If the list size is limited to a single entry,
                it is guaranteed to retrieve the best matching configuration */
                arrayOfNulls<EGLConfig>(1).let { configs ->
                    eglChooseConfig(display, attributes, configs, 1, arg)
                    configs.first().let { config ->
                        DebugMode.assertState(config != null)
                        config?.let { return it }
                    }
                }
            }
        }
        throw GLException(0, "EGL config is NULL")
    }
}