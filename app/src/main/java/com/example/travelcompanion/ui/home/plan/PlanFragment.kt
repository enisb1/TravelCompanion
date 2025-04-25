package com.example.travelcompanion.ui.home.plan

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.travelcompanion.R
import com.example.travelcompanion.db.PlanType
import java.util.Calendar

class PlanFragment : Fragment() {

    private val viewModel: PlanViewModel by viewModels()
    private var selectedDate: Calendar? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_plan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pickDateButton = view.findViewById<Button>(R.id.pickDateButton)
        val typeSpinner = view.findViewById<Spinner>(R.id.typeSpinner)
        val destinationEditText = view.findViewById<EditText>(R.id.destinationEditText)
        val saveButton = view.findViewById<View>(R.id.saveButton)

        // Configure spinner
        val types = PlanType.entries.map { it.name }
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

        // Gestione pulsante Salva
        saveButton.setOnClickListener {
            Log.i("PlanFragment", "Save button clicked")
            val date = selectedDate?.time
            if (date != null) {
                val type = PlanType.valueOf(typeSpinner.selectedItem.toString())
                val destination = destinationEditText.text.toString()
                Log.i("PlanFragment", "Saving plan: $date, $type, $destination")
                viewModel.savePlan(date, type, destination)
                Log.i("PlanFragment", "Plan saved successfully")
            }
        }
    }
}