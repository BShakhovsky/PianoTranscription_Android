@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription

import android.os.SystemClock
import android.widget.ImageButton
import android.widget.SeekBar
import ru.BShakhovsky.Piano_Transcription.Midi.Midi.Track
import ru.BShakhovsky.Piano_Transcription.OpenGL.Render
import ru.BShakhovsky.Piano_Transcription.Utils.DebugMode

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class Play(
    private val render: Render, private val tracks: Array<Track>,
    private val playPause: ImageButton, private val seekBar: SeekBar
) : Runnable {

    var isPlaying: Boolean = false

    // Sound is smoother with Java Executor than with Android Handler,
    // all quick notes are caught and played, probably because a separate non-UI-thread is created:
    private var schedule: ScheduledExecutorService? = null
    private val selTracks = mutableSetOf<Int>()
    private val curIndices = IntArray(tracks.size)

    private var curMilSec = 0L
    private var startMilSec = 0L
    private var startTime = 0L

    override fun run() {
        nextChord().also { (_, stop) ->
            if (stop) {
                seek(0)
                // performClick() does not work, probably because this is not UI-thread:
                playPause.callOnClick()
            } else {
                DebugMode.assertState(schedule != null)
                schedule?.schedule(
                    this,
                    maxOf(0, curMilSec - startMilSec + startTime - SystemClock.uptimeMillis()),
                    TimeUnit.MILLISECONDS
                )
            }
        }
    }

    fun toggle() {
        isPlaying = !isPlaying
        if (isPlaying) {
            render.releaseAllKeys()
            newStart(curMilSec)
            DebugMode.assertState(schedule == null)
            schedule = Executors.newSingleThreadScheduledExecutor()
            DebugMode.assertState(schedule != null)
            schedule?.schedule(this, 0, TimeUnit.MILLISECONDS)
        } else {
            DebugMode.assertState(schedule != null)
            schedule?.shutdown()
            schedule = null
        }
    }

    fun numSelTracks(): Int = selTracks.size

    fun addTrack(trackNo: Int) {
        selTracks += trackNo
        seekTrack(curMilSec, trackNo)
    }

    fun removeTrack(trackNo: Int): Boolean = selTracks.remove(trackNo)

    fun seek(newMilSec: Long) {
        newStart(newMilSec)
        selTracks.forEach { seekTrack(curMilSec, it) }
        render.releaseAllKeys()
    }

    private fun seekTrack(newMilSec: Long, trackNo: Int) {
        curIndices[trackNo] = 0
        while (tracks[trackNo].chords[curIndices[trackNo]].mSec < newMilSec)
            if (++curIndices[trackNo] >= tracks[trackNo].chords.size) return
    }

    private fun newStart(newMilSec: Long) {
        curMilSec = newMilSec
        startMilSec = curMilSec
        startTime = SystemClock.uptimeMillis()
    }

    fun nextChord(): Pair<Boolean, Boolean> {
        DebugMode.assertState(selTracks.isNotEmpty())
        curMilSec.also { prevMilSec ->
            curMilSec = Int.MAX_VALUE.toLong()
            var anyPressed = false
            tracks.forEachIndexed { trackNo, track ->
                if (selTracks.contains(trackNo)) @Suppress("Reformat") with(track.chords) {
                    if (        curIndices[trackNo]         == size)        return@with
                    if (        curIndices[trackNo]         == -1)          ++curIndices[trackNo]
                    if (get(    curIndices[trackNo]).mSec   < prevMilSec)   ++curIndices[trackNo]
                    if (        curIndices[trackNo]         == size)        return@with
                    if (get(    curIndices[trackNo]).mSec   == prevMilSec) {
                        get(    curIndices[trackNo]).notes.forEach { (note, vel) ->
                            when (vel) {
                                0f -> render.releaseKey(note)
                                else -> {
                                    render.pressKey(note, vel)
                                    anyPressed = true
                                }
                            }
                        }
                        ++curIndices[trackNo]
                    }
                    if (curIndices[trackNo] != size) curMilSec =
                        minOf(curMilSec, prevMilSec + 1_000, get(curIndices[trackNo]).mSec)
                }
            }
            seekBar.progress = curMilSec.toInt()

            var stop = true
            tracks.forEachIndexed { trackNo, track ->
                if (selTracks.contains(trackNo) and (curIndices[trackNo] < track.chords.size)) {
                    stop = false
                    return@forEachIndexed
                }
            }

            return anyPressed to stop
        }
    }

    fun prevChord(): Boolean {
        DebugMode.assertState(selTracks.isNotEmpty())
        curMilSec.also { prevMilSec ->
            curMilSec = 0
            tracks.forEachIndexed { trackNo, track ->
                if (selTracks.contains(trackNo)) @Suppress("Reformat") with(track.chords) {
                    if (        curIndices[trackNo]         == -1)          return@with
                    if (        curIndices[trackNo]         == size)        --curIndices[trackNo]
                    if (get(    curIndices[trackNo]).mSec   >= prevMilSec)  --curIndices[trackNo]
                    if (        curIndices[trackNo]         != -1)
                        curMilSec = maxOf(curMilSec, get(curIndices[trackNo]).mSec)
                }
            }
            seekBar.progress = curMilSec.toInt()
            var anyPressed = false
            tracks.forEachIndexed { trackNo, track ->
                if (selTracks.contains(trackNo)) @Suppress("Reformat") with(track.chords) {
                    if (        curIndices[trackNo]         == -1) return@with
                    if (get(    curIndices[trackNo]).mSec   == curMilSec) {
                        get(    curIndices[trackNo]).notes.forEach { (note, vel) ->
                            if (vel != 0f) {
                                render.pressKey(note, vel)
                                anyPressed = true
                            }
                        }
                    }
                }
            }
            return anyPressed
        }
    }
}