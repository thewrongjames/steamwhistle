package com.steamwhistle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * A class for adapting a list of games into a list of [items] (Games) that can be displayed. It is
 * a RecyclerView, which means that it avoid the relatively expensive operations of recreating and
 * requering the individual views of each item every time a new one is needed for display on the
 * page by instead just using the one that was just scrolled off the page.
 */
class WatchlistItemRecyclerAdapter (
    private val items: ArrayList<Game>,
): RecyclerView.Adapter<WatchlistItemRecyclerAdapter.ViewHolder>() {
    var onItemClickListener: (position: Int) -> Unit = {_ ->}

    /**
     * This is a helper class for storing references to
     */
    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.watchlistItemImage)
        val titleView: TextView = view.findViewById(R.id.watchlistItemTitle)
        val priceView: TextView = view.findViewById(R.id.watchlistItemPrice)
        val thresholdView: TextView = view.findViewById(R.id.watchlistItemThreshold)
    }

    /**
     * This is called whenever a new view is created. This should only happen once for as many views
     * as can fit on the screen at one time. After that many are all created they should be
     * "recycled".
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.watchlist_item, parent, false)

        return ViewHolder(view)
    }

    /**
     * This is called whenever an old view that has been scrolled off the screen needs to be
     * replaced with the contents of a new item, so that it can be "recycled".
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // TODO: Actually base these on data.
        val price = 4242
        val threshold = 2000

        // TODO: Update the image from data.

        holder.titleView.text = items[position].name

        holder.priceView.text = holder.view.context.getString(
            R.string.dollars_template,
            String.format("%.2f", price/100.0)
        )

        holder.thresholdView.text = holder.view.context.getString(
            R.string.item_threshold_template,
            String.format("%.2f", threshold/100.0)
        )

        holder.view.setOnClickListener { onItemClickListener(position) }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}