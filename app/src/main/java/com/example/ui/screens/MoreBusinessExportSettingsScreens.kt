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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
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
import com.example.domain.AccountType
import com.example.domain.AppThemeMode
import com.example.domain.IdrFormatter
import com.example.domain.InvoiceReferenceModel
import com.example.domain.InvoiceStatus
import com.example.domain.InvoiceType
import com.example.domain.TransactionType
import com.example.export.PdfReportConfig
import com.example.export.PdfReportGenerator
import com.example.export.StatusImageExporter
import com.example.notifications.ReminderNotificationHelper
import com.example.notifications.WhatsAppNotifierHelper
import com.example.ui.components.StockMarketCandlestickChart
import com.example.ui.components.XynoosCurrencyNominalInput
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RefinedRed
import com.example.ui.theme.SapphireSecondary
import com.example.ui.theme.TabularAmountRow
import com.example.ui.theme.TabularBalanceMedium
import com.example.ui.theme.WarmAmber
import com.example.ui.viewmodel.XynoosUiState

enum class MoreSectionDestination {
    HUB,
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
    onUpdateReminders: (Boolean, Int, Int, Boolean) -> Unit,
    onSyncCloudNow: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccountAndData: () -> Unit
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
    var reportTitle by rememberSaveable { mutableStateOf("Laporan Eksekutif Keuangan & Buku Besar") }
    var orgName by rememberSaveable { mutableStateOf(uiState.prefs.reportOrgName) }
    var orgAddress by rememberSaveable { mutableStateOf(uiState.prefs.reportOrgAddress) }
    var contactInfo by rememberSaveable { mutableStateOf(uiState.prefs.reportContactInfo) }
    var signatory by rememberSaveable { mutableStateOf(uiState.prefs.reportSignatory.ifBlank { uiState.prefs.displayName }) }
    var footerText by rememberSaveable { mutableStateOf(uiState.prefs.reportFooterText) }
    var customNotes by rememberSaveable {
        mutableStateOf("Seluruh angka dalam dokumen ini bersumber dari kueri repositori terverifikasi di XynoosFinance.")
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp).testTag("pdf_export_subscreen")
    ) {
        SubScreenTopBar(title = "Ekspor Gambar Status & PDF", onBack = onBack)

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
                            text = "KARTU STATUS LAPORAN KEUANGAN KEREN (PNG HIGH-RES)",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ekspor Gambar Status Kekayaan, Grafik Saham USD 1m & Target Tabungan",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFF4F7FB),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Total Kekayaan: ${IdrFormatter.format(uiState.overview.netWorthIdr)} (≈ ${IdrFormatter.formatUsdFromIdr(uiState.overview.netWorthIdr, uiState.usdMarket.currentRateIdr)})\n" +
                                "• Kas Tersedia: ${IdrFormatter.format(uiState.overview.availableCashIdr)} (≈ ${IdrFormatter.formatUsdFromIdr(uiState.overview.availableCashIdr, uiState.usdMarket.currentRateIdr)})\n" +
                                "• Target Tabungan Auto-Saldo: ${uiState.overview.savingsProgressPercent}%",
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

            // 2. Live Print-Friendly Document Preview Card (A4 PDF)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("pdf_preview_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "PRATINJAU DOKUMEN CETAK A4 — ${orgName.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF0B8A56),
                            fontWeight = FontWeight.Bold
                        )
                        Text(reportTitle, style = MaterialTheme.typography.titleMedium, color = Color(0xFF0D141F), fontWeight = FontWeight.Bold)
                        Text("$orgAddress • $contactInfo", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4F5D73))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "• Kas Tersedia: ${IdrFormatter.format(uiState.overview.availableCashIdr)}\n" +
                                "• Kekayaan Bersih: ${IdrFormatter.format(uiState.overview.netWorthIdr)}\n" +
                                "• Pemasukan (${uiState.selectedPeriod.labelId}): ${IdrFormatter.format(uiState.overview.periodIncomeIdr)}\n" +
                                "• Pengeluaran (${uiState.selectedPeriod.labelId}): ${IdrFormatter.format(uiState.overview.periodExpenseIdr)}\n" +
                                "• Jumlah Entri Buku Besar: ${uiState.periodTransactions.size} transaksi",
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
                                    periodLabel = uiState.selectedPeriod.labelId,
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
                        Text("Buat Laporan PDF", fontWeight = FontWeight.Bold)
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
    onUpdateReminders: (Boolean, Int, Int, Boolean) -> Unit,
    onUpdateWhatsAppSettings: (String, Boolean) -> Unit,
    onSendWhatsAppNow: (String?) -> Unit,
    onSyncCloudNow: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccountAndData: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
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
        SubScreenTopBar(title = "Pengaturan, WA Notif & Privasi", onBack = onBack)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 0. NEW: Free Official XynoosFinance WhatsApp Notification
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

            // 1. Theme System
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

            // 2. Financial Privacy & Reminders
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Mode Privasi Nominal (Rp ••••••••)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Sembunyikan angka saldo di layar publik & pratinjau notifikasi.", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = uiState.prefs.isPrivacyModeEnabled,
                                onCheckedChange = { onTogglePrivacyMode() },
                                modifier = Modifier.testTag("settings_privacy_switch")
                            )
                        }

                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Pengingat Pencatatan Harian (20:00 WIB)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Notifikasi lokal yang menghormati jam tenang & mode privasi.", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = uiState.prefs.remindersEnabled,
                                onCheckedChange = { enabled ->
                                    onUpdateReminders(enabled, 20, 0, uiState.prefs.quietHoursEnabled)
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
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
                                        title = "Rekap Keuangan Malam XynoosFinance",
                                        availableCashIdr = uiState.overview.availableCashIdr,
                                        isPrivacyMode = uiState.prefs.isPrivacyModeEnabled
                                    )
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Uji Notifikasi Pengingat Sekarang")
                        }
                    }
                }
            }

            // 3. Cloud Synchronization Status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Sinkronisasi Cloud Firestore & Penyimpanan Lokal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Status: ${uiState.lastSyncMessage}", style = MaterialTheme.typography.bodySmall, color = EmeraldPrimary)
                        Text(
                            "• Penyimpanan Utama: Room SQLite (tersedia 100% secara offline)\n" +
                                "• Sinkronisasi Cloud: Firestore Enterprise terisolasi per UID Google\n" +
                                "• Pemrosesan AI: Hanya ringkasan agregat yang dikirim ke Gemini 3.5 Flash-Lite",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onSyncCloudNow,
                            enabled = !uiState.isSyncing,
                            modifier = Modifier.testTag("settings_sync_now_button")
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (uiState.isSyncing) "Menyinkronkan..." else "Sinkronkan ke Cloud Sekarang")
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
                            text = "XynoosFinance v1.0.0 • Native Android Kotlin + Jetpack Compose",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
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

            items(usd.candles.takeLast(8).reversed(), key = { it.timestampMillis }) { tick ->
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

