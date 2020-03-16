@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.Spectrum

import android.content.Context
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.FFprobe
import ru.BShakhovsky.Piano_Transcription.R
import ru.BShakhovsky.Piano_Transcription.Utils.DebugMode
import ru.BShakhovsky.Piano_Transcription.Utils.MessageDialog
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RawAudio(private val context: Context) {

    companion object {
        const val sampleRate: Int = 16_000
    }

    var probeLog: String? = null
        private set
    var ffmpegLog: String? = null
        private set

    var rawData: FloatArray? = null
        private set

    fun probe(path: String) {
        DebugMode.assertState(
            (probeLog == null) and (ffmpegLog == null) and (rawData == null),
            "Unnecessary second FFprobe call"
        )
        probeLog = with(FFprobe.getMediaInformation(path)) {
            context.run {
                @Suppress("IfThenToElvis")
                if (this@with == null) getString(R.string.probeFail) // e.g. midi
                else getString(R.string.probeFmt, (duration ?: 0) / 1_000, format,
                    with(metadataEntries) {
                        if (isEmpty()) "N/A"
                        else joinToString("\n") { with(it) { "$key:\t$value" } }
                    }, streams.joinToString("\n") { with(it) { "$type $fullCodec" } })
            }
        }
    }

    fun ffmpeg(path: String) {
        DebugMode.assertState(rawData == null, "Unnecessary second FFmpeg call")
        ByteArrayOutputStream().also { outArray ->
            PipeTransfer(outArray).also { thread ->
                thread.pipeTo().use { outPipe ->
                    FFmpeg.cancel()
                    thread.start()

                    when (FFmpeg.execute(
                        "-i $path -f f32le -ac 1 -ar $sampleRate pipe:${outPipe.fd}"
                    )) {
                        Config.RETURN_CODE_CANCEL -> {
                            ffmpegLog = "\n\n${context.getString(R.string.cancelled)}"
                            MessageDialog.show(context, R.string.cancel, R.string.cancelled)
                        }

                        Config.RETURN_CODE_SUCCESS -> decodeSuccess(outArray, thread)

                        else -> decodeFail(thread.error)
                    }
                }
            }
        }
        DebugMode.assertState(!ffmpegLog.isNullOrEmpty())
    }

    private fun decodeSuccess(outArray: ByteArrayOutputStream, thread: PipeTransfer) {
        DebugMode.assertState(thread.error == null)
        ffmpegLog = context.getString(R.string.decodeSuccess)

        if (outArray.size() == 0) {
            ffmpegLog += with(context) {
                "\n${getString(R.string.noAudioStream)}\n${
                getString(R.string.ffmpegOut)}\n\n${Config.getLastCommandOutput()}"
            }
            MessageDialog.show(context, R.string.error, R.string.noAudioStream)
            return
        }

        rawData = FloatArray(outArray.size() / 4)
        ByteBuffer.wrap(outArray.toByteArray())
            .order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(rawData)
    }

    private fun decodeFail(error: Throwable?) {
        with(context) {
            with(error) {
                when (this) {
                    null -> getString(R.string.decodeFail)
                    is OutOfMemoryError -> getString(R.string.memory, localizedMessage ?: this)
                    else -> {
                        DebugMode.assertState(this is IOException)
                        localizedMessage ?: toString()
                    }
                }
            }.also { errMsg ->
                ffmpegLog = "$errMsg\n${getString(R.string.ffmpegOut)
                }\n\n${Config.getLastCommandOutput()}"
                MessageDialog.show(context, R.string.error, errMsg)
            }
        }
    }
}