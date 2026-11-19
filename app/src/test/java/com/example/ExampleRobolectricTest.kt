package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UserPreferencesState
import com.example.data.local.XynoosDatabase
import com.example.data.repository.OfflineFirstFinanceRepository
import com.example.domain.AccountType
import com.example.domain.ContributionType
import com.example.domain.FinancialAccountModel
import com.example.domain.FinancialCalculator
import com.example.domain.FinancialGoalModel
import com.example.domain.GoalPriority
import com.example.domain.TransactionModel
import com.example.domain.TransactionType
import com.example.export.PdfReportConfig
import com.example.export.PdfReportGenerator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: XynoosDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, XynoosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun appName_matchesXynoosFinanceIdentity() {
        val appName = context.getString(R.string.app_name)
        assertEquals("XynoosFinance", appName)
    }

    @Test
    fun offlineFirstRepository_atomicTransactionAndGoalContribution_updatesBalancesExactly() = runBlocking {
        val userId = "test_exec_user"
        val dao = database.financeDao()
        val repo = OfflineFirstFinanceRepository(
            dao = dao,
            cloudRepo = null,
            userId = userId
        )

        repo.ensureDefaultCategoriesSeeded()
        val categories = dao.getCategories(userId)
        assertTrue(categories.isNotEmpty())

        val bca = FinancialAccountModel(
            id = "acc_bca_1",
            userId = userId,
            name = "BCA Prioritas",
            accountType = AccountType.BANK,
            openingBalanceIdr = 25_000_000L,
            currentBalanceIdr = 25_000_000L,
            colorHex = "#49E0A0"
        )
        repo.saveAccount(bca)

        val expenseTx = TransactionModel(
            id = "tx_101",
            userId = userId,
            type = TransactionType.EXPENSE,
            amountIdr = 1_500_000L,
            title = "Sewa Ruang Kerja",
            categoryId = "cat_housing",
            categoryName = "Tempat Tinggal",
            accountId = bca.id,
            accountName = bca.name,
            transactionDateMillis = 1_760_000_000_000L
        )
        repo.saveTransaction(expenseTx)

        val accountsAfterExpense = dao.getAccounts(userId)
        assertEquals(23_500_000L, accountsAfterExpense.first { it.id == bca.id }.currentBalanceIdr)

        // Delete transaction and verify atomic balance restoration
        repo.deleteTransaction(expenseTx.id)
        val accountsAfterDelete = dao.getAccounts(userId)
        assertEquals(25_000_000L, accountsAfterDelete.first { it.id == bca.id }.currentBalanceIdr)

        // Create savings goal & record deposit
        val goal = FinancialGoalModel(
            id = "goal_1",
            userId = userId,
            name = "Ekspansi Studio",
            targetAmountIdr = 50_000_000L,
            currentSavedIdr = 10_000_000L,
            targetDateMillis = System.currentTimeMillis() + 86_400_000L * 120,
            priority = GoalPriority.HIGH,
            iconKey = "trending_up"
        )
        repo.saveGoal(goal)
        val updatedGoal = repo.recordGoalContribution(goal.id, 5_000_000L, ContributionType.DEPOSIT, "Setoran bulan Oktober")
        assertEquals(15_000_000L, updatedGoal?.currentSavedIdr)
    }

    @Test
    fun pdfReportGenerator_generatesMultiPageFinancialReportFile() {
        val prefs = UserPreferencesState(
            onboardingCompleted = true,
            displayName = "Budi Santoso",
            reportOrgName = "Xynoos Digital Nusantara",
            reportSignatory = "Budi Santoso, Direktur"
        )
        val account = FinancialAccountModel(
            id = "acc_1",
            userId = "u1",
            name = "Mandiri Utama",
            accountType = AccountType.BANK,
            openingBalanceIdr = 50_000_000L,
            currentBalanceIdr = 62_000_000L,
            colorHex = "#49E0A0"
        )
        val txs = (1..35).map { idx ->
            TransactionModel(
                id = "tx_$idx",
                userId = "u1",
                type = if (idx % 3 == 0) TransactionType.INCOME else TransactionType.EXPENSE,
                amountIdr = 500_000L * idx,
                title = "Transaksi Operasional #$idx",
                categoryId = "cat_biz_ops",
                categoryName = "Operasional Bisnis",
                accountId = account.id,
                accountName = account.name,
                transactionDateMillis = 1_760_000_000_000L
            )
        }
        val overview = FinancialCalculator.calculateOverview(
            accounts = listOf(account),
            allTransactions = txs,
            periodTransactions = txs,
            budgets = emptyList(),
            goals = emptyList()
        )
        val annualSummary = FinancialCalculator.calculateAnnualLedgerSummary(
            year = 2025,
            allTransactions = txs,
            categories = emptyList(),
            budgets = emptyList()
        )
        val typeBreakdown = FinancialCalculator.calculateTypeBreakdown(txs)
        val accountActivities = FinancialCalculator.calculateAccountActivities(listOf(account), txs)
        val incomeBreakdown = FinancialCalculator.calculateIncomeCategoryBreakdown(txs, emptyList())
        val expenseBreakdown = FinancialCalculator.calculateCategoryBreakdown(txs, emptyList())

        val pdfResult = PdfReportGenerator.generateFinancialReportPdf(
            context = context,
            config = PdfReportConfig(
                reportTitle = "Laporan Keuangan Kuartal IV",
                periodLabel = "Tahun 2025",
                scope = com.example.export.PdfReportScope.FULL_YEAR
            ),
            prefs = prefs,
            overview = overview,
            accounts = listOf(account),
            transactions = txs,
            categoryBreakdown = expenseBreakdown,
            incomeCategoryBreakdown = incomeBreakdown,
            typeBreakdown = typeBreakdown,
            accountActivities = accountActivities,
            annualSummary = annualSummary,
            usdRateIdr = 16250.0,
            budgets = emptyList(),
            goals = emptyList(),
            businessPl = FinancialCalculator.calculateBusinessProfitLoss(txs, emptyList()),
            invoices = emptyList()
        )
        assertTrue(pdfResult.isSuccess)
        val info = pdfResult.getOrThrow()
        assertTrue(info.file.exists())
        assertTrue(info.file.length() > 0L)
        assertTrue(info.pageCount >= 2)

        val pngResult = com.example.export.StatusImageExporter.generateStatusImagePng(
            context = context,
            prefs = prefs,
            overview = overview,
            usdMarket = com.example.data.remote.LiveUsdMarketState(currentRateIdr = 16250.0),
            goals = emptyList(),
            periodLabel = "Bulan Ini",
            categoryBreakdown = expenseBreakdown,
            accounts = listOf(account),
            annualSummary = annualSummary
        )
        assertTrue(pngResult.isSuccess)
        val pngInfo = pngResult.getOrThrow()
        assertTrue(pngInfo.file.exists())
        assertTrue(pngInfo.file.length() > 0L)
    }

    @Test
    fun persistentAiSessionsAndIncrementalUnsyncedQueries_workAcrossReopens() = runBlocking {
        val userId = "ai_session_user"
        val dao = database.financeDao()
        val repo = OfflineFirstFinanceRepository(
            dao = dao,
            cloudRepo = null,
            userId = userId
        )

        val defaultSessionId = repo.ensureDefaultAiSession("Halo, ada yang bisa dibantu?")
        val s2 = repo.createAiSession("Audit Bulan Ini", "Siap menganalisis bulan ini.")
        assertTrue(s2.id != defaultSessionId)

        repo.saveAiMessage(
            com.example.domain.AiMessageModel(
                id = "msg_u1",
                sessionId = s2.id,
                userId = userId,
                role = "user",
                content = "Bagaimana kondisi pengeluaran bulan ini?",
                timestampMillis = 1_760_000_100_000L
            )
        )
        val loadedMessages = repo.getAiMessagesForSession(s2.id)
        assertEquals(2, loadedMessages.size)
        assertTrue(loadedMessages.any { it.id == "msg_u1" })

        repo.renameAiSession(s2.id, "Evaluasi Pengeluaran Oktober")
        val sessions = dao.getAiSessions(userId)
        assertEquals("Evaluasi Pengeluaran Oktober", sessions.first { it.id == s2.id }.title)

        repo.deleteAiSession(s2.id)
        assertEquals(0, repo.getAiMessagesForSession(s2.id).size)

        // Check incremental unsynced detection when offline
        val acc = FinancialAccountModel(
            id = "acc_offline",
            userId = userId,
            name = "Dompet Tunai",
            accountType = AccountType.CASH,
            openingBalanceIdr = 250_000L,
            currentBalanceIdr = 250_000L,
            colorHex = "#49E0A0"
        )
        repo.saveAccount(acc)
        assertTrue(dao.getUnsyncedAccounts(userId).any { it.id == "acc_offline" })
    }
}


