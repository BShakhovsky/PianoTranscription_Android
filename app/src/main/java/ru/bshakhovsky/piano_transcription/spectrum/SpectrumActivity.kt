package ru.bshakhovsky.piano_transcription.spectrum

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar

import kotlinx.android.synthetic.main.activity_spectrum.adSpectrum
import kotlinx.android.synthetic.main.activity_spectrum.rawWave
import kotlinx.android.synthetic.main.activity_spectrum.spectrumBar

import ru.bshakhovsky.piano_transcription.R.id.menuGuide
import ru.bshakhovsky.piano_transcription.R.layout.activity_spectrum
import ru.bshakhovsky.piano_transcription.R.menu.menu_main
import ru.bshakhovsky.piano_transcription.databinding.ActivitySpectrumBinding

import ru.bshakhovsky.piano_transcription.ad.AdBanner
import ru.bshakhovsky.piano_transcription.utils.DebugMode

class SpectrumActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var rawAudio: RawAudio
    private lateinit var graphs: Graphs
    private lateinit var thread: DecodeThread

    override fun onCreate(savedInstanceState: Bundle?): Unit =
        super.onCreate(savedInstanceState).also {
            rawAudio = ViewModelProvider(this).get(RawAudio::class.java)
                .apply { initialize(lifecycle, this@SpectrumActivity, cacheDir) }
            graphs = ViewModelProvider(this).get(Graphs::class.java)
            with(DataBindingUtil.setContentView<ActivitySpectrumBinding>(this, activity_spectrum)) {
                audioModel = rawAudio
                graphsModel = graphs

                lifecycleOwner = this@SpectrumActivity // because of DecodeThread
            }

            with(spectrumBar) {
                setSupportActionBar(this)
                DebugMode.assertState(supportActionBar != null)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                setNavigationOnClickListener(this@SpectrumActivity)
            }

            thread = DecodeThread(
                lifecycle, this, rawWave,// spectrum,
                rawAudio, graphs, intent.getParcelableExtra("Uri")
            )

            AdBanner(lifecycle, adSpectrum, applicationContext)
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
                    Snackbar.make(adSpectrum, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .show()
                }
                else -> DebugMode.assertArgument(false)
            }
        }
}