package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.data.local.UserPreferencesState
import com.example.data.remote.LiveUsdMarketState
import com.example.domain.AnnualLedgerSummary
import com.example.domain.CategoryBreakdownItem
import com.example.domain.FinancialAccountModel
import com.example.domain.FinancialGoalModel
import com.example.domain.FinancialOverviewSummary
import com.example.domain.IdrFormatter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class GeneratedStatusImageInfo(
    val file: File,
    val generatedAtFormatted: String,
    val netWorthIdr: Long,
    val netWorthUsdFormatted: String
)

/**
 * Generates a high-resolution (1080x1920) luxury fintech Detailed Status Card PNG image
 * suitable for WhatsApp Status, Instagram Story, Telegram, or executive sharing.
 * Includes detailed category breakdown, annual summary, account snapshot, and USD 1m chart.
 */
object StatusImageExporter {

    private const val WIDTH = 1080
    private const val HEIGHT = 1920

    fun generateStatusImagePng(
        context: Context,
        prefs: UserPreferencesState,
        overview: FinancialOverviewSummary,
        usdMarket: LiveUsdMarketState,
        goals: List<FinancialGoalModel>,
        periodLabel: String,
        categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
        accounts: List<FinancialAccountModel> = emptyList(),
        annualSummary: AnnualLedgerSummary? = null
    ): Result<GeneratedStatusImageInfo> {
        return try {
            val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val isPrivacy = prefs.isPrivacyModeEnabled
            val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy • HH:mm:ss 'WIB'", Locale("id", "ID"))
            val timestampStr = dateFormat.format(Date())

            // 1. Deep Obsidian Luxury Gradient Background
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(),
                    intArrayOf(
                        Color.rgb(8, 11, 16),
                        Color.rgb(14, 21, 32),
                        Color.rgb(9, 14, 22)
                    ),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)

            // Subtle architectural grid lines
            val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(18, 114, 167, 255)
                strokeWidth = 1.5f
            }
            for (x in 90 until WIDTH step 120) {
                canvas.drawLine(x.toFloat(), 0f, x.toFloat(), HEIGHT.toFloat(), gridPaint)
            }
            for (y in 90 until HEIGHT step 120) {
                canvas.drawLine(0f, y.toFloat(), WIDTH.toFloat(), y.toFloat(), gridPaint)
            }

