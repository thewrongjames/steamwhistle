package au.edu.sydney.comp5216.assignment1

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import java.util.ArrayList

class ListItemAdapter(
    private val context_: Context,
    private val values: ArrayList<ListItem>
) : ArrayAdapter<ListItem>(context_, R.layout.item_row, values) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(context)

        // TODO: Use the view holder pattern here.
        val rowView = inflater.inflate(R.layout.item_row, parent, false)

        val textView = rowView.findViewById<TextView>(R.id.item_text)
        val buttonView = rowView.findViewById<Button>(R.id.dummy_button)

        textView.text = values[position].text
        buttonView.text = values[position].buttonText

        return rowView
    }
}