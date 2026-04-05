package com.dailycurator.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailycurator.data.model.AiInsight
import com.dailycurator.ui.theme.AccentGreen
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AIInsightCard(
    insight: AiInsight,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    onExpandedChange: (Boolean) -> Unit = {},
    onRegenerate: (() -> Unit)? = null,
    isRegenerating: Boolean = false,
    showRegenerate: Boolean = true,
) {
    val timeFmt = rememberInsightTimeFormatter()
    val timeLabel = insight.generatedAtEpochMillis?.let { ms ->
        val zdt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
        timeFmt.format(zdt.toLocalDateTime())
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    "ASSISTANT INSIGHT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(Modifier.weight(1f))
                if (showRegenerate && onRegenerate != null) {
                    IconButton(
                        onClick = onRegenerate,
                        enabled = !isRegenerating,
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (isRegenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate insight")
                        }
                    }
                }
                IconButton(onClick = { onExpandedChange(!expanded) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4285F4)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            if (timeLabel != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Updated $timeLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    if (insight.boldPart.isNotEmpty()) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 15.sp
                                    )
                                ) {
                                    append(insight.boldPart)
                                }
                            }
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    InsightMarkdownBody(markdown = insight.insightText)
                    if (insight.recoveryPlan != null) {
                        Spacer(Modifier.height(8.dp))
                        InsightMarkdownBody(
                            markdown = insight.recoveryPlan,
                            baseColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyGoalsInsightCard(
    insight: AiInsight,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    onExpandedChange: (Boolean) -> Unit = {},
    onRegenerate: (() -> Unit)? = null,
    isRegenerating: Boolean = false,
    showRegenerate: Boolean = true,
) {
    val timeFmt = rememberInsightTimeFormatter()
    val timeLabel = insight.generatedAtEpochMillis?.let { ms ->
        val zdt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
        timeFmt.format(zdt.toLocalDateTime())
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(AccentGreen, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(AccentGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(Modifier.size(6.dp))
                        Text(
                            "Weekly goals insight",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showRegenerate && onRegenerate != null) {
                            TextButton(onClick = onRegenerate, enabled = !isRegenerating) {
                                if (isRegenerating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Regenerate")
                                }
                            }
                        }
                        IconButton(onClick = { onExpandedChange(!expanded) }) {
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }
                }
                if (timeLabel != null) {
                    Text(
                        "Updated $timeLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AnimatedVisibility(visible = expanded) {
                    Column {
                        Spacer(Modifier.height(6.dp))
                        if (insight.boldPart.isNotEmpty()) {
                            Text(
                                insight.boldPart,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentGreen
                                )
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        InsightMarkdownBody(markdown = insight.insightText)
                        if (insight.recoveryPlan != null) {
                            Spacer(Modifier.height(6.dp))
                            InsightMarkdownBody(
                                markdown = insight.recoveryPlan,
                                baseColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberInsightTimeFormatter() = remember {
    DateTimeFormatter.ofPattern("MMM d, h:mm a")
}

@Composable
private fun InsightMarkdownBody(
    markdown: String,
    baseColor: Color? = null,
) {
    if (markdown.isBlank()) return
    val scheme = MaterialTheme.colorScheme
    val textColor = baseColor ?: scheme.onSurface
    val colors = markdownColor(
        text = textColor,
        codeText = scheme.onSurfaceVariant,
        inlineCodeText = scheme.onSurfaceVariant,
        linkText = scheme.primary,
        codeBackground = scheme.surfaceContainerHighest,
        inlineCodeBackground = scheme.surfaceContainerHigh,
    )
    Markdown(
        content = markdown,
        colors = colors,
        modifier = Modifier.fillMaxWidth(),
    )
}
