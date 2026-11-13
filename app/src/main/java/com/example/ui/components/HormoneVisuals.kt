package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.DailyLogEntity
import com.example.domain.model.DateKeyUtils
import com.example.domain.model.DateStripItem
import com.example.domain.model.HormoneBreakdown
import com.example.domain.model.HormonePathway
import com.example.domain.model.HormoneProfile
import com.example.domain.model.PersonalCorrelation
import com.example.ui.theme.HormoneColors
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalHormoneColors

fun pathwayColor(pathway: HormonePathway, colors: HormoneColors): Color = when (pathway) {
    HormonePathway.TESTOSTERONE -> colors.testosterone
    HormonePathway.CORTISOL -> colors.cortisol
    HormonePathway.DOPAMINE -> colors.dopamine
    HormonePathway.SEROTONIN -> colors.serotonin
}

fun pathwayIcon(pathway: HormonePathway): ImageVector = when (pathway) {
    HormonePathway.TESTOSTERONE -> Icons.Filled.FitnessCenter
    HormonePathway.CORTISOL -> Icons.Filled.SelfImprovement
    HormonePathway.DOPAMINE -> Icons.Filled.Bolt
    HormonePathway.SEROTONIN -> Icons.Filled.Spa
}

@Composable
fun DateStripSelector(
    items: List<DateStripItem>,
    selectedDateKey: String,
    loggedDates: Set<String>,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.dateKey }) { item ->
            val isSelected = item.dateKey == selectedDateKey
            val isLogged = item.dateKey in loggedDates
            val bgColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            }
            val fgColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = bgColor,
                border = if (item.isToday && !isSelected) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                } else null,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectDate(item.dateKey) }
                    .testTag("date_pill_${item.dateKey}")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.dayShort,
                        style = MaterialTheme.typography.labelSmall,
                        color = fgColor.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.dayNumber,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                        color = fgColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected && isLogged -> MaterialTheme.colorScheme.onPrimary
                                    isLogged -> MaterialTheme.colorScheme.primary
                                    else -> Color.Transparent
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun BioVitalityRingCluster(
    profile: HormoneProfile,
    modifier: Modifier = Modifier
) {
    val hormoneColors = LocalHormoneColors.current
    val animVitality by animateIntAsState(
        targetValue = profile.vitalityScore,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "vitality"
    )
    val tSweep by animateFloatAsState(
        targetValue = (profile.testosterone.score / 100f) * 260f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "t_sweep"
    )
    val cSweep by animateFloatAsState(
        targetValue = (profile.cortisol.score / 100f) * 260f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "c_sweep"
    )
    val dSweep by animateFloatAsState(
        targetValue = (profile.dopamine.score / 100f) * 260f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "d_sweep"
    )
    val sSweep by animateFloatAsState(
        targetValue = (profile.serotonin.score / 100f) * 260f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "s_sweep"
    )

    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)

    Box(
        modifier = modifier
            .size(196.dp)
            .semantics {
                contentDescription = "Bio-vitality score $animVitality out of 100"
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = size.minDimension
            val rings = listOf(
                Pair(hormoneColors.testosterone, tSweep),
                Pair(hormoneColors.dopamine, dSweep),
                Pair(hormoneColors.serotonin, sSweep),
                Pair(hormoneColors.cortisol, cSweep)
            )
            val strokePx = minDim * 0.055f
            val gapPx = minDim * 0.03f

            rings.forEachIndexed { index, (color, sweep) ->
                val inset = strokePx / 2f + index * (strokePx + gapPx)
                val arcSize = Size(minDim - inset * 2f, minDim - inset * 2f)
                val topLeft = Offset((size.width - arcSize.width) / 2f, (size.height - arcSize.height) / 2f)
                val startAngle = 140f

                drawArc(
                    color = trackColor,
                    startAngle = startAngle,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "VITALITY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$animVitality",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "/ 100",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HormoneMetricCard(
    breakdown: HormoneBreakdown,
    initiallyExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hormoneColors = LocalHormoneColors.current
    val accent = pathwayColor(breakdown.pathway, hormoneColors)
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val animatedProgress by animateFloatAsState(
        targetValue = (breakdown.score / 100f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "bar_${breakdown.pathway.name}"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hormone_card_${breakdown.pathway.name.lowercase()}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accent.copy(alpha = 0.16f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = pathwayIcon(breakdown.pathway),
                            contentDescription = breakdown.pathway.displayName,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = breakdown.pathway.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = breakdown.statusLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = accent
                    )
                }
                Text(
                    text = "${breakdown.score}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMono),
                    color = accent
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse factor breakdown" else "Expand factor breakdown",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(accent.copy(alpha = 0.75f), accent)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = breakdown.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "RESEARCH FACTOR ATTRIBUTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    breakdown.contributions.forEach { factor ->
                        val isPositiveEffect = if (breakdown.pathway == HormonePathway.CORTISOL) {
                            factor.delta < 0
                        } else {
                            factor.delta > 0
                        }
                        val deltaColor = when {
                            factor.delta == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                            isPositiveEffect -> hormoneColors.serotonin
                            else -> hormoneColors.cortisol
                        }
                        val prefix = if (factor.delta > 0) "+" else ""
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = factor.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = factor.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = deltaColor.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "$prefix${factor.delta}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = deltaColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
fun MultiHormoneTrendChart(
    logs: List<DailyLogEntity>,
    visiblePathways: Set<HormonePathway>,
    modifier: Modifier = Modifier
) {
    val hormoneColors = LocalHormoneColors.current
    val chronological = remember(logs) { logs.sortedBy { it.dateKey } }
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                val w = size.width
                val h = size.height
                listOf(0.25f, 0.5f, 0.75f).forEach { ratio ->
                    val y = h * (1f - ratio)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (chronological.isEmpty()) return@Canvas

                fun drawSeries(pathway: HormonePathway, color: Color, extractor: (DailyLogEntity) -> Int) {
                    if (pathway !in visiblePathways) return
                    val pts = chronological.mapIndexed { idx, entry ->
                        val x = if (chronological.size == 1) w / 2f else (idx.toFloat() / (chronological.size - 1)) * w
                        val y = h - (extractor(entry).coerceIn(0, 100) / 100f) * h
                        Offset(x, y)
                    }
                    if (pts.size >= 2) {
                        val path = Path().apply {
                            moveTo(pts.first().x, pts.first().y)
                            for (i in 1 until pts.size) {
                                val prev = pts[i - 1]
                                val curr = pts[i]
                                val midX = (prev.x + curr.x) / 2f
                                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    pts.forEach { pt ->
                        drawCircle(color = color, radius = 4.5.dp.toPx(), center = pt)
                    }
                }

                drawSeries(HormonePathway.TESTOSTERONE, hormoneColors.testosterone) { it.testosteroneScore }
                drawSeries(HormonePathway.CORTISOL, hormoneColors.cortisol) { it.cortisolScore }
                drawSeries(HormonePathway.DOPAMINE, hormoneColors.dopamine) { it.dopamineScore }
                drawSeries(HormonePathway.SEROTONIN, hormoneColors.serotonin) { it.serotoninScore }
            }
        }

        if (chronological.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val labels = if (chronological.size <= 5) {
                    chronological
                } else {
                    listOf(chronological.first(), chronological[chronological.size / 2], chronological.last())
                }
                labels.forEach { item ->
                    Text(
                        text = DateKeyUtils.shortLabel(item.dateKey),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SleepPatternChart(
    logs: List<DailyLogEntity>,
    modifier: Modifier = Modifier
) {
    val hormoneColors = LocalHormoneColors.current
    val chronological = remember(logs) { logs.sortedBy { it.dateKey }.takeLast(14) }
    val targetLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                if (chronological.isEmpty()) return@Canvas
                val maxHours = 11f
                val w = size.width
                val h = size.height

                // Optimal 7.5h reference line
                val targetY = h - (7.5f / maxHours) * h
                drawLine(
                    color = targetLineColor,
                    start = Offset(0f, targetY),
                    end = Offset(w, targetY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )

                val count = chronological.size
                val slotWidth = w / count
                val barWidth = (slotWidth * 0.56f).coerceIn(10.dp.toPx(), 28.dp.toPx())

                chronological.forEachIndexed { idx, entry ->
                    val ratio = (entry.sleepHours / maxHours).coerceIn(0.08f, 1f)
                    val barHeight = ratio * h
                    val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                    val top = h - barHeight
                    val barColor = when {
                        entry.sleepHours >= 7.5f -> hormoneColors.serotonin
                        entry.sleepHours >= 6.5f -> hormoneColors.testosterone
                        else -> hormoneColors.cortisol
                    }
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }
        if (chronological.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = DateKeyUtils.shortLabel(chronological.first().dateKey),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Dashed line = 7.5h optimal target",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = DateKeyUtils.shortLabel(chronological.last().dateKey),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PersonalCorrelationCard(
    correlation: PersonalCorrelation,
    modifier: Modifier = Modifier
) {
    val colors = LocalHormoneColors.current
    val accent = pathwayColor(correlation.targetPathway, colors)

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (correlation.isDataBacked) accent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = correlation.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = correlation.deltaLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = correlation.findingSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = correlation.researchContext,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DopamineFrictionCard(
    metrics: com.example.domain.model.DopamineFrictionMetrics,
    modifier: Modifier = Modifier
) {
    val colors = LocalHormoneColors.current
    val accent = when {
        metrics.ratio >= 2.5f -> colors.serotonin
        metrics.ratio >= 1.2f -> colors.dopamine
        else -> colors.cortisol
    }
    val total = (metrics.effortfulPoints + metrics.passivePoints).coerceAtLeast(1)
    val effortShare = (metrics.effortfulPoints.toFloat() / total.toFloat()).coerceIn(0.08f, 0.92f)

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dopamine_friction_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dopamine Friction Ratio",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = metrics.statusLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = accent
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accent.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "${metrics.ratio}x",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold
                        ),
                        color = accent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(colors.cortisol.copy(alpha = 0.45f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(effortShare)
                        .height(10.dp)
                        .background(colors.dopamine)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Effortful: ${metrics.effortfulPoints} pts",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.dopamine
                )
                Text(
                    text = "Passive: ${metrics.passivePoints} pts",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.cortisol
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = metrics.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OvertrainingWarningBanner(
    modifier: Modifier = Modifier
) {
    val colors = LocalHormoneColors.current
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.cortisol.copy(alpha = 0.14f)),
        border = BorderStroke(1.dp, colors.cortisol.copy(alpha = 0.65f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("overtraining_flag_banner")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "OVERTRAINING FLAG ACTIVE",
                style = MaterialTheme.typography.labelLarge,
                color = colors.cortisol
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "High training load paired with <6.5h or poor sleep quality applies a negative recovery modifier (-16 Testosterone, +15 Cortisol). Prioritize sleep & Zone-2 recovery.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

