package ru.bshakhovsky.piano_transcription.media

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View

import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider

import ru.bshakhovsky.piano_transcription.R.id.menuGuide
import ru.bshakhovsky.piano_transcription.R.layout.activity_media
import ru.bshakhovsky.piano_transcription.R.menu.menu_main
import ru.bshakhovsky.piano_transcription.R.string
import ru.bshakhovsky.piano_transcription.databinding.ActivityMediaBinding

import ru.bshakhovsky.piano_transcription.ad.AdBanner
import ru.bshakhovsky.piano_transcription.media.background.DecodeThread
import ru.bshakhovsky.piano_transcription.media.background.DecodeRoutine
import ru.bshakhovsky.piano_transcription.media.graphs.Graphs
import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage

class MediaActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var decodeRoutine: DecodeRoutine
    private lateinit var graphs: Graphs
    private lateinit var thread: DecodeThread

    private lateinit var binding: ActivityMediaBinding

    override fun onCreate(savedInstanceState: Bundle?): Unit =
        super.onCreate(savedInstanceState).also {
            with(ViewModelProvider(this)) {
                decodeRoutine = get(DecodeRoutine::class.java)
                    .apply { initialize(lifecycle, this@MediaActivity, cacheDir) }
                graphs = get(Graphs::class.java)
            }
            with(DataBindingUtil.setContentView<ActivityMediaBinding>(this, activity_media)) {
                binding = this

                audioModel = decodeRoutine
                graphsModel = graphs

                lifecycleOwner = this@MediaActivity // because of DecodeThread
            }

            with(binding) {
                with(transBar) {
                    setSupportActionBar(this)
                    DebugMode.assertState(supportActionBar != null)
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                    setNavigationOnClickListener(this@MediaActivity)
                }

                thread = DecodeThread(
                    lifecycle, this@MediaActivity, rawWave,// spectrum,
                    decodeRoutine, graphs, intent.data
                )

                AdBanner(lifecycle, applicationContext, adTrans)
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
        menuInflater.inflate(menu_main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        super.onOptionsItemSelected(item).also {
            when (item.itemId) {
                menuGuide -> InfoMessage.dialog(this, string.userGuide, string.transGuide)
                else -> DebugMode.assertArgument(false)
            }
        }
}