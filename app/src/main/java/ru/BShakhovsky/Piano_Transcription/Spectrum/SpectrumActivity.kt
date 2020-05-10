@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.Spectrum

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.arthenica.mobileffmpeg.FFmpeg
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import ru.BShakhovsky.Piano_Transcription.AdBanner
import ru.BShakhovsky.Piano_Transcription.Utils.DebugMode
import ru.BShakhovsky.Piano_Transcription.R
import ru.BShakhovsky.Piano_Transcription.Utils.MessageDialog
import java.io.FileNotFoundException
import java.io.IOException

class SpectrumActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        private const val cachePref = "FFmpeg_"
    }

    private lateinit var convertLog: TextView

    private lateinit var rawAudio: RawAudio
    private lateinit var graphs: Graphs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rawAudio = RawAudio(this)
        graphs = Graphs()
        createLayout()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        createLayout()
    }

    private fun createLayout() {
        setContentView(R.layout.activity_spectrum)
        convertLog = findViewById(R.id.textLog)

        with(findViewById<Toolbar>(R.id.spectrumBar)) {
            setSupportActionBar(this)
            DebugMode.assertState(supportActionBar != null)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            setNavigationOnClickListener(this@SpectrumActivity)
        }

        with(rawAudio) {
            with(convertLog) {
                if (rawData == null) {
                    DebugMode.assertState(graphs.waveGraph == null)
                    decode()
                }
                DebugMode.assertState(!probeLog.isNullOrEmpty())
                @SuppressLint("SetTextI18n")
                text = "$probeLog\n\n${ffmpegLog ?: with(PipeTransfer.error) {
                    when (this) {
                        is OutOfMemoryError ->
                            getString(R.string.memoryCopyFile, localizedMessage ?: this)
                        else -> {
                            DebugMode.assertState(this is IOException)
                            this?.localizedMessage ?: toString()
                        }
                    }.also { MessageDialog.show(context, R.string.error, it) }
                }}"

                try {
                    rawData?.let { graphs.drawWave(it) }
                } catch (e: OutOfMemoryError) {
                    getString(R.string.memoryRawGraph, e.localizedMessage ?: e).let { errMsg ->
                        @SuppressLint("SetTextI18n")
                        text = "$text\n\n$errMsg"
                        MessageDialog.show(context, R.string.error, errMsg)
                    }
                }
            }
        }
        findViewById<ImageView>(R.id.rawWave).setImageBitmap(graphs.waveGraph)

        MobileAds.initialize(this)
        AdBanner(findViewById(R.id.adSpectrum))
    }

    private fun decode(): Unit = with(rawAudio) {
        DebugMode.assertState(rawData == null, "Unnecessary second FFmpeg call")
        intent.getParcelableExtra<Uri>("Uri").also { uri ->
            DebugMode.assertState((uri != null) and (contentResolver != null))
            uri?.let { u ->
                contentResolver?.run {
                    try {
                        if (probeLog.isNullOrEmpty()) {
                            DebugMode.assertState(ffmpegLog == null, "Wrong order of FFmpeg calls")
                            // For FFprobe we can use non-seekable input pipe:
                            openFileDescriptor(u, "r").use { inFile ->
                                DebugMode.assertState(inFile != null)
                                inFile?.run { probe(fd) }
                            }
                        }
                        clearCache()
                        if (PipeTransfer.error != null) return
                        /* For FFmpeg we cannot use non-seekable input pipe, because
                        for some media formats it will write "partial file" error,
                        and if audio data is located before "codec format chunk",
                        then FFmpeg will not be able to seek back,
                        and it will not "find" audio stream.

                        I don't know how to get file path from URI,
                        so have to temporarily copy it, so that we know its path */
                        createTempFile(directory = createTempDir(cachePref, "")).let {
                            PipeTransfer.streamToFile(openInputStream(u), it)
                            if (PipeTransfer.error == null) {
                                ffmpeg(it)
                                DebugMode.assertState(!ffmpegLog.isNullOrEmpty())
                            }
                        }
                    } catch (e: FileNotFoundException) {
                        MessageDialog.show(
                            this@SpectrumActivity, R.string.noFile,
                            "${e.localizedMessage ?: e}\n\n$uri"
                        )
                    }
                }
            }
        }
    }

    override fun onClick(view: View?) {
        DebugMode.assertArgument(view != null)
        when (view?.id) {
            -1 -> onBackPressed() // not android.R.id.home
            else -> DebugMode.assertState(false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean = super.onCreateOptionsMenu(menu).also {
        DebugMode.assertArgument(menu != null)
        menuInflater.inflate(R.menu.menu_main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        super.onOptionsItemSelected(item).also {
            when (item.itemId) {
                R.id.menuGuide -> {
                    // TODO: Spectrum --> "Guide" menu
                    Snackbar.make(convertLog, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .show()
                }
                else -> DebugMode.assertArgument(false)
            }
        }

    override fun onStop() {
        super.onStop()
        FFmpeg.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        graphs.waveGraph?.recycle()
        rawAudio.rawData?.close()
        clearCache()
    }

    private fun clearCache() = cacheDir.listFiles { _, name -> name.startsWith(cachePref) }
        ?.forEach { it.deleteRecursively() }
}