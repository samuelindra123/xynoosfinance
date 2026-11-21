package com.example.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val tagName: String,
    val versionName: String,
    val remoteVersionCode: Int,
    val minRequiredVersionCode: Int,
    val isMandatory: Boolean,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val releasePageUrl: String,
    val publishedAt: String = ""
)

sealed class GitHubUpdateCheckResult {
    data object NotConfigured : GitHubUpdateCheckResult()
    data class UpToDate(val latestTag: String) : GitHubUpdateCheckResult()
    data class UpdateAvailable(val update: AppUpdateInfo) : GitHubUpdateCheckResult()
    data class Error(val message: String) : GitHubUpdateCheckResult()
}

/**
 * Checks the latest published release on GitHub Releases (`GET /repos/{owner}/{repo}/releases/latest`),
 * parses structured `XYNOOS_META` tags and SemVer tags, detects whether the release is marked
 * as mandatory ("Wajib Update"), downloads APK assets in-app, and starts Android's package installer.
 */
object GitHubUpdateChecker {

    private val metaRegex = Regex("""XYNOOS_META:([^\s>]+)""")

    fun sanitizeRepoSlug(raw: String): String {
        return raw.trim()
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removeSuffix(".git")
            .trim('/')
    }

    fun resolveEffectiveRepoSlug(userConfiguredSlug: String, buildConfigSlug: String): String {
        val custom = sanitizeRepoSlug(userConfiguredSlug)
        if (custom.contains('/')) return custom
        val baked = sanitizeRepoSlug(buildConfigSlug)
        return if (baked.contains('/')) baked else ""
    }

    suspend fun fetchLatestRelease(
        repoSlug: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): GitHubUpdateCheckResult = withContext(Dispatchers.IO) {
        val cleanSlug = sanitizeRepoSlug(repoSlug)
        val parts = cleanSlug.split("/").filter { it.isNotBlank() }
        if (parts.size != 2) {
            return@withContext GitHubUpdateCheckResult.NotConfigured
        }

        val apiUrl = "https://api.github.com/repos/${parts[0]}/${parts[1]}/releases/latest"
        var conn: HttpURLConnection? = null
        try {
            val url = URL(apiUrl)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "XynoosFinance-Android-Updater")
            }
            val code = conn.responseCode
            if (code == 404) {
                return@withContext GitHubUpdateCheckResult.Error("Belum ada rilis terbit di GitHub ($cleanSlug).")
            }
            if (code !in 200..299) {
                return@withContext GitHubUpdateCheckResult.Error("GitHub API mengembalikan HTTP $code.")
            }

