package ru.bshakhovsky.piano_transcription

import android.os.Bundle
import android.view.View

import androidx.appcompat.app.AppCompatActivity

import ru.bshakhovsky.piano_transcription.R.id.fabGuide
import ru.bshakhovsky.piano_transcription.databinding.ActivityGuideBinding

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.Share

class GuideActivity : AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(ActivityGuideBinding.inflate(layoutInflater)) {
            setContentView(root)

            setSupportActionBar(guideBar)
            DebugMode.assertState(supportActionBar != null)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            guideBar.setNavigationOnClickListener(this@GuideActivity)

            setFinishOnTouchOutside(true)

            fabGuide.setOnClickListener(this@GuideActivity)
        }
    }

    override fun onClick(view: View?) {
        DebugMode.assertArgument(view != null)
        when (view?.id) {
            -1 -> onBackPressed() // not android.R.id.home
            fabGuide -> Share.share(this)
            else -> DebugMode.assertState(false)
        }
    }
}