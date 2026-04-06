package com.dailycurator.data.local

/**
 * Persisted chat message / composer text sizing (Settings → Chat appearance).
 */
enum class ChatFontSizeCategory(
    val storageId: String,
    val displayLabel: String,
    /** Main bubble & markdown body */
    val messageSp: Float,
    val lineHeightSp: Float,
    /** Composer input */
    val composerSp: Float,
    /** HH:mm under bubbles */
    val timestampSp: Float,
    /** Token hint line */
    val tokenHintSp: Float,
) {
    SMALL(
        storageId = "small",
        displayLabel = "Small",
        messageSp = 16f,
        lineHeightSp = 22f,
        composerSp = 16f,
        timestampSp = 11f,
        tokenHintSp = 9f,
    ),
    DEFAULT(
        storageId = "default",
        displayLabel = "Default (WhatsApp-like)",
        messageSp = 18f,
        lineHeightSp = 24f,
        composerSp = 18f,
        timestampSp = 12f,
        tokenHintSp = 10f,
    ),
    LARGE(
        storageId = "large",
        displayLabel = "Large",
        messageSp = 20f,
        lineHeightSp = 27f,
        composerSp = 20f,
        timestampSp = 13f,
        tokenHintSp = 11f,
    ),
    EXTRA_LARGE(
        storageId = "extra_large",
        displayLabel = "Extra large",
        messageSp = 22f,
        lineHeightSp = 30f,
        composerSp = 22f,
        timestampSp = 14f,
        tokenHintSp = 12f,
    ),
    ;

    companion object {
        fun fromStorageId(id: String?): ChatFontSizeCategory {
            if (id.isNullOrBlank()) return DEFAULT
            return entries.firstOrNull { it.storageId == id } ?: DEFAULT
        }
    }
}
