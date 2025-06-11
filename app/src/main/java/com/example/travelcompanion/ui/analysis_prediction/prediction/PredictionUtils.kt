package com.example.travelcompanion.ui.analysis_prediction.prediction

import com.example.travelcompanion.db.trip.Trip
import java.util.Calendar
import kotlin.math.pow

data class TripSummary(
    val totalTrips: Int,
    val avgDistance: Double,
    val avgDuration: Double,
    val topDestination: String,
    val monthlyVariance: Double
)

object PredictionUtils {

    fun analyzeTrips(trips: List<Trip>): TripSummary {
        val totalTrips = trips.size
        val avgDistance = trips.map { it.distance }.average()
        val avgDuration = trips.map { it.duration }.average()

        val topDest = trips.groupBy { it.destination }
            .maxByOrNull { it.value.size }?.key ?: "None"

        val monthlyVar = monthlyVariance(trips)

        return TripSummary(totalTrips, avgDistance, avgDuration, topDest, monthlyVar)
    }

    fun groupTripsByMonth(trips: List<Trip>): List<Pair<Int, Int>> {
        val monthly = mutableMapOf<Int, Int>() // Key: monthIndex, Value: count

        trips.forEach {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.startTimestamp
            val month = cal.get(Calendar.MONTH)
            val year = cal.get(Calendar.YEAR)
            val monthIndex = year * 12 + month // Flatten to single index
            monthly[monthIndex] = monthly.getOrDefault(monthIndex, 0) + 1
        }

        return monthly.toSortedMap().entries.map { it.key to it.value }
    }

    fun totalDistanceByMonth(trips: List<Trip>): List<Pair<Int, Double>> {
        val monthly = mutableMapOf<Int, Double>() // Key: monthIndex, Value: total distance

        trips.forEach {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.startTimestamp
            val month = cal.get(Calendar.MONTH)
            val year = cal.get(Calendar.YEAR)
            val monthIndex = year * 12 + month
            monthly[monthIndex] = monthly.getOrDefault(monthIndex, 0.0) + it.distance
        }

        return monthly.toSortedMap().entries.map { it.key to it.value }
    }

    fun predictNextMonthTripCount(monthlyData: List<Pair<Int, Int>>): Int {
        if (monthlyData.size < 2) return monthlyData.lastOrNull()?.second ?: 0

        val x = monthlyData.map { it.first.toDouble() }
        val y = monthlyData.map { it.second.toDouble() }
        val n = x.size

        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y).sumOf { it.first * it.second }
        val sumX2 = x.sumOf { it * it }

        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX.pow(2))
        val intercept = (sumY - slope * sumX) / n

        val nextMonth = x.maxOrNull()?.plus(1) ?: 0.0
        return (slope * nextMonth + intercept).toInt().coerceAtLeast(0)
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

    fun predictNextMonthDistanceText(monthlyData: List<Pair<Int, Double>>): String {
        val predictedDistance = predictNextMonthDistance(monthlyData)
        return "Predicted distance for next month: ${Math.round(predictedDistance * 10) / 10.0} m. Current average is ${Math.round(monthlyData.map { it.second }.average() * 10) / 10.0} m."
    }

    fun generateRecommendations(trips: List<Trip>): List<String> {
        val messages = mutableListOf<String>()

        val byMonth = groupTripsByMonth(trips)
        val avgTrips = Math.round(byMonth.map { it.second }.average() * 10) / 10.0
        val distinctPlaces = trips.map { it.destination }.toSet()
        val longTrips = trips.count { it.distance > 100_000 }

        val predicted = predictNextMonthTripCount(byMonth)

        if (avgTrips < 2) {
            messages.add("Try to go on at least 2 trips per month to build a good habit!")
        } else {
            messages.add("Great consistency! You are averaging $avgTrips trips per month.")
        }

        if (distinctPlaces.size < 3) {
            messages.add("Consider exploring new destinations to diversify your travel experiences!")
        }

        if (longTrips > 0) {
            messages.add("You are going on long trips. Keep it up!")
        }
        else {
            messages.add("Consider planning a long trip to explore further destinations!")
        }

        // If predicted trips are below average, suggest new travel ideas
        if (predicted < avgTrips) {
            messages.add("A decline in travel activity is forecast. Here are some ideas for new trips: visit a nearby city, explore a natural park, or plan a weekend getaway!")
            messages.add("Try to increase the frequency of your trips to keep your motivation high.")
        } else {
            messages.add("Based on trends, aim for ~$predicted trips next month. Current average is $avgTrips trips per month.")
        }

        return messages
    }

    fun getTripSummary(trips: List<Trip>): TripSummary {
        return analyzeTrips(trips)
    }

    fun movingAverage(values: List<Int>, window: Int): List<Double> {
        return values.windowed(window) { it.average() }
    }

    fun monthlyVariance(trips: List<Trip>): Double {
        val grouped = groupTripsByMonth(trips)
        val counts = grouped.map { it.second }
        val avg = counts.average()
        return counts.map { (it - avg) * (it - avg) }.average()
    }
}