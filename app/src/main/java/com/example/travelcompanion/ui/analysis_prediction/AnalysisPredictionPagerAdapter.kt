package com.example.travelcompanion.ui.analysis_prediction

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.travelcompanion.ui.analysis_prediction.analysis.AnalysisFragment
import com.example.travelcompanion.ui.analysis_prediction.prediction.PredictionFragment

class AnalysisPredictionPagerAdapter(fragment: Fragment): FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AnalysisFragment()
            1 -> PredictionFragment()
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}