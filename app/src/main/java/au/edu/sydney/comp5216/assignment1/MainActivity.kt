package au.edu.sydney.comp5216.assignment1

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import java.util.ArrayList
import android.util.Log
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private val items: ArrayList<ListItem> = ArrayList()

    private var listView: ListView? = null
    private var addItemEditText: EditText? = null
    private var itemsAdapter: ListItemAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialise the adapter for mapping our item strings into the UI.
        itemsAdapter = ListItemAdapter(this, items)

        // Find the views in the activity. Set the listView adapter.
        listView = findViewById<ListView>(R.id.lstView).apply { adapter = itemsAdapter }
        addItemEditText = findViewById(R.id.txtNewItem)

        items.add(ListItem("Hello", "There"))
        items.add(ListItem("Forty", "Two"))

        setupListViewListener()
    }

    /**
     * Add the item currently in [addItemEditText] to [items] list and update [itemsAdapter].
     */
    fun onAddItemClick(view: View) {
        val stringToAdd = (addItemEditText?.text ?: return).toString()
        if (stringToAdd.isEmpty()) return

        itemsAdapter?.add(ListItem(stringToAdd, "Button"))
        addItemEditText?.setText("")
    }

    private fun setupListViewListener() {
        // Add a listener to delete (with a confirm dialogue) on a long-click.
        listView?.onItemLongClickListener =
            AdapterView.OnItemLongClickListener { _, _, position, _ ->
                Log.i(TAG, "Long Clicked item $position")

                AlertDialog.Builder(this@MainActivity)
                    .setTitle(getString(R.string.delete_item_alert_title))
                    .setMessage(getString(R.string.delete_item_alert_message, items[position].text))
                    .setPositiveButton(R.string.delete) { _, _ ->
                        items?.removeAt(position)
                        itemsAdapter?.notifyDataSetChanged()
                    }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .create()
                    .show()

                true
            }

        // Add a listener to edit on a single click.
        listView?.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                val itemToUpdate = itemsAdapter?.getItem(position)
                if (itemToUpdate == null) {
                    Log.e(TAG, "Registered click on non-existant item")
                    return@OnItemClickListener
                }

                Log.i(TAG, "Clicked item $position: $itemToUpdate")

                val intent = Intent(this@MainActivity, EditToDoItemActivity::class.java)

                intent.putExtra("item_text", itemToUpdate.text)
                intent.putExtra("position", position)

                openItemEditor.launch(intent)
            }
    }

    private val openItemEditor = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == RESULT_OK) {
            val editedText = result.data?.extras?.getString("item")
            val position = result.data?.extras?.getInt("position", -1)

            if (editedText == null || position == null || position < 0) {
                Log.e(TAG, "Got invalid data back from EditToDoItemActivity")
                return@registerForActivityResult
            }

            items?.set(position, ListItem(editedText, "Button"))
            itemsAdapter?.notifyDataSetChanged()

            Log.i(TAG, "Updated item $position: $editedText")
            Toast
                .makeText(applicationContext, "Updated: $editedText", Toast.LENGTH_SHORT)
                .show()
        }
    }
}