package com.example.travelcompanion.ui.journal

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.travelcompanion.ui.journal.archive.ArchiveFragment
import com.example.travelcompanion.ui.journal.list.ListFragment
import com.example.travelcompanion.ui.journal.map.MapFragment

class JournalPagerAdapter(fragment: Fragment): FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MapFragment()
            1 -> ListFragment()
            2 -> ArchiveFragment()
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}