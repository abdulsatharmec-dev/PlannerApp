package com.dailycurator.data.gmail

import android.app.Activity
import android.os.Handler
import android.util.Log
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.ActivityResult
import com.dailycurator.data.local.AppPreferences
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

private const val GMAIL_SIGNIN_LOG_TAG = "DailyCuratorGmail"

/**
 * Handles Google Sign-In activity results. Safe to call from any thread; UI work is posted to main.
 */
fun processGmailSignInActivityResult(
    activity: Activity,
    result: ActivityResult,
    prefs: AppPreferences,
) {
    Log.d(
        GMAIL_SIGNIN_LOG_TAG,
        "activity result resultCode=${result.resultCode} hasData=${result.data != null}",
    )
    val main = Handler(Looper.getMainLooper())
    val appCtx = activity.applicationContext

    fun toast(msg: String) {
        main.post { Toast.makeText(appCtx, msg, Toast.LENGTH_LONG).show() }
    }

    fun linkEmail(email: String) {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return
        prefs.upsertGmailLinkedAccount(GmailLinkedAccountPref(email = trimmed, showInSummary = true))
        toast("Gmail linked: $trimmed")
    }

    fun tryLastSignedInAccount(): Boolean {
        val acc = GoogleSignIn.getLastSignedInAccount(activity) ?: return false
        val email = acc.email
        if (email.isNullOrBlank()) return false
        linkEmail(email)
        return true
    }

    when (result.resultCode) {
        Activity.RESULT_CANCELED -> return
        Activity.RESULT_OK -> {
            val data = result.data
            if (data == null) {
                if (!tryLastSignedInAccount()) {
                    toast("No sign-in data. Trying again in a moment may help — or check Play services.")
                }
                return
            }
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            task.addOnCompleteListener { completed ->
                if (completed.isSuccessful) {
                    val email = completed.result?.email
                    if (!email.isNullOrBlank()) {
                        linkEmail(email)
                    } else if (!tryLastSignedInAccount()) {
                        toast("Could not read email from Google account.")
                    }
                } else {
                    if (!tryLastSignedInAccount()) {
                        val ex = completed.exception
                        Log.w(GMAIL_SIGNIN_LOG_TAG, "getSignedInAccountFromIntent failed", ex)
                        val detail = when (ex) {
                            is ApiException -> "code ${ex.statusCode}"
                            else -> ex?.message ?: "unknown error"
                        }
                        toast("Google Sign-In failed ($detail). Check OAuth client + SHA-1 in Cloud Console.")
                    }
                }
            }
        }
        else -> {
            if (!tryLastSignedInAccount()) {
                toast("Sign-in did not finish (code ${result.resultCode}).")
            }
        }
    }
}

/** Second-chance link after Play services finishes persisting the account. */
fun tryGmailSilentLinkFromLastAccount(activity: Activity, prefs: AppPreferences) {
    val acc = GoogleSignIn.getLastSignedInAccount(activity) ?: return
    val email = acc.email?.trim().orEmpty()
    if (email.isEmpty()) return
    val already = prefs.getGmailLinkedAccounts().any { it.email.equals(email, ignoreCase = true) }
    if (already) return
    prefs.upsertGmailLinkedAccount(GmailLinkedAccountPref(email = email, showInSummary = true))
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(activity.applicationContext, "Gmail linked: $email", Toast.LENGTH_LONG).show()
    }
}
