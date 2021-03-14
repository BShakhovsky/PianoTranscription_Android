package ru.bshakhovsky.piano_transcription.media.background

import android.os.Looper
import android.view.View

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.FFprobe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import ru.bshakhovsky.piano_transcription.R.string
import ru.bshakhovsky.piano_transcription.media.graphs.WaveGraph
import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.MinSec

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InterruptedIOException
import java.io.RandomAccessFile

import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteExisting

import kotlin.math.roundToLong

class DecodeRoutine : ViewModel() {

    companion object {
        const val sampleRate: Int = 16_000
        private const val cachePrefDecode = "FFmpeg_"
    }

    /* BothRoutines is not WeakReference, because we need RandomFileArray onDestroy
    to close FileChannel, and cannot be sure that the variable's destructor itself
    (its lifecycle callback) will not be called first (cleaned by GC) */
    private lateinit var data: BothRoutines
    private lateinit var probeLog: String
    val waveGraph: WaveGraph = WaveGraph()

    private val _logVis = MutableLiveData<Int>()
    val logVis: LiveData<Int> get() = _logVis

    private var decodeStarted = false
    private var pipeThread: PipeTransfer? = null

    @MainThread
    @ExperimentalPathApi
    fun initialize(d: BothRoutines) {
        DebugMode.assertState(
            Looper.myLooper() == Looper.getMainLooper(),
            "DecodeRoutine should be initialized in MediaActivity UI-thread"
        )
        data = d
        if (!decodeStarted) {
            decodeStarted = true
            viewModelScope.launch(Dispatchers.Default) { startDecode() }
        }
    }

    @MainThread
    override fun onCleared() {
        DebugMode.assertState(
            Looper.myLooper() == Looper.getMainLooper(),
            "DecodeRoutine should be cleared by MediaActivity UI-thread"
        )
        FFmpeg.cancel()
        pipeThread?.interrupt()
        data.rawData.file?.close()
        clearCache()
        super.onCleared()
    }

    @WorkerThread
    @ExperimentalPathApi
    private suspend fun startDecode() = with(data) {
        DebugMode.assertState(
            Looper.myLooper() != Looper.getMainLooper(),
            "DecodeRoutine should be started in background thread"
        )
        with(waveGraph) {
            with(rawData) {
                DebugMode.assertState(
                    (graphBitmap.value == null) and (graphDrawable.value == null)
                            and (file == null), "Unnecessary second FFmpeg call"
                )
                decode()
                try {
                    file?.let { drawWave(this) }
                } catch (e: OutOfMemoryError) {
                    with(appContext()) {
                        getString(string.memoryRawGraph, e.localizedMessage ?: e).let { errMsg ->
                            withContext(Dispatchers.Main) {
                                ffmpegLog.value += "\n\n$errMsg"
                                alertMsg.value = Triple(string.error, errMsg, null)
                            }
                        }
                    }
                }
            }
        }
    }

