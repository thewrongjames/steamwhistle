package com.steamwhistle

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WatchlistGame::class], version = 1, exportSchema = false)
abstract class SteamWhistleDatabase : RoomDatabase() {
    companion object {
        // This implements the database as a singleton, such that only one instance is even open /
        // in use. Volatile means that the value of this variable is immediately updated on writes.
        // I think we want this so that two threads don't mistakenly try to create it for "the first
        // time". This is the underlying instance of the database, and is private. It should be
        // accessed only through getDatabase, which will ensure that only one instance is ever
        // created.
        @Volatile
        private var instance: SteamWhistleDatabase? = null

        fun getDatabase(context: Context): SteamWhistleDatabase {
            // Synchronized means that only one thing can be executing this block (or any other that
            // uses `this` as its lock) at a time.
            synchronized(this) {
                val newInstance = instance ?: Room.databaseBuilder(
                    context,
                    SteamWhistleDatabase::class.java,
                    "steam_whistle_database"
                ).build()

                instance = newInstance

                return newInstance
            }
        }
    }

    abstract fun watchlistDao(): WatchlistDao
}