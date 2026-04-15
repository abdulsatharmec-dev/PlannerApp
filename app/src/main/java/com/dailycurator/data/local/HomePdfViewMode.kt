package com.dailycurator.data.local

/** Layout mode for the home PDF reader (persisted). */
enum class HomePdfViewMode(val storageId: String) {
    /** Scroll through rendered pages vertically. */
    CONTINUOUS("continuous"),
    /** One rendered page at a time; swipe horizontally. */
    SINGLE_PAGE("single_page"),
    /** Extracted text, wraps to screen width (best for text-heavy PDFs). */
    READING_TEXT("reading_text"),
    ;

    companion object {
        fun fromStorageId(id: String?): HomePdfViewMode {
            val t = id?.trim().orEmpty()
            return entries.find { it.storageId == t } ?: CONTINUOUS
        }
    }
}
