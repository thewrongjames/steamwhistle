package com.steamwhistle

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText

class AddToWatchlistActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_to_watchlist)

        findViewById<EditText>(R.id.addToWatchlistSearchText).setOnEditorActionListener {
            view, _, _ ->
            onSearch(view)
            true
        }
    }

    fun onBackClick(view: View) {
        setResult(RESULT_CANCELED)
        finish()
    }

    fun onSettingsClick(view: View) {
        AlertDialog.Builder(this)
            .setMessage("Not implemented.")
            .setPositiveButton(R.string.okay) {_, _ -> }
            .create()
            .show()
    }

    fun onSearch(view: View) {
        AlertDialog.Builder(this)
            .setMessage("Not implemented.")
            .setPositiveButton(R.string.okay) {_, _ -> }
            .create()
            .show()
    }
}