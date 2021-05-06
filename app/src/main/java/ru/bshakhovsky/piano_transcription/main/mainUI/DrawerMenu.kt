package ru.bshakhovsky.piano_transcription.main.mainUI

import android.app.Activity
import android.content.Intent
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.Switch

import androidx.annotation.CheckResult
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout

import androidx.lifecycle.Lifecycle

import com.google.android.material.navigation.NavigationView
import com.google.android.play.core.review.ReviewManagerFactory

import ru.bshakhovsky.piano_transcription.R // id
import ru.bshakhovsky.piano_transcription.R.id
import ru.bshakhovsky.piano_transcription.R.string

import ru.bshakhovsky.piano_transcription.GuideActivity
import ru.bshakhovsky.piano_transcription.main.MainActivity
import ru.bshakhovsky.piano_transcription.main.openGL.Render
import ru.bshakhovsky.piano_transcription.main.play.Play
import ru.bshakhovsky.piano_transcription.midi.Midi
import ru.bshakhovsky.piano_transcription.midi.MidiActivity
import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage
import ru.bshakhovsky.piano_transcription.utils.WeakPtr

class DrawerMenu(
    lifecycle: Lifecycle, view: View, a: Activity, dLayout: DrawerLayout, dMenu: NavigationView,
    toolbar: Toolbar, private val render: Render, private val play: Play
) : ActionBarDrawerToggle(a, dLayout, toolbar, string.app_name, string.app_name),
    NavigationView.OnNavigationItemSelectedListener, CompoundButton.OnCheckedChangeListener {

    var midi: Midi? = null

    private val content = WeakPtr(lifecycle, view)

    override fun onDrawerOpened(drawerView: View) {}
    override fun onDrawerClosed(drawerView: View) {}
    override fun onDrawerStateChanged(newState: Int) {}

    override fun onDrawerSlide(drawerView: View, slideOffset: Float): Unit = with(content.get()) {
        translationX = drawerView.width * slideOffset / 2
        scaleX = 1 - slideOffset * drawerView.width / width
        scaleY = 1 - slideOffset * drawerView.width / width
    }


    private val activity = WeakPtr(lifecycle, a)
    private val drawerLayout = WeakPtr(lifecycle, dLayout)
    private val drawerMenu = WeakPtr(lifecycle, dMenu)

    override fun onNavigationItemSelected(item: MenuItem): Boolean = true.also {
        activity.get().run {
            when (item.itemId) {
                id.drawerMidi -> startActivity(Intent(this, MidiActivity::class.java).apply {
                    DebugMode.assertState(midi != null)
                    midi?.run {
                        putExtra("Summary", summary)
                        putExtra("Tracks", tracks.map { it.info }.toTypedArray())
                        putExtra("Percuss", percuss)
                    }
                })
                id.drawerGuide -> startActivity(Intent(this, GuideActivity::class.java))
                id.drawerRate -> with(ReviewManagerFactory.create(applicationContext)) {
                    requestReviewFlow().addOnCompleteListener {
                        with(it) { if (isSuccessful) launchReviewFlow(this@run, result) }
                    }
                }
                else -> {
                    checkGroup(item.groupId, item.itemId)
                    (item.actionView as Switch).toggle()
                    return false
                }
            }
            drawerLayout.get().closeDrawer(GravityCompat.START)
        }
    }

    override fun onCheckedChanged(button: CompoundButton?, checked: Boolean) {
        DebugMode.assertArgument(button != null)
        button?.run {
            DebugMode.assertState(midi != null)
            midi?.run {
                drawerMenu.get().menu.let { menu ->
                    when (id) {
                        MainActivity.DrawerItem.CHECK_ALL.id -> for (i in 0..tracks.lastIndex) {
                            with(menu.findItem(R.id.drawerTracks).subMenu.findItem(i)) {
                                if (checkGroup(groupId, itemId))
                                    (actionView as Switch).isChecked = button.isChecked
                            }
                        }
                        else -> {
                            DebugMode.assertState(id in 0..tracks.lastIndex)
                            play.run {
                                DebugMode.assertState(isPlaying.value != null)
                                with(activity.get()) {
                                    when {
                                        isChecked -> addTrack(id)
                                        (isPlaying.value ?: return@with)
                                                and (numSelTracks() == 1) -> {
                                            InfoMessage.dialog(
                                                this, string.justTracks, string.trackNotSelPlaying
                                            )
                                            performClick()
                                            return@with
                                        }
                                        else -> {
                                            removeTrack(id)
                                            render.releaseAllKeys()
                                        }
                                    }
                                    (menu.findItem(R.id.drawerAll).actionView as Switch).text =
                                        getString(string.tracks, numSelTracks(), tracks.size)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @CheckResult
    private fun checkGroup(groupId: Int, itemId: Int) = @Suppress("Reformat") when (groupId) {
        0                       -> false.also { DebugMode.assertArgument(itemId == id.drawerAll) }
        MainActivity.DrawerGroup.TRACKS.id   -> true.also {
            DebugMode.assertState(midi != null)
            midi?.run { DebugMode.assertArgument(itemId in 0..tracks.lastIndex) }
        } else                  -> false.also { DebugMode.assertArgument(false) }
    }
}