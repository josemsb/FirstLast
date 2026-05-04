package com.appgrouplab.firstlast

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.appgrouplab.firstlast.data.MatchNotificationWorker
import com.google.android.gms.ads.MobileAds

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            MatchNotificationWorker.CHANNEL_ID,
            "Partidos próximos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Aviso 1 hora antes de cada partido"
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}
