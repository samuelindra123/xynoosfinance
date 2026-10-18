package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserPreferencesState
import com.example.data.local.XynoosDatabase
import com.example.data.remote.configureAppCheck
import com.example.data.repository.FirestoreFinanceRepository
import com.example.data.repository.OfflineFirstFinanceRepository
import com.example.domain.AppThemeMode
import com.example.domain.TransactionModel
import com.example.domain.TransactionType
import com.example.notifications.ReminderNotificationHelper
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.ui.text.style.TextOverflow
import com.example.export.StatusImageExporter
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.GoalsAndBudgetsScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.MoreSectionDestination
import com.example.ui.screens.MoreSectionScreen
import com.example.ui.screens.OnboardingWizardScreen
import com.example.ui.screens.TransactionDetailDialog
import com.example.ui.screens.TransactionFormBottomSheet
import com.example.ui.screens.TransactionsScreen
import com.example.ui.screens.UsdLiveMarketSubScreen
import com.example.ui.screens.WelcomeAuthScreen
import com.example.ui.screens.XynoosSplashScreen
import com.example.ui.theme.XynoosFinanceTheme
import com.example.ui.viewmodel.XynoosViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class PrimaryNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    HOME("home", "Beranda", Icons.Default.Dashboard, "nav_tab_home"),
    TRANSACTIONS("transactions", "Transaksi", Icons.Default.ReceiptLong, "nav_tab_transactions"),
    USD_MARKET("usd_market", "Kurs USD", Icons.AutoMirrored.Filled.ShowChart, "nav_tab_usd_market"),
    GOALS_BUDGETS("goals_budgets", "Target", Icons.Default.Savings, "nav_tab_goals"),
    AI_ASSISTANT("ai_assistant", "AI Finance", Icons.Default.AutoAwesome, "nav_tab_ai"),
    MORE("more", "Menu", Icons.Default.Menu, "nav_tab_more")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(applicationContext, intent)
        ReminderNotificationHelper.ensureChannelCreated(applicationContext)
        enableEdgeToEdge()

        setContent {
            val appContext = LocalContext.current.applicationContext
            val prefsRepo = remember { UserPreferencesRepository(appContext) }
            val prefsState by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(
                initialValue = UserPreferencesState(themeMode = AppThemeMode.DARK)
            )

            XynoosFinanceTheme(themeMode = prefsState.themeMode) {
                XynoosRootApplication(
                    prefsRepo = prefsRepo,
                    prefsState = prefsState
                )
            }
        }
    }
}

@Composable
fun XynoosRootApplication(
    prefsRepo: UserPreferencesRepository,
    prefsState: UserPreferencesState,
    forceAuthenticatedUserIdForTest: String? = null
) {
    val context = LocalContext.current
    var showSplash by rememberSaveable { mutableStateOf(forceAuthenticatedUserIdForTest == null) }
    var currentUserId by remember {
        mutableStateOf(forceAuthenticatedUserIdForTest ?: runCatching { Firebase.auth.currentUser?.uid }.getOrNull())
    }
    var currentUserEmail by remember {
        mutableStateOf(runCatching { Firebase.auth.currentUser?.email }.getOrNull())
    }
    var currentUserDisplayName by remember {
        mutableStateOf(runCatching { Firebase.auth.currentUser?.displayName }.getOrNull() ?: "")
    }

    DisposableEffect(forceAuthenticatedUserIdForTest) {
        if (forceAuthenticatedUserIdForTest != null) {
            currentUserId = forceAuthenticatedUserIdForTest
            onDispose { }
        } else {
            val authInstance = runCatching { Firebase.auth }.getOrNull()
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                currentUserId = firebaseAuth.currentUser?.uid
                currentUserEmail = firebaseAuth.currentUser?.email
                currentUserDisplayName = firebaseAuth.currentUser?.displayName ?: ""
            }
            authInstance?.addAuthStateListener(listener)
            onDispose {
                authInstance?.removeAuthStateListener(listener)
            }
        }
    }

    if (showSplash) {
        XynoosSplashScreen(onFinished = { showSplash = false })
        return
    }

    val activeUid = currentUserId
    if (activeUid.isNullOrBlank()) {
        WelcomeAuthScreen(
            onAuthSuccess = {
                currentUserId = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
                currentUserEmail = runCatching { Firebase.auth.currentUser?.email }.getOrNull()
                currentUserDisplayName = runCatching { Firebase.auth.currentUser?.displayName }.getOrNull() ?: ""
            }
        )
        return
    }

    val viewModel = remember(activeUid) {
        val db = XynoosDatabase.getInstance(context.applicationContext)
        val cloudRepo = runCatching { FirestoreFinanceRepository(context.applicationContext) }.getOrNull()
        val offlineFirstRepo = OfflineFirstFinanceRepository(
            dao = db.financeDao(),
            cloudRepo = cloudRepo,
            userId = activeUid
        )
        XynoosViewModel(
            repository = offlineFirstRepo,
            cloudRepo = cloudRepo,
            prefsRepo = prefsRepo,
            currentUserId = activeUid,
            userEmail = currentUserEmail
        )
    }

    if (!prefsState.onboardingCompleted) {
        OnboardingWizardScreen(
            initialDisplayName = currentUserDisplayName.ifBlank { prefsState.displayName },
            currentThemeMode = prefsState.themeMode,
            onThemeChanged = { viewModel.setThemeMode(it) },
            onCompleteOnboarding = { name, currency, profileType, accName, accType, openingBal, goalName, goalTarget, reminders, theme ->
                viewModel.completeOnboardingWizard(
                    displayName = name,
                    currencyCode = currency,
                    profileType = profileType,
                    initialAccountName = accName,
                    initialAccountType = accType,
                    initialOpeningBalanceIdr = openingBal,
                    initialGoalName = goalName,
                    initialGoalTargetIdr = goalTarget,
                    remindersEnabled = reminders,
                    themeMode = theme
                )
            }
        )
    } else {
        XynoosAuthenticatedWorkspace(
            viewModel = viewModel,
            onSignOut = {
                CoroutineScope(Dispatchers.Main).launch {
                    runCatching {
                        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
                    }
                    runCatching { Firebase.auth.signOut() }
                    currentUserId = null
                }
            }
        )
    }
}

