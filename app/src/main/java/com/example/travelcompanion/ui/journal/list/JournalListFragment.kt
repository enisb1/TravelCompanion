package com.example.travelcompanion.ui.journal.list

import android.app.AlertDialog
import android.icu.text.SimpleDateFormat
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.R
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.db.trip.Trip
import com.example.travelcompanion.ui.home.plan.CompletedTripViewModelFactory
import com.example.travelcompanion.ui.home.plan.TripViewModel

class JournalListFragment : Fragment() {
    private lateinit var tripViewModel: TripViewModel
    private lateinit var journalListRecyclerView : RecyclerView
    private lateinit var journalListAdapter: JournalListRecyclerViewAdapter

    companion object {
        fun newInstance() = JournalListFragment()
    }

    private val viewModel: JournalListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
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

        val factory = CompletedTripViewModelFactory(repository = TravelCompanionRepository(app = requireActivity().application))
        tripViewModel = ViewModelProvider(this, factory)[TripViewModel::class.java]

        initRecyclerView()


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

        val tvDestination = dialogView.findViewById<TextView>(R.id.tvTripDetailDestination)
        tvDestination.setText(trip.destination)

        val tvType = dialogView.findViewById<TextView>(R.id.tvTripDetailType)
        tvType.setText(trip.type.toString())

        val tvStart = dialogView.findViewById<TextView>(R.id.tvTripDetailStart)
        tvStart.setText(SimpleDateFormat("d/M/yyyy").format(trip.startTimestamp))

        val tvEnd = dialogView.findViewById<TextView>(R.id.tvTripDetailEnd)
        tvEnd.setText(SimpleDateFormat("d/M/yyyy").format(trip.endTimestamp))

        val tvDistance = dialogView.findViewById<TextView>(R.id.tvTripDetailDistance)
        tvDistance.setText(trip.distance.toString())

        val tvDuration = dialogView.findViewById<TextView>(R.id.tvTripDetailDuration)
        tvDuration.setText(trip.duration.toString())

        dialog.show()
    }

    private fun displayJournalList(){
        tripViewModel.completedTrips.observe(viewLifecycleOwner, {
            journalListAdapter.setList(it)
            journalListAdapter.notifyDataSetChanged()
        })
    }

}