package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.XynoosDatabase
import com.example.domain.FinancialCalculator
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Handles scheduled daily reminder alarms and dispatches an actionable notification
 * summarizing today's transaction count and available cash (respecting Privacy Mode & Quiet Hours).
 */
class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = UserPreferencesRepository(appContext).preferencesFlow.first()
                if (!prefs.remindersEnabled) return@launch

                // Respect quiet hours (22:00 - 06:00) if enabled
                val hourNow = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                if (prefs.quietHoursEnabled && (hourNow >= 22 || hourNow < 6)) {
                    return@launch
                }

                val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
                if (uid.isNullOrBlank()) {
                    ReminderNotificationHelper.sendDailyScheduledReminder(
                        context = appContext,
                        availableCashIdr = 0L,
                        todayTxCount = 0,
                        isPrivacyMode = true
                    )
                    return@launch
                }

                val dao = XynoosDatabase.getInstance(appContext).financeDao()
                val accounts = dao.getAccounts(uid).map { it.toDomain() }
                val transactions = dao.getTransactions(uid).map { it.toDomain() }
                val todayCal = Calendar.getInstance()
                val todayYear = todayCal.get(Calendar.YEAR)
                val todayDay = todayCal.get(Calendar.DAY_OF_YEAR)
                val todayCount = transactions.count { tx ->
                    val c = Calendar.getInstance().apply { timeInMillis = tx.transactionDateMillis }
                    !tx.isDeleted && c.get(Calendar.YEAR) == todayYear && c.get(Calendar.DAY_OF_YEAR) == todayDay
                }
                val cashAvailable = FinancialCalculator.calculateOverview(
                    accounts = accounts,
                    allTransactions = transactions,
                    periodTransactions = transactions,
                    budgets = emptyList(),
                    goals = emptyList()
                ).availableCashIdr

                ReminderNotificationHelper.sendDailyScheduledReminder(
                    context = appContext,
                    availableCashIdr = cashAvailable,
                    todayTxCount = todayCount,
                    isPrivacyMode = prefs.isPrivacyModeEnabled
                )
            } catch (_: Exception) {
                // Fail safely without crashing background broadcast
            } finally {
                pendingResult.finish()
            }
        }
    }
}
