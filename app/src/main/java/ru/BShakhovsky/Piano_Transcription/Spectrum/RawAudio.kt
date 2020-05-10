@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.Spectrum

import android.content.Context
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.FFprobe
import ru.BShakhovsky.Piano_Transcription.R
import ru.BShakhovsky.Piano_Transcription.Utils.DebugMode
import ru.BShakhovsky.Piano_Transcription.Utils.MessageDialog
import ru.BShakhovsky.Piano_Transcription.Utils.MinSec

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

import java.nio.channels.FileChannel

class RawAudio(private val context: Context) {

    companion object {
        const val sampleRate: Int = 16_000
    }

    var probeLog: String? = null
        private set
    var ffmpegLog: String? = null
        private set

    // TODO: delete raw audio file after mel-spectrum is calculated
    var rawData: FileChannel? = null
        private set

    fun probe(fd: Int) {
        DebugMode.assertState(
            (probeLog == null) and (ffmpegLog == null) and (rawData == null),
            "Unnecessary second FFprobe call"
        )
        probeLog = with(FFprobe.getMediaInformation("pipe:$fd")) {
            context.run {
                @Suppress("IfThenToElvis")
                if (this@with == null) getString(R.string.probeFail) // e.g. midi
                else (duration ?: 0).let { dur ->
                    getString(R.string.probeFmt, MinSec.minutes(dur), MinSec.seconds(dur), format,
                        with(metadataEntries) {
                            if (isEmpty()) "N/A"
                            else joinToString("\n") { with(it) { "$key:\t$value" } }
                        }, streams.joinToString("\n") { with(it) { "$type $fullCodec" } })
                }
            }
        }
    }

    fun ffmpeg(inFile: File): Unit = createTempFile(directory = inFile.parentFile).let {
        // Raw audio float array of more than 10 minutes causes Out of Memory,
        // so, save to temp file instead of byte array
        DebugMode.assertState(rawData == null, "Unnecessary second FFmpeg call")
        PipeTransfer(it).also { thread ->
            thread.pipeOut().use { outPipe ->
                FFmpeg.cancel()
                thread.start()

                when (FFmpeg.execute(
                    "-i ${inFile.path} -f f32le -ac 1 -ar $sampleRate pipe:${outPipe.fd}"
                )) {
                    Config.RETURN_CODE_CANCEL -> {
                        ffmpegLog = "\n\n${context.getString(R.string.cancelled)}"
                        MessageDialog.show(context, R.string.cancel, R.string.cancelled)
                        deleteTempDir(inFile)
                    }

                    Config.RETURN_CODE_SUCCESS -> {
                        decodeSuccess(it)
                        // deleteTempDir(inFile)
                        inFile.delete()
                    }

                    else -> {
                        decodeFail()
                        deleteTempDir(inFile)
                    }
                }
            }
        }
        DebugMode.assertState(!ffmpegLog.isNullOrEmpty())
    }

    private fun decodeSuccess(outArray: File) = with(context) {
        DebugMode.assertState(PipeTransfer.error == null)
        ffmpegLog = getString(R.string.decodeSuccess)
        with(RandomAccessFile(outArray, "r").channel) {
            if (size() == 0L) {
                ffmpegLog += "\n${getString(R.string.noAudioStream)}\n${
                getString(R.string.ffmpegOut)}\n\n${Config.getLastCommandOutput()}"
                MessageDialog.show(context, R.string.error, R.string.noAudioStream)
            } else rawData = this
        }
    }

    private fun decodeFail() = with(context) {
        ffmpegLog = "${with(PipeTransfer.error) {
            when (this) {
                null -> getString(R.string.decodeFail)
                is OutOfMemoryError -> getString(R.string.memoryDecode, localizedMessage ?: this)
                else -> (localizedMessage ?: toString())
                    .also { DebugMode.assertState(this is IOException) }
            }
        }.also { MessageDialog.show(context, R.string.error, it) }}\n\n${
        getString(R.string.ffmpegOut)}\n\n${Config.getLastCommandOutput()}"
    }

    private fun deleteTempDir(file: File) = file.parentFile.let {
        DebugMode.assertState(it != null)
        it?.deleteRecursively()
    }
}