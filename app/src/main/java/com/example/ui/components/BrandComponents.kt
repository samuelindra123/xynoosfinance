package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.CategoryBreakdownItem
import com.example.domain.IdrFormatter
import com.example.domain.TransactionModel
import com.example.domain.TransactionType
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.RefinedRed
import com.example.ui.theme.SapphireSecondary
import com.example.ui.theme.TabularAmountRow
import com.example.ui.theme.TabularBalanceLarge
import com.example.ui.theme.TabularBalanceMedium
import com.example.ui.theme.WarmAmber
import java.util.Calendar

fun parseColorHex(hex: String, fallback: Color = EmeraldPrimary): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        val longVal = clean.toLong(16)
        if (clean.length == 6) {
            Color(longVal or 0xFF000000L)
        } else {
            Color(longVal)
        }
    } catch (e: Exception) {
        fallback
    }
}

fun getCategoryVectorIcon(iconKey: String): ImageVector {
    return when (iconKey) {
        "work" -> Icons.Default.Work
        "laptop" -> Icons.Default.LaptopMac
        "store" -> Icons.Default.Storefront
        "trending_up" -> Icons.Default.TrendingUp
        "restaurant" -> Icons.Default.Restaurant
        "directions_car" -> Icons.Default.DirectionsCar
        "home" -> Icons.Default.Home
        "bolt" -> Icons.Default.Bolt
        "shopping_bag" -> Icons.Default.ShoppingBag
        "medical_services" -> Icons.Default.MedicalServices
        "school" -> Icons.Default.School
        "movie" -> Icons.Default.Movie
        "account_balance" -> Icons.Default.AccountBalance
        "business_center" -> Icons.Default.BusinessCenter
        "devices" -> Icons.Default.Devices
        else -> Icons.Default.Category
    }
}

@Composable
fun CategoryIconBadge(
    iconKey: String,
    colorHex: String,
    size: Dp = 42.dp
) {
    val accent = parseColorHex(colorHex)
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getCategoryVectorIcon(iconKey),
            contentDescription = iconKey,
            tint = accent,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

/**
 * Custom XynoosFinance geometric X + ascending financial trajectory vector mark.
 */
@Composable
fun XynoosBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    colors = listOf(ObsidianBackground, ObsidianSurfaceElevated)
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(listOf(EmeraldPrimary.copy(alpha = 0.6f), SapphireSecondary.copy(alpha = 0.6f))),
                shape = RoundedCornerShape(size * 0.28f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.18f)) {
            val w = this.size.width
            val h = this.size.height

            // Subtle ring
            drawCircle(
                color = SapphireSecondary.copy(alpha = 0.18f),
                radius = w * 0.48f,
                style = Stroke(width = w * 0.04f)
            )

            // Descending/Cross Sapphire Ribbon
            val sapphirePath = Path().apply {
                moveTo(w * 0.15f, h * 0.18f)
                lineTo(w * 0.32f, h * 0.18f)
                lineTo(w * 0.85f, h * 0.82f)
                lineTo(w * 0.68f, h * 0.82f)
                close()
            }
            drawPath(sapphirePath, color = SapphireSecondary)

            // Ascending Emerald Financial Trajectory Ribbon with Arrowhead
            val emeraldPath = Path().apply {
                moveTo(w * 0.15f, h * 0.82f)
                lineTo(w * 0.66f, h * 0.20f)
                lineTo(w * 0.52f, h * 0.20f)
                lineTo(w * 0.52f, h * 0.10f)
                lineTo(w * 0.88f, h * 0.10f)
                lineTo(w * 0.88f, h * 0.46f)
                lineTo(w * 0.78f, h * 0.46f)
                lineTo(w * 0.78f, h * 0.32f)
                lineTo(w * 0.32f, h * 0.82f)
                close()
            }
            drawPath(emeraldPath, color = EmeraldPrimary)

            // Precision center diamond node
            val diamondPath = Path().apply {
                moveTo(w * 0.50f, h * 0.40f)
                lineTo(w * 0.60f, h * 0.50f)
                lineTo(w * 0.50f, h * 0.60f)
                lineTo(w * 0.40f, h * 0.50f)
                close()
            }
            drawPath(diamondPath, color = Color(0xFFF4F7FB))
        }
    }
}

