package ru.bshakhovsky.piano_transcription.media.background

import android.net.Uri
import android.os.Looper
import android.view.View

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.pdrogfer.mididroid.MidiFile
import com.pdrogfer.mididroid.MidiTrack

import com.pdrogfer.mididroid.event.ChannelEvent
import com.pdrogfer.mididroid.event.NoteOff
import com.pdrogfer.mididroid.event.NoteOn

import com.pdrogfer.mididroid.event.meta.CopyrightNotice
import com.pdrogfer.mididroid.event.meta.InstrumentName
import com.pdrogfer.mididroid.event.meta.KeySignature
import com.pdrogfer.mididroid.event.meta.Tempo
import com.pdrogfer.mididroid.event.meta.Text
import com.pdrogfer.mididroid.event.meta.TextualMetaEvent
import com.pdrogfer.mididroid.event.meta.TrackName

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import ru.bshakhovsky.piano_transcription.R.string
import ru.bshakhovsky.piano_transcription.media.graphs.RollGraph
import ru.bshakhovsky.piano_transcription.media.utils.SingleLiveEvent
import ru.bshakhovsky.piano_transcription.media.utils.TfLiteModel
import ru.bshakhovsky.piano_transcription.utils.DebugMode

import java.io.File
import java.io.OutputStream

class TranscribeRoutine : ViewModel() {

    companion object {
        const val hopSize: Int = 512
        private const val cachePrefTrans = "OutputMidi_"
    }

    /* BothThreads is not WeakReference, because we need RandomFileArray onDestroy
    to close FileChannel, and cannot be sure that the variable's destructor itself
    (its lifecycle callback) will not be called first (cleaned by GC) */
    private lateinit var data: BothRoutines
    val rollGraph: RollGraph = RollGraph()

    var transStarted: Boolean = false
        private set
    private var frames = floatArrayOf()
    private var onsets = floatArrayOf()
    private var volumes = floatArrayOf()

    // Otherwise can be saved many times due to orientation change
    private val _midiSaveStart = SingleLiveEvent()
    val midiSaveStart: LiveData<Unit?> get() = _midiSaveStart

    val savedMidi: MutableLiveData<Uri> = MutableLiveData()
    private val midi = MidiFile().apply { addTrack(MidiTrack()) }

    @MainThread
    fun initialize(d: BothRoutines) {
        DebugMode.assertState(
            Looper.myLooper() == Looper.getMainLooper(),
            "TranscribeRoutine should be initialized in MediaActivity UI-thread"
        )
        data = d
    }

    @MainThread
    override fun onCleared() {
        DebugMode.assertState(
            Looper.myLooper() == Looper.getMainLooper(),
            "TranscribeRoutine should be cleared by MediaActivity UI-thread"
        )
        data.rawData.file?.close()
        clearCache()
        super.onCleared()
    }

    @MainThread
    fun startTranscribe() {
        DebugMode.assertState(
            Looper.myLooper() == Looper.getMainLooper(),
            "TranscribeRoutine should be started in MediaActivity UI-thread"
        )
        if (!transStarted) {
            transStarted = true
            viewModelScope.launch(Dispatchers.Default) { transcribe() }
        }
    }

    @WorkerThread
    private suspend fun transcribe() {
        DebugMode.assertState(
            Looper.myLooper() != Looper.getMainLooper(),
            "Transcription should be started in background thread"
        )
        with(rollGraph) {
            DebugMode.assertState(
                (graphDrawable.value == null) and (isVisible.value == View.VISIBLE),
                "Transcription has already started"
            )
        }
        DebugMode.assertState(savedMidi.value == null, "MIDI-file has already been saved")
        DebugMode.assertState(
            rollGraph.isTranscribed.value == false, "Transcription has already been completed"
        )

        with(data) {
            (TfLiteModel.outStepNotes * hopSize).let { outStepSamples ->
                with(rawData) {
                    try {
                        getZeroPadded(
                            floatLen() + outStepSamples -
                                    (floatLen() - TfLiteModel.inNumSamples) % outStepSamples
                        )
                    } catch (e: OutOfMemoryError) {
                        appContext().getString(string.memoryZeroPad, e.localizedMessage ?: e)
                            .let { errMsg ->
                                withContext(Dispatchers.Main) {
                                    ffmpegLog.value += "\n\n$errMsg"
                                    alertMsg.value = Triple(string.error, errMsg, null)
                                }
                            }
                        return@let
                    }
                }.let { paddedSong ->
                    DebugMode.assertState(
                        (paddedSong.size - TfLiteModel.inNumSamples) % outStepSamples == 0
                    )
                    TfLiteModel().apply { initialize(appContext(), ffmpegLog) }.use { model ->
                        // var (numThreads, nThFound, nanoSecs) = Triple(1, false, Long.MAX_VALUE)
                        (0..(paddedSong.size - TfLiteModel.inNumSamples) / outStepSamples)
                            .forEach { nextSecond(it, outStepSamples, model, paddedSong) }
                        appContext().getString(string.transComplete).let { msg ->
                            withContext(Dispatchers.Main) {
                                rollGraph.isTranscribed.value = true
                                ffmpegLog.value += "\n$msg"
                            }
                        }
                        makeMidi()
                        withContext(Dispatchers.Main) { _midiSaveStart.call() }
                    }
                }
            }
        }
    }

