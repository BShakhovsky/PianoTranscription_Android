package ru.bshakhovsky.piano_transcription.main.play

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.view.View

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel

import ru.bshakhovsky.piano_transcription.R.raw

import ru.bshakhovsky.piano_transcription.utils.DebugMode

class Sound : SoundPool.OnLoadCompleteListener, ViewModel() {

    private val _visibility = MutableLiveData<Int>()
    val visibility: LiveData<Int> get() = _visibility

    private val _loaded = MutableLiveData<MutableSet<Int>>().apply { value = mutableSetOf() }
    val count: LiveData<Int> = Transformations.map(_loaded) { it.size }

    private lateinit var notes: IntArray
    private val sound = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        SoundPool.Builder().setMaxStreams(10).build() else @Suppress("DEPRECATION")
    SoundPool(10, AudioManager.STREAM_MUSIC, 0)).apply { setOnLoadCompleteListener(this@Sound) }

    override fun onCleared() {
        _visibility.value = View.GONE
        DebugMode.assertState(_loaded.value != null)
        _loaded.value?.run {
            clear()
            DebugMode.assertState(isEmpty() and (size == 0))
        }
        sound.setOnLoadCompleteListener(null)
        super.onCleared()
    }

    override fun onLoadComplete(sound: SoundPool?, note: Int, status: Int) {
        DebugMode.assertArgument(sound != null)
        with(_loaded) {
            DebugMode.assertState(value != null)
            value?.run {
                DebugMode.assertState(!contains(note) and (size in 0..87) and (status == 0))
                value = apply { add(note) }
                if (size == 88) onCleared()
            }
        }
    }

    fun load(context: Context) {
        notes = intArrayOf(
            sound.load(context, raw.note00, 1),
            sound.load(context, raw.note01, 1),
            sound.load(context, raw.note02, 1),
            sound.load(context, raw.note03, 1),
            sound.load(context, raw.note04, 1),
            sound.load(context, raw.note05, 1),
            sound.load(context, raw.note06, 1),
            sound.load(context, raw.note07, 1),
            sound.load(context, raw.note08, 1),
            sound.load(context, raw.note09, 1),

            sound.load(context, raw.note10, 1),
            sound.load(context, raw.note11, 1),
            sound.load(context, raw.note12, 1),
            sound.load(context, raw.note13, 1),
            sound.load(context, raw.note14, 1),
            sound.load(context, raw.note15, 1),
            sound.load(context, raw.note16, 1),
            sound.load(context, raw.note17, 1),
            sound.load(context, raw.note18, 1),
            sound.load(context, raw.note19, 1),

            sound.load(context, raw.note20, 1),
            sound.load(context, raw.note21, 1),
            sound.load(context, raw.note22, 1),
            sound.load(context, raw.note23, 1),
            sound.load(context, raw.note24, 1),
            sound.load(context, raw.note25, 1),
            sound.load(context, raw.note26, 1),
            sound.load(context, raw.note27, 1),
            sound.load(context, raw.note28, 1),
            sound.load(context, raw.note29, 1),

            sound.load(context, raw.note30, 1),
            sound.load(context, raw.note31, 1),
            sound.load(context, raw.note32, 1),
            sound.load(context, raw.note33, 1),
            sound.load(context, raw.note34, 1),
            sound.load(context, raw.note35, 1),
            sound.load(context, raw.note36, 1),
            sound.load(context, raw.note37, 1),
            sound.load(context, raw.note38, 1),
            sound.load(context, raw.note39, 1),

            sound.load(context, raw.note40, 1),
            sound.load(context, raw.note41, 1),
            sound.load(context, raw.note42, 1),
            sound.load(context, raw.note43, 1),
            sound.load(context, raw.note44, 1),
            sound.load(context, raw.note45, 1),
            sound.load(context, raw.note46, 1),
            sound.load(context, raw.note47, 1),
            sound.load(context, raw.note48, 1),
            sound.load(context, raw.note49, 1),

            sound.load(context, raw.note50, 1),
            sound.load(context, raw.note51, 1),
            sound.load(context, raw.note52, 1),
            sound.load(context, raw.note53, 1),
            sound.load(context, raw.note54, 1),
            sound.load(context, raw.note55, 1),
            sound.load(context, raw.note56, 1),
            sound.load(context, raw.note57, 1),
            sound.load(context, raw.note58, 1),
            sound.load(context, raw.note59, 1),

            sound.load(context, raw.note60, 1),
            sound.load(context, raw.note61, 1),
            sound.load(context, raw.note62, 1),
            sound.load(context, raw.note63, 1),
            sound.load(context, raw.note64, 1),
            sound.load(context, raw.note65, 1),
            sound.load(context, raw.note66, 1),
            sound.load(context, raw.note67, 1),
            sound.load(context, raw.note68, 1),
            sound.load(context, raw.note69, 1),

            sound.load(context, raw.note70, 1),
            sound.load(context, raw.note71, 1),
            sound.load(context, raw.note72, 1),
            sound.load(context, raw.note73, 1),
            sound.load(context, raw.note74, 1),
            sound.load(context, raw.note75, 1),
            sound.load(context, raw.note76, 1),
            sound.load(context, raw.note77, 1),
            sound.load(context, raw.note78, 1),
            sound.load(context, raw.note79, 1),

            sound.load(context, raw.note80, 1),
            sound.load(context, raw.note81, 1),
            sound.load(context, raw.note82, 1),
            sound.load(context, raw.note83, 1),
            sound.load(context, raw.note84, 1),
            sound.load(context, raw.note85, 1),
            sound.load(context, raw.note86, 1),
            sound.load(context, raw.note87, 1)
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