package com.steamwhistle

data class User(
    val devices: MutableList<String> = mutableListOf(),
    val watchlist: MutableMap<String, Int> = mutableMapOf()
)
