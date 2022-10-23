package com.steamwhistle

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.ZonedDateTime

private class Converters {
    companion object {
        @TypeConverter
        @JvmStatic
        fun toZonedDateTime(zonedDateTime: ZonedDateTime): String {
            return zonedDateTime.toString()
        }

        @TypeConverter
        @JvmStatic
        fun fromZonedDateTime(zonedDateTimeString: String): ZonedDateTime {
            return ZonedDateTime.parse(zonedDateTimeString)
        }
    }
}

@Database(entities = [WatchlistGame::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
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