package com.steamwhistle

import android.os.Parcel
import android.os.Parcelable

/**
 * A concretisation of the abstract [Game] class, specifically for games that are returned in search
 * results.
 */
data class SearchResultGame(
    override val appId: Int,
    override val name: String,
    override val price: Int
) : Game()