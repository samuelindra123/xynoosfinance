package com.example.domain

import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Deterministic Indonesian Rupiah (IDR) formatting and parsing utilities.
 * Uses exact 64-bit integer Rupiah (zero fraction conventions) — never floating-point arithmetic.
 */
object IdrFormatter {
    private val indonesiaLocale = Locale("id", "ID")

    fun format(amountIdr: Long, isPrivacyHidden: Boolean = false, showPlusSign: Boolean = false): String {
        if (isPrivacyHidden) return "Rp ••••••••"
        val nf = NumberFormat.getIntegerInstance(indonesiaLocale)
        nf.maximumFractionDigits = 0
        val absFormatted = nf.format(abs(amountIdr))
        return when {
            amountIdr < 0L -> "-Rp$absFormatted"
            amountIdr > 0L && showPlusSign -> "+Rp$absFormatted"
            else -> "Rp$absFormatted"
        }
    }

    fun formatCompact(amountIdr: Long, isPrivacyHidden: Boolean = false): String {
        if (isPrivacyHidden) return "Rp •••"
        val sign = if (amountIdr < 0) "-" else ""
        val absVal = abs(amountIdr)
        return when {
            absVal >= 1_000_000_000L -> {
                val billions = absVal / 1_000_000_000L
                val tenth = (absVal % 1_000_000_000L) / 100_000_000L
                if (tenth > 0) "${sign}Rp${billions},${tenth} M" else "${sign}Rp${billions} M"
            }
            absVal >= 1_000_000L -> {
                val millions = absVal / 1_000_000L
                val tenth = (absVal % 1_000_000L) / 100_000L
                if (tenth > 0) "${sign}Rp${millions},${tenth} Jt" else "${sign}Rp${millions} Jt"
            }
            absVal >= 1_000L -> {
                val thousands = absVal / 1_000L
                "${sign}Rp${thousands} Rb"
            }
            else -> "${sign}Rp$absVal"
        }
    }

    /**
     * Converts an integer Rupiah amount to formatted real-time US Dollar ($ USD)
     * based on the live 1-minute USD/IDR spot exchange rate.
     */
    fun formatUsdFromIdr(
        amountIdr: Long,
        usdRateIdr: Double,
        isPrivacyHidden: Boolean = false
    ): String {
        if (isPrivacyHidden) return "$ •••••• USD"
        val safeRate = if (usdRateIdr > 100.0) usdRateIdr else 15850.0
        val usdVal = amountIdr.toDouble() / safeRate
        val sign = if (usdVal < 0.0) "-" else ""
        val formatted = String.format(Locale.US, "%,.2f", abs(usdVal))
        return "${sign}$$formatted USD"
    }

    /**
     * Parses user numeric input or natural Indonesian shorthand ("35.000", "35000", "35 ribu", "35rb", "1,5 juta")
     * into exact Long Rupiah. Returns null if invalid or non-numeric.
     */
    fun parseIdrInput(raw: String): Long? {
        val cleaned = raw.trim().lowercase(Locale.ROOT)
            .removePrefix("rp")
            .trim()
        if (cleaned.isEmpty()) return null

        // Check natural language multipliers first
        val multiplier = when {
            cleaned.contains("miliar") || cleaned.endsWith("m") -> 1_000_000_000L
            cleaned.contains("juta") || cleaned.contains("jt") -> 1_000_000L
            cleaned.contains("ribu") || cleaned.contains("rb") || cleaned.endsWith("k") -> 1_000L
            else -> 1L
        }

        if (multiplier > 1L) {
            val numberPart = cleaned
                .replace("miliar", "")
                .replace("juta", "")
                .replace("jt", "")
                .replace("ribu", "")
                .replace("rb", "")
                .replace("k", "")
                .replace("m", "")
                .trim()
                .replace(",", ".")
            val parts = numberPart.split(".")
            return try {
                if (parts.size == 1) {
                    parts[0].trim().toLong() * multiplier
                } else if (parts.size == 2) {
                    val whole = parts[0].trim().toLong()
                    val fracStr = parts[1].trim().padEnd(3, '0').take(3)
                    val frac = fracStr.toLong()
                    (whole * multiplier) + (frac * (multiplier / 1000L))
                } else {
                    null
                }
            } catch (e: NumberFormatException) {
                null
            }
        }

        // Standard integer input with dot or comma thousands separators
        val digitsOnly = cleaned.replace(".", "").replace(",", "").replace(" ", "")
        if (digitsOnly.isEmpty()) return null
        return digitsOnly.toLongOrNull()
    }

