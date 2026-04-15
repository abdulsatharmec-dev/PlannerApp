package com.dailycurator.data.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement
import com.tom_roush.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureNode
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.text.Normalizer

object PdfTextExtractor {

    /**
     * Tries several PDFTextStripper configurations plus optional **tagged-PDF structure** text
     * (`/ActualText` on structure elements). Adobe-style “reading friendly” PDFs often populate that tree
     * even when raw stream extraction is wrong.
     */
    fun extractBestPageText(document: PDDocument, pageIndex0: Int): String {
        if (pageIndex0 !in 0 until document.numberOfPages) return ""
        val candidates = STRIPPER_VARIANTS.mapNotNull { cfg ->
            runCatching { extractPageText(document, pageIndex0, cfg) }.getOrNull()
        }.toMutableList()
        runCatching {
            val tagged = extractTaggedStructureActualText(document, pageIndex0)
            if (tagged.isNotBlank()) {
                candidates.add(tagged)
            }
        }
        if (candidates.isEmpty()) return ""
        val best = candidates.maxByOrNull { scoreExtractedText(it) } ?: return ""
        return Normalizer.normalize(best, Normalizer.Form.NFC).trim()
    }

    /**
     * True when the string still looks like broken encoding / layout (common when the PDF lacks a proper ToUnicode map).
     * Adobe Acrobat “Liquid Mode” uses different tech; PDFBox cannot match that for many files.
     */
    fun isProbablyCorruptExtraction(s: String): Boolean {
        if (s.length < 12) return false
        val score = scoreExtractedText(s)
        val minExpected = 25 + s.length / 6
        if (score < minExpected) return true
        val repl = s.count { it == '\uFFFD' }
        if (repl >= 2 && repl * 20 >= s.length) return true
        val pua = s.count { it.code in 0xE000..0xF8FF }
        if (pua * 10 >= s.length) return true
        val ind = indicLetterCount(s)
        val latin = s.count { it in 'A'..'Z' || it in 'a'..'z' }
        // Long text with many Latin letters but almost no Indic — often wrong mapping for a Malayalam PDF
        if (s.length > 100 && ind < 3 && latin * 3 > s.length) return true
        return false
    }

    private data class StripperVariant(
        val sortByPosition: Boolean,
        val separateByBeads: Boolean,
        val suppressDuplicate: Boolean,
    )

    private val STRIPPER_VARIANTS = listOf(
        StripperVariant(sortByPosition = true, separateByBeads = true, suppressDuplicate = true),
        StripperVariant(sortByPosition = true, separateByBeads = false, suppressDuplicate = true),
        StripperVariant(sortByPosition = false, separateByBeads = false, suppressDuplicate = true),
        StripperVariant(sortByPosition = true, separateByBeads = false, suppressDuplicate = false),
        StripperVariant(sortByPosition = false, separateByBeads = false, suppressDuplicate = false),
        StripperVariant(sortByPosition = false, separateByBeads = true, suppressDuplicate = true),
        StripperVariant(sortByPosition = true, separateByBeads = true, suppressDuplicate = false),
    )

    private fun extractPageText(
        document: PDDocument,
        pageIndex0: Int,
        cfg: StripperVariant,
    ): String {
        val stripper = PDFTextStripper()
        stripper.sortByPosition = cfg.sortByPosition
        stripper.setShouldSeparateByBeads(cfg.separateByBeads)
        stripper.setSuppressDuplicateOverlappingText(cfg.suppressDuplicate)
        stripper.startPage = pageIndex0 + 1
        stripper.endPage = pageIndex0 + 1
        return stripper.getText(document).trim()
    }

    /**
     * Collects `/ActualText` from tagged structure elements on [targetPage].
     * Reflow-oriented PDFs (including many Adobe “reading mode” friendly files) often store correct Unicode here.
     */
    private fun extractTaggedStructureActualText(document: PDDocument, pageIndex0: Int): String {
        val root = document.documentCatalog.structureTreeRoot ?: return ""
        val targetPage = document.getPage(pageIndex0)
        val sb = StringBuilder()
        walkStructureForActualText(root, targetPage, null, sb)
        return sb.toString().trim()
    }

    private fun walkStructureForActualText(
        node: PDStructureNode,
        targetPage: PDPage,
        inheritedPage: PDPage?,
        sb: StringBuilder,
    ) {
        var effectivePage = inheritedPage
        if (node is PDStructureElement) {
            val own = node.page
            if (own != null) {
                effectivePage = own
            }
            if (effectivePage != null && samePdfPage(effectivePage, targetPage)) {
                val at = node.actualText?.trim()
                if (!at.isNullOrEmpty()) {
                    if (sb.isNotEmpty()) sb.append('\n')
                    sb.append(at)
                }
            }
        }
        for (kid in node.kids) {
            if (kid is PDStructureNode) {
                walkStructureForActualText(kid, targetPage, effectivePage, sb)
            }
        }
    }

    private fun samePdfPage(a: PDPage, b: PDPage): Boolean =
        a.cosObject == b.cosObject

    private fun indicLetterCount(s: String): Int {
        var n = 0
        for (ch in s) {
            val c = ch.code
            when {
                c in MALAYALAM_START..MALAYALAM_END -> n++
                c in TAMIL_START..TAMIL_END -> n++
                c in DEVANAGARI_START..DEVANAGARI_END -> n++
            }
        }
        return n
    }

    private fun scoreExtractedText(s: String): Int {
        if (s.isEmpty()) return 0
        var score = 0
        var latin1Supplement = 0
        for (ch in s) {
            val c = ch.code
            when {
                c in MALAYALAM_START..MALAYALAM_END -> score += 10
                c in TAMIL_START..TAMIL_END -> score += 5
                c in DEVANAGARI_START..DEVANAGARI_END -> score += 3
                ch.isLetterOrDigit() -> score += 1
                ch.isWhitespace() -> score += 1
                ch == '\uFFFD' -> score -= 80
                c <= 0x001F -> score -= 8
                c in 0xFFF0..0xFFFF -> score -= 15
                c in 0xE000..0xF8FF -> score -= 12
                c in 0x0080..0x00FF -> {
                    latin1Supplement++
                    score -= 1
                }
            }
        }
        val mal = s.count { it.code in MALAYALAM_START..MALAYALAM_END }
        if (mal >= 2) {
            score += (100 * mal / s.length.coerceAtLeast(1)).coerceAtMost(120)
        }
        // WinAnsi-style junk common when ToUnicode is missing for Indic fonts
        if (s.length > 40 && latin1Supplement * 4 > s.length) {
            score -= 40
        }
        return score
    }

    private const val MALAYALAM_START = 0x0D00
    private const val MALAYALAM_END = 0x0D7F
    private const val TAMIL_START = 0x0B80
    private const val TAMIL_END = 0x0BFF
    private const val DEVANAGARI_START = 0x0900
    private const val DEVANAGARI_END = 0x097F
}
