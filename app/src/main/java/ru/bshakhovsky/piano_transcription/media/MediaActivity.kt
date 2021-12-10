package ru.bshakhovsky.piano_transcription.media

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle

import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider

import com.google.android.material.snackbar.Snackbar

import ru.bshakhovsky.piano_transcription.R.anim.anim_graph
import ru.bshakhovsky.piano_transcription.R.id
import ru.bshakhovsky.piano_transcription.R.layout.activity_media
import ru.bshakhovsky.piano_transcription.R.menu.menu_main
import ru.bshakhovsky.piano_transcription.R.string
import ru.bshakhovsky.piano_transcription.databinding.ActivityMediaBinding

import ru.bshakhovsky.piano_transcription.ad.AdBanner
import ru.bshakhovsky.piano_transcription.media.background.BothRoutines
import ru.bshakhovsky.piano_transcription.media.background.DecodeRoutine
import ru.bshakhovsky.piano_transcription.media.background.TranscribeRoutine
import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage
import ru.bshakhovsky.piano_transcription.utils.MinSec
import ru.bshakhovsky.piano_transcription.utils.Share

class MediaActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityMediaBinding

    private lateinit var bothRoutines: BothRoutines
    private lateinit var decodeRoutine: DecodeRoutine
    private lateinit var transRoutine: TranscribeRoutine

    private val writeMidi =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            with(result) {
                if (resultCode != RESULT_OK) Snackbar.make(
                    binding.root,
                    getString(string.notSaved, getString(string.justMidi)), Snackbar.LENGTH_LONG
                ).setAction(string.saveMidi) { midiOutFile() }.show() else {
                    DebugMode.assertState((data != null) and (data?.data != null))
                    data?.data?.let { uri ->
                        contentResolver.openOutputStream(uri).let { outStream ->
                            DebugMode.assertState(outStream != null)
                            outStream?.let { transRoutine.saveMidi(uri, it) }
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?): Unit =
        super.onCreate(savedInstanceState).also {
            with(ViewModelProvider(this)) {
                bothRoutines = get(BothRoutines::class.java)
                    .apply { with(intent) { initialize(data, getStringExtra("YouTube Link")) } }
                decodeRoutine = get(DecodeRoutine::class.java).apply { initialize(bothRoutines) }
                transRoutine = get(TranscribeRoutine::class.java).apply { initialize(bothRoutines) }
            }
            with(DataBindingUtil.setContentView<ActivityMediaBinding>(this, activity_media)) {
                commonModel = bothRoutines
                decodeModel = decodeRoutine
                transModel = transRoutine

                binding = this
                lifecycleOwner = this@MediaActivity // because of DecodeThread
            }

            showErrors()
            decodeObserve()
            transObserve()
            midiObserve()

            with(binding) {
                with(transBar) {
                    setSupportActionBar(this)
                    DebugMode.assertState(supportActionBar != null)
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                    setNavigationOnClickListener(this@MediaActivity)
                }
                AdBanner(lifecycle, applicationContext, adTrans, string.bannerTrans)
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
            graphBitmap.observe(this@MediaActivity) { bitmap ->
                graphDrawable.value = BitmapDrawable(resources, bitmap)
                with(binding) {
                    arrayOf(rawWave, roll).forEach {
                        it.startAnimation(
                            AnimationUtils.loadAnimation(applicationContext, anim_graph)
                        )
                    }
                }
                with(bothRoutines) {
                    if (rawData.file != null) {
                        DebugMode.assertState(
                            // (drawable.bitmap != null) and
                            (ffmpegLog.value != null) and (logVis.value == View.GONE)
                        )
                        // Yes, would be clearer to make RandomFileArray
                        // as LiveData and observe it, but I am too lazy to do it
                        transRoutine.startTranscribe()
                    }
                }
            }
        }
    }

    private fun transObserve() = with(transRoutine.rollGraph) {
        graphBitmap.observe(this@MediaActivity) { bitmap ->
            graphDrawable.value = BitmapDrawable(resources, bitmap)
            rollDur.value = (bitmap.width * 1_000L * TranscribeRoutine.hopSize
                    / DecodeRoutine.sampleRate).let { milSec ->
                getString(string.duration, MinSec.minutes(milSec), MinSec.seconds(milSec))
            }
        }
        /* If decodeThread.super.onCleared() called 3 times
            (device screen rotated 3 times), this MediaActivity recreates itself
            and all ViewModel data is lost, and transcription starts all-over again.
        Anyway, no real need to delete DecodeRawFloatArray from cache folder ASAP,
            it is Ok for it to be cleared automatically later *//*
        isTranscribed.observe(this@MediaActivity) {
            if (it) decodeThread.onCleared()
            DebugMode.assertState(decodeThread.decodeStarted)
        } */
    }

    private fun midiObserve() = with(transRoutine) {
        midiSaveStart.observe(this@MediaActivity) {
            DebugMode.assertState(savedMidi.value == null)
            DebugMode.assertState(rollGraph.isTranscribed.value == true)
            midiOutFile() // SingleLiveEvent, don't save again after orientation change
        }
        savedMidi.observe(this@MediaActivity)
        { setResult(Activity.RESULT_OK, Intent().apply { data = it }) }
    }

    override fun onBackPressed(): Unit = with(transRoutine) {
        rollGraph.isTranscribed.value.let {
            when {
                transStarted and (it == false) -> InfoMessage.dialog(
                    // Not applicationContext, otherwise java.lang.IllegalStateException:
                    // You need to use Theme.AppCompat (or descendant) with this activity
                    this@MediaActivity, string.warning, string.transInProgress,
                    string.contTrans, true, { super.onBackPressed() })
                (it == true) and (savedMidi.value == null) -> InfoMessage.dialog(
                    /* --//-- */this@MediaActivity, string.warning,
                    getString(string.notSaved, getString(string.justMidi)),
                    string.saveMidi, true, { super.onBackPressed() }) { midiOutFile() }
                else -> super.onBackPressed()
            }
        }
    }

    private fun midiOutFile(): Unit = writeMidi.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        type = "audio/midi"
        addCategory(Intent.CATEGORY_OPENABLE) // don't show list of contacts or timezones
        putExtra(Intent.EXTRA_TITLE, "${bothRoutines.fileName}.mid")
        InfoMessage // For Toasts can use applicationContext, no Theme.AppCompat error
            .toast(applicationContext, getString(string.save, getString(string.justMidi)))
    })

    override fun onClick(view: View?) {
        DebugMode.assertArgument(view != null)
        when (view?.id) {
            -1 -> onBackPressed() // not android.R.id.home
            else -> DebugMode.assertState(false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean = super.onCreateOptionsMenu(menu).also {
        menuInflater.inflate(menu_main, menu)
        menu.findItem(id.menuMic).isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        super.onOptionsItemSelected(item).also {
            when (item.itemId) {
                id.menuGuide -> InfoMessage.dialog(this, string.userGuide, string.transGuide)
                id.menuShare -> Share.share(this)
                else -> DebugMode.assertArgument(false)
            }
        }
}