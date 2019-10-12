package com.fenchtose.movieratings.features.moviecollection.collectionlist

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.fenchtose.movieratings.R
import com.fenchtose.movieratings.model.entity.MovieCollection
import com.fenchtose.movieratings.model.image.ImageLoader

class CollectionListPageAdapter(context: Context,
                                private val imageLoader: ImageLoader,
                                private val callback: AdapterCallback,
                                private val showDeleteOption: Boolean): RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    var data: ArrayList<MovieCollection> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return CollectionItemViewHolder(inflater.inflate(R.layout.movie_collection_item_layout, parent, false), callback, showDeleteOption)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as CollectionItemViewHolder).bind(data[position], imageLoader)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemId(position: Int): Long {
        return data[position].id.hashCode().toLong()
    }

    fun updateData(collections: List<MovieCollection>) {
        data.clear()
        data.addAll(collections)
    }

    interface AdapterCallback {
        fun onClicked(collection: MovieCollection)
        fun onDeleteRequested(collection: MovieCollection)
    }
}