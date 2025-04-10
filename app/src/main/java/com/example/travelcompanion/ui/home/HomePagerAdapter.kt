package com.example.travelcompanion.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.example.travelcompanion.R

class HomePagerAdapter : PagerAdapter() {
    override fun getCount(): Int {
        return 2
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getPageTitle(position: Int): CharSequence? {
        //TODO: change this to string value
        return if (position==0)
            "Start"
        else
            "Plan"
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layoutInflater = LayoutInflater.from(container.context)
        val layout = when (position) {
            0 -> R.layout.home_page_start
            1 -> R.layout.home_page_plan
            else -> throw IllegalArgumentException("Invalid position")
        }
        val view = layoutInflater.inflate(layout, container, false)
        container.addView(view)
        return view
    }
}