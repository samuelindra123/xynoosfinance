package com.example.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.remote.LiveUsdMarketState
import com.example.domain.AnnualLedgerSummary
import com.example.domain.BudgetProgressSummary
import com.example.domain.CategoryBreakdownItem
import com.example.domain.FinancialAccountModel
import com.example.domain.FinancialOverviewSummary
import com.example.domain.IdrFormatter
import com.example.domain.TransactionModel
import com.example.domain.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Provides 100% free WhatsApp notification & detailed financial summary dispatch directly to
 * the user's phone number with official XynoosFinance branding.
 */
object WhatsAppNotifierHelper {

    fun normalizeIndonesianPhone(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        return when {
            digits.startsWith("08") -> "62" + digits.drop(1)
            digits.startsWith("8") -> "62$digits"
            digits.startsWith("62") -> digits
            else -> digits
        }
    }

    fun buildOfficialWhatsAppMessage(
        displayName: String,
        overview: FinancialOverviewSummary,
        usdMarket: LiveUsdMarketState,
        latestTransaction: TransactionModel? = null,
        isPrivacyMode: Boolean = false,
        periodLabel: String = "Bulan Ini",
        accounts: List<FinancialAccountModel> = emptyList(),
        categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
        budgetSummaries: List<BudgetProgressSummary> = emptyList(),
        annualSummary: AnnualLedgerSummary? = null,
        recentTransactions: List<TransactionModel> = emptyList()
    ): String {
        val fullDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy • HH:mm:ss 'WIB' ('Tahun' yyyy)", Locale("id", "ID"))
        val shortTxDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
        val nowFormatted = fullDateFormat.format(Date())
        val cashIdr = IdrFormatter.format(overview.availableCashIdr, isPrivacyMode)
        val cashUsd = IdrFormatter.formatUsdFromIdr(overview.availableCashIdr, usdMarket.currentRateIdr, isPrivacyMode)
        val netWorthIdr = IdrFormatter.format(overview.netWorthIdr, isPrivacyMode)
        val netWorthUsd = IdrFormatter.formatUsdFromIdr(overview.netWorthIdr, usdMarket.currentRateIdr, isPrivacyMode)
        val usdRateStr = IdrFormatter.format(usdMarket.currentRateIdr.toLong())

        return buildString {
            appendLine("*XYNOOSFINANCE — LAPORAN KEUANGAN RINCI*")
            appendLine("📅 *Waktu Cetak:* $nowFormatted")
            appendLine("👤 *Pemilik Akun:* ${displayName.ifBlank { "Eksekutif Xynoos" }}")
            appendLine("────────────────────────")

            if (latestTransaction != null) {
                val txDate = fullDateFormat.format(Date(latestTransaction.transactionDateMillis))
                appendLine("🔔 *Transaksi Terbaru Tercatat:*")
                appendLine("• Deskripsi: *${latestTransaction.title}*")
                appendLine("• Tipe: ${latestTransaction.type.labelId} (${latestTransaction.categoryName})")
                appendLine("• Nominal: *${IdrFormatter.format(latestTransaction.amountIdr, isPrivacyMode)}*")
                appendLine("• Rekening: ${latestTransaction.accountName}")
                appendLine("• Waktu: $txDate")
                appendLine("────────────────────────")
            }

            appendLine("💰 *Total Kas Tersedia:* $cashIdr _(≈ $cashUsd)_")
            appendLine("📈 *Kekayaan Bersih:* $netWorthIdr _(≈ $netWorthUsd)_")
            appendLine("💵 *Kurs Live USD/IDR (1m):* $usdRateStr / $1 USD")
            appendLine("────────────────────────")

            appendLine("📊 *Rincian Arus Kas ($periodLabel):*")
            appendLine("• Pemasukan: ${IdrFormatter.format(overview.periodIncomeIdr, isPrivacyMode, showPlusSign = true)}")
            appendLine("• Pengeluaran Bersih: ${IdrFormatter.format(overview.periodExpenseIdr, isPrivacyMode)}")
            appendLine("• Arus Kas Bersih: ${IdrFormatter.format(overview.netCashFlowIdr, isPrivacyMode, showPlusSign = true)}")

            if (annualSummary != null) {
                appendLine("────────────────────────")
                appendLine("📆 *Rekap Tahunan (${annualSummary.year} — ${annualSummary.transactionCount} Transaksi):*")
                appendLine("• Total Masuk Tahun ${annualSummary.year}: ${IdrFormatter.format(annualSummary.totalIncomeIdr, isPrivacyMode, showPlusSign = true)}")
                appendLine("• Total Keluar/Abis Tahun ${annualSummary.year}: ${IdrFormatter.format(annualSummary.totalNetExpenseIdr, isPrivacyMode)}")
                appendLine("• Surplus/Defisit Tahunan: ${IdrFormatter.format(annualSummary.netCashFlowIdr, isPrivacyMode, showPlusSign = true)}")
            }

            if (accounts.isNotEmpty()) {
                appendLine("────────────────────────")
                appendLine("🏦 *Rincian Saldo Rekening (${accounts.size} Akun):*")
                accounts.take(5).forEach { acc ->
                    appendLine("• ${acc.name} (${acc.accountType.labelId}): ${IdrFormatter.format(acc.currentBalanceIdr, isPrivacyMode)}")
                }
            }

            if (categoryBreakdown.isNotEmpty()) {
                appendLine("────────────────────────")
                appendLine("🏷️ *Rincian Pengeluaran Terbesar per Kategori:*")
                categoryBreakdown.take(5).forEach { cat ->
                    appendLine("• ${cat.categoryName}: ${IdrFormatter.format(cat.amountIdr, isPrivacyMode)} (${String.format(Locale.US, "%.1f", cat.percentage)}% • ${cat.transactionCount}x)")
                }
            }

            if (budgetSummaries.isNotEmpty()) {
                appendLine("────────────────────────")
                appendLine("🛡️ *Status Anggaran Berjalan:*")
                budgetSummaries.take(4).forEach { b ->
                    appendLine("• ${b.budget.name}: ${IdrFormatter.format(b.spentAmountIdr, isPrivacyMode)} / ${IdrFormatter.format(b.budget.limitAmountIdr, isPrivacyMode)} (${b.utilizationPercent}%)")
                }
            }

            val ledgerPreview = recentTransactions.take(5)
            if (ledgerPreview.isNotEmpty()) {
                appendLine("────────────────────────")
                appendLine("🧾 *Rincian ${ledgerPreview.size} Transaksi Terbaru:*")
                ledgerPreview.forEach { tx ->
                    val sign = when (tx.type) {
                        TransactionType.INCOME, TransactionType.REFUND -> "+"
                        TransactionType.EXPENSE -> "-"
                        else -> ""
                    }
                    appendLine("• [${shortTxDateFormat.format(Date(tx.transactionDateMillis))}] ${tx.title} (${tx.categoryName}): $sign${IdrFormatter.format(tx.amountIdr, isPrivacyMode)}")
                }
            }

            appendLine("────────────────────────")
            appendLine("🎯 *Progres Target Tabungan (Auto-Saldo):* ${overview.savingsProgressPercent}%")
            append("_Laporan rinci otomatis gratis dari aplikasi XynoosFinance by Xynoos Technology_")
        }
    }

    fun sendWhatsAppNotificationToUser(
        context: Context,
        phoneNumber: String,
        message: String
    ): Result<Unit> {
        return try {
            val normalized = normalizeIndonesianPhone(phoneNumber)
            val encodedText = Uri.encode(message)
            val url = if (normalized.length >= 9) {
                "https://api.whatsapp.com/send?phone=$normalized&text=$encodedText"
            } else {
                "https://api.whatsapp.com/send?text=$encodedText"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