/**
 * Custom vector hero illustration for the Welcome Screen and Dashboard headers.
 */
@Composable
fun XynoosHeroIllustration(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0A1018), Color(0xFF121E2D), Color(0xFF0C2620))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(EmeraldPrimary.copy(alpha = 0.4f), SapphireSecondary.copy(alpha = 0.4f))
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Architectural perspective grid lines
            for (i in 1..5) {
                val yPos = h * (i / 6f)
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(0f, yPos),
                    end = Offset(w, yPos),
                    strokeWidth = 1f
                )
            }
            for (i in 1..7) {
                val xPos = w * (i / 8f)
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(xPos, 0f),
                    end = Offset(xPos, h),
                    strokeWidth = 1f
                )
            }

            // Glowing Sapphire wealth wave
            val wave1 = Path().apply {
                moveTo(0f, h * 0.78f)
                cubicTo(w * 0.25f, h * 0.72f, w * 0.45f, h * 0.42f, w * 0.72f, h * 0.48f)
                cubicTo(w * 0.85f, h * 0.50f, w * 0.92f, h * 0.30f, w, h * 0.24f)
            }
            drawPath(
                path = wave1,
                color = SapphireSecondary.copy(alpha = 0.7f),
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )

            // Ascending Emerald cash-flow curve
            val wave2 = Path().apply {
                moveTo(0f, h * 0.86f)
                cubicTo(w * 0.30f, h * 0.80f, w * 0.48f, h * 0.62f, w * 0.68f, h * 0.34f)
                cubicTo(w * 0.82f, h * 0.18f, w * 0.90f, h * 0.16f, w, h * 0.10f)
            }
            drawPath(
                path = wave2,
                color = EmeraldPrimary,
                style = Stroke(width = 7f, cap = StrokeCap.Round)
            )

            // Data nodes
            val points = listOf(
                Offset(w * 0.26f, h * 0.74f),
                Offset(w * 0.52f, h * 0.54f),
                Offset(w * 0.74f, h * 0.28f),
                Offset(w * 0.92f, h * 0.14f)
            )
            for (pt in points) {
                drawCircle(color = EmeraldPrimary.copy(alpha = 0.28f), radius = 14f, center = pt)
                drawCircle(color = EmeraldPrimary, radius = 6f, center = pt)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
        ) {
            Text(
                text = "PRIVATE WEALTH & AI LEDGER",
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Arus Kas, Anggaran & Analisis Deterministik IDR",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFF4F7FB)
            )
        }
    }
}

/**
 * Real Cash Flow Bar Chart (Income vs Expense across recent days/months)
 */
