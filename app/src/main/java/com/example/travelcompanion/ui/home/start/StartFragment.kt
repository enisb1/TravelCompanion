package com.example.travelcompanion.ui.home.start

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.travelcompanion.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import java.io.File
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.db.notes.Note
import com.example.travelcompanion.db.pictures.Picture
import com.example.travelcompanion.db.trip.TripState
import com.example.travelcompanion.db.trip.TripType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Locale

class StartFragment : Fragment() {

    private lateinit var viewModel: StartViewModel

    private lateinit var requestPermissionLauncherForLocation: ActivityResultLauncher<String>
    private lateinit var requestPermissionLauncherForNotification: ActivityResultLauncher<String>
    private lateinit var requestPermissionLauncherForCamera: ActivityResultLauncher<String>

    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>

    private lateinit var startButton: Button
    private lateinit var trackingLayout: ConstraintLayout
    private lateinit var map: GoogleMap
    private lateinit var trackingTitle: TextView
    private lateinit var stopButton: ImageButton
    private lateinit var newNoteImage: ImageView
    private lateinit var newPicImage: ImageView
    private lateinit var timerTextView: TextView
    private lateinit var distanceTextView: TextView

    private lateinit var stopDialog: AlertDialog
    private lateinit var newNoteDialog: AlertDialog

    private lateinit var viewPager: ViewPager2  // parent fragment viewPager

    private lateinit var currentPictureFile: File
    private lateinit var currentPictureUri: Uri

    private lateinit var inflater: LayoutInflater

    private val notes: MutableList<Note> = mutableListOf()
    private val pictures: MutableList<Picture> = mutableListOf()

    private lateinit var startDate: Date

    private var unpackedTripId: Long = -1L
    private lateinit var unpackedTripType: String
    private lateinit var unpackedTripDestination: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_start, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // instantiate ViewModel
        val factory = StartViewModelFactory(repository = TravelCompanionRepository(app = requireActivity().application))
        viewModel = ViewModelProvider(this, factory)[StartViewModel::class.java]

        inflater = LayoutInflater.from(requireContext())

        setLaunchers()
        instantiateViews(view)
        buildDialogs()
        setListeners()

