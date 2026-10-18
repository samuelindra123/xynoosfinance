package com.example.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.remote.LiveUsdMarketState
import com.example.domain.FinancialOverviewSummary
import com.example.domain.IdrFormatter
import com.example.domain.TransactionModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Provides 100% free WhatsApp notification & financial summary dispatch directly to
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
        isPrivacyMode: Boolean = false
    ): String {
        val fullDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy • HH:mm:ss 'WIB' ('Tahun' yyyy)", Locale("id", "ID"))
        val nowFormatted = fullDateFormat.format(Date())
        val cashIdr = IdrFormatter.format(overview.availableCashIdr, isPrivacyMode)
        val cashUsd = IdrFormatter.formatUsdFromIdr(overview.availableCashIdr, usdMarket.currentRateIdr, isPrivacyMode)
        val netWorthIdr = IdrFormatter.format(overview.netWorthIdr, isPrivacyMode)
        val netWorthUsd = IdrFormatter.formatUsdFromIdr(overview.netWorthIdr, usdMarket.currentRateIdr, isPrivacyMode)
        val usdRateStr = IdrFormatter.format(usdMarket.currentRateIdr.toLong())

        return buildString {
            appendLine("*XYNOOSFINANCE — NOTIFIKASI KEUANGAN RESMI*")
            appendLine("📅 *Waktu:* $nowFormatted")
            appendLine("👤 *Pemilik Akun:* ${displayName.ifBlank { "Eksekutif Xynoos" }}")
            appendLine("────────────────────────")
            if (latestTransaction != null) {
                val txDate = fullDateFormat.format(Date(latestTransaction.transactionDateMillis))
                appendLine("🔔 *Transaksi Terbaru Tercatat:*")
                appendLine("• Deskripsi: *${latestTransaction.title}*")
                appendLine("• Tipe: ${latestTransaction.type.labelId} (${latestTransaction.categoryName})")
                appendLine("• Nominal: *${IdrFormatter.format(latestTransaction.amountIdr, isPrivacyMode)}*")
                appendLine("• Rekening: ${latestTransaction.accountName}")
                appendLine("• Waktu Transaksi: $txDate")
                appendLine("────────────────────────")
            }
            appendLine("💰 *Total Kas Tersedia:* $cashIdr _(≈ $cashUsd)_")
            appendLine("📈 *Kekayaan Bersih:* $netWorthIdr _(≈ $netWorthUsd)_")
            appendLine("💵 *Kurs Live USD/IDR (1m):* $usdRateStr / $1 USD")
            appendLine("📊 *Arus Kas Periode Ini:*")
            appendLine("• Pemasukan: ${IdrFormatter.format(overview.periodIncomeIdr, isPrivacyMode)}")
            appendLine("• Pengeluaran: ${IdrFormatter.format(overview.periodExpenseIdr, isPrivacyMode)}")
            appendLine("🎯 *Progres Target Tabungan (Otomatis):* ${overview.savingsProgressPercent}%")
            appendLine("────────────────────────")
            append("_Pesan otomatis gratis dari aplikasi XynoosFinance Android_")
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
