@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.Spectrum

import android.os.ParcelFileDescriptor
import ru.BShakhovsky.Piano_Transcription.Utils.DebugMode
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.pow

class PipeTransfer(private val outStream: OutputStream) : Thread() {

    companion object {
        var error: Throwable? = null
            private set

        fun copyStream(inStream: InputStream?, outStream: OutputStream) {
            DebugMode.assertState(inStream != null)
            inStream?.use { inS ->
                try {
                    // TODO: reduce buffer from 128 Mb to smaller size
                    ByteArray(128 * 1_024f.pow(2).toInt()).also { buf ->
                        var len: Int
                        while (inS.read(buf).also { len = it } > 0) outStream.write(buf, 0, len)
                        DebugMode.assertState(len in arrayOf(0, -1))
                    }
                } catch (e: Throwable) {
                    error = e
                } finally {
                    inS.close()
                    outStream.close()
                }
            }
        }
    }

    private var inStream: ParcelFileDescriptor.AutoCloseInputStream? = null

    fun pipeTo(): ParcelFileDescriptor = ParcelFileDescriptor.createPipe().let { pipe ->
        DebugMode.assertState(inStream == null, "pipeTo() must be called just once")
        inStream = ParcelFileDescriptor.AutoCloseInputStream(pipe[0])
        pipe[1]
    }

    override fun run() {
        DebugMode.assertState(inStream != null, "Did you call pipeTo() before start() ?")
        copyStream(inStream, outStream)
    }
}