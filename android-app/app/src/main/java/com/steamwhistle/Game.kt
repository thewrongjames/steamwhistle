package com.steamwhistle

/**
 * A abstract representation of a game. The [appId] should be the steam app ID, and will be used to
 * identify the game.
 */
abstract class Game {
    abstract val appId: Long
    abstract val name: String

    // Games must be comparable.
    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
}