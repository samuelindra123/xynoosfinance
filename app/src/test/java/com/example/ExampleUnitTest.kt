package com.example

import com.example.ai.GroundedAiAssistant
import com.example.domain.AccountType
import com.example.domain.AllocationRuleType
import com.example.domain.BudgetAlertLevel
import com.example.domain.BudgetModel
import com.example.domain.DefaultCategories
import com.example.domain.FinancialAccountModel
import com.example.domain.FinancialCalculator
import com.example.domain.FinancialGoalModel
import com.example.domain.GoalPriority
import com.example.domain.IdrFormatter
import com.example.domain.InvoiceReferenceModel
import com.example.domain.InvoiceStatus
import com.example.domain.InvoiceType
import com.example.domain.TransactionModel
import com.example.domain.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun idrFormatter_formatsAndParsesIndonesianAmountsDeterministically() {
        assertEquals("Rp0", IdrFormatter.format(0L))
        assertEquals("Rp1.250.000", IdrFormatter.format(1_250_000L))
        assertEquals("-Rp500.000", IdrFormatter.format(-500_000L))
        assertEquals("+Rp250.000", IdrFormatter.format(250_000L, showPlusSign = true))
        assertEquals("Rp ••••••••", IdrFormatter.format(10_000_000L, isPrivacyHidden = true))

        assertEquals(35_000L, IdrFormatter.parseIdrInput("35 ribu"))
        assertEquals(35_000L, IdrFormatter.parseIdrInput("35rb"))
        assertEquals(2_500_000L, IdrFormatter.parseIdrInput("2,5 juta"))
        assertEquals(1_250_000L, IdrFormatter.parseIdrInput("Rp 1.250.000"))
    }

    @Test
    fun financialCalculator_newUserZeroState_returnsExactZeroBalances() {
        val overview = FinancialCalculator.calculateOverview(
            accounts = emptyList(),
            allTransactions = emptyList(),
            periodTransactions = emptyList(),
            budgets = emptyList(),
            goals = emptyList()
        )
        assertEquals(0L, overview.totalAssetsIdr)
        assertEquals(0L, overview.totalLiabilitiesIdr)
        assertEquals(0L, overview.netWorthIdr)
        assertEquals(0L, overview.availableCashIdr)
        assertEquals(0L, overview.periodIncomeIdr)
        assertEquals(0L, overview.periodExpenseIdr)
        assertEquals(0L, overview.netCashFlowIdr)
    }

    @Test
    fun financialCalculator_internalTransfer_updatesBothAccountsWithoutInflatingIncomeOrExpense() {
        val bankAcc = FinancialAccountModel(
            id = "acc_bca",
            userId = "u1",
            name = "BCA Utama",
            accountType = AccountType.BANK,
            openingBalanceIdr = 10_000_000L,
            currentBalanceIdr = 10_000_000L,
            colorHex = "#49E0A0"
        )
        val walletAcc = FinancialAccountModel(
            id = "acc_gopay",
            userId = "u1",
            name = "GoPay",
            accountType = AccountType.EWALLET,
            openingBalanceIdr = 500_000L,
            currentBalanceIdr = 500_000L,
            colorHex = "#72A7FF"
        )

        val now = 1_760_000_000_000L
        val txs = listOf(
            TransactionModel(
                id = "tx_inc",
                userId = "u1",
                type = TransactionType.INCOME,
                amountIdr = 5_000_000L,
                title = "Gaji",
                categoryId = "cat_salary",
                categoryName = "Gaji Utama",
                accountId = "acc_bca",
                accountName = "BCA Utama",
                transactionDateMillis = now
            ),
            TransactionModel(
                id = "tx_exp",
                userId = "u1",
                type = TransactionType.EXPENSE,
                amountIdr = 1_000_000L,
                title = "Belanja",
                categoryId = "cat_food",
                categoryName = "Makanan & Minuman",
                accountId = "acc_bca",
                accountName = "BCA Utama",
                transactionDateMillis = now
            ),
            TransactionModel(
                id = "tx_trf",
                userId = "u1",
                type = TransactionType.TRANSFER,
                amountIdr = 2_000_000L,
                title = "Topup GoPay",
                categoryId = "cat_transfer",
                categoryName = "Transfer Internal",
                accountId = "acc_bca",
                accountName = "BCA Utama",
                toAccountId = "acc_gopay",
                toAccountName = "GoPay",
                transactionDateMillis = now
            )
        )

        val bcaBalance = FinancialCalculator.calculateAccountBalance(bankAcc, txs)
        val gopayBalance = FinancialCalculator.calculateAccountBalance(walletAcc, txs)

        // BCA: 10M + 5M - 1M - 2M (transfer out) = 12M
        assertEquals(12_000_000L, bcaBalance)
        // GoPay: 500k + 2M (transfer in) = 2.5M
        assertEquals(2_500_000L, gopayBalance)

        val updatedAccounts = listOf(
            bankAcc.copy(currentBalanceIdr = bcaBalance),
            walletAcc.copy(currentBalanceIdr = gopayBalance)
        )
        val overview = FinancialCalculator.calculateOverview(
            accounts = updatedAccounts,
            allTransactions = txs,
            periodTransactions = txs,
            budgets = emptyList(),
            goals = emptyList()
        )

        // Total assets remain 14.5M, and transfer is NOT counted in income or expense
        assertEquals(14_500_000L, overview.totalAssetsIdr)
        assertEquals(5_000_000L, overview.periodIncomeIdr)
        assertEquals(1_000_000L, overview.periodExpenseIdr)
        assertEquals(4_000_000L, overview.netCashFlowIdr)
    }

    @Test
    fun financialCalculator_budgetProgressAndGoalForecast_areDeterministic() {
        val now = 1_760_000_000_000L
        val budget = BudgetModel(
            id = "b1",
            userId = "u1",
            name = "Anggaran Makan",
            categoryId = "cat_food",
            categoryName = "Makanan & Minuman",
            limitAmountIdr = 2_000_000L,
            periodYearMonth = "2026-10",
            alertThresholdPercent = 80
        )
        val txs = listOf(
            TransactionModel(
                id = "t1",
                userId = "u1",
                type = TransactionType.EXPENSE,
                amountIdr = 1_700_000L,
                title = "Katering Bulanan",
                categoryId = "cat_food",
                categoryName = "Makanan & Minuman",
                accountId = "acc_bca",
                accountName = "BCA",
                transactionDateMillis = now
            )
        )
        val progress = FinancialCalculator.calculateBudgetProgress(budget, txs)
        assertEquals(1_700_000L, progress.spentAmountIdr)
        assertEquals(300_000L, progress.remainingAmountIdr)
        assertEquals(85, progress.utilizationPercent)
        assertEquals(BudgetAlertLevel.WARNING_70, progress.statusLevel)

        val goal = FinancialGoalModel(
            id = "g1",
            userId = "u1",
            name = "Dana Darurat",
            targetAmountIdr = 15_000_000L,
            currentSavedIdr = 6_000_000L,
            targetDateMillis = now + (90L * 24L * 3600L * 1000L),
            priority = GoalPriority.HIGH,
            iconKey = "trending_up",
            allocationRuleType = AllocationRuleType.PERCENT_OF_INCOME,
            allocationValue = 20L
        )
        val monthsNeeded = FinancialCalculator.estimateMonthsToReachGoal(
            targetAmountIdr = goal.targetAmountIdr,
            currentSavedIdr = goal.currentSavedIdr,
            monthlyContributionIdr = 3_000_000L
        )
        assertEquals(3, monthsNeeded)

        val previewMap = FinancialCalculator.previewGoalAllocationsForIncome(
            incomeAmountIdr = 10_000_000L,
            goals = listOf(goal)
        )
        assertEquals(2_000_000L, previewMap["g1"])
    }

    @Test
    fun businessProfitLoss_separatesRealizedCashFromUnpaidInvoices() {
        val now = 1_760_000_000_000L
        val txs = listOf(
            TransactionModel(
                id = "tx_rev",
                userId = "u1",
                type = TransactionType.INCOME,
                amountIdr = 20_000_000L,
                title = "Proyek Aplikasi",
                categoryId = "cat_business_rev",
                categoryName = "Pendapatan Bisnis",
                accountId = "acc_bca",
                accountName = "BCA",
                projectCostCenterId = "PRJ-ALPHA",
                transactionDateMillis = now
            ),
            TransactionModel(
                id = "tx_ops",
                userId = "u1",
                type = TransactionType.EXPENSE,
                amountIdr = 4_500_000L,
                title = "Server Cloud",
                categoryId = "cat_biz_ops",
                categoryName = "Operasional Bisnis",
                accountId = "acc_bca",
                accountName = "BCA",
                projectCostCenterId = "PRJ-ALPHA",
                transactionDateMillis = now
            )
        )
        val invoices = listOf(
            InvoiceReferenceModel(
                id = "inv1",
                userId = "u1",
                invoiceNumber = "INV-01",
                counterpartyName = "PT Nusantara",
                invoiceType = InvoiceType.RECEIVABLE,
                amountIdr = 8_000_000L,
                dueDateMillis = 1_765_000_000_000L,
                status = InvoiceStatus.UNPAID
            )
        )
        val pl = FinancialCalculator.calculateBusinessProfitLoss(txs, invoices)
        assertEquals(20_000_000L, pl.realizedRevenueIdr)
        assertEquals(4_500_000L, pl.operatingExpensesIdr)
        assertEquals(15_500_000L, pl.netProfitIdr)
        assertEquals(8_000_000L, pl.unpaidReceivablesIdr)
        assertEquals(15_500_000L, pl.projectSummaries["PRJ-ALPHA"])
    }

    @Test
    fun groundedAiAssistant_parsesIndonesianNaturalTransactionInput() {
        val assistant = GroundedAiAssistant()
        val cats = DefaultCategories.getDefaults("u1")
        val accounts = listOf(
            FinancialAccountModel(
                id = "acc_cash",
                userId = "u1",
                name = "Tunai (Cash)",
                accountType = AccountType.CASH,
                openingBalanceIdr = 500_000L,
                currentBalanceIdr = 500_000L,
                colorHex = "#49E0A0"
            )
        )
        val draft = assistant.parseNaturalTransactionDraft(
            prompt = "Bayar makan siang 35 ribu pakai cash.",
            accounts = accounts,
            categories = cats
        )
        assertNotNull(draft)
        assertEquals(TransactionType.EXPENSE, draft?.type)
        assertEquals(35_000L, draft?.amountIdr)
        assertEquals("Makanan & Minuman", draft?.categoryName)
        assertTrue(draft!!.accountName.contains("Tunai", ignoreCase = true))
    }

    @Test
    fun autoConnectedGoalsAndUsdFormatting_workDeterministically() {
        val goal = FinancialGoalModel(
            id = "g_auto",
            userId = "u1",
            name = "Beli Rumah",
            targetAmountIdr = 100_000_000L,
            currentSavedIdr = 5_000_000L,
            targetDateMillis = 1_790_000_000_000L,
            priority = GoalPriority.HIGH,
            iconKey = "trending_up"
        )
        val effectiveSaved = FinancialCalculator.calculateEffectiveGoalSavedIdr(
            goal = goal,
            availableCashIdr = 42_500_000L,
            totalAssetsIdr = 45_000_000L
        )
        assertEquals(42_500_000L, effectiveSaved)

        val usdFormatted = IdrFormatter.formatUsdFromIdr(16_250_000L, 16250.0, isPrivacyHidden = false)
        assertEquals("$1,000.00 USD", usdFormatted)

        assertEquals("15.250.000", IdrFormatter.formatGroupedInputString("15250000"))
        assertEquals("Lima Belas Juta Dua Ratus Lima Puluh Ribu Rupiah", IdrFormatter.formatTerbilang(15_250_000L))
        assertEquals("JUTAAN (Jt) • 8 Digit", IdrFormatter.formatNominalScaleBadge(15_250_000L))
        assertEquals("15 Juta + 250 Ribu", IdrFormatter.formatDigitBreakdown(15_250_000L))
    }
}



