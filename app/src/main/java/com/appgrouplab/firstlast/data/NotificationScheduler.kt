package com.appgrouplab.firstlast.data

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.appgrouplab.firstlast.model.Game
import java.time.Instant
import java.util.concurrent.TimeUnit

class NotificationScheduler(private val context: Context) {

    companion object {
        private const val TAG = "match_notification"
        private const val ONE_HOUR_MS = 3_600_000L
    }

    fun scheduleAll(games: List<Game>) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)

        games.forEach { game ->
            try {
                val matchInstant = try {
                    Instant.parse(game.dateTimeIso)
                } catch (_: Exception) {
                    java.time.LocalDateTime.parse(game.dateTimeIso)
                        .toInstant(java.time.ZoneOffset.UTC)
                }

                val delayMs = matchInstant.toEpochMilli() - System.currentTimeMillis() - ONE_HOUR_MS
                if (delayMs <= 0) return@forEach

                val request = OneTimeWorkRequestBuilder<MatchNotificationWorker>()
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .setInputData(workDataOf(
                        MatchNotificationWorker.KEY_HOME   to game.home.name,
                        MatchNotificationWorker.KEY_AWAY   to game.away.name,
                        MatchNotificationWorker.KEY_LEAGUE to game.league.name
                    ))
                    .addTag(TAG)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    "${game.home.key}_${game.away.key}",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            } catch (_: Exception) { }
        }
    }
}
