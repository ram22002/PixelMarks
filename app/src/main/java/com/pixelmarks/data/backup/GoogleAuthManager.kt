package com.pixelmarks.data.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import com.pixelmarks.app.BuildConfig
import com.pixelmarks.core.backup.model.DriveException
import com.pixelmarks.core.backup.model.GoogleAccountInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

interface GoogleAuthManager {
    fun getConnectedAccount(): GoogleAccountInfo?
    suspend fun getAccessToken(): String
    fun getSignInIntent(): Intent
    fun handleSignInResult(data: Intent?): Result<GoogleAccountInfo>
    suspend fun signOut()
    suspend fun disconnect()
}

class GoogleAuthManagerImpl(
    private val context: Context
) : GoogleAuthManager {

    companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        const val OAUTH_CLIENT_ID = BuildConfig.GOOGLE_OAUTH_CLIENT_ID
    }

    private val googleSignInOptions: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DRIVE_APPDATA_SCOPE))
            .build()
    }

    private val signInClient: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(context, googleSignInOptions)
    }

    override fun getConnectedAccount(): GoogleAccountInfo? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val email = account.email ?: return null
        return GoogleAccountInfo(
            email = email,
            displayName = account.displayName
        )
    }

    override suspend fun getAccessToken(): String = withContext(Dispatchers.IO) {
        val account: GoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(context)
            ?: throw DriveException.NotConnected()

        val androidAccount = account.account
            ?: throw DriveException.NotConnected("Android account unavailable")

        try {
            val scopeString = "oauth2:$DRIVE_APPDATA_SCOPE"
            GoogleAuthUtil.getToken(context, androidAccount, scopeString)
        } catch (e: UserRecoverableAuthException) {
            throw DriveException.AuthExpired("Google Drive authorization required. Please reconnect your account.")
        } catch (e: GoogleAuthException) {
            throw DriveException.AuthExpired("Google authorization expired or revoked: ${e.message}")
        } catch (e: IOException) {
            throw DriveException.NetworkError("Network error connecting to Google Drive: ${e.message}", e)
        } catch (e: Exception) {
            throw DriveException.AuthExpired("Unable to obtain Drive access token: ${e.message}")
        }
    }

    override fun getSignInIntent(): Intent {
        return signInClient.signInIntent
    }

    override fun handleSignInResult(data: Intent?): Result<GoogleAccountInfo> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val email = account?.email
            if (account != null && !email.isNullOrBlank()) {
                Result.success(
                    GoogleAccountInfo(
                        email = email,
                        displayName = account.displayName
                    )
                )
            } else {
                Result.failure(DriveException.NotConnected("Sign in returned no account"))
            }
        } catch (e: ApiException) {
            val message = when (e.statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED,
                CommonStatusCodes.CANCELED -> "Google sign-in was cancelled"
                CommonStatusCodes.NETWORK_ERROR -> "Network error during Google sign-in. Please check your internet connection."
                CommonStatusCodes.DEVELOPER_ERROR -> "Google sign-in configuration error (code 10). Verify Android package 'com.pixelmarks.app' and SHA-1 certificate in Google Cloud Console."
                12500 -> "Google sign-in failed (code 12500). Verify that the Google Drive API is enabled and your account is added as a Test User in the OAuth consent screen."
                CommonStatusCodes.SIGN_IN_REQUIRED -> "Google sign-in is required"
                else -> "Google sign-in failed (code ${e.statusCode}): ${e.message}"
            }
            Result.failure(DriveException.NetworkError(message, e))
        } catch (e: Exception) {
            Result.failure(DriveException.NetworkError("Google sign-in error: ${e.message}", e))
        }
    }

    override suspend fun signOut(): Unit = withContext(Dispatchers.IO) {
        try {
            Tasks.await(signInClient.signOut())
        } catch (_: Exception) {}
    }

    override suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
        try {
            Tasks.await(signInClient.revokeAccess())
        } catch (_: Exception) {
            signOut()
        }
    }
}
