package ru.bshakhovsky.piano_transcription.main

import android.content.Intent
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

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider

import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.material.navigation.NavigationView

import kotlinx.android.synthetic.main.activity_main.adMain
import kotlinx.android.synthetic.main.activity_main.drawerLayout
import kotlinx.android.synthetic.main.activity_main.drawerMenu
import kotlinx.android.synthetic.main.activity_main.mainBar
import kotlinx.android.synthetic.main.activity_main.mainLayout
import kotlinx.android.synthetic.main.activity_main.seek

import ru.bshakhovsky.piano_transcription.R.color.colorAccent
import ru.bshakhovsky.piano_transcription.R.drawable
import ru.bshakhovsky.piano_transcription.R // id
import ru.bshakhovsky.piano_transcription.R.id
import ru.bshakhovsky.piano_transcription.R.layout.activity_main
import ru.bshakhovsky.piano_transcription.R.menu.menu_main
import ru.bshakhovsky.piano_transcription.R.string
import ru.bshakhovsky.piano_transcription.databinding.ActivityMainBinding

import ru.bshakhovsky.piano_transcription.AdBanner
import ru.bshakhovsky.piano_transcription.addDialog.AddDialog
import ru.bshakhovsky.piano_transcription.GuideActivity
import ru.bshakhovsky.piano_transcription.main.mainUI.Toggle
import ru.bshakhovsky.piano_transcription.main.openGL.Render
import ru.bshakhovsky.piano_transcription.midi.Midi
import ru.bshakhovsky.piano_transcription.midi.MidiActivity
import ru.bshakhovsky.piano_transcription.spectrum.SpectrumActivity

import ru.bshakhovsky.piano_transcription.utils.Crash
import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.MessageDialog
import ru.bshakhovsky.piano_transcription.utils.StrictPolicy

import ru.bshakhovsky.piano_transcription.web.WebActivity

