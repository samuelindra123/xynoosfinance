package com.example.ai

import android.util.Log
import com.example.data.remote.LiveUsdMarketState
import com.example.domain.AiMessageModel
import com.example.domain.AnnualLedgerSummary
import com.example.domain.BudgetAlertLevel
import com.example.domain.BudgetProgressSummary
import com.example.domain.CategoryBreakdownItem
import com.example.domain.DashboardPeriod
import com.example.domain.FinancialAccountModel
import com.example.domain.FinancialCalculator
import com.example.domain.FinancialGoalModel
import com.example.domain.FinancialOverviewSummary
import com.example.domain.IdrFormatter
import com.example.domain.MonthlyLedgerSummary
import com.example.domain.ProposedTransactionDraft
import com.example.domain.TransactionCategoryModel
import com.example.domain.TransactionModel
import com.example.domain.TransactionType
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Locale

private const val TAG = "XynoosAiAssistant"

data class GroundedAiResult(
    val explanationMarkdown: String,
    val actionType: String, // "SUMMARY", "SPENDING_TREND", "BUDGET_ADVICE", "GOAL_PROJECTION", "DRAFT_TRANSACTION", "GENERAL_QA"
    val proposedDraft: ProposedTransactionDraft? = null,
    val isFromLocalFallback: Boolean = false
)

/**
 * Grounded Xynoos AI Finance Assistant & Advisor.
 * Uses Firebase AI Logic (`gemini-3.5-flash-lite`) with App Check attestation and
 * structured `Schema.obj(...)` JSON output, backed by deterministic repository calculations,
 * multi-turn session history, monthly/annual ledger context, and live USD/IDR exchange rates.
 */
class GroundedAiAssistant {

