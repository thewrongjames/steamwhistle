package au.edu.sydney.comp5216.assignment1

import android.widget.EditText
import android.os.Bundle
import android.content.Intent
import android.app.Activity
import android.app.AlertDialog
import android.view.View
import android.widget.TextView
import android.widget.Toast
import java.time.ZonedDateTime

class AddOrEditItem : Activity() {
    var position: Int = -1
    var originalItem: ShoppingListItem? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_add_or_edit_item)

        val deserialised = ShoppingListItem.deserialiseFromIntent(intent)
        if (deserialised != null) {
            originalItem = deserialised.first
            position = deserialised.second
        }

        if (position < 0) {
            findViewById<TextView>(R.id.addOrEditAction).text = getString(R.string.add_item)
        }

        findViewById<EditText>(R.id.setItemName).setText(originalItem?.name ?: "")
        findViewById<EditText>(R.id.setItemQuantity).setText(originalItem?.quantity ?: "")
    }

    fun onCancel(view: View?) {
        val message = if (position < 0) {
            getString(R.string.cancel_add_alert_message)
        } else {
            getString(R.string.cancel_edit_alert_message)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.cancel_question))
            .setMessage(message)
            .setPositiveButton(R.string.yes) { _, _ ->
                setResult(RESULT_CANCELED)
                finish()
            }
            .setNegativeButton(R.string.no) { _, _ -> }
            .create()
            .show()
    }

    fun onSubmit(view: View?) {
        val name = findViewById<EditText>(R.id.setItemName).text.toString()
        val quantity = findViewById<EditText>(R.id.setItemQuantity).text.toString()

        // Don't allow submission without a name.
        if (name.isEmpty()) {
            Toast
                .makeText(applicationContext, "Name must not be empty.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Create an intent, and store the resulting item on it.
        val item = ShoppingListItem(
            originalItem?.bought ?: false,
            name,
            quantity,
            ZonedDateTime.now()
        )
        val intent = Intent()
        ShoppingListItem.serialiseOntoIntent(item, position, intent)

        // Complete the activity, passing the data back to the parent.
        setResult(RESULT_OK, intent)
        finish()
    }
}