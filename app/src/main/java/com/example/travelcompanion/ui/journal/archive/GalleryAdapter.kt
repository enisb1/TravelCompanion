package com.example.travelcompanion.ui.journal.archive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.R
import com.example.travelcompanion.db.pictures.Picture

class GalleryAdapter(private val imageUris: List<Picture>):
    RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {

    inner class GalleryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.gallery_item_picture)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recview_item_gallery, parent, false)
        return GalleryViewHolder(view)
    }

    override fun getItemCount(): Int {
        return imageUris.size
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        val uri = imageUris[position].uri
        holder.imageView.setImageURI(uri.toUri())

        // set itemView to square size
        holder.itemView.post {  // wait until the view is laid out
            val width = holder.itemView.measuredWidth
            holder.imageView.layoutParams.height = width
            holder.imageView.requestLayout()
        }
    }

}