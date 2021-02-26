package ru.bshakhovsky.piano_transcription.media

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle

import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils

import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider

import ru.bshakhovsky.piano_transcription.R.anim.anim_graph
import ru.bshakhovsky.piano_transcription.R.id.menuGuide
import ru.bshakhovsky.piano_transcription.R.layout.activity_media
import ru.bshakhovsky.piano_transcription.R.menu.menu_main
import ru.bshakhovsky.piano_transcription.R.string
import ru.bshakhovsky.piano_transcription.databinding.ActivityMediaBinding

import ru.bshakhovsky.piano_transcription.ad.AdBanner
import ru.bshakhovsky.piano_transcription.media.background.BothRoutines
import ru.bshakhovsky.piano_transcription.media.background.DecodeRoutine
import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage

import kotlin.io.path.ExperimentalPathApi

class MediaActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var bothRoutines: BothRoutines
    private lateinit var decodeRoutine: DecodeRoutine

    private lateinit var binding: ActivityMediaBinding

    @ExperimentalPathApi
    override fun onCreate(savedInstanceState: Bundle?): Unit =
        super.onCreate(savedInstanceState).also {
            with(ViewModelProvider(this)) {
                bothRoutines = get(BothRoutines::class.java)
                    .apply { with(intent) { initialize(data, getStringExtra("YouTube Link")) } }
                decodeRoutine = get(DecodeRoutine::class.java).apply { initialize(bothRoutines) }
            }
            with(DataBindingUtil.setContentView<ActivityMediaBinding>(this, activity_media)) {
                commonModel = bothRoutines
                decodeModel = decodeRoutine

                binding = this
                lifecycleOwner = this@MediaActivity // because of DecodeThread
            }

            showErrors()
            decodeObserve()

            with(binding) {
                with(transBar) {
                    setSupportActionBar(this)
                    DebugMode.assertState(supportActionBar != null)
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                    setNavigationOnClickListener(this@MediaActivity)
                }

                AdBanner(lifecycle, applicationContext, adTrans)
            }
        }

    private fun showErrors() = with(bothRoutines) { // Keep showing until Ok clicked
        alertMsg.observe(this@MediaActivity) { msg ->
            with(msg) {
                if ((second == null) and (third == null)) {
                    DebugMode.assertState(first == 0)
                    return@observe
                }
                second?.let { str ->
                    // Not applicationContext, otherwise java.lang.IllegalStateException:
                    // You need to use Theme.AppCompat (or descendant) with this activity
                    InfoMessage.dialog(this@MediaActivity, first, str)
                    { alertMsg.value = Triple(0, null, null) }
                } ?: InfoMessage.dialog( // --//--
                    this@MediaActivity, first,
                    third ?: DebugMode.assertState(false).let { return@observe }
                ) { alertMsg.value = Triple(0, null, null) }
            }
        }
    }

    private fun decodeObserve() = with(decodeRoutine) {
        with(waveGraph) {
            graphBitmap.observe(this@MediaActivity) {
                graphDrawable.value = BitmapDrawable(resources, it)
                binding.rawWave
                    .startAnimation(AnimationUtils.loadAnimation(applicationContext, anim_graph))
                with(bothRoutines) {
                    if (rawData != null) {
                        DebugMode.assertState(
                            // (drawable.bitmap != null) and
                            (ffmpegLog.value != null) and (logVis.value == View.GONE)
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