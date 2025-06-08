package com.example.travelcompanion.ui.journal.archive

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.travelcompanion.R
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.db.trip.Trip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.utkala.searchablespinner.SearchableSpinner
import pl.utkala.searchablespinner.StringHintArrayAdapter
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.db.pictures.Picture
import pl.utkala.searchablespinner.OnSearchableItemClick
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ArchiveFragment : Fragment() {

    companion object {
        const val SHOW_ALL_PICTURES_CODE = 0L
    }

    private lateinit var viewModel: ArchiveViewModel

    private lateinit var tripSelectionSpinner: SearchableSpinner
    private lateinit var completedTrips: List<Trip>
    private lateinit var galleryRecView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_archive, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // instantiate ViewModel
        val factory = ArchiveViewModelFactory(repository = TravelCompanionRepository(app = requireActivity().application))
        viewModel = ViewModelProvider(this, factory)[ArchiveViewModel::class.java]

        initializeViews(view)
        setListeners()

        lifecycleScope.launch {
            completedTrips = withContext(Dispatchers.IO) {
                viewModel.getCompletedTrips()
            }
            val tripsDestinations: List<String> = completedTrips.map { it.destination }
            setTripsToSpinner(tripsDestinations)
            // select 'All'
            tripSelectionSpinner.setSelection(1)
            updateGallery(SHOW_ALL_PICTURES_CODE)
        }
    }

    private fun initializeViews(view: View) {
        tripSelectionSpinner = view.findViewById(R.id.trip_selection_spinner)
        tripSelectionSpinner.showHint = true
        galleryRecView = view.findViewById(R.id.gallery_recView)
        galleryRecView.layoutManager = GridLayoutManager(requireContext(), 3)
    }

    private fun updateGallery(tripId: Long) {
        lifecycleScope.launch {
            val adapter: GalleryAdapter?
            if (tripId > 0) {   // show pictures of given trip
                val picturesOfTrip = withContext(Dispatchers.IO) {
                    viewModel.getPicturesByTripId(tripId)
                }
                adapter = GalleryAdapter(picturesOfTrip.sortedBy { it.timestamp })
                    { pic -> showPictureInfoDialog(pic) }
            }
            else {  // tripId = 0 -> show all
                val allPictures = withContext(Dispatchers.IO) {
                    viewModel.getAllPictures()
                }
                adapter = GalleryAdapter(allPictures.sortedBy { it.timestamp })
                    { pic -> showPictureInfoDialog(pic) }
            }
            galleryRecView.adapter = adapter
        }
    }

    private fun setListeners() {
        tripSelectionSpinner.onSearchableItemClick= object: OnSearchableItemClick<Any?> {
            override fun onSearchableItemClicked(item:Any?,position:Int){
                if(position>0){
                    tripSelectionSpinner.setSelection(position)
                    if (item.toString() == getString(R.string.all)) {
                        // show all pictures
                        updateGallery(SHOW_ALL_PICTURES_CODE)
                    }
                    else {  // show for specific position
                        // usa position-2 because position starts from 1 and first 1 is "All"
                        updateGallery(completedTrips[position-2].id)
                    }
                }
            }
        }
    }

    private fun setTripsToSpinner(destinations: List<String>) {
        val tripsToDisplay = listOf(getString(R.string.all)) + destinations
        tripSelectionSpinner.adapter= StringHintArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            tripsToDisplay,
            getString(R.string.select_trip)
        )
    }

    private fun showPictureInfoDialog(
        picture: Picture
    ) {
        val trip = completedTrips.find { it.id == picture.tripId }
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_photo_info, null)

        val tripTitleView = dialogView.findViewById<TextView>(R.id.gallery_dialog_trip_title)
        val photoView = dialogView.findViewById<ImageView>(R.id.gallery_dialog_photo)
        val photoTimeView = dialogView.findViewById<TextView>(R.id.gallery_dialog_time)
        val timeIntoTripView = dialogView.findViewById<TextView>(R.id.gallery_dialog_time_into_trip)

        tripTitleView.text = trip?.destination
        photoTimeView.text = formatTimestamp(picture.timestamp)
        timeIntoTripView.text = formatDuration(picture.timestamp - trip?.startTimestamp!!)

        photoView.setImageURI(picture.uri.toUri())

        AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.close), null)
            .show()
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm, dd MMM yyyy", Locale.getDefault())
        val date = Date(timestamp)
        return sdf.format(date)
    }

    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> "$hours h ${minutes % 60} min"
            minutes > 0 -> "$minutes min ${seconds % 60} sec"
            else -> "$seconds sec"
        }
    }

}