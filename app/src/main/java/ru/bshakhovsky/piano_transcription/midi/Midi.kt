package ru.bshakhovsky.piano_transcription.midi

import android.content.Context

import androidx.annotation.CheckResult
import androidx.annotation.StringRes

import com.pdrogfer.mididroid.MidiFile
import com.pdrogfer.mididroid.event.ChannelAftertouch
import com.pdrogfer.mididroid.event.ChannelEvent
import com.pdrogfer.mididroid.event.Controller
import com.pdrogfer.mididroid.event.MidiEvent
import com.pdrogfer.mididroid.event.NoteAftertouch
import com.pdrogfer.mididroid.event.NoteOff
import com.pdrogfer.mididroid.event.NoteOn
import com.pdrogfer.mididroid.event.PitchBend
import com.pdrogfer.mididroid.event.ProgramChange
import com.pdrogfer.mididroid.event.SystemExclusiveEvent

import com.pdrogfer.mididroid.event.meta.CopyrightNotice
import com.pdrogfer.mididroid.event.meta.CuePoint
import com.pdrogfer.mididroid.event.meta.EndOfTrack
import com.pdrogfer.mididroid.event.meta.GenericMetaEvent
import com.pdrogfer.mididroid.event.meta.InstrumentName
import com.pdrogfer.mididroid.event.meta.KeySignature
import com.pdrogfer.mididroid.event.meta.Lyrics
import com.pdrogfer.mididroid.event.meta.Marker
import com.pdrogfer.mididroid.event.meta.MidiChannelPrefix
import com.pdrogfer.mididroid.event.meta.SequenceNumber
import com.pdrogfer.mididroid.event.meta.SequencerSpecificEvent
import com.pdrogfer.mididroid.event.meta.SmpteOffset
import com.pdrogfer.mididroid.event.meta.Tempo
import com.pdrogfer.mididroid.event.meta.TimeSignature
import com.pdrogfer.mididroid.event.meta.Text
import com.pdrogfer.mididroid.event.meta.TextualMetaEvent
import com.pdrogfer.mididroid.event.meta.TrackName

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.MinSec

import java.io.InputStream
import kotlin.math.roundToLong

class Midi(inStream: InputStream, untitled: String) {

    companion object {
        fun minSecStr(context: Context, @StringRes strId: Int, mSec: Long): String =
            context.getString(strId, MinSec.minutes(mSec), MinSec.seconds(mSec))
    }

    class Chord(var mSec: Long, val notes: MutableMap<Int, Float>)
    class Track(val info: TrackInfo = TrackInfo(), var chords: Array<Chord> = emptyArray())

    val summary: Summary = Summary()
    var tracks: Array<Track> = emptyArray()
        private set
    var percuss: Array<TrackInfo> = emptyArray()
        private set

    val badMidi: Boolean
    val dur: Long

