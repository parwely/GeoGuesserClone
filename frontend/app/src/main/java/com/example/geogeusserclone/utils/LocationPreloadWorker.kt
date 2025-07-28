package com.example.geogeusserclone.utils

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.geogeusserclone.data.repositories.LocationCacheRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Background Worker für Performance-kritische Aufgaben
 */
@HiltWorker
class LocationPreloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val locationCacheRepository: LocationCacheRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Lade Locations im Hintergrund vor
            locationCacheRepository.preloadLocationsInBackground()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedulePeriodicPreload(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val periodicWork = PeriodicWorkRequestBuilder<LocationPreloadWorker>(
                repeatInterval = 2, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "location_preload",
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicWork
                )
        }
    }
}