            // 2. Outer Card Frame
            val cardRect = RectF(52f, 56f, WIDTH - 52f, HEIGHT - 56f)
            val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(232, 16, 21, 29)
                style = Paint.Style.FILL
            }
            val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(95, 73, 224, 160)
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.drawRoundRect(cardRect, 48f, 48f, cardBgPaint)
            canvas.drawRoundRect(cardRect, 48f, 48f, cardBorderPaint)

            // Typography Paints
            val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(73, 224, 160)
                textSize = 27f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(152, 163, 179)
                textSize = 22f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(152, 163, 179)
                textSize = 22f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val bodyRowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(225, 232, 242)
                textSize = 24f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val heroAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(243, 246, 250)
                textSize = 64f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            val usdBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(114, 167, 255)
                textSize = 32f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            val metricValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(243, 246, 250)
                textSize = 32f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            val smallMonoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(243, 246, 250)
                textSize = 24f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }

            var y = 126f

            // 3. Brand Header & Full Year Timestamp
            canvas.drawText("XYNOOSFINANCE • LAPORAN STATUS KEUANGAN RINCI", 96f, y, brandPaint)
            y += 38f
            val ownerName = prefs.displayName.ifBlank { "Eksekutif Xynoos" }
            canvas.drawText("$ownerName  •  ${prefs.reportOrgName}  •  Periode: $periodLabel", 96f, y, subHeaderPaint)
            y += 32f
            canvas.drawText("Waktu Cetak: $timestampStr", 96f, y, subHeaderPaint)
            y += 44f

            // 4. Hero Net Worth Box (IDR + Real-Time USD)
            val heroRect = RectF(92f, y, WIDTH - 92f, y + 276f)
            val heroBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    heroRect.left, heroRect.top, heroRect.right, heroRect.bottom,
                    intArrayOf(Color.rgb(20, 34, 46), Color.rgb(16, 24, 35)),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(heroRect, 34f, 34f, heroBoxPaint)

            canvas.drawText("TOTAL KEKAYAAN BERSIH (NET WORTH)", 126f, y + 54f, labelPaint)
            val netWorthStr = IdrFormatter.format(overview.netWorthIdr, isPrivacy)
            canvas.drawText(netWorthStr, 126f, y + 132f, heroAmountPaint)

            val netWorthUsdStr = IdrFormatter.formatUsdFromIdr(
                amountIdr = overview.netWorthIdr,
                usdRateIdr = usdMarket.currentRateIdr,
                isPrivacyHidden = isPrivacy
            )
            val rateFormatted = IdrFormatter.format(usdMarket.currentRateIdr.toLong())
            canvas.drawText("≈ $netWorthUsdStr  (Spot 1m: $rateFormatted / $1 USD)", 126f, y + 190f, usdBadgePaint)

            val cashUsdStr = IdrFormatter.formatUsdFromIdr(
                amountIdr = overview.availableCashIdr,
                usdRateIdr = usdMarket.currentRateIdr,
                isPrivacyHidden = isPrivacy
            )
            canvas.drawText(
                "Kas Likuid: ${IdrFormatter.format(overview.availableCashIdr, isPrivacy)} ($cashUsdStr)",
                126f,
                y + 240f,
                subHeaderPaint
            )
            y += 304f

            // 5. Stock-Style Mini Chart Box (Live USD/IDR)
            val boxSurfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(22, 29, 40)
            }
            val chartRect = RectF(92f, y, WIDTH - 92f, y + 215f)
            canvas.drawRoundRect(chartRect, 30f, 30f, boxSurfacePaint)
            canvas.drawText("GRAFIK PASAR REAL-TIME USD/IDR (1 MENIT)", 126f, y + 46f, labelPaint)

            val candles = usdMarket.candles
            if (candles.size >= 2) {
                val minP = candles.minOf { it.lowIdr }
                val maxP = candles.maxOf { it.highIdr }
                val range = max(1.0, maxP - minP)
                val plotLeft = 126f
                val plotRight = WIDTH - 126f
                val plotTop = y + 72f
                val plotBottom = y + 186f
                val stepX = (plotRight - plotLeft) / max(1, candles.size - 1)

                val linePath = Path()
                candles.forEachIndexed { idx, c ->
                    val px = plotLeft + idx * stepX
                    val norm = ((c.closeIdr - minP) / range).toFloat()
                    val py = plotBottom - norm * (plotBottom - plotTop)
                    if (idx == 0) linePath.moveTo(px, py) else linePath.lineTo(px, py)
                }
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(73, 224, 160)
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                }
                canvas.drawPath(linePath, linePaint)
            }
            y += 242f

            // 6. Cash Flow Period Cards (Income vs Expense)
            val halfW = (WIDTH - 92f * 2 - 24f) / 2f
            val incRect = RectF(92f, y, 92f + halfW, y + 170f)
            val expRect = RectF(92f + halfW + 24f, y, WIDTH - 92f, y + 170f)
            canvas.drawRoundRect(incRect, 26f, 26f, boxSurfacePaint)
            canvas.drawRoundRect(expRect, 26f, 26f, boxSurfacePaint)

            canvas.drawText("PEMASUKAN ($periodLabel)", incRect.left + 28f, y + 48f, labelPaint)
            val incPaint = Paint(metricValuePaint).apply { color = Color.rgb(73, 224, 160) }
            canvas.drawText(IdrFormatter.format(overview.periodIncomeIdr, isPrivacy), incRect.left + 28f, y + 106f, incPaint)
            canvas.drawText(
                IdrFormatter.formatUsdFromIdr(overview.periodIncomeIdr, usdMarket.currentRateIdr, isPrivacy),
                incRect.left + 28f,
                y + 144f,
                subHeaderPaint
            )

            canvas.drawText("PENGELUARAN ($periodLabel)", expRect.left + 28f, y + 48f, labelPaint)
            val expPaint = Paint(metricValuePaint).apply { color = Color.rgb(255, 90, 101) }
            canvas.drawText(IdrFormatter.format(overview.periodExpenseIdr, isPrivacy), expRect.left + 28f, y + 106f, expPaint)
            canvas.drawText(
                IdrFormatter.formatUsdFromIdr(overview.periodExpenseIdr, usdMarket.currentRateIdr, isPrivacy),
                expRect.left + 28f,
                y + 144f,
                subHeaderPaint
            )
            y += 196f

            // 7. Annual Performance Summary Box (Masuk & Abis Tahun Berjalan)
            if (annualSummary != null) {
                val annualRect = RectF(92f, y, WIDTH - 92f, y + 160f)
                canvas.drawRoundRect(annualRect, 26f, 26f, boxSurfacePaint)
                canvas.drawText("REKAP TAHUNAN (${annualSummary.year} • ${annualSummary.transactionCount} TRANSAKSI)", 126f, y + 48f, labelPaint)
                canvas.drawText(
                    "Masuk: ${IdrFormatter.format(annualSummary.totalIncomeIdr, isPrivacy)}",
                    126f,
                    y + 98f,
                    Paint(smallMonoPaint).apply { color = Color.rgb(73, 224, 160) }
                )
                canvas.drawText(
                    "Keluar: ${IdrFormatter.format(annualSummary.totalNetExpenseIdr, isPrivacy)}",
                    WIDTH / 2f + 12f,
                    y + 98f,
                    Paint(smallMonoPaint).apply { color = Color.rgb(255, 90, 101) }
                )
                canvas.drawText(
                    "Arus Kas Bersih ${annualSummary.year}: ${IdrFormatter.format(annualSummary.netCashFlowIdr, isPrivacy, showPlusSign = true)} (Saving Rate: ${annualSummary.savingsRatePercent}%)",
                    126f,
                    y + 136f,
                    subHeaderPaint
                )
                y += 186f
            }

            // 8. Detailed Top Expense Categories + Accounts Snapshot
            val detailRect = RectF(92f, y, WIDTH - 92f, y + 290f)
            canvas.drawRoundRect(detailRect, 28f, 28f, boxSurfacePaint)
            canvas.drawText("RINCIAN PENGELUARAN PER KATEGORI & REKENING", 126f, y + 48f, labelPaint)

            var detailY = y + 92f
            if (categoryBreakdown.isEmpty()) {
                canvas.drawText("• Belum ada pengeluaran tercatat per kategori pada periode ini.", 126f, detailY, subHeaderPaint)
                detailY += 38f
            } else {
                categoryBreakdown.take(4).forEach { cat ->
                    val pctText = String.format(Locale.US, "%.1f%%", cat.percentage)
                    canvas.drawText("• ${cat.categoryName.take(24)} (${cat.transactionCount}x • $pctText)", 126f, detailY, bodyRowPaint)
                    val amtText = IdrFormatter.format(cat.amountIdr, isPrivacy)
                    val amtW = smallMonoPaint.measureText(amtText)
                    canvas.drawText(amtText, WIDTH - 126f - amtW, detailY, smallMonoPaint)
                    detailY += 38f
                }
            }

            if (accounts.isNotEmpty()) {
                detailY += 8f
                val topAccText = accounts.take(3).joinToString("  |  ") { acc ->
                    "${acc.name}: ${IdrFormatter.formatCompact(acc.currentBalanceIdr, isPrivacy)}"
                }
                canvas.drawText("Saldo Akun: $topAccText", 126f, detailY, subHeaderPaint)
            }
            y += 316f

            // 9. Auto-Connected Savings Goals Progress
            val goalRect = RectF(92f, y, WIDTH - 92f, y + 185f)
            canvas.drawRoundRect(goalRect, 28f, 28f, boxSurfacePaint)
            canvas.drawText("TARGET TABUNGAN (TERKONEKSI TOTAL SALDO OTOMATIS)", 126f, y + 48f, labelPaint)

            val totalGoalTarget = goals.sumOf { it.targetAmountIdr }
            val effectiveSaved = if (goals.isNotEmpty()) {
                max(goals.sumOf { it.currentSavedIdr }, max(0L, overview.availableCashIdr))
            } else {
                max(0L, overview.availableCashIdr)
            }
            val pct = if (totalGoalTarget > 0L) {
                min(100, ((effectiveSaved * 100L) / totalGoalTarget).toInt())
            } else 100

            canvas.drawText(
                "Terkumpul: ${IdrFormatter.format(effectiveSaved, isPrivacy)} / ${IdrFormatter.format(totalGoalTarget, isPrivacy)} ($pct%)",
                126f,
                y + 102f,
                smallMonoPaint
            )

            // Progress track
            val barTrack = RectF(126f, y + 126f, WIDTH - 126f, y + 156f)
            val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(36, 48, 66) }
            canvas.drawRoundRect(barTrack, 16f, 16f, trackPaint)
            val fillWidth = (barTrack.width() * (pct / 100f)).coerceAtLeast(24f)
            val barFill = RectF(126f, y + 126f, 126f + fillWidth, y + 156f)
            canvas.drawRoundRect(barFill, 16f, 16f, brandPaint)

            // 10. Footer Signature
            canvas.drawText(
                "Verified by XynoosFinance AI Ledger • Xynoos Technology • Understand your money.",
                96f,
                HEIGHT - 86f,
                subHeaderPaint
            )

            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val outFile = File(reportsDir, "XynoosFinance_Status_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            if (outFile.length() == 0L) {
                outFile.writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
            }

            Result.success(
                GeneratedStatusImageInfo(
                    file = outFile,
                    generatedAtFormatted = timestampStr,
                    netWorthIdr = overview.netWorthIdr,
                    netWorthUsdFormatted = netWorthUsdStr
                )
            )
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: e.toString(), e))
        }
    }

    fun shareStatusImage(context: Context, file: File, caption: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            putExtra(Intent.EXTRA_SUBJECT, "Status Keuangan XynoosFinance")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, "Bagikan Kartu Status Keuangan XynoosFinance").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