    /**
     * Formats raw user input into dot-separated thousands ("15000000" -> "15.000.000")
     * if it consists of digits/dots, or preserves natural shorthand while typing ("1.5jt").
     */
    fun formatGroupedInputString(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val hasLetters = trimmed.any { it.isLetter() }
        if (hasLetters) return trimmed

        val digitsOnly = trimmed.filter { it.isDigit() }.trimStart('0')
        if (digitsOnly.isEmpty()) {
            return if (trimmed.contains("0")) "0" else ""
        }
        val capped = digitsOnly.take(15) // Up to ratusan triliun
        val parsed = capped.toLongOrNull() ?: return capped
        val nf = NumberFormat.getIntegerInstance(indonesiaLocale)
        nf.maximumFractionDigits = 0
        return nf.format(parsed)
    }

    /**
     * Returns a clear nominal scale badge so the user immediately recognizes the magnitude.
     */
    fun formatNominalScaleBadge(amountIdr: Long): String {
        val absVal = abs(amountIdr)
        if (absVal == 0L) return "NOL RUPIAH • 0 Digit"
        val digits = absVal.toString().length
        return when {
            absVal >= 1_000_000_000_000L -> "TRILIUNAN • $digits Digit"
            absVal >= 1_000_000_000L -> "MILIARAN (M) • $digits Digit"
            absVal >= 1_000_000L -> "JUTAAN (Jt) • $digits Digit"
            absVal >= 1_000L -> "RIBUAN (Rb) • $digits Digit"
            else -> "SATUAN • $digits Digit"
        }
    }

    /**
     * Breaks down an integer Rupiah amount into human-friendly mathematical units
     * (e.g., 15_250_000L -> "15 Juta + 250 Ribu").
     */
    fun formatDigitBreakdown(amountIdr: Long): String {
        val absVal = abs(amountIdr)
        if (absVal == 0L) return "0 Rupiah (Belum ada nominal)"
        val parts = mutableListOf<String>()
        val trillions = absVal / 1_000_000_000_000L
        val billions = (absVal % 1_000_000_000_000L) / 1_000_000_000L
        val millions = (absVal % 1_000_000_000L) / 1_000_000L
        val thousands = (absVal % 1_000_000L) / 1_000L
        val units = absVal % 1_000L

        if (trillions > 0) parts.add("$trillions Triliun")
        if (billions > 0) parts.add("$billions Miliar")
        if (millions > 0) parts.add("$millions Juta")
        if (thousands > 0) parts.add("$thousands Ribu")
        if (units > 0) parts.add("$units Rupiah")

        return parts.joinToString(" + ")
    }

    /**
     * Converts an integer Rupiah amount into formal Indonesian words ("Terbilang")
     * so users never misread the number of zeros.
     */
    fun formatTerbilang(amountIdr: Long): String {
        val absVal = abs(amountIdr)
        if (absVal == 0L) return "Nol Rupiah"
        val prefix = if (amountIdr < 0L) "Minus " else ""
        return "$prefix${terbilangRecursive(absVal).trim().replace(Regex("\\s+"), " ")} Rupiah"
    }

    private val satuanWords = arrayOf(
        "", "Satu", "Dua", "Tiga", "Empat", "Lima",
        "Enam", "Tujuh", "Delapan", "Sembilan", "Sepuluh", "Sebelas"
    )

