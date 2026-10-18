package com.example.data.repository

import com.example.data.local.FinanceDao
import com.example.data.local.toEntity
import com.example.domain.BudgetModel
import com.example.domain.ContributionType
import com.example.domain.DefaultCategories
import com.example.domain.FinancialAccountModel
import com.example.domain.FinancialCalculator
import com.example.domain.FinancialGoalModel
import com.example.domain.GoalContributionModel
import com.example.domain.InvoiceReferenceModel
import com.example.domain.RecurringTransactionModel
import com.example.domain.SyncStatus
import com.example.domain.TransactionCategoryModel
import com.example.domain.TransactionModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Local-first repository backed by Room for instant offline durability and
 * synchronized with Cloud Firestore when authenticated and online.
 */
class OfflineFirstFinanceRepository(
    private val dao: FinanceDao,
    private val cloudRepo: FirestoreFinanceRepository?,
    private val userId: String
) {
    fun observeAccounts(): Flow<List<FinancialAccountModel>> =
        dao.observeAccounts(userId).map { list -> list.map { it.toDomain() } }

    fun observeCategories(): Flow<List<TransactionCategoryModel>> =
        dao.observeCategories(userId).map { list -> list.map { it.toDomain() } }

    fun observeTransactions(): Flow<List<TransactionModel>> =
        dao.observeTransactions(userId).map { list -> list.map { it.toDomain() } }

    fun observeBudgets(): Flow<List<BudgetModel>> =
        dao.observeBudgets(userId).map { list -> list.map { it.toDomain() } }

    fun observeGoals(): Flow<List<FinancialGoalModel>> =
        dao.observeGoals(userId).map { list -> list.map { it.toDomain() } }

    fun observeGoalContributions(): Flow<List<GoalContributionModel>> =
        dao.observeGoalContributions(userId).map { list -> list.map { it.toDomain() } }

    fun observeRecurringTransactions(): Flow<List<RecurringTransactionModel>> =
        dao.observeRecurringTransactions(userId).map { list -> list.map { it.toDomain() } }

    fun observeInvoices(): Flow<List<InvoiceReferenceModel>> =
        dao.observeInvoices(userId).map { list -> list.map { it.toDomain() } }

    suspend fun ensureDefaultCategoriesSeeded() {
        val existing = dao.getCategories(userId)
        if (existing.isEmpty()) {
            val defaults = DefaultCategories.getDefaults(userId).map { it.toEntity() }
            dao.upsertCategories(defaults)
        }
    }

    suspend fun saveAccount(account: FinancialAccountModel): FinancialAccountModel {
        val pending = account.copy(
            userId = userId,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertAccount(pending.toEntity())
        recalculateAndPersistAccountBalances()

        val refreshed = dao.getAccountById(account.id, userId)?.toDomain() ?: pending
        val syncResult = cloudRepo?.syncAccount(refreshed)
        val finalAccount = refreshed.copy(
            syncStatus = if (syncResult?.isSuccess == true) SyncStatus.SYNCED else SyncStatus.LOCAL_ONLY
        )
        dao.upsertAccount(finalAccount.toEntity())
        return finalAccount
    }

    suspend fun saveTransaction(tx: TransactionModel): TransactionModel {
        require(tx.amountIdr > 0L) { "Nominal transaksi harus lebih besar dari Rp0" }
        val pending = tx.copy(
            userId = userId,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertTransaction(pending.toEntity())
        recalculateAndPersistAccountBalances()

        val syncResult = cloudRepo?.syncTransaction(pending)
        val finalTx = pending.copy(
            syncStatus = if (syncResult?.isSuccess == true) SyncStatus.SYNCED else SyncStatus.LOCAL_ONLY
        )
        dao.upsertTransaction(finalTx.toEntity())
        return finalTx
    }

    suspend fun deleteTransaction(transactionId: String) {
        val now = System.currentTimeMillis()
        dao.softDeleteTransaction(transactionId, userId, SyncStatus.PENDING_UPLOAD.name, now)
        recalculateAndPersistAccountBalances()
        val deletedEntity = dao.getTransactionById(transactionId, userId)?.toDomain()
        if (deletedEntity != null && cloudRepo != null) {
            val res = cloudRepo.syncTransaction(deletedEntity)
            if (res.isSuccess) {
                dao.upsertTransaction(deletedEntity.copy(syncStatus = SyncStatus.SYNCED).toEntity())
            }
        }
    }

    suspend fun saveCategory(category: TransactionCategoryModel) {
        dao.upsertCategory(category.copy(userId = userId).toEntity())
    }

    suspend fun saveBudget(budget: BudgetModel) {
        val pending = budget.copy(
            userId = userId,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertBudget(pending.toEntity())
        val res = cloudRepo?.syncBudget(pending)
        if (res?.isSuccess == true) {
            dao.upsertBudget(pending.copy(syncStatus = SyncStatus.SYNCED).toEntity())
        }
    }

    suspend fun deleteBudget(budgetId: String) {
        dao.deleteBudget(budgetId, userId)
    }

    suspend fun saveGoal(goal: FinancialGoalModel) {
        val pending = goal.copy(
            userId = userId,
            isCompleted = goal.currentSavedIdr >= goal.targetAmountIdr,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertGoal(pending.toEntity())
        val res = cloudRepo?.syncGoal(pending)
        if (res?.isSuccess == true) {
            dao.upsertGoal(pending.copy(syncStatus = SyncStatus.SYNCED).toEntity())
        }
    }

    suspend fun recordGoalContribution(
        goalId: String,
        amountIdr: Long,
        type: ContributionType,
        notes: String
    ): FinancialGoalModel? {
        require(amountIdr > 0L) { "Nominal kontribusi harus positif" }
        val contribution = GoalContributionModel(
            id = "contrib_${UUID.randomUUID().toString().replace("-", "").take(16)}",
            userId = userId,
            goalId = goalId,
            amountIdr = amountIdr,
            contributionType = type,
            notes = notes,
            timestampMillis = System.currentTimeMillis()
        )
        val updatedGoalEntity = dao.recordGoalContributionAtomic(
            contribution.toEntity(),
            SyncStatus.PENDING_UPLOAD.name
        ) ?: return null
        val updatedGoal = updatedGoalEntity.toDomain()
        val res = cloudRepo?.syncGoal(updatedGoal)
        if (res?.isSuccess == true) {
            val synced = updatedGoal.copy(syncStatus = SyncStatus.SYNCED)
            dao.upsertGoal(synced.toEntity())
            return synced
        }
        return updatedGoal
    }

    suspend fun deleteGoal(goalId: String) {
        dao.deleteGoal(goalId, userId)
    }

    suspend fun saveRecurringTransaction(recurring: RecurringTransactionModel) {
        dao.upsertRecurringTransaction(recurring.copy(userId = userId).toEntity())
    }

    suspend fun deleteRecurringTransaction(id: String) {
        dao.deleteRecurringTransaction(id, userId)
    }

    suspend fun saveInvoice(invoice: InvoiceReferenceModel) {
        val pending = invoice.copy(
            userId = userId,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertInvoice(pending.toEntity())
        val res = cloudRepo?.syncInvoice(pending)
        if (res?.isSuccess == true) {
            dao.upsertInvoice(pending.copy(syncStatus = SyncStatus.SYNCED).toEntity())
        }
    }

    suspend fun deleteInvoice(id: String) {
        dao.deleteInvoice(id, userId)
    }

    suspend fun recalculateAndPersistAccountBalances() {
        val accounts = dao.getAccounts(userId).map { it.toDomain() }
        val transactions = dao.getTransactions(userId).map { it.toDomain() }
        val updatedEntities = accounts.map { acc ->
            val newBal = FinancialCalculator.calculateAccountBalance(acc, transactions)
            acc.copy(currentBalanceIdr = newBal).toEntity()
        }
        dao.upsertAccounts(updatedEntities)
    }

    /**
     * Synchronizes pending local records to Cloud Firestore and pulls newer cloud records.
     * Prevents duplicate entries by using stable IDs and version numbers.
     */
    suspend fun synchronizeWithCloud(): Result<Int> {
        val remote = cloudRepo ?: return Result.failure(IllegalStateException("Cloud repository not configured"))
        return try {
            var syncedCount = 0
            // 1. Push local pending accounts & transactions
            val localAccounts = dao.getAccounts(userId).map { it.toDomain() }
            for (acc in localAccounts) {
                if (acc.syncStatus != SyncStatus.SYNCED) {
                    if (remote.syncAccount(acc).isSuccess) {
                        dao.upsertAccount(acc.copy(syncStatus = SyncStatus.SYNCED).toEntity())
                        syncedCount++
                    }
                }
            }

            val localTxs = dao.getTransactions(userId).map { it.toDomain() }
            for (tx in localTxs) {
                if (tx.syncStatus != SyncStatus.SYNCED) {
                    if (remote.syncTransaction(tx).isSuccess) {
                        dao.upsertTransaction(tx.copy(syncStatus = SyncStatus.SYNCED).toEntity())
                        syncedCount++
                    }
                }
            }

            // 2. Pull cloud transactions and resolve conflicts by version
            val remoteTxsResult = remote.fetchTransactions()
            if (remoteTxsResult.isSuccess) {
                val localById = dao.getTransactions(userId).associateBy { it.id }
                for (remoteTx in remoteTxsResult.getOrDefault(emptyList())) {
                    val existingLocal = localById[remoteTx.id]
                    if (existingLocal == null || remoteTx.version >= existingLocal.version) {
                        dao.upsertTransaction(remoteTx.toEntity())
                        syncedCount++
                    }
                }
            }

            // 3. Pull cloud accounts
            val remoteAccounts = remote.fetchAccounts()
            val localAccById = dao.getAccounts(userId).associateBy { it.id }
            for (remoteAcc in remoteAccounts) {
                val existingLocal = localAccById[remoteAcc.id]
                if (existingLocal == null || remoteAcc.version >= existingLocal.version) {
                    dao.upsertAccount(remoteAcc.toEntity())
                    syncedCount++
                }
            }

            recalculateAndPersistAccountBalances()
            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAllUserDataLocallyAndCloud() {
        cloudRepo?.deleteCloudUserData()
        dao.deleteAllUserData(userId)
    }
}
