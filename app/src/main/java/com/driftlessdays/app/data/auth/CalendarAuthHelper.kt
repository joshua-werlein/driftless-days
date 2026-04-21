package com.driftlessdays.app.data.auth

import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import androidx.credentials.GetCredentialRequest

object CalendarAuthHelper {

    const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID_HERE"

    fun buildGoogleIdOption(): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .build()
    }

    fun buildCredentialRequest(): GetCredentialRequest {
        return GetCredentialRequest.Builder()
            .addCredentialOption(buildGoogleIdOption())
            .build()
    }
}