    private fun terbilangRecursive(n: Long): String {
        return when {
            n < 12L -> satuanWords[n.toInt()]
            n < 20L -> "${terbilangRecursive(n - 10L)} Belas"
            n < 100L -> "${terbilangRecursive(n / 10L)} Puluh ${terbilangRecursive(n % 10L)}"
            n < 200L -> "Seratus ${terbilangRecursive(n - 100L)}"
            n < 1_000L -> "${terbilangRecursive(n / 100L)} Ratus ${terbilangRecursive(n % 100L)}"
            n < 2_000L -> "Seribu ${terbilangRecursive(n - 1_000L)}"
            n < 1_000_000L -> "${terbilangRecursive(n / 1_000L)} Ribu ${terbilangRecursive(n % 1_000L)}"
            n < 1_000_000_000L -> "${terbilangRecursive(n / 1_000_000L)} Juta ${terbilangRecursive(n % 1_000_000L)}"
            n < 1_000_000_000_000L -> "${terbilangRecursive(n / 1_000_000_000L)} Miliar ${terbilangRecursive(n % 1_000_000_000L)}"
            else -> "${terbilangRecursive(n / 1_000_000_000_000L)} Triliun ${terbilangRecursive(n % 1_000_000_000_000L)}"
        }
    }
}

/**
 * Pure, deterministic financial calculation engine.
 * All monetary arithmetic operates strictly on 64-bit integer Rupiah (Long).
 */
object FinancialCalculator {

    /**
     * Computes deterministic account balance from openingBalanceIdr + ledger transactions.
     * - INCOME: +amountIdr on accountId
     * - EXPENSE: -amountIdr on accountId
     * - REFUND: +amountIdr on accountId
     * - ADJUSTMENT: +amountIdr on accountId
     * - TRANSFER: -amountIdr on source (accountId), +amountIdr on destination (toAccountId)
     */
    fun calculateAccountBalance(
        account: FinancialAccountModel,
        transactions: List<TransactionModel>
    ): Long {
        var balance = account.openingBalanceIdr
        for (tx in transactions) {
            if (tx.isDeleted || tx.amountIdr <= 0L) continue
            when (tx.type) {
                TransactionType.INCOME -> {
                    if (tx.accountId == account.id) balance += tx.amountIdr
                }
                TransactionType.EXPENSE -> {
                    if (tx.accountId == account.id) balance -= tx.amountIdr
                }
                TransactionType.REFUND -> {
                    if (tx.accountId == account.id) balance += tx.amountIdr
                }
                TransactionType.ADJUSTMENT -> {
                    if (tx.accountId == account.id) balance += tx.amountIdr
                }
                TransactionType.TRANSFER -> {
                    if (tx.accountId == account.id) {
                        balance -= tx.amountIdr
                    }
                    if (tx.toAccountId == account.id) {
                        balance += tx.amountIdr
                    }
                }
            }
        }
        return balance
    }

    /**
     * Computes the effective saved balance for a savings goal, automatically connecting
     * to the user's real total available cash / assets without requiring manual top-ups.
     */
    fun calculateEffectiveGoalSavedIdr(
        goal: FinancialGoalModel,
        availableCashIdr: Long,
        totalAssetsIdr: Long
    ): Long {
        val connectedTotalSaldo = max(0L, max(availableCashIdr, totalAssetsIdr))
        return max(goal.currentSavedIdr, connectedTotalSaldo)
    }

