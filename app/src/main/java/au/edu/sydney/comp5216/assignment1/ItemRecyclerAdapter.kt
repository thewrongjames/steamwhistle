package au.edu.sydney.comp5216.assignment1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.ZonedDateTime
import java.time.Duration
import java.util.ArrayList

class ItemRecyclerAdapter(
    private val items: ArrayList<ShoppingListItem>,
    private val itemClickListener: (position: Int) -> Unit,
    private val itemLongClickListener: (position: Int) -> Boolean,
) : RecyclerView.Adapter<ItemRecyclerAdapter.ViewHolder>() {
    // Lots of comments here to explain to myself how this works.

    /**
     * Stores references to the sub-views of the item row view, so that the expensive
     * `findViewById` call only happens once per row-that-fits-on-the-screen.
     */
    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val checkboxView: CheckBox = view.findViewById(R.id.itemCheckbox)
        val textView: TextView = view.findViewById(R.id.itemText)
        val timeView: TextView = view.findViewById(R.id.itemTime)
    }

    /**
     * Create a new view, and return a view holder that contains it. This should only happen as many
     * times as row views fit on the screen at once.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_row, parent, false)
        return ViewHolder(view)
    }

    /**
     * Update an existing view to correspond to a new position. After the screen has been filled
     * with row views, this is all that should happen as the user scrolls, as old views get recycled
     * into new ones.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Populate the values in the views.

        holder.checkboxView.isChecked = items[position].bought

        if (items[position].quantity.isEmpty()) {
            holder.textView.text = items[position].name
        } else {
            holder.textView.text = holder.view.context.getString(
                R.string.item_list_text_template,
                items[position].name,
                items[position].quantity
            )
        }

        val timeLeft = Duration.between(ZonedDateTime.now(), items[position].due)
        if (timeLeft.isNegative) {
            holder.timeView.text = holder.view.context.getString(R.string.overdue)
        } else {
            holder.timeView.text = holder.view.context.getString(
                R.string.item_list_time_template,
                timeLeft.toDays(),
                timeLeft.toHours(),
            )
        }

        // Set the listeners.

        holder.checkboxView.setOnCheckedChangeListener { _, isChecked ->
            items[position].bought = isChecked
        }
        holder.view.setOnClickListener { itemClickListener(position) }
        holder.view.setOnLongClickListener { itemLongClickListener(position) }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}