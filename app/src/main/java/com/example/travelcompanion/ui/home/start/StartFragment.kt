package com.example.travelcompanion.ui.home.start

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.Image
import android.os.Build
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
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
    private lateinit var requestPermissionLauncherForNotification: ActivityResultLauncher<String>

    private lateinit var startButton: Button
    private lateinit var trackingLayout: ConstraintLayout
    private lateinit var map: GoogleMap
    private lateinit var trackingTitle: TextView
    private lateinit var stopButton: ImageButton
    private lateinit var newNoteImage: ImageView
    private lateinit var newPicImage: ImageView


    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_start, container, false)
    }

    override fun onResume() {
        super.onResume()
        Log.i("Tracking", "resumed")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // location permission launcher
        requestPermissionLauncherForLocation = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                Toast.makeText(activity, "Location permission given", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, R.string.location_permission_denied, Toast.LENGTH_SHORT).show()
            }
        }

        // location permission launcher
        requestPermissionLauncherForLocation = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                    setUpMap()
                else
                    Toast.makeText(activity, "Location permission given", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, R.string.location_permission_denied, Toast.LENGTH_SHORT).show()
            }
        }

        requestPermissionLauncherForNotification = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                setUpMap()
            } else {
                Toast.makeText(activity, "Notification permission is required", Toast.LENGTH_SHORT).show()
            }
        }

        instantiateViews(view)
        setListeners()
    }

    private fun enableTabSwiping() {
        viewPager.isUserInputEnabled = true
    }

    private fun disableTabSwiping() {
        viewPager.isUserInputEnabled = false
    }

    private fun instantiateViews(view: View) {
        viewPager = requireParentFragment().requireView().findViewById(R.id.home_viewPager)
        trackingLayout = view.findViewById(R.id.trackingLayout)
        trackingTitle = view.findViewById(R.id.trackingTitle)
        startButton = view.findViewById(R.id.startButton)
        stopButton = view.findViewById(R.id.stopButton)
        newNoteImage = view.findViewById(R.id.newNote)
        newPicImage = view.findViewById(R.id.newPic)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setListeners() {
        // enable tab swiping for the views in the layout
        for (view in arrayOf(trackingLayout, trackingTitle, stopButton, newNoteImage, newPicImage)) {
            view.setOnTouchListener { _, _ ->
                enableTabSwiping()
                false
            }
        }
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
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncherForNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else {
                setUpMap()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpMap() {
        val mapFragment: SupportMapFragment =
            childFragmentManager.findFragmentById(R.id.mapContainer) as SupportMapFragment
        mapFragment.getMapAsync {googleMap ->
            map = googleMap

            // disable tab swiping in order to move freely on the map
            val mapTouchInterceptor = requireView().findViewById<View>(R.id.mapTouchInterceptor)
            mapTouchInterceptor.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        disableTabSwiping()
                        false
                    }
                    else -> false
                }
            }
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
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(addedLatLng, 17f)
                )
            }
            // start tracking
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