    private suspend fun nextSecond(
        curStep: Int, outStepSecs: Int, model: TfLiteModel, paddedSong: FloatArray
    ) {
        /* with(if (nThFound) model.process(this@run) else {
            model.initialize(appContext(), ffmpegLog, numThreads)
            var output: OnsetsFramesWavinput.Outputs
            measureNanoTime { output = model.process(this@run) }.let {
                if (it < nanoSecs) {
                    nanoSecs = it
                    numThreads += 1
                } else {
                    nThFound = true
                    <string name="optimal">" = optimal"</string>
                    withContext(Dispatchers.Main) { ffmpegLog.value += getString(string.optimal) }
                }
            }
            output
        }) { */
        val (nextFrames, nextOnsets, nextVolumes) = model.process(
            paddedSong.sliceArray(
                curStep * outStepSecs until curStep * outStepSecs + TfLiteModel.inNumSamples
            )
        )
        try {
            frames += nextFrames
            onsets += nextOnsets
            volumes += nextVolumes
            rollGraph.drawRoll(frames)
        } catch (e: OutOfMemoryError) {
            with(data) {
                appContext().getString(string.memoryRollGraph, e.localizedMessage ?: e)
                    .let { errMsg ->
                        withContext(Dispatchers.Main) {
                            ffmpegLog.value += "\n\n$errMsg"
                            alertMsg.value = Triple(string.error, errMsg, null)
                        }
                    }
            }
        }
    }

    @MainThread
    fun saveMidi(uri: Uri, outStream: OutputStream) {
        DebugMode.assertState(
            Looper.myLooper() == Looper.getMainLooper(),
            "MIDI-file should be saved from MediaActivity UI-thread"
        )
        DebugMode.assertState(savedMidi.value == null, "MIDI-file has already been saved")
        DebugMode.assertState(
            rollGraph.isTranscribed.value == true, "Transcription has not yet been completed"
        )
        viewModelScope.launch(Dispatchers.IO) {
            DebugMode.assertState(
                Looper.myLooper() != Looper.getMainLooper(),
                "This should be ViewModel-scope"
            )
            clearCache()
            // There is no API to write to OutputStream, so have to write to temp File,
            // then copy File to OutputStream, then delete File
            File.createTempFile(
                cachePrefTrans + "TranscribedMidi_", ".mid", data.appContext().cacheDir
            ).run {
                midi.writeToFile(this)
                outStream.write(readBytes())
                delete()
            }
            withContext(Dispatchers.Main) { savedMidi.value = uri }
        }
    }

