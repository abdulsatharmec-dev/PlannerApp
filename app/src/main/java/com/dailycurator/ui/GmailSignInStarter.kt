package com.dailycurator.ui

import androidx.compose.runtime.staticCompositionLocalOf

/** Gmail linking from Settings; activity result is registered on [com.dailycurator.MainActivity]. */
data class GmailLinkActions(
    /** Google Sign-In sheet without clearing an existing session (try this first). */
    val linkGmail: () -> Unit,
    /** Clears this app’s Google Sign-In session, then opens the picker (use to switch accounts). */
    val linkDifferentGoogleAccount: () -> Unit,
)

val LocalGmailLinkActions = staticCompositionLocalOf<GmailLinkActions?> { null }
