package com.example.travelcompanion.ui.analysis_prediction.prediction

import android.content.Context
import android.provider.Settings.Global.getString
import com.example.travelcompanion.R
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

    fun predictNextMonthDistance(monthlyData: List<Pair<Int, Double>>): Double {
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

    fun generateRecommendations(context: Context, trips: List<Trip>): List<String> {
        val messages = mutableListOf<String>()

        val byMonth = groupTripsByMonth(trips)
        val avgTrips = Math.round(byMonth.map { it.second }.average() * 10) / 10.0
        val distinctPlaces = trips.map { it.destination }.toSet()
        val longTrips = trips.count { it.distance > 100_000 }

        val predicted = predictNextMonthTripCount(byMonth)

        if (avgTrips < 2) {
            messages.add(context.getString(R.string.recommendation_min_trips))
        } else {
            messages.add(context.getString(R.string.recommendation_consistency, avgTrips))
        }

        if (distinctPlaces.size < 3) {
            messages.add(context.getString(R.string.recommendation_explore))
        }

        if (longTrips > 0) {
            messages.add(context.getString(R.string.recommendation_long_trips))
        } else {
            messages.add(context.getString(R.string.recommendation_plan_long_trip))
        }

        if (predicted < avgTrips) {
            messages.add(context.getString(R.string.recommendation_decline))
            messages.add(context.getString(R.string.recommendation_increase_frequency))
        } else {
            messages.add(context.getString(R.string.recommendation_trend, predicted, avgTrips))
        }

        return messages
    }

    fun getTripSummary(trips: List<Trip>): TripSummary {
        return analyzeTrips(trips)
    }

    fun movingAverage(values: List<Int>, window: Int): List<Double> {
        return values.windowed(window) { it.average() }
    }

    fun adaptiveMovingAverage(values: List<Int>, window: Int): List<Float> {
        return values.indices.map { i ->
            val start = maxOf(0, i - window + 1)
            val sublist = values.subList(start, i + 1)
            sublist.average().toFloat()
        }
    }

    fun monthlyVariance(trips: List<Trip>): Double {
        val grouped = groupTripsByMonth(trips)
        val counts = grouped.map { it.second }
        val avg = counts.average()
        return counts.map { (it - avg) * (it - avg) }.average()
    }
}