    @WorkerThread
    private suspend fun makeMidi() {
        DebugMode.assertState(
            Looper.myLooper() != Looper.getMainLooper(),
            "MIDI-file should be created in background thread"
        )
        DebugMode.assertState(savedMidi.value == null, "MIDI-file has already been made")
        DebugMode.assertState(
            rollGraph.isTranscribed.value == true, "Transcription has not yet been completed"
        )

        DebugMode.assertState(midi.trackCount == 1)
        with(midi.tracks[0]) {
            insertEvent(CopyrightNotice(0, 0, "Used Android App created by Boris Shakhovsky"))
            insertEvent(
                Text(
                    0, 0, with(data) {
                        "Automatically transcribed from ${
                            youTubeLink?.let { "YouTube:\r\n\t$it" } ?: "audio:"
                        }\r\n\t${fileName}"
                    }
                )
            )
            insertEvent(TrackName(0, 0, "Acoustic Grand Piano"))
            insertEvent(InstrumentName(0, 0, "Acoustic Grand Piano"))
            insertEvent(Tempo(0, 0, 60_000_000 / 120))

            /* Based on
            https://github.com/tensorflow/magenta/blob/master/magenta/music/sequences_lib.py#L1844
            magenta.music.midi_ionote_sequence_to_midi_file(
            magenta.music.sequences_libpianoroll_to_note_sequence(
                fps=rate/512, min_duration=0, min_midi_pitch=21 ... */

            val starts = LongArray(88) { -1 }
            // Time is in absolute seconds, not relative MIDI ticks
            val frameLenMilSecs = hopSize * 1_000 / DecodeRoutine.sampleRate
            fun endPitch(pitch: Int, endFrame: Int) {
                volumes[(starts[pitch].toInt() * starts.size + pitch)].let { vol ->
                    if (vol !in 0f..1f) DebugMode.assertState(false)
                    insertNote(
                        0, pitch + 21,
                        (volumes[starts[pitch].toInt() * starts.size + pitch] * 80 + 10).toInt(),
                        starts[pitch] * frameLenMilSecs,
                        (endFrame - starts[pitch]) * frameLenMilSecs
                    )
                }
                starts[pitch] = -1
            }

            // Add silent frame at the end so we can do a final loop
            // and terminate any notes that are still active
            frames += FloatArray(starts.size) { Float.NEGATIVE_INFINITY }
            (0 until frames.size / starts.size).forEach { i ->
                starts.indices.forEach { pitch ->
                    (i * starts.size + pitch).let { index ->
                        if (frames[index] > TfLiteModel.threshold) {
                            if (starts[pitch] == -1L) {
                                if (onsets[i * starts.size + pitch] > TfLiteModel.threshold)
                                // Start a note only if we have predicted an onset
                                    starts[pitch] = i.toLong()
                                // else; // Even though the frame is active,
                                // there is no onset, so ignore it
                            } else if ((onsets[index] > TfLiteModel.threshold) and
                                (onsets[(i - 1) * starts.size + pitch] > TfLiteModel.threshold)
                            ) {
                                // Pitch is already active, but because of a new onset,
                                endPitch(pitch, i) // we should end the note
                                starts[pitch] = i.toLong() // and start a new one
                            }
                        } else {
                            DebugMode.assertState(frames[index] < TfLiteModel.threshold)
                            if (starts[pitch] != -1L) endPitch(pitch, i)
                        }
                    }
                }
            }
            DebugMode.assertState(
                (frames.size / starts.size) * frameLenMilSecs >= midi.tracks[0].events.last().tick,
                "Wrong MIDI sequence duration"
            )
        }
        gamma()
    }

    @WorkerThread
    private suspend fun gamma() {
        DebugMode.assertState(
            Looper.myLooper() != Looper.getMainLooper(),
            "Gamma should be calculated in background thread"
        )
        DebugMode.assertState(savedMidi.value == null, "MIDI-file has already been saved")
        DebugMode.assertState(
            rollGraph.isTranscribed.value == true, "Transcription has not yet been completed"
        )

        IntArray(12).let { notesCount ->
            DebugMode.assertState(midi.trackCount == 1)
            with(data) {
                midi.tracks[0].events.forEach { event ->
                    with(event) {
                        when (this) {
                            is TrackName -> DebugMode
                                .assertState(trackName == "Acoustic Grand Piano")
                            is TextualMetaEvent -> when (this) {
                                is InstrumentName -> DebugMode
                                    .assertState(name == "Acoustic Grand Piano")
                                is CopyrightNotice -> DebugMode.assertState(
                                    notice == "Used Android App created by Boris Shakhovsky"
                                )
                                is Text -> DebugMode.assertState(
                                    text == "Automatically transcribed from ${
                                        youTubeLink?.let { "YouTube:\r\n\t$it" } ?: "audio:"
                                    }\r\n\t$fileName")
                                else -> DebugMode.assertState(false, "Wrong text meta-event")
                            }
                            is Tempo -> DebugMode.assertState(bpm == 120f)
                            is ChannelEvent -> {
                                DebugMode.assertState(channel == 0)
                                when (this) {
                                    is NoteOn -> notesCount[noteValue % notesCount.size] += 1
                                    else -> DebugMode.assertState(this is NoteOff)
                                }
                            }
                            else -> DebugMode.assertState(false, "Wrong Midi-event")
                        }
                    }
                }
                with(notesCount.mapIndexed { index, i ->
                    Pair(
                        arrayOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")
                                [index], i
                    )
                }.sortedBy { it.second }.reversed().slice(0 until 7).map {
                    with(it)
                    { DebugMode.assertState(second >= 0).let { if (second > 0) first else "" } }
                }) {
                    joinToString(" ", "${appContext().getString(string.scale)}:\t\t")
                        .let { withContext(Dispatchers.Main) { data.ffmpegLog.value += "\n${it}" } }
                    keySign(this)
                }
            }
        }
    }

    @WorkerThread
    private suspend fun keySign(gamma: List<String>) {
        DebugMode.assertState(
            Looper.myLooper() != Looper.getMainLooper(),
            "Key-signature should be calculated in background thread"
        )
        DebugMode.assertState(savedMidi.value == null, "MIDI-file has already been saved")
        DebugMode.assertState(
            rollGraph.isTranscribed.value == true, "Transcription has not yet been completed"
        )

        gamma.filter { it.length > 1 }.sorted().let { blacks ->
            var result: String? = null

            fun majorMinor(mj: String, mn: String) =
            // Gamma may have none of these two notes, it may actually have just one note
                // DebugMode.assertState(maxOf(gamma.indexOf(mj), gamma.indexOf(mn)) >= 0).let {
                when {
                    gamma.indexOf(mn) == -1 -> mj // Major by default
                    gamma.indexOf(mj) == -1 -> (mn + 'm')
                    gamma.indexOf(mj) < gamma.indexOf(mn) -> mj
                    else -> mn + 'm'
                }

            if (blacks.isEmpty()) result = majorMinor("C", "A")
            else if (blacks.size == 1) {
                if (blacks[0] == "F#") {
                    if ("F" !in gamma) result = majorMinor("G", "E")
                } else if (blacks[0] == "Bb") {
                    if ("B" !in gamma) result = majorMinor("F", "D")
                }
            } else if (blacks.size == 2) {
                if (blacks == listOf("C#", "F#")) {
                    if (("C" !in gamma) and ("F" !in gamma)) result = majorMinor("D", "B")
                } else if (blacks == listOf("Bb", "Eb")) {
                    if (("B" !in gamma) and ("E" !in gamma)) result = majorMinor("Bb", "G")
                }
            } else if (blacks.size == 3) {
                if (blacks == listOf("Ab", "C#", "F#")) {
                    if (("C" !in gamma) and ("F" !in gamma) and ("G" !in gamma))
                        result = majorMinor("A", "F#")
                } else if (blacks == listOf("Ab", "Bb", "Eb")) {
                    if (("A" !in gamma) and ("B" !in gamma) and ("E" !in gamma))
                        result = majorMinor("Eb", "C")
                }
            } else if (blacks.size == 4) {
                if (blacks == listOf("Ab", "C#", "Eb", "F#")) {
                    if (("C" !in gamma) and ("D" !in gamma) and ("F" !in gamma) and ("G" !in gamma))
                        result = majorMinor("E", "C#")
                } else if (blacks == listOf("Ab", "Bb", "C#", "Eb")) {
                    if (("A" !in gamma) and ("B" !in gamma) and ("D" !in gamma) and ("E" !in gamma))
                        result = majorMinor("Ab", "F")
                }
            } else if (("B" !in gamma) and ("E" !in gamma)) result = majorMinor("B", "Ab")
            else if (("C" !in gamma) and ("F" !in gamma)) result = majorMinor("C#", "Bb")

            with(data) {
                appContext().run {
                    if (result != null)
                        addKeySign(result ?: DebugMode.assertState(false).let { return@run })
                    else result = getString(string.badScale)
                    withContext(Dispatchers.Main)
                    { ffmpegLog.value += "\n${getString(string.keySign)}:\t$result" }
                }
            }
        }
    }

    @WorkerThread
    private fun addKeySign(keySign: String) {
        DebugMode.assertState(
            Looper.myLooper() != Looper.getMainLooper(),
            "Key-signature should be added to MIDI-file in background thread"
        )
        DebugMode.assertState(savedMidi.value == null, "MIDI-file has already been saved")
        DebugMode.assertState(
            rollGraph.isTranscribed.value == true, "Transcription has not yet been completed"
        )

        DebugMode.assertState(midi.trackCount == 1)
        midi.tracks[0].insertEvent(
            KeySignature(
                0, 0, when (keySign) {
                    in arrayOf("C", "Am") -> 0
                    in arrayOf("G", "Em") -> 1
                    in arrayOf("D", "Bm") -> 2
                    in arrayOf("A", "F#m") -> 3
                    in arrayOf("E", "C#m") -> 4

                    in arrayOf("B", "Abm") -> 5 // also -7
                    in arrayOf("F#", "Ebm") -> 6 // also -6
                    in arrayOf("C#", "Bbm") -> -5 // also 7

                    in arrayOf("Ab", "Fm") -> -4
                    in arrayOf("Eb", "Cm") -> -3
                    in arrayOf("Bb", "Gm") -> -2
                    in arrayOf("F", "Dm") -> -1

                    else -> 0.also { DebugMode.assertState(false, "Wrong key signature") }
                }, if (keySign.last() == 'm') 1 else 0
            )
        )
    }

    // Both threads
    private fun clearCache() = data.clearCache(cachePrefTrans)
}