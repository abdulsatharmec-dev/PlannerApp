package com.dailycurator.data.media

/**
 * A YouTube playlist added from a watch URL that includes `list=`.
 * [excludedVideoIds] are 11-char ids the user removed from the rotation.
 */
data class MorningPlaylistPref(
    val playlistId: String,
    val sourceWatchUrl: String,
    val excludedVideoIds: List<String> = emptyList(),
)
