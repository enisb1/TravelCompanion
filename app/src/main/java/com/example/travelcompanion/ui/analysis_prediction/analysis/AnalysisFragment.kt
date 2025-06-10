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
import android.widget.TextView
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
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong


class AnalysisFragment : Fragment() {

    private lateinit var viewModel: AnalysisViewModel

    private lateinit var viewPager: ViewPager2  // parent fragment viewPager

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var barChart: BarChart
    private lateinit var topDestinationsListView: ListView
    private lateinit var totalDistanceTxtView: TextView
    private lateinit var travelFrequencyTxtView: TextView

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

            // total distance
            val totalDistance = getTotalDistance(completedTrips)
            // TODO: check if metres is the correct measure unit and if sum is correct
            totalDistanceTxtView.text = totalDistance.toString() + " " + getString(R.string.kilometres)

            // travel frequency
            val travelFrequency = formatDuration(estimateTravelFrequency(completedTrips))
            travelFrequencyTxtView.text = travelFrequency
        }
    }

    private fun initializeViews(view: View) {
        barChart = view.findViewById(R.id.distances_barChart)
        rootLayout = view.findViewById(R.id.analysis_root_layout)
        topDestinationsListView = view.findViewById(R.id.top_destinations_listView)
        viewPager = requireParentFragment().requireView().findViewById(R.id.analysis_prediction_viewPager)
        totalDistanceTxtView = view.findViewById(R.id.total_distance)
        travelFrequencyTxtView = view.findViewById(R.id.travel_frequency)
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

    // in kilometres
    private fun getTotalDistance(trips: List<Trip>): Double {
        val totalMeters = trips.sumOf { it.distance }
        val totalKilometers = totalMeters / 1000.0
        return String.format( Locale.getDefault(),"%.3f", totalKilometers).toDouble()
    }

    private fun estimateTravelFrequency(trips: List<Trip>): Long {
        if (trips.size < 2) return 0L // Not enough data

        val sortedTrips = trips.sortedBy { it.startTimestamp }
        val timeBetweenSuccessiveTrips = mutableListOf<Long>()

        for (i in 1 until sortedTrips.size) {
            val durationBetween = sortedTrips[i].startTimestamp - sortedTrips[i-1].endTimestamp
            timeBetweenSuccessiveTrips.add(durationBetween)
        }

        return timeBetweenSuccessiveTrips.average().roundToLong() // Average days between trips
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