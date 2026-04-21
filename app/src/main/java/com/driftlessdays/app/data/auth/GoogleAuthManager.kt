package com.driftlessdays.app.data.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

data class GoogleUser(
    val id: String,
    val displayName: String?,
    val email: String?,
    val profilePictureUri: String?
)

sealed class AuthResult {
    data class Success(val user: GoogleUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Cancelled : AuthResult()
}

class GoogleAuthManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    // Replace with your actual Web Client ID from Google Cloud Console
    // Go to Credentials → your OAuth client → copy the client ID
    // You need a WEB type client ID here, not the Android one
    private val webClientId = "106244507483-gfshlif8290hsf86nfcuf7a22hh00jtm.apps.googleusercontent.com"

    suspend fun signIn(activityContext: Context): AuthResult {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .setNonce(null)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            handleSignInResult(result)

        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            AuthResult.Cancelled
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign in failed")
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse): AuthResult {
        return when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)
                        AuthResult.Success(
                            GoogleUser(
                                id = googleIdTokenCredential.id,
                                displayName = googleIdTokenCredential.displayName,
                                email = googleIdTokenCredential.id,
                                profilePictureUri = googleIdTokenCredential.profilePictureUri?.toString()
                            )
                        )
                    } catch (e: GoogleIdTokenParsingException) {
                        AuthResult.Error("Invalid Google ID token: ${e.message}")
                    }
                } else {
                    AuthResult.Error("Unexpected credential type")
                }
            }
            else -> AuthResult.Error("Unexpected credential type")
        }
    }

    suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}