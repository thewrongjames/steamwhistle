package com.steamwhistle

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A concretisation of the abstract [Game] class, specifically for games that are being watched by
 * the user.
 */
@Entity(tableName = "watchlist_games")
data class WatchlistGame(
    @PrimaryKey@ColumnInfo(name = "app_id") override val appId: Int,
    override val name: String,
    override val price: Int,
    val threshold: Int,
) : Game()