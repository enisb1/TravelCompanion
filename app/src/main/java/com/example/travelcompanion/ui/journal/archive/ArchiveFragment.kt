package com.example.travelcompanion.ui.journal.archive

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.db.notes.Note
import com.example.travelcompanion.db.pictures.Picture
import com.google.android.material.button.MaterialButton
import pl.utkala.searchablespinner.OnSearchableItemClick
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ArchiveFragment : Fragment() {

    companion object {
        const val SHOW_ALL_DATA_CODE = 0L
    }

    private lateinit var viewModel: ArchiveViewModel

    private lateinit var picturesButton: MaterialButton
    private lateinit var notesButton: MaterialButton
    private lateinit var tripSelectionSpinner: SearchableSpinner
    private lateinit var galleryRecView: RecyclerView
    private lateinit var notesRecView: RecyclerView
    private lateinit var noResourceLayout: ConstraintLayout

    private lateinit var completedTrips: List<Trip>

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
            val tripTitles: List<String> = completedTrips.map { it.title }
            setTripsToSpinner(tripTitles)
            tripSelectionSpinner.setSelection(viewModel.spinnerSelection)
            showGalleryOrNotes()
            updateGalleryOrNotes()
        }
    }

    private fun initializeViews(view: View) {
        tripSelectionSpinner = view.findViewById(R.id.trip_selection_spinner)
        tripSelectionSpinner.showHint = true
        galleryRecView = view.findViewById(R.id.gallery_recView)
        galleryRecView.layoutManager = GridLayoutManager(requireContext(), 3)
        notesRecView = view.findViewById(R.id.notes_recView)
        notesRecView.layoutManager = LinearLayoutManager(requireContext())
        picturesButton = view.findViewById(R.id.pictures_button)
        notesButton = view.findViewById(R.id.notes_button)
        noResourceLayout = view.findViewById(R.id.no_trips_layout_archive)
    }

    private fun updateGallery(tripId: Long) {
        lifecycleScope.launch {
            val adapter: GalleryAdapter?
            val pictures: List<Picture>
            if (tripId > 0) {   // show pictures of given trip
                val picturesOfTrip = withContext(Dispatchers.IO) {
                    viewModel.getPicturesByTripId(tripId)
                }
                adapter = GalleryAdapter(picturesOfTrip.sortedBy { it.timestamp })
                    { pic -> showPictureInfoDialog(pic) }
                pictures = picturesOfTrip
            }
            else {  // tripId = 0 -> show all
                val allPictures = withContext(Dispatchers.IO) {
                    viewModel.getAllPictures()
                }
                adapter = GalleryAdapter(allPictures.sortedBy { it.timestamp })
                    { pic -> showPictureInfoDialog(pic) }
                pictures = allPictures
            }
            if (pictures.isEmpty())
                showNoPicturesLayout()
            galleryRecView.adapter = adapter
        }
    }

    private fun showNoPicturesLayout() {
        noResourceLayout.visibility = View.VISIBLE
        val noPicturesMessage = noResourceLayout.findViewById<TextView>(R.id.tvNoTripsMessage)
        noPicturesMessage.text = "It looks like no pictures have been taken!"
    }

    private fun showNoNotesLayout() {
        noResourceLayout.visibility = View.VISIBLE
        val noNotesMessage = noResourceLayout.findViewById<TextView>(R.id.tvNoTripsMessage)
        noNotesMessage.text = "It looks like no notes have been written!"
    }

    private fun updateNotes(tripId: Long) {
        lifecycleScope.launch {
            val adapter: NotesAdapter?
            val notes: List<Note>
            if (tripId > 0) {
                val notesOfTrip = withContext(Dispatchers.IO) {
                    viewModel.getNotesByTripId(tripId)
                }
                adapter = NotesAdapter(
                    notesOfTrip.sortedBy { it.date },
                    completedTrips.associate { it.id to it.title },
                    completedTrips.associate { it.id to it.destination }
                )
                notes = notesOfTrip
            }
            else {  // tripId = 0 -> show all
                val allNotes = withContext(Dispatchers.IO) {
                    viewModel.getAllNotes()
                }
                adapter = NotesAdapter(
                    allNotes.sortedBy { it.date },
                    completedTrips.associate { it.id to it.title },
                    completedTrips.associate { it.id to it.destination }
                )
                notes = allNotes
            }
            if (notes.isEmpty())
                showNoNotesLayout()
            notesRecView.adapter = adapter
        }
    }

    private fun setListeners() {
        tripSelectionSpinner.onSearchableItemClick= object: OnSearchableItemClick<Any?> {
            override fun onSearchableItemClicked(item:Any?,position:Int){
                if(position>0){
                    tripSelectionSpinner.setSelection(position)
                    viewModel.spinnerSelection = position
                    updateGalleryOrNotes()
                }
            }
        }
        picturesButton.setOnClickListener {
            showGalleryOrNotes()
            updateGalleryOrNotes()
        }
        notesButton.setOnClickListener {
            showGalleryOrNotes()
            updateGalleryOrNotes()
        }
    }

    private fun showGalleryOrNotes() {
        if (picturesButton.isChecked) {
            galleryRecView.visibility = View.VISIBLE
            notesRecView.visibility = View.GONE
        }
        else {
            galleryRecView.visibility = View.GONE
            notesRecView.visibility = View.VISIBLE
        }
    }

    private fun updateGalleryOrNotes() {
        if (viewModel.spinnerSelection == 1) {
            // show all pictures
            if (picturesButton.isChecked)
                updateGallery(SHOW_ALL_DATA_CODE)
            else
                updateNotes(SHOW_ALL_DATA_CODE)
        }
        else {  // show for specific position
            // usa position-2 because position starts from 1 and first 1 is "All"
            if (picturesButton.isChecked)
                updateGallery(completedTrips[viewModel.spinnerSelection-2].id)
            else
                updateNotes(completedTrips[viewModel.spinnerSelection-2].id)
        }
    }

    private fun setTripsToSpinner(titles: List<String>) {
        val tripsToDisplay = listOf(getString(R.string.all)) + titles
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
        val days = hours / 24
        val months = days / 30  // Approximation: 30 days = 1 month

        return when {
            months > 0 -> "$months months ${days % 30} days"
            days > 0 -> "$days days ${hours % 24} hours"
            hours > 0 -> "$hours hours ${minutes % 60} minutes"
            minutes > 0 -> "$minutes minutes ${seconds % 60} seconds"
            else -> "$seconds seconds"
        }
    }

}