package com.example.domain

enum class AppThemeMode(val labelId: String) {
    DARK("Mode Gelap (Obsidian)"),
    LIGHT("Mode Terang"),
    SYSTEM("Ikuti Sistem")
}

enum class FinancialProfileType(val labelId: String, val descriptionId: String) {
    PERSONAL("Pribadi", "Kelola gaji, pengeluaran harian, dan tabungan pribadi"),
    FREELANCE("Freelancer", "Pisahkan pendapatan proyek klien dan biaya operasional mandiri"),
    BUSINESS("Bisnis / UMKM", "Pantau arus kas usaha, piutang, utang, dan laporan laba rugi"),
    COMBINED("Kombinasi (Pribadi & Bisnis)", "Kendali menyeluruh atas aset pribadi dan usaha dalam satu tempat")
}

enum class AccountType(val labelId: String, val isLiability: Boolean = false) {
    CASH("Tunai (Cash)", false),
    BANK("Rekening Bank", false),
    EWALLET("E-Wallet", false),
    SAVINGS("Tabungan & Deposito", false),
    BUSINESS("Rekening Bisnis", false),
    ASSET("Aset Lainnya", false),
    LIABILITY("Kewajiban / Pinjaman", true)
}

enum class TransactionType(val labelId: String) {
    INCOME("Pemasukan"),
    EXPENSE("Pengeluaran"),
    TRANSFER("Transfer"),
    REFUND("Refund / Pengembalian"),
    ADJUSTMENT("Penyesuaian Saldo")
}

enum class GoalPriority(val labelId: String) {
    HIGH("Prioritas Tinggi"),
    MEDIUM("Prioritas Sedang"),
    LOW("Prioritas Rendah")
}

enum class AllocationRuleType(val labelId: String) {
    MANUAL("Kontribusi Manual"),
    FIXED_PER_INCOME("Nominal Tetap per Pemasukan"),
    PERCENT_OF_INCOME("Persentase dari Pemasukan (%)")
}

enum class ContributionType(val labelId: String) {
    DEPOSIT("Setoran Tabungan"),
    WITHDRAWAL("Penarikan Dana"),
    REVERSAL("Koreksi / Reversal")
}

enum class InvoiceType(val labelId: String) {
    RECEIVABLE("Piutang (Tagihan ke Klien)"),
    PAYABLE("Utang (Tagihan Vendor)")
}

enum class InvoiceStatus(val labelId: String) {
    UNPAID("Belum Dibayar"),
    PARTIAL("Dibayar Sebagian"),
    PAID("Lunas"),
    CANCELLED("Dibatalkan")
}

enum class DashboardPeriod(val labelId: String) {
    TODAY("Hari Ini"),
    WEEK("Minggu Ini"),
    MONTH("Bulan Ini"),
    YEAR("Tahun Ini"),
    ALL("Semua Waktu")
}

enum class SyncStatus(val labelId: String) {
    LOCAL_ONLY("Tersimpan Lokal"),
    PENDING_UPLOAD("Menunggu Sinkronisasi"),
    SYNCED("Tersinkronisasi Cloud"),
    SYNC_ERROR("Gagal Sinkronisasi")
}

