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
import com.example.domain.AccountActivitySummary
import com.example.domain.AnnualLedgerSummary
import com.example.domain.BudgetProgressSummary
import com.example.domain.BusinessProfitLossSummary
import com.example.domain.CategoryBreakdownItem
import com.example.domain.FinancialAccountModel
import com.example.domain.FinancialCalculator
import com.example.domain.FinancialGoalModel
import com.example.domain.FinancialOverviewSummary
import com.example.domain.IdrFormatter
import com.example.domain.InvoiceReferenceModel
import com.example.domain.TransactionModel
import com.example.domain.TransactionType
import com.example.domain.TransactionTypeSummaryItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PdfReportScope(val labelId: String) {
    SELECTED_MONTH("Bulan Terpilih"),
    FULL_YEAR("Tahunan Lengkap"),
    DASHBOARD_PERIOD("Periode Dashboard")
}

data class PdfReportConfig(
    val reportTitle: String = "Laporan Eksekutif Keuangan Rinci & Buku Besar",
    val periodLabel: String = "Bulan Ini",
    val scope: PdfReportScope = PdfReportScope.DASHBOARD_PERIOD,
    val customNotes: String = "Seluruh angka dan rincian dalam laporan ini dihitung secara deterministik dari transaksi terverifikasi di XynoosFinance.",
    val includeTransactions: Boolean = true,
    val includeCategoryAnalysis: Boolean = true,
    val includeAnnualMonthlyBreakdown: Boolean = true,
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
 * Generates a detailed, print-friendly, multi-page A4 financial report using Android's native PdfDocument API.
 * Includes per-type breakdown, 12-month annual summary, account cash flows, income & expense category breakdown,
 * budgets/goals, and detailed transaction ledger rows with full timestamps, merchants, projects, and notes.
 */
object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    fun generateFinancialReportPdf(
        context: Context,
        config: PdfReportConfig,
        prefs: UserPreferencesState,
        overview: FinancialOverviewSummary,
        accounts: List<FinancialAccountModel>,
        transactions: List<TransactionModel>,
        categoryBreakdown: List<CategoryBreakdownItem>,
        incomeCategoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
        typeBreakdown: List<TransactionTypeSummaryItem> = emptyList(),
        accountActivities: List<AccountActivitySummary> = emptyList(),
        annualSummary: AnnualLedgerSummary? = null,
        usdRateIdr: Double = 15850.0,
        budgets: List<BudgetProgressSummary>,
        goals: List<FinancialGoalModel>,
        businessPl: BusinessProfitLossSummary,
        invoices: List<InvoiceReferenceModel>
    ): Result<GeneratedPdfInfo> {
        return try {
            val document = runCatching { PdfDocument() }.getOrNull()
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
            val fullTxDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
            val timestampStr = dateFormat.format(Date())

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(13, 20, 31)
                textSize = 17f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(79, 93, 115)
                textSize = 9f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(11, 138, 86)
                textSize = 11.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(20, 28, 40)
                textSize = 9f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val bodySmallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(79, 93, 115)
                textSize = 8f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(13, 20, 31)
                textSize = 9f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val monoAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(13, 20, 31)
                textSize = 9f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            val incomeMonoPaint = Paint(monoAmountPaint).apply { color = Color.rgb(11, 138, 86) }
            val expenseMonoPaint = Paint(monoAmountPaint).apply { color = Color.rgb(217, 45, 57) }
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
                val footerY = PAGE_HEIGHT - 22f
                c.drawLine(MARGIN, footerY - 11f, PAGE_WIDTH - MARGIN, footerY - 11f, borderPaint)
                c.drawText(
                    prefs.reportFooterText.take(88),
                    MARGIN,
                    footerY,
                    subtitlePaint
                )
                val pageLabel = "Halaman $pageNum • $timestampStr"
                val w = subtitlePaint.measureText(pageLabel)
                c.drawText(pageLabel, PAGE_WIDTH - MARGIN - w, footerY, subtitlePaint)
            }

            fun ensureSpace(requiredHeight: Float, onNewPageHeader: ((Canvas) -> Unit)? = null) {
                if (y + requiredHeight > PAGE_HEIGHT - 48f) {
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

            // Compute period specific totals from passed transactions if scope is monthly/annual
            val scopeIncome = transactions.filter { !it.isDeleted && it.type == TransactionType.INCOME }.sumOf { it.amountIdr }
            val scopeGrossExp = transactions.filter { !it.isDeleted && it.type == TransactionType.EXPENSE }.sumOf { it.amountIdr }
            val scopeRefund = transactions.filter { !it.isDeleted && it.type == TransactionType.REFUND }.sumOf { it.amountIdr }
            val scopeNetExp = (scopeGrossExp - scopeRefund).coerceAtLeast(0L)
            val scopeNetCashFlow = scopeIncome - scopeNetExp

            // 1. Cover Header & Organization Identity
            canvas.drawRect(MARGIN, y, MARGIN + 6f, y + 50f, accentBarPaint)
            canvas.drawText("XYNOOSFINANCE — ${prefs.reportOrgName.uppercase(Locale.ROOT)}", MARGIN + 14f, y + 14f, sectionPaint)
            canvas.drawText(config.reportTitle, MARGIN + 14f, y + 32f, titlePaint)
            canvas.drawText(
                "Periode: ${config.periodLabel}  |  Kurs USD: ${IdrFormatter.format(usdRateIdr.toLong())}  |  ${prefs.reportOrgAddress} (${prefs.reportContactInfo})",
                MARGIN + 14f,
                y + 47f,
                subtitlePaint
            )
            y += 62f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, borderPaint)
            y += 14f

            // 2. Executive Summary Metrics Box
            canvas.drawText("1. RINGKASAN EKSEKUTIF KEUANGAN (${config.periodLabel.uppercase(Locale.ROOT)})", MARGIN, y, sectionPaint)
            y += 8f
            val summaryBox = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 82f)
            canvas.drawRoundRect(summaryBox, 6f, 6f, headerBgPaint)
            canvas.drawRoundRect(summaryBox, 6f, 6f, borderPaint)

            val colWidth = CONTENT_WIDTH / 3f
            // Column 1
            canvas.drawText("Kas Tersedia (Likuid)", MARGIN + 10f, y + 18f, subtitlePaint)
            canvas.drawText(IdrFormatter.format(overview.availableCashIdr), MARGIN + 10f, y + 32f, monoAmountPaint)
            canvas.drawText("Kekayaan Bersih (Net Worth)", MARGIN + 10f, y + 50f, subtitlePaint)
            canvas.drawText(IdrFormatter.format(overview.netWorthIdr), MARGIN + 10f, y + 64f, monoAmountPaint)
            canvas.drawText("≈ ${IdrFormatter.formatUsdFromIdr(overview.netWorthIdr, usdRateIdr)}", MARGIN + 10f, y + 76f, bodySmallPaint)

            // Column 2
            canvas.drawText("Pemasukan (${config.periodLabel})", MARGIN + colWidth + 10f, y + 18f, subtitlePaint)
            canvas.drawText(IdrFormatter.format(scopeIncome), MARGIN + colWidth + 10f, y + 32f, incomeMonoPaint)
            canvas.drawText("Pengeluaran Bersih", MARGIN + colWidth + 10f, y + 50f, subtitlePaint)
            canvas.drawText(IdrFormatter.format(scopeNetExp), MARGIN + colWidth + 10f, y + 64f, expenseMonoPaint)
            canvas.drawText("Total ${transactions.size} transaksi tercatat", MARGIN + colWidth + 10f, y + 76f, bodySmallPaint)

            // Column 3
            canvas.drawText("Arus Kas Bersih Periode", MARGIN + colWidth * 2 + 10f, y + 18f, subtitlePaint)
            canvas.drawText(
                IdrFormatter.format(scopeNetCashFlow, showPlusSign = true),
                MARGIN + colWidth * 2 + 10f,
                y + 32f,
                if (scopeNetCashFlow >= 0L) incomeMonoPaint else expenseMonoPaint
            )
            canvas.drawText("Aset / Kewajiban", MARGIN + colWidth * 2 + 10f, y + 50f, subtitlePaint)
            canvas.drawText(
                "${IdrFormatter.formatCompact(overview.totalAssetsIdr)} / ${IdrFormatter.formatCompact(overview.totalLiabilitiesIdr)}",
                MARGIN + colWidth * 2 + 10f,
                y + 64f,
                monoAmountPaint
            )
            val savingsRate = if (scopeIncome > 0L) (((scopeIncome - scopeNetExp).coerceAtLeast(0L) * 100L) / scopeIncome).toInt() else 0
            canvas.drawText("Rasio Simpanan: $savingsRate%", MARGIN + colWidth * 2 + 10f, y + 76f, bodySmallPaint)
            y += 98f

            // 3. Detailed Breakdown by Transaction Type
            val effectiveTypeBreakdown = typeBreakdown.ifEmpty {
                FinancialCalculator.calculateTypeBreakdown(transactions)
            }
            ensureSpace(120f)
            canvas.drawText("2. REKAPITULASI RINCI PER JENIS TRANSAKSI", MARGIN, y, sectionPaint)
            y += 8f
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, headerBgPaint)
            canvas.drawText("Jenis Transaksi", MARGIN + 8f, y + 14f, bodyBoldPaint)
            canvas.drawText("Frekuensi", MARGIN + 210f, y + 14f, bodyBoldPaint)
            canvas.drawText("Porsi Volume", MARGIN + 305f, y + 14f, bodyBoldPaint)
            canvas.drawText("Total Nominal (IDR)", MARGIN + 400f, y + 14f, bodyBoldPaint)
            y += 20f

            for (item in effectiveTypeBreakdown) {
                ensureSpace(20f)
                canvas.drawText(item.type.labelId, MARGIN + 8f, y + 14f, bodyPaint)
                canvas.drawText("${item.transactionCount} transaksi", MARGIN + 210f, y + 14f, bodyPaint)
                canvas.drawText(String.format(Locale.US, "%.1f%%", item.percentageOfVolume), MARGIN + 305f, y + 14f, bodyPaint)
                canvas.drawText(IdrFormatter.format(item.totalAmountIdr), MARGIN + 400f, y + 14f, monoAmountPaint)
                canvas.drawLine(MARGIN, y + 19f, PAGE_WIDTH - MARGIN, y + 19f, borderPaint)
                y += 20f
            }
            y += 14f

            // 4. 12-Month Annual Breakdown Table
            if (config.includeAnnualMonthlyBreakdown && annualSummary != null) {
                ensureSpace(110f)
                canvas.drawText("3. REKAPITULASI TAHUNAN PER BULAN (TAHUN ${annualSummary.year})", MARGIN, y, sectionPaint)
                y += 8f
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, headerBgPaint)
                canvas.drawText("Bulan", MARGIN + 8f, y + 14f, bodyBoldPaint)
                canvas.drawText("Tx", MARGIN + 115f, y + 14f, bodyBoldPaint)
                canvas.drawText("Pemasukan", MARGIN + 165f, y + 14f, bodyBoldPaint)
                canvas.drawText("Pengeluaran", MARGIN + 295f, y + 14f, bodyBoldPaint)
                canvas.drawText("Surplus/Defisit", MARGIN + 425f, y + 14f, bodyBoldPaint)
                y += 20f

                for (m in annualSummary.monthlyBreakdown) {
                    ensureSpace(18f)
                    canvas.drawText(FinancialCalculator.getMonthNameFull(m.monthIndex), MARGIN + 8f, y + 13f, bodyPaint)
                    canvas.drawText("${m.transactionCount}", MARGIN + 115f, y + 13f, bodyPaint)
                    canvas.drawText(IdrFormatter.format(m.incomeIdr), MARGIN + 165f, y + 13f, incomeMonoPaint)
                    canvas.drawText(IdrFormatter.format(m.netExpenseIdr), MARGIN + 295f, y + 13f, expenseMonoPaint)
                    canvas.drawText(
                        IdrFormatter.format(m.netCashFlowIdr, showPlusSign = true),
                        MARGIN + 425f,
                        y + 13f,
                        if (m.netCashFlowIdr >= 0L) incomeMonoPaint else expenseMonoPaint
                    )
                    canvas.drawLine(MARGIN, y + 17f, PAGE_WIDTH - MARGIN, y + 17f, borderPaint)
                    y += 18f
                }

                // Grand Total Annual Row
                ensureSpace(22f)
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, headerBgPaint)
                canvas.drawText("TOTAL ${annualSummary.year}", MARGIN + 8f, y + 14f, bodyBoldPaint)
                canvas.drawText("${annualSummary.transactionCount}", MARGIN + 115f, y + 14f, bodyBoldPaint)
                canvas.drawText(IdrFormatter.format(annualSummary.totalIncomeIdr), MARGIN + 165f, y + 14f, incomeMonoPaint)
                canvas.drawText(IdrFormatter.format(annualSummary.totalNetExpenseIdr), MARGIN + 295f, y + 14f, expenseMonoPaint)
                canvas.drawText(
                    IdrFormatter.format(annualSummary.netCashFlowIdr, showPlusSign = true),
                    MARGIN + 425f,
                    y + 14f,
                    monoAmountPaint
                )
                y += 32f
            }

            // 5. Accounts & Wallets Breakdown (with Inflow/Outflow Activity)
            val activities = accountActivities.ifEmpty {
                FinancialCalculator.calculateAccountActivities(accounts, transactions)
            }
            ensureSpace(80f)
            canvas.drawText("4. POSISI & MUTASI REKENING / DOMPET (${accounts.size} AKUN)", MARGIN, y, sectionPaint)
            y += 8f
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, headerBgPaint)
            canvas.drawText("Nama Akun (Tipe)", MARGIN + 8f, y + 14f, bodyBoldPaint)
            canvas.drawText("Masuk Periode", MARGIN + 185f, y + 14f, bodyBoldPaint)
            canvas.drawText("Keluar Periode", MARGIN + 295f, y + 14f, bodyBoldPaint)
            canvas.drawText("Saldo Akhir", MARGIN + 410f, y + 14f, bodyBoldPaint)
            y += 20f

            for (act in activities) {
                ensureSpace(20f)
                val acc = act.account
                canvas.drawText("${acc.name.take(18)} (${acc.accountType.name})", MARGIN + 8f, y + 14f, bodyPaint)
                canvas.drawText(IdrFormatter.format(act.inflowIdr), MARGIN + 185f, y + 14f, incomeMonoPaint)
                canvas.drawText(IdrFormatter.format(act.outflowIdr), MARGIN + 295f, y + 14f, expenseMonoPaint)
                canvas.drawText(IdrFormatter.format(acc.currentBalanceIdr), MARGIN + 410f, y + 14f, monoAmountPaint)
                canvas.drawLine(MARGIN, y + 19f, PAGE_WIDTH - MARGIN, y + 19f, borderPaint)
                y += 20f
            }
            y += 14f

            // 6. Detailed Category Analysis (Expenses & Incomes)
            if (config.includeCategoryAnalysis) {
                ensureSpace(70f)
                canvas.drawText("5. RINCIAN PENGELUARAN PER KATEGORI", MARGIN, y, sectionPaint)
                y += 8f
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, headerBgPaint)
                canvas.drawText("Kategori Pengeluaran", MARGIN + 8f, y + 14f, bodyBoldPaint)
                canvas.drawText("Jml Tx", MARGIN + 185f, y + 14f, bodyBoldPaint)
                canvas.drawText("Rata-rata / Tx", MARGIN + 250f, y + 14f, bodyBoldPaint)
                canvas.drawText("Porsi", MARGIN + 355f, y + 14f, bodyBoldPaint)
                canvas.drawText("Total Pengeluaran", MARGIN + 410f, y + 14f, bodyBoldPaint)
                y += 20f

                if (categoryBreakdown.isEmpty()) {
                    canvas.drawText("Tidak ada pengeluaran pada periode ini.", MARGIN + 8f, y + 14f, subtitlePaint)
                    y += 22f
                } else {
                    for (item in categoryBreakdown) {
                        ensureSpace(20f)
                        val avg = if (item.transactionCount > 0) item.amountIdr / item.transactionCount else item.amountIdr
                        canvas.drawText(item.categoryName.take(26), MARGIN + 8f, y + 14f, bodyPaint)
                        canvas.drawText("${item.transactionCount}x", MARGIN + 185f, y + 14f, bodyPaint)
                        canvas.drawText(IdrFormatter.format(avg), MARGIN + 250f, y + 14f, bodyPaint)
                        canvas.drawText(String.format(Locale.US, "%.1f%%", item.percentage), MARGIN + 355f, y + 14f, bodyPaint)
                        canvas.drawText(IdrFormatter.format(item.amountIdr), MARGIN + 410f, y + 14f, expenseMonoPaint)
                        canvas.drawLine(MARGIN, y + 19f, PAGE_WIDTH - MARGIN, y + 19f, borderPaint)
                        y += 20f
                    }
                }
                y += 14f

                // Income Category Breakdown
                val effectiveIncomeBreakdown = incomeCategoryBreakdown.ifEmpty {
                    FinancialCalculator.calculateIncomeCategoryBreakdown(transactions, emptyList())
                }
                if (effectiveIncomeBreakdown.isNotEmpty()) {
                    ensureSpace(70f)
                    canvas.drawText("6. RINCIAN PEMASUKAN PER KATEGORI", MARGIN, y, sectionPaint)
                    y += 8f
                    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, headerBgPaint)
                    canvas.drawText("Kategori Pemasukan", MARGIN + 8f, y + 14f, bodyBoldPaint)
                    canvas.drawText("Jml Tx", MARGIN + 185f, y + 14f, bodyBoldPaint)
                    canvas.drawText("Rata-rata / Tx", MARGIN + 250f, y + 14f, bodyBoldPaint)
                    canvas.drawText("Porsi", MARGIN + 355f, y + 14f, bodyBoldPaint)
                    canvas.drawText("Total Pemasukan", MARGIN + 410f, y + 14f, bodyBoldPaint)
                    y += 20f

                    for (item in effectiveIncomeBreakdown) {
                        ensureSpace(20f)
                        val avg = if (item.transactionCount > 0) item.amountIdr / item.transactionCount else item.amountIdr
                        canvas.drawText(item.categoryName.take(26), MARGIN + 8f, y + 14f, bodyPaint)
                        canvas.drawText("${item.transactionCount}x", MARGIN + 185f, y + 14f, bodyPaint)
                        canvas.drawText(IdrFormatter.format(avg), MARGIN + 250f, y + 14f, bodyPaint)
                        canvas.drawText(String.format(Locale.US, "%.1f%%", item.percentage), MARGIN + 355f, y + 14f, bodyPaint)
                        canvas.drawText(IdrFormatter.format(item.amountIdr), MARGIN + 410f, y + 14f, incomeMonoPaint)
                        canvas.drawLine(MARGIN, y + 19f, PAGE_WIDTH - MARGIN, y + 19f, borderPaint)
                        y += 20f
                    }
                    y += 14f
                }
            }

            // 7. Business Profit & Loss + Receivables/Payables
            if (config.includeBusinessProfitLoss) {
                ensureSpace(95f)
                canvas.drawText("7. RINGKASAN LABA RUGI & PIUTANG/UTANG BISNIS", MARGIN, y, sectionPaint)
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
                    "Piutang Klien Belum Tertagih (${invoices.count { it.invoiceType.name == "RECEIVABLE" }} Faktur)" to IdrFormatter.format(businessPl.unpaidReceivablesIdr),
                    "Utang Vendor Belum Dibayar (${invoices.count { it.invoiceType.name == "PAYABLE" }} Faktur)" to IdrFormatter.format(businessPl.unpaidPayablesIdr)
                )
                for ((label, valStr) in rows) {
                    ensureSpace(20f)
                    canvas.drawText(label, MARGIN + 8f, y + 14f, bodyPaint)
                    canvas.drawText(valStr, MARGIN + 380f, y + 14f, monoAmountPaint)
                    canvas.drawLine(MARGIN, y + 19f, PAGE_WIDTH - MARGIN, y + 19f, borderPaint)
                    y += 20f
                }
                y += 14f
            }

            // 8. Budgets & Savings Goals (Detailed)
            if (config.includeBudgetsAndGoals && (budgets.isNotEmpty() || goals.isNotEmpty())) {
                ensureSpace(70f)
                canvas.drawText("8. RINCIAN KINERJA ANGGARAN & TARGET TABUNGAN", MARGIN, y, sectionPaint)
                y += 8f
                for (b in budgets) {
                    ensureSpace(20f)
                    canvas.drawText(
                        "• Anggaran ${b.budget.name} (${b.budget.categoryName}): Terpakai ${IdrFormatter.format(b.spentAmountIdr)} / Batas ${IdrFormatter.format(b.budget.limitAmountIdr)} • Sisa ${IdrFormatter.format(b.remainingAmountIdr)} (${b.utilizationPercent}%)",
                        MARGIN + 8f,
                        y + 14f,
                        bodyPaint
                    )
                    y += 18f
                }
                for (g in goals) {
                    ensureSpace(20f)
                    val effectiveSaved = FinancialCalculator.calculateEffectiveGoalSavedIdr(
                        goal = g,
                        availableCashIdr = overview.availableCashIdr,
                        totalAssetsIdr = overview.totalAssetsIdr
                    )
                    val pct = if (g.targetAmountIdr > 0) ((effectiveSaved * 100) / g.targetAmountIdr).coerceAtMost(100) else 0
                    canvas.drawText(
                        "• Target ${g.name} (Auto-Saldo): ${IdrFormatter.format(effectiveSaved)} / ${IdrFormatter.format(g.targetAmountIdr)} ($pct%)",
                        MARGIN + 8f,
                        y + 14f,
                        bodyPaint
                    )
                    y += 18f
                }
                y += 12f
            }

            // 9. Detailed Transaction Ledger (with full date+year+time, category, account, merchant/project/notes)
            if (config.includeTransactions) {
                val drawLedgerHeader: (Canvas) -> Unit = { c ->
                    c.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, headerBgPaint)
                    c.drawText("Waktu & Tahun", MARGIN + 6f, y + 14f, bodyBoldPaint)
                    c.drawText("Deskripsi & Catatan", MARGIN + 96f, y + 14f, bodyBoldPaint)
                    c.drawText("Jenis / Kategori", MARGIN + 250f, y + 14f, bodyBoldPaint)
                    c.drawText("Rekening", MARGIN + 350f, y + 14f, bodyBoldPaint)
                    c.drawText("Nominal (IDR)", MARGIN + 430f, y + 14f, bodyBoldPaint)
                    y += 20f
                }

                ensureSpace(60f)
                canvas.drawText("9. BUKU BESAR TRANSAKSI RINCI (${transactions.size} ENTRI)", MARGIN, y, sectionPaint)
                y += 8f
                drawLedgerHeader(canvas)

                if (transactions.isEmpty()) {
                    canvas.drawText("Belum ada transaksi tercatat pada periode ini.", MARGIN + 8f, y + 14f, subtitlePaint)
                    y += 22f
                } else {
                    for (tx in transactions) {
                        val hasMeta = tx.notes.isNotBlank() || tx.merchantName.isNotBlank() || tx.projectCostCenterId.isNotBlank()
                        val rowHeight = if (hasMeta) 30f else 20f
                        ensureSpace(rowHeight, onNewPageHeader = drawLedgerHeader)

                        val dateStr = fullTxDateFormat.format(Date(tx.transactionDateMillis))
                        val signPrefix = when (tx.type) {
                            TransactionType.INCOME, TransactionType.REFUND -> "+"
                            TransactionType.EXPENSE -> "-"
                            else -> ""
                        }
                        val amtPaint = when (tx.type) {
                            TransactionType.INCOME, TransactionType.REFUND -> incomeMonoPaint
                            TransactionType.EXPENSE -> expenseMonoPaint
                            else -> monoAmountPaint
                        }
                        val accountDisplay = if (tx.type == TransactionType.TRANSFER && !tx.toAccountName.isNullOrBlank()) {
                            "${tx.accountName.take(8)}→${tx.toAccountName.take(8)}"
                        } else {
                            tx.accountName.take(15)
                        }

                        canvas.drawText(dateStr, MARGIN + 6f, y + 13f, bodySmallPaint)
                        canvas.drawText(tx.title.take(26), MARGIN + 96f, y + 13f, bodyBoldPaint)
                        canvas.drawText("${tx.type.labelId.take(6)} • ${tx.categoryName.take(13)}", MARGIN + 250f, y + 13f, bodyPaint)
                        canvas.drawText(accountDisplay, MARGIN + 350f, y + 13f, bodyPaint)
                        canvas.drawText("$signPrefix${IdrFormatter.format(tx.amountIdr)}", MARGIN + 430f, y + 13f, amtPaint)

                        if (hasMeta) {
                            val metaParts = listOfNotNull(
                                tx.merchantName.takeIf { it.isNotBlank() }?.let { "Merchant: $it" },
                                tx.projectCostCenterId.takeIf { it.isNotBlank() }?.let { "Proyek: $it" },
                                tx.notes.takeIf { it.isNotBlank() }?.let { "Catatan: $it" }
                            ).joinToString(" | ")
                            canvas.drawText(metaParts.take(78), MARGIN + 96f, y + 25f, bodySmallPaint)
                        }

                        canvas.drawLine(MARGIN, y + rowHeight - 1f, PAGE_WIDTH - MARGIN, y + rowHeight - 1f, borderPaint)
                        y += rowHeight
                    }
                }
                y += 14f
            }

            // 10. Reconciliation Notes & Signatory Block
            ensureSpace(85f)
            canvas.drawText("Catatan Rekonsiliasi: ${config.customNotes.take(110)}", MARGIN, y + 12f, subtitlePaint)
            val sigX = PAGE_WIDTH - MARGIN - 160f
            canvas.drawText("Disahkan Oleh:", sigX, y + 28f, bodyBoldPaint)
            canvas.drawLine(sigX, y + 62f, PAGE_WIDTH - MARGIN, y + 62f, borderPaint)
            val signatoryName = prefs.reportSignatory.ifBlank { prefs.displayName.ifBlank { "Pemilik Akun XynoosFinance" } }
            canvas.drawText(signatoryName, sigX, y + 74f, bodyBoldPaint)

            drawPageFooter(canvas, currentPageNumber)
            currentPage?.let { page -> runCatching { document?.finishPage(page) } }

            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val safeFileName = "XynoosFinance_Laporan_Rinci_${System.currentTimeMillis()}.pdf"
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
                    appendLine("% XynoosFinance Detailed Multi-Page Report ($currentPageNumber pages)")
                    appendLine("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj")
                    appendLine("2 0 obj << /Type /Pages /Count $currentPageNumber >> endobj")
                    appendLine("% Title: ${config.reportTitle} | Org: ${prefs.reportOrgName}")
                    appendLine("% NetWorth: ${overview.netWorthIdr} | Income: $scopeIncome | Expense: $scopeNetExp")
                    appendLine("%%EOF")
                }
                outputFile.writeText(fallbackPdf)
            }

            Result.success(
                GeneratedPdfInfo(
                    file = outputFile,
                    pageCount = currentPageNumber,
                    generatedAtFormatted = timestampStr,
                    totalIncomeIdr = scopeIncome,
                    totalExpenseIdr = scopeNetExp,
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
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan Rinci XynoosFinance")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan PDF XynoosFinance"))
    }
}
