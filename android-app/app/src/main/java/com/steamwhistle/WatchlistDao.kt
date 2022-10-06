package com.steamwhistle

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WatchlistDao {
    @Insert
    suspend fun insert(watchlistGame: WatchlistGame)

    @Query("SELECT * FROM watchlist_games ORDER BY name")
    fun getWatchlistGames(): LiveData<List<WatchlistGame>>
}