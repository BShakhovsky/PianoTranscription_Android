package ru.bshakhovsky.piano_transcription

import android.os.Bundle
import android.view.View

import androidx.appcompat.app.AppCompatActivity

import com.google.android.material.snackbar.Snackbar

import ru.bshakhovsky.piano_transcription.databinding.ActivityGuideBinding

import ru.bshakhovsky.piano_transcription.utils.DebugMode

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

            fabGuide.setOnClickListener {
                // TODO: User Guide --> "Share" button
                Snackbar.make(it, "Replace with your own action", Snackbar.LENGTH_LONG).show()
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
}