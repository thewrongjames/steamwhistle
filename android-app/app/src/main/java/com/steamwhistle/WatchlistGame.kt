package com.steamwhistle

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A concretisation of the abstract [Game] class, specifically for games that are being watched by
 * the user. This class is a [Parcelable] to allow it to be easily sent between intents.
 */
@Entity(tableName = "watchlist_games")
data class WatchlistGame(
    @PrimaryKey@ColumnInfo(name = "app_id") override val appId: Int,
    override val name: String,
    override val price: Int,
    val threshold: Int,
) : Game(), Parcelable {
    companion object CREATOR: Parcelable.Creator<WatchlistGame?> {
        override fun createFromParcel(source: Parcel): WatchlistGame {
            return WatchlistGame(source)
        }

        override fun newArray(size: Int) = arrayOfNulls<WatchlistGame>(size)
    }

    constructor(parcel: Parcel) : this(
        appId = parcel.readInt(),
        name = parcel.readString() ?: "",
        price = parcel.readInt(),
        threshold = parcel.readInt(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(appId)
        parcel.writeString(name)
        parcel.writeInt(price)
        parcel.writeInt(threshold)
    }

    override fun describeContents() = 0
}