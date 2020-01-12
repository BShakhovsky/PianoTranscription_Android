@file:Suppress("PackageName", "SpellCheckingInspection")
package ru.BShakhovsky.Piano_Transcription.OpenGL
/*
import android.opengl.EGL14
import android.opengl.GLException
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

class EGLChooser(private val red: Int = 0, private val green: Int = 0, private val blue: Int = 0,
                 private val alpha: Int = 0, private val depth: Int = 0, private val stencil: Int = 0) : GLSurfaceView.EGLConfigChooser {

    override fun chooseConfig(egl: EGL10?, display: EGLDisplay?): EGLConfig {
        val attributes = intArrayOf( // https://www.khronos.org/registry/EGL/sdk/docs/man/html/eglChooseConfig.xhtml

            //======================================================================================
            // SORTED:

//            EGL_CONFIG_CAVEAT, EGL_DONT_CARE > EGL_NONE > EGL_SLOW_CONFIG > EGL_NON_CONFORMANT_CONFIG
                // "Slow" implementation-dependent, typically indicates non-hardware-accelerated (software) implementation
                // For EGL >= 1.3, EGL_NON_CONFORMANT_CONFIG obsolete,
                // since same can be specified via EGL_CONFORMANT on per-client-API basis

            EGL14.EGL_COLOR_BUFFER_TYPE, EGL14.EGL_RGB_BUFFER, // EGL >= 1.2
                // otherwise EGL_LUMINANCE_BUFFER and EGL_LUMINANCE_SIZE (EGL >= 1.2) must be non-zero
                // and EGL_RED_SIZE, EGL_GREEN_SIZE, EGL_BLUE_SIZE must be zero
                // For both RGB and luminance color buffers, EGL_ALPHA_SIZE may be zero or non-zero.

            EGL14.EGL_RED_SIZE, red, EGL14.EGL_GREEN_SIZE, green, EGL14.EGL_BLUE_SIZE, blue, EGL14.EGL_ALPHA_SIZE, alpha, // special: larger
                // by larger total color bits (EGL_RED_SIZE + EGL_GREEN_SIZE + EGL_BLUE_SIZE + EGL_ALPHA_SIZE;
                    // or EGL_LUMINANCE_SIZE + EGL_ALPHA_SIZE for luminance)
                // If requested bits = 0 or EGL_DONT_CARE for a particular color, then number of bits for that component not considered
                // Deeper color buffers > shallower colors, which may be counter-intuitive

//            EGL_BUFFER_SIZE = EGL_RED_SIZE + EGL_GREEN_SIZE + EGL_BLUE_SIZE + EGL_ALPHA_SIZE, smaller
                // does not include padding bits which may be present in pixel format

//            EGL_SAMPLE_BUFFERS, 0, smaller, > 1 is undefined, so only zero or one buffers will produce a match
//            EGL_SAMPLES, 0, smaller

            EGL14.EGL_DEPTH_SIZE, depth, EGL14.EGL_STENCIL_SIZE, stencil, // smaller
//            EGL_ALPHA_MASK_SIZE, smaller, OpenVG only, EGL >= 1.2

//            EGL_NATIVE_VISUAL_TYPE, sort order implementation-defined, depending on the meaning
//            EGL_CONFIG_ID, EGL_DONT_CARE, smaller, always last sorting rule, guarantees unique ordering
                // When specified, all other attributes ignored. Meaning implementation-dependent

            //======================================================================================
            // NOT SORTED:

//            EGL14.EGL_LEVEL, 0, // This specification is honored exactly
                // Buffer zero corresponds to the default frame buffer of the display
                // Buffer level one is the first overlay frame buffer, level two the second overlay frame buffer, and so on
                // Negative buffer levels correspond to underlay frame buffers
                // Most platforms do not support buffer levels other than zero
                // The behavior of windows placed in overlay and underlay planes depends on the underlying platform

            // For EGL_CONFORMANT, EGL_RENDERABLE_TYPE, and EGL_SURFACE_TYPE, only nonzero bits of the mask are considered when matching
            // zero bits in bitmask may be either zero or one in the resulting config
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, // EGL >= 1.2, eglCreateContext, EGL_OPENGL_BIT EGL >= 1.4, ES EGL >= 1.2, ES2 EGL >= 1.3
            EGL14.EGL_CONFORMANT,      EGL14.EGL_OPENGL_ES2_BIT, // EGL >= 1.3, eglCreateContext
//            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                // EGL_MULTISAMPLE_RESOLVE_BOX_BIT // box filtered multisample resolve behavior with eglSurfaceAttrib, EGL >= 1.4
                // EGL_PBUFFER_BIT                 // pixel buffer surfaces
                // EGL_PIXMAP_BIT                  // pixmap surfaces
                // EGL_SWAP_BEHAVIOR_PRESERVED_BIT // for color buffers with eglSurfaceAttrib, EGL >= 1.4
                // EGL_VG_ALPHA_FORMAT_PRE_BIT     // OpenVG with premultiplied alpha values at surface creation time, EGL >= 1.3
                                                   // eglCreatePbufferSurface, eglCreatePixmapSurface, and eglCreateWindowSurface
                // EGL_VG_COLORSPACE_LINEAR_BIT    // OpenVG in linear colorspace at surface creation time, EGL >= 1.3
                                                   // eglCreatePbufferSurface, eglCreatePixmapSurface, and eglCreateWindowSurface

//            EGL14.EGL_TRANSPARENT_TYPE, EGL14.EGL_NONE, // or EGL_TRANSPARENT_RGB
                // Most implementations support only opaque frame buffer configurations.
                // EGL_TRANSPARENT_RED_VALUE, EGL_TRANSPARENT_GREEN_VALUE, EGL_TRANSPARENT_BLUE_VALUE
                    // must be between zero and the maximum color buffer value, default value is EGL_DONT_CARE
                    // ignored unless EGL_TRANSPARENT_TYPE = EGL_TRANSPARENT_RGB

//            EGL_BIND_TO_TEXTURE_RGB, EGL_DONT_CARE, EGL_TRUE, or EGL_FALSE, only for pbuffers
//            EGL_BIND_TO_TEXTURE_RGBA, EGL_DONT_CARE, EGL_TRUE, or EGL_FALSE, only for pbuffers

//            EGL_NATIVE_RENDERABLE, EGL_DONT_CARE, EGL_TRUE, or EGL_FALSE.
//            EGL_MATCH_NATIVE_PIXMAP, EGL_NONE, handle of native pixmap, cast to EGLint, eglCreatePixmapSurface, EGL >= 1.3

//            EGL_MAX_SWAP_INTERVAL, EGL_DONT_CARE, eglSwapInterval
//            EGL_MIN_SWAP_INTERVAL, EGL_DONT_CARE, eglSwapInterval

            EGL14.EGL_NONE)

        val arg = IntArray(1)
        egl?.eglChooseConfig(display, attributes, null, 0, arg)
        if (arg.first() == 0) throw GLException(0, "Could not find EGL configs")

        arrayOfNulls<EGLConfig>(arg.first()).also {
            egl?.eglChooseConfig(display, attributes, it, arg.first(), arg)
//            if (BuildConfig.DEBUG) logAll(configs)
            return choose(egl, display, it)
        }
    }

    private fun choose(egl: EGL10?, display: EGLDisplay?, configs: Array<EGLConfig?>) : EGLConfig {
        var match = configs.first()
        var (minR, minG, minB) = Triple(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
        var (minA, minD, minS) = Triple(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)

        egl?.let { fun find(config: EGLConfig?, attribute: Int) : Int { IntArray(1).also {
                if (egl.eglGetConfigAttrib(display, config, attribute, it)) return it.first()
                throw GLException(0, "Could not find attribute in EGL config") } }

            configs.forEach { config -> Triple(find(config, EGL14.EGL_RED_SIZE),
                                               find(config, EGL14.EGL_GREEN_SIZE),
                                               find(config, EGL14.EGL_BLUE_SIZE)).also { (r, g, b) ->
                                        Triple(find(config, EGL14.EGL_ALPHA_SIZE),
                                               find(config, EGL14.EGL_DEPTH_SIZE),
                                               find(config, EGL14.EGL_STENCIL_SIZE)).also { (a, d, s) ->
                if ((r in red..minR) and (g in green..minG) and (b in blue..minB) and
                    (a in alpha..minA) and (d in depth..minD) and (s in stencil..minS)) {
                        minR = r; minG = g; minB = b; minA = a; minD = d; minS = s; match = config }
            } } }
            match?.let { return it }
            throw GLException(0, "EGL config is NULL")
        }
        throw GLException(0, "EGL context is NULL")
    }

    private fun check(error: Int) {
        if (error == EGL14.EGL_TRUE) return
        throw GLException(0, when (error) {
            EGL14.EGL_BAD_DISPLAY -> "Not an EGL display connection"
            EGL14.EGL_BAD_ATTRIBUTE -> "attribute_list contains an invalid frame buffer configuration attribute or an attribute value that is unrecognized or out of range"
            EGL14.EGL_NOT_INITIALIZED -> "EGL display has not been initialized"
            EGL14.EGL_BAD_PARAMETER -> "EGL number of configs is zero"
            EGL14.EGL_FALSE -> "Unknown EGL error"
            else -> "New EGL error" })
    }
/*
    private fun logAll(egl: EGL10?, display: EGLDisplay?, configs: Array<EGLConfig?>) {
        IntArray(1).also { arg -> configs.forEachIndexed { index, config -> Log.d("EGLConfig", "$index")
            intArrayOf(EGL10.EGL_BUFFER_SIZE, EGL10.EGL_ALPHA_SIZE, EGL10.EGL_BLUE_SIZE, EGL10.EGL_GREEN_SIZE, EGL10.EGL_RED_SIZE,
                EGL10.EGL_DEPTH_SIZE, EGL10.EGL_STENCIL_SIZE, EGL10.EGL_CONFIG_CAVEAT, EGL10.EGL_CONFIG_ID, EGL10.EGL_LEVEL,
                EGL10.EGL_MAX_PBUFFER_HEIGHT, EGL10.EGL_MAX_PBUFFER_PIXELS, EGL10.EGL_MAX_PBUFFER_WIDTH, EGL10.EGL_NATIVE_RENDERABLE,
                EGL10.EGL_NATIVE_VISUAL_ID, EGL10.EGL_NATIVE_VISUAL_TYPE, 12336, // EGL10.EGL_PRESERVED_RESOURCES,
                EGL10.EGL_SAMPLES, EGL10.EGL_SAMPLE_BUFFERS, EGL10.EGL_SURFACE_TYPE, EGL10.EGL_TRANSPARENT_TYPE,
                EGL10.EGL_TRANSPARENT_RED_VALUE, EGL10.EGL_TRANSPARENT_GREEN_VALUE, EGL10.EGL_TRANSPARENT_BLUE_VALUE,
                12345, // EGL10.EGL_BIND_TO_TEXTURE_RGB,
                12346, // EGL10.EGL_BIND_TO_TEXTURE_RGBA,
                12347, // EGL10.EGL_MIN_SWAP_INTERVAL,
                12348, // EGL10.EGL_MAX_SWAP_INTERVAL,
                EGL10.EGL_LUMINANCE_SIZE, EGL10.EGL_ALPHA_MASK_SIZE, EGL10.EGL_COLOR_BUFFER_TYPE, EGL10.EGL_RENDERABLE_TYPE,
                12354 // EGL10.EGL_CONFORMANT
            ).forEachIndexed { i, attr -> egl?.eglGetConfigAttrib(display, config, attr, arg).also { Log.d("EGLConfig",
                "${arrayOf("EGL_BUFFER_SIZE", "EGL_ALPHA_SIZE", "EGL_BLUE_SIZE", "EGL_GREEN_SIZE", "EGL_RED_SIZE", "EGL_DEPTH_SIZE", "EGL_STENCIL_SIZE", "EGL_CONFIG_CAVEAT", "EGL_CONFIG_ID", "EGL_LEVEL", "EGL_MAX_PBUFFER_HEIGHT", "EGL_MAX_PBUFFER_PIXELS", "EGL_MAX_PBUFFER_WIDTH", "EGL_NATIVE_RENDERABLE", "EGL_NATIVE_VISUAL_ID", "EGL_NATIVE_VISUAL_TYPE", "EGL_PRESERVED_RESOURCES", "EGL_SAMPLES", "EGL_SAMPLE_BUFFERS", "EGL_SURFACE_TYPE", "EGL_TRANSPARENT_TYPE", "EGL_TRANSPARENT_RED_VALUE", "EGL_TRANSPARENT_GREEN_VALUE", "EGL_TRANSPARENT_BLUE_VALUE", "EGL_BIND_TO_TEXTURE_RGB", "EGL_BIND_TO_TEXTURE_RGBA", "EGL_MIN_SWAP_INTERVAL", "EGL_MAX_SWAP_INTERVAL", "EGL_LUMINANCE_SIZE", "EGL_ALPHA_MASK_SIZE", "EGL_COLOR_BUFFER_TYPE", "EGL_RENDERABLE_TYPE", "EGL_CONFORMANT")[i]} : ${if (it) value[0] else "ERROR"}") } }
        } }
    }
*/
}*/