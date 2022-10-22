package com.steamwhistle

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class GameDetailsActivity : AppCompatActivity() {

    private lateinit var titleView: TextView
    lateinit var  priceView: TextView
    lateinit var thresholdView: EditText
    lateinit var saveButton: Button
    lateinit var backButton: ImageButton
    lateinit var game:WatchlistGame
    lateinit var lastThresholdText: String

    private val viewModel: WatchlistViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_game_details)
        titleView = findViewById(R.id.gameTitle)
        priceView = findViewById(R.id.gamePrice)
        thresholdView = findViewById(R.id.gameThreshold)
        backButton = findViewById(R.id.gameDetailsBackButton)
        saveButton = findViewById(R.id.saveButton)
        lastThresholdText = "A$20.00"

        thresholdView.addTextChangedListener(textWatcher)

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
        priceView.text = CurrencyUtils.toCurrency(game.price)
        thresholdView.setText(CurrencyUtils.toCurrency(game.threshold))
    }

    private fun showConfirmDialogue() {

        if (game.threshold == CurrencyUtils.toCents(thresholdView.text.toString(), game.threshold)) {
            setResult(RESULT_CANCELED)
            finish()
        }

        AlertDialog.Builder(this)
            .setMessage(getString(R.string.back_message))
            .setPositiveButton(R.string.yes) {_, _ ->
                setResult(RESULT_CANCELED)
                finish()
            }
            .setNegativeButton(R.string.no){_, _ -> }
            .create()
            .show()
    }

    private fun save() {
        game.threshold = CurrencyUtils.toCents(thresholdView.text.toString(), game.threshold)
        viewModel.viewModelScope.launch {
            viewModel.updateGame(game)
        }
        finish()
    }

    private val textWatcher = object : TextWatcher{
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            if (!s.toString().matches("A\\\$(,|\\d){0,8}(\\.\\d?\\d?)?".toRegex())) {
                thresholdView.setText(lastThresholdText)
            } else {
                lastThresholdText = s.toString()
            }
        }
    }
}