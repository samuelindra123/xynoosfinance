package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.ai.GroundedAiAssistant
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserPreferencesState
import com.example.data.remote.AppUpdateInfo
import com.example.data.remote.GitHubUpdateCheckResult
import com.example.data.remote.GitHubUpdateChecker
import com.example.data.remote.LiveUsdMarketState
import com.example.data.remote.LiveUsdRateRepository
import com.example.data.repository.FirestoreFinanceRepository
import com.example.data.repository.OfflineFirstFinanceRepository
import com.example.domain.AccountType
import com.example.domain.AiMessageModel
import com.example.domain.AiSessionModel
import com.example.domain.AllocationRuleType
import com.example.domain.AnnualLedgerSummary
import com.example.domain.AppThemeMode
import com.example.domain.BudgetAlertLevel
import com.example.domain.BudgetModel
import com.example.domain.BudgetProgressSummary
import com.example.domain.BusinessProfitLossSummary
import com.example.domain.CategoryBreakdownItem
import com.example.domain.ContributionType
import com.example.domain.DashboardPeriod
import com.example.domain.FinancialAccountModel
import com.example.domain.FinancialCalculator
import com.example.domain.FinancialGoalModel
import com.example.domain.FinancialOverviewSummary
import com.example.domain.FinancialProfileType
import com.example.domain.GoalContributionModel
import com.example.domain.GoalPriority
import com.example.domain.InvoiceReferenceModel
import com.example.domain.InvoiceStatus
import com.example.domain.InvoiceType
import com.example.domain.MonthlyLedgerSummary
import com.example.domain.ProposedTransactionDraft
import com.example.domain.RecurringTransactionModel
import com.example.domain.TransactionCategoryModel
import com.example.domain.TransactionModel
import com.example.domain.TransactionType
import com.example.export.GeneratedPdfInfo
import com.example.export.GeneratedStatusImageInfo
import com.example.export.PdfReportConfig
import com.example.export.PdfReportGenerator
import com.example.export.PdfReportScope
import com.example.export.StatusImageExporter
import com.example.notifications.ReminderNotificationHelper
import com.example.notifications.WhatsAppNotifierHelper
import com.example.sync.AutoBackupStatusState
import com.example.sync.CloudAutoBackupManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class XynoosUiState(
    val userId: String = "",
    val prefs: UserPreferencesState = UserPreferencesState(),
    val selectedPeriod: DashboardPeriod = DashboardPeriod.MONTH,
    val accounts: List<FinancialAccountModel> = emptyList(),
    val categories: List<TransactionCategoryModel> = emptyList(),
    val allTransactions: List<TransactionModel> = emptyList(),
    val periodTransactions: List<TransactionModel> = emptyList(),
    val filteredTransactions: List<TransactionModel> = emptyList(),
    val budgets: List<BudgetModel> = emptyList(),
    val budgetSummaries: List<BudgetProgressSummary> = emptyList(),
    val goals: List<FinancialGoalModel> = emptyList(),
    val goalContributions: List<GoalContributionModel> = emptyList(),
    val recurringTransactions: List<RecurringTransactionModel> = emptyList(),
    val invoices: List<InvoiceReferenceModel> = emptyList(),
    val overview: FinancialOverviewSummary = FinancialOverviewSummary(
        0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0, 0L, 0L, 0
    ),
    val categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val businessProfitLoss: BusinessProfitLossSummary = BusinessProfitLossSummary(
        0L, 0L, 0L, 0L, 0L, 0L, emptyMap()
    ),
    val analyticsYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val analyticsMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val availableYears: List<Int> = listOf(Calendar.getInstance().get(Calendar.YEAR)),
    val analyticsSearchQuery: String = "",
    val analyticsTypeFilter: TransactionType? = null,
    val analyticsCategoryFilter: String? = null,
    val monthlyLedgerSummary: MonthlyLedgerSummary = MonthlyLedgerSummary(
        year = Calendar.getInstance().get(Calendar.YEAR),
        monthIndex = Calendar.getInstance().get(Calendar.MONTH),
        monthLabel = "",
        shortMonthLabel = "",
        incomeIdr = 0L,
        grossExpenseIdr = 0L,
        refundIdr = 0L,
        netExpenseIdr = 0L,
        transferIdr = 0L,
        adjustmentIdr = 0L,
        netCashFlowIdr = 0L,
        savingsRatePercent = 0,
        transactionCount = 0,
        typeBreakdown = emptyList(),
        expenseCategoryBreakdown = emptyList(),
        incomeCategoryBreakdown = emptyList(),
        budgetSummaries = emptyList(),
        transactions = emptyList()
    ),
    val annualLedgerSummary: AnnualLedgerSummary = AnnualLedgerSummary(
        year = Calendar.getInstance().get(Calendar.YEAR),
        totalIncomeIdr = 0L,
        totalGrossExpenseIdr = 0L,
        totalRefundIdr = 0L,
        totalNetExpenseIdr = 0L,
        totalTransferIdr = 0L,
        totalAdjustmentIdr = 0L,
        netCashFlowIdr = 0L,
        savingsRatePercent = 0,
        transactionCount = 0,
        averageMonthlyIncomeIdr = 0L,
        averageMonthlyExpenseIdr = 0L,
        highestIncomeMonthLabel = "-",
        highestExpenseMonthLabel = "-",
        monthlyBreakdown = emptyList(),
        expenseCategoryBreakdown = emptyList(),
        incomeCategoryBreakdown = emptyList(),
        typeBreakdown = emptyList()
    ),
    val filteredMonthlyTransactions: List<TransactionModel> = emptyList(),
    val usdMarket: LiveUsdMarketState = LiveUsdMarketState(),
    val searchQuery: String = "",
    val selectedTypeFilter: TransactionType? = null,
    val selectedAccountIdFilter: String? = null,
    val sortByAmountDesc: Boolean = false,
    val aiSessions: List<AiSessionModel> = emptyList(),
    val activeAiSessionId: String = "",
    val aiMessages: List<AiMessageModel> = emptyList(),
    val isAiLoading: Boolean = false,
    val pendingAiDraft: ProposedTransactionDraft? = null,
    val isSyncing: Boolean = false,
    val lastSyncMessage: String = "Siap sinkronisasi",
    val autoBackupStatus: AutoBackupStatusState = AutoBackupStatusState(),
    val lastGeneratedPdf: GeneratedPdfInfo? = null,
    val lastGeneratedStatusImage: GeneratedStatusImageInfo? = null,
    val statusBannerMessage: String? = null,
    val availableUpdate: AppUpdateInfo? = null,
    val isCheckingUpdate: Boolean = false,
    val updateStatusText: String = "",
    val effectiveGithubRepoSlug: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
class XynoosViewModel(
    private val repository: OfflineFirstFinanceRepository,
    private val cloudRepo: FirestoreFinanceRepository?,
    private val prefsRepo: UserPreferencesRepository,
    private val currentUserId: String,
    private val userEmail: String?,
    appContext: Context? = null
) : ViewModel() {

    private val applicationContext: Context? = appContext?.applicationContext
    private val aiAssistant = GroundedAiAssistant()
    private val usdRateRepository = LiveUsdRateRepository()
    private val safeCoroutineHandler = CoroutineExceptionHandler { _, _ -> }

    private val defaultAiWelcomeMessage =
        "Halo! Saya **Xynoos AI Finance Advisor**. Riwayat chat per sesi kini tersimpan otomatis agar tidak hilang.\n\n" +
            "Coba tanyakan atau ketik transaksi seperti:\n" +
            "• *\"Audit keuangan saya bulan & tahun ini\"*\n" +
            "• *\"Beli bensin 50 ribu pakai cash\"* / *\"Beli jajan 20rb\"*\n" +
            "• *\"Terima gaji PKL 1.5 juta ke BCA\"*\n" +
            "• *\"Berapa total saldo & tabungan saya dalam USD hari ini?\"*"

    private val _selectedPeriod = MutableStateFlow(DashboardPeriod.MONTH)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    private val _selectedAccountIdFilter = MutableStateFlow<String?>(null)
    private val _sortByAmountDesc = MutableStateFlow(false)

    private val _analyticsYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _analyticsMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    private val _analyticsSearchQuery = MutableStateFlow("")
    private val _analyticsTypeFilter = MutableStateFlow<TransactionType?>(null)
    private val _analyticsCategoryFilter = MutableStateFlow<String?>(null)

    private val _activeAiSessionId = MutableStateFlow("")
    private val _aiSessions = repository.observeAiSessions()
    private val _aiMessages = _activeAiSessionId.flatMapLatest { sid ->
        if (sid.isBlank()) {
            flowOf(
                listOf(
                    AiMessageModel(
                        id = "welcome_ai",
                        sessionId = "default_session",
                        userId = currentUserId,
                        role = "assistant",
                        content = defaultAiWelcomeMessage
                    )
                )
            )
        } else {
            repository.observeAiMessages(sid)
        }
    }
    private val _isAiLoading = MutableStateFlow(false)
    private val _pendingAiDraft = MutableStateFlow<ProposedTransactionDraft?>(null)
    private val _isSyncing = MutableStateFlow(false)
    private val _lastSyncMessage = MutableStateFlow("Data lokal & cloud aktif")
    private val _lastGeneratedPdf = MutableStateFlow<GeneratedPdfInfo?>(null)
    private val _lastGeneratedStatusImage = MutableStateFlow<GeneratedStatusImageInfo?>(null)
    private val _usdMarketState = MutableStateFlow(LiveUsdMarketState())
    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    private val _autoBackupEnabledSnapshot = MutableStateFlow(true)
    private var autoBackupManager: CloudAutoBackupManager? = null
    private val _fallbackAutoBackupStatus = MutableStateFlow(AutoBackupStatusState())
    private val _availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    private val _isCheckingUpdate = MutableStateFlow(false)
    private val _updateStatusText = MutableStateFlow("Belum diperiksa")

    private data class CoreDataBundle(
        val prefs: UserPreferencesState,
        val accounts: List<FinancialAccountModel>,
        val categories: List<TransactionCategoryModel>,
        val transactions: List<TransactionModel>,
        val budgets: List<BudgetModel>,
        val goals: List<FinancialGoalModel>,
        val contributions: List<GoalContributionModel>,
        val recurring: List<RecurringTransactionModel>,
        val invoices: List<InvoiceReferenceModel>
    )

    private val coreDataFlow = combine(
        prefsRepo.preferencesFlow,
        repository.observeAccounts(),
        repository.observeCategories(),
        repository.observeTransactions(),
        repository.observeBudgets()
    ) { prefs, accounts, categories, transactions, budgets ->
         CoreDataBundle(prefs, accounts, categories, transactions, budgets, emptyList(), emptyList(), emptyList(), emptyList())
    }.combine(
        combine(
            repository.observeGoals(),
            repository.observeGoalContributions(),
            repository.observeRecurringTransactions(),
            repository.observeInvoices()
        ) { goals, contributions, recurring, invoices ->
            Quadruple(goals, contributions, recurring, invoices)
        }
    ) { base, extra ->
        base.copy(
            goals = extra.first,
            contributions = extra.second,
            recurring = extra.third,
            invoices = extra.fourth
        )
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private data class FilterBundle(
        val period: DashboardPeriod,
        val search: String,
        val typeFilter: TransactionType?,
        val accountFilter: String?,
        val sortByAmount: Boolean,
        val analyticsYear: Int,
        val analyticsMonth: Int,
        val analyticsSearch: String,
        val analyticsType: TransactionType?,
        val analyticsCategory: String?
    )

    private val analyticsFilterFlow = combine(
        _analyticsYear,
        _analyticsMonth,
        _analyticsSearchQuery,
        _analyticsTypeFilter,
        _analyticsCategoryFilter
    ) { yr, mo, q, t, cat ->
        AnalyticsFilterHolder(yr, mo, q, t, cat)
    }

    private data class AnalyticsFilterHolder(
        val year: Int,
        val month: Int,
        val search: String,
        val type: TransactionType?,
        val categoryId: String?
    )

    private val filterFlow = combine(
        _selectedPeriod,
        _searchQuery,
        _selectedTypeFilter,
        _selectedAccountIdFilter,
        _sortByAmountDesc
    ) { period, search, typeFilter, accountFilter, sortByAmount ->
        FilterBundle(
            period, search, typeFilter, accountFilter, sortByAmount,
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH),
            "", null, null
        )
    }.combine(analyticsFilterFlow) { base, af ->
        base.copy(
            analyticsYear = af.year,
            analyticsMonth = af.month,
            analyticsSearch = af.search,
            analyticsType = af.type,
            analyticsCategory = af.categoryId
        )
    }

    private data class AuxUiBundle(
        val aiSessions: List<AiSessionModel>,
        val activeAiSessionId: String,
        val aiMessages: List<AiMessageModel>,
        val isAiLoading: Boolean,
        val pendingDraft: ProposedTransactionDraft?,
        val isSyncing: Boolean,
        val lastSyncMsg: String,
        val autoBackupStatus: AutoBackupStatusState,
        val lastPdf: GeneratedPdfInfo?,
        val lastStatusImg: GeneratedStatusImageInfo?,
        val usdMarket: LiveUsdMarketState,
        val bannerMsg: String?,
        val availableUpdate: AppUpdateInfo?,
        val isCheckingUpdate: Boolean,
        val updateStatusText: String
    )

    private val aiAuxFlow = combine(
        _aiSessions,
        _activeAiSessionId,
        _aiMessages,
        _isAiLoading,
        _pendingAiDraft
    ) { sessions, activeId, msgs, aiLoad, draft ->
        AuxUiBundle(
            aiSessions = sessions,
            activeAiSessionId = activeId,
            aiMessages = msgs,
            isAiLoading = aiLoad,
            pendingDraft = draft,
            isSyncing = false,
            lastSyncMsg = "",
            autoBackupStatus = AutoBackupStatusState(),
            lastPdf = null,
            lastStatusImg = null,
            usdMarket = LiveUsdMarketState(),
            bannerMsg = null,
            availableUpdate = null,
            isCheckingUpdate = false,
            updateStatusText = ""
        )
    }

    private val syncAndStatusFlow = combine(
        _isSyncing,
        _lastSyncMessage,
        _fallbackAutoBackupStatus
    ) { syncing, syncMsg, backupStat ->
        Triple(syncing, syncMsg, backupStat)
    }

    private val updateAuxFlow = combine(
        _availableUpdate,
        _isCheckingUpdate,
        _updateStatusText
    ) { updateInfo, checking, statusText ->
        Triple(updateInfo, checking, statusText)
    }

    private val auxFlow = combine(
        aiAuxFlow,
        syncAndStatusFlow,
        combine(
            _lastGeneratedPdf,
            _lastGeneratedStatusImage,
            _usdMarketState,
            _statusBannerMessage
        ) { pdf, img, usd, banner ->
            Quadruple(pdf, img, usd, banner)
        },
        updateAuxFlow
    ) { aiBase, syncTriple, extra, updateTriple ->
        aiBase.copy(
            isSyncing = syncTriple.first,
            lastSyncMsg = syncTriple.second,
            autoBackupStatus = syncTriple.third,
            lastPdf = extra.first,
            lastStatusImg = extra.second,
            usdMarket = extra.third,
            bannerMsg = extra.fourth,
            availableUpdate = updateTriple.first,
            isCheckingUpdate = updateTriple.second,
            updateStatusText = updateTriple.third
        )
    }

    val uiState: StateFlow<XynoosUiState> = combine(
        coreDataFlow,
        filterFlow,
        auxFlow
    ) { core, filter, aux ->
        val periodTxs = FinancialCalculator.filterTransactionsByPeriod(core.transactions, filter.period)
        val overview = FinancialCalculator.calculateOverview(
            accounts = core.accounts,
            allTransactions = core.transactions,
            periodTransactions = periodTxs,
            budgets = core.budgets,
            goals = core.goals
        )
        val catBreakdown = FinancialCalculator.calculateCategoryBreakdown(periodTxs, core.categories)
        val budgetSummaries = core.budgets.map { b ->
            FinancialCalculator.calculateBudgetProgress(b, periodTxs)
        }
        val businessPl = FinancialCalculator.calculateBusinessProfitLoss(periodTxs, core.invoices)

        val searched = core.transactions.filter { tx ->
            val matchesQuery = filter.search.isBlank() ||
                tx.title.contains(filter.search, ignoreCase = true) ||
                tx.categoryName.contains(filter.search, ignoreCase = true) ||
                tx.accountName.contains(filter.search, ignoreCase = true) ||
                tx.notes.contains(filter.search, ignoreCase = true) ||
                tx.merchantName.contains(filter.search, ignoreCase = true)
            val matchesType = filter.typeFilter == null || tx.type == filter.typeFilter
            val matchesAccount = filter.accountFilter == null || tx.accountId == filter.accountFilter || tx.toAccountId == filter.accountFilter
            matchesQuery && matchesType && matchesAccount
        }

        val sortedFiltered = if (filter.sortByAmount) {
            searched.sortedByDescending { it.amountIdr }
        } else {
            searched.sortedByDescending { it.transactionDateMillis }
        }

        val availableYears = FinancialCalculator.getAvailableYears(core.transactions)
        val monthlySummary = FinancialCalculator.calculateMonthlyLedgerSummary(
            year = filter.analyticsYear,
            monthIndex = filter.analyticsMonth,
            allTransactions = core.transactions,
            categories = core.categories,
            budgets = core.budgets
        )
        val annualSummary = FinancialCalculator.calculateAnnualLedgerSummary(
            year = filter.analyticsYear,
            allTransactions = core.transactions,
            categories = core.categories,
            budgets = core.budgets
        )
        val filteredMonthlyTxs = monthlySummary.transactions.filter { tx ->
            val matchesQuery = filter.analyticsSearch.isBlank() ||
                tx.title.contains(filter.analyticsSearch, ignoreCase = true) ||
                tx.categoryName.contains(filter.analyticsSearch, ignoreCase = true) ||
                tx.accountName.contains(filter.analyticsSearch, ignoreCase = true) ||
                tx.notes.contains(filter.analyticsSearch, ignoreCase = true) ||
                tx.merchantName.contains(filter.analyticsSearch, ignoreCase = true)
            val matchesType = filter.analyticsType == null || tx.type == filter.analyticsType
            val matchesCat = filter.analyticsCategory == null || tx.categoryId == filter.analyticsCategory
            matchesQuery && matchesType && matchesCat
        }

        XynoosUiState(
            userId = currentUserId,
            prefs = core.prefs,
            selectedPeriod = filter.period,
            accounts = core.accounts,
            categories = core.categories,
            allTransactions = core.transactions,
            periodTransactions = periodTxs,
            filteredTransactions = sortedFiltered,
            budgets = core.budgets,
            budgetSummaries = budgetSummaries,
            goals = core.goals,
            goalContributions = core.contributions,
            recurringTransactions = core.recurring,
            invoices = core.invoices,
            overview = overview,
            categoryBreakdown = catBreakdown,
            businessProfitLoss = businessPl,
            analyticsYear = filter.analyticsYear,
            analyticsMonth = filter.analyticsMonth,
            availableYears = availableYears,
            analyticsSearchQuery = filter.analyticsSearch,
            analyticsTypeFilter = filter.analyticsType,
            analyticsCategoryFilter = filter.analyticsCategory,
            monthlyLedgerSummary = monthlySummary,
            annualLedgerSummary = annualSummary,
            filteredMonthlyTransactions = filteredMonthlyTxs,
            usdMarket = aux.usdMarket,
            searchQuery = filter.search,
            selectedTypeFilter = filter.typeFilter,
            selectedAccountIdFilter = filter.accountFilter,
            sortByAmountDesc = filter.sortByAmount,
            aiSessions = aux.aiSessions,
            activeAiSessionId = aux.activeAiSessionId,
            aiMessages = aux.aiMessages,
            isAiLoading = aux.isAiLoading,
            pendingAiDraft = aux.pendingDraft,
            isSyncing = aux.isSyncing,
            lastSyncMessage = aux.lastSyncMsg,
            autoBackupStatus = aux.autoBackupStatus,
            lastGeneratedPdf = aux.lastPdf,
            lastGeneratedStatusImage = aux.lastStatusImg,
            statusBannerMessage = aux.bannerMsg,
            availableUpdate = aux.availableUpdate,
            isCheckingUpdate = aux.isCheckingUpdate,
            updateStatusText = aux.updateStatusText,
            effectiveGithubRepoSlug = GitHubUpdateChecker.resolveEffectiveRepoSlug(
                userConfiguredSlug = core.prefs.githubUpdateRepo,
                buildConfigSlug = BuildConfig.GITHUB_REPO
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = XynoosUiState(userId = currentUserId)
    )

    init {
        viewModelScope.launch(safeCoroutineHandler) {
            try {
                repository.ensureDefaultCategoriesSeeded()
                val defaultSessionId = repository.ensureDefaultAiSession(defaultAiWelcomeMessage)
                if (_activeAiSessionId.value.isBlank()) {
                    _activeAiSessionId.value = defaultSessionId
                }
                synchronizeNow(silent = true)
                checkForAppUpdate(manual = false)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
            }
        }
        viewModelScope.launch(safeCoroutineHandler) {
            try {
                prefsRepo.preferencesFlow.collect { prefs ->
                    _autoBackupEnabledSnapshot.value = prefs.autoBackupEvery3MinEnabled
                    applicationContext?.let { ctx ->
                        runCatching {
                            ReminderNotificationHelper.scheduleDailyReminder(
                                context = ctx,
                                enabled = prefs.remindersEnabled,
                                hour = prefs.reminderHour,
                                minute = prefs.reminderMinute
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
            }
        }
        applicationContext?.let { ctx ->
            val manager = CloudAutoBackupManager(
                context = ctx,
                repository = repository,
                prefsRepo = prefsRepo,
                isAutoBackupEnabledProvider = { _autoBackupEnabledSnapshot.value },
                onSyncCompleted = { count, timeStr ->
                    if (count > 0) {
                        _lastSyncMessage.value = "Auto-Backup ($count item) pukul $timeStr"
                    }
                }
            )
            autoBackupManager = manager
            runCatching { manager.start() }
            viewModelScope.launch(safeCoroutineHandler) {
                try {
                    manager.statusState.collect { st ->
                        _fallbackAutoBackupStatus.value = st
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Throwable) {
                }
            }
        }
        startLiveUsdMinuteTicker()
    }

    override fun onCleared() {
        runCatching { autoBackupManager?.stop() }
        super.onCleared()
    }

    private fun startLiveUsdMinuteTicker() {
        viewModelScope.launch(safeCoroutineHandler) {
            // Initial fetch
            try {
                val initial = usdRateRepository.fetchLatestUsdMarketState(_usdMarketState.value)
                _usdMarketState.value = initial
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
            }
            var secondsLeft = 60
            while (isActive) {
                delay(1_000L)
                secondsLeft -= 1
                if (secondsLeft <= 0) {
                    try {
                        val refreshed = usdRateRepository.fetchLatestUsdMarketState(_usdMarketState.value)
                        _usdMarketState.value = refreshed
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Throwable) {
                    }
                    secondsLeft = 60
                } else {
                    _usdMarketState.value = _usdMarketState.value.copy(
                        nextUpdateSecondsRemaining = secondsLeft
                    )
                }
            }
        }
    }

    fun refreshUsdMarketNow() {
        viewModelScope.launch {
            val refreshed = usdRateRepository.fetchLatestUsdMarketState(_usdMarketState.value)
            _usdMarketState.value = refreshed
            _statusBannerMessage.value = "Kurs Live USD/IDR diperbarui: ${com.example.domain.IdrFormatter.format(refreshed.currentRateIdr.toLong())} / $1 USD"
        }
    }

    fun dismissBannerMessage() {
        _statusBannerMessage.value = null
    }

    fun setPeriod(period: DashboardPeriod) {
        _selectedPeriod.value = period
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: TransactionType?) {
        _selectedTypeFilter.value = type
    }

    fun setAccountFilter(accountId: String?) {
        _selectedAccountIdFilter.value = accountId
    }

    fun toggleSortOrder() {
        _sortByAmountDesc.value = !_sortByAmountDesc.value
    }

    fun setAnalyticsYear(year: Int) {
        _analyticsYear.value = year
    }

    fun setAnalyticsMonth(monthIndex: Int) {
        _analyticsMonth.value = monthIndex.coerceIn(0, 11)
        _analyticsCategoryFilter.value = null
    }

    fun setAnalyticsYearMonth(year: Int, monthIndex: Int) {
        _analyticsYear.value = year
        _analyticsMonth.value = monthIndex.coerceIn(0, 11)
        _analyticsCategoryFilter.value = null
    }

    fun setAnalyticsSearchQuery(query: String) {
        _analyticsSearchQuery.value = query
    }

    fun setAnalyticsTypeFilter(type: TransactionType?) {
        _analyticsTypeFilter.value = type
        _analyticsCategoryFilter.value = null
    }

    fun setAnalyticsCategoryFilter(categoryId: String?) {
        _analyticsCategoryFilter.value = categoryId
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            prefsRepo.setThemeMode(mode)
        }
    }

    fun togglePrivacyMode() {
        viewModelScope.launch {
            val current = uiState.value.prefs.isPrivacyModeEnabled
            prefsRepo.setPrivacyMode(!current)
        }
    }

    fun completeOnboardingWizard(
        displayName: String,
        currencyCode: String,
        profileType: FinancialProfileType,
        initialAccountName: String,
        initialAccountType: AccountType,
        initialOpeningBalanceIdr: Long,
        initialGoalName: String,
        initialGoalTargetIdr: Long,
        remindersEnabled: Boolean,
        themeMode: AppThemeMode
    ) {
        viewModelScope.launch {
            prefsRepo.completeOnboarding(
                displayName = displayName,
                currencyCode = currencyCode,
                profileType = profileType,
                remindersEnabled = remindersEnabled,
                themeMode = themeMode
            )
            cloudRepo?.upsertUserProfile(
                displayName = displayName.ifBlank { "Pengguna Xynoos" },
                email = userEmail,
                currencyCode = currencyCode,
                financialProfileType = profileType.name,
                onboardingCompleted = true
            )

            // Create initial account with user's explicit opening balance (or 0L if not provided)
            val safeAccountName = initialAccountName.trim().ifBlank { "Tunai Utama" }
            val newAccount = FinancialAccountModel(
                id = "acc_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                userId = currentUserId,
                name = safeAccountName,
                accountType = initialAccountType,
                openingBalanceIdr = initialOpeningBalanceIdr.coerceAtLeast(0L),
                currentBalanceIdr = initialOpeningBalanceIdr.coerceAtLeast(0L),
                colorHex = "#49E0A0"
            )
            repository.saveAccount(newAccount)

            if (initialGoalName.isNotBlank() && initialGoalTargetIdr > 0L) {
                val newGoal = FinancialGoalModel(
                    id = "goal_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                    userId = currentUserId,
                    name = initialGoalName.trim(),
                    targetAmountIdr = initialGoalTargetIdr,
                    currentSavedIdr = 0L,
                    targetDateMillis = System.currentTimeMillis() + (180L * 24L * 3600L * 1000L),
                    priority = GoalPriority.HIGH,
                    iconKey = "trending_up",
                    allocationRuleType = AllocationRuleType.PERCENT_OF_INCOME,
                    allocationValue = 10L
                )
                repository.saveGoal(newGoal)
            }
            _statusBannerMessage.value = "Profil keuangan berhasil disiapkan."
        }
    }

    fun saveAccount(
        existingId: String? = null,
        name: String,
        accountType: AccountType,
        openingBalanceIdr: Long,
        colorHex: String = "#49E0A0",
        institutionName: String = ""
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = existingId ?: "acc_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val existing = uiState.value.accounts.firstOrNull { it.id == id }
            val model = FinancialAccountModel(
                id = id,
                userId = currentUserId,
                name = name.trim(),
                accountType = accountType,
                openingBalanceIdr = openingBalanceIdr,
                currentBalanceIdr = existing?.currentBalanceIdr ?: openingBalanceIdr,
                colorHex = colorHex,
                institutionName = institutionName.trim(),
                version = (existing?.version ?: 0) + 1
            )
            repository.saveAccount(model)
            _statusBannerMessage.value = "Rekening \"${name.trim()}\" berhasil disimpan."
        }
    }

    fun saveTransaction(
        existingId: String? = null,
        type: TransactionType,
        amountIdr: Long,
        title: String,
        categoryId: String,
        categoryName: String,
        accountId: String,
        accountName: String,
        toAccountId: String? = null,
        toAccountName: String? = null,
        notes: String = "",
        merchantName: String = "",
        projectCostCenterId: String = "",
        transactionDateMillis: Long = System.currentTimeMillis()
    ) {
        if (amountIdr <= 0L) {
            _statusBannerMessage.value = "Nominal transaksi harus lebih besar dari Rp0."
            return
        }
        viewModelScope.launch {
            val id = existingId ?: "tx_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            val existing = uiState.value.allTransactions.firstOrNull { it.id == id }
            val tx = TransactionModel(
                id = id,
                userId = currentUserId,
                type = type,
                amountIdr = amountIdr,
                currencyCode = uiState.value.prefs.currencyCode,
                title = title.trim().ifBlank { categoryName },
                categoryId = categoryId,
                categoryName = categoryName,
                accountId = accountId,
                accountName = accountName,
                toAccountId = if (type == TransactionType.TRANSFER) toAccountId else null,
                toAccountName = if (type == TransactionType.TRANSFER) toAccountName else null,
                transferGroupId = if (type == TransactionType.TRANSFER) "trf_$id" else null,
                transactionDateMillis = transactionDateMillis,
                notes = notes.trim(),
                merchantName = merchantName.trim(),
                projectCostCenterId = projectCostCenterId.trim(),
                version = (existing?.version ?: 0) + 1
            )
            repository.saveTransaction(tx)
            _statusBannerMessage.value = "Transaksi \"${tx.title}\" berhasil disimpan."

            // Dispatch meaningful notifications (budget alerts & optional activity alert)
            applicationContext?.let { ctx ->
                val state = uiState.value
                if (state.prefs.activityNotificationsEnabled) {
                    ReminderNotificationHelper.notifyTransactionActivity(
                        context = ctx,
                        tx = tx,
                        isPrivacyMode = state.prefs.isPrivacyModeEnabled
                    )
                }
                if (tx.type == TransactionType.EXPENSE && state.prefs.budgetAlertsEnabled && state.budgets.isNotEmpty()) {
                    val monthTxs = FinancialCalculator.filterTransactionsByPeriod(
                        state.allTransactions.filter { it.id != tx.id } + tx,
                        DashboardPeriod.MONTH
                    )
                    val affectedBudgets = state.budgets
                        .filter { it.categoryId == "ALL" || it.categoryId == tx.categoryId }
                        .map { FinancialCalculator.calculateBudgetProgress(it, monthTxs) }
                        .filter { it.statusLevel != BudgetAlertLevel.SAFE }
                    ReminderNotificationHelper.notifyBudgetAlerts(
                        context = ctx,
                        triggeredBudgets = affectedBudgets,
                        isPrivacyMode = state.prefs.isPrivacyModeEnabled
                    )
                }
            }
        }
    }

    fun deleteTransaction(txId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(txId)
            _statusBannerMessage.value = "Transaksi dihapus & saldo telah diperbarui."
        }
    }

    fun saveCustomCategory(name: String, type: TransactionType, iconKey: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = "cat_${UUID.randomUUID().toString().replace("-", "").take(10)}"
            val cat = TransactionCategoryModel(
                id = id,
                userId = currentUserId,
                name = name.trim(),
                type = type,
                iconKey = iconKey,
                colorHex = colorHex,
                sortOrder = uiState.value.categories.size + 1
            )
            repository.saveCategory(cat)
            _statusBannerMessage.value = "Kategori \"${cat.name}\" ditambahkan."
        }
    }

    fun saveBudget(
        name: String,
        categoryId: String,
        categoryName: String,
        limitAmountIdr: Long,
        carryForward: Boolean = false,
        alertThresholdPercent: Int = 80
    ) {
        if (limitAmountIdr <= 0L) return
        viewModelScope.launch {
            val ym = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
            val id = "bdg_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val budget = BudgetModel(
                id = id,
                userId = currentUserId,
                name = name.trim().ifBlank { "Anggaran $categoryName" },
                categoryId = categoryId,
                categoryName = categoryName,
                limitAmountIdr = limitAmountIdr,
                periodYearMonth = ym,
                carryForward = carryForward,
                alertThresholdPercent = alertThresholdPercent
            )
            repository.saveBudget(budget)
            _statusBannerMessage.value = "Anggaran \"${budget.name}\" berhasil disimpan."
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            repository.deleteBudget(budgetId)
        }
    }

    fun saveGoal(
        name: String,
        targetAmountIdr: Long,
        currentSavedIdr: Long,
        priority: GoalPriority,
        allocationRuleType: AllocationRuleType,
        allocationValue: Long,
        iconKey: String = "trending_up"
    ) {
        if (name.isBlank() || targetAmountIdr <= 0L) return
        viewModelScope.launch {
            val id = "goal_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val goal = FinancialGoalModel(
                id = id,
                userId = currentUserId,
                name = name.trim(),
                targetAmountIdr = targetAmountIdr,
                currentSavedIdr = currentSavedIdr.coerceAtLeast(0L),
                targetDateMillis = System.currentTimeMillis() + (180L * 24L * 3600L * 1000L),
                priority = priority,
                iconKey = iconKey,
                allocationRuleType = allocationRuleType,
                allocationValue = allocationValue.coerceAtLeast(0L)
            )
            repository.saveGoal(goal)
            _statusBannerMessage.value = "Target tabungan \"${goal.name}\" berhasil dibuat."
        }
    }

    fun contributeToGoal(
        goalId: String,
        amountIdr: Long,
        type: ContributionType,
        notes: String
    ) {
        if (amountIdr <= 0L) return
        viewModelScope.launch {
            val updated = repository.recordGoalContribution(goalId, amountIdr, type, notes)
            _statusBannerMessage.value = "${type.labelId} berhasil dicatat."
            if (updated != null && updated.isCompleted) {
                applicationContext?.let { ctx ->
                    ReminderNotificationHelper.notifyGoalCompleted(
                        context = ctx,
                        goalName = updated.name,
                        targetAmountIdr = updated.targetAmountIdr,
                        isPrivacyMode = uiState.value.prefs.isPrivacyModeEnabled
                    )
                }
            }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            repository.deleteGoal(goalId)
        }
    }

    fun saveRecurringTransaction(
        title: String,
        type: TransactionType,
        amountIdr: Long,
        categoryId: String,
        categoryName: String,
        accountId: String,
        accountName: String,
        frequency: String
    ) {
        if (title.isBlank() || amountIdr <= 0L) return
        viewModelScope.launch {
            val item = RecurringTransactionModel(
                id = "rec_${UUID.randomUUID().toString().replace("-", "").take(10)}",
                userId = currentUserId,
                title = title.trim(),
                type = type,
                amountIdr = amountIdr,
                categoryId = categoryId,
                categoryName = categoryName,
                accountId = accountId,
                accountName = accountName,
                frequency = frequency,
                nextExecutionMillis = System.currentTimeMillis() + (30L * 24L * 3600L * 1000L)
            )
            repository.saveRecurringTransaction(item)
            _statusBannerMessage.value = "Jadwal transaksi berulang disimpan."
        }
    }

    fun deleteRecurringTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteRecurringTransaction(id)
        }
    }

    fun saveInvoice(
        invoiceNumber: String,
        counterpartyName: String,
        invoiceType: InvoiceType,
        amountIdr: Long,
        status: InvoiceStatus,
        projectCostCenter: String
    ) {
        if (counterpartyName.isBlank() || amountIdr <= 0L) return
        viewModelScope.launch {
            val inv = InvoiceReferenceModel(
                id = "inv_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                userId = currentUserId,
                invoiceNumber = invoiceNumber.trim().ifBlank { "INV-${System.currentTimeMillis() % 10000}" },
                counterpartyName = counterpartyName.trim(),
                invoiceType = invoiceType,
                amountIdr = amountIdr,
                dueDateMillis = System.currentTimeMillis() + (14L * 24L * 3600L * 1000L),
                status = status,
                projectCostCenter = projectCostCenter.trim()
            )
            repository.saveInvoice(inv)
            _statusBannerMessage.value = "Faktur ${inv.invoiceNumber} berhasil disimpan."
        }
    }

    fun markInvoicePaidAndRecordCashFlow(invoice: InvoiceReferenceModel) {
        viewModelScope.launch {
            val updated = invoice.copy(status = InvoiceStatus.PAID, version = invoice.version + 1)
            repository.saveInvoice(updated)

            val defaultAcc = uiState.value.accounts.firstOrNull()
            if (defaultAcc != null) {
                val txType = if (invoice.invoiceType == InvoiceType.RECEIVABLE) TransactionType.INCOME else TransactionType.EXPENSE
                val catId = if (txType == TransactionType.INCOME) "cat_business_rev" else "cat_biz_ops"
                val catName = if (txType == TransactionType.INCOME) "Pendapatan Bisnis" else "Operasional Bisnis"
                saveTransaction(
                    type = txType,
                    amountIdr = invoice.amountIdr,
                    title = "Pelunasan ${invoice.invoiceNumber} - ${invoice.counterpartyName}",
                    categoryId = catId,
                    categoryName = catName,
                    accountId = defaultAcc.id,
                    accountName = defaultAcc.name,
                    projectCostCenterId = invoice.projectCostCenter
                )
            }
        }
    }

    fun deleteInvoice(id: String) {
        viewModelScope.launch {
            repository.deleteInvoice(id)
        }
    }

    // AI Assistant Sessions & Multi-turn Interaction
    fun selectAiSession(sessionId: String) {
        if (sessionId.isBlank()) return
        _pendingAiDraft.value = null
        _activeAiSessionId.value = sessionId
    }

    fun createNewAiSession(title: String = "") {
        viewModelScope.launch {
            _pendingAiDraft.value = null
            val created = repository.createAiSession(title, defaultAiWelcomeMessage)
            _activeAiSessionId.value = created.id
        }
    }

    fun renameAiSession(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            repository.renameAiSession(sessionId, newTitle)
        }
    }

    fun deleteAiSession(sessionId: String) {
        viewModelScope.launch {
            _pendingAiDraft.value = null
            repository.deleteAiSession(sessionId)
            val nextId = repository.ensureDefaultAiSession(defaultAiWelcomeMessage)
            if (_activeAiSessionId.value == sessionId) {
                _activeAiSessionId.value = nextId
            }
        }
    }

    fun sendAiPrompt(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _isAiLoading.value) return

        _isAiLoading.value = true

        viewModelScope.launch {
            try {
                val sid = _activeAiSessionId.value.ifBlank {
                    repository.ensureDefaultAiSession(defaultAiWelcomeMessage).also {
                        _activeAiSessionId.value = it
                    }
                }
                val historyBefore = repository.getAiMessagesForSession(sid)
                val userMsg = AiMessageModel(
                    id = "msg_${UUID.randomUUID().toString().take(12)}",
                    sessionId = sid,
                    userId = currentUserId,
                    role = "user",
                    content = trimmed,
                    timestampMillis = System.currentTimeMillis()
                )
                repository.saveAiMessage(userMsg)

                val state = uiState.value
                val result = aiAssistant.analyzeWithGroundedGemini(
                    userQuery = trimmed,
                    overview = state.overview,
                    accounts = state.accounts,
                    categories = state.categories,
                    allTransactions = state.allTransactions,
                    budgets = state.budgetSummaries,
                    goals = state.goals,
                    usdMarket = state.usdMarket,
                    monthlySummary = state.monthlyLedgerSummary,
                    annualSummary = state.annualLedgerSummary,
                    recentHistory = historyBefore
                )
                if (result.proposedDraft != null) {
                    _pendingAiDraft.value = result.proposedDraft
                }
                val replyMsg = AiMessageModel(
                    id = "msg_${UUID.randomUUID().toString().take(12)}",
                    sessionId = sid,
                    userId = currentUserId,
                    role = "assistant",
                    content = result.explanationMarkdown,
                    timestampMillis = System.currentTimeMillis(),
                    proposedDraft = result.proposedDraft
                )
                repository.saveAiMessage(replyMsg)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun confirmAiProposedTransaction(draft: ProposedTransactionDraft) {
        viewModelScope.launch {
            val state = uiState.value
            val sid = _activeAiSessionId.value.ifBlank {
                repository.ensureDefaultAiSession(defaultAiWelcomeMessage)
            }
            val matchedAcc = state.accounts.firstOrNull {
                it.name.equals(draft.accountName, ignoreCase = true)
            } ?: state.accounts.firstOrNull()

            val accToUse = matchedAcc ?: run {
                val created = FinancialAccountModel(
                    id = "acc_cash_default",
                    userId = currentUserId,
                    name = draft.accountName.ifBlank { "Tunai (Cash)" },
                    accountType = AccountType.CASH,
                    openingBalanceIdr = 0L,
                    currentBalanceIdr = 0L,
                    colorHex = "#49E0A0"
                )
                repository.saveAccount(created)
            }

            val matchedCat = state.categories.firstOrNull {
                it.name.equals(draft.categoryName, ignoreCase = true)
            } ?: state.categories.firstOrNull { it.type == draft.type }

            saveTransaction(
                type = draft.type,
                amountIdr = draft.amountIdr,
                title = draft.title,
                categoryId = matchedCat?.id ?: "cat_other",
                categoryName = matchedCat?.name ?: draft.categoryName,
                accountId = accToUse.id,
                accountName = accToUse.name,
                notes = draft.notes,
                transactionDateMillis = draft.dateMillis
            )
            _pendingAiDraft.value = null
            repository.saveAiMessage(
                AiMessageModel(
                    id = "msg_${UUID.randomUUID().toString().take(12)}",
                    sessionId = sid,
                    userId = currentUserId,
                    role = "assistant",
                    content = "✅ Transaksi **${draft.title}** sebesar **${com.example.domain.IdrFormatter.format(draft.amountIdr)}** telah dikonfirmasi dan disimpan ke buku besar Anda.",
                    timestampMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun dismissAiProposedTransaction() {
        _pendingAiDraft.value = null
    }

    fun clearAiConversation() {
        createNewAiSession()
    }

    fun generatePdfReport(context: Context, config: PdfReportConfig, onComplete: (Result<GeneratedPdfInfo>) -> Unit = {}) {
        viewModelScope.launch {
            val state = uiState.value
            val targetTransactions = when (config.scope) {
                PdfReportScope.SELECTED_MONTH -> state.monthlyLedgerSummary.transactions
                PdfReportScope.FULL_YEAR -> FinancialCalculator.filterTransactionsByYear(state.allTransactions, state.analyticsYear)
                PdfReportScope.DASHBOARD_PERIOD -> state.periodTransactions
            }
            val effectivePeriodLabel = when (config.scope) {
                PdfReportScope.SELECTED_MONTH -> state.monthlyLedgerSummary.monthLabel
                PdfReportScope.FULL_YEAR -> "Tahun ${state.analyticsYear}"
                PdfReportScope.DASHBOARD_PERIOD -> config.periodLabel
            }
            val expenseBreakdown = when (config.scope) {
                PdfReportScope.SELECTED_MONTH -> state.monthlyLedgerSummary.expenseCategoryBreakdown
                PdfReportScope.FULL_YEAR -> state.annualLedgerSummary.expenseCategoryBreakdown
                PdfReportScope.DASHBOARD_PERIOD -> state.categoryBreakdown
            }
            val incomeBreakdown = when (config.scope) {
                PdfReportScope.SELECTED_MONTH -> state.monthlyLedgerSummary.incomeCategoryBreakdown
                PdfReportScope.FULL_YEAR -> state.annualLedgerSummary.incomeCategoryBreakdown
                PdfReportScope.DASHBOARD_PERIOD -> FinancialCalculator.calculateIncomeCategoryBreakdown(targetTransactions, state.categories)
            }
            val typeBreakdown = FinancialCalculator.calculateTypeBreakdown(targetTransactions)
            val accountActivities = FinancialCalculator.calculateAccountActivities(state.accounts, targetTransactions)
            val scopedBudgets = when (config.scope) {
                PdfReportScope.SELECTED_MONTH -> state.monthlyLedgerSummary.budgetSummaries
                PdfReportScope.FULL_YEAR -> state.budgets.map { FinancialCalculator.calculateBudgetProgress(it, targetTransactions) }
                PdfReportScope.DASHBOARD_PERIOD -> state.budgetSummaries
            }
            val scopedBusinessPl = FinancialCalculator.calculateBusinessProfitLoss(targetTransactions, state.invoices)

            val result = PdfReportGenerator.generateFinancialReportPdf(
                context = context,
                config = config.copy(periodLabel = effectivePeriodLabel),
                prefs = state.prefs,
                overview = state.overview,
                accounts = state.accounts,
                transactions = targetTransactions,
                categoryBreakdown = expenseBreakdown,
                incomeCategoryBreakdown = incomeBreakdown,
                typeBreakdown = typeBreakdown,
                accountActivities = accountActivities,
                annualSummary = state.annualLedgerSummary,
                usdRateIdr = state.usdMarket.currentRateIdr,
                budgets = scopedBudgets,
                goals = state.goals,
                businessPl = scopedBusinessPl,
                invoices = state.invoices
            )
            if (result.isSuccess) {
                _lastGeneratedPdf.value = result.getOrNull()
                _statusBannerMessage.value = "Laporan PDF Rinci (${result.getOrNull()?.pageCount} halaman) berhasil dibuat."
            } else {
                _statusBannerMessage.value = "Gagal membuat PDF: ${result.exceptionOrNull()?.localizedMessage}"
            }
            onComplete(result)
        }
    }

    fun generateStatusImage(
        context: Context,
        autoShare: Boolean = false,
        onComplete: (Result<GeneratedStatusImageInfo>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val state = uiState.value
            val result = StatusImageExporter.generateStatusImagePng(
                context = context,
                prefs = state.prefs,
                overview = state.overview,
                usdMarket = state.usdMarket,
                goals = state.goals,
                periodLabel = state.selectedPeriod.labelId,
                categoryBreakdown = state.categoryBreakdown,
                accounts = state.accounts,
                annualSummary = state.annualLedgerSummary
            )
            if (result.isSuccess) {
                val info = result.getOrNull()
                _lastGeneratedStatusImage.value = info
                _statusBannerMessage.value = "Gambar Status Keuangan Rinci (PNG) berhasil dibuat!"
                if (autoShare && info != null) {
                    val caption = WhatsAppNotifierHelper.buildOfficialWhatsAppMessage(
                        displayName = state.prefs.displayName,
                        overview = state.overview,
                        usdMarket = state.usdMarket,
                        latestTransaction = state.allTransactions.maxByOrNull { it.transactionDateMillis },
                        isPrivacyMode = state.prefs.isPrivacyModeEnabled,
                        periodLabel = state.selectedPeriod.labelId,
                        accounts = state.accounts,
                        categoryBreakdown = state.categoryBreakdown,
                        budgetSummaries = state.budgetSummaries,
                        annualSummary = state.annualLedgerSummary,
                        recentTransactions = state.periodTransactions
                    )
                    StatusImageExporter.shareStatusImage(context, info.file, caption)
                }
            } else {
                _statusBannerMessage.value = "Gagal membuat Gambar Status: ${result.exceptionOrNull()?.localizedMessage}"
            }
            onComplete(result)
        }
    }

    fun updateReportIdentity(
        orgName: String,
        orgAddress: String,
        contactInfo: String,
        footerText: String,
        signatory: String
    ) {
        viewModelScope.launch {
            prefsRepo.updateReportIdentity(orgName, orgAddress, contactInfo, footerText, signatory)
            _statusBannerMessage.value = "Identitas laporan PDF berhasil diperbarui."
        }
    }

    fun updateReminderPreferences(
        enabled: Boolean,
        hour: Int,
        minute: Int,
        quietHours: Boolean,
        budgetAlertsEnabled: Boolean = uiState.value.prefs.budgetAlertsEnabled,
        activityNotificationsEnabled: Boolean = uiState.value.prefs.activityNotificationsEnabled
    ) {
        viewModelScope.launch {
            prefsRepo.updateReminderSettings(
                enabled = enabled,
                hour = hour,
                minute = minute,
                quietHours = quietHours,
                budgetAlertsEnabled = budgetAlertsEnabled,
                activityNotificationsEnabled = activityNotificationsEnabled
            )
            applicationContext?.let { ctx ->
                ReminderNotificationHelper.scheduleDailyReminder(ctx, enabled, hour, minute)
            }
            _statusBannerMessage.value = "Pengaturan notifikasi & jadwal pengingat disimpan."
        }
    }

    fun updateAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateAutoBackupSettings(enabled)
            if (enabled) {
                autoBackupManager?.triggerImmediateIfOnline("Diaktifkan Pengguna")
            }
            _statusBannerMessage.value = if (enabled) {
                "Auto-Backup Cloud setiap 3 menit diaktifkan (hemat RAM)."
            } else {
                "Auto-Backup 3 menit dijeda. Anda tetap bisa sinkronkan manual."
            }
        }
    }

    fun updatePatternLockSettings(
        enabled: Boolean,
        patternHash: String,
        lockOnResume: Boolean = uiState.value.prefs.patternLockOnResume
    ) {
        viewModelScope.launch {
            prefsRepo.updatePatternLock(enabled, patternHash, lockOnResume)
            _statusBannerMessage.value = if (enabled) {
                "Password tipe pola keamanan berhasil diaktifkan!"
            } else {
                "Password tipe pola dinonaktifkan."
            }
        }
    }

    fun updateWhatsAppPreferences(phoneNumber: String, autoNotify: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateWhatsAppSettings(phoneNumber, autoNotify)
            _statusBannerMessage.value = "Pengaturan Notifikasi WhatsApp XynoosFinance disimpan."
        }
    }

    fun sendWhatsAppSummary(context: Context, customPhone: String? = null) {
        val state = uiState.value
        val targetPhone = (customPhone ?: state.prefs.whatsAppPhone).trim()
        val latestTx = state.allTransactions.maxByOrNull { it.transactionDateMillis }
        val message = WhatsAppNotifierHelper.buildOfficialWhatsAppMessage(
            displayName = state.prefs.displayName,
            overview = state.overview,
            usdMarket = state.usdMarket,
            latestTransaction = latestTx,
            isPrivacyMode = state.prefs.isPrivacyModeEnabled,
            periodLabel = state.selectedPeriod.labelId,
            accounts = state.accounts,
            categoryBreakdown = state.categoryBreakdown,
            budgetSummaries = state.budgetSummaries,
            annualSummary = state.annualLedgerSummary,
            recentTransactions = state.periodTransactions
        )
        val res = WhatsAppNotifierHelper.sendWhatsAppNotificationToUser(
            context = context,
            phoneNumber = targetPhone,
            message = message
        )
        if (res.isFailure) {
            _statusBannerMessage.value = "Tidak dapat membuka WhatsApp di perangkat ini."
        }
    }

    fun synchronizeNow(silent: Boolean = false) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            val res = repository.synchronizeWithCloud()
            _isSyncing.value = false
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            if (res.isSuccess) {
                _lastSyncMessage.value = "Tersinkronisasi pukul $timeStr"
                if (!silent) {
                    _statusBannerMessage.value = "Sinkronisasi Cloud Firestore berhasil ($timeStr)."
                }
            } else {
                _lastSyncMessage.value = "Mode Lokal Aktif (Offline)"
                if (!silent) {
                    _statusBannerMessage.value = "Data aman secara lokal. Sinkronisasi cloud akan dicoba kembali saat terhubung."
                }
            }
        }
    }

    fun deleteAllUserAccountData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAllUserDataLocallyAndCloud()
            onComplete()
        }
    }

    fun updateGitHubRepoSlug(repoSlug: String) {
        viewModelScope.launch {
            prefsRepo.updateGitHubRepoSlug(repoSlug)
            val cleaned = GitHubUpdateChecker.sanitizeRepoSlug(repoSlug)
            _statusBannerMessage.value = if (cleaned.contains('/')) {
                "Repositori update GitHub disimpan: $cleaned"
            } else {
                "Repositori update GitHub dikosongkan."
            }
            if (cleaned.contains('/')) {
                checkForAppUpdate(manual = true)
            }
        }
    }

    fun dismissOptionalUpdate(skipVersion: Boolean) {
        val current = _availableUpdate.value ?: return
        if (current.isMandatory) return
        viewModelScope.launch {
            if (skipVersion) {
                prefsRepo.skipUpdateTag(current.tagName)
                _statusBannerMessage.value = "Versi ${current.tagName} dilewati sementara."
            }
            _availableUpdate.value = null
        }
    }

    fun checkForAppUpdate(manual: Boolean = true) {
        if (_isCheckingUpdate.value) return
        viewModelScope.launch {
            val prefs = prefsRepo.preferencesFlow.first()
            val repoSlug = GitHubUpdateChecker.resolveEffectiveRepoSlug(
                userConfiguredSlug = prefs.githubUpdateRepo,
                buildConfigSlug = BuildConfig.GITHUB_REPO
            )
            if (repoSlug.isBlank()) {
                _updateStatusText.value = "Atur repositori GitHub (owner/repo) untuk cek otomatis"
                if (manual) {
                    _statusBannerMessage.value = "Isi nama repositori GitHub Anda (contoh: username/XynoosFinance) di Pengaturan."
                }
                return@launch
            }

            val now = System.currentTimeMillis()
            if (!manual && now - prefs.lastUpdateCheckMillis < 1_800_000L) {
                return@launch
            }

            _isCheckingUpdate.value = true
            _updateStatusText.value = "Memeriksa rilis terbaru di $repoSlug..."
            val res = GitHubUpdateChecker.fetchLatestRelease(
                repoSlug = repoSlug,
                currentVersionCode = BuildConfig.VERSION_CODE,
                currentVersionName = BuildConfig.VERSION_NAME
            )
            _isCheckingUpdate.value = false
            prefsRepo.recordUpdateCheck(now)

            when (res) {
                is GitHubUpdateCheckResult.NotConfigured -> {
                    _updateStatusText.value = "Format repositori harus owner/nama-repo"
                }
                is GitHubUpdateCheckResult.UpToDate -> {
                    _availableUpdate.value = null
                    _updateStatusText.value = "Sudah versi terbaru (v${BuildConfig.VERSION_NAME} • rilis GitHub: ${res.latestTag})"
                    if (manual) {
                        _statusBannerMessage.value = "Aplikasi sudah memakai versi terbaru (v${BuildConfig.VERSION_NAME})."
                    }
                }
                is GitHubUpdateCheckResult.UpdateAvailable -> {
                    val info = res.update
                    _updateStatusText.value = if (info.isMandatory) {
                        "Pembaruan WAJIB tersedia: ${info.tagName}"
                    } else {
                        "Versi baru tersedia: ${info.tagName}"
                    }
                    val skipped = !info.isMandatory && prefs.skippedUpdateTag == info.tagName
                    if (manual || !skipped) {
                        _availableUpdate.value = info
                    }
                }
                is GitHubUpdateCheckResult.Error -> {
                    _updateStatusText.value = res.message
                    if (manual) {
                        _statusBannerMessage.value = res.message
                    }
                }
            }
        }
    }
}
