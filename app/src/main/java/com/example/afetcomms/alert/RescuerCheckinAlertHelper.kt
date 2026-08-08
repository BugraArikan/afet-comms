package com.example.afetcomms.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.afetcomms.R
import com.example.afetcomms.data.local.MessageEntity
import com.example.afetcomms.ui.messages.MessagesActivity
import com.example.afetcomms.util.HelpCallFormatter

object RescuerCheckinAlertHelper {

    const val CHANNEL_RESCUER_CHECKIN = "afetcomms_rescuer_checkin"
    private const val NOTIFICATION_ID = 2003

    fun showIncoming(context: Context, message: MessageEntity) {
        createChannel(context)
        val messagesIntent = Intent(context, MessagesActivity::class.java)
        val pending = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            messagesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = context.getString(R.string.rescuer_checkin_notification_title)
        val body = context.getString(
            R.string.rescuer_checkin_notification_body,
            HelpCallFormatter.title(message),
            message.content
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_RESCUER_CHECKIN)
            .setSmallIcon(R.drawable.ic_disaster_network)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setColor(0xFF16A34A.toInt())
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_RESCUER_CHECKIN,
            context.getString(R.string.rescuer_checkin_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.rescuer_checkin_channel_desc)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }
}
