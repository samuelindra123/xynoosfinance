package com.example.ai

import android.util.Log
import com.example.data.remote.LiveUsdMarketState
import com.example.domain.BudgetProgressSummary
import com.example.domain.CategoryBreakdownItem
import com.example.domain.DashboardPeriod
import com.example.domain.FinancialAccountModel
import com.example.domain.FinancialCalculator
import com.example.domain.FinancialGoalModel
import com.example.domain.FinancialOverviewSummary
import com.example.domain.IdrFormatter
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
 * Grounded Xynoos AI Finance Assistant & Open Q&A Advisor.
 * Uses Firebase AI Logic (`gemini-3.5-flash-lite`) with App Check attestation and
 * structured `Schema.obj(...)` JSON output, backed by deterministic repository calculations
 * and real-time 1-minute USD/IDR exchange rates.
 */
class GroundedAiAssistant {

    private val structuredSchema = Schema.obj(
        properties = mapOf(
            "explanation" to Schema.string(description = "Grounded financial explanation, Q&A answer, and recommendation in natural Indonesian"),
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
     * "Bayar makan siang 35 ribu pakai cash" or "Terima gaji 15 juta ke BCA"
     */
    fun parseNaturalTransactionDraft(
        prompt: String,
        accounts: List<FinancialAccountModel>,
        categories: List<TransactionCategoryModel>
    ): ProposedTransactionDraft? {
        val lower = prompt.lowercase(Locale.ROOT).trim()
        // Do not trigger transaction draft if the user is asking a question
        if (lower.startsWith("apa") || lower.startsWith("bagaimana") || lower.startsWith("kenapa") ||
            lower.startsWith("berapa") || lower.startsWith("jelaskan") || lower.startsWith("kapan") ||
            lower.startsWith("tips") || lower.startsWith("cara") || lower.contains("?")
        ) {
            return null
        }

        val isExpenseTrigger = listOf("bayar", "beli", "makan", "jajan", "ongkos", "bensin", "tagihan", "keluar", "belanja").any { lower.contains(it) }
        val isIncomeTrigger = listOf("terima", "gaji", "masuk", "dapat", "honor", "invoice cair", "pendapatan").any { lower.contains(it) }

        if (!isExpenseTrigger && !isIncomeTrigger) return null

        val regex = Regex("""(\d+[.,]?\d*)\s*(miliar|juta|jt|ribu|rb|k)?""")
        val match = regex.find(lower) ?: return null
        val amountIdr = IdrFormatter.parseIdrInput(match.value) ?: return null
        if (amountIdr <= 0L) return null

        val txType = if (isIncomeTrigger && !isExpenseTrigger) TransactionType.INCOME else TransactionType.EXPENSE

        val matchedCategory = categories.firstOrNull { cat ->
            cat.type == txType && (
                lower.contains(cat.name.lowercase(Locale.ROOT).split(" ").first()) ||
                    (cat.id == "cat_food" && (lower.contains("makan") || lower.contains("kopi") || lower.contains("siang") || lower.contains("malam"))) ||
                    (cat.id == "cat_transport" && (lower.contains("grab") || lower.contains("gojek") || lower.contains("bensin") || lower.contains("tol") || lower.contains("parkir"))) ||
                    (cat.id == "cat_utilities" && (lower.contains("listrik") || lower.contains("internet") || lower.contains("wifi") || lower.contains("air"))) ||
                    (cat.id == "cat_salary" && lower.contains("gaji")) ||
                    (cat.id == "cat_freelance" && (lower.contains("proyek") || lower.contains("klien") || lower.contains("freelance")))
                )
        } ?: categories.firstOrNull { it.type == txType }

        val matchedAccount = accounts.firstOrNull { acc ->
            lower.contains(acc.name.lowercase(Locale.ROOT)) ||
                (acc.accountType.name == "CASH" && (lower.contains("cash") || lower.contains("tunai")))
        } ?: accounts.firstOrNull()

        val cleanedTitle = prompt
            .replace(match.value, "", ignoreCase = true)
            .replace(Regex("""(?i)\b(bayar|beli|pakai|menggunakan|ke|dari|dengan|cash|tunai|rp)\b"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            .ifBlank { if (txType == TransactionType.INCOME) "Pemasukan Baru" else "Pengeluaran Baru" }

        return ProposedTransactionDraft(
            type = txType,
            amountIdr = amountIdr,
            title = cleanedTitle,
            categoryName = matchedCategory?.name ?: if (txType == TransactionType.INCOME) "Gaji Utama" else "Makanan & Minuman",
            accountName = matchedAccount?.name ?: "Tunai (Cash)",
            notes = "Dibuat melalui Xynoos AI dari perintah: \"$prompt\""
        )
    }

    /**
     * Builds a grounded, privacy-minimal financial context from verified repository totals and live USD rate.
     */
    fun buildGroundedContext(
        overview: FinancialOverviewSummary,
        todayTransactions: List<TransactionModel>,
        monthTransactions: List<TransactionModel>,
        categoryBreakdown: List<CategoryBreakdownItem>,
        budgets: List<BudgetProgressSummary>,
        goals: List<FinancialGoalModel>,
        usdMarket: LiveUsdMarketState = LiveUsdMarketState()
    ): String {
        val todayIncome = todayTransactions.filter { !it.isDeleted && it.type == TransactionType.INCOME }.sumOf { it.amountIdr }
        val todayExpense = todayTransactions.filter { !it.isDeleted && it.type == TransactionType.EXPENSE }.sumOf { it.amountIdr }

        val topCatSummary = if (categoryBreakdown.isEmpty()) {
            "Belum ada pengeluaran tercatat bulan ini."
        } else {
            categoryBreakdown.take(4).joinToString("; ") {
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

        val cashUsd = IdrFormatter.formatUsdFromIdr(overview.availableCashIdr, usdMarket.currentRateIdr)
        val netWorthUsd = IdrFormatter.formatUsdFromIdr(overview.netWorthIdr, usdMarket.currentRateIdr)

        return """
            FAKTA KEUANGAN TERVERIFIKASI & KURS REAL-TIME USD/IDR:
            - Kas Tersedia Saat Ini: ${IdrFormatter.format(overview.availableCashIdr)} (Setara Real-Time: $cashUsd)
            - Total Aset Terlacak: ${IdrFormatter.format(overview.totalAssetsIdr)}
            - Total Kewajiban (Utang): ${IdrFormatter.format(overview.totalLiabilitiesIdr)}
            - Kekayaan Bersih (Net Worth): ${IdrFormatter.format(overview.netWorthIdr)} (Setara Real-Time: $netWorthUsd)
            - Kurs Live Spot USD/IDR (Update 1 Menit): ${IdrFormatter.format(usdMarket.currentRateIdr.toLong())} per $1 USD (High: ${IdrFormatter.format(usdMarket.dayHighIdr.toLong())}, Low: ${IdrFormatter.format(usdMarket.dayLowIdr.toLong())})
            - Pemasukan Hari Ini: ${IdrFormatter.format(todayIncome)}
            - Pengeluaran Hari Ini: ${IdrFormatter.format(todayExpense)}
            - Pemasukan Bulan Ini: ${IdrFormatter.format(overview.periodIncomeIdr)}
            - Pengeluaran Bulan Ini: ${IdrFormatter.format(overview.periodExpenseIdr)}
            - Arus Kas Bersih Bulan Ini: ${IdrFormatter.format(overview.netCashFlowIdr)}
            - Distribusi Pengeluaran Terbesar: $topCatSummary
            - Status Anggaran (Budget): $budgetSummary
            - Target Tabungan (Terkoneksi Otomatis ke Total Saldo): $goalSummary
        """.trimIndent()
    }

    /**
     * Generates a deterministic, authoritative local answer in Bahasa Indonesia
     * supporting both ledger queries, live USD queries, and open-ended financial Q&A.
     */
    fun generateDeterministicGroundedAnswer(
        userQuery: String,
        overview: FinancialOverviewSummary,
        todayTransactions: List<TransactionModel>,
        categoryBreakdown: List<CategoryBreakdownItem>,
        budgets: List<BudgetProgressSummary>,
        goals: List<FinancialGoalModel>,
        localDraft: ProposedTransactionDraft?,
        usdMarket: LiveUsdMarketState = LiveUsdMarketState()
    ): GroundedAiResult {
        if (localDraft != null) {
            return GroundedAiResult(
                explanationMarkdown = "Saya telah menyiapkan draf transaksi **${localDraft.type.labelId}** sebesar **${IdrFormatter.format(localDraft.amountIdr)}** untuk **${localDraft.title}** pada kategori **${localDraft.categoryName}** menggunakan akun **${localDraft.accountName}**.\n\nSilakan tinjau dan ketuk **Konfirmasi & Simpan** di bawah agar transaksi ini tercatat ke buku besar Anda beserta tahun & waktu detailnya.",
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

        // 1. Live USD / Dollar / Valas / Kurs questions
        if (lower.contains("usd") || lower.contains("dollar") || lower.contains("dolar") || lower.contains("kurs") || lower.contains("mata uang")) {
            val rateIdr = IdrFormatter.format(usdMarket.currentRateIdr.toLong())
            return GroundedAiResult(
                explanationMarkdown = "**Analisis Saldo dalam Mata Uang Real USD & Kurs Pasar (Update 1 Menit)**\n\n" +
                    "• **Kurs Live USD/IDR Saat Ini**: **$rateIdr / $1 USD** (${if (usdMarket.changeIdr >= 0) "+" else ""}${String.format(Locale.US, "%.2f", usdMarket.changePercent)}%)\n" +
                    "• **Rentang Harian (High / Low)**: ${IdrFormatter.format(usdMarket.dayHighIdr.toLong())} / ${IdrFormatter.format(usdMarket.dayLowIdr.toLong())}\n" +
                    "• **Kas Tersedia Anda dalam USD**: **$cashUsd** (dari ${IdrFormatter.format(overview.availableCashIdr)})\n" +
                    "• **Kekayaan Bersih (Net Worth) dalam USD**: **$netWorthUsd** (dari ${IdrFormatter.format(overview.netWorthIdr)})\n\n" +
                    "**Wawasan Finansial**: Menyimpan sebagian dana darurat atau aset likuid yang dipantau terhadap USD membantu menjaga daya beli terhadap fluktuasi nilai tukar. Anda dapat memantau grafik candlestick 1 menit secara langsung di menu **Pasar & Grafik Live USD**.",
                actionType = "GENERAL_QA",
                isFromLocalFallback = true
            )
        }

        // 2. Open Q&A: Investasi, Saham, Reksadana, Emas, Inflasi, Dana Darurat, Pajak, Bisnis
        if (lower.contains("investasi") || lower.contains("saham") || lower.contains("reksadana") || lower.contains("emas") || lower.contains("inflasi") || lower.contains("dana darurat")) {
            val recommendedEmergencyFund = if (overview.periodExpenseIdr > 0L) overview.periodExpenseIdr * 6L else 30_000_000L
            return GroundedAiResult(
                explanationMarkdown = "**Konsultasi & Tanya Jawab Strategi Keuangan Xynoos AI**\n\n" +
                    "Berdasarkan pertanyaan Anda mengenai **\"$userQuery\"** serta posisi keuangan Anda saat ini:\n" +
                    "• **Posisi Kas Likuid Anda**: ${IdrFormatter.format(overview.availableCashIdr)} (≈ $cashUsd)\n" +
                    "• **Rekomendasi Dana Darurat Ideal (6x Pengeluaran Bulanan)**: ${IdrFormatter.format(recommendedEmergencyFund)}\n\n" +
                    "**Panduan Praktis Berlapis**:\n" +
                    "1. **Lapisan Likuiditas (Dana Darurat)**: Pastikan kas likuid minimal 3–6 kali pengeluaran bulanan tersimpan di rekening tabungan/pasar uang yang aman.\n" +
                    "2. **Lindung Nilai & Valas (USD/Emas)**: Pantau nilai kekayaan bersih Anda dalam USD ($netWorthUsd) agar tahan terhadap inflasi tahunan.\n" +
                    "3. **Investasi Bertumbuh**: Alokasikan surplus arus kas bersih (${IdrFormatter.format(overview.netCashFlowIdr, showPlusSign = true)}) secara konsisten ke instrumen produktif sesuai profil risiko Anda.",
                actionType = "GENERAL_QA",
                isFromLocalFallback = true
            )
        }

        // 3. Goal projection query ("Kalau saya menabung Rp500.000 per bulan, kapan target saya tercapai?")
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
                    explanationMarkdown = "**Analisis Proyeksi Target Tabungan (Terkoneksi Otomatis ke Total Saldo)**\n\n" +
                        "• **Target Aktif**: ${primaryGoal.name}\n" +
                        "• **Saldo Terkoneksi Saat Ini**: ${IdrFormatter.format(effectiveSaved)} (≈ ${IdrFormatter.formatUsdFromIdr(effectiveSaved, usdMarket.currentRateIdr)}) dari target ${IdrFormatter.format(primaryGoal.targetAmountIdr)}\n" +
                        "• **Sisa Kebutuhan Dana**: ${IdrFormatter.format(remaining)}\n" +
                        "• **Asumsi Setoran Rutin**: ${IdrFormatter.format(monthlySaving)} / bulan\n" +
                        "• **Estimasi Waktu Tercapai**: **${months ?: 0} bulan**\n\n" +
                        "*Catatan: Target tabungan di XynoosFinance langsung terkoneksi otomatis dengan total saldo kas Anda tanpa perlu input penambahan manual.*",
                    actionType = "GOAL_PROJECTION",
                    isFromLocalFallback = true
                )
            } else {
                GroundedAiResult(
                    explanationMarkdown = "Anda belum memiliki target tabungan aktif, namun total saldo kas Anda saat ini adalah **${IdrFormatter.format(overview.availableCashIdr)}** (≈ $cashUsd). Dengan asumsi menabung **${IdrFormatter.format(monthlySaving)}/bulan**, dalam 12 bulan Anda akan mengumpulkan tambahan **${IdrFormatter.format(monthlySaving * 12)}**.",
                    actionType = "GOAL_PROJECTION",
                    isFromLocalFallback = true
                )
            }
        }

        if (lower.contains("hari ini")) {
            return GroundedAiResult(
                explanationMarkdown = "**Ringkasan Aktivitas Hari Ini (Data Buku Besar)**\n\n" +
                    "• **Pemasukan Hari Ini**: ${IdrFormatter.format(todayIncome)}\n" +
                    "• **Pengeluaran Hari Ini**: ${IdrFormatter.format(todayExpense)}\n" +
                    "• **Selisih Harian**: ${IdrFormatter.format(todayIncome - todayExpense, showPlusSign = true)}\n" +
                    "• **Total Kas Tersedia**: ${IdrFormatter.format(overview.availableCashIdr)} (≈ $cashUsd)",
                actionType = "SUMMARY",
                isFromLocalFallback = true
            )
        }

        if (lower.contains("kategori") || lower.contains("paling banyak")) {
            val top = categoryBreakdown.firstOrNull()
            val body = if (top == null) {
                "Belum ada pengeluaran tercatat pada periode ini."
            } else {
                "Kategori dengan pengeluaran terbesar bulan ini adalah **${top.categoryName}** sebesar **${IdrFormatter.format(top.amountIdr)}** (${String.format(Locale.US, "%.1f", top.percentage)}% dari total pengeluaran ${IdrFormatter.format(overview.periodExpenseIdr)})."
            }
            return GroundedAiResult(
                explanationMarkdown = "**Analisis Distribusi Kategori Pengeluaran**\n\n$body",
                actionType = "SPENDING_TREND",
                isFromLocalFallback = true
            )
        }

        if (lower.contains("anggaran") || lower.contains("budget") || lower.contains("sisa")) {
            val foodBudget = budgets.firstOrNull { it.budget.categoryName.contains("Makan", ignoreCase = true) } ?: budgets.firstOrNull()
            val detail = if (foodBudget != null) {
                "• **${foodBudget.budget.name} (${foodBudget.budget.categoryName})**: Terpakai ${IdrFormatter.format(foodBudget.spentAmountIdr)} dari batas ${IdrFormatter.format(foodBudget.budget.limitAmountIdr)} (**${foodBudget.utilizationPercent}%**).\n" +
                    "• **Sisa Anggaran**: **${IdrFormatter.format(foodBudget.remainingAmountIdr)}**"
            } else {
                "• **Total Pengeluaran Bulan Ini**: ${IdrFormatter.format(overview.periodExpenseIdr)}\n" +
                    "• **Saran Anggaran 50/30/20**: Dari pemasukan ${IdrFormatter.format(overview.periodIncomeIdr)}, alokasikan maks. ${IdrFormatter.format((overview.periodIncomeIdr * 50) / 100)} untuk kebutuhan pokok, ${IdrFormatter.format((overview.periodIncomeIdr * 30) / 100)} untuk gaya hidup, dan minimal ${IdrFormatter.format((overview.periodIncomeIdr * 20) / 100)} untuk tabungan."
            }
            return GroundedAiResult(
                explanationMarkdown = "**Evaluasi & Rekomendasi Anggaran**\n\n$detail",
                actionType = "BUDGET_ADVICE",
                isFromLocalFallback = true
            )
        }

        val topCatText = categoryBreakdown.firstOrNull()?.let { "${it.categoryName} (${IdrFormatter.format(it.amountIdr)})" } ?: "Belum ada"
        return GroundedAiResult(
            explanationMarkdown = "**Jawaban & Konsultasi Xynoos AI untuk: \"$userQuery\"**\n\n" +
                "• **Kas Tersedia**: ${IdrFormatter.format(overview.availableCashIdr)} _(≈ $cashUsd)_\n" +
                "• **Kekayaan Bersih (Net Worth)**: ${IdrFormatter.format(overview.netWorthIdr)} _(≈ $netWorthUsd)_\n" +
                "• **Kurs Spot USD/IDR (1m)**: ${IdrFormatter.format(usdMarket.currentRateIdr.toLong())} / $1 USD\n" +
                "• **Pemasukan Periode Ini**: ${IdrFormatter.format(overview.periodIncomeIdr)}\n" +
                "• **Pengeluaran Periode Ini**: ${IdrFormatter.format(overview.periodExpenseIdr)}\n" +
                "• **Arus Kas Bersih**: ${IdrFormatter.format(overview.netCashFlowIdr, showPlusSign = true)}\n" +
                "• **Pengeluaran Terbesar**: $topCatText\n" +
                "• **Progres Target Tabungan (Otomatis)**: ${overview.savingsProgressPercent}%\n\n" +
                "**Saran Strategis**: Anda dapat bertanya apa saja kepada saya—mulai dari strategi menabung, analisis kurs Dollar (USD), perencanaan pajak bisnis, hingga perintah catat transaksi otomatis.",
            actionType = "GENERAL_QA",
            isFromLocalFallback = true
        )
    }

    /**
     * Queries Gemini (`gemini-3.5-flash-lite`) with grounded financial facts, live USD rate, and open Q&A capability.
     */
    suspend fun analyzeWithGroundedGemini(
        userQuery: String,
        overview: FinancialOverviewSummary,
        accounts: List<FinancialAccountModel>,
        categories: List<TransactionCategoryModel>,
        allTransactions: List<TransactionModel>,
        budgets: List<BudgetProgressSummary>,
        goals: List<FinancialGoalModel>,
        usdMarket: LiveUsdMarketState = LiveUsdMarketState()
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
            usdMarket = usdMarket
        )

        val fullPrompt = """
            Anda adalah Xynoos AI Finance Assistant & Advisor, konsultan keuangan pribadi, bisnis, dan ekonomi profesional berbahasa Indonesia.
            Anda BUKAN sekadar pencatat transaksi, melainkan juga mitra tanya-jawab (Q&A) cerdas yang bisa menjawab pertanyaan apa pun seputar keuangan, investasi, saham, valuta asing (USD/IDR), perencanaan tabungan, bisnis, maupun pertanyaan umum pengguna dengan ramah dan mendalam.

            ATURAN WAJIB:
            1. Untuk data saldo/transaksi pengguna, gunakan HANYA angka dari FAKTA KEUANGAN TERVERIFIKASI di bawah. Jangan pernah mengarang saldo pengguna.
            2. Jika pengguna bertanya hal umum / konsultasi / tanya-jawab (misal strategi investasi, kurs dollar, inflasi, tips hemat, bisnis, atau pertanyaan bebas), jawab secara komprehensif, edukatif, dan hubungkan secara relevan dengan kondisi saldo/USD pengguna. Set actionType = "GENERAL_QA" dan hasProposedTransaction = false.
            3. Gunakan format Rupiah (contoh: Rp1.500.000) dan sertakan ekuivalen USD jika relevan.
            4. HANYA jika pengguna secara eksplisit memerintahkan pencatatan transaksi baru (misal: "Bayar makan siang 35 ribu pakai cash"), set hasProposedTransaction = true dan isi txType, txAmountIdr, txTitle, txCategoryName, txAccountName.

            $groundedContext

            Daftar Akun Pengguna: ${accounts.joinToString(", ") { it.name }}
            Daftar Kategori: ${categories.joinToString(", ") { "${it.name} (${it.type.name})" }}

            Pertanyaan / Perintah Pengguna: "$userQuery"
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                structuredModel.generateContent(fullPrompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty Gemini response")
            val json = JSONObject(jsonText)
            val explanation = json.optString("explanation").ifBlank {
                generateDeterministicGroundedAnswer(userQuery, overview, todayTxs, catBreakdown, budgets, goals, localDraft, usdMarket).explanationMarkdown
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
            generateDeterministicGroundedAnswer(userQuery, overview, todayTxs, catBreakdown, budgets, goals, localDraft, usdMarket)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini AI call failed; providing deterministic grounded analysis", e)
            generateDeterministicGroundedAnswer(userQuery, overview, todayTxs, catBreakdown, budgets, goals, localDraft, usdMarket)
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
