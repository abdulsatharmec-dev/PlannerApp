package com.dailycurator.data.gmail

import android.content.Context
import com.dailycurator.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope

private val SCOPE_READ = Scope("https://www.googleapis.com/auth/gmail.readonly")
private val SCOPE_SEND = Scope("https://www.googleapis.com/auth/gmail.send")
private val SCOPE_DRIVE_FILE = Scope("https://www.googleapis.com/auth/drive.file")

fun buildGoogleSignInClient(context: Context): GoogleSignInClient {
    val webId = context.getString(R.string.default_web_client_id).trim()
    val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(SCOPE_READ, SCOPE_SEND, SCOPE_DRIVE_FILE)
    if (webId.isNotBlank() && webId != "REPLACE_WITH_WEB_CLIENT_ID") {
        // Web client ID + SHA-1 in Cloud Console avoids DEVELOPER_ERROR; server client id enables proper consent for Gmail scopes.
        builder.requestIdToken(webId).requestServerAuthCode(webId, false)
    }
    return GoogleSignIn.getClient(context, builder.build())
}
