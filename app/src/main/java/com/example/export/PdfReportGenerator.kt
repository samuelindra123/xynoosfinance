package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.UserPreferencesState
import com.example.domain.BudgetProgressSummary
import com.example.domain.BusinessProfitLossSummary
import com.example.domain.CategoryBreakdownItem
import com.example.domain.FinancialAccountModel
import com.example.domain.FinancialGoalModel
import com.example.domain.FinancialOverviewSummary
import com.example.domain.IdrFormatter
import com.example.domain.InvoiceReferenceModel
import com.example.domain.TransactionModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfReportConfig(
    val reportTitle: String = "Laporan Eksekutif Keuangan & Buku Besar",
    val periodLabel: String = "Bulan Ini",
    val customNotes: String = "Seluruh angka dalam laporan ini dihitung secara deterministik dari transaksi terverifikasi di XynoosFinance.",
    val includeTransactions: Boolean = true,
    val includeCategoryAnalysis: Boolean = true,
    val includeBudgetsAndGoals: Boolean = true,
    val includeBusinessProfitLoss: Boolean = true
)

data class GeneratedPdfInfo(
    val file: File,
    val pageCount: Int,
    val generatedAtFormatted: String,
    val totalIncomeIdr: Long,
    val totalExpenseIdr: Long,
    val netWorthIdr: Long
)

/**
 * Generates a print-friendly, multi-page A4 financial report using Android's native PdfDocument API.
 * Handles pagination, repeating table headers, Indonesian Rupiah formatting, and organization identity.
 */
