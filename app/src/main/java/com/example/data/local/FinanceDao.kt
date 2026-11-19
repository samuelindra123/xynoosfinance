package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

@Dao
interface FinanceDao {
    // Accounts
    @Query("SELECT * FROM accounts WHERE userId = :userId ORDER BY createdAtMillis ASC")
    fun observeAccounts(userId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE userId = :userId")
    suspend fun getAccounts(userId: String): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getAccountById(id: String, userId: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccounts(accounts: List<AccountEntity>)

    @Query("DELETE FROM accounts WHERE id = :id AND userId = :userId")
    suspend fun deleteAccount(id: String, userId: String)

    // Categories
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY sortOrder ASC")
    fun observeCategories(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY sortOrder ASC")
    suspend fun getCategories(userId: String): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    // Transactions
    @Query("SELECT * FROM transactions WHERE userId = :userId AND isDeleted = 0 ORDER BY transactionDateMillis DESC, createdAtMillis DESC")
    fun observeTransactions(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND isDeleted = 0 ORDER BY transactionDateMillis DESC")
    suspend fun getTransactions(userId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getTransactionById(id: String, userId: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(transactions: List<TransactionEntity>)

    @Query("UPDATE transactions SET isDeleted = 1, syncStatus = :syncStatus, version = version + 1, updatedAtMillis = :updatedAt WHERE id = :id AND userId = :userId")
    suspend fun softDeleteTransaction(id: String, userId: String, syncStatus: String, updatedAt: Long)

    // Budgets
    @Query("SELECT * FROM budgets WHERE userId = :userId ORDER BY updatedAtMillis DESC")
    fun observeBudgets(userId: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE userId = :userId")
    suspend fun getBudgets(userId: String): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudgets(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets WHERE id = :id AND userId = :userId")
    suspend fun deleteBudget(id: String, userId: String)

    // Goals & Contributions
    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY priority ASC, targetDateMillis ASC")
    fun observeGoals(userId: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE userId = :userId")
    suspend fun getGoals(userId: String): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :goalId AND userId = :userId LIMIT 1")
    suspend fun getGoalById(goalId: String, userId: String): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: GoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoals(goals: List<GoalEntity>)

    @Query("DELETE FROM goals WHERE id = :id AND userId = :userId")
    suspend fun deleteGoal(id: String, userId: String)

    @Query("SELECT * FROM goal_contributions WHERE userId = :userId ORDER BY timestampMillis DESC")
    fun observeGoalContributions(userId: String): Flow<List<GoalContributionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalContribution(contribution: GoalContributionEntity)

    @Transaction
    suspend fun recordGoalContributionAtomic(
        contribution: GoalContributionEntity,
        syncStatus: String
    ): GoalEntity? {
        val goal = getGoalById(contribution.goalId, contribution.userId) ?: return null
        insertGoalContribution(contribution)
        val delta = when (contribution.contributionType) {
            "DEPOSIT" -> contribution.amountIdr
            "WITHDRAWAL", "REVERSAL" -> -contribution.amountIdr
            else -> 0L
        }
        val newSaved = max(0L, goal.currentSavedIdr + delta)
        val updatedGoal = goal.copy(
            currentSavedIdr = newSaved,
            isCompleted = newSaved >= goal.targetAmountIdr,
            syncStatus = syncStatus,
            version = goal.version + 1,
            updatedAtMillis = System.currentTimeMillis()
        )
        upsertGoal(updatedGoal)
        return updatedGoal
    }

    // Recurring Transactions
    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId ORDER BY nextExecutionMillis ASC")
    fun observeRecurringTransactions(userId: String): Flow<List<RecurringTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecurringTransaction(recurring: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id AND userId = :userId")
    suspend fun deleteRecurringTransaction(id: String, userId: String)

    // Business Invoices (Receivables & Payables)
    @Query("SELECT * FROM invoices WHERE userId = :userId ORDER BY dueDateMillis ASC")
    fun observeInvoices(userId: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE userId = :userId")
    suspend fun getInvoices(userId: String): List<InvoiceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvoice(invoice: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvoices(invoices: List<InvoiceEntity>)

    @Query("DELETE FROM invoices WHERE id = :id AND userId = :userId")
    suspend fun deleteInvoice(id: String, userId: String)

    // Incremental Low-RAM Sync Queries (only fetch unsynced records)
    @Query("SELECT * FROM accounts WHERE userId = :userId AND syncStatus != 'SYNCED'")
    suspend fun getUnsyncedAccounts(userId: String): List<AccountEntity>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND syncStatus != 'SYNCED'")
    suspend fun getUnsyncedTransactions(userId: String): List<TransactionEntity>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND syncStatus != 'SYNCED'")
    suspend fun getUnsyncedBudgets(userId: String): List<BudgetEntity>

    @Query("SELECT * FROM goals WHERE userId = :userId AND syncStatus != 'SYNCED'")
    suspend fun getUnsyncedGoals(userId: String): List<GoalEntity>

    @Query("SELECT * FROM invoices WHERE userId = :userId AND syncStatus != 'SYNCED'")
    suspend fun getUnsyncedInvoices(userId: String): List<InvoiceEntity>

    // AI Persistent Chat Sessions & Messages
    @Query("SELECT * FROM ai_sessions WHERE userId = :userId ORDER BY updatedAtMillis DESC")
    fun observeAiSessions(userId: String): Flow<List<AiSessionEntity>>

    @Query("SELECT * FROM ai_sessions WHERE userId = :userId ORDER BY updatedAtMillis DESC")
    suspend fun getAiSessions(userId: String): List<AiSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAiSession(session: AiSessionEntity)

    @Query("DELETE FROM ai_sessions WHERE id = :sessionId AND userId = :userId")
    suspend fun deleteAiSession(sessionId: String, userId: String)

    @Query("SELECT * FROM ai_messages WHERE userId = :userId AND sessionId = :sessionId ORDER BY timestampMillis ASC")
    fun observeAiMessages(userId: String, sessionId: String): Flow<List<AiMessageEntity>>

    @Query("SELECT * FROM ai_messages WHERE userId = :userId AND sessionId = :sessionId ORDER BY timestampMillis ASC")
    suspend fun getAiMessages(userId: String, sessionId: String): List<AiMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAiMessage(message: AiMessageEntity)

    @Query("DELETE FROM ai_messages WHERE sessionId = :sessionId AND userId = :userId")
    suspend fun deleteMessagesForSession(sessionId: String, userId: String)

    @Transaction
    suspend fun deleteAiSessionAndMessages(sessionId: String, userId: String) {
        deleteMessagesForSession(sessionId, userId)
        deleteAiSession(sessionId, userId)
    }

    // User Account Cleanup (Account Deletion / Isolation)
    @Transaction
    suspend fun deleteAllUserData(userId: String) {
        deleteUserTransactions(userId)
        deleteUserAccounts(userId)
        deleteUserCategories(userId)
        deleteUserBudgets(userId)
        deleteUserGoals(userId)
        deleteUserGoalContributions(userId)
        deleteUserRecurring(userId)
        deleteUserInvoices(userId)
        deleteUserAiMessages(userId)
        deleteUserAiSessions(userId)
    }

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun deleteUserTransactions(userId: String)

    @Query("DELETE FROM accounts WHERE userId = :userId")
    suspend fun deleteUserAccounts(userId: String)

    @Query("DELETE FROM categories WHERE userId = :userId")
    suspend fun deleteUserCategories(userId: String)

    @Query("DELETE FROM budgets WHERE userId = :userId")
    suspend fun deleteUserBudgets(userId: String)

    @Query("DELETE FROM goals WHERE userId = :userId")
    suspend fun deleteUserGoals(userId: String)

    @Query("DELETE FROM goal_contributions WHERE userId = :userId")
    suspend fun deleteUserGoalContributions(userId: String)

    @Query("DELETE FROM recurring_transactions WHERE userId = :userId")
    suspend fun deleteUserRecurring(userId: String)

    @Query("DELETE FROM invoices WHERE userId = :userId")
    suspend fun deleteUserInvoices(userId: String)

    @Query("DELETE FROM ai_messages WHERE userId = :userId")
    suspend fun deleteUserAiMessages(userId: String)

    @Query("DELETE FROM ai_sessions WHERE userId = :userId")
    suspend fun deleteUserAiSessions(userId: String)
}

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        GoalContributionEntity::class,
        RecurringTransactionEntity::class,
        InvoiceEntity::class,
        AiSessionEntity::class,
        AiMessageEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class XynoosDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: XynoosDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_userId_transactionDateMillis` ON `transactions` (`userId`, `transactionDateMillis`)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_sessions` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        `lastMessagePreview` TEXT NOT NULL,
                        `messageCount` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ai_sessions_userId_updatedAtMillis` ON `ai_sessions` (`userId`, `updatedAtMillis`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_messages` (
                        `id` TEXT NOT NULL,
                        `sessionId` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `timestampMillis` INTEGER NOT NULL,
                        `draftType` TEXT,
                        `draftAmountIdr` INTEGER,
                        `draftTitle` TEXT,
                        `draftCategoryName` TEXT,
                        `draftAccountName` TEXT,
                        `draftNotes` TEXT,
                        `draftDateMillis` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ai_messages_userId_sessionId_timestampMillis` ON `ai_messages` (`userId`, `sessionId`, `timestampMillis`)"
                )
            }
        }

        fun getInstance(context: Context): XynoosDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    XynoosDatabase::class.java,
                    "xynoos_finance.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
