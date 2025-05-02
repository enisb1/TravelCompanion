package com.example.travelcompanion.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.travelcompanion.R
import com.example.travelcompanion.ui.home.plan.PlanFragment
import com.example.travelcompanion.ui.home.start.StartFragment

class HomePagerAdapter(
    fragment: Fragment,
    private val plannedTripId: Long,
    private val tripType: String,
    private val tripDestination: String
) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> StartFragment().apply {
                arguments = Bundle().apply {
                    putLong("plannedTripId", plannedTripId)
                    putString("tripType", tripType)
                    putString("tripDestination", tripDestination)
                }
            }
            1 -> PlanFragment()
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}