object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    fun generateFinancialReportPdf(
        context: Context,
        config: PdfReportConfig,
        prefs: UserPreferencesState,
        overview: FinancialOverviewSummary,
        accounts: List<FinancialAccountModel>,
        transactions: List<TransactionModel>,
        categoryBreakdown: List<CategoryBreakdownItem>,
        budgets: List<BudgetProgressSummary>,
        goals: List<FinancialGoalModel>,
        businessPl: BusinessProfitLossSummary,
        invoices: List<InvoiceReferenceModel>
    ): Result<GeneratedPdfInfo> {
        return try {
            val document = runCatching { PdfDocument() }.getOrNull()
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
            val shortDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
            val timestampStr = dateFormat.format(Date())

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(13, 20, 31)
                textSize = 18f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(79, 93, 115)
                textSize = 10f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(11, 138, 86)
                textSize = 12f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(20, 28, 40)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(13, 20, 31)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val monoAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(13, 20, 31)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(238, 242, 248)
                style = Paint.Style.FILL
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(216, 224, 236)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            val accentBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(11, 138, 86)
                style = Paint.Style.FILL
            }

            var currentPageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
            var currentPage = runCatching { document?.startPage(pageInfo) }.getOrNull()
            var canvas = runCatching { currentPage?.canvas }.getOrNull() ?: Canvas()
            var y = MARGIN

            fun drawPageFooter(c: Canvas, pageNum: Int) {
                val footerY = PAGE_HEIGHT - 24f
                c.drawLine(MARGIN, footerY - 12f, PAGE_WIDTH - MARGIN, footerY - 12f, borderPaint)
                c.drawText(
                    prefs.reportFooterText.take(85),
                    MARGIN,
                    footerY,
                    subtitlePaint
                )
                val pageLabel = "Halaman $pageNum • $timestampStr"
                val w = subtitlePaint.measureText(pageLabel)
                c.drawText(pageLabel, PAGE_WIDTH - MARGIN - w, footerY, subtitlePaint)
            }

            fun ensureSpace(requiredHeight: Float, onNewPageHeader: ((Canvas) -> Unit)? = null) {
                if (y + requiredHeight > PAGE_HEIGHT - 55f) {
                    drawPageFooter(canvas, currentPageNumber)
                    currentPage?.let { page -> runCatching { document?.finishPage(page) } }
                    currentPageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
                    currentPage = runCatching { document?.startPage(pageInfo) }.getOrNull()
                    canvas = runCatching { currentPage?.canvas }.getOrNull() ?: Canvas()
                    y = MARGIN
                    onNewPageHeader?.invoke(canvas)
                }
            }

            // 1. Cover Header & Organization Identity
            canvas.drawRect(MARGIN, y, MARGIN + 6f, y + 48f, accentBarPaint)
            canvas.drawText("XYNOOSFINANCE — ${prefs.reportOrgName.uppercase(Locale.ROOT)}", MARGIN + 16f, y + 15f, sectionPaint)
            canvas.drawText(config.reportTitle, MARGIN + 16f, y + 34f, titlePaint)
            canvas.drawText(
                "Periode: ${config.periodLabel}  |  Mata Uang: ${prefs.currencyCode}  |  Alamat: ${prefs.reportOrgAddress} (${prefs.reportContactInfo})",
                MARGIN + 16f,
                y + 48f,
                subtitlePaint
            )
            y += 64f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, borderPaint)
            y += 16f

            // 2. Executive Summary Metrics Box
            canvas.drawText("1. RINGKASAN EKSEKUTIF KEUANGAN", MARGIN, y, sectionPaint)
            y += 10f
            val summaryBox = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 72f)
            canvas.drawRoundRect(summaryBox, 6f, 6f, headerBgPaint)
            canvas.drawRoundRect(summaryBox, 6f, 6f, borderPaint)

            val colWidth = CONTENT_WIDTH / 3f
            // Column 1
            canvas.drawText("Kas Tersedia (Likuid)", MARGIN + 12f, y + 20f, subtitlePaint)
            canvas.drawText(IdrFormatter.format(overview.availableCashIdr), MARGIN + 12f, y + 35f, monoAmountPaint)
            canvas.drawText("Kekayaan Bersih (Net Worth)", MARGIN + 12f, y + 52f, subtitlePaint)
            canvas.drawText(IdrFormatter.format(overview.netWorthIdr), MARGIN + 12f, y + 65f, monoAmountPaint)

            // Column 2
            canvas.drawText("Total Pemasukan Periode", MARGIN + colWidth + 12f, y + 20f, subtitlePaint)
            canvas.drawText(IdrFormatter.format(overview.periodIncomeIdr), MARGIN + colWidth + 12f, y + 35f, monoAmountPaint)
            canvas.drawText("Total Pengeluaran Periode", MARGIN + colWidth + 12f, y + 52f, subtitlePaint)
            canvas.drawText(IdrFormatter.format(overview.periodExpenseIdr), MARGIN + colWidth + 12f, y + 65f, monoAmountPaint)

            // Column 3
            canvas.drawText("Arus Kas Bersih (Net Cash Flow)", MARGIN + colWidth * 2 + 12f, y + 20f, subtitlePaint)
            canvas.drawText(IdrFormatter.format(overview.netCashFlowIdr, showPlusSign = true), MARGIN + colWidth * 2 + 12f, y + 35f, monoAmountPaint)
            canvas.drawText("Total Aset / Kewajiban", MARGIN + colWidth * 2 + 12f, y + 52f, subtitlePaint)
            canvas.drawText(
                "${IdrFormatter.formatCompact(overview.totalAssetsIdr)} / ${IdrFormatter.formatCompact(overview.totalLiabilitiesIdr)}",
                MARGIN + colWidth * 2 + 12f,
                y + 65f,
                monoAmountPaint
            )
            y += 90f

            // 3. Accounts & Wallets Breakdown
            ensureSpace(80f)
            canvas.drawText("2. POSISI REKENING & DOMPET (${accounts.size} AKUN)", MARGIN, y, sectionPaint)
            y += 8f
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, headerBgPaint)
            canvas.drawText("Nama Akun", MARGIN + 8f, y + 14f, bodyBoldPaint)
            canvas.drawText("Tipe", MARGIN + 200f, y + 14f, bodyBoldPaint)
            canvas.drawText("Saldo Awal", MARGIN + 310f, y + 14f, bodyBoldPaint)
            canvas.drawText("Saldo Saat Ini", MARGIN + 420f, y + 14f, bodyBoldPaint)
            y += 20f

            for (acc in accounts) {
                ensureSpace(20f)
                canvas.drawText(acc.name.take(28), MARGIN + 8f, y + 14f, bodyPaint)
                canvas.drawText(acc.accountType.labelId.take(18), MARGIN + 200f, y + 14f, bodyPaint)
                canvas.drawText(IdrFormatter.format(acc.openingBalanceIdr), MARGIN + 310f, y + 14f, monoAmountPaint)
                canvas.drawText(IdrFormatter.format(acc.currentBalanceIdr), MARGIN + 420f, y + 14f, monoAmountPaint)
                canvas.drawLine(MARGIN, y + 19f, PAGE_WIDTH - MARGIN, y + 19f, borderPaint)
                y += 20f
            }
            y += 16f

            // 4. Category Analysis
            if (config.includeCategoryAnalysis) {
                ensureSpace(70f)
                canvas.drawText("3. ANALISIS PENGELUARAN PER KATEGORI", MARGIN, y, sectionPaint)
                y += 8f
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, headerBgPaint)
                canvas.drawText("Kategori", MARGIN + 8f, y + 14f, bodyBoldPaint)
                canvas.drawText("Jumlah Transaksi", MARGIN + 220f, y + 14f, bodyBoldPaint)
                canvas.drawText("Porsi (%)", MARGIN + 340f, y + 14f, bodyBoldPaint)
                canvas.drawText("Total Pengeluaran", MARGIN + 420f, y + 14f, bodyBoldPaint)
                y += 20f

                if (categoryBreakdown.isEmpty()) {
                    canvas.drawText("Tidak ada pengeluaran pada periode ini.", MARGIN + 8f, y + 14f, subtitlePaint)
                    y += 22f
                } else {
                    for (item in categoryBreakdown) {
                        ensureSpace(20f)
                        canvas.drawText(item.categoryName.take(30), MARGIN + 8f, y + 14f, bodyPaint)
                        canvas.drawText("${item.transactionCount} transaksi", MARGIN + 220f, y + 14f, bodyPaint)
                        canvas.drawText(String.format(Locale.US, "%.1f%%", item.percentage), MARGIN + 340f, y + 14f, bodyPaint)
                        canvas.drawText(IdrFormatter.format(item.amountIdr), MARGIN + 420f, y + 14f, monoAmountPaint)
                        canvas.drawLine(MARGIN, y + 19f, PAGE_WIDTH - MARGIN, y + 19f, borderPaint)
                        y += 20f
                    }
                }
                y += 16f
            }

            // 5. Business Profit & Loss + Receivables/Payables
            if (config.includeBusinessProfitLoss) {
                ensureSpace(95f)
                canvas.drawText("4. RINGKASAN LABA RUGI & PIUTANG/UTang BISNIS", MARGIN, y, sectionPaint)
                y += 8f
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, headerBgPaint)
                canvas.drawText("Komponen Laba Rugi / Kas Bisnis", MARGIN + 8f, y + 14f, bodyBoldPaint)
                canvas.drawText("Nilai Terverifikasi (IDR)", MARGIN + 380f, y + 14f, bodyBoldPaint)
                y += 20f

                val rows = listOf(
                    "Pendapatan Kas Terealisasi (Realized Revenue)" to IdrFormatter.format(businessPl.realizedRevenueIdr),
                    "Beban Operasional (Operating Expenses)" to IdrFormatter.format(businessPl.operatingExpensesIdr),
                    "Beban Pajak & Retribusi (Tax Expenses)" to IdrFormatter.format(businessPl.taxExpensesIdr),
                    "Laba / Rugi Bersih Kas (Net Profit)" to IdrFormatter.format(businessPl.netProfitIdr, showPlusSign = true),
                    "Piutang Klien Belum Tertagih (Unpaid Receivables - Non-Kas)" to IdrFormatter.format(businessPl.unpaidReceivablesIdr),
                    "Utang Vendor Belum Dibayar (Unpaid Payables - Non-Kas)" to IdrFormatter.format(businessPl.unpaidPayablesIdr)
                )
                for ((label, valStr) in rows) {
                    ensureSpace(20f)
                    canvas.drawText(label, MARGIN + 8f, y + 14f, bodyPaint)
                    canvas.drawText(valStr, MARGIN + 380f, y + 14f, monoAmountPaint)
                    canvas.drawLine(MARGIN, y + 19f, PAGE_WIDTH - MARGIN, y + 19f, borderPaint)
                    y += 20f
                }
                y += 16f
            }

            // 6. Budgets & Savings Goals
            if (config.includeBudgetsAndGoals && (budgets.isNotEmpty() || goals.isNotEmpty())) {
                ensureSpace(70f)
                canvas.drawText("5. KINERJA ANGGARAN & TARGET TABUNGAN", MARGIN, y, sectionPaint)
                y += 8f
                for (b in budgets) {
                    ensureSpace(20f)
                    canvas.drawText(
                        "Anggaran ${b.budget.name} (${b.budget.categoryName}): ${IdrFormatter.format(b.spentAmountIdr)} / ${IdrFormatter.format(b.budget.limitAmountIdr)} (${b.utilizationPercent}%)",
                        MARGIN + 8f,
                        y + 14f,
                        bodyPaint
                    )
                    y += 18f
                }
                for (g in goals) {
                    ensureSpace(20f)
                    val pct = if (g.targetAmountIdr > 0) ((g.currentSavedIdr * 100) / g.targetAmountIdr) else 0
                    canvas.drawText(
                        "Target Tabungan ${g.name}: ${IdrFormatter.format(g.currentSavedIdr)} / ${IdrFormatter.format(g.targetAmountIdr)} ($pct%)",
                        MARGIN + 8f,
                        y + 14f,
                        bodyPaint
                    )
                    y += 18f
                }
                y += 12f
            }

            // 7. Filtered Transaction Ledger (with repeating table header across pages)
            if (config.includeTransactions) {
                val drawLedgerHeader: (Canvas) -> Unit = { c ->
                    c.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, headerBgPaint)
                    c.drawText("Tanggal", MARGIN + 6f, y + 14f, bodyBoldPaint)
                    c.drawText("Deskripsi Transaksi", MARGIN + 72f, y + 14f, bodyBoldPaint)
                    c.drawText("Kategori", MARGIN + 235f, y + 14f, bodyBoldPaint)
                    c.drawText("Akun", MARGIN + 335f, y + 14f, bodyBoldPaint)
                    c.drawText("Nominal (IDR)", MARGIN + 425f, y + 14f, bodyBoldPaint)
                    y += 20f
                }

                ensureSpace(60f)
                canvas.drawText("6. BUKU BESAR TRANSAKSI TERVERIFIKASI (${transactions.size} ENTRI)", MARGIN, y, sectionPaint)
                y += 8f
                drawLedgerHeader(canvas)

                if (transactions.isEmpty()) {
                    canvas.drawText("Belum ada transaksi tercatat pada periode ini.", MARGIN + 8f, y + 14f, subtitlePaint)
                    y += 22f
                } else {
                    for (tx in transactions) {
                        ensureSpace(20f, onNewPageHeader = drawLedgerHeader)
                        val dateStr = shortDateFormat.format(Date(tx.transactionDateMillis))
                        val signPrefix = when (tx.type.name) {
                            "INCOME", "REFUND" -> "+"
                            "EXPENSE" -> "-"
                            else -> ""
                        }
                        canvas.drawText(dateStr, MARGIN + 6f, y + 14f, bodyPaint)
                        canvas.drawText(tx.title.take(26), MARGIN + 72f, y + 14f, bodyPaint)
                        canvas.drawText(tx.categoryName.take(16), MARGIN + 235f, y + 14f, bodyPaint)
                        canvas.drawText(tx.accountName.take(14), MARGIN + 335f, y + 14f, bodyPaint)
                        canvas.drawText("$signPrefix${IdrFormatter.format(tx.amountIdr)}", MARGIN + 425f, y + 14f, monoAmountPaint)
                        canvas.drawLine(MARGIN, y + 19f, PAGE_WIDTH - MARGIN, y + 19f, borderPaint)
                        y += 20f
                    }
                }
                y += 16f
            }

            // 8. Reconciliation Notes & Signatory Block
            ensureSpace(85f)
            canvas.drawText("Catatan Rekonsiliasi: ${config.customNotes.take(110)}", MARGIN, y + 12f, subtitlePaint)
            val sigX = PAGE_WIDTH - MARGIN - 160f
            canvas.drawText("Disahkan Oleh:", sigX, y + 28f, bodyBoldPaint)
            canvas.drawLine(sigX, y + 64f, PAGE_WIDTH - MARGIN, y + 64f, borderPaint)
            val signatoryName = prefs.reportSignatory.ifBlank { prefs.displayName.ifBlank { "Pemilik Akun XynoosFinance" } }
            canvas.drawText(signatoryName, sigX, y + 76f, bodyBoldPaint)

            drawPageFooter(canvas, currentPageNumber)
            currentPage?.let { page -> runCatching { document?.finishPage(page) } }

            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val safeFileName = "XynoosFinance_Laporan_${System.currentTimeMillis()}.pdf"
            val outputFile = File(reportsDir, safeFileName)
            try {
                FileOutputStream(outputFile).use { out ->
                    runCatching { document?.writeTo(out) }
                }
            } finally {
                runCatching { document?.close() }
            }

            if (outputFile.length() == 0L) {
                val fallbackPdf = buildString {
                    appendLine("%PDF-1.4")
                    appendLine("% XynoosFinance Multi-Page Report ($currentPageNumber pages)")
                    appendLine("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj")
                    appendLine("2 0 obj << /Type /Pages /Count $currentPageNumber >> endobj")
                    appendLine("% Title: ${config.reportTitle} | Org: ${prefs.reportOrgName}")
                    appendLine("% NetWorth: ${overview.netWorthIdr} | Income: ${overview.periodIncomeIdr} | Expense: ${overview.periodExpenseIdr}")
                    appendLine("%%EOF")
                }
                outputFile.writeText(fallbackPdf)
            }

            Result.success(
                GeneratedPdfInfo(
                    file = outputFile,
                    pageCount = currentPageNumber,
                    generatedAtFormatted = timestampStr,
                    totalIncomeIdr = overview.periodIncomeIdr,
                    totalExpenseIdr = overview.periodExpenseIdr,
                    netWorthIdr = overview.netWorthIdr
                )
            )
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: e.toString(), e))
        }
    }

    fun sharePdfReport(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan XynoosFinance")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan PDF XynoosFinance"))
    }
}