    /**
     * Computes comprehensive financial overview for a selected period.
     * Invariants enforced:
     * 1. Available cash includes liquid accounts (CASH, BANK, EWALLET, BUSINESS).
     * 2. Total assets includes all non-liability accounts (CASH, BANK, EWALLET, SAVINGS, BUSINESS, ASSET).
     * 3. Total liabilities includes LIABILITY accounts (as positive debt magnitude or negative balance).
     * 4. Net worth = Total Assets - Total Liabilities.
     * 5. Transfers DO NOT inflate consolidated income or expenses.
     * 6. Refunds reduce net expenses for the period (down to 0 minimum).
     * 7. Savings goals automatically connect to Total Saldo (availableCashIdr / totalAssetsIdr) without manual addition.
     */
    fun calculateOverview(
        accounts: List<FinancialAccountModel>,
        allTransactions: List<TransactionModel>,
        periodTransactions: List<TransactionModel>,
        budgets: List<BudgetModel>,
        goals: List<FinancialGoalModel>
    ): FinancialOverviewSummary {
        val activeAccounts = accounts.filter { !it.isArchived }

        var availableCashIdr = 0L
        var totalAssetsIdr = 0L
        var totalLiabilitiesIdr = 0L

        for (acc in activeAccounts) {
            val computedBalance = calculateAccountBalance(acc, allTransactions)
            if (acc.accountType.isLiability) {
                totalLiabilitiesIdr += abs(computedBalance)
            } else {
                totalAssetsIdr += computedBalance
                if (acc.accountType in listOf(
                        AccountType.CASH,
                        AccountType.BANK,
                        AccountType.EWALLET,
                        AccountType.SAVINGS,
                        AccountType.BUSINESS
                    )
                ) {
                    availableCashIdr += computedBalance
                }
            }
        }

        val netWorthIdr = totalAssetsIdr - totalLiabilitiesIdr

        var periodIncomeIdr = 0L
        var grossExpenseIdr = 0L
        var periodRefundsIdr = 0L

        for (tx in periodTransactions) {
            if (tx.isDeleted || tx.amountIdr <= 0L) continue
            when (tx.type) {
                TransactionType.INCOME -> periodIncomeIdr += tx.amountIdr
                TransactionType.EXPENSE -> grossExpenseIdr += tx.amountIdr
                TransactionType.REFUND -> periodRefundsIdr += tx.amountIdr
                TransactionType.TRANSFER, TransactionType.ADJUSTMENT -> {
                    // Transfers and balance adjustments do not count as consolidated income or expense
                }
            }
        }

        val periodExpenseIdr = max(0L, grossExpenseIdr - periodRefundsIdr)
        val netCashFlowIdr = periodIncomeIdr - periodExpenseIdr

        // Budget utilization
        val totalBudgetLimitIdr = budgets.sumOf { it.limitAmountIdr }
        val totalBudgetSpentIdr = if (budgets.any { it.categoryId == "ALL" }) {
            periodExpenseIdr
        } else {
            val budgetedCategoryIds = budgets.map { it.categoryId }.toSet()
            val catExpense = periodTransactions
                .filter { !it.isDeleted && it.type == TransactionType.EXPENSE && it.categoryId in budgetedCategoryIds }
                .sumOf { it.amountIdr }
            val catRefund = periodTransactions
                .filter { !it.isDeleted && it.type == TransactionType.REFUND && it.categoryId in budgetedCategoryIds }
                .sumOf { it.amountIdr }
            max(0L, catExpense - catRefund)
        }

        val budgetUtilizationPercent = if (totalBudgetLimitIdr > 0L) {
            ((totalBudgetSpentIdr * 100L) / totalBudgetLimitIdr).toInt()
        } else {
            0
        }

        // Goals progress — automatically connected to Total Saldo (availableCashIdr) without manual addition
        val totalGoalsTargetIdr = goals.sumOf { it.targetAmountIdr }
        val manualGoalsSavedIdr = goals.sumOf { it.currentSavedIdr }
        val totalGoalsSavedIdr = if (goals.isNotEmpty()) {
            max(manualGoalsSavedIdr, max(0L, availableCashIdr))
        } else {
            manualGoalsSavedIdr
        }
        val savingsProgressPercent = if (totalGoalsTargetIdr > 0L) {
            min(100, ((totalGoalsSavedIdr * 100L) / totalGoalsTargetIdr).toInt())
        } else {
            0
        }

        return FinancialOverviewSummary(
            availableCashIdr = availableCashIdr,
            totalAssetsIdr = totalAssetsIdr,
            totalLiabilitiesIdr = totalLiabilitiesIdr,
            netWorthIdr = netWorthIdr,
            periodIncomeIdr = periodIncomeIdr,
            periodExpenseIdr = periodExpenseIdr,
            netCashFlowIdr = netCashFlowIdr,
            totalBudgetLimitIdr = totalBudgetLimitIdr,
            totalBudgetSpentIdr = totalBudgetSpentIdr,
            budgetUtilizationPercent = budgetUtilizationPercent,
            totalGoalsTargetIdr = totalGoalsTargetIdr,
            totalGoalsSavedIdr = totalGoalsSavedIdr,
            savingsProgressPercent = savingsProgressPercent
        )
    }

