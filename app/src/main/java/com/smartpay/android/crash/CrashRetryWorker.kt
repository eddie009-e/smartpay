package com.smartpay.android.crash

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker to retry sending pending crash reports
 * This ensures crash reports are eventually sent even if the app was offline
 */
class CrashRetryWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Get error reporter instance
            val errorReporter = ErrorReporter(applicationContext)
            
            // Retry sending pending crashes
            errorReporter.retryPendingCrashes()
            
            Result.success()
        } catch (e: Exception) {
            // Retry later if something went wrong
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "CrashRetryWork"
        private const val TAG = "CrashRetryWorker"

        /**
         * Schedule periodic work to retry pending crashes
         */
        fun schedulePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val retryWork = PeriodicWorkRequestBuilder<CrashRetryWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
                flexTimeInterval = 5,
                flexTimeIntervalUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                retryWork
            )
        }

        /**
         * Schedule one-time work to retry pending crashes immediately
         */
        fun scheduleOneTimeWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val retryWork = OneTimeWorkRequestBuilder<CrashRetryWorker>()
                .setConstraints(constraints)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "$WORK_NAME-OneTime",
                ExistingWorkPolicy.REPLACE,
                retryWork
            )
        }

        /**
         * Cancel all pending retry work
         */
        fun cancelWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
        }
    }
}