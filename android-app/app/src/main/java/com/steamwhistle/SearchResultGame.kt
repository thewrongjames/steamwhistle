package com.steamwhistle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A concretisation of the abstract [Game] class, specifically for games that are returned in search
 * results. Uses kotlin serialization to convert Algolia's json response into the objects easily.
 */
@Serializable
data class SearchResultGame(
    @SerialName("objectID")
    override val appId: Long,
    override val name: String,
) : Game()