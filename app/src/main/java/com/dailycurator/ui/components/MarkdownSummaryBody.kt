package com.dailycurator.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor

@Composable
fun MarkdownSummaryBody(
    markdown: String,
    modifier: Modifier = Modifier,
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
        modifier = modifier.fillMaxWidth(),
    )
}
