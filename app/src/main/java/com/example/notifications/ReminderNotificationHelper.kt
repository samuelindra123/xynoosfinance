package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.BudgetAlertLevel
import com.example.domain.BudgetProgressSummary
import com.example.domain.IdrFormatter
import com.example.domain.TransactionModel
import com.example.domain.TransactionType
import java.util.Calendar

object ReminderNotificationHelper {
    const val CHANNEL_ID = "xynoos_finance_reminders"
    const val CHANNEL_BUDGET_ALERTS = "xynoos_budget_alerts"
    const val CHANNEL_ACTIVITY_ALERTS = "xynoos_activity_alerts"

    private const val DAILY_ALARM_REQUEST_CODE = 4201
    private const val NOTIF_ID_DAILY_REMINDER = 1001
    private const val NOTIF_ID_BUDGET_ALERT_BASE = 2000
    private const val NOTIF_ID_GOAL_ACHIEVED = 3001
    private const val NOTIF_ID_TX_ACTIVITY = 4001

    fun ensureChannelCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

                val reminderChannel = NotificationChannel(
                    CHANNEL_ID,
                    "Pengingat Harian XynoosFinance",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Pengingat harian pencatatan pemasukan, pengeluaran, dan cek kas."
                }

                val budgetChannel = NotificationChannel(
                    CHANNEL_BUDGET_ALERTS,
                    "Peringatan Anggaran & Batas Pengeluaran",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Peringatan real-time saat anggaran kategori mencapai 70%, 90%, atau melebihi 100%."
                }

                val activityChannel = NotificationChannel(
                    CHANNEL_ACTIVITY_ALERTS,
                    "Aktivitas Transaksi & Target Tabungan",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Konfirmasi pencatatan transaksi penting dan notifikasi saat target tabungan tercapai."
                }

