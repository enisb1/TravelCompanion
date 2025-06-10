package com.example.travelcompanion.ui.analysis_prediction.prediction

import com.example.travelcompanion.db.trip.Trip
import java.util.Calendar
import kotlin.math.pow

data class TripSummary(
    val totalTrips: Int,
    val avgDistance: Double,
    val avgDuration: Double,
    val topDestination: String
)

object PredictionUtils {

    fun analyzeTrips(trips: List<Trip>): TripSummary {
        val totalTrips = trips.size
        val avgDistance = trips.map { it.distance }.average()
        val avgDuration = trips.map { it.duration }.average()

        val topDest = trips.groupBy { it.destination }
            .maxByOrNull { it.value.size }?.key ?: "None"

        return TripSummary(totalTrips, avgDistance, avgDuration, topDest)
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

    fun generateRecommendations(trips: List<Trip>): List<String> {
        val messages = mutableListOf<String>()

        val byMonth = groupTripsByMonth(trips)
        val avgTrips = byMonth.map { it.second }.average()
        val distinctPlaces = trips.map { it.destination }.toSet()
        val longTrips = trips.count { it.distance > 100_000 }

        if (avgTrips < 2) {
            messages.add("Try planning at least 2 trips per month to build a habit.")
        } else {
            messages.add("Great travel consistency! Keep it up.")
        }

        if (distinctPlaces.size < 3) {
            messages.add("Consider exploring new destinations.")
        }

        if (longTrips > 0) {
            messages.add("You're taking some long-distance trips. Amazing!")
        }

        val predicted = predictNextMonthTripCount(byMonth)
        messages.add("Based on trends, aim for ~$predicted trips next month.")

        return messages
    }

    fun movingAverage(values: List<Int>, window: Int): List<Double> {
        return values.windowed(window) { it.average() }
    }




}