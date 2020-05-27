package ru.bshakhovsky.piano_transcription.main

import android.content.Context
import android.media.SoundPool
import android.view.View

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel

import ru.bshakhovsky.piano_transcription.R.raw

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.WeakPtr

class Sound : SoundPool.OnLoadCompleteListener, ViewModel() {

    private val _visibility = MutableLiveData<Int>().apply { value = View.VISIBLE }
    val visibility: LiveData<Int>
        get() = _visibility

    private val _loaded = MutableLiveData<MutableSet<Int>>().apply { value = mutableSetOf() }
    val count: LiveData<Int> = Transformations.map(_loaded) { it.size }

    private lateinit var notes: IntArray
    private val sound = SoundPool.Builder().setMaxStreams(10).build()
        .apply { setOnLoadCompleteListener(this@Sound) }

    override fun onCleared() {
        _visibility.value = View.GONE
        _loaded.value?.run {
            clear()
            DebugMode.assertState(isEmpty() and (size == 0))
        }
        sound.setOnLoadCompleteListener(null)
        super.onCleared()
    }

    override fun onLoadComplete(sound: SoundPool?, note: Int, status: Int) {
        DebugMode.assertState(_loaded.value != null)
        with(_loaded) {
            value?.run {
                DebugMode.assertArgument(sound != null)
                DebugMode.assertState(!contains(note) and (size in 0..87) and (status == 0))
                value = value.apply { add(note) }
                if (size == 88) onCleared()
            }
        }
    }

    fun load(context: Context, lifecycle: Lifecycle): Unit = WeakPtr(lifecycle, context).run {
        notes = intArrayOf(
            sound.load(get(), raw.note00, 1),
            sound.load(get(), raw.note01, 1),
            sound.load(get(), raw.note02, 1),
            sound.load(get(), raw.note03, 1),
            sound.load(get(), raw.note04, 1),
            sound.load(get(), raw.note05, 1),
            sound.load(get(), raw.note06, 1),
            sound.load(get(), raw.note07, 1),
            sound.load(get(), raw.note08, 1),
            sound.load(get(), raw.note09, 1),

            sound.load(get(), raw.note10, 1),
            sound.load(get(), raw.note11, 1),
            sound.load(get(), raw.note12, 1),
            sound.load(get(), raw.note13, 1),
            sound.load(get(), raw.note14, 1),
            sound.load(get(), raw.note15, 1),
            sound.load(get(), raw.note16, 1),
            sound.load(get(), raw.note17, 1),
            sound.load(get(), raw.note18, 1),
            sound.load(get(), raw.note19, 1),

            sound.load(get(), raw.note20, 1),
            sound.load(get(), raw.note21, 1),
            sound.load(get(), raw.note22, 1),
            sound.load(get(), raw.note23, 1),
            sound.load(get(), raw.note24, 1),
            sound.load(get(), raw.note25, 1),
            sound.load(get(), raw.note26, 1),
            sound.load(get(), raw.note27, 1),
            sound.load(get(), raw.note28, 1),
            sound.load(get(), raw.note29, 1),

            sound.load(get(), raw.note30, 1),
            sound.load(get(), raw.note31, 1),
            sound.load(get(), raw.note32, 1),
            sound.load(get(), raw.note33, 1),
            sound.load(get(), raw.note34, 1),
            sound.load(get(), raw.note35, 1),
            sound.load(get(), raw.note36, 1),
            sound.load(get(), raw.note37, 1),
            sound.load(get(), raw.note38, 1),
            sound.load(get(), raw.note39, 1),

            sound.load(get(), raw.note40, 1),
            sound.load(get(), raw.note41, 1),
            sound.load(get(), raw.note42, 1),
            sound.load(get(), raw.note43, 1),
            sound.load(get(), raw.note44, 1),
            sound.load(get(), raw.note45, 1),
            sound.load(get(), raw.note46, 1),
            sound.load(get(), raw.note47, 1),
            sound.load(get(), raw.note48, 1),
            sound.load(get(), raw.note49, 1),

            sound.load(get(), raw.note50, 1),
            sound.load(get(), raw.note51, 1),
            sound.load(get(), raw.note52, 1),
            sound.load(get(), raw.note53, 1),
            sound.load(get(), raw.note54, 1),
            sound.load(get(), raw.note55, 1),
            sound.load(get(), raw.note56, 1),
            sound.load(get(), raw.note57, 1),
            sound.load(get(), raw.note58, 1),
            sound.load(get(), raw.note59, 1),

            sound.load(get(), raw.note60, 1),
            sound.load(get(), raw.note61, 1),
            sound.load(get(), raw.note62, 1),
            sound.load(get(), raw.note63, 1),
            sound.load(get(), raw.note64, 1),
            sound.load(get(), raw.note65, 1),
            sound.load(get(), raw.note66, 1),
            sound.load(get(), raw.note67, 1),
            sound.load(get(), raw.note68, 1),
            sound.load(get(), raw.note69, 1),

            sound.load(get(), raw.note70, 1),
            sound.load(get(), raw.note71, 1),
            sound.load(get(), raw.note72, 1),
            sound.load(get(), raw.note73, 1),
            sound.load(get(), raw.note74, 1),
            sound.load(get(), raw.note75, 1),
            sound.load(get(), raw.note76, 1),
            sound.load(get(), raw.note77, 1),
            sound.load(get(), raw.note78, 1),
            sound.load(get(), raw.note79, 1),

            sound.load(get(), raw.note80, 1),
            sound.load(get(), raw.note81, 1),
            sound.load(get(), raw.note82, 1),
            sound.load(get(), raw.note83, 1),
            sound.load(get(), raw.note84, 1),
            sound.load(get(), raw.note85, 1),
            sound.load(get(), raw.note86, 1),
            sound.load(get(), raw.note87, 1)
        )
    }

    fun play(note: Int, velocity: Float = 1f) {
        DebugMode.assertArgument(velocity in 0f..1f)
        @Suppress("Reformat") sound.play(
            notes[note],
            velocity * (notes.lastIndex - note) / notes.lastIndex.toFloat(),
            velocity * note                    / notes.lastIndex.toFloat(),
            1, 0, 1f
        )
    }

    fun stop(note: Int): Unit = sound.stop(notes[note])
}