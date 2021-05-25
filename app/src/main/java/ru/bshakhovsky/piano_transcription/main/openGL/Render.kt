package ru.bshakhovsky.piano_transcription.main.openGL

import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.PointF
import android.opengl.GLException
import android.opengl.GLSurfaceView
import android.opengl.GLU
import android.os.SystemClock
import android.widget.ImageButton

import androidx.annotation.CheckResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

import ru.bshakhovsky.piano_transcription.main.mainUI.touchScreen.Touch
import ru.bshakhovsky.piano_transcription.main.openGL.geometry.Geometry
import ru.bshakhovsky.piano_transcription.main.openGL.geometry.Model
import ru.bshakhovsky.piano_transcription.main.play.Sound
import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.WeakPtr

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Render(
    lifecycle: Lifecycle, a: AssetManager, res: Resources, surface: GLSurfaceView,
    play: ImageButton, prv: ImageButton, nxt: ImageButton, private val sound: Sound
) : GLSurfaceView.Renderer, LifecycleObserver {

    /* Not array, because do not need to access by index, but need to quickly insert/erase.
    Set, because MIDI note can be added multiple times, and trueChord.remove()
    will remove just a single instance. Mutable, because need to clear */
    val trueChord: MutableSet<Int> = mutableSetOf() // for realtime transcription

    /* prevChord used only in releaseAllKeys() to copy trueChord before clearing,
    so can be List instead of Set */
    val prevChord: MutableList<Int> = mutableListOf() // to stop highlighting prev keys as wrong

    private val assets = WeakPtr(lifecycle, a)
    private val resources = WeakPtr(lifecycle, res)
    private val surfaceView = WeakPtr(lifecycle, surface)
    private val playPause = WeakPtr(lifecycle, play)
    private val prev = WeakPtr(lifecycle, prv)
    private val next = WeakPtr(lifecycle, nxt)

    private lateinit var model: Model

    private var width = 0
    private var height = 0
    private var x = Geometry.overallLen / 2
    private var yz = Geometry.whiteLen
    private var zoomOut = true
    private var zoomTime = 0L

    private var prevTime = 0L

    init {
        lifecycle.addObserver(this)
        with(surfaceView.get()) {
            setEGLContextClientVersion(3)
            /* Otherwise it may choose stencil (and even depth) sizes = 0,
            and background would be black (and keys would be mixed up if depth size = 0): */
            setEGLConfigChooser(EGLChooser())
            setRenderer(this@Render)
            setOnTouchListener(Touch(this@Render))
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun pause() = surfaceView.get().onPause()

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun resume() = surfaceView.get().onResume()

    override fun onSurfaceCreated(glUnused: GL10?, config: EGLConfig?) {
        GLES.glEnable(GLES.GL_DEPTH_TEST)
        GLES.glEnable(GLES.GL_CULL_FACE)
        GLES.glBlendFunc(GLES.GL_SRC_ALPHA, GLES.GL_ONE_MINUS_SRC_ALPHA)
        model = Model(assets.get(), resources.get())
    }

    override fun onSurfaceChanged(glUnused: GL10?, newWidth: Int, newHeight: Int) {
        width = newWidth
        height = newHeight
        with(model) {
            with(mats) {
                project(width, height)
                viewProject(x, yz, yz)
            }
            textures.resizeReflection(width, height)
        }
    }

    override fun onDrawFrame(glUnused: GL10?) {
        if (zoomOut and (SystemClock.uptimeMillis() > zoomTime)) {
            if (zoomTime != 0L) zoom(1 - .0006f * (SystemClock.uptimeMillis() - zoomTime))
            zoomTime = SystemClock.uptimeMillis()
        }
        with(model) {
            draw(width, height)
            geom.keys.forEach { it.rotate(SystemClock.uptimeMillis() - prevTime) }
            prevTime = SystemClock.uptimeMillis()
        }
        if (DebugMode.debug) GLES.glGetError().let {
            if (it != GLES.GL_NO_ERROR) throw GLException(
                it, @Suppress("LongLine") when (it) {
                    GLES.GL_INVALID_ENUM -> "An unacceptable value is specified for an enumerated argument"
                    GLES.GL_INVALID_VALUE -> "A numeric argument is out of range"
                    GLES.GL_INVALID_OPERATION -> "The specified operation is not allowed in the current state"
                    GLES.GL_INVALID_FRAMEBUFFER_OPERATION -> "The command is trying to render to or read from the framebuffer while the currently bound framebuffer is not framebuffer complete (i.e. the return value from glCheckFramebufferStatus is not GL_FRAMEBUFFER_COMPLETE)"
                    // GLES.GL_STACK_OVERFLOW -> "An attempt has been made to perform an operation that would cause an internal stack to underflow"
                    // GLES.GL_STACK_UNDERFLOW -> "An attempt has been made to perform an operation that would cause an internal stack to overflow"
                    GLES.GL_OUT_OF_MEMORY -> "There is not enough memory left to execute the function. The state of OpenGL is undefined."
                    else -> "Unknown error code: $it"
                }
            )
        }
    }

    fun move(xStart: Float, xEnd: Float, yStart: Float) {
        if (((xStart < xEnd) and (winX(0f).x > 0)) or
            ((xStart > xEnd) and (winX(Geometry.overallLen).x < width))
        ) return
        moveUnlimited(xStart, xEnd, yStart)
        fit()
    }

    fun zoom(scale: Float): Unit =
        (winX(0f) to winX(Geometry.overallLen)).let { (winLeft, winRight) ->
            if ((scale > 1) or (winLeft.x < 0) or (winRight.x > width)) zoomUnlimited(scale)
            if (scale < 1) fit()
        }

    private fun fit() = (winX(0f) to winX(Geometry.overallLen)).let { (winLeft, winRight) ->
        @Suppress("Reformat") when {
            (winLeft.x > 0) and (winRight.x < width) -> {
                DebugMode.assertState(winLeft.y == winRight.y)
                x = Geometry.overallLen / 2
                yz = Geometry.overallLen * .425f // 1 / 2 / sqrt(2) = 0.354
                model.mats.viewProject(x, yz, yz)
                zoomOut = false
            }
            winLeft .x > 0      -> moveUnlimited(winLeft .x,    0f,                 winLeft .y)
            winRight.x < width  -> moveUnlimited(winRight.x,    width.toFloat(),    winRight.y)
        }
    }

    private fun moveUnlimited(xStart: Float, xEnd: Float, yStart: Float) = model.mats.viewProject(
        (x + worldXZ(xStart, yStart).x - worldXZ(xEnd, yStart).x)
            .coerceAtLeast(0f).coerceAtMost(Geometry.overallLen).also { x = it }, yz, yz
    )

    private fun zoomUnlimited(scale: Float) {
        yz = (yz / scale).coerceAtLeast(Geometry.whiteLen).coerceAtMost(Geometry.overallLen)
        model.mats.viewProject(x, yz, yz)
    }

    @CheckResult
    private fun winX(worldX: Float) = floatArrayOf(0f, 0f, 0f).let {
        GLU.gluProject(
            worldX, Geometry.whiteWid, Geometry.whiteLen, model.mats.view,
            0, model.mats.projection, 0, intArrayOf(0, 0, width, height), 0, it, 0
        )
        PointF(it[0], it[1])
    }

    @CheckResult
    private fun worldXZ(xWin: Float, yWin: Float): PointF {
        fun worldCords(zDepth: Float) = floatArrayOf(0f, 0f, 0f, 0f).also { obj ->
            with(model.mats) {
                GLU.gluUnProject(
                    xWin, height - yWin, zDepth, view, 0, projection,
                    0, intArrayOf(0, 0, width, height), 0, obj, 0
                )
            }
            for (i in 0 until obj.lastIndex) obj[i] /= obj.last()
        }
        worldCords(0f).also { (xNear, yNear, zNear) ->
            worldCords(1f).also { (xFar, yFar, zFar) ->
                DebugMode.assertState((yNear > 0) and (zNear > 0))
                (zFar + (Geometry.whiteWid - yFar) * (zNear - zFar) / (yNear - yFar)).also { z ->
                    @Suppress("LongLine", "Reformat")
                    return if (yWin > height / 2)               // (zFar    - yFar * (zNear - zFar) / (yNear - yFar) > 0)
                        if (z > Geometry.blackLen) PointF(
                            xFar + (Geometry.whiteWid                       - yFar) * (xNear - xFar) / (yNear - yFar),
                            z
                        )
                        else PointF(
                            xFar + (Geometry.whiteWid + Geometry.blackWid   - yFar) * (xNear - xFar) / (yNear - yFar),
                            zFar + (Geometry.whiteWid + Geometry.blackWid   - yFar) * (zNear - zFar) / (yNear - yFar)
                        )
                    else PointF(xFar - zFar * (xNear - xFar) / (zNear - zFar), 0f)
                }
            }
        }
    }

    @CheckResult
    private fun winToKey(xWin: Float, yWin: Float) = worldXZ(xWin, yWin).let { xz ->
        if ((xz.y == 0f) or (xz.x !in 0f..Geometry.overallLen) or (xz.y !in 0f..Geometry.whiteLen))
            -1
        else ((xz.x / Geometry.whiteWid + 5).toInt() / 7).let { octave ->
            @Suppress("Reformat") if (xz.y > Geometry.blackLen) {
                when {
                    xz.x < Geometry.whiteWid        -> 0
                    xz.x < Geometry.whiteWid * 2    -> 2
                    else                            -> 3 + (octave - 1) * 12 + when (
                        (xz.x / Geometry.whiteWid - 2).toInt() % 7) {
                        0 -> 0 1 -> 2 2 -> 4 3 -> 5 4 -> 7 5 -> 9 6 -> 11
                        else -> (-1).also { DebugMode.assertArgument(false) }
                    }
                }
            } else (Geometry.whiteWid / 2).let { blackW ->
                        var cord = xz.x
                when {
                            cord - Geometry.whiteWid        < -blackW               -> -1 // 0
                            cord - Geometry.whiteWid        <  blackW               ->       1
                            cord - Geometry.whiteWid        < Geometry.whiteWid     -> -1 // 2
                            cord + Geometry.whiteWid        > Geometry.overallLen   ->       87
                    else -> {
                            cord = (cord - Geometry.whiteWid * 2) %
                                    (Geometry.whiteWid * 7) - Geometry.whiteWid
                        @Suppress("LongLine") when {
                            cord                            < -blackW               -> -1 // (octave - 1) * 12 + 3
                            cord                            <  blackW               ->       (octave - 1) * 12 + 4
                            cord - Geometry.whiteWid        < -blackW               -> -1 // (octave - 1) * 12 + 5
                            cord - Geometry.whiteWid        <  blackW               ->       (octave - 1) * 12 + 6
                            cord - Geometry.whiteWid        < Geometry.whiteWid     -> -1 // (octave - 1) * 12 + 7
                            cord - Geometry.whiteWid * 3    < -blackW               -> -1 // (octave - 1) * 12 + 8
                            cord - Geometry.whiteWid * 3    <  blackW               ->       (octave - 1) * 12 + 9
                            cord - Geometry.whiteWid * 4    < -blackW               -> -1 // (octave - 1) * 12 + 10
                            cord - Geometry.whiteWid * 4    <  blackW               ->       (octave - 1) * 12 + 11
                            cord - Geometry.whiteWid * 5    < -blackW               -> -1 // (octave - 1) * 12 + 12
                            cord - Geometry.whiteWid * 5    <  blackW               ->       (octave - 1) * 12 + 13
                            cord - Geometry.whiteWid * 5    < Geometry.whiteWid     -> -1 // (octave - 1) * 12 + 14
                            else -> (-1).also { DebugMode.assertState(false) }
                        }
                    }
                }
            }
        }
    }

    fun tap(xWin: Float, yWin: Float): Unit =
        if (yWin < height / 2) skipPlay(xWin) else winToKey(xWin, yWin).let {
            if (it != -1) {
                model.geom.keys[it].isTapped = true
                sound.play(it)
            }
        }

    fun longTap(xWin: Float, yWin: Float): Unit =
        if (yWin < height / 2) skipPlay(xWin) else winToKey(xWin, yWin).let {
            if (it != -1) {
                with(model.geom.keys[it]) { isPressed = !isPressed }
                sound.play(it)
            }
        }

    private fun skipPlay(xWin: Float) {
        @Suppress("Reformat") when (xWin) {
            in 0f               ..  width / 3f      -> prev     .get().performClick()
            in width / 3f       ..  width * 2 / 3f  -> playPause.get().performClick()
            in width * 2 / 3f   ..  width.toFloat() -> next     .get().performClick()
            else -> DebugMode.assertArgument(false)
        }
    }

    fun highLightKey(note: Int, isGood: Boolean) {
        if (check(note)) with(model.geom.keys[note]) {
            isCorrect = isGood
            isWrong = !isGood
        }
    }

    fun unHighLightAll() {
        // For some reason, not yet initialized if MIDI is received from another App through Intent
        if (::model.isInitialized) model.geom.keys.forEach {
            with(it) {
                isCorrect = false
                isWrong = false
            }
        }
    }

    fun pressKey(note: Int, velocity: Float) {
        if (check(note)) {
            model.geom.keys[note].isPressed = true
            trueChord += note
            sound.play(note, velocity)
        }
    }

    fun releaseKey(note: Int) {
        if (check(note)) {
            model.geom.keys[note].isPressed = false
            trueChord -= note
            sound.stop(note)
        }
    }

    fun releaseAllKeys() {
        // Play.nextChord() usually called multiple times, we do not want to clear prevNotes:
        if (trueChord.isEmpty()) return // Already cleared
        prevChord.clear()
        prevChord += trueChord // Copy, not reference
        trueChord.clear()

        // For some reason, not yet initialized if MIDI is received from another App through Intent
        if (::model.isInitialized) model.geom.keys.forEachIndexed { note, key ->
            key.isPressed = false
            sound.stop(note)
        }
        unHighLightAll()
    }

    @CheckResult
    private fun check(note: Int) =
        // For some reason, not yet initialized if MIDI is received from another App through Intent
        ::model.isInitialized.also { DebugMode.assertArgument(note in 0..87) }
}