package com.example.travelcompanion.ui.home.plan

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.R
import com.example.travelcompanion.db.TravelCompanionDatabase
import com.example.travelcompanion.db.TripType
import java.util.Calendar

class PlanFragment : Fragment() {

    private lateinit var pickDateButton: Button
    private lateinit var typeSpinner: Spinner
    private lateinit var destinationEditText: EditText
    private lateinit var saveButton: Button

    private lateinit var viewModel: PlanViewModel
    private var selectedDate: Calendar? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_plan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pickDateButton = view.findViewById(R.id.pickDateButton)
        typeSpinner = view.findViewById(R.id.typeSpinner)
        destinationEditText = view.findViewById(R.id.destinationEditText)
        saveButton = view.findViewById(R.id.saveButton)

        // Configure spinner
        val types = TripType.entries.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = adapter

        // Manage date picker button
        pickDateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    selectedDate = Calendar.getInstance().apply {
                        set(selectedYear, selectedMonth, selectedDay)
                    }
                    pickDateButton.text = "Selected Date: ${selectedDay}/${selectedMonth + 1}/${selectedYear}"
                },
                year, month, day
            )
            datePickerDialog.show()
        }

        val dao = TravelCompanionDatabase.getInstance(requireContext()).tripDao()
        val factory = PlanViewModelFactory(dao)
        viewModel = ViewModelProvider(this, factory)[PlanViewModel::class.java]

        // Manage save button
        saveButton.setOnClickListener {
            savePlanData()
        }
    }

    private fun savePlanData() {
        val startDate = selectedDate?.time
        val type = TripType.valueOf(typeSpinner.selectedItem.toString())
        val destination = destinationEditText.text.toString()

        if (startDate != null && destination.isNotEmpty()) {
            viewModel.savePlan(startDate, type, destination)
            clearInput()
        } else {
            Toast.makeText(
                requireContext(),
                "Please fill all fields",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun clearInput() {
        selectedDate = null
        pickDateButton.text = resources.getText(R.string.pick_start_date)
        typeSpinner.setSelection(0)
        destinationEditText.text.clear()
    }
}