    /**
     * Evaluates budget progress and alert levels (SAFE < 70%, WARNING_70 >= 70%, WARNING_90 >= 90%, EXCEEDED >= 100%).
     * Handles refunds consistently and excludes transfers.
     */
    fun calculateBudgetProgress(
        budget: BudgetModel,
        periodTransactions: List<TransactionModel>
    ): BudgetProgressSummary {
        val relevantExpenses = periodTransactions.filter { tx ->
            !tx.isDeleted &&
                tx.type == TransactionType.EXPENSE &&
                (budget.categoryId == "ALL" || tx.categoryId == budget.categoryId)
        }.sumOf { it.amountIdr }

        val relevantRefunds = periodTransactions.filter { tx ->
            !tx.isDeleted &&
                tx.type == TransactionType.REFUND &&
                (budget.categoryId == "ALL" || tx.categoryId == budget.categoryId)
        }.sumOf { it.amountIdr }

        val netSpent = max(0L, relevantExpenses - relevantRefunds)
        val remaining = budget.limitAmountIdr - netSpent
        val utilization = if (budget.limitAmountIdr > 0L) {
            ((netSpent * 100L) / budget.limitAmountIdr).toInt()
        } else {
            0
        }

        val status = when {
            utilization >= 100 -> BudgetAlertLevel.EXCEEDED
            utilization >= 90 -> BudgetAlertLevel.WARNING_90
            utilization >= 70 -> BudgetAlertLevel.WARNING_70
            else -> BudgetAlertLevel.SAFE
        }

        return BudgetProgressSummary(
            budget = budget,
            spentAmountIdr = netSpent,
            remainingAmountIdr = remaining,
            utilizationPercent = utilization,
            statusLevel = status
        )
    }

    /**
     * Calculates expense distribution by category for charts and reports.
     */
    fun calculateCategoryBreakdown(
        periodTransactions: List<TransactionModel>,
        categories: List<TransactionCategoryModel>
    ): List<CategoryBreakdownItem> {
        val colorMap = categories.associate { it.id to it.colorHex }
        val activeExpenses = periodTransactions.filter { !it.isDeleted && it.type == TransactionType.EXPENSE }
        val refundsByCat = periodTransactions
            .filter { !it.isDeleted && it.type == TransactionType.REFUND }
            .groupBy { it.categoryId }
            .mapValues { entry -> entry.value.sumOf { it.amountIdr } }

        val grouped = activeExpenses.groupBy { it.categoryId }
        val netByCat = grouped.mapNotNull { (catId, txs) ->
            val gross = txs.sumOf { it.amountIdr }
            val refund = refundsByCat[catId] ?: 0L
            val net = max(0L, gross - refund)
            if (net > 0L) {
                Triple(catId, txs.first().categoryName, Pair(net, txs.size))
            } else {
                null
            }
        }

        val totalExpense = netByCat.sumOf { it.third.first }
        if (totalExpense <= 0L) return emptyList()

        return netByCat
            .sortedByDescending { it.third.first }
            .map { (catId, catName, pair) ->
                val (amount, count) = pair
                CategoryBreakdownItem(
                    categoryId = catId,
                    categoryName = catName,
                    amountIdr = amount,
                    percentage = ((amount.toDouble() / totalExpense.toDouble()) * 100.0).toFloat(),
                    colorHex = colorMap[catId] ?: "#49E0A0",
                    transactionCount = count
                )
            }
    }

    /**
     * Calculates explicit goal allocation preview for a given income amount,
     * capped safely so total allocation never exceeds available income unless overridden.
     */
    fun previewGoalAllocationsForIncome(
        incomeAmountIdr: Long,
        goals: List<FinancialGoalModel>
    ): Map<String, Long> {
        if (incomeAmountIdr <= 0L) return emptyMap()
        val result = mutableMapOf<String, Long>()
        var remainingIncome = incomeAmountIdr

        val eligibleGoals = goals
            .filter { !it.isCompleted && it.allocationRuleType != AllocationRuleType.MANUAL }
            .sortedBy { it.priority.ordinal }

        for (goal in eligibleGoals) {
            if (remainingIncome <= 0L) break
            val neededForGoal = max(0L, goal.targetAmountIdr - goal.currentSavedIdr)
            if (neededForGoal <= 0L) continue

            val rawAllocation = when (goal.allocationRuleType) {
                AllocationRuleType.FIXED_PER_INCOME -> goal.allocationValue
                AllocationRuleType.PERCENT_OF_INCOME -> (incomeAmountIdr * goal.allocationValue) / 100L
                AllocationRuleType.MANUAL -> 0L
            }
            val safeAllocation = min(min(rawAllocation, neededForGoal), remainingIncome)
            if (safeAllocation > 0L) {
                result[goal.id] = safeAllocation
                remainingIncome -= safeAllocation
            }
        }
        return result
    }

