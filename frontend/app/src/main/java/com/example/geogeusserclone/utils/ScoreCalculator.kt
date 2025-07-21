package com.example.geogeusserclone.utils

object ScoreCalculator {
    fun calculate(distanceKm: Double): Int {
        return when {
            distanceKm <= 1 -> 5000
            distanceKm <= 10 -> (5000 - (distanceKm - 1) * 400).toInt()
            distanceKm <= 100 -> (1400 - (distanceKm - 10) * 10).toInt()
            else -> maxOf(0, (500 - (distanceKm - 100) * 2).toInt())
        }
    }
}