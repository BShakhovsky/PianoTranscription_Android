package ru.bshakhovsky.piano_transcription.main.mainUI

import android.app.Activity
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout

import androidx.lifecycle.Lifecycle

import ru.bshakhovsky.piano_transcription.R.string.app_name

import ru.bshakhovsky.piano_transcription.utils.WeakPtr

class Toggle(
    lifecycle: Lifecycle, view: View, activity: Activity, drawer: DrawerLayout, toolbar: Toolbar
) : ActionBarDrawerToggle(activity, drawer, toolbar, app_name, app_name) {

    private val content = WeakPtr(lifecycle, view)

    override fun onDrawerOpened(drawerView: View) {}
    override fun onDrawerClosed(drawerView: View) {}
    override fun onDrawerStateChanged(newState: Int) {}

    override fun onDrawerSlide(drawerView: View, slideOffset: Float): Unit = with(content.get()) {
        translationX = drawerView.width * slideOffset / 2
        scaleX = 1 - slideOffset * drawerView.width / width
        scaleY = 1 - slideOffset * drawerView.width / width
    }
}