data class FinancialAccountModel(
    val id: String,
    val userId: String,
    val name: String,
    val accountType: AccountType,
    val openingBalanceIdr: Long,
    val currentBalanceIdr: Long,
    val colorHex: String,
    val institutionName: String = "",
    val isArchived: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    val version: Int = 1,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class TransactionCategoryModel(
    val id: String,
    val userId: String,
    val name: String,
    val type: TransactionType, // INCOME or EXPENSE
    val iconKey: String,
    val colorHex: String,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val version: Int = 1
)

data class TransactionModel(
    val id: String,
    val userId: String,
    val type: TransactionType,
    val amountIdr: Long,
    val currencyCode: String = "IDR",
    val title: String,
    val categoryId: String,
    val categoryName: String,
    val accountId: String,
    val accountName: String,
    val toAccountId: String? = null,
    val toAccountName: String? = null,
    val transferGroupId: String? = null,
    val transactionDateMillis: Long,
    val notes: String = "",
    val merchantName: String = "",
    val projectCostCenterId: String = "",
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    val version: Int = 1,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class BudgetModel(
    val id: String,
    val userId: String,
    val name: String,
    val categoryId: String, // "ALL" for total monthly budget or specific categoryId
    val categoryName: String,
    val limitAmountIdr: Long,
    val periodYearMonth: String, // e.g., "2026-10"
    val carryForward: Boolean = false,
    val alertThresholdPercent: Int = 80,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    val version: Int = 1,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class BudgetProgressSummary(
    val budget: BudgetModel,
    val spentAmountIdr: Long,
    val remainingAmountIdr: Long,
    val utilizationPercent: Int,
    val statusLevel: BudgetAlertLevel
)

enum class BudgetAlertLevel {
    SAFE,       // < 70%
    WARNING_70, // 70% - 89%
    WARNING_90, // 90% - 99%
    EXCEEDED    // >= 100%
}

data class FinancialGoalModel(
    val id: String,
    val userId: String,
    val name: String,
    val targetAmountIdr: Long,
    val currentSavedIdr: Long,
    val targetDateMillis: Long,
    val priority: GoalPriority,
    val iconKey: String,
    val allocationRuleType: AllocationRuleType = AllocationRuleType.MANUAL,
    val allocationValue: Long = 0L,
    val linkedAccountId: String? = null,
    val isCompleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    val version: Int = 1,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class GoalContributionModel(
    val id: String,
    val userId: String,
    val goalId: String,
    val amountIdr: Long,
    val contributionType: ContributionType,
    val notes: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
)

data class RecurringTransactionModel(
    val id: String,
    val userId: String,
    val title: String,
    val type: TransactionType,
    val amountIdr: Long,
    val categoryId: String,
    val categoryName: String,
    val accountId: String,
    val accountName: String,
    val frequency: String, // MONTHLY, WEEKLY, DAILY
    val nextExecutionMillis: Long,
    val isActive: Boolean = true
)

data class InvoiceReferenceModel(
    val id: String,
    val userId: String,
    val invoiceNumber: String,
    val counterpartyName: String,
    val invoiceType: InvoiceType,
    val amountIdr: Long,
    val dueDateMillis: Long,
    val status: InvoiceStatus,
    val projectCostCenter: String = "",
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    val version: Int = 1,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class AiMessageModel(
    val id: String,
    val userId: String,
    val role: String, // "user" or "assistant"
    val content: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val proposedDraft: ProposedTransactionDraft? = null
)

data class ProposedTransactionDraft(
    val type: TransactionType,
    val amountIdr: Long,
    val title: String,
    val categoryName: String,
    val accountName: String,
    val notes: String = "",
    val dateMillis: Long = System.currentTimeMillis()
)

data class FinancialOverviewSummary(
    val availableCashIdr: Long,
    val totalAssetsIdr: Long,
    val totalLiabilitiesIdr: Long,
    val netWorthIdr: Long,
    val periodIncomeIdr: Long,
    val periodExpenseIdr: Long,
    val netCashFlowIdr: Long,
    val totalBudgetLimitIdr: Long,
    val totalBudgetSpentIdr: Long,
    val budgetUtilizationPercent: Int,
    val totalGoalsTargetIdr: Long,
    val totalGoalsSavedIdr: Long,
    val savingsProgressPercent: Int
)

data class CategoryBreakdownItem(
    val categoryId: String,
    val categoryName: String,
    val amountIdr: Long,
    val percentage: Float,
    val colorHex: String,
    val transactionCount: Int
)

data class BusinessProfitLossSummary(
    val realizedRevenueIdr: Long,
    val operatingExpensesIdr: Long,
    val taxExpensesIdr: Long,
    val netProfitIdr: Long,
    val unpaidReceivablesIdr: Long,
    val unpaidPayablesIdr: Long,
    val projectSummaries: Map<String, Long>
)

object DefaultCategories {
    fun getDefaults(userId: String): List<TransactionCategoryModel> = listOf(
        // Income Categories
        TransactionCategoryModel("cat_salary", userId, "Gaji Utama", TransactionType.INCOME, "work", "#49E0A0", 1),
        TransactionCategoryModel("cat_freelance", userId, "Freelance & Proyek", TransactionType.INCOME, "laptop", "#72A7FF", 2),
        TransactionCategoryModel("cat_business_rev", userId, "Pendapatan Bisnis", TransactionType.INCOME, "store", "#38BDF8", 3),
        TransactionCategoryModel("cat_investment", userId, "Hasil Investasi", TransactionType.INCOME, "trending_up", "#A78BFA", 4),
        // Expense Categories
        TransactionCategoryModel("cat_food", userId, "Makanan & Minuman", TransactionType.EXPENSE, "restaurant", "#F5A623", 5),
        TransactionCategoryModel("cat_transport", userId, "Transportasi", TransactionType.EXPENSE, "directions_car", "#72A7FF", 6),
        TransactionCategoryModel("cat_housing", userId, "Tempat Tinggal", TransactionType.EXPENSE, "home", "#EC4899", 7),
        TransactionCategoryModel("cat_utilities", userId, "Tagihan & Utilitas", TransactionType.EXPENSE, "bolt", "#FBBF24", 8),
        TransactionCategoryModel("cat_shopping", userId, "Belanja", TransactionType.EXPENSE, "shopping_bag", "#F472B6", 9),
        TransactionCategoryModel("cat_health", userId, "Kesehatan", TransactionType.EXPENSE, "medical_services", "#34D399", 10),
        TransactionCategoryModel("cat_education", userId, "Pendidikan", TransactionType.EXPENSE, "school", "#60A5FA", 11),
        TransactionCategoryModel("cat_entertainment", userId, "Hiburan", TransactionType.EXPENSE, "movie", "#C084FC", 12),
        TransactionCategoryModel("cat_taxes", userId, "Pajak & Retribusi", TransactionType.EXPENSE, "account_balance", "#FF5A65", 13),
        TransactionCategoryModel("cat_biz_ops", userId, "Operasional Bisnis", TransactionType.EXPENSE, "business_center", "#38BDF8", 14),
        TransactionCategoryModel("cat_equipment", userId, "Peralatan & Perangkat", TransactionType.EXPENSE, "devices", "#818CF8", 15),
        TransactionCategoryModel("cat_other", userId, "Lainnya", TransactionType.EXPENSE, "category", "#98A3B3", 16)
    )
}
