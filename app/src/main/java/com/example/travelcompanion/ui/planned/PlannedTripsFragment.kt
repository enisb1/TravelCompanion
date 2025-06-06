package com.example.travelcompanion.ui.planned

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.R
import com.example.travelcompanion.db.trip.Trip
import com.example.travelcompanion.ui.home.plan.TripViewModel
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.db.trip.TripType
import com.example.travelcompanion.ui.home.plan.PlanViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Calendar

class PlannedTripsFragment : Fragment() {

    private lateinit var tripViewModel: TripViewModel
    private lateinit var planRecyclerView : RecyclerView
    private lateinit var planAdapter : PlanRecyclerViewAdapter
    private lateinit var plusButton: FloatingActionButton
    private var selectedDate: Calendar? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_planned_trips, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val plannedTripsVM : PlannedTripsViewModel = ViewModelProvider(this)
            .get(PlannedTripsViewModel::class.java)

        planRecyclerView = view.findViewById(R.id.rvPlanned)
        plusButton = view.findViewById(R.id.fabAdd)


        val factory = PlanViewModelFactory(repository = TravelCompanionRepository(app = requireActivity().application))
        tripViewModel = ViewModelProvider(this, factory)[TripViewModel::class.java]

        initRecyclerView()

        plusButton.setOnClickListener {
            val action = PlannedTripsFragmentDirections.actionNavPlannedToHome(
                1,
                -1,
                "",
                ""
            )
            findNavController().navigate(action)
        }

    }

    private fun initRecyclerView() {
        planRecyclerView.layoutManager = LinearLayoutManager(context)
        planAdapter = PlanRecyclerViewAdapter{
            plan -> showPlanDialog(plan)
        }
        planRecyclerView.adapter = planAdapter

        displayPlanList()
    }

    private fun showPlanDialog(trip: Trip) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_plan_details, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val etDestination = dialogView.findViewById<EditText>(R.id.etDetailDestination)
        etDestination.setText(trip.destination)

        val tvStart = dialogView.findViewById<TextView>(R.id.tvDetailStart)
        tvStart.text = SimpleDateFormat("d/M/yyyy").format(trip.startTimestamp)
        tvStart.setOnClickListener {
            val calendar = Calendar.getInstance()
            // Set the calendar to the trip's start date
            calendar.timeInMillis = trip.startTimestamp
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    selectedDate = Calendar.getInstance().apply {
                        set(selectedYear, selectedMonth, selectedDay)
                    }
                    tvStart.text = "${selectedDay}/${selectedMonth + 1}/${selectedYear}"
                },
                year, month, day
            )
            datePickerDialog.show()
        }

        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerDetailType)
        val types = TripType.entries.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = adapter
        spinnerType.setSelection(when (trip.type) {
            TripType.LOCAL -> 0
            TripType.ONEDAY -> 1
            TripType.MULTIDAY -> 2
        })

        dialogView.findViewById<Button>(R.id.btnStartTrip).setOnClickListener {
            // start trip by navigating to home (start) fragment
            val action = PlannedTripsFragmentDirections.actionNavPlannedToHome(
                0,
                trip.id,
                trip.type.toString(),
                trip.destination
            )
            findNavController().navigate(action)
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.btnEditTrip).setOnClickListener {
            trip.destination = etDestination.text.toString()
            trip.startTimestamp = selectedDate?.timeInMillis ?: trip.startTimestamp
            trip.type = when (spinnerType.selectedItemPosition) {
                0 -> TripType.LOCAL
                1 -> TripType.ONEDAY
                2 -> TripType.MULTIDAY
                else -> trip.type // Default to the current type if something goes wrong
            }

            tripViewModel.updatePlan(trip)
            Toast.makeText(requireContext(), "Trip updated", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            displayPlanList()
        }
        dialogView.findViewById<Button>(R.id.btnDeleteTrip).setOnClickListener{
            tripViewModel.deletePlan(trip)
            Toast.makeText(requireContext(), "Trip deleted", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            displayPlanList()
        }

        dialog.show()
    }

    private fun displayPlanList(){
        tripViewModel.plans.observe(viewLifecycleOwner, {
            planAdapter.setList(it)
            planAdapter.notifyDataSetChanged()
        })
    }
}