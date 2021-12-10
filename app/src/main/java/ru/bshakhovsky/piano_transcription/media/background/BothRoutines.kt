package ru.bshakhovsky.piano_transcription.media.background

import android.app.Application
import android.net.Uri
import android.os.Looper

import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData

import ru.bshakhovsky.piano_transcription.media.utils.RandomFileArray
import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.FileName
import ru.bshakhovsky.piano_transcription.utils.VmAppContext

class BothRoutines(application: Application) :
    VmAppContext(application) { /* appContext():
        File name initialized in TranscribeThread from MediaActivity UI-thread
        All other calls are from background thread */

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

/* TODO
    // Both threads
    suspend fun showAndPrintError(
        @StringRes errTitleId: Int, errMsgStr: String?, @StringRes errMsgId: Int?,
        ffmpegOutput: Boolean = false
    ): Unit = with(appContext()) {
        withContext(Dispatchers.Main) {
            ffmpegLog.value += "\n\n${
                errMsgStr ?: getString(errMsgId ?: string.error
                    .also { DebugMode.assertArgument(false) })
            }"
            alertMsg.value = Triple(errTitleId, errMsgStr, errMsgId)
            if (ffmpegOutput) ffmpegLog.value +=
                "\n\n${getString(string.ffmpegOut)}\n\n${Config.getLastCommandOutput()}"
        }
    }
*/

    // Both threads
    fun clearCache(cachePref: String): Unit? =
        appContext().cacheDir.listFiles { _, name -> name.startsWith(cachePref) }
            ?.forEach { it.delete/*Recursively*/() }
}