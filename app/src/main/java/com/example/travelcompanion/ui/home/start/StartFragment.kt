package com.example.travelcompanion.ui.home.start

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.FragmentContainerView
import com.example.travelcompanion.R
import com.google.android.gms.maps.SupportMapFragment

class StartFragment : Fragment() {

    private val viewModel: StartViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_start, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val startButton: Button = view.findViewById(R.id.startButton)
        val fgContainer: FragmentContainerView = view.findViewById(R.id.mapContainer)
        startButton.setOnClickListener {
            startButton.visibility = View.GONE
            fgContainer.visibility = View.VISIBLE

            val mapFragment: SupportMapFragment =
                childFragmentManager.findFragmentById(R.id.mapContainer) as SupportMapFragment
            mapFragment.getMapAsync {
                
            }

        }
    }
}