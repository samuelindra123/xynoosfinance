package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.domain.IdrFormatter

object ReminderNotificationHelper {
    const val CHANNEL_ID = "xynoos_finance_reminders"

    fun ensureChannelCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pengingat & Ringkasan XynoosFinance",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Pengingat pencatatan transaksi harian, peringatan anggaran, dan pencapaian target tabungan."
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun sendPreviewReminderNotification(
        context: Context,
        title: String,
        availableCashIdr: Long,
        isPrivacyMode: Boolean
    ): Boolean {
        ensureChannelCreated(context)
        if (!hasNotificationPermission(context)) return false

        val cashText = IdrFormatter.format(availableCashIdr, isPrivacyHidden = isPrivacyMode)
        val body = if (isPrivacyMode) {
            "Jangan lupa catat transaksi hari ini. Mode privasi aktif (nominal disembunyikan)."
        } else {
            "Jangan lupa catat transaksi hari ini. Kas tersedia saat ini: $cashText."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(1001, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
