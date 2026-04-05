package com.dailycurator.data.gmail

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val GMAIL_OAUTH_SCOPES =
    "oauth2:https://www.googleapis.com/auth/gmail.readonly https://www.googleapis.com/auth/gmail.send"

private const val TAG = "DailyCuratorGmailToken"

sealed class GmailTokenResult {
    data class Ok(val accessToken: String) : GmailTokenResult()
    data class NeedsUserInteraction(val intent: Intent) : GmailTokenResult()
    data class Failure(val message: String) : GmailTokenResult()
}

@Singleton
class GmailTokenProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    @Suppress("DEPRECATION")
    suspend fun getAccessToken(accountEmail: String): GmailTokenResult = withContext(Dispatchers.IO) {
        try {
            val account = Account(accountEmail, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE)
            val token = GoogleAuthUtil.getToken(context, account, GMAIL_OAUTH_SCOPES)
            if (token.isNullOrBlank()) GmailTokenResult.Failure("Empty token")
            else GmailTokenResult.Ok(token)
        } catch (e: UserRecoverableAuthException) {
            Log.d(TAG, "User recoverable auth for $accountEmail", e)
            val intent = e.intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent != null) GmailTokenResult.NeedsUserInteraction(intent)
            else GmailTokenResult.Failure(e.message ?: "auth_required")
        } catch (e: GooglePlayServicesRepairableException) {
            Log.w(TAG, "Play services need update/repair", e)
            GmailTokenResult.NeedsUserInteraction(
                e.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: GoogleAuthException) {
            Log.e(TAG, "GoogleAuthException for $accountEmail", e)
            GmailTokenResult.Failure(e.message ?: "Google authorization error (check Cloud OAuth + SHA-1).")
        } catch (e: Exception) {
            Log.e(TAG, "getToken failed for $accountEmail", e)
            GmailTokenResult.Failure(e.message ?: "token_error")
        }
    }
}
