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
import com.example.travelcompanion.ui.analysis_prediction.prediction.PredictionUtils.predictNextMonthDistanceText
import com.example.travelcompanion.ui.home.plan.CompletedTripViewModelFactory
import com.example.travelcompanion.ui.home.plan.TripViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlin.math.pow


class PredictionFragment : Fragment() {
    private lateinit var tvTotalTrips : TextView
    private lateinit var tvAvgDistance : TextView
    private lateinit var tvAvgDuration : TextView
    private lateinit var tvTopDestination : TextView
    private lateinit var tvMonthlyVariance : TextView

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

        tvTotalTrips = view.findViewById(R.id.tvTotalTrips)
        tvAvgDistance = view.findViewById(R.id.tvAvgDistance)
        tvAvgDuration = view.findViewById(R.id.tvAvgDuration)
        tvTopDestination = view.findViewById(R.id.tvTopDestination)
        tvMonthlyVariance = view.findViewById(R.id.tvMonthlyVariance)
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

        val summary = PredictionUtils.getTripSummary(trips)
        tvTotalTrips.text = "Total Trips: ${summary.totalTrips}"
        tvAvgDistance.text = "Avg Distance: %.1f m".format(summary.avgDistance)
        tvAvgDuration.text = "Avg Duration: %.1f s".format(summary.avgDuration)
        tvTopDestination.text = "Top Destination: ${summary.topDestination}"
        tvMonthlyVariance.text = "Monthly Variance: %.1f".format(summary.monthlyVariance)

        tvForecast.text = "Predicted number of trips for next month: $predictedCount"

        tvRecommendations.text = recommendations.joinToString("\n") { "- $it" }
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
        lineChart.getDescription().setEnabled(false);

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
        lineChartDistance.getDescription().setEnabled(false);

        val months = grouped.map { monthIndexToString(it.first) }
        lineChartDistance.xAxis.valueFormatter = IndexAxisValueFormatter(months)
        lineChartDistance.xAxis.granularity = 1f
        lineChartDistance.xAxis.labelRotationAngle = -45f
        lineChartDistance.setExtraTopOffset(24f)

        lineChartDistance.invalidate()

        val predictedDistanceText = predictNextMonthDistanceText(grouped)
        tvDistanceForecast.text = predictedDistanceText
    }


}