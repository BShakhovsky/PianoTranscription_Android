package ru.bshakhovsky.piano_transcription.spectrum

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar

import kotlinx.android.synthetic.main.activity_spectrum.adSpectrum
import kotlinx.android.synthetic.main.activity_spectrum.spectrumBar

import ru.bshakhovsky.piano_transcription.R.id.menuGuide
import ru.bshakhovsky.piano_transcription.R.layout.activity_spectrum
import ru.bshakhovsky.piano_transcription.R.menu.menu_main
import ru.bshakhovsky.piano_transcription.R.string
import ru.bshakhovsky.piano_transcription.databinding.ActivitySpectrumBinding

import ru.bshakhovsky.piano_transcription.AdBanner
import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.MessageDialog

class SpectrumActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivitySpectrumBinding
    private lateinit var rawAudio: RawAudio
    private lateinit var graphs: Graphs

    override fun onCreate(savedInstanceState: Bundle?): Unit =
        super.onCreate(savedInstanceState).also {
            binding = DataBindingUtil.setContentView(this, activity_spectrum)
            rawAudio = ViewModelProvider(this).get(RawAudio::class.java)
                .apply { initialize(lifecycle, this@SpectrumActivity, cacheDir) }
            graphs = ViewModelProvider(this).get(Graphs::class.java)
            with(binding) {
                audioModel = rawAudio
                graphsModel = graphs
            }

            with(spectrumBar) {
                setSupportActionBar(this)
                DebugMode.assertState(supportActionBar != null)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                setNavigationOnClickListener(this@SpectrumActivity)
            }

            with(rawAudio) {
                if (rawData == null) {
                    DebugMode.assertState(
                        graphs.waveGraph.value == null, "Unnecessary second FFmpeg call"
                    )
                    /* TODO: Decode in background thread, otherwise SpectrumActivity is leaking
                        if device is rotated during opening the file */
                    decode(intent.getParcelableExtra("Uri"), contentResolver)
                }
                DebugMode.assertState(!probeLog.value.isNullOrEmpty())

                try {
                    rawData?.let { graphs.drawWave(it, resources) }
                } catch (e: OutOfMemoryError) {
                    getString(string.memoryRawGraph, e.localizedMessage ?: e).let { errMsg ->
                        @SuppressLint("SetTextI18n")
                        ffmpegLog.value += "\n\n$errMsg"
                        MessageDialog.show(this@SpectrumActivity, string.error, errMsg)
                    }
                }
            }

            AdBanner(lifecycle, adSpectrum)
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
        menuInflater.inflate(menu_main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        super.onOptionsItemSelected(item).also {
            when (item.itemId) {
                menuGuide -> {
                    // TODO: Spectrum --> "Guide" menu
                    Snackbar.make(
                        binding.root, "Replace with your own action", Snackbar.LENGTH_LONG
                    ).show()
                }
                else -> DebugMode.assertArgument(false)
            }
        }
}