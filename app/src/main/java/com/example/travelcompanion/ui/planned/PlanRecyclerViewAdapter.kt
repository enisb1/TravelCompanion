package com.example.travelcompanion.ui.planned

import PlanViewHolder
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.R
import com.example.travelcompanion.db.Plan


class PlanRecyclerViewAdapter() : RecyclerView.Adapter<PlanViewHolder>() {

    private val planList = ArrayList<Plan>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val listItem = layoutInflater.inflate(R.layout.plan_list_item, parent, false)
        return PlanViewHolder(listItem)
    }

    override fun getItemCount(): Int {
        return planList.size
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        holder.bind(planList[position])
    }

    fun setList(students: List<Plan>) {
        planList.clear()
        planList.addAll(students)
        notifyDataSetChanged()
    }
}