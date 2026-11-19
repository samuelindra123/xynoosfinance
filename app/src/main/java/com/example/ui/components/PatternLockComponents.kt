package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.RefinedRed
import com.example.ui.theme.SapphireSecondary
import java.security.MessageDigest
import kotlin.math.hypot

object PatternLockUtil {
    private const val SALT_PREFIX = "xynoos_pattern_v1:"

    fun encodePattern(dots: List<Int>): String = dots.joinToString("-")

    fun hashPattern(dots: List<Int>): String {
        val raw = "$SALT_PREFIX${encodePattern(dots)}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verifyPattern(dots: List<Int>, storedHash: String): Boolean {
        if (dots.size < 4 || storedHash.isBlank()) return false
        return hashPattern(dots) == storedHash
    }

    /**
     * Standard 3x3 Android pattern midpoint rule:
     * When dragging between two corners/edges directly over a middle dot, automatically connect the midpoint.
     */
    fun resolveIntermediateDot(from: Int, to: Int): Int? {
        if (from !in 0..8 || to !in 0..8 || from == to) return null
        val r1 = from / 3
        val c1 = from % 3
        val r2 = to / 3
        val c2 = to % 3
        val dr = r2 - r1
        val dc = c2 - c1
        return if (dr % 2 == 0 && dc % 2 == 0) {
            val midR = (r1 + r2) / 2
            val midC = (c1 + c2) / 2
            midR * 3 + midC
        } else {
            null
        }
    }
}

/**
 * Interactive 3x3 Dot-Grid Pattern Pad using Jetpack Compose Canvas + drag gesture + haptic feedback.
 */
