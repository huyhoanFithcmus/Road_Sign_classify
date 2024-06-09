package org.tensorflow.lite.examples.classification.model

import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp

data class SpeedData(
    val speed: Double,
    val timestamp: Timestamp?,
    val routePoint: LatLng?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readDouble(),
        parcel.readParcelable(Timestamp::class.java.classLoader),
        parcel.readParcelable(LatLng::class.java.classLoader)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(speed)
        parcel.writeParcelable(timestamp, flags)
        parcel.writeParcelable(routePoint, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SpeedData> {
        override fun createFromParcel(parcel: Parcel): SpeedData {
            return SpeedData(parcel)
        }

        override fun newArray(size: Int): Array<SpeedData?> {
            return arrayOfNulls(size)
        }
    }
}
