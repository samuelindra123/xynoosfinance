package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GroundedAiAssistant
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserPreferencesState
import com.example.data.repository.FirestoreFinanceRepository
import com.example.data.repository.OfflineFirstFinanceRepository
import com.example.domain.AccountType
import com.example.domain.AiMessageModel
import com.example.domain.AllocationRuleType
import com.example.domain.AppThemeMode
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
import com.example.domain.ProposedTransactionDraft
import com.example.domain.RecurringTransactionModel
import com.example.domain.TransactionCategoryModel
import com.example.domain.TransactionModel
import com.example.domain.TransactionType
import com.example.data.remote.LiveUsdMarketState
import com.example.data.remote.LiveUsdRateRepository
import com.example.export.GeneratedPdfInfo
import com.example.export.GeneratedStatusImageInfo
import com.example.export.PdfReportConfig
import com.example.export.PdfReportGenerator
import com.example.export.StatusImageExporter
import com.example.notifications.WhatsAppNotifierHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
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
    val usdMarket: LiveUsdMarketState = LiveUsdMarketState(),
    val searchQuery: String = "",
    val selectedTypeFilter: TransactionType? = null,
    val selectedAccountIdFilter: String? = null,
    val sortByAmountDesc: Boolean = false,
    val aiMessages: List<AiMessageModel> = emptyList(),
    val isAiLoading: Boolean = false,
    val pendingAiDraft: ProposedTransactionDraft? = null,
    val isSyncing: Boolean = false,
    val lastSyncMessage: String = "Siap sinkronisasi",
    val lastGeneratedPdf: GeneratedPdfInfo? = null,
    val lastGeneratedStatusImage: GeneratedStatusImageInfo? = null,
    val statusBannerMessage: String? = null
)