    /**
     * Projects months needed to reach a goal given a monthly savings rate in IDR.
     */
    fun estimateMonthsToReachGoal(
        targetAmountIdr: Long,
        currentSavedIdr: Long,
        monthlyContributionIdr: Long
    ): Int? {
        val remaining = max(0L, targetAmountIdr - currentSavedIdr)
        if (remaining == 0L) return 0
        if (monthlyContributionIdr <= 0L) return null
        return ceil(remaining.toDouble() / monthlyContributionIdr.toDouble()).roundToInt()
    }

    /**
     * Computes Business Profit & Loss and separates realized cash flow from unpaid receivables/payables.
     */
    fun calculateBusinessProfitLoss(
        transactions: List<TransactionModel>,
        invoices: List<InvoiceReferenceModel>
    ): BusinessProfitLossSummary {
        val activeTx = transactions.filter { !it.isDeleted }
        val realizedRevenue = activeTx
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amountIdr }
        val taxExpenses = activeTx
            .filter { it.type == TransactionType.EXPENSE && (it.categoryId == "cat_taxes" || it.categoryName.contains("Pajak", ignoreCase = true)) }
            .sumOf { it.amountIdr }
        val operatingExpenses = activeTx
            .filter { it.type == TransactionType.EXPENSE && it.categoryId != "cat_taxes" && !it.categoryName.contains("Pajak", ignoreCase = true) }
            .sumOf { it.amountIdr }
        val refunds = activeTx
            .filter { it.type == TransactionType.REFUND }
            .sumOf { it.amountIdr }

        val netOpEx = max(0L, operatingExpenses - refunds)
        val netProfit = realizedRevenue - netOpEx - taxExpenses

        val unpaidReceivables = invoices
            .filter { it.invoiceType == InvoiceType.RECEIVABLE && it.status in listOf(InvoiceStatus.UNPAID, InvoiceStatus.PARTIAL) }
            .sumOf { it.amountIdr }
        val unpaidPayables = invoices
            .filter { it.invoiceType == InvoiceType.PAYABLE && it.status in listOf(InvoiceStatus.UNPAID, InvoiceStatus.PARTIAL) }
            .sumOf { it.amountIdr }

        val projectSummaries = activeTx
            .filter { it.projectCostCenterId.isNotBlank() && it.type in listOf(TransactionType.INCOME, TransactionType.EXPENSE) }
            .groupBy { it.projectCostCenterId }
            .mapValues { (_, list) ->
                val inc = list.filter { it.type == TransactionType.INCOME }.sumOf { it.amountIdr }
                val exp = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountIdr }
                inc - exp
            }

        return BusinessProfitLossSummary(
            realizedRevenueIdr = realizedRevenue,
            operatingExpensesIdr = netOpEx,
            taxExpensesIdr = taxExpenses,
            netProfitIdr = netProfit,
            unpaidReceivablesIdr = unpaidReceivables,
            unpaidPayablesIdr = unpaidPayables,
            projectSummaries = projectSummaries
        )
    }

    /**
     * Filters transactions by DashboardPeriod relative to referenceTimeMillis.
     */
    fun filterTransactionsByPeriod(
        transactions: List<TransactionModel>,
        period: DashboardPeriod,
        referenceTimeMillis: Long = System.currentTimeMillis()
    ): List<TransactionModel> {
        if (period == DashboardPeriod.ALL) return transactions.filter { !it.isDeleted }
        val cal = Calendar.getInstance().apply {
            timeInMillis = referenceTimeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMillis = when (period) {
            DashboardPeriod.TODAY -> cal.timeInMillis
            DashboardPeriod.WEEK -> {
                cal.add(Calendar.DAY_OF_YEAR, -6)
                cal.timeInMillis
            }
            DashboardPeriod.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.timeInMillis
            }
            DashboardPeriod.YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            DashboardPeriod.ALL -> 0L
        }
        return transactions.filter { !it.isDeleted && it.transactionDateMillis >= startMillis }
    }
}
