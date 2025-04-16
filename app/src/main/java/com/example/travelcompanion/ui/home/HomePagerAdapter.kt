package com.example.travelcompanion.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.travelcompanion.R
import com.example.travelcompanion.ui.home.plan.PlanFragment
import com.example.travelcompanion.ui.home.start.StartFragment

class HomePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> StartFragment()
            1 -> PlanFragment()
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}