import java.io.FileNotFoundException

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        if (DebugMode.debug) {
            policy = StrictPolicy(lifecycle, this)
            Thread.setDefaultUncaughtExceptionHandler(Crash(getExternalFilesDir("Errors")))
        }

        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        binding = DataBindingUtil.setContentView(this, activity_main)
        model = ViewModelProvider(this).get(MainModel::class.java)
            .apply { initialize(lifecycle, supportFragmentManager) }
        sound = ViewModelProvider(this).get(Sound::class.java)
            .apply { load(applicationContext, lifecycle) }
        with(binding) {
            mainModel = model
            soundModel = sound
            render =
                Render(lifecycle, assets, resources, surfaceView, playPause, prev, next, sound)
            play = ViewModelProvider(this@MainActivity).get(Play::class.java)
                .apply { initialize(lifecycle, this@MainActivity, drawerLayout, render) }
            playModel = play

            lifecycleOwner = this@MainActivity
        }

        setSupportActionBar(mainBar)

        drawerMenu.setNavigationItemSelectedListener(this)
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

        drawerLayout.addDrawerListener(Toggle(lifecycle, mainLayout, this, drawerLayout, mainBar)
            .apply { syncState() })
        seek.setOnSeekBarChangeListener(this)

        MobileAds.initialize(applicationContext)
        if (DebugMode.debug) MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(
                listOf(AdRequest.DEVICE_ID_EMULATOR, "87FD000F52337DF09DBB9E6684B0B878")
            ).build()
        )
        AdBanner(lifecycle, adMain)

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
//                            and (extras == null) // not null when launched from main menu
                            and (clipData == null)
                )

                Intent.ACTION_VIEW -> DebugMode.assertState(
                    (categories.size in arrayOf(1, 2)) and (hasCategory(Intent.CATEGORY_DEFAULT)
                            or hasCategory(Intent.CATEGORY_BROWSABLE))
                            and (type == null) and (data != null) and (dataString in arrayOf(
                        "http://bshakhovsky.github.io", "https://bshakhovsky.github.io"
                    )) and (extras == null) and (clipData == null)
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
                            DebugMode.assertState(mimeTypeCount == 1)
                            if (type == "text/plain") {
                                DebugMode.assertState(
                                    (getMimeType(0) == "text/plain") and hasExtra(Intent.EXTRA_TEXT)
                                )
                                startActivity(Intent(this@MainActivity, WebActivity::class.java)
                                    .apply { putExtras(intent) })
                            } else {
                                DebugMode.assertState(
                                    (type != null)
                                            and (type?.substringBefore('/')
                                            in arrayOf("audio", "video"))
                                            and (getMimeType(0).substringBefore('/')
                                            in arrayOf("audio", "video")) and (getItemAt(0) != null)
                                )
                                getItemAt(0)?.run { openMedia(uri) }
                            }
                        }
                    }
                }

                else -> DebugMode.assertState(false)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean = super.onCreateOptionsMenu(menu).also {
        DebugMode.assertArgument(menu != null)
        menuInflater.inflate(menu_main, menu.also { mainMenu = it })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        super.onOptionsItemSelected(item).also {
            when (item.itemId) {
                id.menuTracks -> drawerLayout.openDrawer(GravityCompat.START)
                id.menuMidi -> midi()
                id.menuGuide -> guide()
                else -> DebugMode.assertArgument(false)
            }
        }

    override fun onNavigationItemSelected(item: MenuItem): Boolean = true.also {
        when (item.itemId) {
            id.drawerMidi -> midi()
            id.drawerGuide -> guide()
            else -> {
                checkGroup(item.groupId, item.itemId)
                (item.actionView as Switch).toggle()
                return false
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    override fun onCheckedChanged(button: CompoundButton?, checked: Boolean) {
        DebugMode.assertArgument(button != null)
        button?.run {
            DebugMode.assertState(midi != null)
            midi?.run {
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
                                    return
                                }
                                else -> removeTrack(id)
                            }
                            (drawerMenu.menu.findItem(R.id.drawerAll).actionView as Switch).text =
                                getString(string.tracks, numSelTracks(), tracks.size)
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

    private fun checkGroup(groupId: Int, itemId: Int) = @Suppress("Reformat") when (groupId) {
        0                       -> false.also { DebugMode.assertArgument(itemId == id.drawerAll) }
        DrawerGroup.TRACKS.id   -> true.also {
            DebugMode.assertState(midi != null)
            midi?.run { DebugMode.assertArgument(itemId in 0..tracks.lastIndex) }
        }
        else                    -> false.also { DebugMode.assertArgument(false) }
    }

    override fun onBackPressed(): Unit = with(drawerLayout) {
        GravityCompat.START.let { if (isDrawerOpen(it)) closeDrawer(it) else super.onBackPressed() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode.toShort().toInt()) { // Dialog fragment on super() will receive it
            AddDialog.RequestCode.OPEN_MEDIA.id -> {
                if (resultCode != RESULT_OK) return
                DebugMode.assertState((data != null) and (data?.data != null))
                data?.data?.let { openMedia(it) }
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

    private fun spectrum(uri: Uri) =
        startActivity(Intent(this, SpectrumActivity::class.java).apply { putExtra("Uri", uri) })

    private fun openMedia(uri: Uri) {
        if (!openMidi(uri)) spectrum(uri)
    }

    private fun openMidi(uri: Uri): Boolean {
        try {
            contentResolver.openInputStream(uri).let { inputStream ->
                DebugMode.assertState(inputStream != null)
                inputStream?.use { inStream ->
                    with(Midi(inStream, getString(string.untitled))) {
                        if (badMidi) return false

                        midi = this
                        midiEnabled(true)
                        tracksEnabled(false) // needs to know old play.isPlaying
                        when {
                            tracks.isNullOrEmpty() ->
                                showError(string.emptyMidi, string.noTracks)
                            dur == 0L -> showError(string.emptyMidi, string.zeroDur)
                            else -> {
                                DebugMode.assertState(::render.isInitialized)
                                play.newMidi(tracks, dur.toInt())
                                tracksEnabled(true)
                            }
                        }
                    }
                }
            }
        } catch (e: FileNotFoundException) {
            MessageDialog.show(this, string.noFile, "${e.localizedMessage ?: e}\n\n$uri")
        }
        return true
    }

    private fun midiEnabled(enabled: Boolean) = with(drawerMenu.menu.findItem(id.drawerMidi)) {
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

    private fun tracksEnabled(enabled: Boolean) = with(drawerMenu.menu) {
        mainMenu?.run { findItem(id.menuTracks).isVisible = enabled }
        model.contVis.value = if (enabled) View.VISIBLE else View.GONE
        with(findItem(id.drawerTracks).subMenu) {
            if (enabled) {
                DebugMode.assertState((midi != null) and (midi?.tracks != null))
                midi?.tracks?.forEachIndexed { i, track ->
                    with(add(1, i, Menu.NONE, track.info.name)) {
                        icon = getDrawable(drawable.queue)
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
                } else play.stop()
            }
        }
    }

    private fun font(t: TextView, enabled: Boolean) =
        t.setTypeface(null, if (enabled) Typeface.BOLD_ITALIC else Typeface.ITALIC)

    override fun onProgressChanged(bar: SeekBar?, pos: Int, fromUser: Boolean) {
        DebugMode.assertArgument(bar != null)
        play.prevNext()
        if (fromUser) play.seek(pos.toLong())
    }

    override fun onStartTrackingTouch(bar: SeekBar?): Unit = DebugMode.assertArgument(bar != null)
    override fun onStopTrackingTouch(bar: SeekBar?): Unit = DebugMode.assertArgument(bar != null)

    private fun showError(titleId: Int, msgId: Int) = MessageDialog.show(this, titleId, msgId)
}