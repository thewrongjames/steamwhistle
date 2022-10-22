package com.steamwhistle

import android.app.AlertDialog
import android.icu.text.NumberFormat
import android.icu.util.Currency
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class GameDetailsActivity : AppCompatActivity() {

    lateinit var titleView: TextView
    lateinit var  priceView: TextView
    lateinit var thresholdView: EditText
    lateinit var saveButton: Button
    lateinit var backButton: ImageButton
    lateinit var game:WatchlistGame

    private val viewModel: WatchlistViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_game_details)
        titleView = findViewById(R.id.gameTitle)
        priceView = findViewById(R.id.gamePrice)
        thresholdView = findViewById(R.id.gameThreshold)
        backButton = findViewById(R.id.gameDetailsBackButton)
        saveButton = findViewById(R.id.saveButton)

        // Save button listener
        backButton.setOnClickListener {
            showConfirmDialogue()
        }

        // Save button listener
        saveButton.setOnClickListener {
            save()
        }

        val receivedGame: WatchlistGame? = if (android.os.Build.VERSION.SDK_INT < 33) {
            // This is deprecated, but the replacement (below) only works in API 33 and up, which
            // is the newest, so I don't really want to force the minSDK up to that.
            intent.getParcelableExtra(WatchlistActivity.GAME_EXTRA_ID)
        } else {
            intent.getParcelableExtra(WatchlistActivity.GAME_EXTRA_ID, WatchlistGame::class.java)
        }

        if (null == receivedGame) {
            finish()
            return
        }

        game = receivedGame
        fillData(game)
    }

    private fun fillData(game:WatchlistGame) {
        titleView.text = game.name
        priceView.text = toCurrency(game.price)
        thresholdView.setText(toCurrency(game.threshold))
    }

    private fun toCurrency(cent: Int) : String {
        val format: NumberFormat = NumberFormat.getCurrencyInstance()
        format.maximumFractionDigits = 2
        format.currency = Currency.getInstance("AUD")

        return format.format(cent/100)
    }

    private fun toCents(currency: String) : Int {
        var cents = currency
            .replace("A$", "")
            .replace(".", "")
        return cents.toInt()
    }

    private fun showConfirmDialogue() {
        AlertDialog.Builder(this)
            .setMessage("Any unsaved changes will be lost. Are you sure you want to go back?")
            .setPositiveButton(R.string.yes) {_, _ ->
                setResult(RESULT_CANCELED)
                finish()
            }
            .setNegativeButton(R.string.no){_, _ -> }
            .create()
            .show()
    }

    private fun save() {
        game.threshold = toCents(thresholdView.text.toString())
        Log.i("DETAILS SAVE", "THRESHOLD: ${thresholdView.text.toString()}")
        viewModel.viewModelScope.launch {
            viewModel.updateGame(game)
        }
        finish()
    }
}