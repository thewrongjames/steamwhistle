package com.steamwhistle

/**
 * A abstract representation of a game. The [appId] should be the steam app ID, and will be used to
 * identify the game. The [price] should be in cents.
 */
abstract class Game {
    abstract val appId: Int
    abstract val name: String
    abstract val price: Int

    // Games must be comparable.
    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
}