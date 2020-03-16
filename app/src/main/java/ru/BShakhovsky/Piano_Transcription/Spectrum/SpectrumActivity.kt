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

class SpectrumActivity : AppCompatActivity(), View.OnClickListener {

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
                try {
                    if (rawData == null) {
                        DebugMode.assertState(graphs.waveGraph == null)
                        decode()
                    }
                    DebugMode.assertState(!probeLog.isNullOrEmpty() and !ffmpegLog.isNullOrEmpty())
                    @SuppressLint("SetTextI18n")
                    text = "$probeLog\n\n$ffmpegLog"
                } catch (e: OutOfMemoryError) {
                    getString(R.string.memory, e.localizedMessage).also { errMsg ->
                        text = errMsg
                        MessageDialog.show(this@SpectrumActivity, R.string.error, errMsg)
                    }
                }
            }
        }
        rawAudio.rawData?.let { graphs.drawWave(it) }
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
                    openInputStream(u).use { inStream ->
                        DebugMode.assertState(inStream != null)
                        /* Cannot use non-seekable input pipe, because
                        for some media formats FFmpeg will write "partial file" error,
                        and if audio data is located before "codec format chunk",
                        and FFmpeg cannot seek back, it would not find audio stream.

                        I don't now how to get file path from URI,
                        so have to temporarily copy it, so that we know its path */
                        cacheDir.listFiles { _, name -> name.startsWith("FFmpeg_") }
                            ?.forEach { it.deleteRecursively() }
                        with(createTempFile(directory = createTempDir("FFmpeg_", ""))) {
                            deleteOnExit()
                            inStream?.run { writeBytes(readBytes()) }
                            if (probeLog.isNullOrEmpty()) {
                                DebugMode.assertState(
                                    ffmpegLog == null, "Wrong order of FFmpeg calls"
                                )
                                /* For FFprobe we could use input pipe with file descriptor from URI
                                But we already have path of copied file anyway: */
                                probe(path)
                            }
                            ffmpeg(path)
                        }
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuGuide -> {
                // TODO: Spectrum --> "Guide" menu
                Snackbar.make(convertLog, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .show()
            }
            else -> DebugMode.assertArgument(false)
        }
        return true
    }

    override fun onStop() {
        super.onStop()
        FFmpeg.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        graphs.waveGraph?.recycle()
    }
}