@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.Midi

import android.os.Parcel
import android.os.Parcelable

class TrackInfo(var name: String? = null, var instrument: String? = null) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readString(), parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name); parcel.writeString(instrument)}

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<TrackInfo> {
        override fun createFromParcel(parcel: Parcel) = TrackInfo(parcel)
        override fun newArray(size: Int) = arrayOfNulls<TrackInfo>(size) }
}