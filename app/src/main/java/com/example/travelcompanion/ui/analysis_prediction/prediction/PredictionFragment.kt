package com.example.travelcompanion.ui.analysis_prediction.prediction

import android.app.AlertDialog
import android.graphics.Color
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.provider.Settings.Global
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.R
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.db.trip.Trip
import com.example.travelcompanion.ui.home.plan.CompletedTripViewModelFactory
import com.example.travelcompanion.ui.home.plan.TripViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.example.travelcompanion.ui.analysis_prediction.CustomMarkerView
import com.example.travelcompanion.ui.analysis_prediction.prediction.PredictionUtils.predictNextMonthDistance
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import java.util.Calendar
import com.google.android.material.floatingactionbutton.FloatingActionButton


class PredictionFragment : Fragment() {

    private lateinit var tvForecast: TextView
    private lateinit var tvRecommendations: TextView
    private lateinit var tripViewModel: TripViewModel
    private lateinit var lineChart: LineChart
    private lateinit var lineChartDistance: LineChart
    private lateinit var tvDistanceForecast: TextView
    private lateinit var fabFilterYear: FloatingActionButton
    private lateinit var fabObjectiveWarning: FloatingActionButton
    private var yearOptions: List<String> = emptyList()
    private var selectedYear: Int? = null
    private var lastPredictedCount: Int = 0
    private var lastPredictedDistance: Double = 0.0
    private lateinit var noTripsLayout: View
    private lateinit var scrollView: View


    companion object {
        fun newInstance() = PredictionFragment()
    }

    private val viewModel: PredictionViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_prediction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val factory =
            CompletedTripViewModelFactory(repository = TravelCompanionRepository(app = requireActivity().application))
        tripViewModel = ViewModelProvider(this, factory)[TripViewModel::class.java]
        tvForecast = view.findViewById(R.id.tvPredictionForecast)
        tvRecommendations = view.findViewById(R.id.tvPredictionRecommend)
        lineChart = view.findViewById(R.id.lineChart)
        lineChartDistance = view.findViewById(R.id.lineChartDistance)
        tvDistanceForecast = view.findViewById(R.id.tvPredictionDistanceForecast)
        noTripsLayout = view.findViewById(R.id.no_trips_constraint_prediction)
        scrollView = view.findViewById(R.id.scrollView)


        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.setScaleEnabled(true)
        lineChart.setMarker(CustomMarkerView(requireContext(), R.layout.marker_view))

        lineChartDistance.setTouchEnabled(true)
        lineChartDistance.setPinchZoom(true)
        lineChartDistance.setScaleEnabled(true)
        lineChartDistance.setMarker(CustomMarkerView(requireContext(), R.layout.marker_view))

        fabFilterYear = view.findViewById(R.id.fabFilterYear)

        tripViewModel.completedTrips.observe(viewLifecycleOwner) {
            setupYearFilter()
            updateViews()
            updateChart()
            updateDistanceChartAndForecast()
        }

        fabFilterYear.setOnClickListener {
            showYearFilterDialog()
        }

