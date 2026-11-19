package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.domain.AccountType
import com.example.domain.AppThemeMode
import com.example.domain.IdrFormatter
import com.example.domain.InvoiceReferenceModel
import com.example.domain.InvoiceStatus
import com.example.domain.InvoiceType
import com.example.domain.TransactionModel
import com.example.domain.TransactionType
import com.example.export.PdfReportConfig
import com.example.export.PdfReportGenerator
import com.example.export.PdfReportScope
import com.example.export.StatusImageExporter
import com.example.notifications.ReminderNotificationHelper
import com.example.notifications.WhatsAppNotifierHelper
import com.example.ui.components.SetupPatternLockDialog
import com.example.ui.components.StockMarketCandlestickChart
import com.example.ui.components.XynoosCurrencyNominalInput
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RefinedRed
import com.example.ui.theme.SapphireSecondary
import com.example.ui.theme.TabularAmountRow
import com.example.ui.theme.TabularBalanceMedium
import com.example.ui.theme.WarmAmber
import com.example.ui.viewmodel.XynoosUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MoreSectionDestination {
    HUB,
    MONTHLY_ANNUAL_ANALYTICS,
    USD_LIVE_MARKET,
    ACCOUNTS,
    BUSINESS,
    PDF_EXPORT,
    CATEGORIES_RECURRING,
    SETTINGS_PRIVACY
}

@Composable
fun MoreSectionScreen(
    uiState: XynoosUiState,
    initialDestination: MoreSectionDestination = MoreSectionDestination.HUB,
    onSaveAccount: (String?, String, AccountType, Long, String, String) -> Unit,
    onSaveInvoice: (String, String, InvoiceType, Long, InvoiceStatus, String) -> Unit,
    onMarkInvoicePaid: (InvoiceReferenceModel) -> Unit,
    onDeleteInvoice: (String) -> Unit,
    onSaveCategory: (String, TransactionType, String, String) -> Unit,
    onSaveRecurring: (String, TransactionType, Long, String, String, String, String, String) -> Unit,
    onDeleteRecurring: (String) -> Unit,
    onGeneratePdf: (PdfReportConfig) -> Unit,
    onGenerateStatusImage: (Boolean) -> Unit = {},
    onRefreshUsdMarket: () -> Unit = {},
    onUpdateWhatsAppSettings: (String, Boolean) -> Unit = { _, _ -> },
    onSendWhatsAppNow: (String?) -> Unit = {},
    onUpdateReportIdentity: (String, String, String, String, String) -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onTogglePrivacyMode: () -> Unit,
    onUpdateReminders: (Boolean, Int, Int, Boolean, Boolean, Boolean) -> Unit,
    onUpdateAutoBackupEnabled: (Boolean) -> Unit = {},
    onUpdatePatternLock: (Boolean, String, Boolean) -> Unit = { _, _, _ -> },
    onCheckAppUpdate: () -> Unit = {},
    onUpdateGitHubRepo: (String) -> Unit = {},
    onSyncCloudNow: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccountAndData: () -> Unit,
    onSelectAnalyticsYear: (Int) -> Unit = {},
    onSelectAnalyticsMonth: (Int) -> Unit = {},
    onSelectAnalyticsYearMonth: (Int, Int) -> Unit = { _, _ -> },
    onAnalyticsSearchChange: (String) -> Unit = {},
    onAnalyticsTypeFilterChange: (TransactionType?) -> Unit = {},
    onAnalyticsCategoryFilterChange: (String?) -> Unit = {},
    onSelectTransaction: (TransactionModel) -> Unit = {},
    onOpenAddTransaction: (TransactionType) -> Unit = {}
) {
    var currentSubScreen by rememberSaveable(initialDestination) { mutableStateOf(initialDestination) }

    if (currentSubScreen != MoreSectionDestination.HUB) {
        BackHandler {
            currentSubScreen = MoreSectionDestination.HUB
        }
    }

    when (currentSubScreen) {
        MoreSectionDestination.HUB -> {
            MoreHubMenu(
                uiState = uiState,
                onNavigate = { currentSubScreen = it },
                onSendWhatsAppQuick = { onSendWhatsAppNow(null) }
            )
        }
        MoreSectionDestination.MONTHLY_ANNUAL_ANALYTICS -> {
            MonthlyAnnualBudgetSubScreen(
                uiState = uiState,
                onBack = { currentSubScreen = MoreSectionDestination.HUB },
                onSelectYear = onSelectAnalyticsYear,
                onSelectMonth = onSelectAnalyticsMonth,
                onSelectYearMonth = onSelectAnalyticsYearMonth,
                onSearchChange = onAnalyticsSearchChange,
                onTypeFilterChange = onAnalyticsTypeFilterChange,
                onCategoryFilterChange = onAnalyticsCategoryFilterChange,
                onSelectTransaction = onSelectTransaction,
                onOpenAddTransaction = onOpenAddTransaction,
                onGenerateDetailedPdf = onGeneratePdf,
                onGenerateStatusImage = { onGenerateStatusImage(true) },
                onSendWhatsAppReport = { onSendWhatsAppNow(null) }
            )
        }
        MoreSectionDestination.USD_LIVE_MARKET -> {
            UsdLiveMarketSubScreen(
                uiState = uiState,
                onBack = { currentSubScreen = MoreSectionDestination.HUB },
                onRefreshNow = onRefreshUsdMarket
            )
        }
        MoreSectionDestination.ACCOUNTS -> {
            AccountsWalletsSubScreen(
                uiState = uiState,
                onBack = { currentSubScreen = MoreSectionDestination.HUB },
                onSaveAccount = onSaveAccount
            )
        }
        MoreSectionDestination.BUSINESS -> {
            BusinessFinanceSubScreen(
                uiState = uiState,
                onBack = { currentSubScreen = MoreSectionDestination.HUB },
                onSaveInvoice = onSaveInvoice,
                onMarkInvoicePaid = onMarkInvoicePaid,
                onDeleteInvoice = onDeleteInvoice
            )
        }
        MoreSectionDestination.PDF_EXPORT -> {
            PdfExportCenterSubScreen(
                uiState = uiState,
                onBack = { currentSubScreen = MoreSectionDestination.HUB },
                onGeneratePdf = onGeneratePdf,
                onGenerateStatusImage = onGenerateStatusImage,
                onSendWhatsAppNow = { onSendWhatsAppNow(null) },
                onUpdateReportIdentity = onUpdateReportIdentity
            )
        }
        MoreSectionDestination.CATEGORIES_RECURRING -> {
            CategoriesAndRecurringSubScreen(
                uiState = uiState,
                onBack = { currentSubScreen = MoreSectionDestination.HUB },
                onSaveCategory = onSaveCategory,
                onSaveRecurring = onSaveRecurring,
                onDeleteRecurring = onDeleteRecurring
            )
        }
        MoreSectionDestination.SETTINGS_PRIVACY -> {
            SettingsAndPrivacySubScreen(
                uiState = uiState,
                onBack = { currentSubScreen = MoreSectionDestination.HUB },
                onSetThemeMode = onSetThemeMode,
                onTogglePrivacyMode = onTogglePrivacyMode,
                onUpdateReminders = onUpdateReminders,
                onUpdateAutoBackupEnabled = onUpdateAutoBackupEnabled,
                onUpdatePatternLock = onUpdatePatternLock,
                onCheckAppUpdate = onCheckAppUpdate,
                onUpdateGitHubRepo = onUpdateGitHubRepo,
                onUpdateWhatsAppSettings = onUpdateWhatsAppSettings,
                onSendWhatsAppNow = onSendWhatsAppNow,
                onSyncCloudNow = onSyncCloudNow,
                onSignOut = onSignOut,
                onDeleteAccountAndData = onDeleteAccountAndData
            )
        }
    }
}

