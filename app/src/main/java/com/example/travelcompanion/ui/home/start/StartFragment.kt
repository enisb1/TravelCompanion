package com.example.travelcompanion.ui.home.start

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.travelcompanion.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

class StartFragment : Fragment() {

    private val viewModel: StartViewModel by viewModels()

    private lateinit var requestPermissionLauncherForLocation: ActivityResultLauncher<String>

    private lateinit var map: GoogleMap
    private lateinit var startButton: Button
    private lateinit var trackingLayout: ConstraintLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_start, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // location permission launcher
        requestPermissionLauncherForLocation = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                setUpMap()
            } else {
                Toast.makeText(activity, R.string.location_permission_denied, Toast.LENGTH_SHORT).show()
            }
        }

        trackingLayout = view.findViewById(R.id.trackingLayout)

        startButton = view.findViewById(R.id.startButton)
        startButton.setOnClickListener {
            // check for permission before setting up the map
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // check if we need to give rationale to the user or not
                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                    AlertDialog.Builder(requireContext())
                        .setMessage(R.string.location_permission_needed)
                        .setPositiveButton(R.string.positive_button_string) { _, _ ->
                            requestPermissionLauncherForLocation.launch(
                                Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                        .setNegativeButton(R.string.negative_button_string, null)
                        .show()
                }else {
                    requestPermissionLauncherForLocation.launch(
                        Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            else {
                setUpMap()
            }
        }
    }

    private fun setUpMap() {
        val mapFragment: SupportMapFragment =
            childFragmentManager.findFragmentById(R.id.mapContainer) as SupportMapFragment
        mapFragment.getMapAsync {googleMap ->
            map = googleMap
            // show start position on map
            // show/hide map and start button
            startButton.visibility = View.GONE
            trackingLayout.visibility = View.VISIBLE
            // draw polyline
            val polylineOptions = PolylineOptions().apply {
                color(Color.GREEN)
                width(20f)
            }
            val points = mutableListOf<LatLng>()
            val polyline = map.addPolyline(polylineOptions)
            // observe changes in ViewModel
            viewModel.locationsList.observe(requireActivity()) { newValue ->
                val addedLatLng = LatLng(newValue[newValue.size-1].latitude,
                    newValue[newValue.size-1].longitude)
                if (newValue.size == 1)
                    addStartAndZoom(addedLatLng)    // added locations is start location
                points.add(addedLatLng)
                polyline.points = points
            }
            // start tracking locations
            requireContext().startService(Intent(requireContext(), TrackingService::class.java))
        }
    }

    private fun addStartAndZoom(startLatLng: LatLng) {
        map.addMarker(
            MarkerOptions()
                .position(startLatLng)
                .title("Start")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(startLatLng, 17f)
        )
    }
}