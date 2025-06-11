package com.example.travelcompanion.ui.analysis_prediction.prediction

import android.app.AlertDialog
import androidx.fragment.app.viewModels
import android.os.Bundle
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
import com.example.travelcompanion.ui.analysis_prediction.prediction.PredictionUtils.predictNextMonthDistanceText
import com.example.travelcompanion.ui.home.plan.CompletedTripViewModelFactory
import com.example.travelcompanion.ui.home.plan.TripViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.example.travelcompanion.ui.analysis_prediction.CustomMarkerView
import java.util.Calendar
import com.google.android.material.floatingactionbutton.FloatingActionButton


class PredictionFragment : Fragment() {
    /*
    private lateinit var tvTotalTrips : TextView
    private lateinit var tvAvgDistance : TextView
    private lateinit var tvAvgDuration : TextView
    private lateinit var tvTopDestination : TextView
    private lateinit var tvMonthlyVariance : TextView
    */

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

        /*
        tvTotalTrips = view.findViewById(R.id.tvTotalTrips)
        tvAvgDistance = view.findViewById(R.id.tvAvgDistance)
        tvAvgDuration = view.findViewById(R.id.tvAvgDuration)
        tvTopDestination = view.findViewById(R.id.tvTopDestination)
        tvMonthlyVariance = view.findViewById(R.id.tvMonthlyVariance)
         */
        tvForecast = view.findViewById(R.id.tvPredictionForecast)
        tvRecommendations = view.findViewById(R.id.tvPredictionRecommend)
        lineChart = view.findViewById(R.id.lineChart)
        lineChartDistance = view.findViewById(R.id.lineChartDistance)
        tvDistanceForecast = view.findViewById(R.id.tvPredictionDistanceForecast)

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
            val prefs = requireContext().getSharedPreferences("goals", 0)
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
                Toast.makeText(this.context, if (selectedYear == null) "Filter: All" else "Filter by year: $selectedYear", Toast.LENGTH_SHORT).show()
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
        val recommendations = PredictionUtils.generateRecommendations(trips)

        /*
        val summary = PredictionUtils.getTripSummary(trips)
        tvTotalTrips.text = "Total Trips: ${summary.totalTrips}"
        tvAvgDistance.text = "Avg Distance: %.1f m".format(summary.avgDistance)
        tvAvgDuration.text = "Avg Duration: %.1f s".format(summary.avgDuration)
        tvTopDestination.text = "Top Destination: ${summary.topDestination}"
        tvMonthlyVariance.text = "Monthly Variance: %.1f".format(summary.monthlyVariance)
         */

        tvForecast.text = "Predicted number of trips for next month: $predictedCount"

        tvRecommendations.text = recommendations.joinToString("\n") { "- $it" }

        val predictedDistance = PredictionUtils.predictNextMonthDistance(
            PredictionUtils.totalDistanceByMonth(trips)
        )
        lastPredictedDistance = predictedDistance

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

        val dataSet = LineDataSet(entries, "Monthly trips")
        dataSet.color = android.graphics.Color.BLUE
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(true)

        val movingAvgSet = LineDataSet(movingAvgEntries, "Moving Average (3 months)")
        movingAvgSet.color = android.graphics.Color.RED
        movingAvgSet.setDrawCircles(false)
        movingAvgSet.setDrawValues(false)
        movingAvgSet.lineWidth = 2f

        // DataSet for prediction
        val predictionSet = LineDataSet(listOf(predictedEntry), "Prediction")
        predictionSet.color = android.graphics.Color.GREEN
        predictionSet.setDrawCircles(true)
        predictionSet.setDrawValues(true)
        predictionSet.circleRadius = 7f
        predictionSet.setCircleColor(android.graphics.Color.GREEN)
        predictionSet.lineWidth = 0f // No line for prediction

        val lineData = LineData(dataSet, movingAvgSet, predictionSet)
        lineChart.data = lineData
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

        val dataSet = LineDataSet(entries, "Monthly distance (m)")
        dataSet.color = android.graphics.Color.BLUE
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(true)

        val movingAvgSet = LineDataSet(movingAvgEntries, "Average monthly distance (m)")
        movingAvgSet.color = android.graphics.Color.RED
        movingAvgSet.setDrawCircles(false)
        movingAvgSet.setDrawValues(false)
        movingAvgSet.lineWidth = 2f

        // DataSet for prediction
        val predictionSet = LineDataSet(listOf(predictedEntry), "Prediction")
        predictionSet.color = android.graphics.Color.GREEN
        predictionSet.setDrawCircles(true)
        predictionSet.setDrawValues(true)
        predictionSet.circleRadius = 7f
        predictionSet.setCircleColor(android.graphics.Color.GREEN)
        predictionSet.lineWidth = 0f // No line for prediction

        val lineData = LineData(dataSet, movingAvgSet, predictionSet)
        lineChartDistance.data = lineData
        lineChartDistance.description.isEnabled = false

        val months = grouped.map { monthIndexToString(it.first) } +
                listOf("Next")
        lineChartDistance.xAxis.valueFormatter = IndexAxisValueFormatter(months)
        lineChartDistance.xAxis.granularity = 1f
        lineChartDistance.xAxis.labelRotationAngle = -45f
        lineChartDistance.setExtraTopOffset(24f)

        lineChartDistance.invalidate()

        val predictedDistanceText = predictNextMonthDistanceText(grouped)
        tvDistanceForecast.text = predictedDistanceText
    }

    private fun checkObjectives(predictedTrips: Int, predictedDistance: Double) {
        val prefs = requireContext().getSharedPreferences("goals", 0)
        val tripsGoal = prefs.getInt("monthlyTripsGoal", 0)
        val distanceGoal = prefs.getInt("monthlyDistanceGoal", 0)
        val show = (tripsGoal > 0 && predictedTrips < tripsGoal) ||
                (distanceGoal > 0 && predictedDistance < distanceGoal)
        fabObjectiveWarning.visibility = if (show) View.VISIBLE else View.GONE
    }


}