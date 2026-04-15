package com.dailycurator.data.media

data class MorningClip(
    val id: String,
    val label: String,
    val youtubeVideoId: String?,
    val localUri: String?,
) {
    val isYoutube: Boolean get() = youtubeVideoId != null
}
