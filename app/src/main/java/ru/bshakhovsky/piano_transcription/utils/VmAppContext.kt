package ru.bshakhovsky.piano_transcription.utils

import android.app.Application
import android.content.Context

import androidx.lifecycle.AndroidViewModel

abstract class VmAppContext(application: Application) : AndroidViewModel(application) {

    // Both threads
    fun appContext(): Context = getApplication<Application>().applicationContext
}