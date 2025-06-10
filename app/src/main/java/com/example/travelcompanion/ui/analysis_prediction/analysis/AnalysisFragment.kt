package com.example.travelcompanion.ui.analysis_prediction.analysis

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.travelcompanion.R
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.db.trip.Trip
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt


class AnalysisFragment : Fragment() {

    companion object {
        fun newInstance() = AnalysisFragment()
    }

    private lateinit var viewModel: AnalysisViewModel

    private lateinit var viewPager: ViewPager2  // parent fragment viewPager

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var barChart: BarChart
    private lateinit var topDestinationsListView: ListView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_analysis, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // instantiate ViewModel
        val factory = AnalysisViewModelFactory(repository = TravelCompanionRepository(app = requireActivity().application))
        viewModel = ViewModelProvider(this, factory)[AnalysisViewModel::class.java]

        initializeViews(view)
        setListeners()

        lifecycleScope.launch {
            val completedTrips = withContext(Dispatchers.IO) {
                viewModel.getCompletedTrips()
            }
            val distancesPerMonth: Map<String, Float> = getDistancesPerMonth(completedTrips)
            val labels = distancesPerMonth.keys.toList() // ["Feb", "Mar", "Apr"]

// Create entries with proper x-values (index-based)
            val entries = distancesPerMonth.values.mapIndexed { index, value ->
                BarEntry(index.toFloat(), value)
            }

            val dataSet = BarDataSet(entries, "Distances")
            dataSet.valueTextSize = 10f
            dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.primary))
            val barData = BarData(dataSet)
            barChart.data = barData

// Configure X axis
            val xAxis = barChart.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.textSize = 10f
            xAxis.isGranularityEnabled = true
            xAxis.setLabelCount(labels.size)
            xAxis.setCenterAxisLabels(false) // Very important if not using grouped bars
// Formatter
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase): String {
                    val index = value.roundToInt()
                    return if (index in labels.indices) labels[index] else ""
                }
            }

            barChart.axisRight.setDrawLabels(false)
            barChart.setDrawGridBackground(false)
            barChart.legend.isEnabled = false
            barChart.setVisibleXRangeMaximum(5f)
            barChart.description.isEnabled = false
            barChart.isDoubleTapToZoomEnabled = false
            barChart.setPinchZoom(false)
            barChart.invalidate()

            // top destinations
            val topDestinations = getTopDestinations(completedTrips)
            val destinationStrings: List<String> = topDestinations.mapIndexed { index, entry ->
                "#${index+1}: ${entry.key} (${entry.value} trips)"
            }
            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.destinations_list_item, // your custom layout
                R.id.destination,        // the TextView inside that layout
                destinationStrings
            )
            topDestinationsListView.adapter = adapter

        }
    }

    private fun initializeViews(view: View) {
        barChart = view.findViewById(R.id.distances_barChart)
        rootLayout = view.findViewById(R.id.analysis_root_layout)
        topDestinationsListView = view.findViewById(R.id.top_destinations_listView)
        viewPager = requireParentFragment().requireView().findViewById(R.id.analysis_prediction_viewPager)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setListeners() {
        barChart.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    disableTabSwiping()
                    false
                }
                else -> false
            }
        }
        rootLayout.setOnTouchListener { _, _ ->
            enableTabSwiping()
            false
        }
        topDestinationsListView.setOnTouchListener { _, _ ->
            enableTabSwiping()
            false
        }
    }

    private fun disableTabSwiping() {
        viewPager.isUserInputEnabled = false
    }

    private fun enableTabSwiping() {
        viewPager.isUserInputEnabled = true
    }

    //TODO: CENTER LABELS UNDER THE BARS
    private fun setUpBarChart(distancesPerMonth: Map<String, Float>) {
        val labels = distancesPerMonth.keys.toList()
        barChart.axisRight.setDrawLabels(false)
        barChart.setDrawGridBackground(false)
        barChart.legend.isEnabled = false
        barChart.description.isEnabled = false
        Log.i("rere", distancesPerMonth.toString())

        val xAxis = barChart.xAxis
        xAxis.textSize = 10f
        barChart.extraBottomOffset = 4f
        xAxis.setLabelCount(distancesPerMonth.size, true)
        val formatter: ValueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase): String {
                val index = value.toInt()
                Log.i("rere", index.toString())
                return if (index >= 0 && index < labels.size) labels[index] else ""
            }
        }
        xAxis.granularity = 1f // minimum axis-step (interval) is 1
        xAxis.isGranularityEnabled = true
        xAxis.valueFormatter = formatter
        Log.i("labels", distancesPerMonth.keys.toString())
        xAxis.position = XAxis.XAxisPosition.BOTTOM
    }

    private fun showBarChart(distancesPerMonth: Map<String, Float>) {
        val title = "Title"
        val entries = distancesPerMonth.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value)
        }

        val barDataSet = BarDataSet(entries, title)
        barDataSet.valueTextSize = 10f
        barDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.primary))

        val data = BarData(barDataSet)
        barChart.setData(data)
        barChart.invalidate()
        //barChart.setVisibleXRangeMaximum(5f)
    }
    
    // TODO: put in separate calculating class
    // given a  list of trips, it returns a map that maps months to total distances
    private fun getDistancesPerMonth(trips: List<Trip>): Map<String, Float> {
        val distancesPerMonth: MutableMap<String, Float> = mutableMapOf()
        val sortedTrips = trips.sortedBy { it.startTimestamp }
        var sum = 0.0F
        for (trip in sortedTrips) {
            val monthYear = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(trip.startTimestamp)
            if (distancesPerMonth.containsKey(monthYear)) {
                distancesPerMonth[monthYear] = distancesPerMonth[monthYear]!! + trip.distance.toFloat()
                sum += trip.distance.toFloat()
            }
            else
                distancesPerMonth[monthYear] = trip.distance.toFloat()
        }
        return distancesPerMonth
    }

    private fun getTopDestinations(trips: List<Trip>): List<Map.Entry<String, Int>> {
        return trips
            .groupingBy { it.destination }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
    }
}