@Composable
fun PatternLockPad(
    onPatternCompleted: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    accentColor: Color = EmeraldPrimary
) {
    val haptic = LocalHapticFeedback.current
    val selectedDots = remember { mutableStateListOf<Int>() }
    var currentDragOffset by remember { mutableStateOf<Offset?>(null) }
    var padSize by remember { mutableStateOf(IntSize.Zero) }

    val activeColor by animateColorAsState(
        targetValue = if (isError) RefinedRed else accentColor,
        label = "patternPadColor"
    )

    fun dotCenter(index: Int, width: Float, height: Float): Offset {
        val row = index / 3
        val col = index % 3
        val cellW = width / 3f
        val cellH = height / 3f
        return Offset(col * cellW + cellW / 2f, row * cellH + cellH / 2f)
    }

    fun hitDot(point: Offset, width: Float, height: Float): Int? {
        val hitRadius = minOf(width, height) / 7.2f
        for (idx in 0..8) {
            val center = dotCenter(idx, width, height)
            val dist = hypot(point.x - center.x, point.y - center.y)
            if (dist <= hitRadius) return idx
        }
        return null
    }

    fun trySelectDot(idx: Int) {
        if (idx !in selectedDots) {
            val last = selectedDots.lastOrNull()
            if (last != null) {
                val mid = PatternLockUtil.resolveIntermediateDot(last, idx)
                if (mid != null && mid !in selectedDots) {
                    selectedDots.add(mid)
                }
            }
            selectedDots.add(idx)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0E1623))
            .border(1.dp, activeColor.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
            .onSizeChanged { padSize = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        selectedDots.clear()
                        currentDragOffset = startOffset
                        val w = padSize.width.toFloat()
                        val h = padSize.height.toFloat()
                        if (w > 0f && h > 0f) {
                            hitDot(startOffset, w, h)?.let { trySelectDot(it) }
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentDragOffset = change.position
                        val w = padSize.width.toFloat()
                        val h = padSize.height.toFloat()
                        if (w > 0f && h > 0f) {
                            hitDot(change.position, w, h)?.let { trySelectDot(it) }
                        }
                    },
                    onDragEnd = {
                        currentDragOffset = null
                        if (selectedDots.isNotEmpty()) {
                            onPatternCompleted(selectedDots.toList())
                        }
                        selectedDots.clear()
                    },
                    onDragCancel = {
                        currentDragOffset = null
                        selectedDots.clear()
                    }
                )
            }
            .testTag("pattern_lock_pad")
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            val w = size.width
            val h = size.height

            // Connecting lines between selected dots
            if (selectedDots.isNotEmpty()) {
                val path = Path()
                selectedDots.forEachIndexed { i, dotIdx ->
                    val c = dotCenter(dotIdx, w, h)
                    if (i == 0) path.moveTo(c.x, c.y) else path.lineTo(c.x, c.y)
                }
                val dragPos = currentDragOffset
                if (dragPos != null) {
                    val padPx = 14.dp.toPx()
                    path.lineTo((dragPos.x - padPx).coerceIn(0f, w), (dragPos.y - padPx).coerceIn(0f, h))
                }
                drawPath(
                    path = path,
                    color = activeColor.copy(alpha = 0.78f),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 9 Pattern Nodes (0..8)
            for (idx in 0..8) {
                val center = dotCenter(idx, w, h)
                val isSelected = idx in selectedDots

                if (isSelected) {
                    drawCircle(
                        color = activeColor.copy(alpha = 0.20f),
                        radius = 22.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = activeColor.copy(alpha = 0.55f),
                        radius = 22.dp.toPx(),
                        center = center,
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                } else {
                    drawCircle(
                        color = Color(0xFF1A2738),
                        radius = 16.dp.toPx(),
                        center = center
                    )
                }

                drawCircle(
                    color = if (isSelected) activeColor else Color(0xFF7D8DA3),
                    radius = if (isSelected) 9.dp.toPx() else 6.dp.toPx(),
                    center = center
                )
            }
        }
    }
}

/**
 * Full-screen Pattern Lock Gate blocking access until the user draws the saved pattern.
 */
@Composable
fun PatternLockGateScreen(
    displayName: String,
    storedPatternHash: String,
    onUnlocked: () -> Unit,
    onForgotPatternSignOut: () -> Unit
) {
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    var showResetConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pattern_lock_gate_screen"),
        color = ObsidianBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            XynoosBrandLogo(size = 64.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(EmeraldPrimary.copy(alpha = 0.14f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                    Text(
                        text = "KUNCI KEAMANAN POLA AKTIF",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Selamat datang kembali${if (displayName.isNotBlank()) ", $displayName" else ""}",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFF4F7FB),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMsg ?: "Gambar pola keamanan (minimal 4 titik) untuk membuka XynoosFinance.",
                style = MaterialTheme.typography.bodySmall,
                color = if (errorMsg != null) RefinedRed else Color(0xFF98A3B3),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            Box(modifier = Modifier.widthIn(max = 310.dp)) {
                PatternLockPad(
                    isError = errorMsg != null,
                    onPatternCompleted = { dots ->
                        if (dots.size < 4) {
                            errorMsg = "Hubungkan minimal 4 titik pola."
                        } else if (PatternLockUtil.verifyPattern(dots, storedPatternHash)) {
                            errorMsg = null
                            failedAttempts = 0
                            onUnlocked()
                        } else {
                            failedAttempts += 1
                            errorMsg = "Pola tidak cocok (Percobaan ke-$failedAttempts). Coba lagi."
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            TextButton(
                onClick = { showResetConfirm = true },
                modifier = Modifier.testTag("forgot_pattern_button")
            ) {
                Text(
                    text = "Lupa Pola? Verifikasi Ulang via Google Sign-In",
                    style = MaterialTheme.typography.labelMedium,
                    color = SapphireSecondary
                )
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Password Tipe Pola?") },
            text = {
                Text("Untuk menjaga keamanan buku besar Anda, reset pola memerlukan keluar (Sign Out) lalu masuk kembali menggunakan Akun Google terverifikasi Anda. Data cloud & lokal tetap aman.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onForgotPatternSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SapphireSecondary, contentColor = Color(0xFF081018))
                ) {
                    Text("Keluar & Verifikasi Google", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

/**
 * 2-Step Interactive Dialog to create or change the 3x3 Pattern Lock.
 */
@Composable
fun SetupPatternLockDialog(
    isChangingExisting: Boolean,
    onDismiss: () -> Unit,
    onPatternSaved: (String) -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1 = Draw new pattern, 2 = Confirm pattern
    var firstPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var statusText by remember {
        mutableStateOf("Hubungkan minimal 4 titik untuk membuat pola keamanan baru.")
    }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Pattern, contentDescription = null, tint = EmeraldPrimary)
                Text(
                    text = when {
                        step == 2 -> "Konfirmasi Pola Anda"
                        isChangingExisting -> "Ubah Password Tipe Pola"
                        else -> "Buat Password Tipe Pola"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isError) RefinedRed.copy(alpha = 0.14f) else EmeraldPrimary.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) RefinedRed else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                    )
                }

                Box(modifier = Modifier.widthIn(max = 260.dp)) {
                    PatternLockPad(
                        isError = isError,
                        accentColor = if (step == 1) SapphireSecondary else EmeraldPrimary,
                        onPatternCompleted = { dots ->
                            if (step == 1) {
                                if (dots.size < 4) {
                                    isError = true
                                    statusText = "Terlalu pendek! Minimal harus menghubungkan 4 titik."
                                } else {
                                    firstPattern = dots
                                    step = 2
                                    isError = false
                                    statusText = "Bagus (${dots.size} titik)! Gambar ulang pola yang sama untuk konfirmasi."
                                }
                            } else {
                                if (dots == firstPattern) {
                                    val hashed = PatternLockUtil.hashPattern(dots)
                                    onPatternSaved(hashed)
                                } else {
                                    isError = true
                                    statusText = "Pola konfirmasi tidak cocok! Silakan gambar ulang pola yang sama."
                                }
                            }
                        }
                    )
                }

                if (step == 2) {
                    OutlinedButton(
                        onClick = {
                            step = 1
                            firstPattern = emptyList()
                            isError = false
                            statusText = "Silakan gambar pola baru minimal 4 titik."
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Ulangi Dari Awal")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
