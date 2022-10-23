package com.steamwhistle

import kotlinx.serialization.Serializable

/**
 * A representation of a tracked game on firebase. It is not complete, and only stores the
 * properties that we need locally.
 */
@Serializable
data class FirebaseGame(
    val appId: Long,
    val name: String,
    val price: Long,
)
