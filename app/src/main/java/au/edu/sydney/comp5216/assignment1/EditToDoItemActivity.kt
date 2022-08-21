package au.edu.sydney.comp5216.assignment1

import android.widget.EditText
import android.os.Bundle
import android.content.Intent
import android.app.Activity
import android.view.View

class EditToDoItemActivity : Activity() {
    var position: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_edit_item)

        val itemText = intent.getStringExtra("item_text")
        position = intent.getIntExtra("position", -1)
        if (position == -1) position = null

        findViewById<EditText>(R.id.etEditItem).setText(itemText)
    }

    fun onSubmit(view: View?) {
        val text = findViewById<EditText>(R.id.etEditItem).text.toString()
        // Don't allow editing the item to be empty.
        if (text.isEmpty()) return

        // Create a data intent for returning the updated item.

        val dataIntent = Intent()
        dataIntent.putExtra("item", text)
        dataIntent.putExtra("position", position)

        // Complete the activity, passing the data back to the parent.
        setResult(RESULT_OK, dataIntent)
        finish()
    }
}