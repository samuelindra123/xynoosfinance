package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.AllocationRuleType
import com.example.domain.BudgetAlertLevel
import com.example.domain.BudgetProgressSummary
import com.example.domain.ContributionType
import com.example.domain.FinancialCalculator
import com.example.domain.FinancialGoalModel
import com.example.domain.GoalPriority
import com.example.domain.IdrFormatter
import com.example.domain.ProposedTransactionDraft
import com.example.domain.TransactionType
import com.example.ui.components.XynoosCurrencyNominalInput
import com.example.ui.components.XynoosEmptyStateCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RefinedRed
import com.example.ui.theme.SapphireSecondary
import com.example.ui.theme.TabularAmountRow
import com.example.ui.theme.TabularBalanceMedium
import com.example.ui.theme.WarmAmber
import com.example.ui.viewmodel.XynoosUiState

@Composable
fun GoalsAndBudgetsScreen(
    uiState: XynoosUiState,
    onCreateGoal: (
        name: String,
        targetAmountIdr: Long,
        currentSavedIdr: Long,
        priority: GoalPriority,
        allocationRuleType: AllocationRuleType,
        allocationValue: Long
    ) -> Unit,
    onContributeGoal: (goalId: String, amountIdr: Long, type: ContributionType, notes: String) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onCreateBudget: (name: String, categoryId: String, categoryName: String, limitAmountIdr: Long) -> Unit,
    onDeleteBudget: (String) -> Unit
) {
    var selectedSubTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Goals, 1 = Budgets
    var showCreateGoalDialog by remember { mutableStateOf(false) }
    var showCreateBudgetDialog by remember { mutableStateOf(false) }
    var activeContributionGoal by remember { mutableStateOf<FinancialGoalModel?>(null) }
    val isPrivacy = uiState.prefs.isPrivacyModeEnabled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("goals_budgets_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedSubTab == 0) "Target Tabungan" else "Anggaran Cerdas",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = {
                    if (selectedSubTab == 0) showCreateGoalDialog = true else showCreateBudgetDialog = true
                },
                modifier = Modifier.testTag("create_goal_or_budget_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (selectedSubTab == 0) "Target Baru" else "Anggaran Baru")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                label = { Text("Target Tabungan (${uiState.goals.size})") },
                modifier = Modifier.testTag("tab_goals_chip")
            )
            FilterChip(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                label = { Text("Anggaran Bulanan (${uiState.budgets.size})") },
                modifier = Modifier.testTag("tab_budgets_chip")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedSubTab == 0) {
            // Goals View
            if (uiState.goals.isEmpty()) {
                XynoosEmptyStateCard(
                    title = "Belum Ada Target Tabungan",
                    subtitle = "Tetapkan target untuk laptop, dana darurat, pendidikan, atau modal usaha dengan aturan alokasi eksplisit.",
                    actionLabel = "Buat Target Tabungan",
                    onActionClick = { showCreateGoalDialog = true }
                )
            } else {
                val sampleIncome = if (uiState.overview.periodIncomeIdr > 0L) uiState.overview.periodIncomeIdr else 10_000_000L
                val allocationPreview = remember(uiState.goals, sampleIncome) {
                    FinancialCalculator.previewGoalAllocationsForIncome(sampleIncome, uiState.goals)
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "🔗 Terkoneksi Otomatis ke Total Saldo Kas & Tabungan",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val connectedCash = uiState.overview.availableCashIdr.coerceAtLeast(0L)
                                val connectedUsd = IdrFormatter.formatUsdFromIdr(
                                    connectedCash,
                                    uiState.usdMarket.currentRateIdr,
                                    isPrivacy
                                )
                                Text(
                                    text = "Saldo Aktif Terkoneksi: ${IdrFormatter.format(connectedCash, isPrivacy)} (≈ $connectedUsd) • Progres target otomatis mengikuti total saldo Anda tanpa perlu input manual.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    items(uiState.goals, key = { it.id }) { goal ->
                        val effectiveSavedIdr = FinancialCalculator.calculateEffectiveGoalSavedIdr(
                            goal = goal,
                            availableCashIdr = uiState.overview.availableCashIdr,
                            totalAssetsIdr = uiState.overview.totalAssetsIdr
                        )
                        val isGoalReached = effectiveSavedIdr >= goal.targetAmountIdr && goal.targetAmountIdr > 0L
                        val progressFraction = if (goal.targetAmountIdr > 0L) {
                            (effectiveSavedIdr.toFloat() / goal.targetAmountIdr.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        val pct = (progressFraction * 100f).toInt()
                        val monthsRemaining = FinancialCalculator.estimateMonthsToReachGoal(
                            goal.targetAmountIdr,
                            effectiveSavedIdr,
                            1_500_000L
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("goal_card_${goal.id}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(goal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${goal.priority.labelId} • Terkoneksi Total Saldo Otomatis",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SapphireSecondary
                                        )
                                    }
                                    IconButton(onClick = { onDeleteGoal(goal.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus Target", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column {
                                        Text(
                                            text = IdrFormatter.format(effectiveSavedIdr, isPrivacyHidden = isPrivacy),
                                            style = TabularBalanceMedium,
                                            color = EmeraldPrimary
                                        )
                                        Text(
                                            text = "≈ ${IdrFormatter.formatUsdFromIdr(effectiveSavedIdr, uiState.usdMarket.currentRateIdr, isPrivacy)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SapphireSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Target: ${IdrFormatter.format(goal.targetAmountIdr, isPrivacyHidden = isPrivacy)} ($pct%)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "≈ ${IdrFormatter.formatUsdFromIdr(goal.targetAmountIdr, uiState.usdMarket.currentRateIdr, isPrivacy)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progressFraction },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                    color = if (isGoalReached) EmeraldPrimary else SapphireSecondary
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isGoalReached) "🎉 Target Tercapai dari Total Saldo!" else "Proyeksi @Rp1,5Jt/bln: ~${monthsRemaining ?: 0} bln",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isGoalReached) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isGoalReached) FontWeight.Bold else FontWeight.Normal
                                    )
                                    OutlinedButton(
                                        onClick = { activeContributionGoal = goal },
                                        modifier = Modifier.testTag("contribute_goal_${goal.id}")
                                    ) {
                                        Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Alokasi Khusus")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Smart Budgets View
            if (uiState.budgetSummaries.isEmpty()) {
                XynoosEmptyStateCard(
                    title = "Belum Ada Anggaran Aktif",
                    subtitle = "Buat batas anggaran bulanan keseluruhan atau per kategori dengan peringatan ambang batas 70%, 90%, dan 100%.",
                    actionLabel = "Buat Anggaran",
                    onActionClick = { showCreateBudgetDialog = true }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.budgetSummaries, key = { it.budget.id }) { summary ->
                        BudgetSummaryCard(
                            summary = summary,
                            isPrivacyHidden = isPrivacy,
                            onDelete = { onDeleteBudget(summary.budget.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateGoalDialog) {
        CreateGoalDialog(
            onDismiss = { showCreateGoalDialog = false },
            onSave = { name, target, saved, priority, ruleType, ruleVal ->
                onCreateGoal(name, target, saved, priority, ruleType, ruleVal)
                showCreateGoalDialog = false
            }
        )
    }

    if (activeContributionGoal != null) {
        val g = activeContributionGoal!!
        GoalContributionDialog(
            goal = g,
            onDismiss = { activeContributionGoal = null },
            onConfirm = { amount, type, notes ->
                onContributeGoal(g.id, amount, type, notes)
                activeContributionGoal = null
            }
        )
    }

    if (showCreateBudgetDialog) {
        CreateBudgetDialog(
            categories = uiState.categories,
            onDismiss = { showCreateBudgetDialog = false },
            onSave = { name, catId, catName, limit ->
                onCreateBudget(name, catId, catName, limit)
                showCreateBudgetDialog = false
            }
        )
    }
}

@Composable
private fun BudgetSummaryCard(
    summary: BudgetProgressSummary,
    isPrivacyHidden: Boolean,
    onDelete: () -> Unit
) {
    val barColor = when (summary.statusLevel) {
        BudgetAlertLevel.SAFE -> EmeraldPrimary
        BudgetAlertLevel.WARNING_70 -> SapphireSecondary
        BudgetAlertLevel.WARNING_90 -> WarmAmber
        BudgetAlertLevel.EXCEEDED -> RefinedRed
    }
    val badgeText = when (summary.statusLevel) {
        BudgetAlertLevel.SAFE -> "Aman (<70%)"
        BudgetAlertLevel.WARNING_70 -> "Perhatian (≥70%)"
        BudgetAlertLevel.WARNING_90 -> "Hampir Habis (≥90%)"
        BudgetAlertLevel.EXCEEDED -> "Melebihi Anggaran (≥100%)"
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("budget_card_${summary.budget.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(summary.budget.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${summary.budget.categoryName} • Periode ${summary.budget.periodYearMonth}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus Anggaran")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Terpakai: ${IdrFormatter.format(summary.spentAmountIdr, isPrivacyHidden)}",
                    style = TabularAmountRow,
                    color = barColor
                )
                Text(
                    text = "Batas: ${IdrFormatter.format(summary.budget.limitAmountIdr, isPrivacyHidden)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (summary.utilizationPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = barColor
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (summary.statusLevel != BudgetAlertLevel.SAFE) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = barColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = "$badgeText • ${summary.utilizationPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = barColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Sisa: ${IdrFormatter.format(summary.remainingAmountIdr, isPrivacyHidden)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CreateGoalDialog(
    onDismiss: () -> Unit,
    onSave: (String, Long, Long, GoalPriority, AllocationRuleType, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetInput by remember { mutableStateOf("25.000.000") }
    var savedInput by remember { mutableStateOf("0") }
    var priority by remember { mutableStateOf(GoalPriority.HIGH) }
    var ruleType by remember { mutableStateOf(AllocationRuleType.PERCENT_OF_INCOME) }
    var ruleValueInput by remember { mutableStateOf("10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat Target Tabungan Baru") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Target (Misal: MacBook Pro / Dana Darurat)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_name_input")
                )
                XynoosCurrencyNominalInput(
                    value = targetInput,
                    onValueChange = { targetInput = it },
                    label = "Target Nominal Tabungan (IDR)",
                    inputTestTag = "goal_target_input",
                    accentColor = SapphireSecondary,
                    compactMode = true
                )
                XynoosCurrencyNominalInput(
                    value = savedInput,
                    onValueChange = { savedInput = it },
                    label = "Dana Awal Terkumpul (IDR / Opsional)",
                    accentColor = EmeraldPrimary,
                    compactMode = true
                )
                Text("Prioritas & Aturan Alokasi", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GoalPriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.name) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AllocationRuleType.entries.forEach { r ->
                        FilterChip(
                            selected = ruleType == r,
                            onClick = { ruleType = r },
                            label = { Text(r.labelId) }
                        )
                    }
                }
                if (ruleType != AllocationRuleType.MANUAL) {
                    if (ruleType == AllocationRuleType.PERCENT_OF_INCOME) {
                        OutlinedTextField(
                            value = ruleValueInput,
                            onValueChange = { ruleValueInput = it },
                            label = { Text("Persentase Alokasi (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        XynoosCurrencyNominalInput(
                            value = ruleValueInput,
                            onValueChange = { ruleValueInput = it },
                            label = "Nominal Alokasi Tetap (IDR)",
                            accentColor = EmeraldPrimary,
                            compactMode = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = IdrFormatter.parseIdrInput(targetInput) ?: 0L
                    val saved = IdrFormatter.parseIdrInput(savedInput) ?: 0L
                    val ruleVal = IdrFormatter.parseIdrInput(ruleValueInput) ?: 0L
                    if (name.isNotBlank() && target > 0L) {
                        onSave(name, target, saved, priority, ruleType, ruleVal)
                    }
                },
                modifier = Modifier.testTag("save_goal_button")
            ) {
                Text("Simpan Target")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun GoalContributionDialog(
    goal: FinancialGoalModel,
    onDismiss: () -> Unit,
    onConfirm: (Long, ContributionType, String) -> Unit
) {
    var amountInput by remember { mutableStateOf("1.000.000") }
    var type by remember { mutableStateOf(ContributionType.DEPOSIT) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kontribusi: ${goal.name}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ContributionType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.labelId.split(" ").first()) }
                        )
                    }
                }
                XynoosCurrencyNominalInput(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = "Nominal Kontribusi (${type.labelId})",
                    inputTestTag = "contribution_amount_input",
                    accentColor = if (type == ContributionType.DEPOSIT) EmeraldPrimary else RefinedRed,
                    compactMode = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = IdrFormatter.parseIdrInput(amountInput) ?: 0L
                    if (amt > 0L) onConfirm(amt, type, notes)
                },
                modifier = Modifier.testTag("confirm_contribution_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun CreateBudgetDialog(
    categories: List<com.example.domain.TransactionCategoryModel>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long) -> Unit
) {
    val expenseCats = categories.filter { it.type == TransactionType.EXPENSE }
    var selectedCatId by remember { mutableStateOf("ALL") }
    var selectedCatName by remember { mutableStateOf("Semua Kategori") }
    var budgetName by remember { mutableStateOf("Anggaran Bulanan Utama") }
    var limitInput by remember { mutableStateOf("5.000.000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat Anggaran Cerdas") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = budgetName,
                    onValueChange = { budgetName = it },
                    label = { Text("Nama Anggaran") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("budget_name_input")
                )
                XynoosCurrencyNominalInput(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = "Batas Pengeluaran Anggaran (IDR)",
                    inputTestTag = "budget_limit_input",
                    accentColor = SapphireSecondary,
                    compactMode = true
                )
                Text("Pilih Cakupan Kategori", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedCatId == "ALL",
                        onClick = {
                            selectedCatId = "ALL"
                            selectedCatName = "Semua Kategori"
                        },
                        label = { Text("Total Bulanan") }
                    )
                    expenseCats.forEach { cat ->
                        FilterChip(
                            selected = selectedCatId == cat.id,
                            onClick = {
                                selectedCatId = cat.id
                                selectedCatName = cat.name
                                budgetName = "Anggaran ${cat.name}"
                            },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = IdrFormatter.parseIdrInput(limitInput) ?: 0L
                    if (limit > 0L) {
                        onSave(budgetName, selectedCatId, selectedCatName, limit)
                    }
                },
                modifier = Modifier.testTag("save_budget_button")
            ) {
                Text("Simpan Anggaran")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun AiAssistantScreen(
    uiState: XynoosUiState,
    onSendPrompt: (String) -> Unit,
    onConfirmDraft: (ProposedTransactionDraft) -> Unit,
    onDismissDraft: () -> Unit,
    onClearHistory: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val isKeyboardOpen = imeBottomPx > 0
    val listState = rememberLazyListState()
    var inputPrompt by rememberSaveable { mutableStateOf("") }

    val suggestedPrompts = listOf(
        "Berapa saldo saya dalam USD hari ini?",
        "Bagaimana strategi investasi & dana darurat?",
        "Bayar makan siang 35 ribu pakai cash.",
        "Berapa total pengeluaran bulan ini?",
        "Kategori apa yang paling banyak menghabiskan uang?",
        "Kalau saya menabung Rp500.000 per bulan, kapan target saya tercapai?"
    )

    // Auto-scroll to latest message whenever a message arrives, AI starts loading, or keyboard opens
    LaunchedEffect(uiState.aiMessages.size, uiState.isAiLoading, isKeyboardOpen) {
        val totalItems = uiState.aiMessages.size + (if (uiState.isAiLoading) 1 else 0)
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("ai_assistant_screen")
    ) {
        // Compact Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text("Xynoos AI Advisor & Tanya Jawab", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (!isKeyboardOpen) {
                        Text(
                            text = "Tanya Jawab Bebas • Buku Besar & Kurs USD (${IdrFormatter.format(uiState.usdMarket.currentRateIdr.toLong())})",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary
                        )
                    }
                }
            }
            IconButton(
                onClick = onClearHistory,
                modifier = Modifier.testTag("clear_ai_history_button")
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Bersihkan Riwayat Chat")
            }
        }

        // Quick Suggested Prompts (compact horizontal strip)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestedPrompts.forEachIndexed { idx, prompt ->
                SuggestionChip(
                    onClick = { onSendPrompt(prompt) },
                    label = { Text(prompt, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("ai_suggestion_$idx")
                )
            }
        }

        // Editable AI Transaction Confirmation Card (when AI proposes a transaction)
        if (uiState.pendingAiDraft != null) {
            AiTransactionConfirmationCard(
                draft = uiState.pendingAiDraft,
                onConfirm = onConfirmDraft,
                onCancel = onDismissDraft
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Conversation Messages — fills remaining space above the sticky input bar
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(uiState.aiMessages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isUser) "Anda" else "Xynoos AI (Grounded)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isUser) EmeraldPrimary else SapphireSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!isUser) {
                                    IconButton(
                                        onClick = {
                                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cb.setPrimaryClip(ClipData.newPlainText("Xynoos AI", msg.content))
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Salin Jawaban",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.content,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (uiState.isAiLoading) {
                item(key = "ai_loading_indicator") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = EmeraldPrimary)
                        Text("Menghitung fakta buku besar & menyusun analisis...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Sticky Input Bar — automatically rises right above the keyboard
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { inputPrompt = it },
                    placeholder = {
                        Text(
                            "Ketik pesan atau transaksi (misal: Bayar makan siang 35 ribu pakai cash)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_prompt_input"),
                    shape = RoundedCornerShape(16.dp),
                    minLines = 1,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputPrompt.isNotBlank() && !uiState.isAiLoading) {
                                onSendPrompt(inputPrompt)
                                inputPrompt = ""
                                keyboardController?.hide()
                            }
                        }
                    )
                )
                Button(
                    onClick = {
                        if (inputPrompt.isNotBlank()) {
                            onSendPrompt(inputPrompt)
                            inputPrompt = ""
                            keyboardController?.hide()
                        }
                    },
                    enabled = inputPrompt.isNotBlank() && !uiState.isAiLoading,
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("ai_send_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim")
                }
            }
        }
    }
}

@Composable
private fun AiTransactionConfirmationCard(
    draft: ProposedTransactionDraft,
    onConfirm: (ProposedTransactionDraft) -> Unit,
    onCancel: () -> Unit
) {
    var editableTitle by remember(draft) { mutableStateOf(draft.title) }
    var editableAmount by remember(draft) { mutableStateOf(draft.amountIdr.toString()) }
    var editableCategory by remember(draft) { mutableStateOf(draft.categoryName) }
    var editableAccount by remember(draft) { mutableStateOf(draft.accountName) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, EmeraldPrimary, RoundedCornerShape(18.dp))
            .testTag("ai_transaction_confirmation_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Konfirmasi Draf Transaksi AI (Wajib Persetujuan Anda)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "AI tidak pernah mengubah saldo tanpa persetujuan eksplisit Anda. Tinjau atau ubah detail di bawah sebelum menyimpan:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = editableTitle,
                onValueChange = { editableTitle = it },
                label = { Text("Deskripsi") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            XynoosCurrencyNominalInput(
                value = editableAmount,
                onValueChange = { editableAmount = it },
                label = "Nominal Draf Transaksi (IDR)",
                accentColor = EmeraldPrimary,
                compactMode = true,
                showQuickChips = false
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = editableCategory,
                    onValueChange = { editableCategory = it },
                    label = { Text("Kategori") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = editableAccount,
                    onValueChange = { editableAccount = it },
                    label = { Text("Akun") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("cancel_ai_draft_button")
                ) {
                    Text("Batalkan")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val amt = IdrFormatter.parseIdrInput(editableAmount) ?: draft.amountIdr
                        onConfirm(
                            draft.copy(
                                title = editableTitle.ifBlank { draft.title },
                                amountIdr = amt,
                                categoryName = editableCategory.ifBlank { draft.categoryName },
                                accountName = editableAccount.ifBlank { draft.accountName }
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("confirm_ai_draft_button")
                ) {
                    Text("Konfirmasi & Simpan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
