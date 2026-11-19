package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.AnnualLedgerSummary
import com.example.domain.BudgetAlertLevel
import com.example.domain.CategoryBreakdownItem
import com.example.domain.FinancialCalculator
import com.example.domain.IdrFormatter
import com.example.domain.MonthlyLedgerSummary
import com.example.domain.TransactionModel
import com.example.domain.TransactionType
import com.example.export.PdfReportConfig
import com.example.export.PdfReportScope
import com.example.ui.components.XynoosEmptyStateCard
import com.example.ui.components.parseColorHex
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RefinedRed
import com.example.ui.theme.SapphireSecondary
import com.example.ui.theme.TabularAmountRow
import com.example.ui.theme.TabularBalanceMedium
import com.example.ui.theme.WarmAmber
import com.example.ui.viewmodel.XynoosUiState
import java.util.Calendar
import java.util.Locale

/**
 * Full-screen dedicated page for Monthly & Annual Budget, Income, Expense & Transaction Ledger Analytics.
 */
@Composable
fun MonthlyAnnualBudgetSubScreen(
    uiState: XynoosUiState,
    onBack: () -> Unit,
    onSelectYear: (Int) -> Unit,
    onSelectMonth: (Int) -> Unit,
    onSelectYearMonth: (Int, Int) -> Unit,
    onSearchChange: (String) -> Unit,
    onTypeFilterChange: (TransactionType?) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onSelectTransaction: (TransactionModel) -> Unit,
    onOpenAddTransaction: (TransactionType) -> Unit,
    onGenerateDetailedPdf: (PdfReportConfig) -> Unit,
    onGenerateStatusImage: () -> Unit,
    onSendWhatsAppReport: () -> Unit
) {
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("monthly_annual_budget_subscreen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("monthly_annual_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
                Column {
                    Text(
                        text = "Anggaran, Pemasukan & Rekap",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Analisis Detail Bulanan (${uiState.monthlyLedgerSummary.monthLabel}) & Tahunan (${uiState.analyticsYear})",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        MonthlyAnnualBudgetPanel(
            uiState = uiState,
            onSelectYear = onSelectYear,
            onSelectMonth = onSelectMonth,
            onSelectYearMonth = onSelectYearMonth,
            onSearchChange = onSearchChange,
            onTypeFilterChange = onTypeFilterChange,
            onCategoryFilterChange = onCategoryFilterChange,
            onSelectTransaction = onSelectTransaction,
            onOpenAddTransaction = onOpenAddTransaction,
            onGenerateDetailedPdf = onGenerateDetailedPdf,
            onGenerateStatusImage = onGenerateStatusImage,
            onSendWhatsAppReport = onSendWhatsAppReport
        )
    }
}

/**
 * Reusable responsive monthly & annual budget/income/expense analytics panel.
 * Embedded both inside the dedicated page and as a tab inside GoalsAndBudgetsScreen.
 */
@Composable
fun MonthlyAnnualBudgetPanel(
    uiState: XynoosUiState,
    onSelectYear: (Int) -> Unit,
    onSelectMonth: (Int) -> Unit,
    onSelectYearMonth: (Int, Int) -> Unit,
    onSearchChange: (String) -> Unit,
    onTypeFilterChange: (TransactionType?) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onSelectTransaction: (TransactionModel) -> Unit,
    onOpenAddTransaction: (TransactionType) -> Unit,
    onGenerateDetailedPdf: (PdfReportConfig) -> Unit,
    onGenerateStatusImage: () -> Unit,
    onSendWhatsAppReport: () -> Unit
) {
    val isPrivacy = uiState.prefs.isPrivacyModeEnabled
    val monthly = uiState.monthlyLedgerSummary
    val annual = uiState.annualLedgerSummary
    val usdRate = uiState.usdMarket.currentRateIdr
    var categorySectionMode by rememberSaveable { mutableIntStateOf(0) } // 0 = Pengeluaran, 1 = Pemasukan
    var viewScopeTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Detail Bulanan, 1 = Rekap Tahunan

    val nowCal = remember { Calendar.getInstance() }
    val currentCalYear = nowCal.get(Calendar.YEAR)
    val currentCalMonth = nowCal.get(Calendar.MONTH)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("monthly_annual_panel_list"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // 1. Interactive Year & Month Selector Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Pilih Tahun & Bulan",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            TextButton(
                                onClick = { onSelectYearMonth(currentCalYear, currentCalMonth) },
                                modifier = Modifier.testTag("jump_current_month_button")
                            ) {
                                Text("Bulan Ini")
                            }
                        }

                        // Year Switcher
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onSelectYear(uiState.analyticsYear - 1) },
                                modifier = Modifier.testTag("prev_year_button")
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Tahun Sebelumnya")
                            }
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val yearsToShow = (uiState.availableYears + listOf(
                                    uiState.analyticsYear - 1,
                                    uiState.analyticsYear,
                                    uiState.analyticsYear + 1
                                )).distinct().sortedDescending()
                                yearsToShow.forEach { yr ->
                                    FilterChip(
                                        selected = uiState.analyticsYear == yr,
                                        onClick = { onSelectYear(yr) },
                                        label = { Text("Tahun $yr", fontWeight = FontWeight.SemiBold) },
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .testTag("year_chip_$yr")
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onSelectYear(uiState.analyticsYear + 1) },
                                modifier = Modifier.testTag("next_year_button")
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Tahun Berikutnya")
                            }
                        }

                        // 12-Month Selector Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            annual.monthlyBreakdown.forEach { m ->
                                val isSelected = m.monthIndex == uiState.analyticsMonth
                                val hasData = m.transactionCount > 0
                                val dotColor = when {
                                    !hasData -> Color.Transparent
                                    m.netCashFlowIdr >= 0L -> EmeraldPrimary
                                    else -> RefinedRed
                                }
                                Surface(
                                    onClick = { onSelectMonth(m.monthIndex) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    },
                                    modifier = Modifier
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .testTag("month_chip_${m.monthIndex}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Text(
                                                text = m.shortMonthLabel,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (hasData) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(dotColor)
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (hasData) "${m.transactionCount} tx" else "—",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Scope Switcher: Detail Bulanan vs Rekap Tahunan
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = viewScopeTab == 0,
                                onClick = { viewScopeTab = 0 },
                                label = { Text("Detail Bulan ${monthly.shortMonthLabel} ${monthly.year}") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("scope_monthly_chip")
                            )
                            FilterChip(
                                selected = viewScopeTab == 1,
                                onClick = { viewScopeTab = 1 },
                                label = { Text("Rekap Tahunan ${annual.year}") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("scope_annual_chip")
                            )
                        }
                    }
                }
            }

            if (viewScopeTab == 0) {
                // ================= MONTHLY VIEW =================
                // 2. Hero Monthly Summary Card (Income, Expense, Net Cash Flow, Per-Type Breakdown)
                item {
                    MonthlyHeroOverviewCard(
                        monthly = monthly,
                        usdRateIdr = usdRate,
                        isPrivacyHidden = isPrivacy,
                        isWideScreen = isWideScreen,
                        selectedTypeFilter = uiState.analyticsTypeFilter,
                        onSelectTypeFilter = { tappedType ->
                            onTypeFilterChange(if (uiState.analyticsTypeFilter == tappedType) null else tappedType)
                        }
                    )
                }

                // 3. Budget Compliance for Selected Month
                if (monthly.budgetSummaries.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Realisasi Anggaran — ${monthly.monthLabel}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                monthly.budgetSummaries.forEach { summary ->
                                    val barColor = when (summary.statusLevel) {
                                        BudgetAlertLevel.SAFE -> EmeraldPrimary
                                        BudgetAlertLevel.WARNING_70 -> SapphireSecondary
                                        BudgetAlertLevel.WARNING_90 -> WarmAmber
                                        BudgetAlertLevel.EXCEEDED -> RefinedRed
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${summary.budget.name} (${summary.budget.categoryName})",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (summary.statusLevel != BudgetAlertLevel.SAFE) {
                                                    Icon(
                                                        Icons.Default.Warning,
                                                        contentDescription = null,
                                                        tint = barColor,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = "${summary.utilizationPercent}%",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = barColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        LinearProgressIndicator(
                                            progress = { (summary.utilizationPercent / 100f).coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(7.dp)
                                                .clip(CircleShape),
                                            color = barColor
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Terpakai: ${IdrFormatter.format(summary.spentAmountIdr, isPrivacy)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = barColor
                                            )
                                            Text(
                                                text = "Batas: ${IdrFormatter.format(summary.budget.limitAmountIdr, isPrivacy)} (Sisa ${IdrFormatter.format(summary.remainingAmountIdr, isPrivacy)})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Category Breakdown for Selected Month (Expense vs Income toggle)
                item {
                    MonthlyCategoryBreakdownCard(
                        monthLabel = monthly.monthLabel,
                        expenseBreakdown = monthly.expenseCategoryBreakdown,
                        incomeBreakdown = monthly.incomeCategoryBreakdown,
                        selectedMode = categorySectionMode,
                        selectedCategoryFilter = uiState.analyticsCategoryFilter,
                        isPrivacyHidden = isPrivacy,
                        onSelectMode = { categorySectionMode = it },
                        onCategoryClicked = { catId ->
                            onCategoryFilterChange(if (uiState.analyticsCategoryFilter == catId) null else catId)
                        }
                    )
                }

                // 5. Search & Type/Category Filter Bar for Selected Month's Transactions
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Cari & Filter Transaksi — ${monthly.monthLabel}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = uiState.analyticsSearchQuery,
                                onValueChange = onSearchChange,
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (uiState.analyticsSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onSearchChange("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Hapus Pencarian")
                                        }
                                    }
                                },
                                placeholder = { Text("Cari nama transaksi, kategori, rekening, atau catatan...") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("analytics_search_input")
                            )

                            // Filter Chips by Jenis Transaksi
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = uiState.analyticsTypeFilter == null,
                                    onClick = { onTypeFilterChange(null) },
                                    label = { Text("Semua Jenis (${monthly.transactionCount})") },
                                    modifier = Modifier.testTag("analytics_filter_type_ALL")
                                )
                                monthly.typeBreakdown.forEach { item ->
                                    FilterChip(
                                        selected = uiState.analyticsTypeFilter == item.type,
                                        onClick = {
                                            onTypeFilterChange(
                                                if (uiState.analyticsTypeFilter == item.type) null else item.type
                                            )
                                        },
                                        label = {
                                            Text("${item.type.labelId} (${item.transactionCount})")
                                        },
                                        modifier = Modifier.testTag("analytics_filter_type_${item.type.name}")
                                    )
                                }
                            }

                            if (uiState.analyticsCategoryFilter != null) {
                                val selectedCatName = uiState.categories.firstOrNull {
                                    it.id == uiState.analyticsCategoryFilter
                                }?.name ?: uiState.analyticsCategoryFilter
                                FilterChip(
                                    selected = true,
                                    onClick = { onCategoryFilterChange(null) },
                                    label = { Text("Kategori: $selectedCatName  ✕") },
                                    modifier = Modifier.testTag("analytics_clear_category_chip")
                                )
                            }

                            // Filtered Subtotal Summary Strip
                            val filteredList = uiState.filteredMonthlyTransactions
                            val filteredTotal = filteredList.sumOf { it.amountIdr }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Menampilkan ${filteredList.size} transaksi",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Subtotal: ${IdrFormatter.format(filteredTotal, isPrivacy)}",
                                    style = TabularAmountRow,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                    }
                }

                // 6. Monthly Transaction List
                if (uiState.filteredMonthlyTransactions.isEmpty()) {
                    item {
                        XynoosEmptyStateCard(
                            title = "Tidak Ada Transaksi pada ${monthly.monthLabel}",
                            subtitle = if (monthly.transactionCount == 0) {
                                "Belum ada pemasukan, pengeluaran, atau transfer yang tercatat pada bulan ${monthly.monthLabel}."
                            } else {
                                "Tidak ada transaksi yang cocok dengan filter pencarian / jenis yang dipilih."
                            },
                            actionLabel = "Catat Transaksi Baru",
                            onActionClick = { onOpenAddTransaction(TransactionType.EXPENSE) }
                        )
                    }
                } else {
                    items(
                        items = uiState.filteredMonthlyTransactions,
                        key = { it.id }
                    ) { tx ->
                        TransactionRowCard(
                            tx = tx,
                            categories = uiState.categories,
                            isPrivacyHidden = isPrivacy,
                            onClick = { onSelectTransaction(tx) }
                        )
                    }
                }
            } else {
                // ================= ANNUAL VIEW =================
                item {
                    AnnualOverviewExecutiveCard(
                        annual = annual,
                        usdRateIdr = usdRate,
                        isPrivacyHidden = isPrivacy,
                        isWideScreen = isWideScreen
                    )
                }

                // 12-Month Bar Comparison Chart
                item {
                    AnnualMonthlyBarChartCard(
                        annual = annual,
                        selectedMonthIndex = uiState.analyticsMonth,
                        isPrivacyHidden = isPrivacy
                    )
                }

                // Interactive 12-Month Breakdown Table (tap any month to jump into its detail)
                item {
                    AnnualMonthlyTableCard(
                        annual = annual,
                        selectedMonthIndex = uiState.analyticsMonth,
                        isPrivacyHidden = isPrivacy,
                        onMonthRowClick = { monthIdx ->
                            onSelectMonth(monthIdx)
                            viewScopeTab = 0
                        }
                    )
                }

                // Annual Category Breakdown (Expenses & Incomes across the full year)
                item {
                    MonthlyCategoryBreakdownCard(
                        monthLabel = "Sepanjang Tahun ${annual.year}",
                        expenseBreakdown = annual.expenseCategoryBreakdown,
                        incomeBreakdown = annual.incomeCategoryBreakdown,
                        selectedMode = categorySectionMode,
                        selectedCategoryFilter = null,
                        isPrivacyHidden = isPrivacy,
                        onSelectMode = { categorySectionMode = it },
                        onCategoryClicked = { }
                    )
                }
            }

            // 7. Quick Detailed Report Export Card (Always available at bottom of analytics)
            item {
                DetailedReportActionCard(
                    monthLabel = monthly.monthLabel,
                    year = annual.year,
                    onExportMonthPdf = {
                        onGenerateDetailedPdf(
                            PdfReportConfig(
                                reportTitle = "Laporan Keuangan Rinci Bulan ${monthly.monthLabel}",
                                periodLabel = monthly.monthLabel,
                                scope = PdfReportScope.SELECTED_MONTH
                            )
                        )
                    },
                    onExportYearPdf = {
                        onGenerateDetailedPdf(
                            PdfReportConfig(
                                reportTitle = "Laporan Keuangan Rinci Tahunan ${annual.year}",
                                periodLabel = "Tahun ${annual.year}",
                                scope = PdfReportScope.FULL_YEAR
                            )
                        )
                    },
                    onExportStatusImage = onGenerateStatusImage,
                    onSendWhatsApp = onSendWhatsAppReport
                )
            }
        }
    }
}

