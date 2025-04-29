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
import androidx.fragment.app.viewModels
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

class StartFragment : Fragment() {

    private val viewModel: StartViewModel by viewModels()

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

    private lateinit var viewPager: ViewPager2  // parent fragment viewPager

    private lateinit var currentPictureFile: File
    private lateinit var currentPictureUri: Uri

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_start, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setLaunchers()
        instantiateViews(view)
        setListeners()
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
                    setUpMap()
                else    // notification permission is also needed
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
                // TODO: save uri in local database to later access it
            } else {
                // clean up the empty file if no photo was taken
                if (currentPictureFile.exists()) {
                    currentPictureFile.delete()
                }
                Toast.makeText(requireContext(), "Photo capture cancelled", Toast.LENGTH_SHORT).show()
            }
        }
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
        newNoteImage.setOnClickListener {
            // show dialog
            val inflater = LayoutInflater.from(requireContext())
            val dialogView = inflater.inflate(R.layout.dialog_add_note, null)

            val editTextTitle = dialogView.findViewById<EditText>(R.id.titleEditText)
            val editTextNote = dialogView.findViewById<EditText>(R.id.contentEditText)

            AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton(getString(R.string.add)) { _, _ ->
                    val title = editTextTitle.text.toString()
                    val note = editTextNote.text.toString()
                    //TODO: save in db
                }
                .setNegativeButton(
                    getString(R.string.cancel)
                ) { dialog: DialogInterface, _ -> dialog.dismiss() }
                .show()
        }
        newPicImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncherForCamera.launch(Manifest.permission.CAMERA)
            } else {
                takePicture()
            }
        }
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
    private fun setUpMap() {
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