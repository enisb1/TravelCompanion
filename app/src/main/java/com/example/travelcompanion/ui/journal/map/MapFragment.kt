package com.example.travelcompanion.ui.journal.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.travelcompanion.R
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.db.locations.TripLocation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap

class MapFragment : Fragment() {

    private companion object {
        val COLOR_PINK = Color.rgb(237, 166, 177)
        val COLOR_GREY_BLUE = Color.rgb(112, 127, 166)
        val COLOR_LIGHT_GREEN = Color.rgb(113, 171, 150)
        val COLOR_GREEN = Color.rgb(29, 130, 78)
        val COLOR_RED = Color.rgb(232, 90, 86)
        val COLOR_CYAN = Color.rgb(82, 194, 209)
        val COLOR_MAGENTA = Color.rgb(233, 102, 137)
        val COLOR_BROWN = Color.rgb(166, 136, 110)
        val pathColors: List<Int> = listOf(COLOR_PINK, COLOR_GREY_BLUE, COLOR_LIGHT_GREEN, COLOR_GREEN,
            COLOR_RED, COLOR_CYAN, COLOR_MAGENTA, COLOR_BROWN)
    }

    private lateinit var viewModel: MapViewModel

    private lateinit var map: GoogleMap

    private lateinit var titleTxtView: TextView
    private lateinit var parentConstraintLayout: ConstraintLayout
    private lateinit var mapFrameLayout: FrameLayout
    private lateinit var noTripsLayout: ConstraintLayout

    private lateinit var viewPager: ViewPager2  // parent fragment viewPager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // instantiate ViewModel
        val factory = MapViewModelFactory(repository = TravelCompanionRepository(app = requireActivity().application))
        viewModel = ViewModelProvider(this, factory)[MapViewModel::class.java]

        instantiateViews(view)
        setListeners()

        lifecycleScope.launch {
            val locations = withContext(Dispatchers.IO) {
                viewModel.getLocations()
            }
            val locationsPerTrip: List<List<TripLocation>> = locations
                .groupBy { it.tripId }
                .values
                .map { it.sortedBy { location -> location.timestamp } }
            if (locationsPerTrip.isNotEmpty()) {
                drawMap(locationsPerTrip)
            }
            else {
                titleTxtView.visibility = View.GONE
                mapFrameLayout.visibility = View.GONE
                noTripsLayout.visibility = View.VISIBLE
            }
        }
    }

    private fun instantiateViews(view: View) {
        titleTxtView = view.findViewById(R.id.journal_map_title)
        parentConstraintLayout = view.findViewById(R.id.journal_map_constraint_layout)
        mapFrameLayout = view.findViewById(R.id.journal_map_frame)
        noTripsLayout = view.findViewById(R.id.no_trips_layout_map)
        viewPager = requireParentFragment().requireView().findViewById(R.id.journal_viewPager)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setListeners() {
        // enable tab swiping when clicking outside the map
        for (view in arrayOf(titleTxtView, parentConstraintLayout)) {
            view.setOnTouchListener { _, _ ->
                enableTabSwiping()
                false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun drawMap(locationsPerTrip: List<List<TripLocation>>) {
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
            lifecycleScope.launch {
                // show trips on map
                for (locationList in locationsPerTrip) {
                    val tripDestination = withContext(Dispatchers.IO) {
                        viewModel.getTripById(locationList[0].tripId)?.destination
                    }
                    // start
                    val pathColor = pathColors.random()
                    val startIcon = vectorToBitmapWithColor(requireContext(), R.drawable.map_marker, pathColor)
                    val startMarker = map.addMarker(
                        MarkerOptions()
                            .position(LatLng(locationList[0].latitude, locationList[0].longitude))
                            .title("Trip start: $tripDestination")
                            .icon(startIcon)
                    )
                    startMarker?.showInfoWindow() // it's going to show the last start marker (zoomed)
                    // polyline
                    val polylineOptions = PolylineOptions()
                        .addAll(locationList.map { LatLng(it.latitude, it.longitude) })
                        .color(pathColor) // or randomize per trip
                        .width(20f)
                    // end
                    val finishIcon = vectorToBitmap(requireContext(), R.drawable.ic_flag)
                    map.addMarker(
                        MarkerOptions()
                            .position(LatLng(
                                locationList[locationList.size - 1].latitude,
                                locationList[locationList.size - 1].longitude
                            ))
                            .title("Trip end: $tripDestination")
                            .icon(finishIcon)
                            .anchor(0.2f, 1.0f)
                    )

                    map.addPolyline(polylineOptions)
                }
                // zoom to last trip location
                if (locationsPerTrip.isNotEmpty()) {
                    val lastTripLocations: List<TripLocation> = locationsPerTrip[locationsPerTrip.size-1]
                    val startOfLastTrip = LatLng(
                        lastTripLocations[0].latitude,
                        lastTripLocations[0].longitude
                    )
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(startOfLastTrip, 17f)
                    )
                }
            }

        }
    }

    private fun disableTabSwiping() {
        viewPager.isUserInputEnabled = false
    }

    private fun enableTabSwiping() {
        viewPager.isUserInputEnabled = true
    }

    private fun vectorToBitmap(context: Context, vectorResId: Int): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)!!
        val bitmap = createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun vectorToBitmapWithColor(context: Context, vectorResId: Int, color: Int): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(context, vectorResId)!!.mutate()
        drawable.setTint(color)

        val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}