        fabObjectiveWarning = view.findViewById(R.id.fabObjectiveWarning)
        fabObjectiveWarning.setOnClickListener {
            val prefs = requireContext().getSharedPreferences("settings", 0)
            val tripsGoal = prefs.getInt("monthlyTripsGoal", 0)
            val distanceGoal = prefs.getInt("monthlyDistanceGoal", 0)
            val tripsText = "Predicted trips: $lastPredictedCount / Goal: $tripsGoal"
            val distanceText = "Predicted distance: ${"%.0f".format(lastPredictedDistance)} m / Goal: $distanceGoal m"
            AlertDialog.Builder(requireContext())
                .setTitle("You're struggling to meet your goals!")
                .setMessage("$tripsText\n$distanceText")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun setupYearFilter() {
        val trips = tripViewModel.completedTrips.value ?: emptyList()
        val years = trips.map {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.startTimestamp
            cal.get(Calendar.YEAR)
        }.distinct().sorted()
        yearOptions = listOf("All") + years.map { it.toString() }
        if (selectedYear == null) selectedYear = null // default "All"
    }

    private fun filterTripsByYear(trips: List<Trip>): List<Trip> {
        return selectedYear?.let { year ->
            trips.filter {
                val cal = Calendar.getInstance()
                cal.timeInMillis = it.startTimestamp
                cal.get(Calendar.YEAR) == year
            }
        } ?: trips
    }

    private fun showYearFilterDialog() {
        val checkedItem = yearOptions.indexOf(selectedYear?.toString() ?: "All")
        AlertDialog.Builder(requireContext())
            .setTitle("Filter by year")
            .setSingleChoiceItems(yearOptions.toTypedArray(), checkedItem) { dialog, which ->
                val selected = yearOptions[which]
                selectedYear = selected.toIntOrNull()
                updateViews()
                updateChart()
                updateDistanceChartAndForecast()
                Toast.makeText(
                    this.context,
                    if (selectedYear == null)
                        getString(R.string.filter_all)
                    else
                        getString(R.string.filter_by_year1, selectedYear.toString()),
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateViews() {
        val trips = filterTripsByYear(tripViewModel.completedTrips.value ?: emptyList())
        val grouped = PredictionUtils.groupTripsByMonth(trips)
        val predictedCount = PredictionUtils.predictNextMonthTripCount(grouped)
        lastPredictedCount = predictedCount
        val recommendations = PredictionUtils.generateRecommendations(
            requireContext(),
            trips
        )

        tvForecast.text = getString(R.string.predicted_number_of_trips, predictedCount.toString())

        tvRecommendations.text = recommendations.joinToString("\n") { "- $it" }

        val predictedDistance = PredictionUtils.predictNextMonthDistance(
            PredictionUtils.totalDistanceByMonth(trips)
        )
        lastPredictedDistance = predictedDistance

        if (trips.isEmpty()) {
            scrollView.visibility = View.GONE
            fabFilterYear.visibility = View.GONE
            noTripsLayout.visibility = View.VISIBLE
        } else {
            scrollView.visibility = View.VISIBLE
            fabFilterYear.visibility = View.VISIBLE
            noTripsLayout.visibility = View.GONE
        }

        checkObjectives(predictedCount, predictedDistance)

    }

    private fun updateChart() {
        val trips = filterTripsByYear(tripViewModel.completedTrips.value ?: emptyList())
        val grouped = PredictionUtils.groupTripsByMonth(trips)

        val entries = grouped.mapIndexed { index, pair ->
            Entry(index.toFloat(), pair.second.toFloat())
        }

        val predictedCount = PredictionUtils.predictNextMonthTripCount(grouped)
        val nextMonthIndex = grouped.size
        val predictedEntry = Entry(nextMonthIndex.toFloat(), predictedCount.toFloat())

        // Moving average (window size of 3 months)
        val movingAvg = PredictionUtils.adaptiveMovingAverage(grouped.map { it.second }, 3)
        val movingAvgEntries = movingAvg.mapIndexed { index, value ->
            Entry(index.toFloat(), value)
        }

        val dataSet = LineDataSet(entries, "")
        dataSet.color = android.graphics.Color.BLUE
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(true)

        val movingAvgSet = LineDataSet(movingAvgEntries, "")
        movingAvgSet.color = android.graphics.Color.RED
        movingAvgSet.setDrawCircles(false)
        movingAvgSet.setDrawValues(false)
        movingAvgSet.lineWidth = 2f

        // DataSet for prediction
        val predictionSet = LineDataSet(listOf(predictedEntry), "")
        predictionSet.color = android.graphics.Color.GREEN
        predictionSet.setDrawCircles(true)
        predictionSet.setDrawValues(true)
        predictionSet.circleRadius = 7f
        predictionSet.setCircleColor(android.graphics.Color.GREEN)
        predictionSet.lineWidth = 0f // No line for prediction

        val legend = lineChart.legend
        // remove last entry from legend
        legend.setCustom(
            listOf(
                LegendEntry(getString(R.string.legend_monthly_trips), Legend.LegendForm.SQUARE, 10f, 2f, null, Color.BLUE),
                LegendEntry(getString(R.string.legend_moving_avg), Legend.LegendForm.SQUARE, 10f, 2f, null, android.graphics.Color.RED),
                LegendEntry(getString(R.string.legend_prediction), Legend.LegendForm.SQUARE, 10f, 2f, null, android.graphics.Color.GREEN)
            )
        )

        if (entries.isNotEmpty()) {
            val lastEntry = entries.last()
            val connectingSet = LineDataSet(listOf(lastEntry, predictedEntry), "")
            connectingSet.color = android.graphics.Color.GREEN
            connectingSet.setDrawCircles(false)
            connectingSet.setDrawValues(false)
            connectingSet.enableDashedLine(10f, 10f, 0f)
            connectingSet.lineWidth = 2f

            val lineData = LineData(dataSet, movingAvgSet, predictionSet, connectingSet)
            lineChart.data = lineData
        } else {
            val lineData = LineData(dataSet, movingAvgSet, predictionSet)
            lineChart.data = lineData
        }


        lineChart.description.isEnabled = false

        val months = grouped.map { monthIndexToString(it.first) } +
                listOf("Next") // Label for the prediction
        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(months)
        lineChart.xAxis.granularity = 1f
        lineChart.xAxis.labelRotationAngle = -45f
        lineChart.setExtraTopOffset(24f)

        lineChart.invalidate()
    }

    // Convert monthIndex to "MM/yyyy"
    private fun monthIndexToString(monthIndex: Int): String {
        val year = monthIndex / 12
        val month = (monthIndex % 12) + 1
        return "%02d/%d".format(month, year)
    }

    private fun updateDistanceChartAndForecast() {
        val trips = filterTripsByYear(tripViewModel.completedTrips.value ?: emptyList())
        val grouped = PredictionUtils.totalDistanceByMonth(trips)

        val entries = grouped.mapIndexed { index, pair ->
            Entry(index.toFloat(), pair.second.toFloat())
        }

        val predictedDistance = PredictionUtils.predictNextMonthDistance(grouped)
        val nextMonthIndex = grouped.size
        val predictedEntry = Entry(nextMonthIndex.toFloat(), predictedDistance.toFloat())

        val movingAvg = PredictionUtils.adaptiveMovingAverage(grouped.map { it.second.toInt() }, 3)
        val movingAvgEntries = movingAvg.mapIndexed { index, value ->
            Entry(index.toFloat(), value)
        }

        val dataSet = LineDataSet(entries, "")
        dataSet.color = android.graphics.Color.BLUE
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(true)

        val movingAvgSet = LineDataSet(movingAvgEntries, "")
        movingAvgSet.color = android.graphics.Color.RED
        movingAvgSet.setDrawCircles(false)
        movingAvgSet.setDrawValues(false)
        movingAvgSet.lineWidth = 2f

        // DataSet for prediction
        val predictionSet = LineDataSet(listOf(predictedEntry), "")
        predictionSet.color = android.graphics.Color.GREEN
        predictionSet.setDrawCircles(true)
        predictionSet.setDrawValues(true)
        predictionSet.circleRadius = 7f
        predictionSet.setCircleColor(android.graphics.Color.GREEN)
        predictionSet.lineWidth = 0f // No line for prediction

        val legend = lineChartDistance.legend
        // remove last entry from legend
        legend.setCustom(
            listOf(
                LegendEntry(getString(R.string.legend_monthly_distance), Legend.LegendForm.SQUARE, 10f, 2f, null, Color.BLUE),
                LegendEntry(getString(R.string.legend_moving_avg), Legend.LegendForm.SQUARE, 10f, 2f, null, android.graphics.Color.RED),
                LegendEntry(getString(R.string.legend_prediction), Legend.LegendForm.SQUARE, 10f, 2f, null, android.graphics.Color.GREEN)
            )
        )

        if (entries.isNotEmpty()) {
            val lastEntry = entries.last()
            val connectingSet = LineDataSet(listOf(lastEntry, predictedEntry), "")
            connectingSet.color = android.graphics.Color.GREEN
            connectingSet.setDrawCircles(false)
            connectingSet.setDrawValues(false)
            connectingSet.enableDashedLine(10f, 10f, 0f)
            connectingSet.lineWidth = 2f

            val lineData = LineData(dataSet, movingAvgSet, predictionSet, connectingSet)
            lineChartDistance.data = lineData
        } else {
            val lineData = LineData(dataSet, movingAvgSet, predictionSet)
            lineChartDistance.data = lineData
        }

        lineChartDistance.description.isEnabled = false

        val months = grouped.map { monthIndexToString(it.first) } + listOf("Next")
        lineChartDistance.xAxis.valueFormatter = IndexAxisValueFormatter(months)
        lineChartDistance.xAxis.granularity = 1f
        lineChartDistance.xAxis.labelRotationAngle = -45f
        lineChartDistance.setExtraTopOffset(24f)

        lineChartDistance.invalidate()

        tvDistanceForecast.text = getString(R.string.predicted_distance, predictNextMonthDistance(grouped))
        }


    private fun checkObjectives(predictedTrips: Int, predictedDistance: Double) {
        val prefs = requireContext().getSharedPreferences("settings", 0)
        val tripsGoal = prefs.getInt("monthlyTripsGoal", 0)
        val distanceGoal = prefs.getInt("monthlyDistanceGoal", 0)
        val show = (tripsGoal > 0 && predictedTrips < tripsGoal) ||
                (distanceGoal > 0 && predictedDistance < distanceGoal)
        fabObjectiveWarning.visibility = if (show) View.VISIBLE else View.GONE
    }


}