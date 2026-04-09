package com.dailycurator.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

enum class MarkdownSummaryStyle {
    /** Library defaults (headings can be large). */
    Default,
    /** Smaller, color-tinted headings and comfortable body for long digests (e.g. Gmail summary). */
    CompactRich,
}

@Composable
fun MarkdownSummaryBody(
    markdown: String,
    modifier: Modifier = Modifier,
    baseColor: Color? = null,
    style: MarkdownSummaryStyle = MarkdownSummaryStyle.Default,
) {
    if (markdown.isBlank()) return
    val scheme = MaterialTheme.colorScheme
    val typo = MaterialTheme.typography
    val textColor = baseColor ?: scheme.onSurface
    val colors = markdownColor(
        text = textColor,
        codeText = scheme.onSurfaceVariant,
        inlineCodeText = scheme.onSurfaceVariant,
        linkText = scheme.secondary,
        codeBackground = scheme.surfaceContainerHighest,
        inlineCodeBackground = scheme.surfaceContainerHigh,
    )
    val body = typo.bodyMedium.copy(color = textColor)
    val typography = when (style) {
        MarkdownSummaryStyle.Default -> markdownTypography(
            h1 = typo.headlineSmall.copy(color = textColor),
            h2 = typo.titleLarge.copy(color = textColor),
            h3 = typo.titleMedium.copy(color = textColor),
            h4 = typo.titleSmall.copy(color = textColor),
            h5 = typo.bodyLarge.copy(
                color = textColor,
                fontWeight = FontWeight.SemiBold,
            ),
            h6 = typo.bodyMedium.copy(
                color = textColor,
                fontWeight = FontWeight.Medium,
            ),
            text = body,
            code = body.copy(fontFamily = FontFamily.Monospace),
            quote = body.copy(fontStyle = FontStyle.Italic),
            paragraph = body,
            ordered = body,
            bullet = body,
            list = body,
        )
        MarkdownSummaryStyle.CompactRich -> markdownTypography(
            h1 = typo.titleLarge.copy(
                color = scheme.primary,
                fontWeight = FontWeight.SemiBold,
            ),
            h2 = typo.titleMedium.copy(
                color = scheme.tertiary,
                fontWeight = FontWeight.SemiBold,
            ),
            h3 = typo.titleSmall.copy(
                color = scheme.secondary,
                fontWeight = FontWeight.Medium,
            ),
            h4 = typo.labelLarge.copy(
                color = scheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            ),
            h5 = typo.bodyLarge.copy(
                color = scheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            ),
            h6 = typo.bodyMedium.copy(
                color = scheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            ),
            text = body,
            code = body.copy(fontFamily = FontFamily.Monospace),
            quote = body.copy(
                fontStyle = FontStyle.Italic,
                color = scheme.onSurfaceVariant,
            ),
            paragraph = body,
            ordered = body,
            bullet = body,
            list = body,
        )
    }
    Markdown(
        content = markdown,
        colors = colors,
        typography = typography,
        modifier = modifier.fillMaxWidth(),
    )
}
