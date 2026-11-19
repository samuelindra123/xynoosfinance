package com.example

import android.content.Context
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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

    @Test
    fun githubUpdateChecker_parsesMandatoryAndOptionalReleasesAndApkAssets() {
        val mandatoryJson = """
            {
              "tag_name": "v2.1.0",
              "name": "XynoosFinance v2.1.0",
              "draft": false,
              "html_url": "https://github.com/xynoos/XynoosFinance/releases/tag/v2.1.0",
              "body": "## Rilis Baru\\n- Tambah fitur trigger update\\n<!-- XYNOOS_META:versionCode=5;minVersionCode=5;mandatory=true -->",
              "assets": [
                {
                  "name": "XynoosFinance-v2.1.0-Debug.apk",
                  "browser_download_url": "https://github.com/xynoos/XynoosFinance/releases/download/v2.1.0/XynoosFinance-v2.1.0-Debug.apk"
                },
                {
                  "name": "XynoosFinance-v2.1.0-Release.apk",
                  "browser_download_url": "https://github.com/xynoos/XynoosFinance/releases/download/v2.1.0/XynoosFinance-v2.1.0-Release.apk"
                }
              ]
            }
        """.trimIndent()

        val result = com.example.data.remote.GitHubUpdateChecker.parseReleaseJson(
            jsonString = mandatoryJson,
            currentVersionCode = 2,
            currentVersionName = "2.0.0"
        )
        assertTrue(result is com.example.data.remote.GitHubUpdateCheckResult.UpdateAvailable)
        val info = (result as com.example.data.remote.GitHubUpdateCheckResult.UpdateAvailable).update
        assertTrue(info.isMandatory)
        assertEquals("v2.1.0", info.tagName)
        assertEquals(5, info.remoteVersionCode)
        assertTrue(info.apkDownloadUrl.endsWith("-Release.apk"))
        assertTrue(!info.releaseNotes.contains("XYNOOS_META"))

        val upToDate = com.example.data.remote.GitHubUpdateChecker.parseReleaseJson(
            jsonString = mandatoryJson,
            currentVersionCode = 5,
            currentVersionName = "2.1.0"
        )
        assertTrue(upToDate is com.example.data.remote.GitHubUpdateCheckResult.UpToDate)
    }

    @org.junit.Rule
    @JvmField
    val composeTestRule = androidx.compose.ui.test.junit4.createComposeRule()

    @Test
    fun mainActivity_coldLaunchAndSplashTransition_completesWithoutCrash() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        org.junit.Assert.assertNotNull(activity)
        org.robolectric.shadows.ShadowLooper.idleMainLooper(900L, java.util.concurrent.TimeUnit.MILLISECONDS)
        controller.pause().stop().destroy()
    }

    @Test
    fun allPrimaryScreensAndDialogs_renderAndNavigateWithoutCrash() {
        val prefsRepo = com.example.data.local.UserPreferencesRepository(context)
        val offlineFirstRepo = OfflineFirstFinanceRepository(
            dao = database.financeDao(),
            cloudRepo = null,
            userId = "ui_test_user"
        )
        val viewModel = com.example.ui.viewmodel.XynoosViewModel(
            repository = offlineFirstRepo,
            cloudRepo = null,
            prefsRepo = prefsRepo,
            currentUserId = "ui_test_user",
            userEmail = "test@xynoos.id",
            appContext = context
        )
        runBlocking {
            prefsRepo.completeOnboarding(
                displayName = "Pengguna Uji",
                currencyCode = "IDR",
                profileType = com.example.domain.FinancialProfileType.COMBINED,
                remindersEnabled = false,
                themeMode = com.example.domain.AppThemeMode.DARK
            )
            offlineFirstRepo.ensureDefaultCategoriesSeeded()
            offlineFirstRepo.saveAccount(
                FinancialAccountModel(
                    id = "acc_test_1",
                    userId = "ui_test_user",
                    name = "BCA Utama",
                    accountType = AccountType.BANK,
                    openingBalanceIdr = 10_000_000L,
                    currentBalanceIdr = 10_000_000L,
                    colorHex = "#49E0A0"
                )
            )
            offlineFirstRepo.saveTransaction(
                TransactionModel(
                    id = "tx_ui_1",
                    userId = "ui_test_user",
                    type = TransactionType.EXPENSE,
                    amountIdr = 50_000L,
                    title = "Beli Bensin",
                    categoryId = "cat_fuel",
                    categoryName = "Beli Bensin",
                    accountId = "acc_test_1",
                    accountName = "BCA Utama",
                    transactionDateMillis = System.currentTimeMillis()
                )
            )
        }

        composeTestRule.setContent {
            com.example.ui.theme.XynoosFinanceTheme(themeMode = com.example.domain.AppThemeMode.DARK) {
                XynoosAuthenticatedWorkspace(
                    viewModel = viewModel,
                    onSignOut = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_dashboard_screen").assertExists()

        // Navigate across all bottom tabs
        composeTestRule.onNodeWithTag("nav_tab_transactions").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nav_tab_usd_market").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("usd_live_market_subscreen").assertExists()

        composeTestRule.onNodeWithTag("nav_tab_goals").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tab_budgets_chip").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tab_monthly_annual_chip").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nav_tab_ai").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nav_tab_more").performClick()
        composeTestRule.waitForIdle()

        // Back to home -> Monthly/Annual dedicated screen -> back -> Quick Add Transaction form
        composeTestRule.onNodeWithTag("nav_tab_home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_dashboard_screen")
            .performScrollToNode(hasTestTag("home_open_monthly_annual_card"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_open_monthly_annual_card").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("monthly_annual_budget_subscreen").assertExists()
        composeTestRule.onNodeWithTag("monthly_annual_panel_list")
            .performScrollToNode(hasTestTag("scope_annual_chip"))
        composeTestRule.onNodeWithTag("scope_annual_chip").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("monthly_annual_back_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("fab_quick_add_transaction").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("transaction_form_sheet").assertExists()
    }

    @Test
    fun onboardingAndPatternLockAndUpdateDialogAndWelcome_renderCleanly() {
        composeTestRule.setContent {
            com.example.ui.theme.XynoosFinanceTheme(themeMode = com.example.domain.AppThemeMode.DARK) {
                com.example.ui.screens.OnboardingWizardScreen(
                    initialDisplayName = "Budi",
                    currentThemeMode = com.example.domain.AppThemeMode.DARK,
                    onThemeChanged = {},
                    onCompleteOnboarding = { _, _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("onboarding_screen").assertExists()
        repeat(4) {
            composeTestRule.onNodeWithTag("onboarding_next_button").performClick()
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun welcomeAuthAndPatternLockAndUpdateDialog_andAppCheckHelpers_executeSafely() {
        com.example.data.remote.configureAppCheck(context, null)
        com.example.notifications.ReminderNotificationHelper.ensureChannelCreated(context)
        com.example.notifications.ReminderNotificationHelper.sendPreviewReminderNotification(
            context = context,
            title = "Uji Notifikasi",
            availableCashIdr = 1_000_000L,
            isPrivacyMode = false
        )

        val patternHash = com.example.ui.components.PatternLockUtil.hashPattern(listOf(0, 1, 2, 5))
        val updateInfo = com.example.data.remote.AppUpdateInfo(
            tagName = "v2.0.2",
            versionName = "2.0.2",
            remoteVersionCode = 10,
            minRequiredVersionCode = 10,
            isMandatory = true,
            releaseTitle = "Rilis Wajib",
            releaseNotes = "Perbaikan force close",
            apkDownloadUrl = "https://github.com/samuelindra123/xynoosfinance/releases/latest",
            releasePageUrl = "https://github.com/samuelindra123/xynoosfinance/releases/latest"
        )

        composeTestRule.setContent {
            com.example.ui.theme.XynoosFinanceTheme(themeMode = com.example.domain.AppThemeMode.DARK) {
                androidx.compose.foundation.layout.Box {
                    com.example.ui.screens.WelcomeAuthScreen(onAuthSuccess = {})
                    com.example.ui.components.PatternLockGateScreen(
                        displayName = "Samuel",
                        storedPatternHash = patternHash,
                        onUnlocked = {},
                        onForgotPatternSignOut = {}
                    )
                    com.example.ui.components.AppUpdatePromptDialog(
                        updateInfo = updateInfo,
                        currentVersionLabel = "v2.0.1 (Build 3)",
                        isChecking = false,
                        onUpdateNow = {},
                        onRecheck = {},
                        onRemindLater = {},
                        onSkipVersion = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pattern_lock_gate_screen").assertExists()
        composeTestRule.onNodeWithTag("app_update_prompt_dialog").assertExists()
    }
}
