@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process

import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Record(private val context: Context) {

    var sampleRate: Int = 0
    var nChannels: Short = 0
    var nBits: Short = 0

    private var isRecording = true
    private var totalBuf = byteArrayOf()

    init {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

        var record: AudioRecord? = null
        var (byteBuf, shortBuf) = byteArrayOf() to shortArrayOf()
        fun initRecord() = intArrayOf(
            8_000, 11_025, 16_000, 22_050, 44_100, 48_000,
            88_200, 96_000, 176_400, 192_000, 352_800, 384_000
        ).reversed().forEach { rate ->
            intArrayOf(AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO)
                .reversed().forEach { channels ->
                    intArrayOf(AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT)
                        .reversed().forEach { format ->
                            AudioRecord.getMinBufferSize(rate, channels, format).also { bufSize ->
                                if (!intArrayOf(AudioRecord.ERROR, AudioRecord.ERROR_BAD_VALUE)
                                        .contains(bufSize)
                                ) {
                                    record = AudioRecord(
                                        MediaRecorder.AudioSource.MIC,
                                        AudioFormat.SAMPLE_RATE_UNSPECIFIED,
                                        channels, format, bufSize
                                    )
                                    sampleRate = (record ?: return@also).sampleRate
                                    nChannels = when (channels) {
                                        AudioFormat.CHANNEL_IN_MONO -> 1
                                        AudioFormat.CHANNEL_IN_STEREO -> 2
                                        else -> {
                                            Assert.argument(false)
                                            0
                                        }
                                    }
                                    byteBuf = ByteArray(bufSize)
                                    nBits = when (format) {
                                        AudioFormat.ENCODING_PCM_8BIT -> 8
                                        AudioFormat.ENCODING_PCM_16BIT -> {
                                            shortBuf = ShortArray(bufSize / 2)
                                            16
                                        }
                                        else -> {
                                            Assert.argument(false)
                                            0
                                        }
                                    }
                                    return
                                }
                            }
                        }
                }
        }
        initRecord()
        if ((record == null) or (record?.state != AudioRecord.STATE_INITIALIZED)
            or (byteBuf.isEmpty())
        ) {
            Assert.state(false)
            showError()
        } else record?.run {
            startRecording()
            Thread {
                while (isRecording) {
                    val samplesRead = if (shortBuf.isNotEmpty()) {
                        read(shortBuf, 0, shortBuf.size).apply {
                            ByteBuffer.wrap(byteBuf).order(ByteOrder.LITTLE_ENDIAN)
                                .asShortBuffer().put(shortBuf)
                        }
                    } else read(byteBuf, 0, byteBuf.size)
                    if (intArrayOf(
                            AudioRecord.ERROR_INVALID_OPERATION, AudioRecord.ERROR_BAD_VALUE
                        ).contains(samplesRead)
                    ) {
                        Assert.state(false)
                        showError()
                        return@Thread
                    }
                    totalBuf += byteBuf
                }
                try {
                    stop()
                } finally {
                    release()
                }
            }.start()
        }
    }

    fun finish(outStream: OutputStream) {
        isRecording = false
        with(outStream) {
            (nChannels * nBits / 8).toShort().also { blockAlign ->
                fun writeNumber(data: Number) {
                    ByteArray(
                        when (data) {
                            is Short -> 2
                            is Int -> 4
                            else -> {
                                Assert.argument(false)
                                0
                            }
                        }
                    ).also { bytes ->
                        with(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)) {
                            when (data) {
                                is Short -> asShortBuffer().put(data)
                                is Int -> asIntBuffer().put(data)
                                else -> Assert.argument(false)
                            }
                        }
                        write(bytes)
                    }
                }

                fun writeShort(data: Short) = writeNumber(data)
                fun writeInt(data: Int) = writeNumber(data)

                write("RIFF".toByteArray())
                writeInt(36 + totalBuf.size * blockAlign)
                @Suppress("SpellCheckingInspection")
                write("WAVEfmt ".toByteArray())
                // Size of format chunk = 4 shorts and 2 ints = 16 always
                writeInt(2 + 2 + 4 + 4 + 2 + 2)
                writeShort(1.toShort()) // format 1 = Pulse Code Modulation
                writeShort(nChannels)
                writeInt(sampleRate)
                writeInt(sampleRate * blockAlign)
                writeShort(blockAlign)
                writeShort(nBits)
                write("data".toByteArray())
                writeInt(totalBuf.size)
                write(totalBuf)
            }
        }
    }

    private fun showError() = MainActivity.msgDialog(context, R.string.error, R.string.micError)
}