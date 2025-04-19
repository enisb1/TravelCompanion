package com.example.travelcompanion.ui.home.start

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentContainerView
import com.example.travelcompanion.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class StartFragment : Fragment() {

    private val viewModel: StartViewModel by viewModels()
    private lateinit var requestPermissionLauncherForLocation: ActivityResultLauncher<Array<String>>
    private lateinit var map: GoogleMap
    private lateinit var startButton: Button
    private lateinit var fgContainerView: FragmentContainerView

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
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (fineGranted && coarseGranted) {
                addStartAndZoom()
            } else {
                Toast.makeText(activity, "Location permission is required", Toast.LENGTH_SHORT).show()
            }
        }

        startButton = view.findViewById(R.id.startButton)
        fgContainerView = view.findViewById(R.id.mapContainer)
        startButton.setOnClickListener {
            val mapFragment: SupportMapFragment =
                childFragmentManager.findFragmentById(R.id.mapContainer) as SupportMapFragment
            mapFragment.getMapAsync {googleMap ->
                map = googleMap
                setUpMap()
                // TODO: set interval to draw line based on current location (needs to be distant from
                // previous location)
            }
        }
    }

    private fun setUpMap() {
        // check for permission before setting up the map
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncherForLocation.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
        else {
            addStartAndZoom()
        }
    }

    @SuppressLint("MissingPermission")
    private fun addStartAndZoom() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)

                // Call this when the map is ready
                map.addMarker(
                    MarkerOptions()
                        .position(currentLatLng)
                        .title("Start")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )

                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f)
                )
            }
        }
        // show/hide map and start button
        startButton.visibility = View.GONE
        fgContainerView.visibility = View.VISIBLE
    }
}