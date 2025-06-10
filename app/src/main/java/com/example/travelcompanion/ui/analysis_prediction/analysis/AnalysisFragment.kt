package com.example.travelcompanion.ui.analysis_prediction.analysis

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.travelcompanion.R
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.db.trip.Trip
import com.example.travelcompanion.ui.home.start.StartViewModel
import com.example.travelcompanion.ui.home.start.StartViewModelFactory
import com.example.travelcompanion.ui.journal.archive.ArchiveViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AnalysisFragment : Fragment() {

    companion object {
        fun newInstance() = AnalysisFragment()
    }

    private lateinit var viewModel: AnalysisViewModel

    private lateinit var barChart: BarChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_analysis, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // instantiate ViewModel
        val factory = AnalysisViewModelFactory(repository = TravelCompanionRepository(app = requireActivity().application))
        viewModel = ViewModelProvider(this, factory)[AnalysisViewModel::class.java]

        barChart = view.findViewById(R.id.distances_barChart)
        lifecycleScope.launch {
            val completedTrips = withContext(Dispatchers.IO) {
                viewModel.getCompletedTrips()
            }
            val distancesPerMonth: Map<String, Float> = getDistancesPerMonth(completedTrips)
            setUpBarChart(distancesPerMonth)
            showBarChart(distancesPerMonth)
        }
    }

    private fun setUpBarChart(distancesPerMonth: Map<String, Float>) {
        barChart.axisRight.setDrawLabels(false)
        barChart.setDrawGridBackground(false)
        barChart.legend.isEnabled = false
        barChart.description.isEnabled = false
        barChart.xAxis.textSize = 13f
        barChart.xAxis.setCenterAxisLabels(true)
        barChart.extraBottomOffset = 4f
        barChart.xAxis.setLabelCount(distancesPerMonth.size, true)
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(distancesPerMonth.keys)
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
    }

    private fun showBarChart(distancesPerMonth: Map<String, Float>) {
        val entries: MutableList<BarEntry> = mutableListOf()
        val title = "Title"
        var x = 0.0F

        for (distance in distancesPerMonth.values) {
            val barEntry = BarEntry(x++, distance)
            entries.add(barEntry)
        }

        val barDataSet = BarDataSet(entries, title)
        barDataSet.valueTextSize = 10f
        barDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.primary))

        val data = BarData(barDataSet)
        barChart.setData(data)
        barChart.invalidate()
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
}