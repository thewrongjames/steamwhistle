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
import androidx.recyclerview.widget.RecyclerView
import java.lang.IndexOutOfBoundsException
import java.time.ZonedDateTime

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private val items: ArrayList<ShoppingListItem> = ArrayList()

    private var recyclerView: RecyclerView? = null
    private var itemRecyclerAdapter: ItemRecyclerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialise the adapter for mapping our item strings into the UI.
        itemRecyclerAdapter = ItemRecyclerAdapter(
            items,
            clickListener@{ position ->
                Log.i(TAG, "Clicked item at position $position")

                val item: ShoppingListItem
                try {
                    item = items[position]
                } catch (error: IndexOutOfBoundsException) {
                    Log.e(TAG, "Registered click on non-existent item")
                    return@clickListener
                }

                val intent = Intent(this@MainActivity, AddOrEditItem::class.java)

                intent.putExtra("position", position)
                intent.putExtra("bought", item.bought)
                intent.putExtra("name", item.name)
                intent.putExtra("quantity", item.quantity)
                intent.putExtra("due", item.due)

                openItemEditor.launch(intent)
            },
            longClickListener@{ position ->
                Log.i(TAG, "Long clicked item at position $position")

                try {
                    items[position]
                } catch (error: IndexOutOfBoundsException) {
                    Log.e(TAG, "Registered long clock on non-existent item")
                    return@longClickListener false
                }

                AlertDialog.Builder(this@MainActivity)
                    .setTitle(getString(R.string.delete_item_alert_title))
                    .setMessage(getString(R.string.delete_item_alert_message, items[position].name))
                    .setPositiveButton(R.string.delete) { _, _ ->
                        items.removeAt(position)
                        itemRecyclerAdapter?.notifyItemChanged(position)
                    }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .create()
                    .show()

                // Indicate that we have handled the long click.
                true
            },
        )

        // Find the views in the activity. Set the listView adapter.
        recyclerView = findViewById<RecyclerView>(R.id.listRecycler).apply {
            adapter = itemRecyclerAdapter
        }

        items.add(
            ShoppingListItem(
                false,
                "Tinned tomatoes",
                "200 grams",
                ZonedDateTime.parse("2022-08-31T00:00+00:00"),
            )
        )
    }

    /**
     * Open the [AddOrEditItem] activity to add an item.
     */
    fun onAddItemClick(view: View) {
        Log.i(TAG, "Add item clicked.")
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

            items[position] = ShoppingListItem(false, editedText, "200 grams", ZonedDateTime.now())
            itemRecyclerAdapter?.notifyItemChanged(position)

            Log.i(TAG, "Updated item $position: $editedText")
            Toast
                .makeText(applicationContext, "Updated: $editedText", Toast.LENGTH_SHORT)
                .show()
        }
    }
}