@Composable
private fun MonthlyHeroOverviewCard(
    monthly: MonthlyLedgerSummary,
    usdRateIdr: Double,
    isPrivacyHidden: Boolean,
    isWideScreen: Boolean,
    selectedTypeFilter: TransactionType?,
    onSelectTypeFilter: (TransactionType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0E1824), Color(0xFF142436), Color(0xFF0E2921))
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(EmeraldPrimary.copy(alpha = 0.55f), SapphireSecondary.copy(alpha = 0.45f))
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "REKAP BULANAN RINCI",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = monthly.monthLabel,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFFF4F7FB),
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${monthly.transactionCount} Transaksi",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Primary Income vs Net Expense KPIs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = Color(0xFF111D2B),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Masuk Bulan Ini", style = MaterialTheme.typography.labelSmall, color = Color(0xFF98A3B3))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = IdrFormatter.format(monthly.incomeIdr, isPrivacyHidden, showPlusSign = true),
                            style = TabularBalanceMedium,
                            color = EmeraldPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "≈ ${IdrFormatter.formatUsdFromIdr(monthly.incomeIdr, usdRateIdr, isPrivacyHidden)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF98A3B3)
                        )
                    }
                }
                Surface(
                    color = Color(0xFF111D2B),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Abis / Keluar Bersih", style = MaterialTheme.typography.labelSmall, color = Color(0xFF98A3B3))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = IdrFormatter.format(monthly.netExpenseIdr, isPrivacyHidden),
                            style = TabularBalanceMedium,
                            color = RefinedRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "≈ ${IdrFormatter.formatUsdFromIdr(monthly.netExpenseIdr, usdRateIdr, isPrivacyHidden)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF98A3B3)
                        )
                    }
                }
            }

            // Net Cash Flow & Savings Rate Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF101925))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Surplus / Defisit Bulan Ini", style = MaterialTheme.typography.labelSmall, color = Color(0xFF98A3B3))
                    Text(
                        text = IdrFormatter.format(monthly.netCashFlowIdr, isPrivacyHidden, showPlusSign = true),
                        style = TabularAmountRow,
                        color = if (monthly.netCashFlowIdr >= 0L) EmeraldPrimary else RefinedRed,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Rasio Simpanan", style = MaterialTheme.typography.labelSmall, color = Color(0xFF98A3B3))
                    Text(
                        text = "${monthly.savingsRatePercent}%",
                        style = TabularAmountRow,
                        color = SapphireSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Interactive Per-Type Breakdown Row (Click any type to filter transactions below)
            Text(
                text = "Rincian Per Jenis (Ketuk untuk filter daftar transaksi)",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF98A3B3)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                monthly.typeBreakdown.forEach { item ->
                    val isTypeSelected = selectedTypeFilter == item.type
                    val typeColor = when (item.type) {
                        TransactionType.INCOME, TransactionType.REFUND -> EmeraldPrimary
                        TransactionType.EXPENSE -> RefinedRed
                        TransactionType.TRANSFER -> SapphireSecondary
                        TransactionType.ADJUSTMENT -> WarmAmber
                    }
                    Surface(
                        onClick = { onSelectTypeFilter(item.type) },
                        color = if (isTypeSelected) typeColor.copy(alpha = 0.22f) else Color(0xFF131D2B),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .border(
                                width = if (isTypeSelected) 1.5.dp else 1.dp,
                                color = if (isTypeSelected) typeColor else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .testTag("monthly_type_card_${item.type.name}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${item.type.labelId} (${item.transactionCount}x)",
                                style = MaterialTheme.typography.labelSmall,
                                color = typeColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = IdrFormatter.format(item.totalAmountIdr, isPrivacyHidden),
                                style = TabularAmountRow,
                                color = Color(0xFFF4F7FB)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyCategoryBreakdownCard(
    monthLabel: String,
    expenseBreakdown: List<CategoryBreakdownItem>,
    incomeBreakdown: List<CategoryBreakdownItem>,
    selectedMode: Int,
    selectedCategoryFilter: String?,
    isPrivacyHidden: Boolean,
    onSelectMode: (Int) -> Unit,
    onCategoryClicked: (String) -> Unit
) {
    val activeList = if (selectedMode == 0) expenseBreakdown else incomeBreakdown

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Rincian Per Kategori — $monthLabel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedMode == 0,
                    onClick = { onSelectMode(0) },
                    label = { Text("Pengeluaran (${expenseBreakdown.size})") },
                    modifier = Modifier.testTag("cat_breakdown_expense_chip")
                )
                FilterChip(
                    selected = selectedMode == 1,
                    onClick = { onSelectMode(1) },
                    label = { Text("Pemasukan (${incomeBreakdown.size})") },
                    modifier = Modifier.testTag("cat_breakdown_income_chip")
                )
            }

            if (activeList.isEmpty()) {
                Text(
                    text = if (selectedMode == 0) {
                        "Belum ada pengeluaran tercatat pada periode ini."
                    } else {
                        "Belum ada pemasukan tercatat pada periode ini."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                activeList.forEach { cat ->
                    val isSelected = selectedCategoryFilter == cat.categoryId
                    val barColor = parseColorHex(cat.colorHex)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) barColor.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .clickable { onCategoryClicked(cat.categoryId) }
                            .padding(vertical = 6.dp, horizontal = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(barColor)
                                )
                                Text(
                                    text = "${cat.categoryName} (${cat.transactionCount}x)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${IdrFormatter.format(cat.amountIdr, isPrivacyHidden)} (${String.format(Locale.US, "%.1f", cat.percentage)}%)",
                                style = TabularAmountRow,
                                color = if (selectedMode == 0) RefinedRed else EmeraldPrimary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (cat.percentage / 100f).coerceIn(0.02f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = barColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnualOverviewExecutiveCard(
    annual: AnnualLedgerSummary,
    usdRateIdr: Double,
    isPrivacyHidden: Boolean,
    isWideScreen: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("annual_overview_card"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0E1722), Color(0xFF162435), Color(0xFF0E2920))
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(SapphireSecondary.copy(alpha = 0.5f), EmeraldPrimary.copy(alpha = 0.5f))
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "REKAPITULASI TAHUNAN LENGKAP",
                        style = MaterialTheme.typography.labelSmall,
                        color = SapphireSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tahun ${annual.year}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFFF4F7FB),
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SapphireSecondary.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${annual.transactionCount} Transaksi",
                        style = MaterialTheme.typography.labelMedium,
                        color = SapphireSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = Color(0xFF111D2B),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Total Masuk Tahun ${annual.year}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF98A3B3))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = IdrFormatter.format(annual.totalIncomeIdr, isPrivacyHidden, showPlusSign = true),
                            style = TabularBalanceMedium,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = "≈ ${IdrFormatter.formatUsdFromIdr(annual.totalIncomeIdr, usdRateIdr, isPrivacyHidden)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF98A3B3)
                        )
                    }
                }
                Surface(
                    color = Color(0xFF111D2B),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Total Abis Tahun ${annual.year}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF98A3B3))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = IdrFormatter.format(annual.totalNetExpenseIdr, isPrivacyHidden),
                            style = TabularBalanceMedium,
                            color = RefinedRed
                        )
                        Text(
                            text = "≈ ${IdrFormatter.formatUsdFromIdr(annual.totalNetExpenseIdr, usdRateIdr, isPrivacyHidden)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF98A3B3)
                        )
                    }
                }
            }

            Surface(
                color = Color(0xFF101925),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Surplus / Defisit Tahunan", style = MaterialTheme.typography.bodySmall, color = Color(0xFFBAC6D8))
                        Text(
                            text = IdrFormatter.format(annual.netCashFlowIdr, isPrivacyHidden, showPlusSign = true),
                            style = TabularAmountRow,
                            color = if (annual.netCashFlowIdr >= 0L) EmeraldPrimary else RefinedRed
                        )
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Rata-rata Masuk / Bulan Aktif", style = MaterialTheme.typography.bodySmall, color = Color(0xFF98A3B3))
                        Text(IdrFormatter.format(annual.averageMonthlyIncomeIdr, isPrivacyHidden), style = TabularAmountRow, color = Color(0xFFF4F7FB))
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Rata-rata Keluar / Bulan Aktif", style = MaterialTheme.typography.bodySmall, color = Color(0xFF98A3B3))
                        Text(IdrFormatter.format(annual.averageMonthlyExpenseIdr, isPrivacyHidden), style = TabularAmountRow, color = Color(0xFFF4F7FB))
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Total Transfer & Refund", style = MaterialTheme.typography.bodySmall, color = Color(0xFF98A3B3))
                        Text(
                            text = "${IdrFormatter.formatCompact(annual.totalTransferIdr, isPrivacyHidden)} / ${IdrFormatter.formatCompact(annual.totalRefundIdr, isPrivacyHidden)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SapphireSecondary
                        )
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Bulan Pemasukan Tertinggi", style = MaterialTheme.typography.labelSmall, color = Color(0xFF98A3B3))
                        Text(annual.highestIncomeMonthLabel, style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Bulan Pengeluaran Tertinggi", style = MaterialTheme.typography.labelSmall, color = Color(0xFF98A3B3))
                        Text(annual.highestExpenseMonthLabel, style = MaterialTheme.typography.labelSmall, color = RefinedRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnualMonthlyBarChartCard(
    annual: AnnualLedgerSummary,
    selectedMonthIndex: Int,
    isPrivacyHidden: Boolean
) {
    val maxMonthVal = annual.monthlyBreakdown.maxOfOrNull { maxOf(it.incomeIdr, it.netExpenseIdr) }?.coerceAtLeast(1L) ?: 1L

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grafik Pemasukan vs Pengeluaran (Jan – Des ${annual.year})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(EmeraldPrimary))
                    Text("Masuk", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(RefinedRed))
                    Text("Abis / Keluar", style = MaterialTheme.typography.labelSmall)
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val count = annual.monthlyBreakdown.size.coerceAtLeast(1)
                val slotW = size.width / count.toFloat()
                val barW = (slotW * 0.28f).coerceAtLeast(4f)
                val usableH = size.height - 10f

                for (i in 0..3) {
                    val gy = size.height * (i / 3f)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.16f),
                        start = Offset(0f, gy),
                        end = Offset(size.width, gy),
                        strokeWidth = 1f
                    )
                }

                annual.monthlyBreakdown.forEachIndexed { idx, m ->
                    val cx = slotW * idx + slotW / 2f
                    if (idx == selectedMonthIndex) {
                        drawRoundRect(
                            color = SapphireSecondary.copy(alpha = 0.12f),
                            topLeft = Offset(slotW * idx + 2f, 0f),
                            size = Size(slotW - 4f, size.height),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                    val incH = if (m.incomeIdr > 0L) ((m.incomeIdr.toFloat() / maxMonthVal.toFloat()) * usableH).coerceAtLeast(5f) else 2f
                    val expH = if (m.netExpenseIdr > 0L) ((m.netExpenseIdr.toFloat() / maxMonthVal.toFloat()) * usableH).coerceAtLeast(5f) else 2f

                    drawRoundRect(
                        color = EmeraldPrimary,
                        topLeft = Offset(cx - barW - 1.5f, size.height - incH),
                        size = Size(barW, incH),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = RefinedRed,
                        topLeft = Offset(cx + 1.5f, size.height - expH),
                        size = Size(barW, expH),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                annual.monthlyBreakdown.forEach { m ->
                    Text(
                        text = m.shortMonthLabel.take(1),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (m.monthIndex == selectedMonthIndex) SapphireSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (m.monthIndex == selectedMonthIndex) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnnualMonthlyTableCard(
    annual: AnnualLedgerSummary,
    selectedMonthIndex: Int,
    isPrivacyHidden: Boolean,
    onMonthRowClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Rincian Per Bulan Sepanjang Tahun ${annual.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Ketuk salah satu bulan untuk melihat daftar lengkap transaksinya.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            annual.monthlyBreakdown.forEach { m ->
                val isSelected = m.monthIndex == selectedMonthIndex
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        )
                        .clickable { onMonthRowClick(m.monthIndex) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("annual_month_row_${m.monthIndex}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${FinancialCalculator.getMonthNameFull(m.monthIndex)} (${m.transactionCount} tx)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = IdrFormatter.format(m.netCashFlowIdr, isPrivacyHidden, showPlusSign = true),
                            style = TabularAmountRow,
                            color = if (m.netCashFlowIdr >= 0L) EmeraldPrimary else RefinedRed
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Masuk: ${IdrFormatter.format(m.incomeIdr, isPrivacyHidden)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = "Abis: ${IdrFormatter.format(m.netExpenseIdr, isPrivacyHidden)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = RefinedRed
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("TOTAL ${annual.year}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = "Masuk ${IdrFormatter.formatCompact(annual.totalIncomeIdr, isPrivacyHidden)} • Abis ${IdrFormatter.formatCompact(annual.totalNetExpenseIdr, isPrivacyHidden)}",
                    style = TabularAmountRow,
                    color = SapphireSecondary
                )
            }
        }
    }
}

@Composable
private fun DetailedReportActionCard(
    monthLabel: String,
    year: Int,
    onExportMonthPdf: () -> Unit,
    onExportYearPdf: () -> Unit,
    onExportStatusImage: () -> Unit,
    onSendWhatsApp: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1724)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EmeraldPrimary.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "EKSPOR LAPORAN RINCI (BUKAN CUMA TOTAL)",
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Cetak laporan lengkap berisi rincian per jenis, tabel 12 bulan, mutasi rekening, detail kategori, anggaran, dan daftar transaksi.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFBAC6D8)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExportMonthPdf,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("export_detailed_month_pdf_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color(0xFF042115)
                    )
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PDF $monthLabel", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onClick = onExportYearPdf,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("export_detailed_year_pdf_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SapphireSecondary,
                        contentColor = Color(0xFF061A38)
                    )
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PDF Tahun $year", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onExportStatusImage,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("export_detailed_status_png_button")
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Status PNG Rinci", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onSendWhatsApp,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("export_detailed_wa_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Kirim Rincian WA", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}
