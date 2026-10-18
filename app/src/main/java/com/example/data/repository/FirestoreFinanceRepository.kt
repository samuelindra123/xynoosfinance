package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.domain.AccountType
import com.example.domain.AllocationRuleType
import com.example.domain.BudgetModel
import com.example.domain.FinancialAccountModel
import com.example.domain.FinancialGoalModel
import com.example.domain.GoalPriority
import com.example.domain.InvoiceReferenceModel
import com.example.domain.InvoiceStatus
import com.example.domain.InvoiceType
import com.example.domain.SyncStatus
import com.example.domain.TransactionModel
import com.example.domain.TransactionType
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@IgnoreExtraProperties
data class FirestoreTransactionDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "EXPENSE",
    val amountIdr: Long = 0L,
    val currencyCode: String = "IDR",
    val title: String = "",
    val categoryId: String = "cat_other",
    val categoryName: String = "Lainnya",
    val accountId: String = "acc_cash",
    val accountName: String = "Tunai",
    val toAccountId: String? = null,
    val toAccountName: String? = null,
    val transferGroupId: String? = null,
    val transactionDateMillis: Long = 0L,
    val notes: String? = null,
    val merchantName: String? = null,
    val projectCostCenterId: String? = null,
    val isDeleted: Boolean = false,
    val version: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomain(): TransactionModel {
        val createdMillis = createdAt?.toDate()?.time ?: transactionDateMillis
        val updatedMillis = updatedAt?.toDate()?.time ?: createdMillis
        return TransactionModel(
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
            notes = notes.orEmpty(),
            merchantName = merchantName.orEmpty(),
            projectCostCenterId = projectCostCenterId.orEmpty(),
            isDeleted = isDeleted,
            syncStatus = SyncStatus.SYNCED,
            version = version.toInt(),
            createdAtMillis = createdMillis,
            updatedAtMillis = updatedMillis
        )
    }
}

class FirestoreFinanceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")
    }

    suspend fun upsertUserProfile(
        displayName: String,
        email: String?,
        currencyCode: String,
        financialProfileType: String,
        onboardingCompleted: Boolean,
        version: Int = 1
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
        return try {
            val existing = docRef.get().await()
            val createdField = if (existing.exists() && existing.getTimestamp("createdAt") != null) {
                existing.getTimestamp("createdAt")!!
            } else {
                FieldValue.serverTimestamp()
            }
            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "displayName" to displayName.take(100).ifBlank { "Pengguna Xynoos" },
                "email" to email?.take(150),
                "currencyCode" to currencyCode,
                "financialProfileType" to financialProfileType,
                "onboardingCompleted" to onboardingCompleted,
                "version" to version,
                "createdAt" to createdField,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncAccount(account: FinancialAccountModel): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("accounts").document(account.id)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val remoteVersion = (existing.getLong("version") ?: 0L).toInt()
                if (remoteVersion > account.version) {
                    // Explicit conflict protection: do not overwrite newer cloud version
                    return Result.success(account.id)
                }
            }
            val createdField = if (existing.exists() && existing.getTimestamp("createdAt") != null) {
                existing.getTimestamp("createdAt")!!
            } else {
                FieldValue.serverTimestamp()
            }
            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "name" to account.name.take(80),
                "accountType" to account.accountType.name,
                "openingBalanceIdr" to account.openingBalanceIdr,
                "currentBalanceIdr" to account.currentBalanceIdr,
                "colorHex" to account.colorHex.take(12),
                "institutionName" to account.institutionName.take(80).ifBlank { null },
                "isArchived" to account.isArchived,
                "version" to account.version,
                "createdAt" to createdField,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(account.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncTransaction(tx: TransactionModel): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("transactions").document(tx.id)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val remoteVersion = (existing.getLong("version") ?: 0L).toInt()
                if (remoteVersion > tx.version) {
                    return Result.success(tx.id)
                }
            }
            val createdField = if (existing.exists() && existing.getTimestamp("createdAt") != null) {
                existing.getTimestamp("createdAt")!!
            } else {
                FieldValue.serverTimestamp()
            }
            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "type" to tx.type.name,
                "amountIdr" to tx.amountIdr,
                "currencyCode" to tx.currencyCode,
                "title" to tx.title.take(160).ifBlank { "Transaksi" },
                "categoryId" to tx.categoryId.take(128),
                "categoryName" to tx.categoryName.take(80),
                "accountId" to tx.accountId.take(128),
                "accountName" to tx.accountName.take(80),
                "toAccountId" to tx.toAccountId?.take(128)?.ifBlank { null },
                "toAccountName" to tx.toAccountName?.take(80)?.ifBlank { null },
                "transferGroupId" to tx.transferGroupId?.take(128)?.ifBlank { null },
                "transactionDateMillis" to tx.transactionDateMillis,
                "notes" to tx.notes.take(500).ifBlank { null },
                "merchantName" to tx.merchantName.take(120).ifBlank { null },
                "projectCostCenterId" to tx.projectCostCenterId.take(128).ifBlank { null },
                "isDeleted" to tx.isDeleted,
                "version" to tx.version,
                "createdAt" to createdField,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(tx.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getTransactionByIdForUser(ownerUid: String, txId: String): Result<TransactionModel?> {
        val docRef = db.collection("users").document(ownerUid).collection("transactions").document(txId)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                Result.success(null)
            } else {
                val doc = snap.toObject(
                    FirestoreTransactionDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                Result.success(doc?.toDomain())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun fetchTransactions(): Result<List<TransactionModel>> {
        val uid = requireUserId()
        val path = "users/$uid/transactions"
        return try {
            val snapshot = db.collection("users")
                .document(uid)
                .collection("transactions")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val list = snapshot.toObjects(
                FirestoreTransactionDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).map { it.toDomain() }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    fun observeTransactions(targetUid: String? = null): Flow<List<TransactionModel>> = flow {
        val uid = targetUid ?: auth.currentUser?.uid ?: "unauthenticated_uid"
        val path = "users/$uid/transactions"
        emitAll(
            db.collection("users")
                .document(uid)
                .collection("transactions")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestoreTransactionDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.toDomain() }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun fetchAccounts(): List<FinancialAccountModel> {
        val uid = requireUserId()
        val path = "users/$uid/accounts"
        return try {
            val snap = db.collection("users").document(uid).collection("accounts")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                val typeStr = doc.getString("accountType") ?: "CASH"
                FinancialAccountModel(
                    id = doc.id,
                    userId = uid,
                    name = name,
                    accountType = runCatching { AccountType.valueOf(typeStr) }.getOrDefault(AccountType.CASH),
                    openingBalanceIdr = doc.getLong("openingBalanceIdr") ?: 0L,
                    currentBalanceIdr = doc.getLong("currentBalanceIdr") ?: 0L,
                    colorHex = doc.getString("colorHex") ?: "#49E0A0",
                    institutionName = doc.getString("institutionName").orEmpty(),
                    isArchived = doc.getBoolean("isArchived") ?: false,
                    syncStatus = SyncStatus.SYNCED,
                    version = (doc.getLong("version") ?: 1L).toInt()
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    suspend fun syncBudget(budget: BudgetModel): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("budgets").document(budget.id)
        return try {
            val existing = docRef.get().await()
            val createdField = if (existing.exists() && existing.getTimestamp("createdAt") != null) {
                existing.getTimestamp("createdAt")!!
            } else {
                FieldValue.serverTimestamp()
            }
            val payload = mapOf<String, Any>(
                "userId" to uid,
                "name" to budget.name.take(80),
                "categoryId" to budget.categoryId.take(128),
                "categoryName" to budget.categoryName.take(80),
                "limitAmountIdr" to budget.limitAmountIdr,
                "periodYearMonth" to budget.periodYearMonth.take(16),
                "carryForward" to budget.carryForward,
                "alertThresholdPercent" to budget.alertThresholdPercent,
                "version" to budget.version,
                "createdAt" to createdField,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(budget.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncGoal(goal: FinancialGoalModel): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("goals").document(goal.id)
        return try {
            val existing = docRef.get().await()
            val createdField = if (existing.exists() && existing.getTimestamp("createdAt") != null) {
                existing.getTimestamp("createdAt")!!
            } else {
                FieldValue.serverTimestamp()
            }
            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "name" to goal.name.take(80),
                "targetAmountIdr" to goal.targetAmountIdr,
                "currentSavedIdr" to goal.currentSavedIdr,
                "targetDateMillis" to goal.targetDateMillis,
                "priority" to goal.priority.name,
                "iconKey" to goal.iconKey.take(40),
                "allocationRuleType" to goal.allocationRuleType.name,
                "allocationValue" to goal.allocationValue,
                "linkedAccountId" to goal.linkedAccountId?.take(128)?.ifBlank { null },
                "isCompleted" to goal.isCompleted,
                "version" to goal.version,
                "createdAt" to createdField,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(goal.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncInvoice(invoice: InvoiceReferenceModel): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("invoices").document(invoice.id)
        return try {
            val existing = docRef.get().await()
            val createdField = if (existing.exists() && existing.getTimestamp("createdAt") != null) {
                existing.getTimestamp("createdAt")!!
            } else {
                FieldValue.serverTimestamp()
            }
            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "invoiceNumber" to invoice.invoiceNumber.take(60),
                "counterpartyName" to invoice.counterpartyName.take(100),
                "invoiceType" to invoice.invoiceType.name,
                "amountIdr" to invoice.amountIdr,
                "dueDateMillis" to invoice.dueDateMillis,
                "status" to invoice.status.name,
                "projectCostCenter" to invoice.projectCostCenter.take(80).ifBlank { null },
                "version" to invoice.version,
                "createdAt" to createdField,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(invoice.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudUserData(): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val userDoc = db.collection("users").document(uid)
        return try {
            val subcollections = listOf("transactions", "accounts", "categories", "budgets", "goals", "goal_contributions", "invoices")
            for (sub in subcollections) {
                val snap = userDoc.collection(sub).whereEqualTo("userId", uid).get().await()
                for (doc in snap.documents) {
                    doc.reference.delete().await()
                }
            }
            userDoc.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, userDoc.path)
            Result.failure(e)
        }
    }
}
