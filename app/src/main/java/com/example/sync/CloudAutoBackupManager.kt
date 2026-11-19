package com.example.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.OfflineFirstFinanceRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AutoBackupStatusState(
    val isOnline: Boolean = false,
    val isBackingUp: Boolean = false,
    val statusText: String = "Menunggu koneksi internet...",
    val lastBackupTimeLabel: String = "-"
)

/**
 * Low-RAM 3-minute cloud auto-backup scheduler.
 * - Uses ConnectivityManager.NetworkCallback to detect validated internet connectivity
 *   without polling or allocating wake locks.
 * - Runs a single IO coroutine guarded by a Mutex every 3 minutes (180_000 ms).
 * - Only queries and uploads unsynced SQLite rows incrementally, keeping heap memory minimal.
 */
class CloudAutoBackupManager(
    context: Context,
    private val repository: OfflineFirstFinanceRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val isAutoBackupEnabledProvider: () -> Boolean,
    private val onSyncCompleted: ((Int, String) -> Unit)? = null
) {
    companion object {
        const val AUTO_BACKUP_INTERVAL_MS = 180_000L // 3 minutes
    }

    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val safeHandler = CoroutineExceptionHandler { _, _ -> }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + safeHandler)
    private val syncMutex = Mutex()

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _statusState = MutableStateFlow(
        AutoBackupStatusState(
            isOnline = _isOnline.value,
            statusText = if (_isOnline.value) "Online • Auto-Backup 3 menit siap" else "Offline • Tersimpan aman di lokal"
        )
    )
    val statusState: StateFlow<AutoBackupStatusState> = _statusState.asStateFlow()

    private var timerJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun start() {
        registerConnectivityCallback()
        if (timerJob?.isActive == true) return

        timerJob = scope.launch {
            while (isActive) {
                delay(AUTO_BACKUP_INTERVAL_MS)
                if (isAutoBackupEnabledProvider() && _isOnline.value) {
                    runIncrementalBackupInternal(triggerReason = "Siklus 3 Menit")
                }
            }
        }
    }

    fun triggerImmediateIfOnline(reason: String = "Koneksi Pulih") {
        if (!isAutoBackupEnabledProvider() || !_isOnline.value) return
        scope.launch {
            runIncrementalBackupInternal(triggerReason = reason)
        }
    }

    suspend fun runBackupNow(): Result<Int> = runIncrementalBackupInternal("Manual / Otomatis")

    private suspend fun runIncrementalBackupInternal(triggerReason: String): Result<Int> {
        if (!_isOnline.value) {
            _statusState.value = _statusState.value.copy(
                isOnline = false,
                isBackingUp = false,
                statusText = "Offline • Menunggu koneksi internet"
            )
            return Result.failure(IllegalStateException("Tidak terhubung internet"))
        }

        val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
        if (uid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Belum login akun Google"))
        }

        if (syncMutex.isLocked) {
            return Result.success(0)
        }

        return syncMutex.withLock {
            _statusState.value = _statusState.value.copy(
                isOnline = true,
                isBackingUp = true,
                statusText = "Mencadangkan perubahan ke Cloud ($triggerReason)..."
            )
            try {
                val res = repository.backupPendingChangesIncremental()
                val now = System.currentTimeMillis()
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale("id", "ID")).format(Date(now))
                if (res.isSuccess) {
                    val count = res.getOrDefault(0)
                    prefsRepo.recordAutoBackupTimestamp(now)
                    val msg = if (count > 0) {
                        "Auto-Backup berhasil ($count item) • $timeStr"
                    } else {
                        "Cloud ter-update ($timeStr) • Hemat RAM"
                    }
                    _statusState.value = AutoBackupStatusState(
                        isOnline = true,
                        isBackingUp = false,
                        statusText = msg,
                        lastBackupTimeLabel = timeStr
                    )
                    onSyncCompleted?.invoke(count, timeStr)
                } else {
                    _statusState.value = _statusState.value.copy(
                        isOnline = true,
                        isBackingUp = false,
                        statusText = "Tertunda ($timeStr) • Dicoba ulang otomatis"
                    )
                }
                res
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                _statusState.value = _statusState.value.copy(
                    isOnline = _isOnline.value,
                    isBackingUp = false,
                    statusText = "Cadangan lokal aman • Coba ulang nanti"
                )
                Result.failure(t)
            }
        }
    }

    private fun registerConnectivityCallback() {
        val cm = connectivityManager ?: return
        if (networkCallback != null) return

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val online = checkCurrentConnectivity()
                val wasOffline = !_isOnline.value
                _isOnline.value = online
                if (online) {
                    _statusState.value = _statusState.value.copy(
                        isOnline = true,
                        statusText = "Terhubung Internet • Auto-Backup 3 menit aktif"
                    )
                    if (wasOffline) {
                        triggerImmediateIfOnline("Internet Terhubung")
                    }
                }
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val online = hasInternetCapability(networkCapabilities)
                _isOnline.value = online
            }

            override fun onLost(network: Network) {
                val online = checkCurrentConnectivity()
                _isOnline.value = online
                if (!online) {
                    _statusState.value = _statusState.value.copy(
                        isOnline = false,
                        isBackingUp = false,
                        statusText = "Offline • Data aman di perangkat (Room)"
                    )
                }
            }
        }
        networkCallback = callback

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching { cm.registerNetworkCallback(request, callback) }
    }

    private fun checkCurrentConnectivity(): Boolean {
        val cm = connectivityManager ?: return false
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return hasInternetCapability(caps)
    }

    private fun hasInternetCapability(caps: NetworkCapabilities): Boolean {
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun stop() {
        timerJob?.cancel()
        timerJob = null
        val cm = connectivityManager
        val cb = networkCallback
        if (cm != null && cb != null) {
            runCatching { cm.unregisterNetworkCallback(cb) }
        }
        networkCallback = null
        scope.cancel()
    }
}
