package com.example.travelcompanion.ui.journal.list

import android.app.AlertDialog
import android.icu.text.SimpleDateFormat
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.R
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.db.trip.Trip
import com.example.travelcompanion.ui.home.plan.CompletedTripViewModelFactory
import com.example.travelcompanion.ui.home.plan.TripViewModel
import com.example.travelcompanion.ui.journal.archive.ArchiveFragment

class JournalListFragment : Fragment() {
    private lateinit var tripViewModel: TripViewModel
    private lateinit var spinnerTripType: Spinner
    private lateinit var journalListRecyclerView : RecyclerView
    private lateinit var journalListAdapter: JournalListRecyclerViewAdapter
    private lateinit var noTripsLayout: View
    private lateinit var allTripsLabel: String

    companion object {
        fun newInstance() = JournalListFragment()
    }

    private val viewModel: JournalListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        journalListRecyclerView = view.findViewById(R.id.rvJournalList)
        spinnerTripType = view.findViewById(R.id.spinnerJournalListTripType)
        noTripsLayout = view.findViewById(R.id.noTripsLayout)
        allTripsLabel = getString(R.string.all)

        val factory =
            CompletedTripViewModelFactory(repository = TravelCompanionRepository(app = requireActivity().application))
        tripViewModel = ViewModelProvider(this, factory)[TripViewModel::class.java]

        // Initialize the spinner to filter results based on trip type
        val tripTypes =
            listOf(allTripsLabel) + com.example.travelcompanion.db.trip.TripType.values()
                .map { it.name }
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tripTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTripType.adapter = adapter

        initRecyclerView()

        val fabPopulateDb = view.findViewById<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton>(R.id.fabPopulateDb)
        fabPopulateDb.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                DatabaseSeeder.seed(TravelCompanionRepository(app = requireActivity().application))
                Toast.makeText(this.context, "Database seeded with sample trips", Toast.LENGTH_SHORT).show()
            }
        }

        spinnerTripType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                updateListAndVisibility()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        tripViewModel.completedTrips.observe(viewLifecycleOwner) {
            updateListAndVisibility()
        }
    }

    private fun updateListAndVisibility() {
        val trips = tripViewModel.completedTrips.value ?: emptyList()
        val selectedType = spinnerTripType.selectedItem?.toString()
        val filtered = getFilteredTrips(trips, selectedType)
        val tvNoTripsMessage = noTripsLayout.findViewById<TextView>(R.id.tvNoTripsMessage)
        val linearLayoutListFilter = requireView().findViewById<View>(R.id.linearLayoutListFilter)

        if (filtered.isEmpty()) {
            journalListRecyclerView.visibility = View.GONE
            noTripsLayout.visibility = View.VISIBLE

            // Hide filter layout if there are no trips at all
            if (selectedType != null && selectedType == allTripsLabel) {
                linearLayoutListFilter.visibility = View.GONE
            } else {
                linearLayoutListFilter.visibility = View.VISIBLE
            }

            if (selectedType != null && selectedType != allTripsLabel) {
                tvNoTripsMessage.text = getString(
                    R.string.looks_like_you_have_not_completed_any_type_trips_yet,
                    selectedType
                )
            } else {
                tvNoTripsMessage.setText(R.string.looks_like_you_have_not_completed_any_trips_yet)
            }
        } else {
            journalListRecyclerView.visibility = View.VISIBLE
            noTripsLayout.visibility = View.GONE
            linearLayoutListFilter.visibility = View.VISIBLE
            journalListAdapter.setList(filtered)
            journalListAdapter.notifyDataSetChanged()
        }
    }

    private fun getFilteredTrips(trips: List<Trip>, selectedType: String?): List<Trip> {
        return if (selectedType == null || selectedType == allTripsLabel) {
            trips
        } else {
            trips.filter { it.type.name == selectedType }
        }
    }

    private fun initRecyclerView() {
        journalListRecyclerView.layoutManager = LinearLayoutManager(context)
        journalListAdapter = JournalListRecyclerViewAdapter {
                trip -> showTripDialog(trip)
        }
        journalListRecyclerView.adapter = journalListAdapter

        displayJournalList()
    }

    private fun showTripDialog(trip: Trip) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_trip_details, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTripDetailTitle)
        tvTitle.setText(trip.title)

        val tvDestination = dialogView.findViewById<TextView>(R.id.tvTripDetailDestination)
        tvDestination.setText(trip.destination)

        val tvType = dialogView.findViewById<TextView>(R.id.tvTripDetailType)
        tvType.setText(trip.type.toString())

        val tvStart = dialogView.findViewById<TextView>(R.id.tvTripDetailStart)
        tvStart.setText(SimpleDateFormat("d/M/yyyy").format(trip.startTimestamp))

        val tvEnd = dialogView.findViewById<TextView>(R.id.tvTripDetailEnd)
        tvEnd.text = SimpleDateFormat("d/M/yyyy").format(trip.endTimestamp)

        val tvDistance = dialogView.findViewById<TextView>(R.id.tvTripDetailDistance)
        tvDistance.text = getString(R.string.trip_dist_m, trip.distance.toString())

        val tvDuration = dialogView.findViewById<TextView>(R.id.tvTripDetailDuration)
        tvDuration.text = getString(R.string.trip_duration_s, trip.duration.toString())

        val btnDelete = dialogView.findViewById<Button>(R.id.btnDeleteTrip)
        btnDelete.setOnClickListener {
            tripViewModel.deleteTrip(trip)
            ArchiveFragment.deletedTrip = true
            dialog.dismiss()
            Toast.makeText(requireContext(), R.string.trip_deleted, Toast.LENGTH_SHORT).show()
        }

        // Remove background in dialog
        dialogView.setBackgroundResource(android.R.color.transparent)

        dialog.show()
    }

    private fun displayJournalList(){
        tripViewModel.completedTrips.observe(viewLifecycleOwner, {
            journalListAdapter.setList(it)
            journalListAdapter.notifyDataSetChanged()
        })
    }

}