class XynoosViewModel(
    private val repository: OfflineFirstFinanceRepository,
    private val cloudRepo: FirestoreFinanceRepository?,
    private val prefsRepo: UserPreferencesRepository,
    private val currentUserId: String,
    private val userEmail: String?
) : ViewModel() {

    private val aiAssistant = GroundedAiAssistant()
    private val usdRateRepository = LiveUsdRateRepository()

    private val _selectedPeriod = MutableStateFlow(DashboardPeriod.MONTH)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    private val _selectedAccountIdFilter = MutableStateFlow<String?>(null)
    private val _sortByAmountDesc = MutableStateFlow(false)

    private val _aiMessages = MutableStateFlow<List<AiMessageModel>>(
        listOf(
            AiMessageModel(
                id = "welcome_ai",
                userId = currentUserId,
                role = "assistant",
                content = "Halo! Saya **Xynoos AI Finance Advisor & Q&A**. Anda bisa **bertanya apa saja** (strategi investasi, analisis kurs Live USD/IDR, inflasi, pajak, tips menabung) maupun mencatat transaksi otomatis:\n\n• *\"Berapa total saldo & tabungan saya dalam Dollar USD hari ini?\"*\n• *\"Bagaimana strategi investasi dan dana darurat yang aman?\"*\n• *\"Bayar makan siang 35 ribu pakai cash\"*\n• *\"Kalau saya menabung Rp500.000 per bulan, kapan target saya tercapai?\"*"
            )
        )
    )
    private val _isAiLoading = MutableStateFlow(false)
    private val _pendingAiDraft = MutableStateFlow<ProposedTransactionDraft?>(null)
    private val _isSyncing = MutableStateFlow(false)
    private val _lastSyncMessage = MutableStateFlow("Data lokal & cloud aktif")
    private val _lastGeneratedPdf = MutableStateFlow<GeneratedPdfInfo?>(null)
    private val _lastGeneratedStatusImage = MutableStateFlow<GeneratedStatusImageInfo?>(null)
    private val _usdMarketState = MutableStateFlow(LiveUsdMarketState())
    private val _statusBannerMessage = MutableStateFlow<String?>(null)

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
        val sortByAmount: Boolean
    )

    private val filterFlow = combine(
        _selectedPeriod,
        _searchQuery,
        _selectedTypeFilter,
        _selectedAccountIdFilter,
        _sortByAmountDesc
    ) { period, search, typeFilter, accountFilter, sortByAmount ->
        FilterBundle(period, search, typeFilter, accountFilter, sortByAmount)
    }

    private data class AuxUiBundle(
        val aiMessages: List<AiMessageModel>,
        val isAiLoading: Boolean,
        val pendingDraft: ProposedTransactionDraft?,
        val isSyncing: Boolean,
        val lastSyncMsg: String,
        val lastPdf: GeneratedPdfInfo?,
        val lastStatusImg: GeneratedStatusImageInfo?,
        val usdMarket: LiveUsdMarketState,
        val bannerMsg: String?
    )

    private val auxFlow = combine(
        _aiMessages,
        _isAiLoading,
        _pendingAiDraft,
        _isSyncing,
        _lastSyncMessage
    ) { msgs, aiLoad, draft, syncing, syncMsg ->
        AuxUiBundle(msgs, aiLoad, draft, syncing, syncMsg, null, null, LiveUsdMarketState(), null)
    }.combine(
        combine(
            _lastGeneratedPdf,
            _lastGeneratedStatusImage,
            _usdMarketState,
            _statusBannerMessage
        ) { pdf, img, usd, banner ->
            Quadruple(pdf, img, usd, banner)
        }
    ) { base, extra ->
        base.copy(
            lastPdf = extra.first,
            lastStatusImg = extra.second,
            usdMarket = extra.third,
            bannerMsg = extra.fourth
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
            usdMarket = aux.usdMarket,
            searchQuery = filter.search,
            selectedTypeFilter = filter.typeFilter,
            selectedAccountIdFilter = filter.accountFilter,
            sortByAmountDesc = filter.sortByAmount,
            aiMessages = aux.aiMessages,
            isAiLoading = aux.isAiLoading,
            pendingAiDraft = aux.pendingDraft,
            isSyncing = aux.isSyncing,
            lastSyncMessage = aux.lastSyncMsg,
            lastGeneratedPdf = aux.lastPdf,
            lastGeneratedStatusImage = aux.lastStatusImg,
            statusBannerMessage = aux.bannerMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = XynoosUiState(userId = currentUserId)
    )

    init {
        viewModelScope.launch {
            repository.ensureDefaultCategoriesSeeded()
            synchronizeNow(silent = true)
        }
        startLiveUsdMinuteTicker()
    }

    private fun startLiveUsdMinuteTicker() {
        viewModelScope.launch {
            // Initial fetch
            val initial = usdRateRepository.fetchLatestUsdMarketState(_usdMarketState.value)
            _usdMarketState.value = initial
            var secondsLeft = 60
            while (isActive) {
                delay(1_000L)
                secondsLeft -= 1
                if (secondsLeft <= 0) {
                    val refreshed = usdRateRepository.fetchLatestUsdMarketState(_usdMarketState.value)
                    _usdMarketState.value = refreshed
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
            repository.recordGoalContribution(goalId, amountIdr, type, notes)
            _statusBannerMessage.value = "${type.labelId} berhasil dicatat."
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

    // AI Assistant Interaction
    fun sendAiPrompt(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _isAiLoading.value) return

        val userMsg = AiMessageModel(
            id = "msg_${UUID.randomUUID().toString().take(10)}",
            userId = currentUserId,
            role = "user",
            content = trimmed
        )
        _aiMessages.value = _aiMessages.value + userMsg
        _isAiLoading.value = true

        viewModelScope.launch {
            try {
                val state = uiState.value
                val result = aiAssistant.analyzeWithGroundedGemini(
                    userQuery = trimmed,
                    overview = state.overview,
                    accounts = state.accounts,
                    categories = state.categories,
                    allTransactions = state.allTransactions,
                    budgets = state.budgetSummaries,
                    goals = state.goals,
                    usdMarket = state.usdMarket
                )
                if (result.proposedDraft != null) {
                    _pendingAiDraft.value = result.proposedDraft
                }
                val replyMsg = AiMessageModel(
                    id = "msg_${UUID.randomUUID().toString().take(10)}",
                    userId = currentUserId,
                    role = "assistant",
                    content = result.explanationMarkdown,
                    proposedDraft = result.proposedDraft
                )
                _aiMessages.value = _aiMessages.value + replyMsg
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun confirmAiProposedTransaction(draft: ProposedTransactionDraft) {
        viewModelScope.launch {
            val state = uiState.value
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
            _aiMessages.value = _aiMessages.value + AiMessageModel(
                id = "msg_${UUID.randomUUID().toString().take(10)}",
                userId = currentUserId,
                role = "assistant",
                content = "✅ Transaksi **${draft.title}** sebesar **${com.example.domain.IdrFormatter.format(draft.amountIdr)}** telah dikonfirmasi dan disimpan ke buku besar Anda."
            )
        }
    }

    fun dismissAiProposedTransaction() {
        _pendingAiDraft.value = null
    }

    fun clearAiConversation() {
        _pendingAiDraft.value = null
        _aiMessages.value = listOf(
            AiMessageModel(
                id = "welcome_reset",
                userId = currentUserId,
                role = "assistant",
                content = "Riwayat percakapan telah dihapus. Silakan ajukan pertanyaan keuangan apa saja (Q&A) atau catat transaksi baru!"
            )
        )
    }

    fun generatePdfReport(context: Context, config: PdfReportConfig, onComplete: (Result<GeneratedPdfInfo>) -> Unit = {}) {
        viewModelScope.launch {
            val state = uiState.value
            val result = PdfReportGenerator.generateFinancialReportPdf(
                context = context,
                config = config,
                prefs = state.prefs,
                overview = state.overview,
                accounts = state.accounts,
                transactions = state.periodTransactions,
                categoryBreakdown = state.categoryBreakdown,
                budgets = state.budgetSummaries,
                goals = state.goals,
                businessPl = state.businessProfitLoss,
                invoices = state.invoices
            )
            if (result.isSuccess) {
                _lastGeneratedPdf.value = result.getOrNull()
                _statusBannerMessage.value = "Laporan PDF (${result.getOrNull()?.pageCount} halaman) berhasil dibuat."
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
                periodLabel = state.selectedPeriod.labelId
            )
            if (result.isSuccess) {
                val info = result.getOrNull()
                _lastGeneratedStatusImage.value = info
                _statusBannerMessage.value = "Gambar Status Keuangan Keren (PNG) berhasil dibuat!"
                if (autoShare && info != null) {
                    val caption = WhatsAppNotifierHelper.buildOfficialWhatsAppMessage(
                        displayName = state.prefs.displayName,
                        overview = state.overview,
                        usdMarket = state.usdMarket,
                        latestTransaction = state.allTransactions.maxByOrNull { it.transactionDateMillis },
                        isPrivacyMode = state.prefs.isPrivacyModeEnabled
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

    fun updateReminderPreferences(enabled: Boolean, hour: Int, minute: Int, quietHours: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateReminderSettings(enabled, hour, minute, quietHours)
            _statusBannerMessage.value = "Pengaturan pengingat berhasil disimpan."
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
            isPrivacyMode = state.prefs.isPrivacyModeEnabled
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
}
