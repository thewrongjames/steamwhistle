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
                ShoppingListItem.serialiseOntoIntent(item, position, intent)

                openAddOrEdit.launch(intent)
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

        // Launch the AddOrEditItem activity with no extras to indicate we are creating a new item.git
        val intent = Intent(this@MainActivity, AddOrEditItem::class.java)
        openAddOrEdit.launch(intent)
    }

    private val openAddOrEdit = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) handleActivityResult@{ result: ActivityResult? ->
        val deserialised = ShoppingListItem.deserialiseFromActivityResult(result)
        if (deserialised == null) {
            Log.w(TAG, "Failed to deserialise result from item add or edit")
            return@handleActivityResult
        }
        val (item, position) = deserialised

        val action: String
        if (position in 0 until items.size) {
            // Updating an item. Technically we have only said that a negative position means
            // adding, and not that an out of range position means adding. However, we don't have
            // anything else to do with the out of range items, so we may as well add them.
            items[position] = item
            itemRecyclerAdapter?.notifyItemChanged(position)
            action = "Updated"
        } else {
            items.add(item)
            itemRecyclerAdapter?.notifyItemChanged(items.size - 1)
            action = "Added"
        }

        Log.i(TAG, "$action item at $position")
        Toast
            .makeText(applicationContext, "$action: ${item.name}", Toast.LENGTH_SHORT)
            .show()

        if (position < 0) {
            // Adding an item.
            items.add(item)
            itemRecyclerAdapter?.notifyItemInserted(items.size- 1)
        } else {
            // Updating an item.
            items[position]
        }
    }
}