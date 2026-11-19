package com.example.data.local

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.AppThemeMode
import com.example.domain.FinancialProfileType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.xynoosDataStore by preferencesDataStore(
    name = "xynoos_user_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler(
        produceNewData = { emptyPreferences() }
    )
)

data class UserPreferencesState(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val isPrivacyModeEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val displayName: String = "",
    val currencyCode: String = "IDR",
    val financialProfileType: FinancialProfileType = FinancialProfileType.PERSONAL,
    val remindersEnabled: Boolean = true,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val quietHoursEnabled: Boolean = true,
    val budgetAlertsEnabled: Boolean = true,
    val activityNotificationsEnabled: Boolean = true,
    val autoBackupEvery3MinEnabled: Boolean = true,
    val lastAutoBackupMillis: Long = 0L,
    val patternLockEnabled: Boolean = false,
    val patternLockHash: String = "",
    val patternLockOnResume: Boolean = true,
    val reportOrgName: String = "Xynoos Private Wealth",
    val reportOrgAddress: String = "Jakarta Selatan, DKI Jakarta, Indonesia",
    val reportContactInfo: String = "finance@xynoos.id",
    val reportFooterText: String = "Dokumen rahasia — Dihasilkan secara deterministik oleh XynoosFinance",
    val reportSignatory: String = "",
    val whatsAppPhone: String = "",
    val whatsAppAutoNotifyOnTransaction: Boolean = false,
    val githubUpdateRepo: String = "",
    val lastUpdateCheckMillis: Long = 0L,
    val skippedUpdateTag: String = ""
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PRIVACY_MODE = booleanPreferencesKey("privacy_mode")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val PROFILE_TYPE = stringPreferencesKey("financial_profile_type")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val QUIET_HOURS = booleanPreferencesKey("quiet_hours_enabled")
        val BUDGET_ALERTS_ENABLED = booleanPreferencesKey("budget_alerts_enabled")
        val ACTIVITY_NOTIFS_ENABLED = booleanPreferencesKey("activity_notifs_enabled")
        val AUTO_BACKUP_3MIN = booleanPreferencesKey("auto_backup_3min_enabled")
        val LAST_AUTO_BACKUP_MILLIS = longPreferencesKey("last_auto_backup_millis")
        val PATTERN_LOCK_ENABLED = booleanPreferencesKey("pattern_lock_enabled")
        val PATTERN_LOCK_HASH = stringPreferencesKey("pattern_lock_hash")
        val PATTERN_LOCK_ON_RESUME = booleanPreferencesKey("pattern_lock_on_resume")
        val REPORT_ORG_NAME = stringPreferencesKey("report_org_name")
        val REPORT_ORG_ADDRESS = stringPreferencesKey("report_org_address")
        val REPORT_CONTACT_INFO = stringPreferencesKey("report_contact_info")
        val REPORT_FOOTER_TEXT = stringPreferencesKey("report_footer_text")
        val REPORT_SIGNATORY = stringPreferencesKey("report_signatory")
        val WHATSAPP_PHONE = stringPreferencesKey("whatsapp_phone")
        val WHATSAPP_AUTO_NOTIFY = booleanPreferencesKey("whatsapp_auto_notify")
        val GITHUB_UPDATE_REPO = stringPreferencesKey("github_update_repo")
        val LAST_UPDATE_CHECK_MILLIS = longPreferencesKey("last_update_check_millis")
        val SKIPPED_UPDATE_TAG = stringPreferencesKey("skipped_update_tag")
    }

    val preferencesFlow: Flow<UserPreferencesState> = context.xynoosDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: AppThemeMode.DARK.name
        val profileStr = prefs[Keys.PROFILE_TYPE] ?: FinancialProfileType.PERSONAL.name
        UserPreferencesState(
            themeMode = runCatching { AppThemeMode.valueOf(themeStr) }.getOrDefault(AppThemeMode.DARK),
            isPrivacyModeEnabled = prefs[Keys.PRIVACY_MODE] ?: false,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            displayName = prefs[Keys.DISPLAY_NAME] ?: "",
            currencyCode = prefs[Keys.CURRENCY_CODE] ?: "IDR",
            financialProfileType = runCatching { FinancialProfileType.valueOf(profileStr) }.getOrDefault(FinancialProfileType.PERSONAL),
            remindersEnabled = prefs[Keys.REMINDERS_ENABLED] ?: true,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 20,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            quietHoursEnabled = prefs[Keys.QUIET_HOURS] ?: true,
            budgetAlertsEnabled = prefs[Keys.BUDGET_ALERTS_ENABLED] ?: true,
            activityNotificationsEnabled = prefs[Keys.ACTIVITY_NOTIFS_ENABLED] ?: true,
            autoBackupEvery3MinEnabled = prefs[Keys.AUTO_BACKUP_3MIN] ?: true,
            lastAutoBackupMillis = prefs[Keys.LAST_AUTO_BACKUP_MILLIS] ?: 0L,
            patternLockEnabled = prefs[Keys.PATTERN_LOCK_ENABLED] ?: false,
            patternLockHash = prefs[Keys.PATTERN_LOCK_HASH] ?: "",
            patternLockOnResume = prefs[Keys.PATTERN_LOCK_ON_RESUME] ?: true,
            reportOrgName = prefs[Keys.REPORT_ORG_NAME] ?: "Xynoos Private Wealth",
            reportOrgAddress = prefs[Keys.REPORT_ORG_ADDRESS] ?: "Jakarta Selatan, DKI Jakarta, Indonesia",
            reportContactInfo = prefs[Keys.REPORT_CONTACT_INFO] ?: "finance@xynoos.id",
            reportFooterText = prefs[Keys.REPORT_FOOTER_TEXT] ?: "Dokumen rahasia — Dihasilkan secara deterministik oleh XynoosFinance",
            reportSignatory = prefs[Keys.REPORT_SIGNATORY] ?: (prefs[Keys.DISPLAY_NAME] ?: ""),
            whatsAppPhone = prefs[Keys.WHATSAPP_PHONE] ?: "",
            whatsAppAutoNotifyOnTransaction = prefs[Keys.WHATSAPP_AUTO_NOTIFY] ?: false,
            githubUpdateRepo = prefs[Keys.GITHUB_UPDATE_REPO] ?: "",
            lastUpdateCheckMillis = prefs[Keys.LAST_UPDATE_CHECK_MILLIS] ?: 0L,
            skippedUpdateTag = prefs[Keys.SKIPPED_UPDATE_TAG] ?: ""
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.xynoosDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setPrivacyMode(enabled: Boolean) {
        context.xynoosDataStore.edit { it[Keys.PRIVACY_MODE] = enabled }
    }

    suspend fun completeOnboarding(
        displayName: String,
        currencyCode: String,
        profileType: FinancialProfileType,
        remindersEnabled: Boolean,
        themeMode: AppThemeMode
    ) {
        context.xynoosDataStore.edit { prefs ->
            prefs[Keys.DISPLAY_NAME] = displayName.trim().ifEmpty { "Pengguna Xynoos" }
            prefs[Keys.CURRENCY_CODE] = currencyCode
            prefs[Keys.PROFILE_TYPE] = profileType.name
            prefs[Keys.REMINDERS_ENABLED] = remindersEnabled
            prefs[Keys.THEME_MODE] = themeMode.name
            prefs[Keys.ONBOARDING_COMPLETED] = true
            if (prefs[Keys.REPORT_SIGNATORY].isNullOrBlank()) {
                prefs[Keys.REPORT_SIGNATORY] = displayName.trim()
            }
        }
    }

    suspend fun updateProfilePreferences(
        displayName: String,
        profileType: FinancialProfileType,
        currencyCode: String
    ) {
        context.xynoosDataStore.edit { prefs ->
            prefs[Keys.DISPLAY_NAME] = displayName
            prefs[Keys.PROFILE_TYPE] = profileType.name
            prefs[Keys.CURRENCY_CODE] = currencyCode
        }
    }

    suspend fun updateReminderSettings(
        enabled: Boolean,
        hour: Int,
        minute: Int,
        quietHours: Boolean,
        budgetAlertsEnabled: Boolean = true,
        activityNotificationsEnabled: Boolean = true
    ) {
        context.xynoosDataStore.edit { prefs ->
            prefs[Keys.REMINDERS_ENABLED] = enabled
            prefs[Keys.REMINDER_HOUR] = hour
            prefs[Keys.REMINDER_MINUTE] = minute
            prefs[Keys.QUIET_HOURS] = quietHours
            prefs[Keys.BUDGET_ALERTS_ENABLED] = budgetAlertsEnabled
            prefs[Keys.ACTIVITY_NOTIFS_ENABLED] = activityNotificationsEnabled
        }
    }

    suspend fun updateAutoBackupSettings(
        enabled: Boolean,
        lastBackupMillis: Long? = null
    ) {
        context.xynoosDataStore.edit { prefs ->
            prefs[Keys.AUTO_BACKUP_3MIN] = enabled
            if (lastBackupMillis != null) {
                prefs[Keys.LAST_AUTO_BACKUP_MILLIS] = lastBackupMillis
            }
        }
    }

    suspend fun recordAutoBackupTimestamp(timestampMillis: Long = System.currentTimeMillis()) {
        context.xynoosDataStore.edit { prefs ->
            prefs[Keys.LAST_AUTO_BACKUP_MILLIS] = timestampMillis
        }
    }

    suspend fun updatePatternLock(
        enabled: Boolean,
        patternHash: String? = null,
        lockOnResume: Boolean? = null
    ) {
        context.xynoosDataStore.edit { prefs ->
            prefs[Keys.PATTERN_LOCK_ENABLED] = enabled
            if (patternHash != null) {
                prefs[Keys.PATTERN_LOCK_HASH] = patternHash
            }
            if (lockOnResume != null) {
                prefs[Keys.PATTERN_LOCK_ON_RESUME] = lockOnResume
            }
        }
    }

    suspend fun updateReportIdentity(
        orgName: String,
        orgAddress: String,
        contactInfo: String,
        footerText: String,
        signatory: String
    ) {
        context.xynoosDataStore.edit { prefs ->
            prefs[Keys.REPORT_ORG_NAME] = orgName
            prefs[Keys.REPORT_ORG_ADDRESS] = orgAddress
            prefs[Keys.REPORT_CONTACT_INFO] = contactInfo
            prefs[Keys.REPORT_FOOTER_TEXT] = footerText
            prefs[Keys.REPORT_SIGNATORY] = signatory
        }
    }

    suspend fun updateWhatsAppSettings(
        phoneNumber: String,
        autoNotify: Boolean
    ) {
        context.xynoosDataStore.edit { prefs ->
            prefs[Keys.WHATSAPP_PHONE] = phoneNumber.trim()
            prefs[Keys.WHATSAPP_AUTO_NOTIFY] = autoNotify
        }
    }

    suspend fun updateGitHubRepoSlug(repoSlug: String) {
        context.xynoosDataStore.edit { prefs ->
            prefs[Keys.GITHUB_UPDATE_REPO] = repoSlug.trim().removePrefix("https://github.com/").trim('/')
        }
    }

    suspend fun recordUpdateCheck(timestampMillis: Long = System.currentTimeMillis()) {
        context.xynoosDataStore.edit { prefs ->
            prefs[Keys.LAST_UPDATE_CHECK_MILLIS] = timestampMillis
        }
    }

    suspend fun skipUpdateTag(tagName: String) {
        context.xynoosDataStore.edit { prefs ->
            prefs[Keys.SKIPPED_UPDATE_TAG] = tagName.trim()
        }
    }
}
