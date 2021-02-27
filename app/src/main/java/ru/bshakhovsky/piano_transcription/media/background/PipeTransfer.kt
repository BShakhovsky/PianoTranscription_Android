package ru.bshakhovsky.piano_transcription.media.background

import android.os.ParcelFileDescriptor
import androidx.annotation.CheckResult

import ru.bshakhovsky.piano_transcription.utils.DebugMode

import java.io.InputStream
import java.io.InterruptedIOException

import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.appendBytes

import kotlin.math.pow

class PipeTransfer(private val outPath: Path) : Thread() {

    companion object {
        var error: Throwable? = null
            private set

        @ExperimentalPathApi
        fun streamToPath(inStream: InputStream?, outPath: Path) {
            DebugMode.assertState(inStream != null)
            inStream?.use { inS ->
                try {
                    // TODO: reduce buffer from 64 Mb to smaller size
                    ByteArray(64 * 1_024f.pow(2).toInt()).also { buf ->
                        var len: Int
                        while (inS.read(buf).also { len = it } > 0)
                            if (interrupted()) throw InterruptedIOException()
                            else outPath.appendBytes(buf.sliceArray(0 until len))
                        DebugMode.assertState(len in arrayOf(0, -1))
                    }
                } catch (e: Throwable) {
                    error = e
                } finally {
                    inS.close()
                }
            }
        }
    }

    private var inStream: ParcelFileDescriptor.AutoCloseInputStream? = null

    @CheckResult
    fun pipeOut(): ParcelFileDescriptor = ParcelFileDescriptor.createPipe().let { pipe ->
        DebugMode.assertState(inStream == null, "pipeOut() must be called just once")
        inStream = ParcelFileDescriptor.AutoCloseInputStream(pipe[0])
        pipe[1]
    }

    @ExperimentalPathApi
    override fun run(): Unit = streamToPath(inStream, outPath)
        .also { DebugMode.assertState(inStream != null, "Did you call pipeOut() before start() ?") }
}