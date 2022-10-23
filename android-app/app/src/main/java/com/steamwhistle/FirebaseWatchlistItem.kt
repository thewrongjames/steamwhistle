package com.steamwhistle

import kotlinx.serialization.Serializable

/**
 * A representation of a watchlist item on firebase. The timestamps are broken into seconds and
 * nanos for serialisation purposes.
 */
@Serializable
data class FirebaseWatchlistItem(
    val appId: Long,
    val threshold: Long,
    val updatedSeconds: Long,
    val updatedNanos: Int,
    val createdSeconds: Long,
    val createdNanos: Int,
    val isActive: Boolean,
)