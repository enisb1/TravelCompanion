package com.example.travelcompanion.ui.journal.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.db.trip.Trip
import android.view.LayoutInflater
import com.example.travelcompanion.R
import com.example.travelcompanion.ui.journal.list.JournalListViewHolder


class JournalListRecyclerViewAdapter(private val clickFun: (Trip) -> Unit) : RecyclerView.Adapter<JournalListViewHolder>() {

    private val tripList = ArrayList<Trip>()
    private var onItemClick: (Trip) -> Unit = clickFun

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalListViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        // TODO: Decide whether to reuse plan_list_item or journal_list_item
        val listItem = layoutInflater.inflate(R.layout.plan_list_item, parent, false)
        return JournalListViewHolder(listItem)
    }

    override fun getItemCount(): Int {
        return tripList.size
    }

    override fun onBindViewHolder(holder: JournalListViewHolder, position: Int) {
        val trip = tripList[position]
        holder.bind(trip)
        holder.itemView.setOnClickListener {
            onItemClick(trip)
        }
    }

    fun setList(trips: List<Trip>) {
        tripList.clear()
        tripList.addAll(trips)
        notifyDataSetChanged()
    }
}