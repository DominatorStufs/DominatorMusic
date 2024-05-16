package app.vitune.android.models

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import app.vitune.android.lib.providers.innertube.Innertube

data class Mood(
    val name: String,
    val color: Color,
    val browseId: String?,
    val params: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        name = parcel.readString()!!,
        color = Color(parcel.readLong()),
        browseId = parcel.readString()!!,
        params = parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) = with(parcel) {
        writeString(name)
        writeLong(color.value.toLong())
        writeString(browseId)
        writeString(params)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Mood> {
        override fun createFromParcel(parcel: Parcel) = Mood(parcel)
        override fun newArray(size: Int) = arrayOfNulls<Mood>(size)
    }
}

fun Innertube.Mood.Item.toUiMood() = Mood(
    name = title,
    color = Color(stripeColor),
    browseId = endpoint.browseId,
    params = endpoint.params
)
