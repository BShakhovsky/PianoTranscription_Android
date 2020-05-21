package ru.bshakhovsky.piano_transcription.midi

import android.os.Parcel
import android.os.Parcelable
import ru.bshakhovsky.piano_transcription.utils.DebugMode

class TrackInfo(var name: String? = null, var instrument: String? = null) : Parcelable {

    constructor(parcel: Parcel) : this(parcel.readString()!!, parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int): Unit = with(parcel) {
        DebugMode.assertArgument(name != null)
//        DebugMode.assertArgument(instrument != null)
        writeString(name)
        writeString(instrument)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TrackInfo> {
        override fun createFromParcel(parcel: Parcel): TrackInfo = TrackInfo(parcel)
        override fun newArray(size: Int): Array<TrackInfo?> = arrayOfNulls(size)
    }
}