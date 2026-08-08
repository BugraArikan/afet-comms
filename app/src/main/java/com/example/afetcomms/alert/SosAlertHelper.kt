package com.example.afetcomms.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.afetcomms.R
import com.example.afetcomms.data.local.MessageEntity
import com.example.afetcomms.ui.alert.SosAlertActivity
import com.example.afetcomms.ui.messages.MessagesActivity
import com.example.afetcomms.util.AppPreferences
import com.example.afetcomms.util.HelpCallFormatter

object SosAlertHelper {

    const val CHANNEL_SOS = "afetcomms_sos"
    private const val NOTIFICATION_INCOMING = 2001
    private const val NOTIFICATION_SENT = 2002
    private const val RINGTONE_MAX_MS = 8_000L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeRingtone: Ringtone? = null
    private var activeVibrator: Vibrator? = null

    fun showIncoming(context: Context, message: MessageEntity) {
        if (!AppPreferences.sosAlertsEnabled(context)) return
        vibrateOnce(context)
        playAlarmSound(context)
        showNotification(
            context,
            NOTIFICATION_INCOMING,
            context.getString(R.string.sos_notification_incoming_title),
            context.getString(
                R.string.sos_notification_incoming_body,
                HelpCallFormatter.title(message),
                message.content
            ),
            fullScreen = true,
            message = message,
            incoming = true
        )
        launchAlertScreen(context, message, incoming = true)
    }

    fun showSent(context: Context, message: MessageEntity) {
        if (!AppPreferences.sosAlertsEnabled(context)) return
        vibrateOnce(context)
        showNotification(
            context,
            NOTIFICATION_SENT,
            context.getString(R.string.sos_notification_sent_title),
            context.getString(R.string.sos_notification_sent_body),
            fullScreen = false,
            message = message,
            incoming = false
        )
        launchAlertScreen(context, message, incoming = false)
    }

    /** Tam ekran uyarı kapatıldığında veya yeni uyarı öncesinde çağrılır. */
    fun stopAlerts(context: Context) {
        stopRingtoneOnly()
        try {
            activeVibrator?.cancel()
        } catch (_: Exception) {
        }
        activeVibrator = null
    }

    private fun launchAlertScreen(context: Context, message: MessageEntity, incoming: Boolean) {
        val intent = Intent(context, SosAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(SosAlertActivity.EXTRA_SENDER, message.senderId)
            putExtra(SosAlertActivity.EXTRA_CONTENT, message.content)
            putExtra(SosAlertActivity.EXTRA_INCOMING, incoming)
        }
        context.startActivity(intent)
    }

    private fun showNotification(
        context: Context,
        id: Int,
        title: String,
        body: String,
        fullScreen: Boolean,
        message: MessageEntity,
        incoming: Boolean
    ) {
        createChannel(context)
        val alertIntent = Intent(context, SosAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(SosAlertActivity.EXTRA_SENDER, message.senderId)
            putExtra(SosAlertActivity.EXTRA_CONTENT, message.content)
            putExtra(SosAlertActivity.EXTRA_INCOMING, incoming)
        }
        val fullScreenPending = PendingIntent.getActivity(
            context, id, alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val messagesIntent = Intent(context, MessagesActivity::class.java)
        val messagesPending = PendingIntent.getActivity(
            context, id + 100, messagesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_SOS)
            .setSmallIcon(R.drawable.ic_disaster_network)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(fullScreenPending)
            .addAction(0, context.getString(R.string.sos_open_messages), messagesPending)
            .setColor(0xFFDC2626.toInt())

        if (fullScreen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setFullScreenIntent(fullScreenPending, true)
        }

        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_SOS,
            context.getString(R.string.sos_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.sos_channel_desc)
            enableVibration(true)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun vibrateOnce(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        activeVibrator?.cancel()
        activeVibrator = vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(600, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(600)
        }
    }

    private fun stopRingtoneOnly() {
        mainHandler.removeCallbacksAndMessages(null)
        try {
            activeRingtone?.stop()
        } catch (_: Exception) {
        }
        activeRingtone = null
    }

    private fun playAlarmSound(context: Context) {
        if (!AppPreferences.sosSoundEnabled(context)) return
        stopRingtoneOnly()
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri) ?: return
            activeRingtone = ringtone
            ringtone.play()
            mainHandler.postDelayed({
                try {
                    activeRingtone?.stop()
                } catch (_: Exception) {
                }
                activeRingtone = null
            }, RINGTONE_MAX_MS)
        } catch (_: Exception) {
        }
    }
}
