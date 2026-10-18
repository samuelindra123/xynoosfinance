package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.domain.TransactionModel
import com.example.domain.TransactionType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class FirestoreFinanceRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun syncTransaction_authenticatedOwner_succeedsAndEmitsRealtimeUpdate() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreFinanceRepository(firestore, auth)
        val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        val tx = TransactionModel(
            id = txId,
            userId = aliceUid,
            type = TransactionType.INCOME,
            amountIdr = 12_500_000L,
            currencyCode = "IDR",
            title = "Pembayaran Klien Desain",
            categoryId = "cat_freelance",
            categoryName = "Freelance & Proyek",
            accountId = "acc_bca",
            accountName = "BCA Utama",
            transactionDateMillis = 1760000000000L
        )

        val syncRes = withTimeout(DEFAULT_TIMEOUT_MS) { repository.syncTransaction(tx) }
        assertTrue(syncRes.isSuccess)

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTransactions(aliceUid).first { list -> list.any { it.id == txId } }
        }
        assertTrue(emitted.any { it.id == txId && it.amountIdr == 12_500_000L })
    }

    @Test
    fun getTransactionById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreFinanceRepository(firestore, auth)
        val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        val tx = TransactionModel(
            id = txId,
            userId = aliceUid,
            type = TransactionType.EXPENSE,
            amountIdr = 75_000L,
            currencyCode = "IDR",
            title = "Kopi & Rapat",
            categoryId = "cat_food",
            categoryName = "Makanan & Minuman",
            accountId = "acc_cash",
            accountName = "Tunai",
            transactionDateMillis = 1760000000000L
        )
        withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.syncTransaction(tx).getOrThrow() }

        signInTestUser(BOB_EMAIL)
        val bobRepo = FirestoreFinanceRepository(firestore, auth)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getTransactionByIdForUser(aliceUid, txId)
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for cross-user access")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun observeTransactions_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = FirestoreFinanceRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTransactions("alice_unauth_test").first()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for unauthenticated caller")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@xynoos.test"
        const val BOB_EMAIL = "bob@xynoos.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
