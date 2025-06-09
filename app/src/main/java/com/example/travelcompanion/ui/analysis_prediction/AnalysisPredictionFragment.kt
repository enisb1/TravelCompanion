package com.example.travelcompanion.ui.analysis_prediction

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.travelcompanion.R
import com.example.travelcompanion.ui.home.HomePagerAdapter
import com.example.travelcompanion.ui.home.start.StartFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class AnalysisPredictionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_analysis_prediction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressPredictionVM : AnalysisPredictionViewModel =
            ViewModelProvider(this)[AnalysisPredictionViewModel::class.java]


        val tabLayout : TabLayout = view.findViewById(R.id.analysis_prediction_tabLayout)
        val viewPager: ViewPager2 = view.findViewById(R.id.analysis_prediction_viewPager)

        viewPager.adapter = AnalysisPredictionPagerAdapter(this)

        viewPager.setCurrentItem(0, false)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.analysis)
                1 -> getString(R.string.prediction)
                else -> null
            }
        }.attach()
    }
}