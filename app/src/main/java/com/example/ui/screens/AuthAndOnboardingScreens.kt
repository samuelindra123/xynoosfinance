package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.remote.attemptAutoSignIn
import com.example.data.remote.onGoogleSignInClicked
import com.example.domain.AccountType
import com.example.domain.AppThemeMode
import com.example.domain.FinancialProfileType
import com.example.domain.IdrFormatter
import com.example.ui.components.XynoosBrandLogo
import com.example.ui.components.XynoosCurrencyNominalInput
import com.example.ui.components.XynoosHeroIllustration
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.SapphireSecondary
import kotlinx.coroutines.delay

@Composable
fun XynoosSplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(650L)
        onFinished()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            XynoosBrandLogo(size = 88.dp)
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "XynoosFinance",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Understand your money. Control your future.",
                style = MaterialTheme.typography.bodySmall,
                color = EmeraldPrimary
            )
        }
    }
}

@Composable
fun WelcomeAuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("welcome_auth_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                XynoosBrandLogo(size = 44.dp)
                Column {
                    Text(
                        text = "XynoosFinance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Private Wealth & AI Financial Ledger",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            XynoosHeroIllustration()
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Your money, understood.",
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Kelola setiap transaksi Rupiah, pantau anggaran & target tabungan, dan ambil keputusan finansial lebih cerdas bersama asisten AI pribadi Anda.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Value Pillars
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Buku besar deterministik IDR (tanpa pembulatan float)", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = SapphireSecondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Penyimpanan Local-First (Room) + Sinkronisasi Cloud Firestore", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Kendali penuh privasi data & ekspor laporan PDF profesional", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (authError != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = authError ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    authError = null
                    isLoading = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            authError = msg
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isLoading = false
                            authError = "Masuk dengan Google dibatalkan. Silakan pilih akun Google Anda untuk melanjutkan."
                        }
                    )
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_sign_in_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Memverifikasi Kredensial Google...")
                } else {
                    Text(
                        text = "Continue with Google",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Hanya menggunakan autentikasi Google resmi. Data keuangan Anda diisolasi per pengguna dan tidak pernah dibagikan ke pihak ketiga.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun OnboardingWizardScreen(
    initialDisplayName: String,
    currentThemeMode: AppThemeMode,
    onThemeChanged: (AppThemeMode) -> Unit,
    onCompleteOnboarding: (
        displayName: String,
        currencyCode: String,
        profileType: FinancialProfileType,
        initialAccountName: String,
        initialAccountType: AccountType,
        initialOpeningBalanceIdr: Long,
        initialGoalName: String,
        initialGoalTargetIdr: Long,
        remindersEnabled: Boolean,
        themeMode: AppThemeMode
    ) -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    val totalSteps = 5

    var displayName by rememberSaveable { mutableStateOf(initialDisplayName.ifBlank { "Eksekutif Xynoos" }) }
    var currencyCode by rememberSaveable { mutableStateOf("IDR") }
    var profileType by rememberSaveable { mutableStateOf(FinancialProfileType.COMBINED) }

    var accountName by rememberSaveable { mutableStateOf("BCA Utama") }
    var accountType by rememberSaveable { mutableStateOf(AccountType.BANK) }
    var openingBalanceInput by rememberSaveable { mutableStateOf("0") }

    var goalName by rememberSaveable { mutableStateOf("Dana Darurat & Modal Kerja") }
    var goalTargetInput by rememberSaveable { mutableStateOf("50000000") }

    var remindersEnabled by rememberSaveable { mutableStateOf(true) }
    var selectedTheme by rememberSaveable { mutableStateOf(currentThemeMode) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("onboarding_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Langkah $step dari $totalSteps",
                    style = MaterialTheme.typography.labelLarge,
                    color = EmeraldPrimary
                )
                TextButton(
                    onClick = {
                        val parsedOpening = IdrFormatter.parseIdrInput(openingBalanceInput) ?: 0L
                        val parsedGoal = IdrFormatter.parseIdrInput(goalTargetInput) ?: 0L
                        onCompleteOnboarding(
                            displayName,
                            currencyCode,
                            profileType,
                            accountName,
                            accountType,
                            parsedOpening,
                            goalName,
                            parsedGoal,
                            remindersEnabled,
                            selectedTheme
                        )
                    },
                    modifier = Modifier.testTag("skip_onboarding_button")
                ) {
                    Text("Lewati ke Dashboard")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { step.toFloat() / totalSteps.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = EmeraldPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedContent(
                targetState = step,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                label = "onboardingStep"
            ) { targetStep ->
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (targetStep) {
                        1 -> {
                            Text("Identitas & Profil Keuangan", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "Sesuaikan nama tampilan, mata uang utama (default Rupiah IDR), dan fokus pengelolaan keuangan Anda.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Nama Tampilan / Pemilik Akun") },
                                modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input"),
                                singleLine = true
                            )
                            Text("Mata Uang Default", style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("IDR", "USD", "SGD").forEach { code ->
                                    FilterChip(
                                        selected = currencyCode == code,
                                        onClick = { currencyCode = code },
                                        label = { Text(if (code == "IDR") "IDR (Rupiah)" else code) }
                                    )
                                }
                            }
                            Text("Tipe Profil Keuangan", style = MaterialTheme.typography.titleSmall)
                            FinancialProfileType.entries.forEach { type ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { profileType = type },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (profileType == type) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(type.labelId, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(type.descriptionId, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        2 -> {
                            Text("Akun & Saldo Awal", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "XynoosFinance tidak pernah mengarang saldo awal. Masukkan saldo nyata Anda saat ini atau mulai dari Rp0.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = accountName,
                                onValueChange = { accountName = it },
                                label = { Text("Nama Rekening / Dompet Pertama") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Text("Tipe Akun", style = MaterialTheme.typography.titleSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(AccountType.CASH, AccountType.BANK, AccountType.EWALLET, AccountType.BUSINESS).forEach { t ->
                                    FilterChip(
                                        selected = accountType == t,
                                        onClick = { accountType = t },
                                        label = { Text(t.labelId.split(" ").first()) }
                                    )
                                }
                            }
                            XynoosCurrencyNominalInput(
                                value = openingBalanceInput,
                                onValueChange = { openingBalanceInput = it },
                                label = "Saldo Awal Rekening Nyata (IDR)",
                                inputTestTag = "onboarding_balance_input",
                                accentColor = EmeraldPrimary,
                                helperSubtitle = "Ketik angka saldo atau pilih chip nominal di bawah"
                            )
                        }

                        3 -> {
                            Text("Target Tabungan Pertama (Opsional)", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "Tetapkan target tabungan dengan aturan alokasi eksplisit (10% dari pemasukan secara default, dapat diubah kapan saja).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = goalName,
                                onValueChange = { goalName = it },
                                label = { Text("Nama Target (Kosongkan jika ingin dilewati)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            XynoosCurrencyNominalInput(
                                value = goalTargetInput,
                                onValueChange = { goalTargetInput = it },
                                label = "Target Dana Tabungan (IDR)",
                                accentColor = SapphireSecondary,
                                helperSubtitle = "Masukkan target nominal tabungan Anda"
                            )
                        }

                        4 -> {
                            Text("Pengingat & Tema Tampilan", style = MaterialTheme.typography.headlineMedium)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = EmeraldPrimary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("Pengingat Pencatatan Harian", style = MaterialTheme.typography.titleSmall)
                                            Text("Notifikasi lokal pukul 20:00 WIB untuk menjaga akurasi buku besar.", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    Switch(
                                        checked = remindersEnabled,
                                        onCheckedChange = { enabled ->
                                            remindersEnabled = enabled
                                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        }
                                    )
                                }
                            }

                            Text("Tema Visual XynoosFinance", style = MaterialTheme.typography.titleSmall)
                            AppThemeMode.entries.forEach { mode ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedTheme = mode
                                            onThemeChanged(mode)
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedTheme == mode) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(mode.labelId, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                        if (selectedTheme == mode) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary)
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            val parsedOpening = IdrFormatter.parseIdrInput(openingBalanceInput) ?: 0L
                            val parsedGoal = IdrFormatter.parseIdrInput(goalTargetInput) ?: 0L
                            Text("Siap Memulai XynoosFinance", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "Periksa kembali ringkasan konfigurasi awal Anda sebelum masuk ke Dashboard Utama.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("• Nama: $displayName", style = MaterialTheme.typography.bodyMedium)
                                    Text("• Profil: ${profileType.labelId} ($currencyCode)", style = MaterialTheme.typography.bodyMedium)
                                    Text("• Akun Pertama: $accountName (${accountType.labelId})", style = MaterialTheme.typography.bodyMedium)
                                    Text("• Saldo Awal: ${IdrFormatter.format(parsedOpening)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    if (goalName.isNotBlank() && parsedGoal > 0L) {
                                        Text("• Target Tabungan: $goalName (${IdrFormatter.format(parsedGoal)})", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Text("• Tema: ${selectedTheme.labelId}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kembali")
                    }
                }
                Button(
                    onClick = {
                        if (step < totalSteps) {
                            step++
                        } else {
                            val parsedOpening = IdrFormatter.parseIdrInput(openingBalanceInput) ?: 0L
                            val parsedGoal = IdrFormatter.parseIdrInput(goalTargetInput) ?: 0L
                            onCompleteOnboarding(
                                displayName,
                                currencyCode,
                                profileType,
                                accountName,
                                accountType,
                                parsedOpening,
                                goalName,
                                parsedGoal,
                                remindersEnabled,
                                selectedTheme
                            )
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp).testTag("onboarding_next_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(if (step < totalSteps) "Lanjutkan" else "Masuk ke Dashboard")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}
