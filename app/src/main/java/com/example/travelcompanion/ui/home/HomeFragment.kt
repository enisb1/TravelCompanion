package com.example.travelcompanion.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.travelcompanion.R
import com.google.android.material.tabs.TabLayout

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
        val viewPager: ViewPager = view.findViewById(R.id.home_viewPager)

        val tabPagerAdapter : PagerAdapter = HomePagerAdapter()
        viewPager.adapter = tabPagerAdapter
        tabLayout.setupWithViewPager(viewPager)
    }

}