            val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            parseReleaseJson(
                jsonString = body,
                currentVersionCode = currentVersionCode,
                currentVersionName = currentVersionName
            )
        } catch (e: Exception) {
            GitHubUpdateCheckResult.Error(e.localizedMessage ?: "Gagal terhubung ke GitHub Releases.")
        } finally {
            conn?.disconnect()
        }
    }

    fun parseReleaseJson(
        jsonString: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): GitHubUpdateCheckResult {
        return runCatching {
            val json = JSONObject(jsonString)
            if (json.optBoolean("draft", false)) {
                return@runCatching GitHubUpdateCheckResult.UpToDate(currentVersionName)
            }

            val tagName = json.optString("tag_name", "").trim()
            val releaseTitle = json.optString("name", tagName).ifBlank { tagName }
            val rawBody = json.optString("body", "")
            val htmlUrl = json.optString("html_url", "")
            val publishedAt = json.optString("published_at", "")

            val cleanVersionName = tagName.removePrefix("v").removePrefix("V").trim().ifBlank { currentVersionName }

            // Parse optional structured metadata: XYNOOS_META:versionCode=5;minVersionCode=5;mandatory=true
            var metaVersionCode: Int? = null
            var metaMinVersionCode: Int? = null
            var metaMandatory = false

            val metaMatch = metaRegex.find(rawBody)
            if (metaMatch != null) {
                val pairs = metaMatch.groupValues[1].split(";")
                pairs.forEach { kv ->
                    val idx = kv.indexOf('=')
                    if (idx > 0) {
                        val k = kv.substring(0, idx).trim()
                        val v = kv.substring(idx + 1).trim()
                        when (k) {
                            "versionCode" -> metaVersionCode = v.toIntOrNull()
                            "minVersionCode" -> metaMinVersionCode = v.toIntOrNull()
                            "mandatory" -> metaMandatory = v.equals("true", ignoreCase = true)
                        }
                    }
                }
            }

            val inferredVersionCode = metaVersionCode ?: computePseudoVersionCode(cleanVersionName)
            val hasNewerCode = metaVersionCode != null && metaVersionCode!! > currentVersionCode
            val hasNewerSemVer = compareSemVer(cleanVersionName, currentVersionName) > 0

            if (!hasNewerCode && !hasNewerSemVer) {
                return@runCatching GitHubUpdateCheckResult.UpToDate(tagName.ifBlank { currentVersionName })
            }

            val minRequired = metaMinVersionCode ?: if (metaMandatory) inferredVersionCode else 0
            val isMandatory = metaMandatory ||
                (metaMinVersionCode != null && currentVersionCode < metaMinVersionCode!!) ||
                rawBody.contains("[WAJIB_UPDATE]", ignoreCase = true) ||
                releaseTitle.contains("[WAJIB]", ignoreCase = true)

            // Prefer the Release APK asset URL so tapping "Update Sekarang" starts the APK download right away
            var apkUrl = ""
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                var fallbackApk = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val assetName = asset.optString("name", "")
                    val downloadUrl = asset.optString("browser_download_url", "")
                    if (assetName.endsWith("-Release.apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                        apkUrl = downloadUrl
                        break
                    } else if (assetName.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank() && fallbackApk.isBlank()) {
                        fallbackApk = downloadUrl
                    }
                }
                if (apkUrl.isBlank()) {
                    apkUrl = fallbackApk
                }
            }

            val cleanedNotes = rawBody
                .replace(Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL), "")
                .replace("[WAJIB_UPDATE]", "", ignoreCase = true)
                .trim()
                .take(700)
                .ifBlank { "Peningkatan performa, stabilitas, dan fitur terbaru XynoosFinance." }

            GitHubUpdateCheckResult.UpdateAvailable(
                AppUpdateInfo(
                    tagName = tagName.ifBlank { "v$cleanVersionName" },
                    versionName = cleanVersionName,
                    remoteVersionCode = inferredVersionCode,
                    minRequiredVersionCode = minRequired,
                    isMandatory = isMandatory,
                    releaseTitle = releaseTitle,
                    releaseNotes = cleanedNotes,
                    apkDownloadUrl = apkUrl.ifBlank { htmlUrl },
                    releasePageUrl = htmlUrl,
                    publishedAt = publishedAt
                )
            )
        }.getOrElse { err ->
            GitHubUpdateCheckResult.Error(err.localizedMessage ?: "Gagal memproses data rilis GitHub.")
        }
    }

    fun compareSemVer(remoteVersion: String, localVersion: String): Int {
        val rParts = extractNumericParts(remoteVersion)
        val lParts = extractNumericParts(localVersion)
        val maxLen = maxOf(rParts.size, lParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val l = lParts.getOrElse(i) { 0 }
            if (r != l) return r.compareTo(l)
        }
        return 0
    }

    private fun extractNumericParts(version: String): List<Int> {
        return version
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .split(".")
            .mapNotNull { segment ->
                segment.filter { it.isDigit() }.toIntOrNull()
            }
    }

    private fun computePseudoVersionCode(versionName: String): Int {
        val parts = extractNumericParts(versionName)
        val major = parts.getOrElse(0) { 0 }
        val minor = parts.getOrElse(1) { 0 }
        val patch = parts.getOrElse(2) { 0 }
        return (major * 10000) + (minor * 100) + patch
    }

    suspend fun downloadReleaseApk(
        context: Context,
        updateInfo: AppUpdateInfo,
        onProgress: (Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val targetUrl = updateInfo.apkDownloadUrl
        val parsedUrl = runCatching { URL(targetUrl) }.getOrElse {
            throw IllegalArgumentException("Tautan APK rilis tidak valid.")
        }
        require(parsedUrl.protocol == "https" && parsedUrl.host == "github.com" &&
            parsedUrl.path.endsWith(".apk", ignoreCase = true)
        ) {
            "Rilis GitHub belum menyediakan file APK resmi untuk diunduh."
        }

        val connection = (parsedUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/octet-stream")
            setRequestProperty("User-Agent", "XynoosFinance-Android-Updater")
        }
        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("Unduhan APK gagal (HTTP $responseCode).")
            }
            val totalBytes = connection.contentLengthLong
            require(totalBytes <= MAX_APK_BYTES) {
                "Ukuran APK melebihi batas aman unduhan."
            }

            val updateDirectory = File(context.cacheDir, "updates").apply { mkdirs() }
            val safeVersionName = updateInfo.versionName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val targetFile = File(updateDirectory, "xynoosfinance-$safeVersionName.apk")
            val temporaryFile = File(updateDirectory, "xynoosfinance-$safeVersionName.apk.part")
            var downloadedBytes = 0L
            var lastReportedAt = 0L
            connection.inputStream.use { input ->
                FileOutputStream(temporaryFile).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        downloadedBytes += count
                        require(downloadedBytes <= MAX_APK_BYTES) {
                            "Ukuran APK melebihi batas aman unduhan."
                        }
                        output.write(buffer, 0, count)
                        val now = System.currentTimeMillis()
                        if (now - lastReportedAt >= 250L) {
                            val percent = if (totalBytes > 0) {
                                ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                            } else {
                                -1
                            }
                            withContext(Dispatchers.Main) { onProgress(percent) }
                            lastReportedAt = now
                        }
                    }
                    output.fd.sync()
                }
            }
            require(downloadedBytes > 0L) { "File APK yang diunduh kosong." }
            if (targetFile.exists()) targetFile.delete()
            check(temporaryFile.renameTo(targetFile)) { "Gagal menyiapkan APK untuk dipasang." }
            withContext(Dispatchers.Main) { onProgress(100) }
            targetFile
        } catch (error: Throwable) {
            val safeVersionName = updateInfo.versionName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            File(context.cacheDir, "updates/xynoosfinance-$safeVersionName.apk.part").delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    fun installDownloadedApk(context: Context, apkFile: File): Boolean {
        if (!apkFile.isFile) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val permissionIntent = Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(permissionIntent)
            return false
        }

        return try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private const val MAX_APK_BYTES = 150L * 1024L * 1024L
}
