@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.Midi

import android.os.Parcel
import android.os.Parcelable
import ru.BShakhovsky.Piano_Transcription.DebugMode

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