package ru.bshakhovsky.piano_transcription.main

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri

import android.os.Build
import android.os.Bundle
import android.os.StrictMode

import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View

import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import com.google.android.gms.ads.MobileAds
import com.google.android.material.navigation.NavigationView

import kotlinx.android.synthetic.main.activity_main.adMain
import kotlinx.android.synthetic.main.activity_main.control
import kotlinx.android.synthetic.main.activity_main.curTime
import kotlinx.android.synthetic.main.activity_main.drawerLayout
import kotlinx.android.synthetic.main.activity_main.drawerMenu
import kotlinx.android.synthetic.main.activity_main.durTime
import kotlinx.android.synthetic.main.activity_main.mainBar
import kotlinx.android.synthetic.main.activity_main.mainLayout
import kotlinx.android.synthetic.main.activity_main.newMedia
import kotlinx.android.synthetic.main.activity_main.next
import kotlinx.android.synthetic.main.activity_main.playPause
import kotlinx.android.synthetic.main.activity_main.prev
import kotlinx.android.synthetic.main.activity_main.seek
import kotlinx.android.synthetic.main.activity_main.soundBar
import kotlinx.android.synthetic.main.activity_main.soundCount
import kotlinx.android.synthetic.main.activity_main.surfaceView

import ru.bshakhovsky.piano_transcription.R.color.colorAccent
import ru.bshakhovsky.piano_transcription.R.drawable
import ru.bshakhovsky.piano_transcription.R // id
import ru.bshakhovsky.piano_transcription.R.id
import ru.bshakhovsky.piano_transcription.R.layout.activity_main
import ru.bshakhovsky.piano_transcription.R.menu.menu_main
import ru.bshakhovsky.piano_transcription.R.string

import ru.bshakhovsky.piano_transcription.AdBanner
import ru.bshakhovsky.piano_transcription.GuideActivity
import ru.bshakhovsky.piano_transcription.addDialog.AddDialog
import ru.bshakhovsky.piano_transcription.main.mainUI.Crash
import ru.bshakhovsky.piano_transcription.main.mainUI.Toggle
import ru.bshakhovsky.piano_transcription.main.mainUI.Touch
import ru.bshakhovsky.piano_transcription.main.openGL.Render
import ru.bshakhovsky.piano_transcription.midi.Midi
import ru.bshakhovsky.piano_transcription.midi.MidiActivity
import ru.bshakhovsky.piano_transcription.spectrum.SpectrumActivity
import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.MessageDialog
import ru.bshakhovsky.piano_transcription.web.WebActivity

