package com.dailycurator.data.local

/**
 * Reorderable blocks on the Today (home) screen, persisted in [AppPreferences].
 */
enum class HomeLayoutSection(val storageId: String) {
    DAY_WINDOW("day_window"),
    MORNING_MOTIVATION("morning_motivation"),
    HOME_PDF("home_pdf"),
    ASSISTANT_INSIGHT("assistant_insight"),
    TOP5_PRIORITIES("top5_priorities"),
    WEEKLY_GOALS("weekly_goals"),
    GMAIL_DIGEST("gmail_digest"),
    ;

    companion object {
        val defaultOrder: List<HomeLayoutSection> = entries.toList()

        fun fromStorageId(id: String): HomeLayoutSection? =
            entries.find { it.storageId == id }

        /**
         * Parses stored ids in order, drops unknowns, then appends any missing sections so the list stays complete.
         */
        fun normalizeOrder(ids: List<String>): List<HomeLayoutSection> {
            val out = ArrayList<HomeLayoutSection>(entries.size)
            val seen = HashSet<HomeLayoutSection>()
            for (raw in ids) {
                val s = fromStorageId(raw) ?: continue
                if (seen.add(s)) out.add(s)
            }
            for (s in defaultOrder) {
                if (s !in seen) out.add(s)
            }
            return out
        }
    }
}
