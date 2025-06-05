package com.example.travelcompanion.ui.journal.map

import android.annotation.SuppressLint
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.example.travelcompanion.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment

class MapFragment : Fragment() {

    companion object {
        fun newInstance() = MapFragment()
    }

    private lateinit var map: GoogleMap

    private lateinit var viewPager: ViewPager2  // parent fragment viewPager

    private val viewModel: MapViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        drawMap()

        viewPager = requireParentFragment().requireView().findViewById(R.id.journal_viewPager)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun drawMap() {
        val mapFragment: SupportMapFragment =
            childFragmentManager.findFragmentById(R.id.journal_map_container) as SupportMapFragment
        mapFragment.getMapAsync {googleMap ->
            map = googleMap
            map.uiSettings.isZoomControlsEnabled = true
            // disable tab swiping in order to move freely on the map
            val mapTouchInterceptor = requireView().findViewById<View>(R.id.journal_map_touchInterceptor)
            mapTouchInterceptor.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        disableTabSwiping()
                        false
                    }
                    else -> false
                }
            }
        }
    }

    private fun disableTabSwiping() {
        viewPager.isUserInputEnabled = false
    }
}