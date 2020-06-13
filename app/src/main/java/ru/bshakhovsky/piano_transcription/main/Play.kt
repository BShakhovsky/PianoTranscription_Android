package ru.bshakhovsky.piano_transcription.main

import android.app.Activity
import android.os.Looper
import android.os.SystemClock
import android.view.View
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel

import ru.bshakhovsky.piano_transcription.R.string

import ru.bshakhovsky.piano_transcription.main.openGL.Render
import ru.bshakhovsky.piano_transcription.midi.Midi
import ru.bshakhovsky.piano_transcription.midi.Midi.Track
import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage
import ru.bshakhovsky.piano_transcription.utils.WeakPtr

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class Play : Runnable, ViewModel(), LifecycleObserver {

    private class TracksArray(val tracks: Array<Track>) {
        val curIndices: IntArray = IntArray(tracks.size)
        val selTracks: MutableSet<Int> = mutableSetOf()

        var curMilSec = 0L
        var startMilSec = 0L
        var startTime = 0L
    }

    private var tracksArray: TracksArray? = null

    private lateinit var activity: WeakPtr<Activity>
    private lateinit var drawer: WeakPtr<DrawerLayout>
    private lateinit var render: Render

    private val _isPlaying = MutableLiveData<Boolean>().apply { value = false }
    val isPlaying: LiveData<Boolean>
        get() = _isPlaying

    private val _duration = MutableLiveData<Int>()
    val duration: LiveData<Int>
        get() = _duration
    private val _progress = MutableLiveData<Int>()
    val progress: LiveData<Int>
        get() = _progress

    val durText: LiveData<String> = Transformations.map(duration)
    { Midi.minSecStr(activity.get(), string.timeOf, it.toLong()) }
    val curText: LiveData<String> = Transformations.map(progress)
    { Midi.minSecStr(activity.get(), string.timeCur, it.toLong()) }

    private val _prevVis = MutableLiveData<Int>()
    val prevVis: LiveData<Int>
        get() = _prevVis
    private val _nextVis = MutableLiveData<Int>()
    val nextVis: LiveData<Int>
        get() = _nextVis

    /* Sound is smoother with Java Executor than with Android Handler,
    all quick notes are caught and played, because a separate non-UI-thread is created.

    But will have to update the following LiveData in activity.runOnUiThread {} :
        _isPlaying                  toggle()
        seek bar _progress          seek(), nextChord(), prevChord()
        buttons _prevVis, _nextVis  prevNext()

    New Midi-tracks are reset only from MainActivity UI-thread:
        seek bar _duration          newMidi() */
    private var schedule: ScheduledExecutorService? = null

    fun initialize(lifecycle: Lifecycle, a: Activity, d: DrawerLayout, r: Render) {
        lifecycle.addObserver(this)
        activity = WeakPtr(lifecycle, a)
        drawer = WeakPtr(lifecycle, d)
        render = r
    }

    fun newMidi(tracks: Array<Track>, dur: Long) {
        tracksArray = TracksArray(tracks)
        DebugMode.assertState(
            Looper.myLooper() == Looper.getMainLooper(),
            "New Midi-tracks should not be reset from SingleThreadScheduledExecutor"
        )
        _duration.value = dur.toInt()
        stopPlaying()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun startPlaying() {
        DebugMode.assertState(
            (Looper.myLooper() == Looper.getMainLooper()) and (isPlaying.value != null)
        )
        if (!(isPlaying.value ?: return)) playPause()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun stopPlaying() {
        DebugMode.assertState(
            (Looper.myLooper() == Looper.getMainLooper()) and (isPlaying.value != null)
        )
        if (isPlaying.value ?: return) playPause()
    }

    override fun run(): Unit = nextChord().let { (_, stop) ->
        if (stop) {
            seek(0)
            playPause()
        } else {
            DebugMode.assertState((schedule != null) and (tracksArray != null))
            tracksArray?.run {
                schedule?.schedule(
                    this@Play,
                    /* If currently it is long silence and next notes milliseconds
                    are far in the future, curMilSec is set 1 second away in nextChord(),
                    so scheduler keeps executing every second,
                    and seek bar keeps scrolling smoothly, and time text updates accordingly */
                    maxOf(0, curMilSec - startMilSec + startTime - SystemClock.uptimeMillis()),
                    TimeUnit.MILLISECONDS
                )
            }
        }
    }


    fun playPause() {
        DebugMode.assertState(isPlaying.value != null)
        if (!(isPlaying.value ?: return) and noTracks()) return
        toggle()
        prevNext()
    }

    fun prev() {
        DebugMode.assertState(
            (Looper.myLooper() == Looper.getMainLooper()) and (isPlaying.value != null)
        )
        if (!(isPlaying.value ?: return) and !noTracks()) {
            render.releaseAllKeys()
            DebugMode.assertState(progress.value != null)
            progress.value?.let { prevMilSec ->
                do {
                    val anyPressed = prevChord()
                    DebugMode.assertState(progress.value != null)
                } while (((prevMilSec - (progress.value ?: return@let) < 1) or !anyPressed)
                    and (progress.value != 0)
                )
            }
        }
    }

    fun next() {
        DebugMode.assertState(
            (Looper.myLooper() == Looper.getMainLooper()) and (isPlaying.value != null)
        )
        if (!(isPlaying.value ?: return) and !noTracks()) {
            render.releaseAllKeys()
            DebugMode.assertState(progress.value != null)
            progress.value?.let { prevMilSec ->
                do {
                    val (anyPressed, stop) = nextChord()
                    DebugMode.assertState(progress.value != null)
                } while ((((progress.value ?: return@let) - prevMilSec < 1) or !anyPressed)
                    and !stop
                )
            }
        }
    }


    private fun toggle() = activity.get().runOnUiThread {
        DebugMode.assertState(isPlaying.value != null)
        _isPlaying.value = isPlaying.value?.not()
        DebugMode.assertState(isPlaying.value != null)
        if (isPlaying.value ?: return@runOnUiThread) {
            render.releaseAllKeys()
            DebugMode.assertState(tracksArray != null)
            tracksArray?.run { newStart(curMilSec) }
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

    private fun noTracks() = tracksArray?.selTracks.isNullOrEmpty().also {
        /* Can be clicked by touching GL view, even when buttons are invisible,
        also, no need to show error message at start: */
        if (it and (tracksArray != null)) {
            InfoMessage.toast(activity.get(), string.trackNotSel)
            drawer.get().openDrawer(GravityCompat.START)
        }
    }


    fun numSelTracks(): Int = (tracksArray?.selTracks?.size ?: 0)
        .also { DebugMode.assertState(Looper.myLooper() == Looper.getMainLooper()) }

    fun prevNext(): Unit = activity.get().runOnUiThread {
        DebugMode.assertState(isPlaying.value != null)
        _prevVis.value = if ((isPlaying.value ?: return@runOnUiThread) or (progress.value == 0))
            View.INVISIBLE else View.VISIBLE
        _nextVis.value = if ((isPlaying.value ?: return@runOnUiThread)
            or (progress.value == duration.value)
        ) View.INVISIBLE else View.VISIBLE
    }

    fun addTrack(trackNo: Int) {
        DebugMode.assertState(
            (Looper.myLooper() == Looper.getMainLooper()) and (tracksArray != null)
        )
        tracksArray?.run {
            selTracks += trackNo
            seekTrack(curMilSec, trackNo)
        }
    }

    fun removeTrack(trackNo: Int): Unit = DebugMode.assertState(
        (Looper.myLooper() == Looper.getMainLooper()) and (tracksArray != null)
    ).let { tracksArray?.run { selTracks -= trackNo } }

    fun seek(newMilSec: Int) {
        newStart(newMilSec.toLong())
        DebugMode.assertState(tracksArray != null)
        tracksArray?.run { selTracks.forEach { seekTrack(curMilSec, it) } }
        render.releaseAllKeys()
        activity.get().runOnUiThread { _progress.value = newMilSec }
    }


    private fun seekTrack(newMilSec: Long, trackNo: Int) {
        DebugMode.assertState(tracksArray != null)
        tracksArray?.run {
            curIndices[trackNo] = 0
            while (tracks[trackNo].chords[curIndices[trackNo]].mSec < newMilSec)
                if (++curIndices[trackNo] >= tracks[trackNo].chords.size) break
        }
    }

    private fun newStart(newMilSec: Long) {
        DebugMode.assertState(tracksArray != null)
        tracksArray?.run {
            curMilSec = newMilSec
            startMilSec = curMilSec
            startTime = SystemClock.uptimeMillis()
        }
    }

    private fun nextChord(): Pair<Boolean, Boolean> {
        var (anyPressed, stop) = false to true
        tracksArray?.run { // Can be clicked by touching GL view, even when buttons are invisible
            curMilSec.let { prevMilSec ->
                DebugMode.assertState((selTracks.isNotEmpty()) and (duration.value != null))
                (duration.value?.toLong() ?: Short.MAX_VALUE.toLong()).let { dur ->
                    /* If currently it is long silence and next notes milliseconds
                    are far in the future, curMilSec is set 1 second away, so scheduler will execute
                    after 1 second and will be executing every second until next notes,
                    so seek bar still will be scrolling smoothly,
                    and time text will be updating accordingly */
                    curMilSec = minOf(curMilSec + 1_000, dur)
                    tracks.forEachIndexed { trackNo, track ->
                        if (selTracks.contains(trackNo)) @Suppress("Reformat") with(track.chords) {
                            if (    curIndices[trackNo]       == size)      return@with
                            if (    curIndices[trackNo]       == -1)        ++curIndices[trackNo]
                            if (get(curIndices[trackNo]).mSec < prevMilSec) ++curIndices[trackNo]
                            if (    curIndices[trackNo]       == size)      return@with
                            if (get(curIndices[trackNo]).mSec == prevMilSec) {
                                get(curIndices[trackNo]).notes.forEach { (note, vel) ->
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
                                minOf(curMilSec, get(curIndices[trackNo]).mSec)
                        }
                    }
                    activity.get().runOnUiThread { _progress.value = curMilSec.toInt() }

                    tracks.forEachIndexed { trackNo, track ->
                        if (selTracks.contains(trackNo)
                            and (curIndices[trackNo] < track.chords.size)
                        ) {
                            stop = false
                            return@forEachIndexed
                        }
                    }
                    DebugMode.assertState(
                        if (stop) curMilSec == dur
                        // If not stop, curMilSec may be == dur if clicked from UI:
                        else curMilSec <= dur
                    )
                }
            }
        }
        return anyPressed to stop
    }

    private fun prevChord(): Boolean {
        DebugMode.assertState((Looper.myLooper() == Looper.getMainLooper()))
        var anyPressed = false
        tracksArray?.run { // Can be clicked by touching GL view, even when buttons are invisible
            curMilSec.let { prevMilSec ->
                DebugMode.assertState(selTracks.isNotEmpty())
                curMilSec = 0
                tracks.forEachIndexed { trackNo, track ->
                    if (selTracks.contains(trackNo)) @Suppress("Reformat") with(track.chords) {
                        if (    curIndices[trackNo]       == -1)         return@with
                        // Somehow curIndices were size+1 once:
//                        if (    curIndices[trackNo]       == size)       --curIndices[trackNo]
                                curIndices[trackNo] = minOf(size - 1, curIndices[trackNo])
                        if (get(curIndices[trackNo]).mSec >= prevMilSec) --curIndices[trackNo]
                        if (    curIndices[trackNo]       != -1)
                            curMilSec = maxOf(curMilSec, get(curIndices[trackNo]).mSec)
                    }
                }
                activity.get().runOnUiThread { _progress.value = curMilSec.toInt() }

                tracks.forEachIndexed { trackNo, track ->
                    if (selTracks.contains(trackNo)) @Suppress("Reformat") with(track.chords) {
                        if (    curIndices[trackNo]       == -1) return@with
                        if (get(curIndices[trackNo]).mSec == curMilSec) {
                            get(curIndices[trackNo]).notes.forEach { (note, vel) ->
                                if (vel != 0f) {
                                    render.pressKey(note, vel)
                                    anyPressed = true
                                }
                            }
                        }
                    }
                }
            }
        }
        return anyPressed
    }
}