import java.io.FileNotFoundException
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    CompoundButton.OnCheckedChangeListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    internal enum class DrawerGroup(val id: Int) { TRACKS(1) }
    internal enum class DrawerItem(val id: Int) { CHECK_ALL(-1) }

    private lateinit var render: Render

    private var mainMenu: Menu? = null

    private var midi: Midi? = null
    private var play: Play? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (DebugMode.debug) {
            StrictMode.enableDefaults()
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    detectContentUriWithoutPermission()//.detectUntaggedSockets()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { //detectNonSdkApiUsage().
                        penaltyListener(
                            Executors.newSingleThreadExecutor(),
                            StrictMode.OnVmViolationListener {
                                runOnUiThread {
                                    Toast.makeText(
                                        this@MainActivity,
                                        it.localizedMessage ?: "Unknown Vm policy violation",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            })
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            detectCredentialProtectedWhileLocked().detectImplicitDirectBoot()
                    }
                }
            } //.detectAll().detectCleartextNetwork().detectActivityLeaks().
                .detectFileUriExposure().detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects().detectLeakedSqlLiteObjects()
                .penaltyLog().build())
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    detectUnbufferedIo()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        penaltyListener(
                            Executors.newSingleThreadExecutor(),
                            StrictMode.OnThreadViolationListener { v ->
                                runOnUiThread {
                                    Toast.makeText(
                                        this@MainActivity,
                                        v.localizedMessage ?: "Unknown Thread policy violation",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            })
                    }
                }
            } //.detectAll().detectCustomSlowCalls().detectDiskReads().
                .detectDiskWrites().detectNetwork().detectResourceMismatches()
                .penaltyDialog().penaltyLog().build())
        }
        if (DebugMode.debug) Thread.setDefaultUncaughtExceptionHandler(Crash(this))
        if (intent.hasExtra("Crash")) AlertDialog.Builder(this).setTitle(string.error).apply {
            if (DebugMode.debug) setMessage(intent.getStringExtra("Crash"))
            else setMessage(string.crash)
        }.setIcon(drawable.info).setPositiveButton("Ok") { _, _ -> }.setCancelable(false).show()

        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        setContentView(activity_main)
        setSupportActionBar(mainBar)

        with(surfaceView) {
            setEGLContextClientVersion(3)
            render = Render(context, playPause, prev, next, soundBar, soundCount)
            setRenderer(render)
            setOnTouchListener(Touch(render))
        }

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
        drawerLayout.addDrawerListener(Toggle(this, drawerLayout, mainBar, mainLayout)
            .apply { syncState() })

        arrayOf<View>(newMedia, playPause, prev, next).forEach { it.setOnClickListener(this) }
        seek.setOnSeekBarChangeListener(this)

        MobileAds.initialize(this)
        AdBanner(adMain)

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
                            and (extras == null) and (clipData == null)
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
                        DebugMode.assertState((play != null) and (id in 0..tracks.lastIndex))
                        play?.run {
                            when {
                                isChecked -> addTrack(id)
                                isPlaying and (numSelTracks() == 1) -> {
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

    override fun onStart() {
        super.onStart()
        if (!(play ?: return).isPlaying) playPause.performClick()
    }

    override fun onStop() {
        super.onStop()
        if ((play ?: return).isPlaying) playPause.performClick()
    }

    override fun onResume() {
        super.onResume()
        surfaceView.onResume()
    }

    override fun onPause() {
        surfaceView.onPause()
        super.onPause()
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
                                play = Play(render, tracks, playPause, seek)
                                tracksEnabled(true)
                            }
                        }
                    }
                }
            }
        } catch (e: FileNotFoundException) {
            showMsg(string.noFile, "${e.localizedMessage ?: e}\n\n$uri")
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
        control.visibility = if (enabled) View.VISIBLE else View.GONE
        if (enabled) {
            DebugMode.assertState((midi != null) and (midi?.tracks != null))
            midi?.run {
                durTime.text = Midi.minSecStr(this@MainActivity, string.timeOf, dur)
                seek.max = dur.toInt()
            }
        } else {
            seek.progress = 0
            prev.visibility = View.GONE
            next.visibility = View.VISIBLE
            // Null for at least first two calls,
            // then after created for the first time, never becomes null:
//            DebugMode.assertState(play != null)
            play?.run { if (isPlaying) playPause.performClick() }
        }
        with(findItem(id.drawerTracks).subMenu) {
            if (enabled) {
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
                }
            }
        }
    }

    private fun font(t: TextView, enabled: Boolean) =
        t.setTypeface(null, if (enabled) Typeface.BOLD_ITALIC else Typeface.ITALIC)

    override fun onClick(view: View?) {
        DebugMode.assertArgument(view != null)
        view?.run {
            when (id) {
                R.id.newMedia -> AddDialog()
                    .show(supportFragmentManager, "Dialog Add")
                else -> {
                    // Can be clicked by touching GL view, even when buttons are invisible
//                    DebugMode.assertState(play != null)
                    play?.run {
                        seek.progress.also { prevMilSec ->
                            when (id) {
                                R.id.playPause -> {
                                    if (!isPlaying and noTracks()) return
                                    view.setBackgroundResource(
                                        if (isPlaying) android.R.drawable.ic_media_play
                                        else android.R.drawable.ic_media_pause
                                    )
                                    toggle()
                                    prevNext()
                                }
                                R.id.prev -> if (!isPlaying and !noTracks()) {
                                    render.releaseAllKeys()
                                    do {
                                        val anyPressed = prevChord()
                                    } while (((prevMilSec - seek.progress < 1) or !anyPressed)
                                        and (seek.progress != 0)
                                    )
                                }
                                R.id.next -> if (!isPlaying and !noTracks()) {
                                    render.releaseAllKeys()
                                    do {
                                        val (anyPressed, stop) = nextChord()
                                    } while (((seek.progress - prevMilSec < 1) or !anyPressed)
                                        and !stop
                                    )
                                }
                                else -> DebugMode.assertArgument(false)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onProgressChanged(bar: SeekBar?, pos: Int, fromUser: Boolean) {
        DebugMode.assertArgument(bar != null)
        curTime.text = Midi.minSecStr(this, string.timeCur, pos.toLong())
        prevNext()
        if (fromUser) {
            DebugMode.assertState(play != null)
            play?.seek(pos.toLong())
        }
    }

    override fun onStartTrackingTouch(bar: SeekBar?): Unit = DebugMode.assertArgument(bar != null)
    override fun onStopTrackingTouch(bar: SeekBar?): Unit = DebugMode.assertArgument(bar != null)

    private fun prevNext() {
        /* When exception occurs in a child activity,
        and "setDefaultUncaughtExceptionHandler" recreates the activity,
        and MIDI-file has already been opened (play was not null),
        for some reason onProgressChanged is called and we end up here,
        and assert would cause infinite loop: */
        if (DebugMode.debug) if (play == null) showMsg(string.error, "Play == null")
        play?.run {
            with(seek) {
                prev.visibility = if ((isPlaying) or (progress == 0))
                    View.INVISIBLE else View.VISIBLE
                next.visibility = if ((isPlaying) or (progress == max))
                    View.INVISIBLE else View.VISIBLE
            }
        }
    }

    private fun noTracks(): Boolean {
        DebugMode.assertState(play != null)
        if (play?.numSelTracks() == 0) {
            showError(string.justTracks, string.trackNotSel)
            drawerLayout.openDrawer(GravityCompat.START)
            return true
        }
        return false
    }

    private fun showError(titleId: Int, msgId: Int) = MessageDialog.show(this, titleId, msgId)
    private fun showMsg(titleId: Int, msgStr: String) = MessageDialog.show(this, titleId, msgStr)
}