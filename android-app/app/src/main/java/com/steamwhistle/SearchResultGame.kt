package com.steamwhistle

/**
 * A concretisation of the abstract [Game] class, specifically for games that are returned in search
 * results.
 */
data class SearchResultGame(
    override val appId: Int,
    override val name: String,
    override val price: Int
) : Game()