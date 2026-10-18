package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.AccountType
import com.example.domain.AllocationRuleType
import com.example.domain.BudgetModel
import com.example.domain.ContributionType
import com.example.domain.FinancialAccountModel
import com.example.domain.FinancialGoalModel
import com.example.domain.GoalContributionModel
import com.example.domain.GoalPriority
import com.example.domain.InvoiceReferenceModel
import com.example.domain.InvoiceStatus
import com.example.domain.InvoiceType
import com.example.domain.RecurringTransactionModel
import com.example.domain.SyncStatus
import com.example.domain.TransactionCategoryModel
import com.example.domain.TransactionModel
import com.example.domain.TransactionType

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["userId"])]
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val accountType: String,
    val openingBalanceIdr: Long,
    val currentBalanceIdr: Long,
    val colorHex: String,
    val institutionName: String,
    val isArchived: Boolean,
    val syncStatus: String,
    val version: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toDomain() = FinancialAccountModel(
        id = id,
        userId = userId,
        name = name,
        accountType = runCatching { AccountType.valueOf(accountType) }.getOrDefault(AccountType.CASH),
        openingBalanceIdr = openingBalanceIdr,
        currentBalanceIdr = currentBalanceIdr,
        colorHex = colorHex,
        institutionName = institutionName,
        isArchived = isArchived,
        syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }.getOrDefault(SyncStatus.LOCAL_ONLY),
        version = version,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

fun FinancialAccountModel.toEntity() = AccountEntity(
    id = id,
    userId = userId,
    name = name,
    accountType = accountType.name,
    openingBalanceIdr = openingBalanceIdr,
    currentBalanceIdr = currentBalanceIdr,
    colorHex = colorHex,
    institutionName = institutionName,
    isArchived = isArchived,
    syncStatus = syncStatus.name,
    version = version,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["userId"])]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val type: String,
    val iconKey: String,
    val colorHex: String,
    val sortOrder: Int,
    val isArchived: Boolean,
    val version: Int
) {
    fun toDomain() = TransactionCategoryModel(
        id = id,
        userId = userId,
        name = name,
        type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE),
        iconKey = iconKey,
        colorHex = colorHex,
        sortOrder = sortOrder,
        isArchived = isArchived,
        version = version
    )
}

fun TransactionCategoryModel.toEntity() = CategoryEntity(
    id = id,
    userId = userId,
    name = name,
    type = type.name,
    iconKey = iconKey,
    colorHex = colorHex,
    sortOrder = sortOrder,
    isArchived = isArchived,
    version = version
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["userId", "transactionDateMillis"]),
        Index(value = ["accountId"]),
        Index(value = ["categoryId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val amountIdr: Long,
    val currencyCode: String,
    val title: String,
    val categoryId: String,
    val categoryName: String,
    val accountId: String,
    val accountName: String,
    val toAccountId: String?,
    val toAccountName: String?,
    val transferGroupId: String?,
    val transactionDateMillis: Long,
    val notes: String,
    val merchantName: String,
    val projectCostCenterId: String,
    val isDeleted: Boolean,
    val syncStatus: String,
    val version: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toDomain() = TransactionModel(
        id = id,
        userId = userId,
        type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE),
        amountIdr = amountIdr,
        currencyCode = currencyCode,
        title = title,
        categoryId = categoryId,
        categoryName = categoryName,
        accountId = accountId,
        accountName = accountName,
        toAccountId = toAccountId,
        toAccountName = toAccountName,
        transferGroupId = transferGroupId,
        transactionDateMillis = transactionDateMillis,
        notes = notes,
        merchantName = merchantName,
        projectCostCenterId = projectCostCenterId,
        isDeleted = isDeleted,
        syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }.getOrDefault(SyncStatus.LOCAL_ONLY),
        version = version,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

fun TransactionModel.toEntity() = TransactionEntity(
    id = id,
    userId = userId,
    type = type.name,
    amountIdr = amountIdr,
    currencyCode = currencyCode,
    title = title,
    categoryId = categoryId,
    categoryName = categoryName,
    accountId = accountId,
    accountName = accountName,
    toAccountId = toAccountId,
    toAccountName = toAccountName,
    transferGroupId = transferGroupId,
    transactionDateMillis = transactionDateMillis,
    notes = notes,
    merchantName = merchantName,
    projectCostCenterId = projectCostCenterId,
    isDeleted = isDeleted,
    syncStatus = syncStatus.name,
    version = version,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["userId", "periodYearMonth"])]
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val categoryId: String,
    val categoryName: String,
    val limitAmountIdr: Long,
    val periodYearMonth: String,
    val carryForward: Boolean,
    val alertThresholdPercent: Int,
    val syncStatus: String,
    val version: Int,
    val updatedAtMillis: Long
) {
    fun toDomain() = BudgetModel(
        id = id,
        userId = userId,
        name = name,
        categoryId = categoryId,
        categoryName = categoryName,
        limitAmountIdr = limitAmountIdr,
        periodYearMonth = periodYearMonth,
        carryForward = carryForward,
        alertThresholdPercent = alertThresholdPercent,
        syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }.getOrDefault(SyncStatus.LOCAL_ONLY),
        version = version,
        updatedAtMillis = updatedAtMillis
    )
}

fun BudgetModel.toEntity() = BudgetEntity(
    id = id,
    userId = userId,
    name = name,
    categoryId = categoryId,
    categoryName = categoryName,
    limitAmountIdr = limitAmountIdr,
    periodYearMonth = periodYearMonth,
    carryForward = carryForward,
    alertThresholdPercent = alertThresholdPercent,
    syncStatus = syncStatus.name,
    version = version,
    updatedAtMillis = updatedAtMillis
)