        // unpack received strings (empty if no value was sent)
        unpackedTripId = arguments?.getLong("plannedTripId") ?: -1L
        unpackedTripType = arguments?.getString("tripType") ?: ""
        unpackedTripDestination = arguments?.getString("tripDestination") ?: ""
        if (unpackedTripId != -1L && unpackedTripType.isNotEmpty() && unpackedTripDestination.isNotEmpty())
            startButton.performClick()
    }

    private fun enableTabSwiping() {
        viewPager.isUserInputEnabled = true
    }

    private fun disableTabSwiping() {
        viewPager.isUserInputEnabled = false
    }

    private fun setLaunchers() {
        requestPermissionLauncherForLocation = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                    startTracking()
                else
                    requestPermissionLauncherForNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Toast.makeText(activity, R.string.fine_location_permission_denied, Toast.LENGTH_SHORT).show()
            }
        }

        requestPermissionLauncherForNotification = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startTracking()
            } else {
                Toast.makeText(activity, "Notification permission is required", Toast.LENGTH_SHORT).show()
            }
        }

        requestPermissionLauncherForCamera = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                takePicture()
            } else {
                Toast.makeText(activity, "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }

        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult? ->
            if (result!!.resultCode == RESULT_OK) {
                // add picture to pictures list
                val picture = Picture(id = 0, timestamp = Date().time, uri = currentPictureUri.toString())
                pictures.add(picture)
            } else {
                // clean up the empty file if no photo was taken
                if (currentPictureFile.exists()) {
                    currentPictureFile.delete()
                }
                Toast.makeText(requireContext(), "Photo capture cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun endTrip(title: String, tripType: TripType, tripDestination: String) {
        lifecycleScope.launch {
            val tripId = withContext(Dispatchers.IO) {
                viewModel.saveTrip(title, startDate, tripType,
                    tripDestination, TripState.COMPLETED)
            }
            notes.forEach {
                it.tripId = tripId
                viewModel.saveNote(it)
            }
            pictures.forEach {
                it.tripId = tripId
                viewModel.savePicture(it)
            }
            viewModel.saveLocations(tripId)

            resetToStart()
            Toast.makeText(requireContext(), "Trip completed!", Toast.LENGTH_SHORT).show()
            //stop foreground tracking service
            val stopIntent = Intent(requireContext(), TrackingService::class.java)
            stopIntent.action = "ACTION_STOP"
            requireContext().startService(stopIntent)
        }
    }

    private fun buildDialogs() {
        // --- stop dialog ---
        val dialogStopView = inflater.inflate(R.layout.dialog_stop_tracking, null)
        distanceTextView = dialogStopView.findViewById(R.id.distanceTextViewStopTracking)
        val tripTypeSpinner: Spinner = dialogStopView.findViewById(R.id.typeSpinnerStopTracking)
        val destinationEditText: EditText = dialogStopView.findViewById(R.id.destinationEditTextStopTracking)

        // Configure spinner
        val types = TripType.entries.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tripTypeSpinner.adapter = adapter

        stopDialog = AlertDialog.Builder(requireContext())
            .setView(dialogStopView)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val destination = destinationEditText.text.toString()
                if (destination.isEmpty())
                    Toast.makeText(requireContext(), "Destination is needed", Toast.LENGTH_SHORT).show()
                else {
                    val titleInput = dialogStopView.findViewById<EditText>(R.id.titleEditTextStopTracking).text
                    endTrip(
                        title = if (titleInput.isNotEmpty()) {
                            titleInput.toString()
                        } else {
                            "Trip on $startDate"
                        },
                        TripType.valueOf(tripTypeSpinner.selectedItem.toString()),
                        destination
                    )
                }
            }
            .setNegativeButton(
                getString(R.string.cancel)
            ) { dialog: DialogInterface, _ -> dialog.dismiss() }
            .setOnDismissListener {
                tripTypeSpinner.setSelection(0)
                destinationEditText.setText("")
            }
            .create()

        // --- new note dialog ---
        val dialogNewNoteView = inflater.inflate(R.layout.dialog_add_note, null)

        val editTextTitle = dialogNewNoteView.findViewById<EditText>(R.id.titleEditText)
        val editTextContent = dialogNewNoteView.findViewById<EditText>(R.id.contentEditText)

        newNoteDialog = AlertDialog.Builder(requireContext())
            .setView(dialogNewNoteView)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val title = editTextTitle.text.toString()
                val content = editTextContent.text.toString()
                if (title.isEmpty() || content.isEmpty())
                    Toast.makeText(requireContext(), "Title and note content are needed", Toast.LENGTH_SHORT).show()
                else {
                    notes.add(Note(id = 0, title = title, date = Date().time, content = content))
                    Toast.makeText(requireContext(), "Note added to trip", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(
                getString(R.string.cancel)
            ) { dialog: DialogInterface, _ -> dialog.dismiss() }
            .setOnDismissListener {
                editTextTitle.setText("")
                editTextContent.setText("")
            }
            .create()
    }

    private fun instantiateViews(view: View) {
        viewPager = requireParentFragment().requireView().findViewById(R.id.home_viewPager)
        trackingLayout = view.findViewById(R.id.trackingLayout)
        trackingTitle = view.findViewById(R.id.trackingTitle)
        startButton = view.findViewById(R.id.startButton)
        stopButton = view.findViewById(R.id.stopButton)
        newNoteImage = view.findViewById(R.id.newNote)
        newPicImage = view.findViewById(R.id.newPic)
        timerTextView = view.findViewById(R.id.trackingTimer)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setListeners() {
        // enable tab swiping when clicking outside the map
        for (view in arrayOf(trackingLayout, trackingTitle, stopButton, newNoteImage, newPicImage)) {
            view.setOnTouchListener { _, _ ->
                enableTabSwiping()
                false
            }
        }
        startButton.setOnClickListener {
            // check for permission before setting up the map
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
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
                startTracking()
            }
        }
        stopButton.setOnClickListener {
            val distance = TrackingRepository.currentDistance
            distanceTextView.text = "Distance: %.01f m".format(distance)
            if (unpackedTripId != -1L && unpackedTripType.isNotEmpty() && unpackedTripDestination.isNotEmpty()) {
                endTrip(
                    title = unpackedTripId.toString(), TripType.valueOf(unpackedTripType), unpackedTripDestination)
                unpackedTripId = -1L
                unpackedTripType = ""
                unpackedTripDestination = ""
                // TODO: remove trip through its id
            }
            else
                stopDialog.show()
        }
        newNoteImage.setOnClickListener {
            newNoteDialog.show()
        }
        newPicImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncherForCamera.launch(Manifest.permission.CAMERA)
            } else {
                takePicture()
            }
        }
    }

    private fun resetToStart() {
        trackingLayout.visibility = View.GONE
        startButton.visibility = View.VISIBLE
        // reset data
        TrackingRepository.resetData()
        map.clear()
    }

    private fun takePicture() {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ), "TravelCompanion"
        )

        // create directory if it doesn't exist
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, "photo_" + System.currentTimeMillis() + ".jpg")
        // generate content uri through file provider to allow access to camera app
        val uri: Uri = FileProvider.getUriForFile(requireContext(),
            "com.example.travelcompanion.provider", file)

        currentPictureFile = file
        currentPictureUri = uri

        val takePicIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePicIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)   // extra used to indicate uri to store image
        takePictureLauncher.launch(takePicIntent)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun startTracking() {
        val mapFragment: SupportMapFragment =
            childFragmentManager.findFragmentById(R.id.mapContainer) as SupportMapFragment
        mapFragment.getMapAsync {googleMap ->
            map = googleMap
            map.uiSettings.isZoomControlsEnabled = true
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
                if (newValue.isNotEmpty()) {
                    val addedLatLng = LatLng(newValue[newValue.size-1].latitude,
                        newValue[newValue.size-1].longitude)
                    if (newValue.size == 1)
                        addStartAndZoom(addedLatLng)    // added location is start location
                    points.add(addedLatLng)
                    polyline.points = points
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(addedLatLng, 17f)
                    )
                }
            }
            viewModel.timerSeconds.observe(requireActivity()) { newValue ->
                timerTextView.text = String.format(
                    Locale.getDefault(), "%02d:%02d", newValue/60, newValue%60)
            }
            // start tracking
            requireContext().startService(Intent(requireContext(), TrackingService::class.java))
            // set startDate
            startDate = Date()
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