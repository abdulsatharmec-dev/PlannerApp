package com.dailycurator.backup

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dailycurator.data.gmail.GmailTokenResult
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val DRIVE_OAUTH_SCOPE =
    "oauth2:https://www.googleapis.com/auth/drive.file"

private const val TAG = "DailyCuratorDriveToken"

@Singleton
class DriveAccessTokenProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    @Suppress("DEPRECATION")
    fun getAccessToken(accountEmail: String): GmailTokenResult {
        return try {
            val account = Account(accountEmail, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE)
            val token = GoogleAuthUtil.getToken(context, account, DRIVE_OAUTH_SCOPE)
            if (token.isNullOrBlank()) GmailTokenResult.Failure("Empty Drive token")
            else GmailTokenResult.Ok(token)
        } catch (e: UserRecoverableAuthException) {
            Log.d(TAG, "User recoverable auth for Drive $accountEmail", e)
            val intent = e.intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent != null) GmailTokenResult.NeedsUserInteraction(intent)
            else GmailTokenResult.Failure(e.message ?: "drive_auth_required")
        } catch (e: GooglePlayServicesRepairableException) {
            GmailTokenResult.NeedsUserInteraction(
                e.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: GoogleAuthException) {
            Log.e(TAG, "GoogleAuthException Drive $accountEmail", e)
            GmailTokenResult.Failure(e.message ?: "Google Drive authorization error.")
        } catch (e: Exception) {
            Log.e(TAG, "Drive getToken failed", e)
            GmailTokenResult.Failure(e.message ?: "drive_token_error")
        }
    }
}
