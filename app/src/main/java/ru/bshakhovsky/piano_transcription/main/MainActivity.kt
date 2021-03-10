package ru.bshakhovsky.piano_transcription.main

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle

import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View

import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

import androidx.annotation.CheckResult
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider

import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

import com.google.android.material.navigation.NavigationView

import ru.bshakhovsky.piano_transcription.R.color.colorAccent
import ru.bshakhovsky.piano_transcription.R.drawable
import ru.bshakhovsky.piano_transcription.R // id
import ru.bshakhovsky.piano_transcription.R.id
import ru.bshakhovsky.piano_transcription.R.layout.activity_main
import ru.bshakhovsky.piano_transcription.R.menu.menu_main
import ru.bshakhovsky.piano_transcription.R.string
import ru.bshakhovsky.piano_transcription.databinding.ActivityMainBinding

import ru.bshakhovsky.piano_transcription.GuideActivity
import ru.bshakhovsky.piano_transcription.ad.AdBanner
import ru.bshakhovsky.piano_transcription.ad.AdInterstitial
import ru.bshakhovsky.piano_transcription.addDialog.AddDialog
import ru.bshakhovsky.piano_transcription.main.mainUI.Toggle
import ru.bshakhovsky.piano_transcription.main.openGL.Render
import ru.bshakhovsky.piano_transcription.media.MediaActivity
import ru.bshakhovsky.piano_transcription.midi.Midi
import ru.bshakhovsky.piano_transcription.midi.MidiActivity
import ru.bshakhovsky.piano_transcription.utils.Crash
import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage
import ru.bshakhovsky.piano_transcription.utils.ShareReview
import ru.bshakhovsky.piano_transcription.utils.StrictPolicy