@Composable
private fun MoreHubMenu(
    uiState: XynoosUiState,
    onNavigate: (MoreSectionDestination) -> Unit,
    onSendWhatsAppQuick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("more_hub_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text("Menu & Ekosistem Keuangan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Akses terminal grafik kurs Dollar (USD) per menit, ekspor gambar status & PDF, notifikasi WhatsApp gratis, dan pusat bisnis.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            MoreMenuCard(
                title = "Anggaran, Pemasukan & Rekap Bulanan/Tahunan",
                subtitle = "Cari & filter transaksi per jenis/bulan (${uiState.monthlyLedgerSummary.monthLabel}) + Rekap masuk & abis Tahun ${uiState.analyticsYear}",
                icon = Icons.Default.CalendarMonth,
                accent = EmeraldPrimary,
                testTag = "menu_monthly_annual_card",
                onClick = { onNavigate(MoreSectionDestination.MONTHLY_ANNUAL_ANALYTICS) }
            )
        }

        item {
            MoreMenuCard(
                title = "Pasar & Grafik Live USD/IDR (Update 1 Menit)",
                subtitle = "Kurs Spot: ${IdrFormatter.format(uiState.usdMarket.currentRateIdr.toLong())} / $1 USD • Grafik Saham Candlestick & Konversi Saldo USD",
                icon = Icons.Default.TrendingUp,
                accent = EmeraldPrimary,
                testTag = "menu_usd_market_card",
                onClick = { onNavigate(MoreSectionDestination.USD_LIVE_MARKET) }
            )
        }

        item {
            MoreMenuCard(
                title = "Pusat Laporan, Ekspor Gambar Status PNG & PDF",
                subtitle = "Export Gambar Status Keren (WA/IG Story) & Cetak Laporan A4 Multi-Halaman",
                icon = Icons.Default.Image,
                accent = WarmAmber,
                testTag = "menu_pdf_export_card",
                onClick = { onNavigate(MoreSectionDestination.PDF_EXPORT) }
            )
        }

        item {
            MoreMenuCard(
                title = "Rekening, Dompet & Kewajiban (IDR & USD)",
                subtitle = "${uiState.accounts.size} akun aktif • Kelola Tunai, Bank, E-Wallet, Tabungan & Pinjaman",
                icon = Icons.Default.AccountBalanceWallet,
                accent = EmeraldPrimary,
                testTag = "menu_accounts_card",
                onClick = { onNavigate(MoreSectionDestination.ACCOUNTS) }
            )
        }

        item {
            MoreMenuCard(
                title = "Bisnis, Laba Rugi, Piutang & Utang",
                subtitle = "Laporan P&L kas terealisasi vs piutang klien (${uiState.invoices.size} faktur) & cost center",
                icon = Icons.Default.BusinessCenter,
                accent = SapphireSecondary,
                testTag = "menu_business_card",
                onClick = { onNavigate(MoreSectionDestination.BUSINESS) }
            )
        }

        item {
            MoreMenuCard(
                title = "Kategori & Transaksi Berulang",
                subtitle = "${uiState.categories.size} kategori & ${uiState.recurringTransactions.size} jadwal transaksi rutin",
                icon = Icons.Default.Category,
                accent = EmeraldPrimary,
                testTag = "menu_categories_card",
                onClick = { onNavigate(MoreSectionDestination.CATEGORIES_RECURRING) }
            )
        }

        item {
            MoreMenuCard(
                title = "Notifikasi WhatsApp Gratis (XynoosFinance) & Pengaturan",
                subtitle = "Kirim rekap saldo ke nomor WA Anda (${uiState.prefs.whatsAppPhone.ifBlank { "Atur Nomor" }}), Tema & Privasi",
                icon = Icons.Default.Settings,
                accent = SapphireSecondary,
                testTag = "menu_settings_card",
                onClick = { onNavigate(MoreSectionDestination.SETTINGS_PRIVACY) }
            )
        }
    }
}

@Composable
private fun MoreMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SubScreenTopBar(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("subscreen_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        action?.invoke()
    }
}

