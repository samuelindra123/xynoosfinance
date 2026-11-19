package com.example.data.remote

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "XynoosAuth"

private fun getDefaultWebClientId(context: Context): String? {
    val resourceId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    if (resourceId == 0) return null
    return try {
        context.getString(resourceId)
    } catch (_: Exception) {
        null
    }
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

/**
 * Silent Auto-Sign-In on startup using GetGoogleIdOption in its own single-option request.
 */
fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    val firebaseAuth = runCatching { Firebase.auth }.getOrNull()
    if (firebaseAuth?.currentUser != null) {
        onAuthSuccess()
        return
    }
    if (credentialManager == null || firebaseAuth == null) {
        onUnauthenticated()
        return
    }
    val clientId = getDefaultWebClientId(context) ?: run {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    // One option per request for Android 16 (API 36) bottom sheet responsiveness
    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                firebaseAuth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            onUnauthenticated()
        }
    }
}

/**
 * Interactive Google Sign-In using GetSignInWithGoogleOption in its own single-option request.
 */
fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    if (credentialManager == null) {
        onAuthError("Layanan Google Credential Manager tidak tersedia di perangkat ini.")
        return
    }
    val firebaseAuth = runCatching { Firebase.auth }.getOrNull() ?: run {
        onAuthError("Layanan autentikasi Firebase belum siap.")
        return
    }
    val clientId = getDefaultWebClientId(context) ?: run {
        onAuthError("Konfigurasi Google Sign-In belum ditemukan (default_web_client_id).")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                firebaseAuth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Tipe kredensial Google tidak dikenali.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (t: Throwable) {
            Log.e(TAG, "Google Sign-In failed", t)
            onAuthError(t.localizedMessage ?: "Gagal masuk dengan Google. Pastikan Google Play Services aktif.")
        }
    }
}

fun signOutFromGoogle(
    credentialManager: CredentialManager?,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    runCatching { Firebase.auth.signOut() }
    scope.launch {
        try {
            credentialManager?.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to clear credential state", t)
        } finally {
            onSignOutComplete()
        }
    }
}