@Composable
fun CashFlowComparisonChart(
    incomeIdr: Long,
    expenseIdr: Long,
    transactions: List<TransactionModel>,
    isPrivacyHidden: Boolean,
    modifier: Modifier = Modifier
) {
    val maxVal = maxOf(incomeIdr, expenseIdr, 1L).toFloat()
    val incomeRatio by animateFloatAsState(
        targetValue = (incomeIdr.toFloat() / maxVal).coerceIn(0.04f, 1f),
        animationSpec = tween(500),
        label = "incomeBar"
    )
    val expenseRatio by animateFloatAsState(
        targetValue = (expenseIdr.toFloat() / maxVal).coerceIn(0.04f, 1f),
        animationSpec = tween(500),
        label = "expenseBar"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Arus Kas: Pemasukan vs Pengeluaran",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${transactions.size} Transaksi",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            // Horizontal comparison bars
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Pemasukan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = IdrFormatter.format(incomeIdr, isPrivacyHidden = isPrivacyHidden, showPlusSign = true),
                            style = TabularAmountRow,
                            color = EmeraldPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (incomeIdr == 0L) 0.02f else incomeRatio)
                                .height(12.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary)
                        )
                    }
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Pengeluaran Bersih",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = IdrFormatter.format(expenseIdr, isPrivacyHidden = isPrivacyHidden),
                            style = TabularAmountRow,
                            color = RefinedRed
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (expenseIdr == 0L) 0.02f else expenseRatio)
                                .height(12.dp)
                                .clip(CircleShape)
                                .background(RefinedRed)
                        )
                    }
                }
            }

            // Daily bucket mini-bars from real database transactions
            if (transactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                val cal = Calendar.getInstance()
                val buckets = (6 downTo 0).map { daysAgo ->
                    cal.timeInMillis = System.currentTimeMillis()
                    cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                    val targetDay = cal.get(Calendar.DAY_OF_YEAR)
                    val targetYear = cal.get(Calendar.YEAR)
                    val dayTxs = transactions.filter { tx ->
                        val c2 = Calendar.getInstance().apply { timeInMillis = tx.transactionDateMillis }
                        c2.get(Calendar.DAY_OF_YEAR) == targetDay && c2.get(Calendar.YEAR) == targetYear
                    }
                    val inc = dayTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountIdr }
                    val exp = dayTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountIdr }
                    Pair(inc, exp)
                }
                val maxBucket = buckets.maxOfOrNull { maxOf(it.first, it.second) }?.coerceAtLeast(1L) ?: 1L

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    val slotWidth = size.width / buckets.size
                    val barW = slotWidth * 0.26f
                    buckets.forEachIndexed { index, (inc, exp) ->
                        val centerX = slotWidth * index + slotWidth / 2f
                        val incH = ((inc.toFloat() / maxBucket.toFloat()) * (size.height - 8f)).coerceAtLeast(4f)
                        val expH = ((exp.toFloat() / maxBucket.toFloat()) * (size.height - 8f)).coerceAtLeast(4f)

                        drawRoundRect(
                            color = EmeraldPrimary,
                            topLeft = Offset(centerX - barW - 2f, size.height - incH),
                            size = Size(barW, incH),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        drawRoundRect(
                            color = RefinedRed,
                            topLeft = Offset(centerX + 2f, size.height - expH),
                            size = Size(barW, expH),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Real Category Distribution Donut & Breakdown Card
 */
@Composable
fun CategoryDistributionCard(
    breakdown: List<CategoryBreakdownItem>,
    isPrivacyHidden: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Distribusi Pengeluaran per Kategori",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (breakdown.isEmpty()) {
                Text(
                    text = "Belum ada pengeluaran tercatat pada periode ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Canvas(modifier = Modifier.size(110.dp)) {
                        var startAngle = -90f
                        val strokeWidth = 22.dp.toPx()
                        breakdown.forEach { item ->
                            val sweep = (item.percentage / 100f) * 360f
                            drawArc(
                                color = parseColorHex(item.colorHex),
                                startAngle = startAngle,
                                sweepAngle = sweep.coerceAtLeast(2f),
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            startAngle += sweep
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        breakdown.take(4).forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(parseColorHex(item.colorHex))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.categoryName,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "${item.percentage.toInt()}% • ${IdrFormatter.formatCompact(item.amountIdr, isPrivacyHidden)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun XynoosEmptyStateCard(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            XynoosBrandLogo(size = 54.dp)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

/**
 * Professional Stock-Market Style OHLC Candlestick + Area Chart for Live USD/IDR (1-Minute Interval).
 */
@Composable
fun StockMarketCandlestickChart(
    usdMarket: com.example.data.remote.LiveUsdMarketState,
    availableCashIdr: Long,
    netWorthIdr: Long,
    isPrivacyHidden: Boolean,
    chartHeight: Dp = 200.dp,
    onOpenFullTerminal: (() -> Unit)? = null,
    onRefreshNow: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isPositive = usdMarket.changeIdr >= 0.0
    val badgeColor = if (isPositive) EmeraldPrimary else RefinedRed
    val changeSign = if (isPositive) "+" else ""

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C131D)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(EmeraldPrimary.copy(alpha = 0.45f), SapphireSecondary.copy(alpha = 0.45f))
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(18.dp)
        ) {
            // Top Terminal Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary)
                        )
                        Text(
                            text = "LIVE FOREX & STOCK TERMINAL • USD/IDR (1M)",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${IdrFormatter.format(usdMarket.currentRateIdr.toLong())} / $1 USD",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFFF4F7FB),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$changeSign${String.format(java.util.Locale.US, "%.2f", usdMarket.changeIdr)} (${changeSign}${String.format(java.util.Locale.US, "%.2f", usdMarket.changePercent)}%)",
                            style = MaterialTheme.typography.labelMedium,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SapphireSecondary.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Update ${usdMarket.nextUpdateSecondsRemaining}d",
                        style = MaterialTheme.typography.labelSmall,
                        color = SapphireSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real-Time Portfolio Value in USD Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF131E2E))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Saldo Kas (Real USD)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF98A3B3)
                    )
                    Text(
                        text = IdrFormatter.formatUsdFromIdr(availableCashIdr, usdMarket.currentRateIdr, isPrivacyHidden),
                        style = TabularAmountRow,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Kekayaan Bersih (Real USD)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF98A3B3)
                    )
                    Text(
                        text = IdrFormatter.formatUsdFromIdr(netWorthIdr, usdMarket.currentRateIdr, isPrivacyHidden),
                        style = TabularAmountRow,
                        color = SapphireSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stock-Style OHLC Candlestick & Glowing Line Canvas
            val candles = usdMarket.candles
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF080D14))
                    .padding(10.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Horizontal Grid Lines
                    for (i in 0..4) {
                        val y = h * (i / 4f)
                        drawLine(
                            color = Color.White.copy(alpha = 0.06f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }
                    // Vertical Grid Lines
                    for (i in 0..5) {
                        val x = w * (i / 5f)
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1f
                        )
                    }

                    if (candles.size >= 2) {
                        val minPrice = candles.minOf { it.lowIdr }
                        val maxPrice = candles.maxOf { it.highIdr }
                        val priceSpan = (maxPrice - minPrice).coerceAtLeast(1.0)
                        val slotW = w / candles.size.toFloat()
                        val bodyW = (slotW * 0.58f).coerceAtLeast(4f)

                        val closeLinePath = Path()
                        val areaPath = Path()

                        candles.forEachIndexed { idx, c ->
                            val cx = slotW * idx + slotW / 2f
                            val highY = (h - ((c.highIdr - minPrice) / priceSpan).toFloat() * h).coerceIn(2f, h - 2f)
                            val lowY = (h - ((c.lowIdr - minPrice) / priceSpan).toFloat() * h).coerceIn(2f, h - 2f)
                            val openY = (h - ((c.openIdr - minPrice) / priceSpan).toFloat() * h).coerceIn(2f, h - 2f)
                            val closeY = (h - ((c.closeIdr - minPrice) / priceSpan).toFloat() * h).coerceIn(2f, h - 2f)

                            val candleColor = if (c.isBullish) EmeraldPrimary else RefinedRed

                            // High-Low Wick
                            drawLine(
                                color = candleColor.copy(alpha = 0.85f),
                                start = Offset(cx, highY),
                                end = Offset(cx, lowY),
                                strokeWidth = 2f
                            )

                            // Open-Close Body
                            val topY = minOf(openY, closeY)
                            val botY = maxOf(openY, closeY)
                            val bodyHeight = (botY - topY).coerceAtLeast(3f)
                            drawRoundRect(
                                color = candleColor,
                                topLeft = Offset(cx - bodyW / 2f, topY),
                                size = Size(bodyW, bodyHeight),
                                cornerRadius = CornerRadius(2f, 2f)
                            )

                            if (idx == 0) {
                                closeLinePath.moveTo(cx, closeY)
                                areaPath.moveTo(cx, h)
                                areaPath.lineTo(cx, closeY)
                            } else {
                                closeLinePath.lineTo(cx, closeY)
                                areaPath.lineTo(cx, closeY)
                            }
                            if (idx == candles.lastIndex) {
                                areaPath.lineTo(cx, h)
                                areaPath.close()
                            }
                        }

                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(SapphireSecondary.copy(alpha = 0.18f), Color.Transparent)
                            )
                        )
                        drawPath(
                            path = closeLinePath,
                            color = SapphireSecondary.copy(alpha = 0.75f),
                            style = Stroke(width = 2.2f, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "High: ${IdrFormatter.format(usdMarket.dayHighIdr.toLong())} • Low: ${IdrFormatter.format(usdMarket.dayLowIdr.toLong())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF98A3B3)
                )
                if (onOpenFullTerminal != null) {
                    Button(
                        onClick = onOpenFullTerminal,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary.copy(alpha = 0.2f),
                            contentColor = EmeraldPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buka Terminal USD 1m", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                } else if (onRefreshNow != null) {
                    Button(
                        onClick = onRefreshNow,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SapphireSecondary.copy(alpha = 0.2f),
                            contentColor = SapphireSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Refresh Kurs Sekarang", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Financial Balance & Nominal Counter Input UI ("Layar Hitung Nominal Saldo").
 * Eliminates zero-counting confusion by providing:
 * 1. Live digital banking balance readout ("Rp 15.000.000") + USD equivalent
 * 2. Scale & digit counter badge ("JUTAAN (Jt) • 8 Digit")
 * 3. Mathematical unit breakdown ("15 Juta + 250 Ribu") & Indonesian Terbilang ("Lima Belas Juta...")
 * 4. Automatic thousand-dot separator formatting as the user types
 * 5. One-tap denomination chips (+50 Rb, +100 Rb, +1 Jt, ×1.000) & interactive numeric keypad
 */
@Composable
fun XynoosCurrencyNominalInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    inputTestTag: String? = null,
    accentColor: Color = EmeraldPrimary,
    usdRateIdr: Double = 15850.0,
    compactMode: Boolean = false,
    showQuickChips: Boolean = true,
    helperSubtitle: String? = null
) {
    var showCalculatorPad by remember { mutableStateOf(false) }
    val parsedAmount = remember(value) { IdrFormatter.parseIdrInput(value) ?: 0L }
    val scaleBadge = remember(parsedAmount) { IdrFormatter.formatNominalScaleBadge(parsedAmount) }
    val digitBreakdown = remember(parsedAmount) { IdrFormatter.formatDigitBreakdown(parsedAmount) }
    val terbilangText = remember(parsedAmount) { IdrFormatter.formatTerbilang(parsedAmount) }
    val usdEquivalent = remember(parsedAmount, usdRateIdr) {
        IdrFormatter.formatUsdFromIdr(parsedAmount, usdRateIdr, isPrivacyHidden = false)
    }

    fun updateWithAmount(newAmount: Long) {
        val safe = newAmount.coerceIn(0L, 999_999_999_999_999L)
        onValueChange(if (safe == 0L) "0" else IdrFormatter.formatGroupedInputString(safe.toString()))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A111A)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.2.dp,
                    brush = Brush.horizontalGradient(
                        listOf(accentColor.copy(alpha = 0.65f), SapphireSecondary.copy(alpha = 0.45f))
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(if (compactMode) 12.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Top Bar: Label + Scale & Digit Counter Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFBAC6D8),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = scaleBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 2. Digital Banking Balance Counter Screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF101B29), Color(0xFF0B131E))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = IdrFormatter.format(parsedAmount),
                            style = if (compactMode) TabularBalanceMedium else TabularBalanceLarge,
                            color = if (parsedAmount > 0L) accentColor else Color(0xFF6E7D91),
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "≈ $usdEquivalent",
                            style = MaterialTheme.typography.labelSmall,
                            color = SapphireSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Unit Arithmetic Breakdown ("Hitungan: 15 Juta + 250 Ribu")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SapphireSecondary.copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "HITUNGAN",
                                style = MaterialTheme.typography.labelSmall,
                                color = SapphireSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = digitBreakdown,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFE6EDF7),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Formal Indonesian Terbilang Readout
                    Text(
                        text = "Terbilang: $terbilangText",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF98A8BC),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 3. Auto-Formatted Text Input with Rp Prefix, Clear Button, and Keypad Toggle
            val fieldModifier = if (inputTestTag != null) {
                Modifier.fillMaxWidth().testTag(inputTestTag)
            } else {
                Modifier.fillMaxWidth()
            }

            OutlinedTextField(
                value = value,
                onValueChange = { rawInput ->
                    val formatted = IdrFormatter.formatGroupedInputString(rawInput)
                    onValueChange(formatted)
                },
                label = {
                    Text(
                        helperSubtitle ?: "Ketik Angka Nominal (Otomatis Titik Ribuan / Jutaan)",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                prefix = {
                    Text(
                        text = "Rp ",
                        style = TabularAmountRow,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (value.isNotEmpty() && value != "0") {
                            IconButton(
                                onClick = { onValueChange("") },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Reset Nominal",
                                    tint = Color(0xFF98A3B3),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { showCalculatorPad = !showCalculatorPad },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.Default.Calculate,
                                contentDescription = "Buka Tombol Hitung Nominal",
                                tint = if (showCalculatorPad) accentColor else SapphireSecondary
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = TabularBalanceMedium.copy(color = Color(0xFFF4F7FB), fontWeight = FontWeight.Bold),
                modifier = fieldModifier
            )

            // 4. Quick Nominal Adder & Zero-Multiplier Chips
            if (showQuickChips) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val quickActions = listOf(
                        "×1.000 (Ribu)" to {
                            val base = if (parsedAmount == 0L) 1L else parsedAmount
                            updateWithAmount(base * 1_000L)
                        },
                        "×1 Juta" to {
                            val base = if (parsedAmount == 0L) 1L else parsedAmount
                            updateWithAmount(base * 1_000_000L)
                        },
                        "+50 Rb" to { updateWithAmount(parsedAmount + 50_000L) },
                        "+100 Rb" to { updateWithAmount(parsedAmount + 100_000L) },
                        "+500 Rb" to { updateWithAmount(parsedAmount + 500_000L) },
                        "+1 Jt" to { updateWithAmount(parsedAmount + 1_000_000L) },
                        "+5 Jt" to { updateWithAmount(parsedAmount + 5_000_000L) },
                        "+10 Jt" to { updateWithAmount(parsedAmount + 10_000_000L) },
                        "Reset Rp0" to { onValueChange("0") }
                    )

                    quickActions.forEach { (chipLabel, action) ->
                        SuggestionChip(
                            onClick = action,
                            label = {
                                Text(
                                    text = chipLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = Color(0xFF142132),
                                labelColor = if (chipLabel.startsWith("×")) accentColor else Color(0xFFE6EDF7)
                            )
                        )
                    }
                }
            }

            // 5. Expandable Interactive Banking Keypad ("Mesin Hitung Saldo")
            AnimatedVisibility(visible = showCalculatorPad) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF111C2B))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "KEYPAD HITUNG NOMINAL SALDO",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tutup Keypad",
                            style = MaterialTheme.typography.labelSmall,
                            color = SapphireSecondary,
                            modifier = Modifier.clickable { showCalculatorPad = false }
                        )
                    }

                    val appendDigits: (String) -> Unit = { digits ->
                        val currentDigits = parsedAmount.toString().let { if (it == "0") "" else it }
                        val combined = (currentDigits + digits).take(15)
                        val nextVal = combined.toLongOrNull() ?: 0L
                        updateWithAmount(nextVal)
                    }

                    val padRows = listOf(
                        listOf(
                            "7" to { appendDigits("7") },
                            "8" to { appendDigits("8") },
                            "9" to { appendDigits("9") },
                            "⌫ Hapus" to { updateWithAmount(parsedAmount / 10L) }
                        ),
                        listOf(
                            "4" to { appendDigits("4") },
                            "5" to { appendDigits("5") },
                            "6" to { appendDigits("6") },
                            "+100 Rb" to { updateWithAmount(parsedAmount + 100_000L) }
                        ),
                        listOf(
                            "1" to { appendDigits("1") },
                            "2" to { appendDigits("2") },
                            "3" to { appendDigits("3") },
                            "+1 Juta" to { updateWithAmount(parsedAmount + 1_000_000L) }
                        ),
                        listOf(
                            "0" to { appendDigits("0") },
                            "000" to { appendDigits("000") },
                            "000.000" to { appendDigits("000000") },
                            "Reset" to { updateWithAmount(0L) }
                        )
                    )

                    padRows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowItems.forEach { (btnText, onClickAction) ->
                                val isAction = btnText.startsWith("+") || btnText.startsWith("⌫") || btnText == "Reset" || btnText.length >= 3
                                OutlinedButton(
                                    onClick = onClickAction,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isAction) accentColor.copy(alpha = 0.14f) else Color(0xFF0B131E),
                                        contentColor = if (isAction) accentColor else Color(0xFFF4F7FB)
                                    )
                                ) {
                                    Text(
                                        text = btnText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


