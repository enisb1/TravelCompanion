package com.example.travelcompanion.ui.analysis_prediction.prediction

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.R
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.ui.home.plan.CompletedTripViewModelFactory
import com.example.travelcompanion.ui.home.plan.TripViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlin.math.pow


class PredictionFragment : Fragment() {
    private lateinit var tvSummary: TextView
    private lateinit var tvForecast: TextView
    private lateinit var tvRecommendations: TextView
    private lateinit var tripViewModel: TripViewModel
    private lateinit var lineChart: LineChart
    private lateinit var lineChartDistance: LineChart
    private lateinit var tvDistanceForecast: TextView


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

        tvSummary = view.findViewById(R.id.tvPredictionSummary)
        tvForecast = view.findViewById(R.id.tvPredictionForecast)
        tvRecommendations = view.findViewById(R.id.tvPredictionRecommend)
        lineChart = view.findViewById(R.id.lineChart)
        lineChartDistance = view.findViewById(R.id.lineChartDistance)
        tvDistanceForecast = view.findViewById(R.id.tvPredictionDistanceForecast)


        tripViewModel.completedTrips.observe(viewLifecycleOwner) {
            updateViews()
            updateChart()
            updateDistanceChartAndForecast()
        }
    }

    private fun updateViews() {
        val trips = tripViewModel.completedTrips.value ?: emptyList()
        val grouped = PredictionUtils.groupTripsByMonth(trips)
        val predictedCount = PredictionUtils.predictNextMonthTripCount(grouped)
        val recommendations = PredictionUtils.generateRecommendations(trips)

        tvSummary.text = PredictionUtils.getTripSummary(trips)

        tvForecast.text = "Predicted number of trips for next month: $predictedCount"

        tvRecommendations.text = recommendations.joinToString("\n") { "- $it" }

        val variance = PredictionUtils.monthlyVariance(trips)
    }

    private fun updateChart() {
        val trips = tripViewModel.completedTrips.value ?: emptyList()
        val grouped = PredictionUtils.groupTripsByMonth(trips)

        val entries = grouped.mapIndexed { index, pair ->
            Entry(index.toFloat(), pair.second.toFloat())
        }

        // Moving average (window size of 3 months)
        val movingAvg = PredictionUtils.movingAverage(grouped.map { it.second }, 3)
        val movingAvgEntries = movingAvg.mapIndexed { index, value ->
            Entry((index + 2).toFloat(), value.toFloat())
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

        val lineData = LineData(dataSet, movingAvgSet)
        lineChart.data = lineData

        val months = grouped.map { monthIndexToString(it.first) }
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
        val trips = tripViewModel.completedTrips.value ?: emptyList()
        val grouped = PredictionUtils.totalDistanceByMonth(trips)

        val entries = grouped.mapIndexed { index, pair ->
            Entry(index.toFloat(), pair.second.toFloat())
        }

        val movingAvg = PredictionUtils.movingAverage(grouped.map { it.second.toInt() }, 3)
        val movingAvgEntries = movingAvg.mapIndexed { index, value ->
            Entry((index + 2).toFloat(), value.toFloat())
        }

        val dataSet = LineDataSet(entries, "Monthly distance (m)")
        dataSet.color = android.graphics.Color.GREEN
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(true)

        val movingAvgSet = LineDataSet(movingAvgEntries, "Average monthly distance (m)")
        movingAvgSet.color = android.graphics.Color.MAGENTA
        movingAvgSet.setDrawCircles(false)
        movingAvgSet.setDrawValues(false)
        movingAvgSet.lineWidth = 2f

        val lineData = LineData(dataSet, movingAvgSet)
        lineChartDistance.data = lineData

        val months = grouped.map { monthIndexToString(it.first) }
        lineChartDistance.xAxis.valueFormatter = IndexAxisValueFormatter(months)
        lineChartDistance.xAxis.granularity = 1f
        lineChartDistance.xAxis.labelRotationAngle = -45f
        lineChartDistance.setExtraTopOffset(24f)

        lineChartDistance.invalidate()

        val predictedDistance = predictNextMonthDistance(grouped)
        tvDistanceForecast.text = "Predicted distance for next month: %.1f m".format(predictedDistance)
    }

    private fun predictNextMonthDistance(monthlyData: List<Pair<Int, Double>>): Double {
        if (monthlyData.size < 2) return monthlyData.lastOrNull()?.second ?: 0.0

        val x = monthlyData.map { it.first.toDouble() }
        val y = monthlyData.map { it.second }
        val n = x.size

        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y).sumOf { it.first * it.second }
        val sumX2 = x.sumOf { it * it }

        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX.pow(2))
        val intercept = (sumY - slope * sumX) / n

        val nextMonth = x.maxOrNull()?.plus(1) ?: 0.0
        return (slope * nextMonth + intercept).coerceAtLeast(0.0)
    }
}