@Entity(
    tableName = "goals",
    indices = [Index(value = ["userId"])]
)
data class GoalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val targetAmountIdr: Long,
    val currentSavedIdr: Long,
    val targetDateMillis: Long,
    val priority: String,
    val iconKey: String,
    val allocationRuleType: String,
    val allocationValue: Long,
    val linkedAccountId: String?,
    val isCompleted: Boolean,
    val syncStatus: String,
    val version: Int,
    val updatedAtMillis: Long
) {
    fun toDomain() = FinancialGoalModel(
        id = id,
        userId = userId,
        name = name,
        targetAmountIdr = targetAmountIdr,
        currentSavedIdr = currentSavedIdr,
        targetDateMillis = targetDateMillis,
        priority = runCatching { GoalPriority.valueOf(priority) }.getOrDefault(GoalPriority.MEDIUM),
        iconKey = iconKey,
        allocationRuleType = runCatching { AllocationRuleType.valueOf(allocationRuleType) }.getOrDefault(AllocationRuleType.MANUAL),
        allocationValue = allocationValue,
        linkedAccountId = linkedAccountId,
        isCompleted = isCompleted,
        syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }.getOrDefault(SyncStatus.LOCAL_ONLY),
        version = version,
        updatedAtMillis = updatedAtMillis
    )
}

fun FinancialGoalModel.toEntity() = GoalEntity(
    id = id,
    userId = userId,
    name = name,
    targetAmountIdr = targetAmountIdr,
    currentSavedIdr = currentSavedIdr,
    targetDateMillis = targetDateMillis,
    priority = priority.name,
    iconKey = iconKey,
    allocationRuleType = allocationRuleType.name,
    allocationValue = allocationValue,
    linkedAccountId = linkedAccountId,
    isCompleted = isCompleted,
    syncStatus = syncStatus.name,
    version = version,
    updatedAtMillis = updatedAtMillis
)

@Entity(
    tableName = "goal_contributions",
    indices = [Index(value = ["userId", "goalId"])]
)
data class GoalContributionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val goalId: String,
    val amountIdr: Long,
    val contributionType: String,
    val notes: String,
    val timestampMillis: Long
) {
    fun toDomain() = GoalContributionModel(
        id = id,
        userId = userId,
        goalId = goalId,
        amountIdr = amountIdr,
        contributionType = runCatching { ContributionType.valueOf(contributionType) }.getOrDefault(ContributionType.DEPOSIT),
        notes = notes,
        timestampMillis = timestampMillis
    )
}

fun GoalContributionModel.toEntity() = GoalContributionEntity(
    id = id,
    userId = userId,
    goalId = goalId,
    amountIdr = amountIdr,
    contributionType = contributionType.name,
    notes = notes,
    timestampMillis = timestampMillis
)

@Entity(
    tableName = "recurring_transactions",
    indices = [Index(value = ["userId"])]
)
data class RecurringTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val type: String,
    val amountIdr: Long,
    val categoryId: String,
    val categoryName: String,
    val accountId: String,
    val accountName: String,
    val frequency: String,
    val nextExecutionMillis: Long,
    val isActive: Boolean
) {
    fun toDomain() = RecurringTransactionModel(
        id = id,
        userId = userId,
        title = title,
        type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE),
        amountIdr = amountIdr,
        categoryId = categoryId,
        categoryName = categoryName,
        accountId = accountId,
        accountName = accountName,
        frequency = frequency,
        nextExecutionMillis = nextExecutionMillis,
        isActive = isActive
    )
}

fun RecurringTransactionModel.toEntity() = RecurringTransactionEntity(
    id = id,
    userId = userId,
    title = title,
    type = type.name,
    amountIdr = amountIdr,
    categoryId = categoryId,
    categoryName = categoryName,
    accountId = accountId,
    accountName = accountName,
    frequency = frequency,
    nextExecutionMillis = nextExecutionMillis,
    isActive = isActive
)

@Entity(
    tableName = "invoices",
    indices = [Index(value = ["userId"])]
)
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val invoiceNumber: String,
    val counterpartyName: String,
    val invoiceType: String,
    val amountIdr: Long,
    val dueDateMillis: Long,
    val status: String,
    val projectCostCenter: String,
    val syncStatus: String,
    val version: Int,
    val updatedAtMillis: Long
) {
    fun toDomain() = InvoiceReferenceModel(
        id = id,
        userId = userId,
        invoiceNumber = invoiceNumber,
        counterpartyName = counterpartyName,
        invoiceType = runCatching { InvoiceType.valueOf(invoiceType) }.getOrDefault(InvoiceType.RECEIVABLE),
        amountIdr = amountIdr,
        dueDateMillis = dueDateMillis,
        status = runCatching { InvoiceStatus.valueOf(status) }.getOrDefault(InvoiceStatus.UNPAID),
        projectCostCenter = projectCostCenter,
        syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }.getOrDefault(SyncStatus.LOCAL_ONLY),
        version = version,
        updatedAtMillis = updatedAtMillis
    )
}

fun InvoiceReferenceModel.toEntity() = InvoiceEntity(
    id = id,
    userId = userId,
    invoiceNumber = invoiceNumber,
    counterpartyName = counterpartyName,
    invoiceType = invoiceType.name,
    amountIdr = amountIdr,
    dueDateMillis = dueDateMillis,
    status = status.name,
    projectCostCenter = projectCostCenter,
    syncStatus = syncStatus.name,
    version = version,
    updatedAtMillis = updatedAtMillis
)