import java.io.FileNotFoundException
import java.util.Locale

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {

    internal enum class RequestCode(val id: Int) { SPECTRUM(10) }
    internal enum class DrawerGroup(val id: Int) { TRACKS(1) }
    internal enum class DrawerItem(val id: Int) { CHECK_ALL(-1) }

    private lateinit var policy: StrictPolicy

    private lateinit var binding: ActivityMainBinding

    private lateinit var model: MainModel
    private lateinit var sound: Sound

    private lateinit var play: Play
    private lateinit var render: Render

    private var midi: Midi? = null
    private var mainMenu: Menu? = null
    private lateinit var interstitial: AdInterstitial

    override fun onCreate(savedInstanceState: Bundle?) {
        if (DebugMode.debug) {
            Thread.setDefaultUncaughtExceptionHandler(Crash(getExternalFilesDir("Errors")))
            policy = StrictPolicy(lifecycle, this@MainActivity)
        }
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        with(ViewModelProvider(this)) {
            model = get(MainModel::class.java)
                .apply { initialize(lifecycle, supportFragmentManager) }
            sound = get(Sound::class.java).apply { load(applicationContext) }
            with(
                DataBindingUtil
                    .setContentView<ActivityMainBinding>(this@MainActivity, activity_main)
            ) {
                binding = this

                mainModel = model
                soundModel = sound

                render =
                    Render(lifecycle, assets, resources, surfaceView, playPause, prev, next, sound)
                play = get(Play::class.java)
                    .apply { initialize(lifecycle, this@MainActivity, drawerLayout, render) }
                playModel = play

                lifecycleOwner = this@MainActivity
            }
        }

        with(binding) {
            setSupportActionBar(mainBar)

            drawerMenu.setNavigationItemSelectedListener(this@MainActivity)
            with(drawerMenu.menu) {
                intArrayOf(id.drawerMidi, id.drawerAll).forEach {
                    with(findItem(it).actionView as TextView) {
                        gravity = Gravity.CENTER_VERTICAL
                        setTextColor(getColor(colorAccent))
                    }
                }
                with(findItem(id.drawerAll).actionView as Switch) {
                    id = DrawerItem.CHECK_ALL.id
                    setOnCheckedChangeListener(this@MainActivity)
                }
            }
            midiEnabled(false)
            tracksEnabled(false)

            drawerLayout.addDrawerListener(Toggle(
                lifecycle, mainLayout, this@MainActivity, drawerLayout, mainBar
            ).apply { syncState() })
            seek.setOnSeekBarChangeListener(this@MainActivity)

            MobileAds.initialize(applicationContext)
            if (DebugMode.debug) MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder().setTestDeviceIds(
                    listOf(AdRequest.DEVICE_ID_EMULATOR, "87FD000F52337DF09DBB9E6684B0B878")
                ).build()
            )
            AdBanner(lifecycle, applicationContext, adMain, getString(string.bannerMain))
            interstitial = AdInterstitial(lifecycle, this@MainActivity)
        }

        onNewIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        DebugMode.assertArgument(intent != null)
        intent?.run {
            when (action) {
                Intent.ACTION_MAIN -> DebugMode.assertState(
                    (categories.size == 1) and hasCategory(Intent.CATEGORY_LAUNCHER)
                            and (type == null) and (data == null) and (dataString == null)
                            // and (extras == null)
                            /* Not null when launched on Emulator 2.7 Q VGA API 24 from main menu:
                            extras != null = dalvik.system.PathClassLoader[DexPathList[
                            [zip file "/data/app/ru.BShakhovsky.Piano_Transcription-2/base.apk"],
                            nativeLibraryDirectories=
                            [/data/app/ru.BShakhovsky.Piano_Transcription-2/lib/x86,
                            /data/app/ru.BShakhovsky.Piano_Transcription-2/base.apk!/lib/x86,
                            /system/lib, /vendor/lib]]] */
                            and (clipData == null)
                )

                Intent.ACTION_VIEW -> DebugMode.assertState(
                    // (categories.size in arrayOf(1, 2)) and (hasCategory(Intent.CATEGORY_DEFAULT)
                    //        or hasCategory(Intent.CATEGORY_BROWSABLE)) and
                    // categories = null when tried to go to website from Google Play
                    (type == null) and (data != null)
                            and with(dataString!!.toLowerCase(Locale.getDefault())) {
                        startsWith("http://bshakhovsky.github.io") or
                                startsWith("https://bshakhovsky.github.io")
                    } and (extras == null) and (clipData == null)
                )

                Intent.ACTION_SEND -> {
                    if (categories != null) DebugMode.assertState(
                        (categories.size == 1) and hasCategory(Intent.CATEGORY_DEFAULT)
                    )
                    DebugMode.assertState(
                        (data == null) and (dataString == null)
                                and (extras != null) and (clipData != null)
                    )
                    clipData?.run {
                        DebugMode.assertState((itemCount == 1) and (description != null))
                        description?.run {
                            DebugMode.assertState(
                                (mimeTypeCount == 1) and (type != null)
                                        and (type?.substringBefore('/')
                                        in arrayOf("audio", "video"))
                                        and (getMimeType(0).substringBefore('/')
                                        in arrayOf("audio", "video")) and (getItemAt(0) != null)
                            )
                            getItemAt(0)?.run { openMedia(uri) }
                        }
                    }
                }

                // Restarted due to UncaughtExceptionHandler:
                else -> DebugMode.assertState(action == null)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration): Unit =
        super.onConfigurationChanged(newConfig)
            .also { interstitial = AdInterstitial(lifecycle, this) }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean = super.onCreateOptionsMenu(menu).also {
        DebugMode.assertArgument(menu != null)
        menuInflater.inflate(menu_main, menu.also { mainMenu = it })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        super.onOptionsItemSelected(item).also {
            when (item.itemId) {
                id.menuTracks -> binding.drawerLayout.openDrawer(GravityCompat.START)
                id.menuMidi -> midi()
                id.menuGuide -> guide()
                id.menuShare -> ShareReview.share(this)
                id.menuRate -> ShareReview.review(this)
                else -> DebugMode.assertArgument(false)
            }
        }

    override fun onNavigationItemSelected(item: MenuItem): Boolean = true.also {
        when (item.itemId) {
            id.drawerMidi -> midi()
            id.drawerGuide -> guide()
            id.drawerShare -> ShareReview.share(this)
            id.drawerRate -> ShareReview.review(this)
            else -> {
                checkGroup(item.groupId, item.itemId)
                (item.actionView as Switch).toggle()
                return false
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    override fun onCheckedChanged(button: CompoundButton?, checked: Boolean) {
        DebugMode.assertArgument(button != null)
        button?.run {
            DebugMode.assertState(midi != null)
            midi?.run {
                binding.run {
                    when (id) {
                        DrawerItem.CHECK_ALL.id -> for (i in 0..tracks.lastIndex) {
                            with(drawerMenu.menu.findItem(R.id.drawerTracks).subMenu.findItem(i)) {
                                if (checkGroup(groupId, itemId))
                                    (actionView as Switch).isChecked = button.isChecked
                            }
                        }
                        else -> {
                            DebugMode.assertState(id in 0..tracks.lastIndex)
                            with(play) {
                                DebugMode.assertState(isPlaying.value != null)
                                when {
                                    isChecked -> addTrack(id)
                                    (isPlaying.value ?: return@with) and (numSelTracks() == 1) -> {
                                        showError(string.justTracks, string.trackNotSelPlaying)
                                        performClick()
                                        return@with
                                    }
                                    else -> removeTrack(id)
                                }
                                (drawerMenu.menu.findItem(R.id.drawerAll).actionView as Switch)
                                    .text = getString(string.tracks, numSelTracks(), tracks.size)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun midi() = startActivity(Intent(this, MidiActivity::class.java).apply {
        DebugMode.assertState(midi != null)
        midi?.run {
            putExtra("Summary", summary)
            putExtra("Tracks", tracks.map { it.info }.toTypedArray())
            putExtra("Percuss", percuss)
        }
    })

    private fun guide() = startActivity(Intent(this, GuideActivity::class.java))

    @CheckResult
    private fun checkGroup(groupId: Int, itemId: Int) = @Suppress("Reformat") when (groupId) {
        0                       -> false.also { DebugMode.assertArgument(itemId == id.drawerAll) }
        DrawerGroup.TRACKS.id   -> true.also {
            DebugMode.assertState(midi != null)
            midi?.run { DebugMode.assertArgument(itemId in 0..tracks.lastIndex) }
        }
        else                    -> false.also { DebugMode.assertArgument(false) }
    }

    override fun onBackPressed(): Unit = with(binding.drawerLayout) {
        GravityCompat.START.let { if (isDrawerOpen(it)) closeDrawer(it) else super.onBackPressed() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode.toShort().toInt()) { // Dialog fragment on super() will receive it
            RequestCode.SPECTRUM.id -> {
                if (resultCode != RESULT_OK) return
                DebugMode.assertState((data != null) and (data?.data != null))
                data?.data?.let { uri -> openMidi(uri).let { ok -> DebugMode.assertState(ok) } }
            }
            AddDialog.RequestCode.SURF.id, AddDialog.RequestCode.OPEN_MEDIA.id -> {
                if (resultCode != RESULT_OK) return
                DebugMode.assertState(data != null)
                data?.let { intent ->
                    DebugMode.assertState(intent.data != null)
                    intent.data?.let { openMedia(it, intent.getStringExtra("YouTube Link")) }
                }
            }
            AddDialog.RequestCode.OPEN_MIDI.id -> {
                if (resultCode != RESULT_OK) return
                DebugMode.assertState((data != null) and (data?.data != null))
                data?.data?.let { if (!openMidi(it)) showError(string.badFile, string.notMidi) }
            }
            AddDialog.RequestCode.WRITE_3GP.id, AddDialog.Permission.RECORD_SETTINGS.id ->
                super.onActivityResult(requestCode, resultCode, data)
            else -> DebugMode.assertArgument(false)
        }
    }

    fun openMedia(file: Uri, youTubeLink: String? = null) {
        if (!openMidi(file)) startActivityForResult(
            Intent(this, MediaActivity::class.java).apply {
                data = file
                youTubeLink?.let { putExtra("YouTube Link", it) }
            }, RequestCode.SPECTRUM.id
        )
    }

    @CheckResult
    private fun openMidi(uri: Uri): Boolean {
        try {
            contentResolver.openInputStream(uri).let { inputStream ->
                DebugMode.assertState(inputStream != null)
                inputStream?.use { inStream ->
                    with(Midi(inStream, getString(string.untitled))) {
                        if (badMidi) return false
                        interstitial.show()

                        midi = this
                        midiEnabled(true)
                        tracksEnabled(false) // needs to know old play.isPlaying
                        when {
                            tracks.isNullOrEmpty() -> showError(string.emptyMidi, string.noTracks)
                            dur == 0L -> showError(string.emptyMidi, string.zeroDur)
                            else -> {
                                DebugMode.assertState(::render.isInitialized)
                                play.newMidi(tracks, dur)
                                tracksEnabled(true)
                            }
                        }
                    }
                }
            }
        } catch (e: FileNotFoundException) {
            InfoMessage.dialog(this, string.noFile, "${e.localizedMessage ?: e}\n\n$uri")
        }
        return true
    }

    private fun midiEnabled(enabled: Boolean) =
        with(binding.drawerMenu.menu.findItem(id.drawerMidi)) {
            mainMenu?.run { findItem(id.menuMidi).isVisible = enabled }
            isEnabled = enabled
            (actionView as TextView).let { t ->
                if (enabled) {
                    DebugMode.assertState((midi != null) and (midi?.summary != null))
                    midi?.summary?.run {
                        t.text = getString(
                            string.keyTemp,
                            if (keys.isNullOrEmpty()) "" else keys.first().key,
                            if (tempos.isNullOrEmpty()) 0 else tempos.first().bpm.toInt()
                        )
                    }
                } else t.text = getString(string.noMidi)
                font(t, enabled)
            }
        }

    private fun tracksEnabled(enabled: Boolean) = with(binding.drawerMenu.menu) {
        mainMenu?.run { findItem(id.menuTracks).isVisible = enabled }
        model.contVis.value = if (enabled) View.VISIBLE else View.GONE
        with(findItem(id.drawerTracks).subMenu) {
            if (enabled) {
                DebugMode.assertState((midi != null) and (midi?.tracks != null))
                midi?.tracks?.forEachIndexed { i, track ->
                    with(add(1, i, Menu.NONE, track.info.name)) {
                        icon = ContextCompat.getDrawable(applicationContext, drawable.queue)
                        actionView = Switch(this@MainActivity).apply {
                            id = i
                            showText = true
                            textOn = "+"
                            textOff = ""
                            setOnCheckedChangeListener(this@MainActivity)
                        }
                    }
                }
            } else removeGroup(DrawerGroup.TRACKS.id)
        }
        with(findItem(id.drawerAll)) {
            isEnabled = enabled
            with(actionView as Switch) {
                isEnabled = enabled
                text = getString(string.tracks, 0, if (enabled) midi?.tracks?.size else 0)
                font(this, enabled)
                if (enabled) {
                    toggle()
                    if (!isChecked) toggle()
                }
            }
        }
    }

    private fun font(t: TextView, enabled: Boolean) =
        t.setTypeface(null, if (enabled) Typeface.BOLD_ITALIC else Typeface.ITALIC)

    override fun onProgressChanged(bar: SeekBar?, pos: Int, fromUser: Boolean) {
        DebugMode.assertArgument(bar != null)
        play.prevNext()
        if (fromUser) play.seek(pos)
    }

    override fun onStartTrackingTouch(bar: SeekBar?): Unit = DebugMode.assertArgument(bar != null)
    override fun onStopTrackingTouch(bar: SeekBar?): Unit = DebugMode.assertArgument(bar != null)

    @CheckResult
    private fun showError(@StringRes titleId: Int, @StringRes msgId: Int) =
        InfoMessage.dialog(this, titleId, msgId)
}