    @WorkerThread
    @ExperimentalPathApi
    private suspend fun decode() = with(data) {
        DebugMode.assertState(
            Looper.myLooper() != Looper.getMainLooper(),
            "Decoding should be called from background thread"
        )
        DebugMode.assertState(rawData.file == null, "Unnecessary second FFmpeg call")
        with(appContext()) {
            DebugMode.assertState(youTubeLink == null)
//            youTubeLink?.let {
//                withContext(Dispatchers.Main) { ffmpegLog.value = getString(string.youTube, it) }
//            }
            try {
                DebugMode.assertState(contentResolver != null)
                contentResolver?.run {
                    if (!::probeLog.isInitialized) {
                        DebugMode.assertState(
                            if (youTubeLink == null) ffmpegLog.value == null
                            else ffmpegLog.value != null, "Wrong order of FFmpeg calls"
                        )
                        // For FFprobe we can use non-seekable input pipe:
                        withContext(Dispatchers.IO) {
                            @Suppress("BlockingMethodInNonBlockingContext")
                            openFileDescriptor(mediaFile, "r")
                        }.use { inFile ->
                            DebugMode.assertState(inFile != null)
                            inFile?.run { probe(fd) }
                        }
                    }
                    clearCache()
                    if (PipeTransfer.error != null) return@run
                    /* For FFmpeg we cannot use non-seekable input pipe, because
                    for some media formats it will write "partial file" error,
                    and if audio data is located before "codec format chunk",
                    then FFmpeg will not be able to seek back, and it will not "find" audio stream.

                    I don't know how to get file path from URI,
                    so have to temporarily copy it, so that we know its path */
                    createTempFile(
                        createTempDirectory(cachePrefDecode), "InputMedia_"
                    ).let { copiedMedia ->
                        PipeTransfer.streamToPath(withContext(Dispatchers.IO) {
                            @Suppress("BlockingMethodInNonBlockingContext")
                            openInputStream(mediaFile)
                        }, copiedMedia)
                        PipeTransfer.error.let { e ->
                            when (e) {
                                null -> {
                                    ffmpeg(copiedMedia)
                                    DebugMode.assertState(!ffmpegLog.value.isNullOrEmpty())
                                }
                                else -> withContext(Dispatchers.Main) {
                                    ffmpegLog.value += "\n\n${
                                        when (e) {
                                            is OutOfMemoryError -> getString(
                                                string.memoryCopyFile, e.localizedMessage ?: e
                                            )
                                            is InterruptedIOException -> getString(
                                                string.fileInterrupt, e.localizedMessage ?: e
                                            )
                                            else -> (e.localizedMessage ?: e.toString()).also {
                                                DebugMode.assertState(e is IOException)
                                            }
                                        }.also {
                                            alertMsg.value = Triple(string.error, it, null)
                                        }
                                    }\n\n${getString(string.ffmpegOut)}\n\n${
                                        Config.getLastCommandOutput()
                                    }"
                                }
                            }
                        }
                    }
                }
            } catch (e: FileNotFoundException) {
                withContext(Dispatchers.Main) {
                    alertMsg.value =
                        Triple(string.noFile, "${e.localizedMessage ?: e}\n\n$mediaFile", null)
                }
            }
        }
    }

    @WorkerThread
    private suspend fun probe(fd: Int) = data.run {
        DebugMode.assertState(
            Looper.myLooper() != Looper.getMainLooper(),
            "FFprobe should be called from background thread"
        )
        DebugMode.assertState(
            !::probeLog.isInitialized and (rawData.file == null), "Unnecessary second FFprobe call"
        )
        probeLog = "$fileName\n" + with(FFprobe.getMediaInformation("pipe:$fd")) {
            appContext().run {
                if (this@with == null) getString(string.probeFail) // e.g. midi
                else ((duration?.toFloat() ?: 0f) * 1_000).roundToLong().let { dur ->
                    getString(string.probeFmt, MinSec.minutes(dur), MinSec.seconds(dur), format,
                        with(mediaProperties) {
                            if (has("tags")) with(getJSONObject("tags")) {
                                keys().asSequence().joinToString("\n") { "$it:\t${getString(it)}" }
                            } else "N/A"
                        }, streams.joinToString("\n") { with(it) { "$type $codec" } })
                }
            }
        }
        youTubeLink?.run {
            DebugMode.assertState(ffmpegLog.value != null, "YouTube Link not written")
            withContext(Dispatchers.Main) { ffmpegLog.value += "\n$probeLog" }
        } ?: run {
            DebugMode.assertState(ffmpegLog.value == null, "Unnecessary second FFprobe call")
            withContext(Dispatchers.Main) { ffmpegLog.value = probeLog }
        }
    }

    @WorkerThread
    @ExperimentalPathApi
    private suspend fun ffmpeg(inPath: Path) =
        createTempFile(inPath.parent, "DecodedRawFloatArray_", ".pcm").let {
            DebugMode.assertState(
                Looper.myLooper() != Looper.getMainLooper(),
                "FFmpeg should be called from background thread"
            )
            with(data) {
                // Raw audio float array of more than 10 minutes causes Out of Memory,
                // so, save to temp file instead of byte array
                DebugMode.assertState(rawData.file == null, "Unnecessary second FFmpeg call")
                pipeThread = PipeTransfer(it).apply {
                    pipeOut().use { outPipe ->
                        FFmpeg.cancel()
                        start()

                        when (FFmpeg.execute(
                            "-i $inPath -f f32le -ac 1 -ar $sampleRate pipe:${outPipe.fd}"
                        )) {
                            Config.RETURN_CODE_SUCCESS -> {
                                decodeSuccess(it.toFile())
                                inPath.deleteExisting()
                            }
                            Config.RETURN_CODE_CANCEL -> {
                                decodeCancelled()
                                clearCache()
                            }
                            else -> {
                                decodeFail()
                                clearCache()
                            }
                        }
                    }
                }
                DebugMode.assertState(!ffmpegLog.value.isNullOrEmpty())
                withContext(Dispatchers.Main) { _logVis.value = View.GONE }
            }
        }

    @WorkerThread
    private suspend fun decodeSuccess(outArray: File) = with(data) {
        DebugMode.assertState(
            Looper.myLooper() != Looper.getMainLooper(),
            "FFmpeg should succeed in background thread"
        )
        DebugMode.assertState(PipeTransfer.error == null)
        with(appContext()) {
            withContext(Dispatchers.Main) {
                ffmpegLog.value += "\n\n${getString(string.decodeSuccess)}"
                with(withContext(Dispatchers.IO) {
                    @Suppress("BlockingMethodInNonBlockingContext") RandomAccessFile(outArray, "r")
                }.channel) {
                    if (withContext(Dispatchers.IO) {
                            @Suppress("BlockingMethodInNonBlockingContext") size()
                        } == 0L) {
                        ffmpegLog.value += "\n${getString(string.noAudioStream)}\n${
                            getString(string.ffmpegOut)
                        }\n\n${Config.getLastCommandOutput()}"
                        alertMsg.value = Triple(string.error, null, string.noAudioStream)
                    } else rawData.file = this
                }
            }
        }
    }

    @WorkerThread
    private suspend fun decodeCancelled() = with(data) {
        DebugMode.assertState(
            Looper.myLooper() != Looper.getMainLooper(),
            "FFmpeg should be canceled from background thread"
        )
        with(appContext()) {
            withContext(Dispatchers.Main) {
                ffmpegLog.value += "\n\n${getString(string.cancelled)}"
                alertMsg.value = Triple(string.cancel, null, string.cancelled)
            }
        }
    }

    @WorkerThread
    private suspend fun decodeFail() = with(data) {
        DebugMode.assertState(
            Looper.myLooper() != Looper.getMainLooper(),
            "FFmpeg should fail in background thread"
        )
        with(appContext()) {
            withContext(Dispatchers.Main) {
                ffmpegLog.value += "\n\n${
                    with(PipeTransfer.error) {
                        when (this) {
                            null -> getString(string.decodeFail)
                            is InterruptedIOException ->
                                getString(string.fileInterrupt, localizedMessage ?: this)
                            is OutOfMemoryError ->
                                getString(string.memoryDecode, localizedMessage ?: this)
                            else -> (localizedMessage ?: toString())
                                .also { DebugMode.assertState(this is IOException) }
                        }
                    }.also { alertMsg.value = Triple(string.error, it, null) }
                }\n\n${getString(string.ffmpegOut)}\n\n${Config.getLastCommandOutput()}"
            }
        }
    }

    // Both threads
    private fun clearCache() = data.clearCache(cachePrefDecode)
}