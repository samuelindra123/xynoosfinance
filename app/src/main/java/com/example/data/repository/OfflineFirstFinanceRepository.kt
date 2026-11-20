package com.example.data.repository

import com.example.data.local.AiSessionEntity
import com.example.data.local.FinanceDao
import com.example.data.local.toEntity
import com.example.domain.AiMessageModel
import com.example.domain.AiSessionModel
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
import kotlinx.coroutines.CancellationException
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
    private suspend inline fun <T> safeCloudCall(block: () -> Result<T>): Result<T>? {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
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

    // Persistent AI Chat Sessions & Messages
    fun observeAiSessions(): Flow<List<AiSessionModel>> =
        dao.observeAiSessions(userId).map { list -> list.map { it.toDomain() } }

    fun observeAiMessages(sessionId: String): Flow<List<AiMessageModel>> =
        dao.observeAiMessages(userId, sessionId).map { list -> list.map { it.toDomain() } }

    suspend fun ensureDefaultAiSession(welcomeContent: String): String {
        val existing = dao.getAiSessions(userId)
        if (existing.isNotEmpty()) {
            return existing.first().id
        }
        val now = System.currentTimeMillis()
        val sessionId = "ai_sess_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val session = AiSessionEntity(
            id = sessionId,
            userId = userId,
            title = "Konsultasi Keuangan #1",
            createdAtMillis = now,
            updatedAtMillis = now,
            lastMessagePreview = "Siap membantu analisis keuangan & catat transaksi...",
            messageCount = 1
        )
        dao.upsertAiSession(session)
        dao.upsertAiMessage(
            AiMessageModel(
                id = "msg_welcome_$sessionId",
                sessionId = sessionId,
                userId = userId,
                role = "assistant",
                content = welcomeContent,
                timestampMillis = now
            ).toEntity()
        )
        return sessionId
    }

    suspend fun createAiSession(title: String, welcomeContent: String): AiSessionModel {
        val now = System.currentTimeMillis()
        val count = dao.getAiSessions(userId).size + 1
        val finalTitle = title.trim().ifBlank { "Sesi Konsultasi #$count" }
        val session = AiSessionModel(
            id = "ai_sess_${UUID.randomUUID().toString().replace("-", "").take(10)}",
            userId = userId,
            title = finalTitle,
            createdAtMillis = now,
            updatedAtMillis = now,
            lastMessagePreview = "Sesi baru dimulai",
            messageCount = 1
        )
        dao.upsertAiSession(session.toEntity())
        dao.upsertAiMessage(
            AiMessageModel(
                id = "msg_welcome_${session.id}",
                sessionId = session.id,
                userId = userId,
                role = "assistant",
                content = welcomeContent,
                timestampMillis = now
            ).toEntity()
        )
        return session
    }

    suspend fun renameAiSession(sessionId: String, newTitle: String) {
        val cleaned = newTitle.trim().take(60)
        if (cleaned.isEmpty()) return
        val current = dao.getAiSessions(userId).firstOrNull { it.id == sessionId } ?: return
        dao.upsertAiSession(current.copy(title = cleaned, updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun deleteAiSession(sessionId: String) {
        dao.deleteAiSessionAndMessages(sessionId, userId)
    }

    suspend fun saveAiMessage(message: AiMessageModel) {
        val normalized = message.copy(userId = userId)
        dao.upsertAiMessage(normalized.toEntity())
        val sessions = dao.getAiSessions(userId)
        val existingSession = sessions.firstOrNull { it.id == normalized.sessionId }
        val preview = normalized.content
            .replace("\n", " ")
            .replace("**", "")
            .trim()
            .take(80)
        val existingMsgsCount = dao.getAiMessages(userId, normalized.sessionId).size
        if (existingSession != null) {
            val shouldAutoTitle = normalized.role == "user" &&
                (existingSession.title.startsWith("Sesi Konsultasi") || existingSession.title.startsWith("Konsultasi Keuangan"))
            val updatedTitle = if (shouldAutoTitle) {
                normalized.content.trim().take(36).let { if (normalized.content.length > 36) "$it…" else it }
            } else {
                existingSession.title
            }
            dao.upsertAiSession(
                existingSession.copy(
                    title = updatedTitle,
                    updatedAtMillis = normalized.timestampMillis,
                    lastMessagePreview = preview,
                    messageCount = existingMsgsCount
                )
            )
        } else {
            dao.upsertAiSession(
                AiSessionEntity(
                    id = normalized.sessionId,
                    userId = userId,
                    title = if (normalized.role == "user") normalized.content.take(36) else "Sesi Konsultasi",
                    createdAtMillis = normalized.timestampMillis,
                    updatedAtMillis = normalized.timestampMillis,
                    lastMessagePreview = preview,
                    messageCount = existingMsgsCount
                )
            )
        }
    }

    suspend fun getAiMessagesForSession(sessionId: String): List<AiMessageModel> =
        dao.getAiMessages(userId, sessionId).map { it.toDomain() }

    suspend fun ensureDefaultCategoriesSeeded() {
        val existing = dao.getCategories(userId)
        val existingMap = existing.associateBy { it.id }
        val defaults = DefaultCategories.getDefaults(userId)
        val toUpsert = buildList {
            defaults.forEach { cat ->
                val prev = existingMap[cat.id]
                if (prev == null) {
                    add(cat.toEntity())
                } else if (cat.id == "cat_salary" && prev.name == "Gaji Utama") {
                    add(prev.copy(name = cat.name))
                }
            }
        }
        if (toUpsert.isNotEmpty()) {
            dao.upsertCategories(toUpsert)
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
        val syncResult = cloudRepo?.let { safeCloudCall { it.syncAccount(refreshed) } }
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

        val syncResult = cloudRepo?.let { safeCloudCall { it.syncTransaction(pending) } }
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
            val res = safeCloudCall { cloudRepo.syncTransaction(deletedEntity) }
            if (res?.isSuccess == true) {
                dao.upsertTransaction(deletedEntity.copy(syncStatus = SyncStatus.SYNCED).toEntity())
            }
        }
    }

    suspend fun saveCategory(category: TransactionCategoryModel) {
        val normalized = category.copy(userId = userId)
        dao.upsertCategory(normalized.toEntity())
        cloudRepo?.let { safeCloudCall { it.syncCategory(normalized) } }
    }

    suspend fun saveBudget(budget: BudgetModel) {
        val pending = budget.copy(
            userId = userId,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertBudget(pending.toEntity())
        val res = cloudRepo?.let { safeCloudCall { it.syncBudget(pending) } }
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
        val res = cloudRepo?.let { safeCloudCall { it.syncGoal(pending) } }
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
        val res = cloudRepo?.let { safeCloudCall { it.syncGoal(updatedGoal) } }
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
        val res = cloudRepo?.let { safeCloudCall { it.syncInvoice(pending) } }
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
     * Low-RAM incremental backup: only queries and uploads unsynced local records
     * without loading full tables or overwriting cloud with stale data.
     */
    suspend fun backupPendingChangesIncremental(): Result<Int> {
        val remote = cloudRepo ?: return Result.failure(IllegalStateException("Cloud repository not configured"))
        return try {
            var pushedCount = 0
            var uploadFailure: Throwable? = null

            val pendingAccounts = dao.getUnsyncedAccounts(userId).map { it.toDomain() }
            for (acc in pendingAccounts) {
                val result = remote.syncAccount(acc)
                if (result.isSuccess) {
                    dao.upsertAccount(acc.copy(syncStatus = SyncStatus.SYNCED).toEntity())
                    pushedCount++
                } else if (uploadFailure == null) {
                    uploadFailure = result.exceptionOrNull()
                }
            }

            val pendingTxs = dao.getUnsyncedTransactions(userId).map { it.toDomain() }
            for (tx in pendingTxs) {
                val result = remote.syncTransaction(tx)
                if (result.isSuccess) {
                    dao.upsertTransaction(tx.copy(syncStatus = SyncStatus.SYNCED).toEntity())
                    pushedCount++
                } else if (uploadFailure == null) {
                    uploadFailure = result.exceptionOrNull()
                }
            }

            val pendingBudgets = dao.getUnsyncedBudgets(userId).map { it.toDomain() }
            for (bdg in pendingBudgets) {
                val result = remote.syncBudget(bdg)
                if (result.isSuccess) {
                    dao.upsertBudget(bdg.copy(syncStatus = SyncStatus.SYNCED).toEntity())
                    pushedCount++
                } else if (uploadFailure == null) {
                    uploadFailure = result.exceptionOrNull()
                }
            }

            val pendingGoals = dao.getUnsyncedGoals(userId).map { it.toDomain() }
            for (goal in pendingGoals) {
                val result = remote.syncGoal(goal)
                if (result.isSuccess) {
                    dao.upsertGoal(goal.copy(syncStatus = SyncStatus.SYNCED).toEntity())
                    pushedCount++
                } else if (uploadFailure == null) {
                    uploadFailure = result.exceptionOrNull()
                }
            }

            val pendingInvoices = dao.getUnsyncedInvoices(userId).map { it.toDomain() }
            for (inv in pendingInvoices) {
                val result = remote.syncInvoice(inv)
                if (result.isSuccess) {
                    dao.upsertInvoice(inv.copy(syncStatus = SyncStatus.SYNCED).toEntity())
                    pushedCount++
                } else if (uploadFailure == null) {
                    uploadFailure = result.exceptionOrNull()
                }
            }

            uploadFailure?.let { Result.failure(it) } ?: Result.success(pushedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * Full bidirectional sync: pushes pending local records across all collections
     * and pulls newer cloud records isolated per Google UID.
     */
    suspend fun synchronizeWithCloud(): Result<Int> {
        val remote = cloudRepo ?: return Result.failure(IllegalStateException("Cloud repository not configured"))
        return try {
            val backupResult = backupPendingChangesIncremental()
            var syncedCount = backupResult.getOrDefault(0)
            var uploadFailure = backupResult.exceptionOrNull()

            // Sync custom user categories
            val defaultIds = DefaultCategories.getDefaults(userId).map { it.id }.toSet()
            val customLocalCats = dao.getCategories(userId).map { it.toDomain() }.filter { it.id !in defaultIds }
            for (cat in customLocalCats) {
                val result = remote.syncCategory(cat)
                if (result.isSuccess) syncedCount++
                else if (uploadFailure == null) uploadFailure = result.exceptionOrNull()
            }

            // Pull cloud transactions and resolve conflicts by version
            for (remoteTx in remote.fetchTransactions().getOrThrow()) {
                val existingLocal = dao.getTransactionById(remoteTx.id, userId)
                if (existingLocal == null || remoteTx.version > existingLocal.version ||
                    (remoteTx.version == existingLocal.version && existingLocal.syncStatus == SyncStatus.SYNCED.name)
                ) {
                    dao.upsertTransaction(remoteTx.toEntity())
                    syncedCount++
                }
            }

            // Pull cloud accounts
            val remoteAccounts = remote.fetchAccounts()
            val localAccById = dao.getAccounts(userId).associateBy { it.id }
            for (remoteAcc in remoteAccounts) {
                val existingLocal = localAccById[remoteAcc.id]
                if (existingLocal == null || remoteAcc.version >= existingLocal.version) {
                    dao.upsertAccount(remoteAcc.toEntity())
                    syncedCount++
                }
            }

            // Pull cloud budgets, goals, invoices, categories
            val localBdgById = dao.getBudgets(userId).associateBy { it.id }
            for (remoteBdg in remote.fetchBudgets()) {
                val existingLocal = localBdgById[remoteBdg.id]
                if (existingLocal == null || remoteBdg.version >= existingLocal.version) {
                    dao.upsertBudget(remoteBdg.toEntity())
                    syncedCount++
                }
            }

            val localGoalById = dao.getGoals(userId).associateBy { it.id }
            for (remoteGoal in remote.fetchGoals()) {
                val existingLocal = localGoalById[remoteGoal.id]
                if (existingLocal == null || remoteGoal.version >= existingLocal.version) {
                    dao.upsertGoal(remoteGoal.toEntity())
                    syncedCount++
                }
            }

            val localInvById = dao.getInvoices(userId).associateBy { it.id }
            for (remoteInv in remote.fetchInvoices()) {
                val existingLocal = localInvById[remoteInv.id]
                if (existingLocal == null || remoteInv.version >= existingLocal.version) {
                    dao.upsertInvoice(remoteInv.toEntity())
                    syncedCount++
                }
            }

            val localCatById = dao.getCategories(userId).associateBy { it.id }
            for (remoteCat in remote.fetchCategories()) {
                if (localCatById[remoteCat.id] == null) {
                    dao.upsertCategory(remoteCat.toEntity())
                    syncedCount++
                }
            }

            recalculateAndPersistAccountBalances()
            uploadFailure?.let { Result.failure(it) } ?: Result.success(syncedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun deleteAllUserDataLocallyAndCloud() {
        cloudRepo?.let { safeCloudCall { it.deleteCloudUserData() } }
        dao.deleteAllUserData(userId)
    }
}
