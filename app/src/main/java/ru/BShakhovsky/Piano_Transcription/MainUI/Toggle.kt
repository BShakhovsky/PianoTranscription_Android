@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.MainUI

import android.app.Activity
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import ru.BShakhovsky.Piano_Transcription.R

class Toggle(
    activity: Activity, drawer: DrawerLayout, toolbar: Toolbar, private val content: View
) : ActionBarDrawerToggle(activity, drawer, toolbar, R.string.app_name, R.string.app_name) {

    override fun onDrawerOpened(drawerView: View) {}
    override fun onDrawerClosed(drawerView: View) {}
    override fun onDrawerStateChanged(newState: Int) {}

    override fun onDrawerSlide(drawerView: View, slideOffset: Float): Unit = with(content) {
        translationX = drawerView.width * slideOffset / 2
        scaleX = 1 - slideOffset * drawerView.width / width
        scaleY = 1 - slideOffset * drawerView.width / width
    }
}