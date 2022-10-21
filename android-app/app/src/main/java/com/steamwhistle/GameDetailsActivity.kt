package com.steamwhistle

import android.icu.text.NumberFormat
import android.icu.util.Currency
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class GameDetailsActivity : AppCompatActivity() {

    lateinit var titleView: TextView
    lateinit var  priceView: TextView
    lateinit var thresholdView: EditText
    lateinit var game:WatchlistGame

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_game_details)
        titleView = findViewById(R.id.gameTitle)
        priceView = findViewById(R.id.gamePrice)
        thresholdView = findViewById(R.id.gameThreshold)

        game = intent.getParcelableExtra(WatchlistActivity.GAME)!!
        fillData(game)
    }

    private fun fillData(game:WatchlistGame) {
        titleView.text = game.name
        priceView.text = toCurrency(game.price.toString())
        thresholdView.setText(toCurrency(game.threshold.toString()))
    }

    private fun toCurrency(cent: String) : String {
        val format: NumberFormat = NumberFormat.getCurrencyInstance()
        format.maximumFractionDigits = 2
        format.currency = Currency.getInstance("AUD")

        return format.format(cent.toInt()/100)
    }
}