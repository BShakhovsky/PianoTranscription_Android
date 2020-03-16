@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.Spectrum

import android.os.ParcelFileDescriptor
import ru.BShakhovsky.Piano_Transcription.Utils.DebugMode
import java.io.OutputStream

class PipeTransfer(private val outStream: OutputStream) : Thread() {

    var error: Throwable? = null
        private set

    private var inStream: ParcelFileDescriptor.AutoCloseInputStream? = null

    fun pipeTo(): ParcelFileDescriptor = ParcelFileDescriptor.createPipe().let { pipe ->
        DebugMode.assertState(inStream == null, "pipeTo() must be called just once")
        inStream = ParcelFileDescriptor.AutoCloseInputStream(pipe[0])
        DebugMode.assertState(inStream != null)
        pipe[1]
    }

    override fun run() {
        DebugMode.assertState(inStream != null, "Did you call pipeTo() before start() ?")
        inStream?.use { inS ->
            try {
                ByteArray(1_024).also { buf ->
                    var len: Int
                    while (inS.read(buf).also { len = it } > 0)
                        outStream.write(buf, 0, len)
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