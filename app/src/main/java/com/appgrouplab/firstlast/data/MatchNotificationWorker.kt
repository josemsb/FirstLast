package com.appgrouplab.firstlast.data

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.appgrouplab.firstlast.R

class MatchNotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        const val CHANNEL_ID = "match_notifications"
        const val KEY_HOME    = "home"
        const val KEY_AWAY    = "away"
        const val KEY_LEAGUE  = "league"
    }

    override fun doWork(): Result {
        val home   = inputData.getString(KEY_HOME)   ?: return Result.failure()
        val away   = inputData.getString(KEY_AWAY)   ?: return Result.failure()
        val league = inputData.getString(KEY_LEAGUE) ?: return Result.failure()

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚽ Partido en 1 hora")
            .setContentText("$home vs $away · $league")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$home vs $away\n$league"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)

        return Result.success()
    }
}
