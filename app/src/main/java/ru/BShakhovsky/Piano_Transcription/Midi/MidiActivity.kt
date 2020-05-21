package ru.bshakhovsky.piano_transcription.midi

import android.os.Bundle
import android.view.View
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_midi.cue
import kotlinx.android.synthetic.main.activity_midi.cueGroup
import kotlinx.android.synthetic.main.activity_midi.copyright
import kotlinx.android.synthetic.main.activity_midi.copyrightGroup
import kotlinx.android.synthetic.main.activity_midi.fabMidi
import kotlinx.android.synthetic.main.activity_midi.garbage
import kotlinx.android.synthetic.main.activity_midi.garbageGroup
import kotlinx.android.synthetic.main.activity_midi.harm
import kotlinx.android.synthetic.main.activity_midi.keys
import kotlinx.android.synthetic.main.activity_midi.keysGroup
import kotlinx.android.synthetic.main.activity_midi.lyrics
import kotlinx.android.synthetic.main.activity_midi.lyricsGroup
import kotlinx.android.synthetic.main.activity_midi.marker
import kotlinx.android.synthetic.main.activity_midi.markerGroup
import kotlinx.android.synthetic.main.activity_midi.midiBar
import kotlinx.android.synthetic.main.activity_midi.midiInfo
import kotlinx.android.synthetic.main.activity_midi.otherText
import kotlinx.android.synthetic.main.activity_midi.otherGroup
import kotlinx.android.synthetic.main.activity_midi.percuss
import kotlinx.android.synthetic.main.activity_midi.tempos
import kotlinx.android.synthetic.main.activity_midi.temposGroup

import ru.bshakhovsky.piano_transcription.R.layout.activity_midi
import ru.bshakhovsky.piano_transcription.R.string

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import kotlin.math.roundToInt

class MidiActivity : AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_midi)

        setSupportActionBar(midiBar)
        DebugMode.assertState(supportActionBar != null)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        midiBar.setNavigationOnClickListener(this)

        setFinishOnTouchOutside(true)

        fabMidi.setOnClickListener { view ->
            // TODO: Midi Info --> "Share" button
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).show()
        }

        intent.getParcelableExtra<Summary>("Summary").also { summary ->
            DebugMode.assertState(summary != null)
            summary?.let { s ->
                with(midiInfo) {
                    if (s.copyright.isEmpty()) removeView(copyrightGroup)
                    else copyright.text = s.copyright.joinToString("\n")

                    if (s.keys.isEmpty()) removeView(keysGroup)
                    else keys.text = s.keys.joinToString("\n") {
                        with(it) { "${Midi.minSecStr(context, string.timeCur, milSec)} $key" }
                    }
                    if (s.tempos.isEmpty()) removeView(temposGroup)
                    else tempos.text = s.tempos.joinToString("\n") {
                        with(it) {
                            "${Midi.minSecStr(context, string.timeCur, milSec)
                            } BPM ${bpm.roundToInt()}"
                        }
                    }

                    if (s.lyrics.isEmpty()) removeView(lyricsGroup)
                    else lyrics.text = s.lyrics.joinToString("\n")

                    if (s.marker.isEmpty()) removeView(markerGroup)
                    else marker.text = s.marker.joinToString("\n")

                    if (s.cue.isEmpty()) removeView(cueGroup)
                    else cue.text = s.cue.joinToString("\n")

                    if (s.text.isEmpty()) removeView(otherGroup)
                    else otherText.text = s.text.joinToString("\n")

                    if (s.garbage.isEmpty()) removeView(garbageGroup)
                    else garbage.text = s.garbage.joinToString("\n")
                }
            }
            fun joinText(key: String): String {
                DebugMode.assertState(
                    (intent.hasExtra(key)) and (intent.getParcelableArrayExtra(key) != null)
                )
                with(intent.getParcelableArrayExtra(key)?.joinToString("\n") {
                    with(it as TrackInfo) { "$name ${instrument ?: ""}" }
                }) { return if (isNullOrEmpty()) "N/A" else this!! }
            }
            harm.text = joinText("Tracks")
            percuss.text = joinText("Percuss")
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