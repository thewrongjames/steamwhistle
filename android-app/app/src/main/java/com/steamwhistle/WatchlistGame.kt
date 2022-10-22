package com.steamwhistle

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.ZonedDateTime

/**
 * A concretisation of the abstract [Game] class, specifically for games that are being watched by
 * the user. This class is a [Parcelable] to allow it to be easily sent between intents.
 */
@Entity(tableName = "watchlist_games")
data class WatchlistGame(
    @PrimaryKey@ColumnInfo(name = "app_id") override val appId: Int,
    override val name: String,
    val price: Int,
    var threshold: Int,
    val created: ZonedDateTime,
    var updated: ZonedDateTime,
    @ColumnInfo(name = "is_active") var isActive: Boolean,
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
        created = ZonedDateTime.parse(parcel.readString()),
        updated = ZonedDateTime.parse(parcel.readString()),
        isActive = parcel.readInt() != 0,
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(appId)
        parcel.writeString(name)
        parcel.writeInt(price)
        parcel.writeInt(threshold)
        parcel.writeString(created.toString())
        parcel.writeString(updated.toString())
        parcel.writeInt(if (isActive) 1 else 0)
    }

    fun getCreatedSecondsAndNanos(): Pair<Long, Int> {
        return Pair(created.toEpochSecond(), created.nano)
    }

    fun getUpdatedSecondsAndNanos(): Pair<Long, Int> {
        return Pair(created.toEpochSecond(), created.nano)
    }

    override fun describeContents() = 0
}