    init {
        val midi = MidiFile(inStream)
        DebugMode.assertState(
            (midi.resolution and 0x80_00) == 0,
            "Time-code-based time: ticks per frame * fps SMPTE time = ticks per second"
        )
        badMidi = midi.tracks.isEmpty()
        if (!badMidi) {
            val tempos = sortedMapOf<Long, Int>()
            val ticks = sortedMapOf<Long, Long>()
            midi.tracks.forEachIndexed { i, track ->
                val curTrack = Track()
                var (curTick, isPercuss) = (0L to false)
                track.events.forEach { event ->
                    with(event) {
                        curTick += delta
                        ticks += curTick to 0L
                        when (this) {
                            is TrackName -> curTrack.info.name = "$i $trackName"
                            is TextualMetaEvent -> when (this) {
                                is Text -> summary.text += text
                                is CopyrightNotice -> summary.copyright += notice
                                is InstrumentName -> curTrack.info.instrument = name
                                is Lyrics -> summary.lyrics += lyric
                                is Marker -> summary.marker += markerName
                                is CuePoint -> summary.cue += cue
                                else -> DebugMode.assertState(false, "Wrong text meta-event")
                            }
                            is Tempo -> {
                                tempos += curTick to mpqn
                                summary.tempos += Summary.Bpm(curTick, bpm)
                            }
                            is KeySignature -> {
                                fun majMin(maj: String, minor: String) =
                                    if (scale == 0) "$maj-Major" else "$minor-Minor"
                                summary.keys += Summary.Key(
                                    curTick, when (key) {
                                        -7 -> majMin("Cb", "Ab")
                                        -6 -> majMin("Gb", "Eb")
                                        -5 -> majMin("Db", "Bb")
                                        -4 -> majMin("Ab", "F")
                                        -3 -> majMin("Eb", "C")
                                        -2 -> majMin("Bb", "G")
                                        -1 -> majMin("F", "D")

                                        0 -> "Natural ${majMin("C", "A")}"

                                        1 -> majMin("G", "E")
                                        2 -> majMin("D", "B")
                                        3 -> majMin("A", "F#")
                                        4 -> majMin("E", "C#")
                                        5 -> majMin("B", "G#")
                                        6 -> majMin("F#", "D#")
                                        7 -> majMin("C#", "A#")

                                        else -> "unknown".also {
                                            DebugMode.assertState(false, "Wrong key signature")
                                        }
                                    }
                                )
                            }
                            is ChannelEvent -> when (channel) {
                                /* Percussion MIDI channel should be 10 instead of 9,
                                but there seems to be bug in library
                                https://github.com/LeffelMania/android-midi-lib/issues/17 */
                                9 -> isPercuss = true
                                else -> addNote(this, curTrack, curTick)
                            }
                            else -> DebugMode.assertState(
                                (this is SequencerSpecificEvent) or (this is SequenceNumber)
                                        or (this is MidiChannelPrefix) or (this is EndOfTrack)
                                        or (this is SmpteOffset) or (this is TimeSignature)
                                        or (this is SystemExclusiveEvent)
                                        or (this is GenericMetaEvent)
                            )
                        }
                    }
                }
                with(curTrack) {
                    with(info) { if (name.isNullOrEmpty()) name = untitled }
                    when {
                        isPercuss -> percuss += info
                        chords.isEmpty() -> with(info) {
                            if (!(name.isNullOrEmpty() and instrument.isNullOrEmpty()))
                                summary.garbage += "${info.name ?: ""} ${info.instrument ?: ""}"
                        }
                        else -> tracks += curTrack
                    }
                }
            }

            if (tempos.isEmpty())
                tempos += (0L to 60_000_000 / 120).also { DebugMode.assertState(false) }
            if (!tempos.containsKey(0))
                tempos += (0L to 60_000_000 / 120).also { DebugMode.assertState(false) }
            var (lastTick, lastMicSec) = 0L to 0L
            ticks.keys.forEach { tick ->
                DebugMode.assertState(tempos.containsKey(lastTick))
                tempos[lastTick]?.let { ticks[tick] = lastMicSec + (tick - lastTick) * it }
                if (tempos.containsKey(tick)) {
                    lastTick = tick
                    DebugMode.assertState(ticks[tick] != null)
                    ticks[tick]?.let { lastMicSec = it }
                }
            }
            ticks.entries.forEach { (key, value) ->
                ticks[key] = (value / 1_000.0 / midi.resolution).roundToLong()
            }

            summary.tempos.forEach { with(it) { milSec = ticks.getOrDefault(milSec, 0) } }
            summary.keys.forEach { with(it) { milSec = ticks.getOrDefault(milSec, 0) } }
            tracks.forEach { track ->
                track.chords.forEach { chord -> with(chord) { mSec = ticks.getOrDefault(mSec, 0) } }
            }
        }
        dur = if (tracks.isEmpty()) 0L else tracks.maxByOrNull {
            it.chords.last().mSec
        }!!.chords.last().mSec
    }

    @CheckResult
    private fun addNote(event: MidiEvent, curTrack: Track, curTick: Long) =
        (if (curTrack.chords.isEmpty()) true
        else curTrack.chords.last().mSec != curTick).let { newChord ->
            with(event) {
                when (this) {
                    is NoteOn -> when {
                        newChord -> curTrack.chords += Chord(
                            curTick, mutableMapOf(noteValue - 21 to velocity / 127f)
                        )
                        else -> curTrack.chords.last().notes[noteValue - 21] = maxOf(
                            curTrack.chords.last().notes[noteValue - 21] ?: 0f, velocity / 127f
                        )
                    }
                    is NoteOff -> when {
                        newChord -> curTrack.chords += Chord(
                            curTick, mutableMapOf(noteValue - 21 to 0f)
                        )
                        else -> curTrack.chords.last().notes.getOrPut(noteValue - 21) { 0f }
                    }
                    else -> DebugMode.assertState(
                        (this is NoteAftertouch) or (this is Controller)
                                or (this is ProgramChange) or (this is ChannelAftertouch)
                                or (this is PitchBend)
                    )
                }
            }
        }
}