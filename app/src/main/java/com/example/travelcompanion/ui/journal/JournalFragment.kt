package com.example.travelcompanion.ui.journal

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.travelcompanion.R
import com.example.travelcompanion.ui.home.HomePagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class JournalFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_journal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val journalVM : JournalViewModel = ViewModelProvider(this).get(JournalViewModel::class.java)

        val tabLayout : TabLayout = view.findViewById(R.id.journal_tabLayout)
        val viewPager: ViewPager2 = view.findViewById(R.id.journal_viewPager)

        viewPager.adapter = JournalPagerAdapter(this)

        // TODO: change default tab to list tab?
        //viewPager.setCurrentItem(0, false)

        // Attach TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.map)
                1 -> getString(R.string.list)
                2 -> getString(R.string.archive)
                else -> null
            }
        }.attach()

        // TODO: use VM
    }
}