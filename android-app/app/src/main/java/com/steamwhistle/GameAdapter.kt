package com.steamwhistle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.ListAdapter
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

/**
 * This is an extension of ListAdapter which is (well, this one is) an extension of
 * RecyclerView.Adapter, for handling list data, in this case, [Game]s. This apparently does smart
 * stuff such as computing diffs between lists in a background thread.
 */
class GameAdapter : ListAdapter<Game, GameAdapter.ViewHolder>(GameComparator()) {
    /**
     * This should be set in order to handle clicks on games. It is called whenever any game is
     * clicked, and it receives the position of the game that was clicked.
     */
    var onItemClickListener: (position: Int) -> Unit = {_ ->}
    var onItemClickListenerForDetail: (game: WatchlistGame) -> Unit = {_ ->}
    var onLongPress: (game:WatchlistGame) -> Unit = {_ ->}
    var updateThreshold: (game:WatchlistGame) -> Unit = {_ ->}

    /**
     * This is a helper class for storing references to the sub-views of the game item view. This is
     * so that the expensive operation of finding them only needs to be done once for as many of
     * them fit on the screen.
     */
    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.watchlistItemImage)
        val titleView: TextView = view.findViewById(R.id.watchlistItemTitle)
        val priceView: TextView = view.findViewById(R.id.watchlistItemPrice)
        val thresholdView: TextView = view.findViewById(R.id.watchlistItemThreshold)
    }

    /**
     * This is for comparing two games. The ListAdapter needs this to do its magic.
     */
    class GameComparator : DiffUtil.ItemCallback<Game>() {
        /**
         * Check if two games represent the same object, though they may have been internally
         * updated. Two games represent the same actual game if they have the same steam app ID.
         */
        override fun areItemsTheSame(oldItem: Game, newItem: Game): Boolean {
            return oldItem.appId == newItem.appId
        }

        /**
         * Check if two games are actual identical copies of each other, that is, if they represent
         * the same game and also agree on all of its properties. All game classes should implement
         * .equals to support this (for instance by being data classes).
         */
        override fun areContentsTheSame(oldItem: Game, newItem: Game): Boolean {
            return oldItem == newItem
        }
    }

    /**
     * This is called whenever a new view is created. This should only happen once for as many views
     * as can fit on the screen at one time. After that many are all created they should be
     * "recycled".
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.game_list_item, parent, false)

        return ViewHolder(view)
    }

    /**
     * This is called whenever an old view that has been scrolled off the screen needs to be
     * replaced with the contents of a new item, so that it can be "recycled".
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val game = getItem(position)

        // If the game is a WatchlistGame, we can display a threshold, otherwise we just display
        // nothing there.
        val thresholdText = when(game) {
            is WatchlistGame -> String.format("%.2f", game.threshold/100.0)
            else -> ""
        }

        // TODO: Get the image from data.
        holder.titleView.text = game.name
        holder.priceView.text = holder.view.context.getString(
            R.string.dollars_template,
            String.format("%.2f", game.price/100.0)
        )
        holder.view.setOnClickListener { onItemClickListener(position) }
        holder.imageView.setOnClickListener { onItemClickListenerForDetail(game as WatchlistGame) }
        holder.thresholdView.setOnClickListener { updateThreshold(game as WatchlistGame) }
        holder.view.setOnLongClickListener {
            onLongPress(game as WatchlistGame)
            true
        }
        holder.thresholdView.text = thresholdText

    }
}