package com.example.travelcompanion.ui.analysis_prediction.prediction

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.travelcompanion.R
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.db.trip.Trip
import com.example.travelcompanion.ui.home.plan.CompletedTripViewModelFactory
import com.example.travelcompanion.ui.home.plan.TripViewModel
import com.example.travelcompanion.ui.journal.list.JournalListViewModel

class PredictionFragment : Fragment() {
    private lateinit var tvSummary: TextView
    private lateinit var tvForecast: TextView
    private lateinit var tvRecommendations: TextView
    private lateinit var tripViewModel: TripViewModel

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

        tripViewModel.completedTrips.observe(viewLifecycleOwner) {
            updatePredictions()
        }
    }

    private fun updatePredictions() {
        val trips = tripViewModel.completedTrips.value ?: emptyList()
        val summary = PredictionUtils.analyzeTrips(trips)
        val grouped = PredictionUtils.groupTripsByMonth(trips)
        val predictedCount = PredictionUtils.predictNextMonthTripCount(grouped)
        val recommendations = PredictionUtils.generateRecommendations(trips)

        tvSummary.text = "Total Trips: ${summary.totalTrips}, " +
                "Avg Distance: ${summary.avgDistance} km, " +
                "Avg Duration: ${summary.avgDuration} mins, " +
                "Top Destination: ${summary.topDestination}"

        tvForecast.text = "Predicted Trips Next Month: $predictedCount"

        tvRecommendations.text = recommendations.joinToString("\n") { "- $it" }
    }
}