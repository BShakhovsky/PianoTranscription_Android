package ru.bshakhovsky.piano_transcription.main

import androidx.fragment.app.FragmentManager

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import ru.bshakhovsky.piano_transcription.addDialog.AddDialog
import ru.bshakhovsky.piano_transcription.utils.WeakPtr

class MainModel : ViewModel() {

    private lateinit var fragment: WeakPtr<FragmentManager>

    val contVis: MutableLiveData<Int> = MutableLiveData<Int>()

    fun initialize(lifecycle: Lifecycle, f: FragmentManager): Unit =
        run { fragment = WeakPtr(lifecycle, f) }

    fun dialogAdd(): Unit = AddDialog().show(fragment.get(), "Dialog Add")
}