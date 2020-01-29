@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription

import android.os.Handler
import android.widget.ImageButton
import android.widget.SeekBar
import ru.BShakhovsky.Piano_Transcription.Midi.Midi.Track
import ru.BShakhovsky.Piano_Transcription.OpenGL.Render

class Play(private val render: Render, private val tracks: Array<Track>,
           private val playPause: ImageButton, private val seekBar: SeekBar) : Runnable {

    var isPlaying = false

    private var handle = Handler()
    private val selTracks = mutableSetOf<Int>()
    private val curIndices = IntArray(tracks.size)
    private var curMilSec = 0L

    override fun run() {
        Assert.state(selTracks.isNotEmpty())
        curMilSec.also { prevMilSec ->
            curMilSec = Long.MAX_VALUE
            tracks.forEachIndexed { trackNo, track -> if (selTracks.contains(trackNo)) with(track.chords) {
                if (curIndices[trackNo] >= size) return@with
                if (this[curIndices[trackNo]].mSec == prevMilSec) {
                    this[curIndices[trackNo]].notes.forEach { with(it) { when {
                        isOn -> render.pressKey(note, vel); else -> render.releaseKey(note) } } }
                    ++curIndices[trackNo]
                }
                if (curIndices[trackNo] < size) curMilSec = minOf(curMilSec,
                    prevMilSec + 1_000, this[curIndices[trackNo]].mSec)
            } }
            seekBar.progress = curMilSec.toInt()

            var stop = true
            tracks.forEachIndexed { trackNo, track -> if (curIndices[trackNo] < track.chords.size) {
                stop = false; return@forEachIndexed } }
            if (stop) { playPause.performClick(); seek(0) }
            else handle.postDelayed(this, curMilSec - prevMilSec)
        }
    }

    fun toggle() { isPlaying = !isPlaying; if (isPlaying) { render.releaseAllKeys()
        handle.post(this) } else handle.removeCallbacks(this) }

    fun numSelTracks() = selTracks.size
    fun addTrack(trackNo: Int) { selTracks.add(trackNo); seekTrack(curMilSec, trackNo) }
    fun removeTrack(trackNo: Int) = selTracks.remove(trackNo)

    fun seek(newMilSec: Long) {
        curMilSec = newMilSec
        selTracks.forEach { seekTrack(curMilSec, it) }
        render.releaseAllKeys()
    }
    private fun seekTrack(newMilSec: Long, trackNo: Int) {
        curIndices[trackNo] = 0
        while (tracks[trackNo].chords[curIndices[trackNo]].mSec < newMilSec)
            if (++curIndices[trackNo] >= tracks[trackNo].chords.size) return
    }
}