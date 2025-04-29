package com.example.travelcompanion.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.travelcompanion.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val homeVM : HomeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        // TODO: use VM
        // TabLayout setup
        val tabLayout : TabLayout = view.findViewById(R.id.home_tabLayout)
        val viewPager: ViewPager2 = view.findViewById(R.id.home_viewPager)

        viewPager.adapter = HomePagerAdapter(this)

        val defaultTab = arguments?.getInt("tab") ?: 0
        viewPager.setCurrentItem(defaultTab, false)

        // Attach TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Start"
                1 -> "Plan"
                else -> null
            }
        }.attach()
    }

}