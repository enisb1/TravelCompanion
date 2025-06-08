package com.example.travelcompanion.ui.journal.archive

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.travelcompanion.R
import pl.utkala.searchablespinner.SearchableSpinner
import pl.utkala.searchablespinner.StringHintArrayAdapter

class ArchiveFragment : Fragment() {

    companion object {
        fun newInstance() = ArchiveFragment()
    }

    private lateinit var tripSelectionSpinner: SearchableSpinner

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_archive, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tripSelectionSpinner = view.findViewById(R.id.trip_selection_spinner)
        tripSelectionSpinner.showHint = true
        tripSelectionSpinner.adapter= StringHintArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("prova", "test", "daje"),
            "Select Trip"
        )

    }
}