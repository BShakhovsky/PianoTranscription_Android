package ru.bshakhovsky.piano_transcription.midi

import android.content.Intent
import android.os.Bundle
import android.view.View

import androidx.annotation.CheckResult
import androidx.appcompat.app.AppCompatActivity

import ru.bshakhovsky.piano_transcription.R.id.fabMidi
import ru.bshakhovsky.piano_transcription.R.string
import ru.bshakhovsky.piano_transcription.databinding.ActivityMidiBinding

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage

import kotlin.math.roundToInt

class MidiActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var content: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(ActivityMidiBinding.inflate(layoutInflater)) {
            setContentView(root)

            setSupportActionBar(midiBar)
            DebugMode.assertState(supportActionBar != null)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            midiBar.setNavigationOnClickListener(this@MidiActivity)

            setFinishOnTouchOutside(true)

            fabMidi.setOnClickListener(this@MidiActivity)

            intent.getParcelableExtra<Summary>("Summary").also { summary ->
                DebugMode.assertState(summary != null)
                summary?.let { s ->
                    with(midiInfo) {
                        content = getString(string.midiSum)

                        if (s.copyright.isEmpty()) removeView(copyrightGroup)
                        else copyright.text = s.copyright.joinToString("\n")
                            .also { content += "\n\n\n${getString(string.copyright)}\n\n$it" }

                        if (s.keys.isEmpty()) removeView(keysGroup)
                        else keys.text = s.keys.joinToString("\n") {
                            with(it) { "${Midi.minSecStr(context, string.timeCur, milSec)} $key" }
                        }.also { content += "\n\n\n${getString(string.keys)}\n\n$it" }
                        if (s.tempos.isEmpty()) removeView(temposGroup)
                        else tempos.text = s.tempos.joinToString("\n") {
                            with(it) {
                                "${
                                    Midi.minSecStr(context, string.timeCur, milSec)
                                } BPM ${bpm.roundToInt()}"
                            }
                        }.also { content += "\n\n\n${getString(string.tempos)}\n\n$it" }

                        if (s.lyrics.isEmpty()) removeView(lyricsGroup)
                        else lyrics.text = s.lyrics.joinToString("\n")
                            .also { content += "\n\n\n${getString(string.lyrics)}\n\n$it" }

                        if (s.marker.isEmpty()) removeView(markerGroup)
                        else marker.text = s.marker.joinToString("\n")
                            .also { content += "\n\n\n${getString(string.marker)}\n\n$it" }

                        if (s.cue.isEmpty()) removeView(cueGroup)
                        else cue.text = s.cue.joinToString("\n")
                            .also { content += "\n\n\n${getString(string.cue)}\n\n$it" }

                        if (s.text.isEmpty()) removeView(otherGroup)
                        else otherText.text = s.text.joinToString("\n")
                            .also { content += "\n\n\n${getString(string.otherText)}\n\n$it" }

                        if (s.garbage.isEmpty()) removeView(garbageGroup)
                        else garbage.text = s.garbage.joinToString("\n")
                            .also { content += "\n\n\n${getString(string.garbage)}\n\n$it" }
                    }
                }
                @CheckResult
                fun joinText(key: String): String {
                    DebugMode.assertState(
                        (intent.hasExtra(key)) and (intent.getParcelableArrayExtra(key) != null)
                    )
                    with(intent.getParcelableArrayExtra(key)?.joinToString("\n") {
                        with(it as TrackInfo) { "$name ${instrument ?: ""}" }
                    }) { return if (isNullOrEmpty()) "N/A" else this }
                }
                harm.text = joinText("Tracks")
                    .also { content += "\n\n\n${getString(string.harm)}\n\n$it" }
                percuss.text = joinText("Percuss")
                    .also { content += "\n\n\n${getString(string.percuss)}\n\n$it" }
            }
        }
    }

    override fun onClick(view: View?) {
        DebugMode.assertArgument(view != null)
        when (view?.id) {
            -1 -> onBackPressed() // not android.R.id.home
            fabMidi -> startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(string.midiSum))
                putExtra(Intent.EXTRA_TEXT, content)
                DebugMode.assertState(resolveActivity(packageManager) != null)
                InfoMessage.toast(applicationContext, string.midiShare)
            }, null))
            else -> DebugMode.assertState(false)
        }
    }
}