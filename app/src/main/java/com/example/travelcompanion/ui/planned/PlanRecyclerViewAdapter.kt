package com.example.travelcompanion.ui.planned

import PlanViewHolder
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.R
import com.example.travelcompanion.db.trip.Trip


class PlanRecyclerViewAdapter(val clickFun : (Trip) -> Unit) : RecyclerView.Adapter<PlanViewHolder>() {

    private val tripList = ArrayList<Trip>()
    private var onItemClick: (Trip) -> Unit

    init {
        onItemClick = clickFun
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val listItem = layoutInflater.inflate(R.layout.plan_list_item, parent, false)
        return PlanViewHolder(listItem)
    }

    override fun getItemCount(): Int {
        return tripList.size
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        val plan = tripList[position]
        holder.bind(plan)
        holder.itemView.setOnClickListener {
            onItemClick(plan)
        }
    }



    fun setList(trips: List<Trip>) {
        tripList.clear()
        tripList.addAll(trips)
        notifyDataSetChanged()
    }
}