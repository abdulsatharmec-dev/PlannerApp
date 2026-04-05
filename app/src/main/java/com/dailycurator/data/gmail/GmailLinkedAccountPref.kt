package com.dailycurator.data.gmail

import com.google.gson.annotations.SerializedName

data class GmailLinkedAccountPref(
    @SerializedName("email") val email: String,
    @SerializedName("showInSummary") val showInSummary: Boolean = true,
)
