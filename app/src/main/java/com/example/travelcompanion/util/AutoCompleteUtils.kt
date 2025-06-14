package com.example.travelcompanion.util

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.example.travelcompanion.ui.home.plan.TripViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun setupDestinationAutoComplete(
    context: Context,
    autoCompleteTextView: AutoCompleteTextView,
    viewModel: TripViewModel,
    coroutineScope: CoroutineScope
) {
    coroutineScope.launch {
        val destinations = withContext(Dispatchers.IO) {
            viewModel.getDistinctDestinations()
        }
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, destinations)
        autoCompleteTextView.setAdapter(adapter)
    }
}