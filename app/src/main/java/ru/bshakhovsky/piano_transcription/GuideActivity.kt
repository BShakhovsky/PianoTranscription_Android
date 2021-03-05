package ru.bshakhovsky.piano_transcription

import android.content.Intent
import android.os.Bundle
import android.view.View

import androidx.appcompat.app.AppCompatActivity

import ru.bshakhovsky.piano_transcription.R.id.fabGuide
import ru.bshakhovsky.piano_transcription.R.string
import ru.bshakhovsky.piano_transcription.databinding.ActivityGuideBinding

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage

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
            fabGuide -> startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "${getString(string.app_name)} for Android")
                putExtra(
                    Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=$packageName"
                )
                DebugMode.assertState(resolveActivity(packageManager) != null)
                InfoMessage.toast(applicationContext, string.guideShare)
            }, null))
            else -> DebugMode.assertState(false)
        }
    }
}