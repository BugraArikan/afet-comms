package com.example.afetcomms.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.afetcomms.AfetCommsApp
import com.example.afetcomms.R
import com.example.afetcomms.ui.main.MainActivity

class BleRelayService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        val familyId = intent?.getStringExtra(EXTRA_FAMILY_ID)
        val senderId = intent?.getStringExtra(EXTRA_SENDER_ID)
        if (!familyId.isNullOrBlank() && !senderId.isNullOrBlank()) {
            (application as AfetCommsApp).startTransports(familyId, senderId)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        (application as AfetCommsApp).stopTransports()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_relay_title))
            .setContentText(getString(R.string.notification_relay_body))
            .setSmallIcon(R.drawable.ic_disaster_network)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "afetcomms_relay"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_FAMILY_ID = "extra_family_id"
        const val EXTRA_SENDER_ID = "extra_sender_id"

        fun start(context: Context, familyId: String, senderId: String) {
            val intent = Intent(context, BleRelayService::class.java).apply {
                putExtra(EXTRA_FAMILY_ID, familyId)
                putExtra(EXTRA_SENDER_ID, senderId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BleRelayService::class.java))
        }
    }
}
