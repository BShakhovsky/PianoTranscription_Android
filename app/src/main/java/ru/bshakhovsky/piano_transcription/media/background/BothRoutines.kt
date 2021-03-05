package ru.bshakhovsky.piano_transcription.media.background

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Looper

import androidx.annotation.MainThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

import ru.bshakhovsky.piano_transcription.media.utils.RandomFileArray
import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.FileName

class BothRoutines(application: Application) : AndroidViewModel(application) {

    //                              <titleId, msgStr?, msgId?>
    val alertMsg: MutableLiveData<Triple<Int, String?, Int?>> = MutableLiveData()
    val ffmpegLog: MutableLiveData<String> = MutableLiveData()
    val rawData: RandomFileArray = RandomFileArray()

    /* Uri is not WeakReference, because we need it during DecodeThread.decode(),
    and after that will need file name during TranscribeThread.makeMidi() and gamma(),
    but the WeakReference would be null for short time during orientation change
    (its lifecycle callback onDestroy will be called) */
    lateinit var mediaFile: Uri private set

    /* File name is not WeakReference, because we need it during makeMidi() and gamma(),
    but the WeakReference would be null for short time during orientation change
    (its lifecycle callback onDestroy will be called) */
    lateinit var fileName: String private set

    var youTubeLink: String? = null
        private set

    @MainThread
    fun initialize(file: Uri?, youTube: String?) {
        DebugMode.assertState(
            Looper.myLooper() == Looper.getMainLooper(),
            "DecodeThread should be initialized in MediaActivity UI-thread"
        )
        DebugMode.assertArgument(file != null)
        file?.let {
            mediaFile = it
            fileName = FileName.getName(appContext(), mediaFile)
        }
        youTube?.let { youTubeLink = it }
    }

    // File name initialized in TranscribeThread from MediaActivity UI-thread
    // All other calls are from background thread
    fun appContext(): Context = getApplication<Application>().applicationContext

    // Both threads
    fun clearCache(cachePref: String): Unit? =
        appContext().cacheDir.listFiles { _, name -> name.startsWith(cachePref) }
            ?.forEach { it.deleteRecursively() }
}