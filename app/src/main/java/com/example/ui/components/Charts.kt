package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.MonthlyChartPoint
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.LocalHisabColors
import com.example.util.IndiaFormatters
import kotlin.math.max

@Composable
fun MonthlyEarningsBarChart(
    points: List<MonthlyChartPoint>,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current
    var selectedIdx by remember(points) { mutableIntStateOf((points.size - 1).coerceAtLeast(0)) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extra.cardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.monthlyEarningsGraph,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChartLegendDot(color = extra.emerald, label = strings.totalEarnings)
                    ChartLegendDot(color = extra.gold, label = strings.totalPaymentsMade)
                }
            }

            if (points.isNotEmpty() && selectedIdx in points.indices) {
                val sel = points[selectedIdx]
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${sel.label}: ${IndiaFormatters.formatInrCompact(sel.earningsPaise)} / ${IndiaFormatters.formatInrCompact(sel.paidOutPaise)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = extra.textMuted
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val maxVal = max(
                1L,
                points.maxOfOrNull { max(it.earningsPaise, it.paidOutPaise) } ?: 1L
            )
            val gridColor = extra.cardBorder

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val chartHeight = size.height - 10f
                val step = size.width / points.size.coerceAtLeast(1)

                // Subtle horizontal guidelines
                for (i in 0..2) {
                    val y = chartHeight * (i / 2f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.5f
                    )
                }

                points.forEachIndexed { idx, pt ->
                    val centerX = step * idx + step / 2f
                    val barW = (step * 0.28f).coerceIn(10f, 26f)
                    val gap = 4f

                    val earnH = ((pt.earningsPaise.toFloat() / maxVal.toFloat()) * chartHeight)
                        .coerceAtLeast(if (pt.earningsPaise > 0L) 6f else 2f)
                    val paidH = ((pt.paidOutPaise.toFloat() / maxVal.toFloat()) * chartHeight)
                        .coerceAtLeast(if (pt.paidOutPaise > 0L) 6f else 2f)

                    drawRoundRect(
                        color = if (idx == selectedIdx) extra.emerald else extra.emerald.copy(alpha = 0.72f),
                        topLeft = Offset(centerX - barW - gap / 2f, chartHeight - earnH),
                        size = Size(barW, earnH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = if (idx == selectedIdx) extra.gold else extra.gold.copy(alpha = 0.72f),
                        topLeft = Offset(centerX + gap / 2f, chartHeight - paidH),
                        size = Size(barW, paidH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                points.forEachIndexed { idx, pt ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.small)
                            .clickable { selectedIdx = idx }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pt.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (idx == selectedIdx) FontWeight.Bold else FontWeight.Normal,
                            color = if (idx == selectedIdx) MaterialTheme.colorScheme.primary else extra.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentOverviewDonutChart(
    totalEarningsPaise: Long,
    totalAdvancePaise: Long,
    totalPaidPaise: Long,
    totalBalancePaise: Long,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current

    val paidVal = totalPaidPaise.coerceAtLeast(0L)
    val advVal = totalAdvancePaise.coerceAtLeast(0L)
    val balVal = totalBalancePaise.coerceAtLeast(0L)
    val total = (paidVal + advVal + balVal).coerceAtLeast(1L)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extra.cardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = strings.paymentOverviewChart,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(124.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val hasData = paidVal + advVal + balVal > 0L
                    val trackColor = extra.cardBorder
                    Canvas(modifier = Modifier.size(116.dp)) {
                        val strokeWidth = 22f
                        val inset = strokeWidth / 2f
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        if (!hasData) {
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(width = strokeWidth)
                            )
                        } else {
                            var startAngle = -90f
                            val slices = listOf(
                                paidVal to extra.emerald,
                                advVal to extra.gold,
                                balVal to extra.crimson
                            )
                            slices.forEach { (value, color) ->
                                if (value > 0L) {
                                    val sweep = (value.toFloat() / total.toFloat()) * 360f
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        topLeft = Offset(inset, inset),
                                        size = arcSize,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweep
                                }
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = IndiaFormatters.formatInrCompact(totalEarningsPaise),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.totalEarnings,
                            style = MaterialTheme.typography.labelSmall,
                            color = extra.textMuted,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DonutStatRow(
                        color = extra.emerald,
                        label = strings.totalPaymentsMade,
                        value = IndiaFormatters.formatInr(totalPaidPaise, showDecimals = false)
                    )
                    DonutStatRow(
                        color = extra.gold,
                        label = strings.totalAdvanceGiven,
                        value = IndiaFormatters.formatInr(totalAdvancePaise, showDecimals = false)
                    )
                    DonutStatRow(
                        color = if (totalBalancePaise < 0L) extra.emerald else extra.crimson,
                        label = strings.totalOutstandingBalance,
                        value = IndiaFormatters.formatInr(totalBalancePaise, showDecimals = false)
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutStatRow(color: Color, label: String, value: String) {
    val extra = LocalHisabColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = extra.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    val extra = LocalHisabColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = extra.textMuted,
            maxLines = 1
        )
    }
}
