@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription

import android.content.Context
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

import ru.BShakhovsky.Piano_Transcription.MainUI.Crash
import ru.BShakhovsky.Piano_Transcription.MainUI.Toggle
import ru.BShakhovsky.Piano_Transcription.MainUI.Touch
import ru.BShakhovsky.Piano_Transcription.Midi.Midi
import ru.BShakhovsky.Piano_Transcription.Midi.MidiActivity
import ru.BShakhovsky.Piano_Transcription.OpenGL.Render

import java.io.FileNotFoundException
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    CompoundButton.OnCheckedChangeListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    companion object {
        fun msgDialog(
            context: Context, titleId: Int, msgStr: String,
            okId: Int = R.string.ok, okAction: (() -> Unit) = {}
        ): AlertDialog = AlertDialog.Builder(context).setTitle(titleId).setMessage(msgStr)
            .setIcon(R.drawable.info).setPositiveButton(okId) { _, _ -> okAction() }
            .setCancelable(false).show()

        fun msgDialog(
            context: Context, titleId: Int, msgId: Int,
            okId: Int = R.string.ok, okAction: (() -> Unit) = {}
        ): AlertDialog = msgDialog(context, titleId, context.getString(msgId), okId, okAction)
    }

    internal enum class DrawerGroup(val id: Int) { TRACKS(1) }
    internal enum class DrawerItem(val id: Int) { CHECK_ALL(-1) }

    private lateinit var mainMenu: Menu
    private lateinit var render: Render

    private var midi: Midi? = null
    private var play: Play? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (DebugMode.debug) {
            StrictMode.enableDefaults()
            with(StrictMode.VmPolicy.Builder()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    detectContentUriWithoutPermission()//.detectUntaggedSockets()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { //detectNonSdkApiUsage().
                        penaltyListener(
                            Executors.newSingleThreadExecutor(), StrictMode.OnVmViolationListener {
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
                StrictMode.setVmPolicy(//detectAll().detectCleartextNetwork().detectActivityLeaks().
                    detectFileUriExposure().detectLeakedClosableObjects()
                        .detectLeakedRegistrationObjects().detectLeakedSqlLiteObjects()
                        .penaltyLog().build()
                )
            }
            with(StrictMode.ThreadPolicy.Builder()) {
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
                StrictMode.setThreadPolicy(//detectAll().detectCustomSlowCalls().detectDiskReads().
                    detectDiskWrites().detectNetwork().detectResourceMismatches()
                        .penaltyDialog().penaltyLog().build()
                )
            }
        }
        Thread.setDefaultUncaughtExceptionHandler(Crash(this))
        if (intent.hasExtra("Crash")) AlertDialog.Builder(this).setTitle(R.string.error).apply {
            if (BuildConfig.DEBUG) setMessage(intent.getStringExtra("Crash"))
            else setMessage(R.string.crash)
        }.setIcon(R.drawable.info).setPositiveButton("Ok") { _, _ -> }.setCancelable(false).show()

        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        setContentView(R.layout.activity_main)
        setSupportActionBar(mainBar)

        with(surfaceView) {
            setEGLContextClientVersion(3)
            render = Render(context, playPause, prev, next, soundBar, soundCount)
            setRenderer(render)
            setOnTouchListener(Touch(render))
        }

        drawerMenu.setNavigationItemSelectedListener(this)
        with(drawerMenu.menu) {
            intArrayOf(R.id.drawerMidi, R.id.drawerAll).forEach {
                with(findItem(it).actionView as TextView) {
                    gravity = Gravity.CENTER_VERTICAL
                    setTextColor(getColor(R.color.colorAccent))
                }
            }
            with(findItem(R.id.drawerAll).actionView as Switch) {
                id = DrawerItem.CHECK_ALL.id
                setOnCheckedChangeListener(this@MainActivity)
            }
        }
        midiEnabled(false)
        tracksEnabled(false)
        with(Toggle(this, drawerLayout, mainBar, mainLayout)) {
            drawerLayout.addDrawerListener(this)
            syncState()
        }

        arrayOf<View>(newMedia, playPause, prev, next).forEach { it.setOnClickListener(this) }
        seek.setOnSeekBarChangeListener(this)

        MobileAds.initialize(this)
        AdBanner(adMain)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        mainMenu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuTracks -> drawerLayout.openDrawer(GravityCompat.START)
            R.id.menuMidi -> midi()
            R.id.menuGuide -> guide()
            else -> DebugMode.assertArgument(false)
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.drawerMidi -> midi()
            R.id.drawerGuide -> guide()
            else -> {
                checkGroup(item.groupId, item.itemId)
                (item.actionView as Switch).toggle()
                return false
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
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
                        midi?.run {
                            DebugMode.assertState((play != null) and (id in 0..tracks.lastIndex))
                            play?.run {
                                when {
                                    isChecked -> addTrack(id)
                                    isPlaying and (numSelTracks() == 1) -> {
                                        showError(R.string.justTracks, R.string.trackNotSelPlaying)
                                        performClick()
                                        return
                                    }
                                    else -> removeTrack(id)
                                }
                                (drawerMenu.menu.findItem(R.id.drawerAll)
                                    .actionView as Switch).text =
                                    getString(R.string.tracks, numSelTracks(), tracks.size)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun midi() {
        Intent(this, MidiActivity::class.java).also { intent ->
            DebugMode.assertState(midi != null)
            midi?.run {
                intent.putExtra("Summary", summary)
                intent.putExtra("Tracks", tracks.map { it.info }.toTypedArray())
                intent.putExtra("Percuss", percuss)
            }
            startActivity(intent)
        }
    }

    private fun guide() = startActivity(Intent(this, GuideActivity::class.java))

    private fun checkGroup(groupId: Int, itemId: Int) = when (groupId) {
        0 -> {
            DebugMode.assertArgument(itemId == R.id.drawerAll)
            false
        }
        DrawerGroup.TRACKS.id -> {
            DebugMode.assertArgument(itemId in 0..midi!!.tracks.lastIndex)
            true
        }
        else -> {
            DebugMode.assertArgument(false)
            false
        }
    }

    override fun onBackPressed() {
        with(drawerLayout) {
            GravityCompat.START.also {
                if (isDrawerOpen(it)) closeDrawer(it) else super.onBackPressed()
            }
        }
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
//        if (::render.isInitialized)
        surfaceView.onResume()
    }

    override fun onPause() {
//        if (::render.isInitialized)
        surfaceView.onPause()
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode.toShort().toInt()) { // Dialog fragment on super() will receive it
            AddDialog.RequestCode.OPEN_MEDIA.id -> {
                if (resultCode != RESULT_OK) return
                DebugMode.assertState((data != null) and (data?.data != null))
                data?.data?.let { openMidi(it) }
            }
            AddDialog.RequestCode.OPEN_MIDI.id -> {
                if (resultCode != RESULT_OK) return
                DebugMode.assertState((data != null) and (data?.data != null))
                data?.data?.let { openMidi(it) }
            }
            AddDialog.RequestCode.WRITE_WAV.id, AddDialog.Permission.RECORD_SETTINGS.id ->
                super.onActivityResult(requestCode, resultCode, data)
            else -> DebugMode.assertArgument(false)
        }
    }

    private fun openMidi(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)
        } catch (e: FileNotFoundException) {
            with(e) {
                showMsg(R.string.notFound, "${localizedMessage ?: this}\n\n$uri")
            }
            return
        }.also { inputStream ->
            DebugMode.assertState(inputStream != null)
            inputStream?.use { inStream ->
                with(Midi(inStream, getString(R.string.untitled))) {
                    if (badMidi) {
                        showError(R.string.badFile, R.string.notMidi)
                        return
                    }
                    midi = this
                    midiEnabled(true)
                    tracksEnabled(false) // needs to know old play.isPlaying
                    when {
                        tracks.isNullOrEmpty() -> {
                            showError(R.string.emptyMidi, R.string.noTracks)
                            return
                        }
                        dur == 0L -> {
                            showError(R.string.emptyMidi, R.string.zeroDur)
                            return
                        }
                        else -> {
                            DebugMode.assertState(::render.isInitialized)
                            play = Play(render, tracks, playPause, seek)
                            tracksEnabled(true)
                        }
                    }
                }
            }
        }
    }

    private fun midiEnabled(enabled: Boolean) {
        if (::mainMenu.isInitialized) mainMenu.findItem(R.id.menuMidi).isVisible = enabled
        with(drawerMenu.menu.findItem(R.id.drawerMidi)) {
            isEnabled = enabled
            (actionView as TextView).also { t ->
                if (enabled) {
                    DebugMode.assertState((midi != null) and (midi?.summary != null))
                    midi?.summary?.run {
                        t.text = getString(
                            R.string.keyTemp,
                            if (keys.isNullOrEmpty()) "" else keys.first().key,
                            if (tempos.isNullOrEmpty()) 0 else tempos.first().bpm.toInt()
                        )
                    }
                } else t.text = getString(R.string.noMidi)
                font(t, enabled)
            }
        }
    }

    private fun tracksEnabled(enabled: Boolean) {
        if (::mainMenu.isInitialized) mainMenu.findItem(R.id.menuTracks).isVisible = enabled
        control.visibility = if (enabled) View.VISIBLE else View.GONE
        if (enabled) {
            DebugMode.assertState((midi != null) and (midi?.tracks != null))
            midi?.run {
                durTime.text = Midi.minSecStr(this@MainActivity, R.string.timeOf, dur)
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
        with(drawerMenu.menu) {
            with(findItem(R.id.drawerTracks).subMenu) {
                if (enabled) {
                    midi?.tracks?.forEachIndexed { i, track ->
                        (Switch(this@MainActivity)).run {
                            id = i
                            showText = true
                            textOn = "+"
                            textOff = ""
                            setOnCheckedChangeListener(this@MainActivity)
                            with(add(1, i, Menu.NONE, track.info.name)) {
                                icon = getDrawable(R.drawable.queue)
                                actionView = this@run
                            }
                        }
                    }
                } else removeGroup(DrawerGroup.TRACKS.id)
            }
            with(findItem(R.id.drawerAll)) {
                isEnabled = enabled
                with(actionView as Switch) {
                    isEnabled = enabled
                    text = getString(R.string.tracks, 0, if (enabled) midi?.tracks?.size else 0)
                    font(this, enabled)
                    if (enabled) {
                        toggle()
                        if (!isChecked) toggle()
                    }
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
                R.id.newMedia -> AddDialog().show(supportFragmentManager, "Dialog Add")
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
        curTime.text = Midi.minSecStr(this, R.string.timeCur, pos.toLong())
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
        if (DebugMode.debug) if (play == null) showMsg(R.string.error, "Play == null")
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
            showError(R.string.justTracks, R.string.trackNotSel)
            drawerLayout.openDrawer(GravityCompat.START)
            return true
        }
        return false
    }

    private fun showError(titleId: Int, msgId: Int) = msgDialog(this, titleId, msgId)
    private fun showMsg(titleId: Int, msgStr: String) = msgDialog(this, titleId, msgStr)
}