                manager.createNotificationChannels(listOf(reminderChannel, budgetChannel, activityChannel))
            }
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

    private fun buildOpenAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            9001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Schedules or cancels daily reminder notifications using AlarmManager.setInexactRepeating
     * (compliant with Google Play policy, no SCHEDULE_EXACT_ALARM permission required).
     */
    fun scheduleDailyReminder(
        context: Context,
        enabled: Boolean,
        hour: Int,
        minute: Int
    ) {
        runCatching {
            ensureChannelCreated(context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, DailyReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                DAILY_ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (!enabled) {
                alarmManager.cancel(pendingIntent)
                return
            }

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                set(Calendar.MINUTE, minute.coerceIn(0, 59))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }

    fun sendDailyScheduledReminder(
        context: Context,
        availableCashIdr: Long,
        todayTxCount: Int,
        isPrivacyMode: Boolean
    ): Boolean {
        ensureChannelCreated(context)
        if (!hasNotificationPermission(context)) return false

        val cashText = IdrFormatter.format(availableCashIdr, isPrivacyHidden = isPrivacyMode)
        val body = when {
            isPrivacyMode -> "Waktunya cek & catat transaksi hari ini ($todayTxCount transaksi tercatat). Mode Privasi aktif."
            todayTxCount == 0 -> "Belum ada transaksi tercatat hari ini. Yuk catat jajan, bensin, atau pemasukanmu! Kas: $cashText."
            else -> "Sudah $todayTxCount transaksi tercatat hari ini. Total kas tersedia: $cashText."
        }

        return postNotification(
            context = context,
            channelId = CHANNEL_ID,
            notificationId = NOTIF_ID_DAILY_REMINDER,
            title = "Pengingat Keuangan Harian • XynoosFinance",
            body = body,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }

    fun notifyBudgetAlerts(
        context: Context,
        triggeredBudgets: List<BudgetProgressSummary>,
        isPrivacyMode: Boolean
    ) {
        if (triggeredBudgets.isEmpty()) return
        ensureChannelCreated(context)
        if (!hasNotificationPermission(context)) return

        triggeredBudgets.take(3).forEachIndexed { idx, summary ->
            val title = when (summary.statusLevel) {
                BudgetAlertLevel.EXCEEDED -> "🚨 Anggaran Melebihi Batas: ${summary.budget.name}"
                BudgetAlertLevel.WARNING_90 -> "⚠️ Anggaran Hampir Habis (${summary.utilizationPercent}%): ${summary.budget.name}"
                else -> "ℹ️ Perhatian Anggaran (${summary.utilizationPercent}%): ${summary.budget.name}"
            }
            val body = if (isPrivacyMode) {
                "Kategori ${summary.budget.categoryName} telah menggunakan ${summary.utilizationPercent}% dari anggaran bulan ini."
            } else {
                "Terpakai ${IdrFormatter.format(summary.spentAmountIdr)} dari batas ${IdrFormatter.format(summary.budget.limitAmountIdr)}. Sisa: ${IdrFormatter.format(summary.remainingAmountIdr)}."
            }

            postNotification(
                context = context,
                channelId = CHANNEL_BUDGET_ALERTS,
                notificationId = NOTIF_ID_BUDGET_ALERT_BASE + idx + summary.budget.id.hashCode().and(0xFF),
                title = title,
                body = body,
                priority = NotificationCompat.PRIORITY_HIGH
            )
        }
    }

    fun notifyGoalCompleted(
        context: Context,
        goalName: String,
        targetAmountIdr: Long,
        isPrivacyMode: Boolean
    ) {
        ensureChannelCreated(context)
        if (!hasNotificationPermission(context)) return

        val amountStr = IdrFormatter.format(targetAmountIdr, isPrivacyHidden = isPrivacyMode)
        val body = "Selamat! Target tabungan \"$goalName\" ($amountStr) telah tercapai 100%!"
        postNotification(
            context = context,
            channelId = CHANNEL_ACTIVITY_ALERTS,
            notificationId = NOTIF_ID_GOAL_ACHIEVED + goalName.hashCode().and(0xFF),
            title = "🎉 Target Tabungan Tercapai!",
            body = body,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    fun notifyTransactionActivity(
        context: Context,
        tx: TransactionModel,
        isPrivacyMode: Boolean
    ) {
        ensureChannelCreated(context)
        if (!hasNotificationPermission(context)) return

        val sign = if (tx.type == TransactionType.INCOME || tx.type == TransactionType.REFUND) "+" else "-"
        val amountStr = IdrFormatter.format(tx.amountIdr, isPrivacyHidden = isPrivacyMode)
        val title = when (tx.type) {
            TransactionType.INCOME -> "💰 Pemasukan Tercatat: ${tx.title}"
            TransactionType.EXPENSE -> "🧾 Pengeluaran Tercatat: ${tx.title}"
            TransactionType.TRANSFER -> "⇄ Transfer Rekening: ${tx.title}"
            else -> "✓ Transaksi Tercatat: ${tx.title}"
        }
        val body = if (isPrivacyMode) {
            "Kategori: ${tx.categoryName} • Rekening: ${tx.accountName} (Nominal disembunyikan)"
        } else {
            "$sign$amountStr • ${tx.categoryName} (${tx.accountName})"
        }
        postNotification(
            context = context,
            channelId = CHANNEL_ACTIVITY_ALERTS,
            notificationId = NOTIF_ID_TX_ACTIVITY,
            title = title,
            body = body,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
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
            "Notifikasi aktif! Mode privasi menyembunyikan nominal saldo. Ketuk untuk membuka XynoosFinance."
        } else {
            "Notifikasi aktif! Kas tersedia saat ini: $cashText. Peringatan anggaran & pengingat harian siap bekerja."
        }

        return postNotification(
            context = context,
            channelId = CHANNEL_ID,
            notificationId = NOTIF_ID_DAILY_REMINDER,
            title = title,
            body = body,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }

    private fun postNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        priority: Int
    ): Boolean {
        return try {
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stat_xynoos)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(priority)
                .setContentIntent(buildOpenAppPendingIntent(context))
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}
