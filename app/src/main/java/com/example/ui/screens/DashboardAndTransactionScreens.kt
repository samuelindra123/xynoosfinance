package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.BudgetAlertLevel
import com.example.domain.DashboardPeriod
import com.example.domain.FinancialAccountModel
import com.example.domain.IdrFormatter
import com.example.domain.TransactionCategoryModel
import com.example.domain.TransactionModel
import com.example.domain.TransactionType
import com.example.ui.components.CashFlowComparisonChart
import com.example.ui.components.CategoryDistributionCard
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.StockMarketCandlestickChart
import com.example.ui.components.XynoosCurrencyNominalInput
import com.example.ui.components.XynoosEmptyStateCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RefinedRed
import com.example.ui.theme.SapphireSecondary
import com.example.ui.theme.TabularAmountRow
import com.example.ui.theme.TabularBalanceLarge
import com.example.ui.theme.TabularBalanceMedium
import com.example.ui.theme.WarmAmber
import com.example.ui.viewmodel.XynoosUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    uiState: XynoosUiState,
    onSelectPeriod: (DashboardPeriod) -> Unit,
    onTogglePrivacy: () -> Unit,
    onOpenAddTransaction: (TransactionType) -> Unit,
    onOpenAiAssistant: () -> Unit,
    onSelectTransaction: (TransactionModel) -> Unit,
    onSyncClicked: () -> Unit,
    onOpenUsdMarketTerminal: () -> Unit = {},
    onQuickExportStatusImage: () -> Unit = {},
    onSendWhatsAppQuick: () -> Unit = {}
) {
    val overview = uiState.overview
    val isPrivacy = uiState.prefs.isPrivacyModeEnabled

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header with Greeting, Sync Status & Privacy Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Selamat datang kembali,",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.prefs.displayName.ifBlank { "Eksekutif Xynoos" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSyncClicked,
                        modifier = Modifier.testTag("sync_now_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isSyncing) Icons.Default.Sync else Icons.Default.CloudDone,
                            contentDescription = "Sinkronisasi Cloud",
                            tint = EmeraldPrimary
                        )
                    }
                    IconButton(
                        onClick = onTogglePrivacy,
                        modifier = Modifier.testTag("privacy_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isPrivacy) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Sembunyikan atau Tampilkan Nominal",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 2. Period Selector Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DashboardPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = uiState.selectedPeriod == period,
                        onClick = { onSelectPeriod(period) },
                        label = { Text(period.labelId) },
                        modifier = Modifier.testTag("period_chip_${period.name}")
                    )
                }
            }
        }

        // 3. Hero Balance & Net Worth Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0E1824),
                                Color(0xFF142634),
                                Color(0xFF0E2921)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(EmeraldPrimary.copy(alpha = 0.5f), SapphireSecondary.copy(alpha = 0.4f))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(22.dp)
                    .testTag("hero_balance_card")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL KAS TERSEDIA (LIKUID)",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.lastSyncMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF98A3B3)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = IdrFormatter.format(overview.availableCashIdr, isPrivacyHidden = isPrivacy),
                        style = TabularBalanceLarge,
                        color = Color(0xFFF4F7FB),
                        modifier = Modifier.testTag("available_cash_text")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "≈ ${IdrFormatter.formatUsdFromIdr(overview.availableCashIdr, uiState.usdMarket.currentRateIdr, isPrivacy)}  •  Kurs 1m: ${IdrFormatter.format(uiState.usdMarket.currentRateIdr.toLong())} / $1 USD (${uiState.usdMarket.nextUpdateSecondsRemaining}d)",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Kekayaan Bersih (Net Worth)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF98A3B3))
                            Text(
                                text = IdrFormatter.format(overview.netWorthIdr, isPrivacyHidden = isPrivacy),
                                style = TabularBalanceMedium,
                                color = SapphireSecondary
                            )
                            Text(
                                text = "≈ ${IdrFormatter.formatUsdFromIdr(overview.netWorthIdr, uiState.usdMarket.currentRateIdr, isPrivacy)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF98A3B3)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Aset / Kewajiban", style = MaterialTheme.typography.labelSmall, color = Color(0xFF98A3B3))
                            Text(
                                text = "${IdrFormatter.formatCompact(overview.totalAssetsIdr, isPrivacy)} / ${IdrFormatter.formatCompact(overview.totalLiabilitiesIdr, isPrivacy)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFF4F7FB)
                            )
                            Text(
                                text = "Target Tabungan: ${overview.savingsProgressPercent}% (Auto)",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }
        }

        // 4. Quick Actions Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        label = "Pemasukan",
                        icon = Icons.Default.Add,
                        accent = EmeraldPrimary,
                        modifier = Modifier.weight(1f).testTag("quick_add_income_button"),
                        onClick = { onOpenAddTransaction(TransactionType.INCOME) }
                    )
                    QuickActionButton(
                        label = "Pengeluaran",
                        icon = Icons.Default.Remove,
                        accent = RefinedRed,
                        modifier = Modifier.weight(1f).testTag("quick_add_expense_button"),
                        onClick = { onOpenAddTransaction(TransactionType.EXPENSE) }
                    )
                    QuickActionButton(
                        label = "Transfer",
                        icon = Icons.Default.SwapHoriz,
                        accent = SapphireSecondary,
                        modifier = Modifier.weight(1f).testTag("quick_add_transfer_button"),
                        onClick = { onOpenAddTransaction(TransactionType.TRANSFER) }
                    )
                    QuickActionButton(
                        label = "Xynoos AI",
                        icon = Icons.Default.AutoAwesome,
                        accent = WarmAmber,
                        modifier = Modifier.weight(1f).testTag("quick_ai_button"),
                        onClick = onOpenAiAssistant
                    )
                }

                // Secondary Executive Quick Strip: Live USD Chart, Export Status PNG, Free WhatsApp Notif
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenUsdMarketTerminal,
                        modifier = Modifier.weight(1f).testTag("quick_open_usd_terminal_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text("📈 Grafik USD 1m", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onQuickExportStatusImage,
                        modifier = Modifier.weight(1f).testTag("quick_export_status_png_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text("🖼️ Export Status PNG", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onSendWhatsAppQuick,
                        modifier = Modifier.weight(1f).testTag("quick_whatsapp_notif_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text("💬 Kirim Notif WA", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }

        // 5. Stock-Style Live USD/IDR (1-Minute) Candlestick Chart Card
        item {
            StockMarketCandlestickChart(
                usdMarket = uiState.usdMarket,
                availableCashIdr = overview.availableCashIdr,
                netWorthIdr = overview.netWorthIdr,
                isPrivacyHidden = isPrivacy,
                chartHeight = 185.dp,
                onOpenFullTerminal = onOpenUsdMarketTerminal
            )
        }

        // 6. Period Net Cash Flow & Auto-Connected Savings Progress Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Arus Kas Bersih (${uiState.selectedPeriod.labelId})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = IdrFormatter.format(overview.netCashFlowIdr, isPrivacyHidden = isPrivacy, showPlusSign = true),
                            style = TabularBalanceMedium,
                            color = if (overview.netCashFlowIdr >= 0L) EmeraldPrimary else RefinedRed
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (overview.budgetUtilizationPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = if (overview.budgetUtilizationPercent >= 90) RefinedRed else EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Utilisasi Anggaran: ${overview.budgetUtilizationPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Target Tabungan (Auto-Saldo)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = IdrFormatter.format(overview.totalGoalsSavedIdr, isPrivacyHidden = isPrivacy),
                            style = TabularBalanceMedium,
                            color = SapphireSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (overview.savingsProgressPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = SapphireSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Terkoneksi Saldo: ${overview.savingsProgressPercent}% (${IdrFormatter.formatUsdFromIdr(overview.totalGoalsSavedIdr, uiState.usdMarket.currentRateIdr, isPrivacy)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 7. Real Charts from Database Query Results
        item {
            CashFlowComparisonChart(
                incomeIdr = overview.periodIncomeIdr,
                expenseIdr = overview.periodExpenseIdr,
                transactions = uiState.periodTransactions,
                isPrivacyHidden = isPrivacy
            )
        }

        item {
            CategoryDistributionCard(
                breakdown = uiState.categoryBreakdown,
                isPrivacyHidden = isPrivacy
            )
        }

        // 7. Recent Activity Header & List
        item {
            Text(
                text = "Aktivitas Transaksi Terbaru",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (uiState.periodTransactions.isEmpty()) {
            item {
                XynoosEmptyStateCard(
                    title = "Belum Ada Transaksi pada Periode Ini",
                    subtitle = "Catat pemasukan, pengeluaran, atau transfer antar rekening untuk melihat grafik arus kas secara real-time.",
                    actionLabel = "Catat Transaksi Pertama",
                    onActionClick = { onOpenAddTransaction(TransactionType.EXPENSE) }
                )
            }
        } else {
            items(
                items = uiState.periodTransactions.take(8),
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
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(68.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TransactionRowCard(
    tx: TransactionModel,
    categories: List<TransactionCategoryModel>,
    isPrivacyHidden: Boolean,
    onClick: () -> Unit
) {
    val matchedCat = categories.firstOrNull { it.id == tx.categoryId }
    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy • HH:mm 'WIB' ('Tahun' yyyy)", Locale("id", "ID")) }
    val amountColor = when (tx.type) {
        TransactionType.INCOME, TransactionType.REFUND -> EmeraldPrimary
        TransactionType.EXPENSE -> RefinedRed
        TransactionType.TRANSFER -> SapphireSecondary
        TransactionType.ADJUSTMENT -> WarmAmber
    }
    val prefix = when (tx.type) {
        TransactionType.INCOME, TransactionType.REFUND -> "+"
        TransactionType.EXPENSE -> "-"
        TransactionType.TRANSFER -> "⇄ "
        TransactionType.ADJUSTMENT -> "±"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("transaction_item_${tx.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryIconBadge(
                iconKey = matchedCat?.iconKey ?: "category",
                colorHex = matchedCat?.colorHex ?: "#49E0A0"
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val accountDisplay = if (tx.type == TransactionType.TRANSFER && !tx.toAccountName.isNullOrBlank()) {
                    "${tx.accountName} → ${tx.toAccountName}"
                } else {
                    "${tx.categoryName} • ${tx.accountName}"
                }
                Text(
                    text = accountDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = dateFormat.format(Date(tx.transactionDateMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefix${IdrFormatter.format(tx.amountIdr, isPrivacyHidden = isPrivacyHidden)}",
                    style = TabularAmountRow,
                    color = amountColor
                )
                if (tx.projectCostCenterId.isNotBlank()) {
                    Text(
                        text = tx.projectCostCenterId,
                        style = MaterialTheme.typography.labelSmall,
                        color = SapphireSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionsScreen(
    uiState: XynoosUiState,
    onSearchChange: (String) -> Unit,
    onTypeFilterChange: (TransactionType?) -> Unit,
    onAccountFilterChange: (String?) -> Unit,
    onToggleSort: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onSelectTransaction: (TransactionModel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("transactions_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Buku Besar Transaksi",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Row {
                IconButton(onClick = onToggleSort, modifier = Modifier.testTag("sort_transactions_button")) {
                    Icon(Icons.Default.Sort, contentDescription = "Urutkan Transaksi")
                }
                Button(
                    onClick = onAddTransactionClick,
                    modifier = Modifier.testTag("add_transaction_top_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Catat")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChange,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Cari deskripsi, kategori, akun, atau proyek...") },
            modifier = Modifier.fillMaxWidth().testTag("search_transactions_input"),
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Type Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.selectedTypeFilter == null,
                onClick = { onTypeFilterChange(null) },
                label = { Text("Semua Tipe") }
            )
            TransactionType.entries.forEach { t ->
                FilterChip(
                    selected = uiState.selectedTypeFilter == t,
                    onClick = { onTypeFilterChange(if (uiState.selectedTypeFilter == t) null else t) },
                    label = { Text(t.labelId) }
                )
            }
        }

        // Account Filter Chips
        if (uiState.accounts.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedAccountIdFilter == null,
                    onClick = { onAccountFilterChange(null) },
                    label = { Text("Semua Akun") }
                )
                uiState.accounts.forEach { acc ->
                    FilterChip(
                        selected = uiState.selectedAccountIdFilter == acc.id,
                        onClick = {
                            onAccountFilterChange(if (uiState.selectedAccountIdFilter == acc.id) null else acc.id)
                        },
                        label = { Text(acc.name) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (uiState.filteredTransactions.isEmpty()) {
            XynoosEmptyStateCard(
                title = "Tidak Ada Transaksi Ditemukan",
                subtitle = "Ubah kata kunci pencarian atau tambahkan transaksi baru ke buku besar.",
                actionLabel = "Tambah Transaksi",
                onActionClick = onAddTransactionClick
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(
                    items = uiState.filteredTransactions,
                    key = { it.id }
                ) { tx ->
                    TransactionRowCard(
                        tx = tx,
                        categories = uiState.categories,
                        isPrivacyHidden = uiState.prefs.isPrivacyModeEnabled,
                        onClick = { onSelectTransaction(tx) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormBottomSheet(
    initialType: TransactionType = TransactionType.EXPENSE,
    editingTransaction: TransactionModel? = null,
    accounts: List<FinancialAccountModel>,
    categories: List<TransactionCategoryModel>,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        type: TransactionType,
        amountIdr: Long,
        title: String,
        categoryId: String,
        categoryName: String,
        accountId: String,
        accountName: String,
        toAccountId: String?,
        toAccountName: String?,
        notes: String,
        merchantName: String,
        projectCostCenterId: String
    ) -> Unit
) {
    var selectedType by rememberSaveable { mutableStateOf(editingTransaction?.type ?: initialType) }
    var amountInput by rememberSaveable { mutableStateOf(editingTransaction?.amountIdr?.toString() ?: "") }
    var titleInput by rememberSaveable { mutableStateOf(editingTransaction?.title ?: "") }
    var notesInput by rememberSaveable { mutableStateOf(editingTransaction?.notes ?: "") }
    var merchantInput by rememberSaveable { mutableStateOf(editingTransaction?.merchantName ?: "") }
    var projectInput by rememberSaveable { mutableStateOf(editingTransaction?.projectCostCenterId ?: "") }

    val availableCategories = categories.filter {
        if (selectedType == TransactionType.INCOME) it.type == TransactionType.INCOME else it.type == TransactionType.EXPENSE
    }.ifEmpty { categories }

    var selectedCategoryId by rememberSaveable {
        mutableStateOf(editingTransaction?.categoryId ?: availableCategories.firstOrNull()?.id ?: "cat_food")
    }
    var selectedAccountId by rememberSaveable {
        mutableStateOf(editingTransaction?.accountId ?: accounts.firstOrNull()?.id ?: "acc_default")
    }
    var selectedToAccountId by rememberSaveable {
        mutableStateOf(editingTransaction?.toAccountId ?: accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id ?: "")
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .testTag("transaction_form_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (editingTransaction == null) "Catat Transaksi Baru" else "Edit Transaksi",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // Type Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionType.entries.forEach { t ->
                    FilterChip(
                        selected = selectedType == t,
                        onClick = {
                            selectedType = t
                            val firstMatching = categories.firstOrNull {
                                if (t == TransactionType.INCOME) it.type == TransactionType.INCOME else it.type == TransactionType.EXPENSE
                            }
                            if (firstMatching != null) selectedCategoryId = firstMatching.id
                        },
                        label = { Text(t.labelId) },
                        modifier = Modifier.testTag("tx_type_chip_${t.name}")
                    )
                }
            }

            // Dedicated Financial Nominal & Balance Calculator Input UI
            val parsedAmount = IdrFormatter.parseIdrInput(amountInput) ?: 0L
            XynoosCurrencyNominalInput(
                value = amountInput,
                onValueChange = {
                    amountInput = it
                    validationError = null
                },
                label = "Nominal Transaksi (${selectedType.labelId})",
                inputTestTag = "tx_amount_input",
                accentColor = when (selectedType) {
                    TransactionType.INCOME, TransactionType.REFUND -> EmeraldPrimary
                    TransactionType.EXPENSE -> RefinedRed
                    else -> SapphireSecondary
                },
                helperSubtitle = "Ketik angka atau gunakan tombol hitung nominal di bawah"
            )

            // Live Account Balance Calculation Preview ("Simulasi Hitungan Saldo Rekening")
            val activeAcc = accounts.firstOrNull { it.id == selectedAccountId } ?: accounts.firstOrNull()
            if (activeAcc != null) {
                val currentAccBal = activeAcc.currentBalanceIdr
                val projectedAccBal = when (selectedType) {
                    TransactionType.INCOME, TransactionType.REFUND, TransactionType.ADJUSTMENT -> currentAccBal + parsedAmount
                    TransactionType.EXPENSE, TransactionType.TRANSFER -> currentAccBal - parsedAmount
                }
                val deltaSign = when (selectedType) {
                    TransactionType.INCOME, TransactionType.REFUND, TransactionType.ADJUSTMENT -> "+"
                    TransactionType.EXPENSE, TransactionType.TRANSFER -> "-"
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "SIMULASI HITUNGAN SALDO REKENING (${activeAcc.name})",
                            style = MaterialTheme.typography.labelSmall,
                            color = SapphireSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Saldo Saat Ini", style = MaterialTheme.typography.bodySmall)
                            Text(IdrFormatter.format(currentAccBal), style = TabularAmountRow)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Mutasi (${selectedType.labelId})", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "$deltaSign${IdrFormatter.format(parsedAmount)}",
                                style = TabularAmountRow,
                                color = if (deltaSign == "+") EmeraldPrimary else RefinedRed
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Saldo Akhir Setelah Simpan", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = IdrFormatter.format(projectedAccBal),
                                style = TabularBalanceMedium,
                                color = if (projectedAccBal >= 0L) EmeraldPrimary else RefinedRed,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            val fullDateStamp = remember(editingTransaction) {
                val fmt = SimpleDateFormat("EEEE, dd MMMM yyyy • HH:mm:ss 'WIB' ('Tahun' yyyy)", Locale("id", "ID"))
                fmt.format(Date(editingTransaction?.transactionDateMillis ?: System.currentTimeMillis()))
            }
            Text(
                text = "Waktu & Tahun Tercatat: $fullDateStamp",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                label = { Text("Judul / Deskripsi Transaksi") },
                modifier = Modifier.fillMaxWidth().testTag("tx_title_input"),
                singleLine = true
            )

            // Account Selection
            if (accounts.isNotEmpty()) {
                Text("Rekening Sumber", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text(acc.name) }
                        )
                    }
                }
            }

            if (selectedType == TransactionType.TRANSFER && accounts.size > 1) {
                Text("Rekening Tujuan Transfer", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = selectedToAccountId == acc.id,
                            onClick = { selectedToAccountId = acc.id },
                            label = { Text(acc.name) }
                        )
                    }
                }
            }

            // Category Selection
            if (selectedType != TransactionType.TRANSFER && availableCategories.isNotEmpty()) {
                Text("Kategori", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = merchantInput,
                    onValueChange = { merchantInput = it },
                    label = { Text("Merchant / Klien (Opsional)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = projectInput,
                    onValueChange = { projectInput = it },
                    label = { Text("Proyek / Cost Center") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = notesInput,
                onValueChange = { notesInput = it },
                label = { Text("Catatan Tambahan (Opsional)") },
                modifier = Modifier.fillMaxWidth()
            )

            if (validationError != null) {
                Text(
                    text = validationError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("Batal")
                }
                Button(
                    onClick = {
                        if (parsedAmount <= 0L) {
                            validationError = "Masukkan nominal lebih besar dari Rp0."
                            return@Button
                        }
                        if (selectedType == TransactionType.TRANSFER && selectedAccountId == selectedToAccountId) {
                            validationError = "Rekening asal dan tujuan transfer tidak boleh sama."
                            return@Button
                        }
                        val chosenAcc = accounts.firstOrNull { it.id == selectedAccountId } ?: accounts.firstOrNull()
                        val chosenToAcc = accounts.firstOrNull { it.id == selectedToAccountId }
                        val chosenCat = categories.firstOrNull { it.id == selectedCategoryId }

                        onSave(
                            editingTransaction?.id,
                            selectedType,
                            parsedAmount,
                            titleInput.ifBlank {
                                if (selectedType == TransactionType.TRANSFER) "Transfer Antar Rekening" else (chosenCat?.name ?: "Transaksi")
                            },
                            if (selectedType == TransactionType.TRANSFER) "cat_transfer" else (chosenCat?.id ?: "cat_other"),
                            if (selectedType == TransactionType.TRANSFER) "Transfer Internal" else (chosenCat?.name ?: "Lainnya"),
                            chosenAcc?.id ?: "acc_default",
                            chosenAcc?.name ?: "Tunai Utama",
                            chosenToAcc?.id,
                            chosenToAcc?.name,
                            notesInput,
                            merchantInput,
                            projectInput
                        )
                    },
                    modifier = Modifier.weight(1f).height(50.dp).testTag("save_transaction_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Simpan Transaksi")
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun TransactionDetailDialog(
    transaction: TransactionModel,
    isPrivacyHidden: Boolean,
    onDismiss: () -> Unit,
    onEdit: (TransactionModel) -> Unit,
    onDelete: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEEE, dd MMMM yyyy • HH:mm:ss 'WIB' ('Tahun' yyyy)", Locale("id", "ID")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(transaction.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = IdrFormatter.format(transaction.amountIdr, isPrivacyHidden = isPrivacyHidden),
                    style = TabularBalanceLarge,
                    color = if (transaction.type == TransactionType.INCOME) EmeraldPrimary else SapphireSecondary
                )
                Text("Tipe: ${transaction.type.labelId}", style = MaterialTheme.typography.bodyMedium)
                Text("Kategori: ${transaction.categoryName}", style = MaterialTheme.typography.bodyMedium)
                Text("Rekening: ${transaction.accountName}", style = MaterialTheme.typography.bodyMedium)
                if (!transaction.toAccountName.isNullOrBlank()) {
                    Text("Rekening Tujuan: ${transaction.toAccountName}", style = MaterialTheme.typography.bodyMedium)
                }
                Text("Waktu & Tahun Detail: ${dateFormat.format(Date(transaction.transactionDateMillis))}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (transaction.merchantName.isNotBlank()) {
                    Text("Merchant/Klien: ${transaction.merchantName}", style = MaterialTheme.typography.bodyMedium)
                }
                if (transaction.projectCostCenterId.isNotBlank()) {
                    Text("Proyek/Cost Center: ${transaction.projectCostCenterId}", style = MaterialTheme.typography.bodyMedium)
                }
                if (transaction.notes.isNotBlank()) {
                    Text("Catatan: ${transaction.notes}", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = "Status Sinkronisasi: ${transaction.syncStatus.labelId} (v${transaction.version})",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldPrimary
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { onDelete(transaction.id) },
                    modifier = Modifier.testTag("delete_transaction_confirm_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = RefinedRed)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hapus", color = RefinedRed)
                }
                Button(
                    onClick = { onEdit(transaction) },
                    modifier = Modifier.testTag("edit_transaction_button")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