@Composable
private fun AccountsWalletsSubScreen(
    uiState: XynoosUiState,
    onBack: () -> Unit,
    onSaveAccount: (String?, String, AccountType, Long, String, String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val isPrivacy = uiState.prefs.isPrivacyModeEnabled

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp).testTag("accounts_subscreen")
    ) {
        SubScreenTopBar(
            title = "Rekening & Dompet",
            onBack = onBack,
            action = {
                Button(onClick = { showAddDialog = true }, modifier = Modifier.testTag("add_account_button")) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Akun")
                }
            }
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(uiState.accounts, key = { it.id }) { acc ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("account_card_${acc.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(acc.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${acc.accountType.labelId} ${if (acc.institutionName.isNotBlank()) "• ${acc.institutionName}" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Saldo Awal: ${IdrFormatter.format(acc.openingBalanceIdr, isPrivacy)} • ${acc.syncStatus.labelId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary
                            )
                        }
                        Text(
                            text = IdrFormatter.format(acc.currentBalanceIdr, isPrivacy),
                            style = TabularBalanceMedium,
                            color = if (acc.accountType.isLiability) RefinedRed else EmeraldPrimary
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var institution by remember { mutableStateOf("") }
        var openingInput by remember { mutableStateOf("0") }
        var selectedType by remember { mutableStateOf(AccountType.BANK) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah Rekening / Dompet") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Akun (Misal: Mandiri Bisnis / GoPay)") },
                        singleLine = true,
                        modifier = Modifier.testTag("new_account_name_input")
                    )
                    OutlinedTextField(
                        value = institution,
                        onValueChange = { institution = it },
                        label = { Text("Nama Institusi / Bank (Opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    XynoosCurrencyNominalInput(
                        value = openingInput,
                        onValueChange = { openingInput = it },
                        label = "Saldo Awal Rekening / Dompet (IDR)",
                        inputTestTag = "new_account_balance_input",
                        accentColor = EmeraldPrimary,
                        compactMode = true
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AccountType.entries.forEach { t ->
                            FilterChip(
                                selected = selectedType == t,
                                onClick = { selectedType = t },
                                label = { Text(t.labelId) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bal = IdrFormatter.parseIdrInput(openingInput) ?: 0L
                        if (name.isNotBlank()) {
                            onSaveAccount(null, name, selectedType, bal, "#49E0A0", institution)
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_save_account_button")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun BusinessFinanceSubScreen(
    uiState: XynoosUiState,
    onBack: () -> Unit,
    onSaveInvoice: (String, String, InvoiceType, Long, InvoiceStatus, String) -> Unit,
    onMarkInvoicePaid: (InvoiceReferenceModel) -> Unit,
    onDeleteInvoice: (String) -> Unit
) {
    var showInvoiceDialog by remember { mutableStateOf(false) }
    val pl = uiState.businessProfitLoss
    val isPrivacy = uiState.prefs.isPrivacyModeEnabled

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp).testTag("business_subscreen")
    ) {
        SubScreenTopBar(
            title = "Bisnis, Laba Rugi & Faktur",
            onBack = onBack,
            action = {
                Button(onClick = { showInvoiceDialog = true }, modifier = Modifier.testTag("add_invoice_button")) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Catat Faktur")
                }
            }
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Laporan Laba Rugi Kas Terealisasi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Memisahkan kas masuk nyata dari piutang klien yang belum dibayar.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Pendapatan Kas Terealisasi")
                            Text(IdrFormatter.format(pl.realizedRevenueIdr, isPrivacy), style = TabularAmountRow, color = EmeraldPrimary)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Beban Operasional")
                            Text(IdrFormatter.format(pl.operatingExpensesIdr, isPrivacy), style = TabularAmountRow, color = RefinedRed)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Pajak & Retribusi")
                            Text(IdrFormatter.format(pl.taxExpensesIdr, isPrivacy), style = TabularAmountRow, color = WarmAmber)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Laba / Rugi Bersih Kas", fontWeight = FontWeight.Bold)
                            Text(
                                IdrFormatter.format(pl.netProfitIdr, isPrivacy, showPlusSign = true),
                                style = TabularBalanceMedium,
                                color = if (pl.netProfitIdr >= 0L) EmeraldPrimary else RefinedRed
                            )
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Piutang Belum Tertagih", style = MaterialTheme.typography.labelSmall)
                            Text(IdrFormatter.format(pl.unpaidReceivablesIdr, isPrivacy), style = TabularBalanceMedium, color = SapphireSecondary)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Utang Vendor Belum Lunas", style = MaterialTheme.typography.labelSmall)
                            Text(IdrFormatter.format(pl.unpaidPayablesIdr, isPrivacy), style = TabularBalanceMedium, color = WarmAmber)
                        }
                    }
                }
            }

            item {
                Text("Daftar Piutang & Utang (Invoices)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(uiState.invoices, key = { it.id }) { inv ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${inv.invoiceNumber} • ${inv.counterpartyName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("${inv.invoiceType.labelId} • Status: ${inv.status.labelId}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(IdrFormatter.format(inv.amountIdr, isPrivacy), style = TabularAmountRow, color = SapphireSecondary)
                        }
                        if (inv.status != InvoiceStatus.PAID) {
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onMarkInvoicePaid(inv) }) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Tandai Lunas & Catat Kas")
                                }
                                TextButton(onClick = { onDeleteInvoice(inv.id) }) {
                                    Text("Hapus", color = RefinedRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInvoiceDialog) {
        var invNumber by remember { mutableStateOf("INV-2026-01") }
        var counterparty by remember { mutableStateOf("") }
        var amountInput by remember { mutableStateOf("5000000") }
        var invType by remember { mutableStateOf(InvoiceType.RECEIVABLE) }
        var project by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showInvoiceDialog = false },
            title = { Text("Catat Piutang / Utang Bisnis") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        InvoiceType.entries.forEach { t ->
                            FilterChip(selected = invType == t, onClick = { invType = t }, label = { Text(t.name) })
                        }
                    }
                    OutlinedTextField(value = invNumber, onValueChange = { invNumber = it }, label = { Text("Nomor Invoice") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = counterparty, onValueChange = { counterparty = it }, label = { Text("Nama Klien / Vendor") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    XynoosCurrencyNominalInput(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = "Nominal Faktur / Invoice (IDR)",
                        accentColor = SapphireSecondary,
                        compactMode = true
                    )
                    OutlinedTextField(value = project, onValueChange = { project = it }, label = { Text("Proyek / Cost Center") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = IdrFormatter.parseIdrInput(amountInput) ?: 0L
                    if (counterparty.isNotBlank() && amt > 0L) {
                        onSaveInvoice(invNumber, counterparty, invType, amt, InvoiceStatus.UNPAID, project)
                        showInvoiceDialog = false
                    }
                }) {
                    Text("Simpan Faktur")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInvoiceDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun PdfExportCenterSubScreen(
    uiState: XynoosUiState,
    onBack: () -> Unit,
    onGeneratePdf: (PdfReportConfig) -> Unit,
    onGenerateStatusImage: (Boolean) -> Unit,
    onSendWhatsAppNow: () -> Unit,
    onUpdateReportIdentity: (String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var reportScope by rememberSaveable { mutableStateOf(PdfReportScope.SELECTED_MONTH) }
    var reportTitle by rememberSaveable { mutableStateOf("Laporan Eksekutif Keuangan Rinci & Buku Besar") }
    var orgName by rememberSaveable { mutableStateOf(uiState.prefs.reportOrgName) }
    var orgAddress by rememberSaveable { mutableStateOf(uiState.prefs.reportOrgAddress) }
    var contactInfo by rememberSaveable { mutableStateOf(uiState.prefs.reportContactInfo) }
    var signatory by rememberSaveable { mutableStateOf(uiState.prefs.reportSignatory.ifBlank { uiState.prefs.displayName }) }
    var footerText by rememberSaveable { mutableStateOf(uiState.prefs.reportFooterText) }
    var customNotes by rememberSaveable {
        mutableStateOf("Seluruh angka dan rincian dalam dokumen ini bersumber dari kueri repositori terverifikasi di XynoosFinance.")
    }

    val scopeIncome = when (reportScope) {
        PdfReportScope.SELECTED_MONTH -> uiState.monthlyLedgerSummary.incomeIdr
        PdfReportScope.FULL_YEAR -> uiState.annualLedgerSummary.totalIncomeIdr
        PdfReportScope.DASHBOARD_PERIOD -> uiState.overview.periodIncomeIdr
    }
    val scopeExpense = when (reportScope) {
        PdfReportScope.SELECTED_MONTH -> uiState.monthlyLedgerSummary.netExpenseIdr
        PdfReportScope.FULL_YEAR -> uiState.annualLedgerSummary.totalNetExpenseIdr
        PdfReportScope.DASHBOARD_PERIOD -> uiState.overview.periodExpenseIdr
    }
    val scopeTxCount = when (reportScope) {
        PdfReportScope.SELECTED_MONTH -> uiState.monthlyLedgerSummary.transactionCount
        PdfReportScope.FULL_YEAR -> uiState.annualLedgerSummary.transactionCount
        PdfReportScope.DASHBOARD_PERIOD -> uiState.periodTransactions.size
    }
    val scopeCategories = when (reportScope) {
        PdfReportScope.SELECTED_MONTH -> uiState.monthlyLedgerSummary.expenseCategoryBreakdown
        PdfReportScope.FULL_YEAR -> uiState.annualLedgerSummary.expenseCategoryBreakdown
        PdfReportScope.DASHBOARD_PERIOD -> uiState.categoryBreakdown
    }
    val scopeLabel = when (reportScope) {
        PdfReportScope.SELECTED_MONTH -> uiState.monthlyLedgerSummary.monthLabel
        PdfReportScope.FULL_YEAR -> "Tahun ${uiState.analyticsYear}"
        PdfReportScope.DASHBOARD_PERIOD -> uiState.selectedPeriod.labelId
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp).testTag("pdf_export_subscreen")
    ) {
        SubScreenTopBar(title = "Ekspor Laporan Rinci & Status", onBack = onBack)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. NEW: Luxury PNG Status Image Card Exporter (For WhatsApp Status / IG Story)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("status_image_export_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1724)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "KARTU STATUS LAPORAN KEUANGAN RINCI (PNG HIGH-RES)",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ekspor Gambar Status Kekayaan, Grafik USD 1m, Rincian Kategori & Rekap Tahunan",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFF4F7FB),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Kekayaan Bersih: ${IdrFormatter.format(uiState.overview.netWorthIdr)} (≈ ${IdrFormatter.formatUsdFromIdr(uiState.overview.netWorthIdr, uiState.usdMarket.currentRateIdr)})\n" +
                                "• Rekap Tahun ${uiState.analyticsYear}: Masuk ${IdrFormatter.formatCompact(uiState.annualLedgerSummary.totalIncomeIdr)} | Abis ${IdrFormatter.formatCompact(uiState.annualLedgerSummary.totalNetExpenseIdr)}\n" +
                                "• Rincian Kategori & Saldo Rekening disertakan otomatis",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF98A3B3)
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onGenerateStatusImage(true) },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("generate_status_image_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SapphireSecondary,
                                    contentColor = Color(0xFF081018)
                                )
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Export & Share Gambar Status", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onSendWhatsAppNow,
                                modifier = Modifier.height(48.dp).testTag("send_whatsapp_report_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Kirim WA")
                            }
                        }
                        if (uiState.lastGeneratedStatusImage != null) {
                            Text(
                                text = "✅ Gambar Status siap: ${uiState.lastGeneratedStatusImage.file.name} (${uiState.lastGeneratedStatusImage.generatedAtFormatted})",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }

            // 2. Scope Selector for Detailed PDF
            item {
                Text("Pilih Cakupan Periode Laporan PDF Rinci", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PdfReportScope.entries.forEach { scope ->
                        val chipLabel = when (scope) {
                            PdfReportScope.SELECTED_MONTH -> "Bulan: ${uiState.monthlyLedgerSummary.monthLabel}"
                            PdfReportScope.FULL_YEAR -> "Tahunan: ${uiState.analyticsYear}"
                            PdfReportScope.DASHBOARD_PERIOD -> "Dashboard: ${uiState.selectedPeriod.labelId}"
                        }
                        FilterChip(
                            selected = reportScope == scope,
                            onClick = { reportScope = scope },
                            label = { Text(chipLabel) },
                            modifier = Modifier.testTag("pdf_scope_chip_${scope.name}")
                        )
                    }
                }
            }

            // 3. Detailed Live Document Preview Card (A4 PDF)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("pdf_preview_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "PRATINJAU LAPORAN RINCI A4 — ${orgName.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF0B8A56),
                            fontWeight = FontWeight.Bold
                        )
                        Text(reportTitle, style = MaterialTheme.typography.titleMedium, color = Color(0xFF0D141F), fontWeight = FontWeight.Bold)
                        Text("Cakupan: $scopeLabel • $orgAddress • $contactInfo", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4F5D73))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "• Kas Tersedia: ${IdrFormatter.format(uiState.overview.availableCashIdr)} | Net Worth: ${IdrFormatter.format(uiState.overview.netWorthIdr)}\n" +
                                "• Pemasukan ($scopeLabel): ${IdrFormatter.format(scopeIncome)}\n" +
                                "• Pengeluaran Bersih ($scopeLabel): ${IdrFormatter.format(scopeExpense)}\n" +
                                "• Rekap Tahun ${uiState.analyticsYear}: Masuk ${IdrFormatter.format(uiState.annualLedgerSummary.totalIncomeIdr)} | Abis ${IdrFormatter.format(uiState.annualLedgerSummary.totalNetExpenseIdr)}\n" +
                                "• Rincian Per Kategori (${scopeCategories.size} kategori): ${scopeCategories.take(3).joinToString { "${it.categoryName} (${String.format(Locale.US, "%.0f", it.percentage)}%)" }.ifBlank { "Belum ada" }}\n" +
                                "• Rincian Buku Besar: $scopeTxCount entri transaksi lengkap + tabel 12 bulan + mutasi ${uiState.accounts.size} akun + anggaran",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF0D141F)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Penandatangan: ${signatory.ifBlank { "Pemilik Akun" }}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4F5D73))
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            onUpdateReportIdentity(orgName, orgAddress, contactInfo, footerText, signatory)
                            onGeneratePdf(
                                PdfReportConfig(
                                    reportTitle = reportTitle,
                                    periodLabel = scopeLabel,
                                    scope = reportScope,
                                    customNotes = customNotes
                                )
                            )
                        },
                        modifier = Modifier.weight(1f).height(52.dp).testTag("generate_pdf_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Buat Laporan PDF Rinci", fontWeight = FontWeight.Bold)
                    }

                    if (uiState.lastGeneratedPdf != null) {
                        OutlinedButton(
                            onClick = {
                                PdfReportGenerator.sharePdfReport(context, uiState.lastGeneratedPdf.file)
                            },
                            modifier = Modifier.height(52.dp).testTag("share_pdf_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Bagikan PDF")
                            Spacer(Modifier.width(6.dp))
                            Text("Bagikan")
                        }
                    }
                }
            }

            if (uiState.lastGeneratedPdf != null) {
                val info = uiState.lastGeneratedPdf
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth().testTag("generated_pdf_status_card")
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("PDF Siap Dibagikan (${info.pageCount} Halaman A4)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("File: ${info.file.name}", style = MaterialTheme.typography.bodySmall)
                            Text("Dibuat pada: ${info.generatedAtFormatted}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            item {
                Text("Kustomisasi Identitas & Kop Laporan PDF", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                OutlinedTextField(
                    value = reportTitle,
                    onValueChange = { reportTitle = it },
                    label = { Text("Judul Laporan") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = orgName,
                    onValueChange = { orgName = it },
                    label = { Text("Nama Bisnis / Organisasi / Pribadi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = orgAddress,
                    onValueChange = { orgAddress = it },
                    label = { Text("Alamat Organisasi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = contactInfo,
                        onValueChange = { contactInfo = it },
                        label = { Text("Kontak / Email") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = signatory,
                        onValueChange = { signatory = it },
                        label = { Text("Nama Penandatangan") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = customNotes,
                    onValueChange = { customNotes = it },
                    label = { Text("Catatan Rekonsiliasi / Kaki Dokumen") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CategoriesAndRecurringSubScreen(
    uiState: XynoosUiState,
    onBack: () -> Unit,
    onSaveCategory: (String, TransactionType, String, String) -> Unit,
    onSaveRecurring: (String, TransactionType, Long, String, String, String, String, String) -> Unit,
    onDeleteRecurring: (String) -> Unit
) {
    var newCatName by remember { mutableStateOf("") }
    var newCatType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var recTitle by remember { mutableStateOf("") }
    var recAmountInput by remember { mutableStateOf("500000") }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp).testTag("categories_subscreen")
    ) {
        SubScreenTopBar(title = "Kategori & Jadwal Berulang", onBack = onBack)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tambah Kategori Kustom", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = newCatType == TransactionType.EXPENSE,
                                onClick = { newCatType = TransactionType.EXPENSE },
                                label = { Text("Pengeluaran") }
                            )
                            FilterChip(
                                selected = newCatType == TransactionType.INCOME,
                                onClick = { newCatType = TransactionType.INCOME },
                                label = { Text("Pemasukan") }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newCatName,
                                onValueChange = { newCatName = it },
                                label = { Text("Nama Kategori Baru") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(onClick = {
                                if (newCatName.isNotBlank()) {
                                    onSaveCategory(newCatName, newCatType, "category", "#49E0A0")
                                    newCatName = ""
                                }
                            }) {
                                Text("Tambah")
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tambah Jadwal Transaksi Berulang", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = recTitle,
                            onValueChange = { recTitle = it },
                            label = { Text("Nama Tagihan / Rutinitas") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        XynoosCurrencyNominalInput(
                            value = recAmountInput,
                            onValueChange = { recAmountInput = it },
                            label = "Nominal Tagihan Berulang (IDR)",
                            accentColor = EmeraldPrimary,
                            compactMode = true
                        )
                        Button(onClick = {
                            val amt = IdrFormatter.parseIdrInput(recAmountInput) ?: 0L
                            val firstAcc = uiState.accounts.firstOrNull()
                            if (recTitle.isNotBlank() && amt > 0L) {
                                onSaveRecurring(
                                    recTitle,
                                    TransactionType.EXPENSE,
                                    amt,
                                    "cat_utilities",
                                    "Tagihan & Utilitas",
                                    firstAcc?.id ?: "acc_default",
                                    firstAcc?.name ?: "Tunai Utama",
                                    "BULANAN"
                                )
                                recTitle = ""
                            }
                        }) {
                            Text("Simpan Jadwal Bulanan")
                        }
                    }
                }
            }

            if (uiState.recurringTransactions.isNotEmpty()) {
                item {
                    Text("Jadwal Berulang Aktif", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(uiState.recurringTransactions, key = { it.id }) { rec ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            Arrangement.SpaceBetween,
                            Alignment.CenterVertically
                        ) {
                            Column {
                                Text(rec.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("${rec.frequency} • ${rec.accountName}", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(IdrFormatter.format(rec.amountIdr), style = TabularAmountRow)
                                IconButton(onClick = { onDeleteRecurring(rec.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Daftar Kategori (${uiState.categories.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(uiState.categories, key = { it.id }) { cat ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text(cat.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(cat.type.labelId, style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsAndPrivacySubScreen(
    uiState: XynoosUiState,
    onBack: () -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onTogglePrivacyMode: () -> Unit,
    onUpdateReminders: (Boolean, Int, Int, Boolean, Boolean, Boolean) -> Unit,
    onUpdateAutoBackupEnabled: (Boolean) -> Unit,
    onUpdatePatternLock: (Boolean, String, Boolean) -> Unit,
    onCheckAppUpdate: () -> Unit,
    onUpdateGitHubRepo: (String) -> Unit,
    onUpdateWhatsAppSettings: (String, Boolean) -> Unit,
    onSendWhatsAppNow: (String?) -> Unit,
    onSyncCloudNow: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccountAndData: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showSetupPatternDialog by remember { mutableStateOf(false) }
    var repoSlugInput by rememberSaveable(uiState.effectiveGithubRepoSlug) {
        mutableStateOf(uiState.effectiveGithubRepoSlug)
    }
    var waPhoneInput by rememberSaveable(uiState.prefs.whatsAppPhone) { mutableStateOf(uiState.prefs.whatsAppPhone) }
    var waAutoNotify by rememberSaveable(uiState.prefs.whatsAppAutoNotifyOnTransaction) {
        mutableStateOf(uiState.prefs.whatsAppAutoNotifyOnTransaction)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            ReminderNotificationHelper.sendPreviewReminderNotification(
                context = context,
                title = "Pengingat XynoosFinance Aktif",
                availableCashIdr = uiState.overview.availableCashIdr,
                isPrivacyMode = uiState.prefs.isPrivacyModeEnabled
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp).testTag("settings_subscreen")
    ) {
        SubScreenTopBar(title = "Pengaturan, Keamanan & Cloud", onBack = onBack)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 0A. NEW: Pattern Lock Security ("Password Tipe Pola")
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("pattern_lock_settings_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Pattern, contentDescription = null, tint = EmeraldPrimary)
                            Text(
                                text = "Keamanan Password Tipe Pola",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                        Text(
                            text = if (uiState.prefs.patternLockEnabled) {
                                "Aktif: Aplikasi terkunci dengan pola 3x3 yang dienkripsi SHA-256 setiap kali dibuka."
                            } else {
                                "Lindungi catatan saldo dan transaksi Anda dengan kunci layar pola gambar 3x3."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Aktifkan Kunci Pola Aplikasi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Minta pola keamanan saat membuka XynoosFinance", style = MaterialTheme.typography.labelSmall)
                            }
                            Switch(
                                checked = uiState.prefs.patternLockEnabled,
                                onCheckedChange = { wantEnable ->
                                    if (wantEnable) {
                                        if (uiState.prefs.patternLockHash.isBlank()) {
                                            showSetupPatternDialog = true
                                        } else {
                                            onUpdatePatternLock(true, uiState.prefs.patternLockHash, uiState.prefs.patternLockOnResume)
                                        }
                                    } else {
                                        onUpdatePatternLock(false, uiState.prefs.patternLockHash, uiState.prefs.patternLockOnResume)
                                    }
                                },
                                modifier = Modifier.testTag("pattern_lock_switch")
                            )
                        }

                        if (uiState.prefs.patternLockEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Kunci Otomatis Saat Keluar Aplikasi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text("Minta pola kembali setelah berpindah aplikasi / standby", style = MaterialTheme.typography.labelSmall)
                                }
                                Switch(
                                    checked = uiState.prefs.patternLockOnResume,
                                    onCheckedChange = { resumeLock ->
                                        onUpdatePatternLock(true, uiState.prefs.patternLockHash, resumeLock)
                                    }
                                )
                            }
                        }

                        Button(
                            onClick = { showSetupPatternDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary.copy(alpha = 0.16f),
                                contentColor = EmeraldPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("setup_pattern_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (uiState.prefs.patternLockHash.isBlank()) "Buat Password Tipe Pola (3x3)" else "Ubah Pola Keamanan Sekarang",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 0B. NEW: Low-RAM 3-Minute Auto Cloud Backup Status & Control
            item {
                val backupStatus = uiState.autoBackupStatus
                val lastBackupLabel = remember(uiState.prefs.lastAutoBackupMillis) {
                    if (uiState.prefs.lastAutoBackupMillis > 0L) {
                        SimpleDateFormat("dd MMM HH:mm:ss", Locale("id", "ID")).format(Date(uiState.prefs.lastAutoBackupMillis))
                    } else {
                        "Belum pernah berjalan"
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("auto_backup_settings_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = if (backupStatus.isOnline) Icons.Default.CloudSync else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (backupStatus.isOnline) EmeraldPrimary else WarmAmber
                                )
                                Text(
                                    text = "Auto-Backup Cloud (Setiap 3 Menit)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "• Koneksi: ${if (backupStatus.isOnline) "Online Terhubung Internet" else "Offline (Mode Lokal)"}\n" +
                                "• Status Mesin: ${backupStatus.statusText}\n" +
                                "• Cadangan Otomatis Terakhir: $lastBackupLabel\n" +
                                "• Optimasi RAM: Kueri inkremental hanya mengirim baris baru/berubah ke akun Google Anda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-Backup Tiap 3 Menit Saat Online", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Otomatis backup ke Firestore tanpa menguras RAM", style = MaterialTheme.typography.labelSmall)
                            }
                            Switch(
                                checked = uiState.prefs.autoBackupEvery3MinEnabled,
                                onCheckedChange = { onUpdateAutoBackupEnabled(it) },
                                modifier = Modifier.testTag("auto_backup_3min_switch")
                            )
                        }

                        Button(
                            onClick = onSyncCloudNow,
                            enabled = !uiState.isSyncing,
                            modifier = Modifier.fillMaxWidth().testTag("settings_sync_now_button")
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (uiState.isSyncing) "Menyinkronkan..." else "Backup & Sinkronkan Cloud Sekarang")
                        }
                    }
                }
            }

            // 0C. NEW: Automatic GitHub Releases App Update & Mandatory Update Trigger
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("app_update_settings_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = EmeraldPrimary)
                            Text(
                                text = "Pembaruan Aplikasi (GitHub Releases)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                        Text(
                            text = "Versi Terpasang: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})\n" +
                                "Status: ${uiState.updateStatusText.ifBlank { "Siap memeriksa rilis terbaru GitHub" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = repoSlugInput,
                            onValueChange = { repoSlugInput = it },
                            label = { Text("Repositori GitHub (owner/nama-repo)") },
                            placeholder = { Text("contoh: xynoosdeveloper/XynoosFinance") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("github_repo_input")
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onUpdateGitHubRepo(repoSlugInput) },
                                modifier = Modifier.weight(1f).testTag("save_github_repo_button")
                            ) {
                                Text("Simpan Repo")
                            }
                            Button(
                                onClick = {
                                    if (repoSlugInput.trim() != uiState.effectiveGithubRepoSlug) {
                                        onUpdateGitHubRepo(repoSlugInput)
                                    } else {
                                        onCheckAppUpdate()
                                    }
                                },
                                enabled = !uiState.isCheckingUpdate,
                                modifier = Modifier.weight(1f).testTag("check_app_update_button")
                            ) {
                                Text(if (uiState.isCheckingUpdate) "Memeriksa..." else "Cek Update")
                            }
                        }
                    }
                }
            }

            // 1. Free Official XynoosFinance WhatsApp Notification
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("whatsapp_settings_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "💬 Notifikasi WhatsApp Gratis (XynoosFinance)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = "Kirim rekapitulasi saldo, kurs Live USD, dan waktu detail tahun transaksi langsung ke nomor WhatsApp Anda secara gratis atas nama XynoosFinance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = waPhoneInput,
                            onValueChange = { waPhoneInput = it },
                            label = { Text("Nomor WhatsApp Anda (Contoh: 081234567890)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth().testTag("wa_phone_input"),
                            singleLine = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Siapkan Notifikasi WA Setiap Transaksi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("Sertakan waktu detail & tahun pada pesan XynoosFinance", style = MaterialTheme.typography.labelSmall)
                            }
                            Switch(
                                checked = waAutoNotify,
                                onCheckedChange = {
                                    waAutoNotify = it
                                    onUpdateWhatsAppSettings(waPhoneInput, it)
                                }
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onUpdateWhatsAppSettings(waPhoneInput, waAutoNotify) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Simpan Nomor WA")
                            }
                            OutlinedButton(
                                onClick = {
                                    onUpdateWhatsAppSettings(waPhoneInput, waAutoNotify)
                                    onSendWhatsAppNow(waPhoneInput)
                                },
                                modifier = Modifier.weight(1f).testTag("send_wa_now_button")
                            ) {
                                Text("Kirim ke WA Sekarang")
                            }
                        }
                    }
                }
            }

            // 2. Theme System
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Tema Visual Aplikasi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Perubahan tema berlaku seketika tanpa memulai ulang aplikasi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = uiState.prefs.themeMode == mode,
                                    onClick = { onSetThemeMode(mode) },
                                    label = { Text(mode.labelId) },
                                    modifier = Modifier.testTag("theme_chip_${mode.name}")
                                )
                            }
                        }
                    }
                }
            }

            // 3. Smart Useful Notifications & Financial Privacy
            item {
                val reminderPresets = listOf(
                    Triple(8, 0, "08:00 Pagi"),
                    Triple(12, 30, "12:30 Siang"),
                    Triple(20, 0, "20:00 Malam"),
                    Triple(21, 30, "21:30 Rekap")
                )
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("notifications_settings_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Notifikasi Berguna & Privasi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Mode Privasi Nominal (Rp ••••••••)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Sembunyikan angka saldo di layar publik & isi notifikasi.", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = uiState.prefs.isPrivacyModeEnabled,
                                onCheckedChange = { onTogglePrivacyMode() },
                                modifier = Modifier.testTag("settings_privacy_switch")
                            )
                        }

                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Pengingat Harian Terjadwal (${String.format(Locale.US, "%02d:%02d", uiState.prefs.reminderHour, uiState.prefs.reminderMinute)} WIB)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Notifikasi otomatis setiap hari menghormati jam tenang.", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = uiState.prefs.remindersEnabled,
                                onCheckedChange = { enabled ->
                                    onUpdateReminders(
                                        enabled,
                                        uiState.prefs.reminderHour,
                                        uiState.prefs.reminderMinute,
                                        uiState.prefs.quietHoursEnabled,
                                        uiState.prefs.budgetAlertsEnabled,
                                        uiState.prefs.activityNotificationsEnabled
                                    )
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            )
                        }

                        if (uiState.prefs.remindersEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                reminderPresets.forEach { (hr, min, label) ->
                                    FilterChip(
                                        selected = uiState.prefs.reminderHour == hr && uiState.prefs.reminderMinute == min,
                                        onClick = {
                                            onUpdateReminders(
                                                true,
                                                hr,
                                                min,
                                                uiState.prefs.quietHoursEnabled,
                                                uiState.prefs.budgetAlertsEnabled,
                                                uiState.prefs.activityNotificationsEnabled
                                            )
                                        },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }

                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Peringatan Otomatis Batas Anggaran", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Beri notif real-time jika pos belanja mencapai ≥70%, ≥90%, atau melebihi batas.", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = uiState.prefs.budgetAlertsEnabled,
                                onCheckedChange = { alertOn ->
                                    onUpdateReminders(
                                        uiState.prefs.remindersEnabled,
                                        uiState.prefs.reminderHour,
                                        uiState.prefs.reminderMinute,
                                        uiState.prefs.quietHoursEnabled,
                                        alertOn,
                                        uiState.prefs.activityNotificationsEnabled
                                    )
                                }
                            )
                        }

                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Notif Konfirmasi Setiap Transaksi Baru", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Tampilkan bukti notifikasi instan tiap selesai mencatat transaksi.", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = uiState.prefs.activityNotificationsEnabled,
                                onCheckedChange = { actOn ->
                                    onUpdateReminders(
                                        uiState.prefs.remindersEnabled,
                                        uiState.prefs.reminderHour,
                                        uiState.prefs.reminderMinute,
                                        uiState.prefs.quietHoursEnabled,
                                        uiState.prefs.budgetAlertsEnabled,
                                        actOn
                                    )
                                }
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !ReminderNotificationHelper.hasNotificationPermission(context)
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    ReminderNotificationHelper.sendPreviewReminderNotification(
                                        context = context,
                                        title = "Notifikasi XynoosFinance Aktif!",
                                        availableCashIdr = uiState.overview.availableCashIdr,
                                        isPrivacyMode = uiState.prefs.isPrivacyModeEnabled
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Kirim & Uji Notifikasi Sekarang")
                        }
                    }
                }
            }

            // 4. Sign Out & Account Deletion
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Keamanan Akun & Manajemen Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Isolasi Data: Room SQLite (offline) + Cloud Firestore terpisah per UID Google Anda.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Keluar dari Akun Google (Logout)")
                        }
                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RefinedRed,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("delete_account_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Hapus Seluruh Data & Akun Secara Permanen")
                        }
                        Text(
                            text = "XynoosFinance • Built by Xynoos Technology",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showSetupPatternDialog) {
        SetupPatternLockDialog(
            isChangingExisting = uiState.prefs.patternLockHash.isNotBlank(),
            onDismiss = { showSetupPatternDialog = false },
            onPatternSaved = { hash ->
                onUpdatePatternLock(true, hash, uiState.prefs.patternLockOnResume)
                showSetupPatternDialog = false
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Hapus Seluruh Data & Akun?") },
            text = {
                Text("Tindakan ini akan menghapus seluruh transaksi, rekening, anggaran, target tabungan di database lokal (Room) dan Cloud Firestore secara permanen, lalu mengeluarkan akun Anda.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAccountAndData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RefinedRed)
                ) {
                    Text("Ya, Hapus Permanen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

/**
 * Dedicated Professional Finance Terminal Page for Real-Time 1-Minute USD/IDR Candlestick Chart & Portfolio Valuation.
 */
@Composable
fun UsdLiveMarketSubScreen(
    uiState: XynoosUiState,
    onBack: () -> Unit,
    onRefreshNow: () -> Unit
) {
    val usd = uiState.usdMarket
    val isPrivacy = uiState.prefs.isPrivacyModeEnabled
    var converterIdrInput by rememberSaveable { mutableStateOf("10000000") }
    val parsedConverterIdr = IdrFormatter.parseIdrInput(converterIdrInput) ?: 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag("usd_live_market_subscreen")
    ) {
        SubScreenTopBar(title = "Terminal Pasar & Grafik USD/IDR (1m)", onBack = onBack)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. Full Professional Stock/Forex Candlestick Terminal
            item {
                StockMarketCandlestickChart(
                    usdMarket = usd,
                    availableCashIdr = uiState.overview.availableCashIdr,
                    netWorthIdr = uiState.overview.netWorthIdr,
                    isPrivacyHidden = isPrivacy,
                    chartHeight = 250.dp,
                    onRefreshNow = onRefreshNow
                )
            }

            // 2. Market Telemetry & Auto-Update Status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Telemetri Kurs Pasar Real-Time (Auto-Refresh 1 Menit)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = "Sumber Data: ${usd.sourceLabel}\n" +
                                "Pembaruan Terakhir: ${usd.lastUpdatedFormatted.ifBlank { "Baru saja" }}\n" +
                                "Hitung Mundur Siklus 1 Menit Berikutnya: ${usd.nextUpdateSecondsRemaining} detik",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Open Harian", style = MaterialTheme.typography.labelSmall)
                                Text(IdrFormatter.format(usd.dayOpenIdr.toLong()), style = TabularAmountRow)
                            }
                            Column {
                                Text("Tertinggi (High)", style = MaterialTheme.typography.labelSmall)
                                Text(IdrFormatter.format(usd.dayHighIdr.toLong()), style = TabularAmountRow, color = EmeraldPrimary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Terendah (Low)", style = MaterialTheme.typography.labelSmall)
                                Text(IdrFormatter.format(usd.dayLowIdr.toLong()), style = TabularAmountRow, color = RefinedRed)
                            }
                        }
                    }
                }
            }

            // 3. Portfolio & Savings Valuation in Real USD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Valuasi Saldo & Tabungan Anda dalam Dollar (Real USD)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Total Kas Likuid", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "${IdrFormatter.format(uiState.overview.availableCashIdr, isPrivacy)} (≈ ${IdrFormatter.formatUsdFromIdr(uiState.overview.availableCashIdr, usd.currentRateIdr, isPrivacy)})",
                                style = TabularAmountRow,
                                color = EmeraldPrimary
                            )
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Kekayaan Bersih (Net Worth)", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "${IdrFormatter.format(uiState.overview.netWorthIdr, isPrivacy)} (≈ ${IdrFormatter.formatUsdFromIdr(uiState.overview.netWorthIdr, usd.currentRateIdr, isPrivacy)})",
                                style = TabularAmountRow,
                                color = SapphireSecondary
                            )
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Target Tabungan (Terkoneksi Saldo)", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "${IdrFormatter.format(uiState.overview.totalGoalsSavedIdr, isPrivacy)} (≈ ${IdrFormatter.formatUsdFromIdr(uiState.overview.totalGoalsSavedIdr, usd.currentRateIdr, isPrivacy)})",
                                style = TabularAmountRow,
                                color = WarmAmber
                            )
                        }
                        uiState.accounts.forEach { acc ->
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("• ${acc.name} (${acc.accountType.labelId})", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "${IdrFormatter.format(acc.currentBalanceIdr, isPrivacy)} → ${IdrFormatter.formatUsdFromIdr(acc.currentBalanceIdr, usd.currentRateIdr, isPrivacy)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 4. Interactive Real-Time IDR <-> USD Converter
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Kalkulator Konversi Instan IDR ⇄ Real USD", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        XynoosCurrencyNominalInput(
                            value = converterIdrInput,
                            onValueChange = { converterIdrInput = it },
                            label = "Nominal Rupiah untuk Dikonversi ke USD",
                            inputTestTag = "usd_converter_input",
                            accentColor = EmeraldPrimary,
                            usdRateIdr = usd.currentRateIdr
                        )
                        Text(
                            text = "Setara Nilai Pasar: ${IdrFormatter.formatUsdFromIdr(parsedConverterIdr, usd.currentRateIdr)}",
                            style = TabularBalanceMedium,
                            color = EmeraldPrimary
                        )
                    }
                }
            }

            // 5. Recent 1-Minute Candlestick Log with Full Year Timestamp
            item {
                Text(
                    text = "Log Pergerakan Candlestick Per Menit (Detail Waktu & Tahun)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            val recentCandles = usd.candles.takeLast(8).reversed()
            items(recentCandles.indices.toList(), key = { idx -> "${recentCandles[idx].timestampMillis}_$idx" }) { idx ->
                val tick = recentCandles[idx]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(tick.fullDateYearLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "O: ${IdrFormatter.format(tick.openIdr.toLong())} • H: ${IdrFormatter.format(tick.highIdr.toLong())} • L: ${IdrFormatter.format(tick.lowIdr.toLong())}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = IdrFormatter.format(tick.closeIdr.toLong()),
                            style = TabularAmountRow,
                            color = if (tick.isBullish) EmeraldPrimary else RefinedRed
                        )
                    }
                }
            }
        }
    }
}