@Composable
fun XynoosAuthenticatedWorkspace(
    viewModel: XynoosViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable { mutableStateOf(PrimaryNavDestination.HOME) }
    var showTransactionSheet by rememberSaveable { mutableStateOf(false) }
    var initialTxSheetType by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var editingTransaction by remember { mutableStateOf<TransactionModel?>(null) }
    var inspectingTransaction by remember { mutableStateOf<TransactionModel?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusBannerMessage) {
        val msg = uiState.statusBannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissBannerMessage()
        }
    }

    if (currentTab != PrimaryNavDestination.HOME) {
        BackHandler {
            currentTab = PrimaryNavDestination.HOME
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 700.dp
        val density = LocalDensity.current
        val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                if (!isKeyboardOpen && (currentTab == PrimaryNavDestination.HOME || currentTab == PrimaryNavDestination.TRANSACTIONS)) {
                    FloatingActionButton(
                        onClick = {
                            editingTransaction = null
                            initialTxSheetType = TransactionType.EXPENSE
                            showTransactionSheet = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.testTag("fab_quick_add_transaction")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Transaksi Cepat")
                    }
                }
            },
            bottomBar = {
                if (!isExpandedScreen && !isKeyboardOpen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().testTag("bottom_navigation_bar")
                    ) {
                        PrimaryNavDestination.entries.forEach { dest ->
                            NavigationBarItem(
                                selected = currentTab == dest,
                                onClick = { currentTab = dest },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding()
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("navigation_rail")
                    ) {
                        PrimaryNavDestination.entries.forEach { dest ->
                            NavigationRailItem(
                                selected = currentTab == dest,
                                onClick = { currentTab = dest },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (currentTab) {
                        PrimaryNavDestination.HOME -> {
                            HomeDashboardScreen(
                                uiState = uiState,
                                onSelectPeriod = { viewModel.setPeriod(it) },
                                onTogglePrivacy = { viewModel.togglePrivacyMode() },
                                onOpenAddTransaction = { txType ->
                                    editingTransaction = null
                                    initialTxSheetType = txType
                                    showTransactionSheet = true
                                },
                                onOpenAiAssistant = { currentTab = PrimaryNavDestination.AI_ASSISTANT },
                                onSelectTransaction = { inspectingTransaction = it },
                                onSyncClicked = { viewModel.synchronizeNow(silent = false) },
                                onOpenUsdMarketTerminal = { currentTab = PrimaryNavDestination.USD_MARKET },
                                onQuickExportStatusImage = {
                                    viewModel.generateStatusImage(context, autoShare = true)
                                },
                                onSendWhatsAppQuick = {
                                    viewModel.sendWhatsAppSummary(context)
                                }
                            )
                        }

                        PrimaryNavDestination.TRANSACTIONS -> {
                            TransactionsScreen(
                                uiState = uiState,
                                onSearchChange = { viewModel.setSearchQuery(it) },
                                onTypeFilterChange = { viewModel.setTypeFilter(it) },
                                onAccountFilterChange = { viewModel.setAccountFilter(it) },
                                onToggleSort = { viewModel.toggleSortOrder() },
                                onAddTransactionClick = {
                                    editingTransaction = null
                                    initialTxSheetType = TransactionType.EXPENSE
                                    showTransactionSheet = true
                                },
                                onSelectTransaction = { inspectingTransaction = it }
                            )
                        }

                        PrimaryNavDestination.USD_MARKET -> {
                            UsdLiveMarketSubScreen(
                                uiState = uiState,
                                onBack = { currentTab = PrimaryNavDestination.HOME },
                                onRefreshNow = { viewModel.refreshUsdMarketNow() }
                            )
                        }

                        PrimaryNavDestination.GOALS_BUDGETS -> {
                            GoalsAndBudgetsScreen(
                                uiState = uiState,
                                onCreateGoal = { name, target, saved, priority, ruleType, ruleVal ->
                                    viewModel.saveGoal(name, target, saved, priority, ruleType, ruleVal)
                                },
                                onContributeGoal = { goalId, amt, type, notes ->
                                    viewModel.contributeToGoal(goalId, amt, type, notes)
                                },
                                onDeleteGoal = { viewModel.deleteGoal(it) },
                                onCreateBudget = { name, catId, catName, limit ->
                                    viewModel.saveBudget(name, catId, catName, limit)
                                },
                                onDeleteBudget = { viewModel.deleteBudget(it) }
                            )
                        }

                        PrimaryNavDestination.AI_ASSISTANT -> {
                            AiAssistantScreen(
                                uiState = uiState,
                                onSendPrompt = { viewModel.sendAiPrompt(it) },
                                onConfirmDraft = { viewModel.confirmAiProposedTransaction(it) },
                                onDismissDraft = { viewModel.dismissAiProposedTransaction() },
                                onClearHistory = { viewModel.clearAiConversation() }
                            )
                        }

                        PrimaryNavDestination.MORE -> {
                            MoreSectionScreen(
                                uiState = uiState,
                                onSaveAccount = { existingId, name, type, openingBal, color, inst ->
                                    viewModel.saveAccount(existingId, name, type, openingBal, color, inst)
                                },
                                onSaveInvoice = { num, counterparty, type, amt, status, proj ->
                                    viewModel.saveInvoice(num, counterparty, type, amt, status, proj)
                                },
                                onMarkInvoicePaid = { viewModel.markInvoicePaidAndRecordCashFlow(it) },
                                onDeleteInvoice = { viewModel.deleteInvoice(it) },
                                onSaveCategory = { name, type, icon, color ->
                                    viewModel.saveCustomCategory(name, type, icon, color)
                                },
                                onSaveRecurring = { title, type, amt, catId, catName, accId, accName, freq ->
                                    viewModel.saveRecurringTransaction(title, type, amt, catId, catName, accId, accName, freq)
                                },
                                onDeleteRecurring = { viewModel.deleteRecurringTransaction(it) },
                                onGeneratePdf = { config ->
                                    viewModel.generatePdfReport(context, config)
                                },
                                onGenerateStatusImage = { shareAfter ->
                                    viewModel.generateStatusImage(context, autoShare = shareAfter)
                                },
                                onRefreshUsdMarket = { viewModel.refreshUsdMarketNow() },
                                onUpdateWhatsAppSettings = { phone, autoNotify ->
                                    viewModel.updateWhatsAppPreferences(phone, autoNotify)
                                },
                                onSendWhatsAppNow = { overridePhone ->
                                    viewModel.sendWhatsAppSummary(context, overridePhone)
                                },
                                onUpdateReportIdentity = { org, addr, contact, footer, sig ->
                                    viewModel.updateReportIdentity(org, addr, contact, footer, sig)
                                },
                                onSetThemeMode = { viewModel.setThemeMode(it) },
                                onTogglePrivacyMode = { viewModel.togglePrivacyMode() },
                                onUpdateReminders = { enabled, hr, min, quiet ->
                                    viewModel.updateReminderPreferences(enabled, hr, min, quiet)
                                },
                                onSyncCloudNow = { viewModel.synchronizeNow(silent = false) },
                                onSignOut = onSignOut,
                                onDeleteAccountAndData = {
                                    viewModel.deleteAllUserAccountData {
                                        onSignOut()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTransactionSheet) {
        TransactionFormBottomSheet(
            initialType = initialTxSheetType,
            editingTransaction = editingTransaction,
            accounts = uiState.accounts,
            categories = uiState.categories,
            onDismiss = {
                showTransactionSheet = false
                editingTransaction = null
            },
            onSave = { existingId, type, amountIdr, title, catId, catName, accId, accName, toAccId, toAccName, notes, merchant, project ->
                viewModel.saveTransaction(
                    existingId = existingId,
                    type = type,
                    amountIdr = amountIdr,
                    title = title,
                    categoryId = catId,
                    categoryName = catName,
                    accountId = accId,
                    accountName = accName,
                    toAccountId = toAccId,
                    toAccountName = toAccName,
                    notes = notes,
                    merchantName = merchant,
                    projectCostCenterId = project
                )
                showTransactionSheet = false
                editingTransaction = null
            }
        )
    }

    val currentInspecting = inspectingTransaction
    if (currentInspecting != null) {
        TransactionDetailDialog(
            transaction = currentInspecting,
            isPrivacyHidden = uiState.prefs.isPrivacyModeEnabled,
            onDismiss = { inspectingTransaction = null },
            onEdit = { tx ->
                inspectingTransaction = null
                editingTransaction = tx
                initialTxSheetType = tx.type
                showTransactionSheet = true
            },
            onDelete = { txId ->
                inspectingTransaction = null
                viewModel.deleteTransaction(txId)
            }
        )
    }
}
