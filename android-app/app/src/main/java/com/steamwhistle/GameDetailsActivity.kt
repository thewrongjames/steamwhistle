package com.steamwhistle

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView

class WatchGameActivity : AppCompatActivity() {

    lateinit var imageView: ImageView
    lateinit var titleView: TextView
    lateinit var  priceView: TextView
    lateinit var thresholdView: TextView
    lateinit var game:WatchlistGame
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch_game)
        imageView = findViewById(R.id.watchlistItemImage)
        titleView = findViewById(R.id.watchlistItemTitle)
        priceView = findViewById(R.id.watchlistItemPrice)
        thresholdView = findViewById(R.id.watchlistItemThreshold)
        game = intent.getParcelableExtra(WatchlistActivity.GAME)!!
        fillData(game)
    }

    fun fillData(game:WatchlistGame)
    {
        imageView.setImageResource(R.drawable.ic_baseline_add_24)
        titleView.text= game.name
        priceView.text = game.price.toString()
        thresholdView.text = game.threshold.toString()
    }
}