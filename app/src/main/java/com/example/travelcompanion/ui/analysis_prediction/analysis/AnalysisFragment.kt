package com.example.travelcompanion.ui.analysis_prediction.analysis

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
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
import com.kview.FullLengthListView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import java.math.BigDecimal
import java.math.RoundingMode


class AnalysisFragment : Fragment() {

    private lateinit var viewModel: AnalysisViewModel

    private lateinit var viewPager: ViewPager2  // parent fragment viewPager

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var barChart: BarChart
    private lateinit var topDestinationsListView: FullLengthListView
    private lateinit var totalDistanceTxtView: TextView
    private lateinit var travelFrequencyTxtView: TextView
    private lateinit var yearSpinner: Spinner
    private lateinit var noTripsLayout: ConstraintLayout

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

        noTripsLayout = view.findViewById(R.id.no_trips_constraint_analysis)

        initializeViews(view)
        setListeners()

        lifecycleScope.launch {
            val completedTrips = withContext(Dispatchers.IO) {
                viewModel.getCompletedTrips()
            }

            if(completedTrips.isEmpty()){
                noTripsLayout.visibility = View.VISIBLE
                rootLayout.visibility = View.GONE
            } else {
                noTripsLayout.visibility = View.GONE
                rootLayout.visibility = View.VISIBLE
            }

            //  bar chart
            setUpYearsSpinner(completedTrips)
            yearSpinner.setSelection(viewModel.spinnerSelection.value ?: 0)
            yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    viewModel.spinnerSelection.value = position
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            viewModel.spinnerSelection.observe(viewLifecycleOwner) {
                val (labels, values) = if (yearSpinner.selectedItemPosition > 0) {
                    val calendar = Calendar.getInstance()
                    getDistancesPerMonth(completedTrips.filter {
                        calendar.timeInMillis = it.startTimestamp
                        calendar.get(Calendar.YEAR).toString() == yearSpinner.selectedItem })
                } else {
                    getDistancesPerMonth(completedTrips)
                }

                setDataToBarChart(values)
                setXLabelsToBarChart(labels)
                configureBarChartXAxis()
                configureBarChart()
            }

            // top destinations
            val topDestinations = getTopDestinations(completedTrips)
            showTopDestinations(topDestinations)

            // total distance
            val totalDistance = getTotalDistance(completedTrips)
            totalDistanceTxtView.text = getString(R.string.tot_dist_km, totalDistance.toString())

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
        yearSpinner = view.findViewById(R.id.year_spinner)
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

    private fun setUpYearsSpinner(completedTrips: List<Trip>) {
        val years = listOf(getString(R.string.all)) + getYearsOfTrips(completedTrips)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        yearSpinner.adapter = adapter
    }

    private fun setDataToBarChart(distancesPerMonth: List<Float>) {
        val entries = distancesPerMonth.mapIndexed { index, value ->
            BarEntry(index.toFloat(), value)
        }

        val dataSet = BarDataSet(entries, "Distances")
        dataSet.valueTextSize = 10f
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.primary))
        val barData = BarData(dataSet)
        barChart.data = barData
    }

    private fun configureBarChartXAxis() {
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textSize = 10f
        xAxis.isGranularityEnabled = true
        xAxis.setCenterAxisLabels(false) // Very important if not using grouped bars
    }

    private fun setXLabelsToBarChart(labels: List<String>) {
        val xAxis: XAxis = barChart.xAxis
        xAxis.setLabelCount(labels.size)
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase): String {
                val index = value.roundToInt()
                return if (index in labels.indices) labels[index] else ""
            }
        }
    }

    private fun configureBarChart() {
        barChart.axisRight.setDrawLabels(false)
        barChart.setDrawGridBackground(false)
        barChart.legend.isEnabled = false
        barChart.setVisibleXRangeMaximum(5f)
        barChart.description.isEnabled = false
        barChart.isDoubleTapToZoomEnabled = false
        barChart.setPinchZoom(false)
        barChart.invalidate()
    }

    // given a  list of trips, it returns a map that maps months to total distances
    private fun getDistancesPerMonth(trips: List<Trip>): Pair<List<String>, List<Float>> {
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
        val months = distancesPerMonth.keys.toList()
        val distances = distancesPerMonth.values.toList()

        return Pair(months, distances)
    }

    private fun getTopDestinations(trips: List<Trip>): List<Map.Entry<String, Int>> {
        return trips
            .groupingBy { it.destination }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
    }

    private fun showTopDestinations(topDestinations: List<Map. Entry<String, Int>>) {
        val destinationStrings: List<String> = topDestinations.mapIndexed { index, entry ->
            "#${index+1}: ${entry.key} (${entry.value} ${if (entry.value == 1) "trip" else "trips"})"
        }
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.destinations_list_item, // your custom layout
            R.id.destination,        // the TextView inside that layout
            destinationStrings
        )
        topDestinationsListView.adapter = adapter
    }

    // in kilometres
    private fun getTotalDistance(trips: List<Trip>): Double {
        val totalMeters = trips.sumOf { it.distance }
        val totalKilometers = totalMeters / 1000.0
        return BigDecimal(totalKilometers).setScale(3, RoundingMode.HALF_DOWN).toDouble()
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
            months > 0 -> getString(R.string.duration_months_days, months, days % 30)
            days > 0 -> getString(R.string.duration_days_hours, days, hours % 24)
            hours > 0 -> getString(R.string.duration_hours_minutes, hours, minutes % 60)
            minutes > 0 -> getString(R.string.duration_minutes_seconds, minutes, seconds % 60)
            else -> getString(R.string.duration_seconds, seconds)
        }
    }

    private fun getYearsOfTrips(trips: List<Trip>): List<String> {
        val calendar = Calendar.getInstance()
        return trips.map {
            calendar.timeInMillis = it.startTimestamp
            calendar.get(Calendar.YEAR).toString()
        }.toSet().toList().sortedDescending()  // Sort in descending order
    }
}