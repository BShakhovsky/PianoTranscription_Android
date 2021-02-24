package ru.bshakhovsky.piano_transcription.media.background

import android.content.Context
import android.net.Uri
import android.view.View

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.FFprobe

import ru.bshakhovsky.piano_transcription.R.string

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage
import ru.bshakhovsky.piano_transcription.utils.MinSec
import ru.bshakhovsky.piano_transcription.utils.WeakPtr

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InterruptedIOException
import java.io.RandomAccessFile

import java.nio.channels.FileChannel

import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteExisting

import kotlin.math.roundToLong

class DecodeRoutine : ViewModel() {

    companion object {
        const val sampleRate: Int = 16_000

        private const val cachePref = "FFmpeg_"
    }

    private lateinit var context: WeakPtr<Context>

    /* File is not WeakReference, because we need this variable onDestroy for clearing cache,
    and cannot be sure that the variable's destructor itself
    (its lifecycle callback) will not be called first (cleaned by GC) */
    private lateinit var cacheDir: File

    private lateinit var probeLog: String
    val ffmpegLog: MutableLiveData<String> = MutableLiveData()

    private val _logVis = MutableLiveData<Int>()
    val logVis: LiveData<Int>
        get() = _logVis

    // TODO: Delete decoded raw audio file after mel-spectrum is calculated
    var rawData: FileChannel? = null
        private set

    fun initialize(lifecycle: Lifecycle, c: Context, cd: File) {
        context = WeakPtr(lifecycle, c)
        cacheDir = cd
    }

    override fun onCleared() {
        FFmpeg.cancel()
        rawData?.close()
        clearCache()
        super.onCleared()
    }

    @ExperimentalPathApi
    fun decode(uri: Uri) {
        DebugMode.assertState(rawData == null, "Unnecessary second FFmpeg call")
        with(context.get()) {
            try {
                DebugMode.assertState(contentResolver != null)
                contentResolver?.run {
                    if (!::probeLog.isInitialized) {
                        DebugMode.assertState(
                            ffmpegLog.value == null, "Wrong order of FFmpeg calls"
                        )
                        // For FFprobe we can use non-seekable input pipe:
                        openFileDescriptor(uri, "r").use { inFile ->
                            DebugMode.assertState(inFile != null)
                            inFile?.run { probe(fd) }
                        }
                    }
                    clearCache()
                    if (PipeTransfer.error != null) return
                    /* For FFmpeg we cannot use non-seekable input pipe, because
                    for some media formats it will write "partial file" error,
                    and if audio data is located before "codec format chunk",
                    then FFmpeg will not be able to seek back, and it will not "find" audio stream.

                    I don't know how to get file path from URI,
                    so have to temporarily copy it, so that we know its path */
                    createTempFile(
                        createTempDirectory(cachePref), "InputMedia_"
                    ).let { copiedMedia ->
                        PipeTransfer.streamToFile(openInputStream(uri), copiedMedia)
                        PipeTransfer.error.let { e ->
                            when (e) {
                                null -> {
                                    ffmpeg(copiedMedia)
                                    DebugMode.assertState(!ffmpegLog.value.isNullOrEmpty())
                                }
                                else -> {
                                    ffmpegLog.value += "\n\n${
                                        when (e) {
                                            is OutOfMemoryError -> getString(
                                                string.memoryCopyFile, e.localizedMessage ?: e
                                            )
                                            is InterruptedIOException -> getString(
                                                string.fileInterrupt, e.localizedMessage ?: e
                                            )
                                            else -> (e.localizedMessage ?: e.toString())
                                                .also { DebugMode.assertState(e is IOException) }
                                        }.also { InfoMessage.dialog(this@with, string.error, it) }
                                    }\n\n${
                                        getString(string.ffmpegOut)
                                    }\n\n${Config.getLastCommandOutput()}"
                                }
                            }
                        }
                    }
                }
            } catch (e: FileNotFoundException) {
                InfoMessage.dialog(this, string.noFile, "${e.localizedMessage ?: e}\n\n$uri")
            }
        }
    }

    private fun probe(fd: Int) {
        DebugMode.assertState(
            !::probeLog.isInitialized and (rawData == null), "Unnecessary second FFprobe call"
        )
        probeLog = with(FFprobe.getMediaInformation("pipe:$fd")) {
            context.get().run {
                @Suppress("IfThenToElvis")
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
        DebugMode.assertState(ffmpegLog.value == null, "Unnecessary second FFprobe call")
        ffmpegLog.value = probeLog
    }

    @ExperimentalPathApi
    private fun ffmpeg(inPath: Path) =
        createTempFile(inPath.parent, "DecodedRawFloatArray_", ".pcm").let {
            // Raw audio float array of more than 10 minutes causes Out of Memory,
            // so, save to temp file instead of byte array
            DebugMode.assertState(rawData == null, "Unnecessary second FFmpeg call")
            PipeTransfer(it).also { thread ->
                thread.pipeOut().use { outPipe ->
                    FFmpeg.cancel()
                    thread.start()

                    when (FFmpeg.execute(
                        "-i $inPath -f f32le -ac 1 -ar $sampleRate pipe:${outPipe.fd}"
                    )) {
                        Config.RETURN_CODE_SUCCESS -> {
                            decodeSuccess(it.toFile())
                            // TODO: Delete decoded raw audio file after mel-spectrum is calculated
                            inPath.deleteExisting()
                        }
                        Config.RETURN_CODE_CANCEL -> {
                            with(context.get()) {
                                ffmpegLog.value += "\n\n${getString(string.cancelled)}"
                                InfoMessage.dialog(this, string.cancel, string.cancelled)
                            }
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
            _logVis.value = View.GONE
        }

    private fun decodeSuccess(outArray: File) = context.get().run {
        DebugMode.assertState(PipeTransfer.error == null)
        ffmpegLog.value += "\n\n${getString(string.decodeSuccess)}"
        with(RandomAccessFile(outArray, "r").channel) {
            if (size() == 0L) {
                ffmpegLog.value += "\n${getString(string.noAudioStream)}\n${
                    getString(string.ffmpegOut)
                }\n\n${Config.getLastCommandOutput()}"
                InfoMessage.dialog(this@run, string.error, string.noAudioStream)
            } else rawData = this
        }
    }

    private fun decodeFail() = with(context.get()) {
        ffmpegLog.value += "\n\n${
            with(PipeTransfer.error) {
                when (this) {
                    null -> getString(string.decodeFail)
                    is InterruptedIOException ->
                        getString(string.fileInterrupt, localizedMessage ?: this)
                    is OutOfMemoryError -> getString(string.memoryDecode, localizedMessage ?: this)
                    else -> (localizedMessage ?: toString())
                        .also { DebugMode.assertState(this is IOException) }
                }
            }.also { InfoMessage.dialog(this, string.error, it) }
        }\n\n${getString(string.ffmpegOut)}\n\n${Config.getLastCommandOutput()}"
    }

    private fun clearCache() = cacheDir.listFiles { _, name -> name.startsWith(cachePref) }
        ?.forEach { it.deleteRecursively() }
}