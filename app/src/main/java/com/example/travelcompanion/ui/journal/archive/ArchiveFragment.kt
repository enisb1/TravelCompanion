package com.example.travelcompanion.ui.journal.archive

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.travelcompanion.R
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.db.trip.Trip
import com.example.travelcompanion.ui.journal.map.MapViewModel
import com.example.travelcompanion.ui.journal.map.MapViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.utkala.searchablespinner.SearchableSpinner
import pl.utkala.searchablespinner.StringHintArrayAdapter
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pl.utkala.searchablespinner.OnSearchableItemClick

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
            var adapter: GalleryAdapter? = null
            if (tripId > 0) {   // show pictures of given trip
                val picturesOfTrip = withContext(Dispatchers.IO) {
                    viewModel.getPicturesByTripId(tripId)
                }
                adapter = GalleryAdapter(picturesOfTrip)
            }
            else {  // tripId = 0 -> show all
                val allPictures = withContext(Dispatchers.IO) {
                    viewModel.getAllPictures()
                }
                adapter = GalleryAdapter(allPictures)
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

}