    private val structuredSchema = Schema.obj(
        properties = mapOf(
            "explanation" to Schema.string(description = "Grounded financial explanation, actionable recommendation, and Q&A answer in natural Indonesian"),
            "actionType" to Schema.enumeration(
                values = listOf("SUMMARY", "SPENDING_TREND", "BUDGET_ADVICE", "GOAL_PROJECTION", "DRAFT_TRANSACTION", "GENERAL_QA"),
                description = "Category of financial action or Q&A"
            ),
            "hasProposedTransaction" to Schema.boolean(description = "True ONLY if the user message explicitly asks to record an income or expense transaction"),
            "txType" to Schema.string(description = "INCOME or EXPENSE if hasProposedTransaction is true"),
            "txAmountIdr" to Schema.integer(description = "Transaction amount in integer Rupiah if hasProposedTransaction is true"),
            "txTitle" to Schema.string(description = "Short description of the proposed transaction"),
            "txCategoryName" to Schema.string(description = "Matched category name"),
            "txAccountName" to Schema.string(description = "Matched account name")
        ),
        optionalProperties = listOf("txType", "txAmountIdr", "txTitle", "txCategoryName", "txAccountName")
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = structuredSchema
            }
        )
    }

    private val streamingModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.5-flash-lite")
    }

    /**
     * Deterministically parses natural Indonesian transaction input such as:
     * "Beli bensin pertamax 50 ribu pakai gopay", "Beli jajan seblak 25rb", or "Terima gaji pkl 1.5 juta ke BCA"
     */
    fun parseNaturalTransactionDraft(
        prompt: String,
        accounts: List<FinancialAccountModel>,
        categories: List<TransactionCategoryModel>
    ): ProposedTransactionDraft? {
        val lower = prompt.lowercase(Locale.ROOT).trim()
        if (lower.startsWith("apa") || lower.startsWith("bagaimana") || lower.startsWith("gimana") ||
            lower.startsWith("kenapa") || lower.startsWith("berapa") || lower.startsWith("jelaskan") ||
            lower.startsWith("kapan") || lower.startsWith("tips") || lower.startsWith("cara") ||
            lower.startsWith("audit") || lower.startsWith("evaluasi") || lower.startsWith("analis") ||
            lower.contains("?")
        ) {
            return null
        }

        val isExpenseTrigger = listOf(
            "bayar", "beli", "makan", "jajan", "ngopi", "kopi", "ongkos", "bensin", "bbm",
            "pertalite", "pertamax", "solar", "tagihan", "keluar", "belanja", "topup", "top up",
            "pulsa", "kuota", "paket data", "ojol", "parkir", "tol", "skincare", "potong rambut",
            "barber", "sedekah", "infaq", "langganan", "nonton", "bioskop"
        ).any { lower.contains(it) }

        val isIncomeTrigger = listOf(
            "terima", "gaji", "pkl", "magang", "masuk", "dapat", "honor", "bonus", "thr",
            "lembur", "uang saku", "kiriman", "jualan", "omzet", "komisi", "cashback",
            "dividen", "invoice cair", "pendapatan", "pemasukan"
        ).any { lower.contains(it) }

        if (!isExpenseTrigger && !isIncomeTrigger) return null

        val regex = Regex("""(\d+[.,]?\d*)\s*(miliar|juta|jt|ribu|rb|k)?""")
        val match = regex.find(lower) ?: return null
        val amountIdr = IdrFormatter.parseIdrInput(match.value) ?: return null
        if (amountIdr <= 0L) return null

        val txType = if (isIncomeTrigger && !isExpenseTrigger) TransactionType.INCOME else TransactionType.EXPENSE

        val matchedCategory = matchCategorySmart(lower, txType, categories)

        val matchedAccount = accounts.firstOrNull { acc ->
            lower.contains(acc.name.lowercase(Locale.ROOT)) ||
                (acc.accountType.name == "CASH" && (lower.contains("cash") || lower.contains("tunai")))
        } ?: accounts.firstOrNull()

        val cleanedTitle = prompt
            .replace(match.value, "", ignoreCase = true)
            .replace(Regex("""(?i)\b(catat|tolong|pakai|menggunakan|ke|dari|dengan|cash|tunai|rp)\b"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            .ifBlank { if (txType == TransactionType.INCOME) "Pemasukan Baru" else "Pengeluaran Baru" }

        return ProposedTransactionDraft(
            type = txType,
            amountIdr = amountIdr,
            title = cleanedTitle,
            categoryName = matchedCategory?.name ?: if (txType == TransactionType.INCOME) "Gaji Pekerjaan" else "Beli Makanan & Lauk",
            accountName = matchedAccount?.name ?: "Tunai (Cash)",
            notes = "Dibuat melalui Xynoos AI dari perintah: \"$prompt\""
        )
    }

    private fun matchCategorySmart(
        lower: String,
        txType: TransactionType,
        categories: List<TransactionCategoryModel>
    ): TransactionCategoryModel? {
        val byId: (String) -> TransactionCategoryModel? = { id ->
            categories.firstOrNull { it.id == id && it.type == txType }
        }
        if (txType == TransactionType.INCOME) {
            return when {
                lower.contains("pkl") || lower.contains("magang") || lower.contains("intern") -> byId("cat_salary_pkl")
                lower.contains("gaji lain") || lower.contains("sampingan") -> byId("cat_salary_other")
                lower.contains("bonus") || lower.contains("thr") || lower.contains("lembur") -> byId("cat_bonus_thr")
                lower.contains("uang saku") || lower.contains("kiriman") || lower.contains("orang tua") -> byId("cat_allowance")
                lower.contains("jualan") || lower.contains("side hustle") || lower.contains("dagang") -> byId("cat_side_hustle")
                lower.contains("cashback") || lower.contains("komisi") || lower.contains("hadiah") -> byId("cat_cashback_prize")
                lower.contains("freelance") || lower.contains("proyek") || lower.contains("klien") -> byId("cat_freelance")
                lower.contains("investasi") || lower.contains("dividen") || lower.contains("saham") -> byId("cat_investment")
                lower.contains("bisnis") || lower.contains("omzet") -> byId("cat_business_rev")
                lower.contains("gaji") -> byId("cat_salary")
                else -> categories.firstOrNull { cat ->
                    cat.type == txType && lower.contains(cat.name.lowercase(Locale.ROOT).split(" ").first())
                } ?: categories.firstOrNull { it.type == txType }
            }
        }

        return when {
            lower.contains("bensin") || lower.contains("bbm") || lower.contains("pertamax") || lower.contains("pertalite") || lower.contains("solar") -> byId("cat_buy_fuel")
            lower.contains("jajan") || lower.contains("camilan") || lower.contains("snack") || lower.contains("seblak") || lower.contains("gorengan") || lower.contains("cimol") -> byId("cat_buy_snack")
            lower.contains("kopi") || lower.contains("ngopi") || lower.contains("boba") || lower.contains("cafe") || lower.contains("nongkrong") || lower.contains("matcha") -> byId("cat_coffee_hangout")
            lower.contains("makan") || lower.contains("lauk") || lower.contains("warteg") || lower.contains("nasi") || lower.contains("bakso") || lower.contains("ayam") -> byId("cat_buy_food")
            lower.contains("ojol") || lower.contains("grab") || lower.contains("gojek") || lower.contains("maxim") || lower.contains("parkir") || lower.contains("tol") -> byId("cat_ojol_parking")
            lower.contains("pulsa") || lower.contains("kuota") || lower.contains("paket data") || lower.contains("wifi") || lower.contains("internet") -> byId("cat_pulsa_data")
            lower.contains("langganan") || lower.contains("netflix") || lower.contains("spotify") || lower.contains("youtube") -> byId("cat_subscriptions")
            lower.contains("skincare") || lower.contains("barber") || lower.contains("potong rambut") || lower.contains("sabun") -> byId("cat_skincare_grooming")
            lower.contains("game") || lower.contains("top up") || lower.contains("topup") || lower.contains("hobi") -> byId("cat_hobby_gaming")
            lower.contains("sedekah") || lower.contains("infaq") || lower.contains("kado") || lower.contains("donasi") -> byId("cat_gift_charity")
            lower.contains("listrik") || lower.contains("token") || lower.contains("air") || lower.contains("pdam") || lower.contains("tagihan") -> byId("cat_utilities")
            lower.contains("kos") || lower.contains("kontrakan") || lower.contains("sewa") -> byId("cat_housing")
            lower.contains("obat") || lower.contains("dokter") || lower.contains("apotek") || lower.contains("klinik") -> byId("cat_health")
            else -> categories.firstOrNull { cat ->
                cat.type == txType && lower.contains(cat.name.lowercase(Locale.ROOT).split(" ").first())
            } ?: categories.firstOrNull { it.type == txType }
        }
    }

    /**
     * Builds a grounded, privacy-minimal financial context from verified repository totals,
     * annual/monthly analytics, account balances, and live USD rate.
     */
    fun buildGroundedContext(
        overview: FinancialOverviewSummary,
        todayTransactions: List<TransactionModel>,
        monthTransactions: List<TransactionModel>,
        categoryBreakdown: List<CategoryBreakdownItem>,
        budgets: List<BudgetProgressSummary>,
        goals: List<FinancialGoalModel>,
        usdMarket: LiveUsdMarketState = LiveUsdMarketState(),
        accounts: List<FinancialAccountModel> = emptyList(),
        monthlySummary: MonthlyLedgerSummary? = null,
        annualSummary: AnnualLedgerSummary? = null
    ): String {
        val todayIncome = todayTransactions.filter { !it.isDeleted && it.type == TransactionType.INCOME }.sumOf { it.amountIdr }
        val todayExpense = todayTransactions.filter { !it.isDeleted && it.type == TransactionType.EXPENSE }.sumOf { it.amountIdr }

        val topCatSummary = if (categoryBreakdown.isEmpty()) {
            "Belum ada pengeluaran tercatat bulan ini."
        } else {
            categoryBreakdown.take(5).joinToString("; ") {
                "${it.categoryName}: ${IdrFormatter.format(it.amountIdr)} (${String.format(Locale.US, "%.1f", it.percentage)}%)"
            }
        }

        val budgetSummary = if (budgets.isEmpty()) {
            "Belum ada anggaran aktif."
        } else {
            budgets.joinToString("; ") {
                "${it.budget.name} (${it.budget.categoryName}): Terpakai ${IdrFormatter.format(it.spentAmountIdr)} dari ${IdrFormatter.format(it.budget.limitAmountIdr)} (${it.utilizationPercent}%, Sisa: ${IdrFormatter.format(it.remainingAmountIdr)})"
            }
        }

        val goalSummary = if (goals.isEmpty()) {
            "Belum ada target tabungan."
        } else {
            goals.joinToString("; ") { g ->
                val effectiveSaved = FinancialCalculator.calculateEffectiveGoalSavedIdr(g, overview.availableCashIdr, overview.totalAssetsIdr)
                "${g.name}: Terkoneksi Saldo ${IdrFormatter.format(effectiveSaved)} dari target ${IdrFormatter.format(g.targetAmountIdr)}"
            }
        }

        val accountsLine = if (accounts.isEmpty()) {
            "Belum ada rekening."
        } else {
            accounts.joinToString("; ") { "${it.name} (${it.accountType.labelId}): ${IdrFormatter.format(it.currentBalanceIdr)}" }
        }

        val monthlyLine = if (monthlySummary != null) {
            "Bulan ${monthlySummary.monthLabel}: Masuk ${IdrFormatter.format(monthlySummary.incomeIdr)}, Abis ${IdrFormatter.format(monthlySummary.netExpenseIdr)}, Surplus ${IdrFormatter.format(monthlySummary.netCashFlowIdr)} (Saving rate ${monthlySummary.savingsRatePercent}%, ${monthlySummary.transactionCount} transaksi)"
        } else {
            "Bulan Berjalan: Masuk ${IdrFormatter.format(overview.periodIncomeIdr)}, Abis ${IdrFormatter.format(overview.periodExpenseIdr)}"
        }

        val annualLine = if (annualSummary != null) {
            "Tahun ${annualSummary.year}: Total Masuk ${IdrFormatter.format(annualSummary.totalIncomeIdr)}, Total Abis ${IdrFormatter.format(annualSummary.totalNetExpenseIdr)}, Sisa/Surplus ${IdrFormatter.format(annualSummary.netCashFlowIdr)} (Saving rate ${annualSummary.savingsRatePercent}%, rata-rata keluar/bln ${IdrFormatter.format(annualSummary.averageMonthlyExpenseIdr)})"
        } else {
            "Belum ada rekap tahunan."
        }

        val recentTxLines = monthTransactions.take(5).joinToString("; ") {
            "${it.title} (${it.type.labelId} ${IdrFormatter.format(it.amountIdr)} - ${it.categoryName})"
        }.ifBlank { "Belum ada transaksi bulan ini." }

        val cashUsd = IdrFormatter.formatUsdFromIdr(overview.availableCashIdr, usdMarket.currentRateIdr)
        val netWorthUsd = IdrFormatter.formatUsdFromIdr(overview.netWorthIdr, usdMarket.currentRateIdr)

        return """
            FAKTA KEUANGAN TERVERIFIKASI & KURS REAL-TIME USD/IDR:
            - Kas Tersedia Saat Ini: ${IdrFormatter.format(overview.availableCashIdr)} (Setara Real-Time: $cashUsd)
            - Total Aset Terlacak: ${IdrFormatter.format(overview.totalAssetsIdr)} | Kewajiban: ${IdrFormatter.format(overview.totalLiabilitiesIdr)}
            - Kekayaan Bersih (Net Worth): ${IdrFormatter.format(overview.netWorthIdr)} (Setara Real-Time: $netWorthUsd)
            - Daftar Saldo Rekening: $accountsLine
            - Kurs Live Spot USD/IDR: ${IdrFormatter.format(usdMarket.currentRateIdr.toLong())} / $1 USD
            - Hari Ini: Masuk ${IdrFormatter.format(todayIncome)}, Keluar ${IdrFormatter.format(todayExpense)}
            - $monthlyLine
            - $annualLine
            - Top Pengeluaran Kategori: $topCatSummary
            - Status Anggaran: $budgetSummary
            - Target Tabungan: $goalSummary
            - 5 Transaksi Terbaru: $recentTxLines
        """.trimIndent()
    }

    /**
     * Generates a deterministic, rich, actionable local answer in Bahasa Indonesia
     * when offline or as structured fallback.
     */
    fun generateDeterministicGroundedAnswer(
        userQuery: String,
        overview: FinancialOverviewSummary,
        todayTransactions: List<TransactionModel>,
        categoryBreakdown: List<CategoryBreakdownItem>,
        budgets: List<BudgetProgressSummary>,
        goals: List<FinancialGoalModel>,
        localDraft: ProposedTransactionDraft?,
        usdMarket: LiveUsdMarketState = LiveUsdMarketState(),
        accounts: List<FinancialAccountModel> = emptyList(),
        monthlySummary: MonthlyLedgerSummary? = null,
        annualSummary: AnnualLedgerSummary? = null
    ): GroundedAiResult {
        if (localDraft != null) {
            return GroundedAiResult(
                explanationMarkdown = "Siap! Saya sudah menyiapkan draf transaksi **${localDraft.type.labelId}** sebesar **${IdrFormatter.format(localDraft.amountIdr)}** untuk **${localDraft.title}** di kategori **${localDraft.categoryName}** (rekening **${localDraft.accountName}**).\n\nCek detail di kartu konfirmasi lalu ketuk **Konfirmasi & Simpan** ya!",
                actionType = "DRAFT_TRANSACTION",
                proposedDraft = localDraft,
                isFromLocalFallback = true
            )
        }

        val lower = userQuery.lowercase(Locale.ROOT)
        val todayIncome = todayTransactions.filter { !it.isDeleted && it.type == TransactionType.INCOME }.sumOf { it.amountIdr }
        val todayExpense = todayTransactions.filter { !it.isDeleted && it.type == TransactionType.EXPENSE }.sumOf { it.amountIdr }
        val cashUsd = IdrFormatter.formatUsdFromIdr(overview.availableCashIdr, usdMarket.currentRateIdr)
        val netWorthUsd = IdrFormatter.formatUsdFromIdr(overview.netWorthIdr, usdMarket.currentRateIdr)

        // Audit & Evaluasi Keuangan Lengkap ("Audit keuangan saya bulan & tahun ini")
        if (lower.contains("audit") || lower.contains("kesehatan") || lower.contains("evaluasi") || lower.contains("tahun ini") || lower.contains("boros")) {
            val savingRate = monthlySummary?.savingsRatePercent
                ?: if (overview.periodIncomeIdr > 0) (((overview.periodIncomeIdr - overview.periodExpenseIdr) * 100) / overview.periodIncomeIdr).toInt() else 0
            val healthBadge = when {
                savingRate >= 30 -> "Sangat Sehat (Surplus ≥30%)"
                savingRate >= 15 -> "Sehat & Stabil (Surplus 15-29%)"
                savingRate > 0 -> "Perlu Penghematan Tipis (Surplus <15%)"
                else -> "Waspada Defisit (Pengeluaran ≥ Pemasukan)"
            }
            val topBurn = categoryBreakdown.take(3).joinToString("\n") {
                "  • **${it.categoryName}**: ${IdrFormatter.format(it.amountIdr)} (${String.format(Locale.US, "%.1f", it.percentage)}%)"
            }.ifBlank { "  • Belum ada pengeluaran tercatat." }
            val criticalBudgets = budgets.filter { it.statusLevel != BudgetAlertLevel.SAFE }
            val budgetWarnText = if (criticalBudgets.isEmpty()) {
                "Semua anggaran masih aman di bawah 70%."
            } else {
                criticalBudgets.joinToString("\n") { "  • ⚠️ **${it.budget.name}**: sudah ${it.utilizationPercent}% (sisa ${IdrFormatter.format(it.remainingAmountIdr)})" }
            }
            val annualText = if (annualSummary != null) {
                "• **Rekap Tahun ${annualSummary.year}**: Masuk **${IdrFormatter.format(annualSummary.totalIncomeIdr)}** | Abis **${IdrFormatter.format(annualSummary.totalNetExpenseIdr)}** | Bersih **${IdrFormatter.format(annualSummary.netCashFlowIdr, showPlusSign = true)}**"
            } else ""
            val accountTop = accounts.maxByOrNull { it.currentBalanceIdr }?.let { "${it.name} (${IdrFormatter.format(it.currentBalanceIdr)})" } ?: "-"

            return GroundedAiResult(
                explanationMarkdown = "**Audit Keuangan Rinci Xynoos AI**\n\n" +
                    "• **Status Kesehatan Kas**: **$healthBadge** (Rasio Tabungan: **$savingRate%**)\n" +
                    "• **Total Kas Likuid**: ${IdrFormatter.format(overview.availableCashIdr)} (≈ $cashUsd)\n" +
                    "• **Rekening Terbesar**: $accountTop\n" +
                    (if (annualText.isNotEmpty()) "$annualText\n" else "") +
                    "\n**3 Pos Pengeluaran Terbesar (Audit Kebocoran):**\n$topBurn\n\n" +
                    "**Status Peringatan Anggaran:**\n$budgetWarnText\n\n" +
                    "**Langkah Praktis Berikutnya:**\n" +
                    "1. Tetapkan batas harian agar kategori terbesar turun minimal 10% bulan depan.\n" +
                    "2. Pisahkan saldo kas masuk (misal Gaji Pekerjaan / PKL) ke rekening tabungan khusus di awal periode.",
                actionType = "SUMMARY",
                isFromLocalFallback = true
            )
        }

        // Live USD / Dollar / Valas / Kurs questions
        if (lower.contains("usd") || lower.contains("dollar") || lower.contains("dolar") || lower.contains("kurs") || lower.contains("mata uang")) {
            val rateIdr = IdrFormatter.format(usdMarket.currentRateIdr.toLong())
            return GroundedAiResult(
                explanationMarkdown = "**Analisis Saldo dalam Mata Uang Real USD & Kurs Pasar**\n\n" +
                    "• **Kurs Live USD/IDR**: **$rateIdr / $1 USD** (${if (usdMarket.changeIdr >= 0) "+" else ""}${String.format(Locale.US, "%.2f", usdMarket.changePercent)}%)\n" +
                    "• **Rentang Harian**: ${IdrFormatter.format(usdMarket.dayHighIdr.toLong())} (High) / ${IdrFormatter.format(usdMarket.dayLowIdr.toLong())} (Low)\n" +
                    "• **Kas Tersedia**: **${IdrFormatter.format(overview.availableCashIdr)}** → **$cashUsd**\n" +
                    "• **Net Worth**: **${IdrFormatter.format(overview.netWorthIdr)}** → **$netWorthUsd**\n\n" +
                    "**Saran Strategis**: Memantau aset bersih dalam ekuivalen USD membantu mengukur daya beli riil terhadap inflasi global. Cek grafik candlestick 1 menit di tab **Kurs USD**.",
                actionType = "GENERAL_QA",
                isFromLocalFallback = true
            )
        }

        // Investment & Emergency Fund Q&A
        if (lower.contains("investasi") || lower.contains("saham") || lower.contains("reksadana") || lower.contains("emas") || lower.contains("inflasi") || lower.contains("dana darurat")) {
            val monthlyExpBase = monthlySummary?.netExpenseIdr?.takeIf { it > 0L } ?: overview.periodExpenseIdr
            val recommendedEmergencyFund = if (monthlyExpBase > 0L) monthlyExpBase * 6L else 18_000_000L
            val coverageMonths = if (monthlyExpBase > 0L) (overview.availableCashIdr.toDouble() / monthlyExpBase.toDouble()) else 0.0
            return GroundedAiResult(
                explanationMarkdown = "**Konsultasi Dana Darurat & Strategi Investasi Personal**\n\n" +
                    "• **Kas Likuid Anda**: ${IdrFormatter.format(overview.availableCashIdr)} (≈ $cashUsd)\n" +
                    "• **Cakupan Dana Darurat Saat Ini**: **${String.format(Locale.US, "%.1f", coverageMonths)} bulan** pengeluaran\n" +
                    "• **Target Ideal (6x Pengeluaran Bulanan)**: ${IdrFormatter.format(recommendedEmergencyFund)}\n\n" +
                    "**Rekomendasi Bertahap:**\n" +
                    "1. **Amankan 3–6x pengeluaran** di tabungan likuid/RDPU terlebih dahulu sebelum mengambil aset volatil.\n" +
                    "2. **Alokasi Surplus Bulanan (${IdrFormatter.format(overview.netCashFlowIdr, showPlusSign = true)})**: Terapkan rumus 50% kebutuhan pokok, 30% gaya hidup (jajan/nongkrong), 20% tabungan & investasi.\n" +
                    "3. **Diversifikasi**: Kombinasikan instrumen likuid (RDPU/Deposito), lindung nilai (Emas/USD), dan pertumbuhan jangka panjang.",
                actionType = "GENERAL_QA",
                isFromLocalFallback = true
            )
        }

        // Goal projection query
        if (lower.contains("menabung") || lower.contains("target") || lower.contains("kapan")) {
            val regex = Regex("""(\d+[.,]?\d*)\s*(miliar|juta|jt|ribu|rb|k)?""")
            val match = regex.find(lower.replace("rp", ""))
            val monthlySaving = match?.value?.let { IdrFormatter.parseIdrInput(it) } ?: 500_000L
            val primaryGoal = goals.firstOrNull { !it.isCompleted } ?: goals.firstOrNull()
            return if (primaryGoal != null) {
                val effectiveSaved = FinancialCalculator.calculateEffectiveGoalSavedIdr(
                    primaryGoal,
                    overview.availableCashIdr,
                    overview.totalAssetsIdr
                )
                val months = FinancialCalculator.estimateMonthsToReachGoal(
                    primaryGoal.targetAmountIdr,
                    effectiveSaved,
                    monthlySaving
                )
                val remaining = (primaryGoal.targetAmountIdr - effectiveSaved).coerceAtLeast(0L)
                GroundedAiResult(
                    explanationMarkdown = "**Proyeksi Pencapaian Target Tabungan**\n\n" +
                        "• **Target**: ${primaryGoal.name}\n" +
                        "• **Saldo Terkoneksi Saat Ini**: ${IdrFormatter.format(effectiveSaved)} dari ${IdrFormatter.format(primaryGoal.targetAmountIdr)}\n" +
                        "• **Kekurangan Dana**: ${IdrFormatter.format(remaining)}\n" +
                        "• **Jika Menabung**: ${IdrFormatter.format(monthlySaving)} / bulan → Tercapai dalam **${months ?: 0} bulan**\n\n" +
                        "💡 *Tip: Aktifkan aturan alokasi otomatis atau sisihkan begitu Gaji/PKL masuk agar target tercapai lebih cepat.*",
                    actionType = "GOAL_PROJECTION",
                    isFromLocalFallback = true
                )
            } else {
                GroundedAiResult(
                    explanationMarkdown = "Anda belum membuat target tabungan di menu **Target**, namun kas Anda saat ini **${IdrFormatter.format(overview.availableCashIdr)}** (≈ $cashUsd). Dengan menabung **${IdrFormatter.format(monthlySaving)}/bulan**, dalam setahun Anda menambah **${IdrFormatter.format(monthlySaving * 12)}**!",
                    actionType = "GOAL_PROJECTION",
                    isFromLocalFallback = true
                )
            }
        }

        if (lower.contains("hari ini")) {
            return GroundedAiResult(
                explanationMarkdown = "**Ringkasan Aktivitas Hari Ini**\n\n" +
                    "• **Pemasukan**: ${IdrFormatter.format(todayIncome)}\n" +
                    "• **Pengeluaran**: ${IdrFormatter.format(todayExpense)}\n" +
                    "• **Selisih Hari Ini**: ${IdrFormatter.format(todayIncome - todayExpense, showPlusSign = true)}\n" +
                    "• **Total Kas Tersedia**: ${IdrFormatter.format(overview.availableCashIdr)} (≈ $cashUsd)",
                actionType = "SUMMARY",
                isFromLocalFallback = true
            )
        }

        if (lower.contains("kategori") || lower.contains("paling banyak")) {
            val topList = categoryBreakdown.take(4).mapIndexed { idx, item ->
                "${idx + 1}. **${item.categoryName}**: ${IdrFormatter.format(item.amountIdr)} (${String.format(Locale.US, "%.1f", item.percentage)}% • ${item.transactionCount}x)"
            }.joinToString("\n").ifBlank { "Belum ada pengeluaran tercatat pada periode ini." }
            return GroundedAiResult(
                explanationMarkdown = "**Analisis Rincian Kategori Pengeluaran Terbesar**\n\n$topList\n\n💡 *Tip: Kamu bisa mengeklik kategori mana pun di halaman Anggaran Bulanan & Tahunan untuk melihat riwayat detail transaksinya.*",
                actionType = "SPENDING_TREND",
                isFromLocalFallback = true
            )
        }

        if (lower.contains("anggaran") || lower.contains("budget") || lower.contains("sisa")) {
            val detail = if (budgets.isNotEmpty()) {
                budgets.joinToString("\n") {
                    "• **${it.budget.name}** (${it.budget.categoryName}): Terpakai **${it.utilizationPercent}%** (${IdrFormatter.format(it.spentAmountIdr)} / ${IdrFormatter.format(it.budget.limitAmountIdr)}) • Sisa **${IdrFormatter.format(it.remainingAmountIdr)}**"
                }
            } else {
                val baseIncome = monthlySummary?.incomeIdr?.takeIf { it > 0L } ?: overview.periodIncomeIdr
                "Belum ada anggaran khusus. Rekomendasi alokasi **50 / 30 / 20** dari pemasukan ${IdrFormatter.format(baseIncome)}:\n" +
                    "• Kebutuhan Pokok (50%): ${IdrFormatter.format((baseIncome * 50) / 100)}\n" +
                    "• Jajan & Gaya Hidup (30%): ${IdrFormatter.format((baseIncome * 30) / 100)}\n" +
                    "• Tabungan & Investasi (20%): ${IdrFormatter.format((baseIncome * 20) / 100)}"
            }
            return GroundedAiResult(
                explanationMarkdown = "**Evaluasi & Rekomendasi Anggaran**\n\n$detail",
                actionType = "BUDGET_ADVICE",
                isFromLocalFallback = true
            )
        }

        val topCatText = categoryBreakdown.firstOrNull()?.let { "${it.categoryName} (${IdrFormatter.format(it.amountIdr)})" } ?: "Belum ada"
        val annualLine = annualSummary?.let {
            "\n• **Rekap Tahun ${it.year}**: Masuk ${IdrFormatter.format(it.totalIncomeIdr)} | Abis ${IdrFormatter.format(it.totalNetExpenseIdr)}"
        } ?: ""

        return GroundedAiResult(
            explanationMarkdown = "**Konsultasi Xynoos AI untuk: \"$userQuery\"**\n\n" +
                "• **Kas Tersedia**: ${IdrFormatter.format(overview.availableCashIdr)} _(≈ $cashUsd)_\n" +
                "• **Kekayaan Bersih**: ${IdrFormatter.format(overview.netWorthIdr)} _(≈ $netWorthUsd)_\n" +
                "• **Bulan Ini**: Masuk ${IdrFormatter.format(overview.periodIncomeIdr)} • Keluar ${IdrFormatter.format(overview.periodExpenseIdr)}$annualLine\n" +
                "• **Pos Terbesar**: $topCatText\n\n" +
                "**Contoh hal lain yang bisa langsung kamu lakukan:**\n" +
                "• Ketik transaksi langsung: *\"Beli bensin 35 ribu\"*, *\"Beli jajan 20rb\"*, atau *\"Terima gaji PKL 1.5 juta\"*\n" +
                "• Minta analisis: *\"Audit keuangan saya bulan & tahun ini\"* atau *\"Evaluasi anggaran jajan & makan saya\"*",
            actionType = "GENERAL_QA",
            isFromLocalFallback = true
        )
    }

    /**
     * Queries Gemini (`gemini-3.5-flash-lite`) with grounded financial facts,
     * annual/monthly context, recent session chat history, and live USD rate.
     */
    suspend fun analyzeWithGroundedGemini(
        userQuery: String,
        overview: FinancialOverviewSummary,
        accounts: List<FinancialAccountModel>,
        categories: List<TransactionCategoryModel>,
        allTransactions: List<TransactionModel>,
        budgets: List<BudgetProgressSummary>,
        goals: List<FinancialGoalModel>,
        usdMarket: LiveUsdMarketState = LiveUsdMarketState(),
        monthlySummary: MonthlyLedgerSummary? = null,
        annualSummary: AnnualLedgerSummary? = null,
        recentHistory: List<AiMessageModel> = emptyList()
    ): GroundedAiResult {
        val todayTxs = FinancialCalculator.filterTransactionsByPeriod(allTransactions, DashboardPeriod.TODAY)
        val monthTxs = FinancialCalculator.filterTransactionsByPeriod(allTransactions, DashboardPeriod.MONTH)
        val catBreakdown = FinancialCalculator.calculateCategoryBreakdown(monthTxs, categories)
        val localDraft = parseNaturalTransactionDraft(userQuery, accounts, categories)

        val groundedContext = buildGroundedContext(
            overview = overview,
            todayTransactions = todayTxs,
            monthTransactions = monthTxs,
            categoryBreakdown = catBreakdown,
            budgets = budgets,
            goals = goals,
            usdMarket = usdMarket,
            accounts = accounts,
            monthlySummary = monthlySummary,
            annualSummary = annualSummary
        )

        val historyText = recentHistory
            .takeLast(6)
            .joinToString("\n") { msg ->
                val speaker = if (msg.role == "user") "Pengguna" else "Xynoos AI"
                "$speaker: ${msg.content.replace("\n", " ").take(240)}"
            }

        val fullPrompt = """
            Anda adalah Xynoos AI Finance Advisor, konsultan keuangan pribadi, mahasiswa/PKL, UMKM, & ekonomi profesional berbahasa Indonesia.
            Gaya bicara: cerdas, suportif, terstruktur dengan poin jelas & angka pasti, serta memberi langkah tindakan konkret (actionable advice).

            ATURAN WAJIB:
            1. Gunakan HANYA angka terverifikasi dari FAKTA KEUANGAN TERVERIFIKASI di bawah. Jangan pernah mengarang angka saldo pengguna.
            2. Pertahankan kesinambungan topik dari RIWAYAT SESI CHAT jika pengguna merujuk percakapan sebelumnya.
            3. Berikan analisis mendalam, deteksi kebocoran pengeluaran (seperti jajan/kopi/bensin berlebih), strategi menabung, dan perbandingan pemasukan vs pengeluaran bulanan/tahunan bila relevan.
            4. HANYA jika pengguna secara eksplisit memerintahkan pencatatan transaksi baru (misal: "Beli bensin 35 ribu", "Beli jajan 20rb", "Terima gaji PKL 1.5 juta ke BCA"), set hasProposedTransaction = true dan isi txType, txAmountIdr, txTitle, txCategoryName (cocokkan paling akurat dari Daftar Kategori), txAccountName.
            5. Untuk pertanyaan/konsultasi/audit/saran, set hasProposedTransaction = false.

            $groundedContext

            Daftar Akun Pengguna: ${accounts.joinToString(", ") { it.name }}
            Daftar Kategori Tersedia: ${categories.joinToString(", ") { "${it.name} (${it.type.name})" }}

            ${if (historyText.isNotBlank()) "RIWAYAT SESI CHAT TERAKHIR:\n$historyText\n" else ""}
            Pesan Pengguna Terbaru: "$userQuery"
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                structuredModel.generateContent(fullPrompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty Gemini response")
            val json = JSONObject(jsonText)
            val explanation = json.optString("explanation").ifBlank {
                generateDeterministicGroundedAnswer(
                    userQuery, overview, todayTxs, catBreakdown, budgets, goals, localDraft,
                    usdMarket, accounts, monthlySummary, annualSummary
                ).explanationMarkdown
            }
            val actionType = json.optString("actionType", "GENERAL_QA")
            val hasProposed = json.optBoolean("hasProposedTransaction", false)

            val aiDraft = if (hasProposed && json.optLong("txAmountIdr", 0L) > 0L) {
                val rawType = json.optString("txType", "EXPENSE").uppercase(Locale.ROOT)
                val parsedType = if (rawType == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
                ProposedTransactionDraft(
                    type = parsedType,
                    amountIdr = json.optLong("txAmountIdr"),
                    title = json.optString("txTitle", "Transaksi Baru").ifBlank { "Transaksi Baru" },
                    categoryName = json.optString("txCategoryName").ifBlank { localDraft?.categoryName ?: "Lainnya" },
                    accountName = json.optString("txAccountName").ifBlank { localDraft?.accountName ?: "Tunai (Cash)" },
                    notes = "Diusulkan oleh Xynoos AI"
                )
            } else {
                localDraft
            }

            GroundedAiResult(
                explanationMarkdown = explanation,
                actionType = if (aiDraft != null) "DRAFT_TRANSACTION" else actionType,
                proposedDraft = aiDraft,
                isFromLocalFallback = false
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini request timed out after 60s; using deterministic grounded engine", e)
            generateDeterministicGroundedAnswer(
                userQuery, overview, todayTxs, catBreakdown, budgets, goals, localDraft,
                usdMarket, accounts, monthlySummary, annualSummary
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini AI call failed; providing deterministic grounded analysis", e)
            generateDeterministicGroundedAnswer(
                userQuery, overview, todayTxs, catBreakdown, budgets, goals, localDraft,
                usdMarket, accounts, monthlySummary, annualSummary
            )
        }
    }

    /**
     * Streams grounded financial commentary using generateContentStream() for real-time typing UX.
     */
    fun streamGroundedInsight(
        userQuery: String,
        groundedContext: String
    ): Flow<String> = flow {
        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            streamingModel.generateContentStream(
                "Jawab singkat, akurat, dan terstruktur dalam Bahasa Indonesia berdasarkan fakta berikut:\n$groundedContext\n\nPertanyaan: $userQuery"
            ).collect { chunk ->
                accumulated.append(chunk.text.orEmpty())
                emit(accumulated.toString())
            }
        }
    }
}
