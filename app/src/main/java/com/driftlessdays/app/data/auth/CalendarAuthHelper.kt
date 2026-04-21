package com.driftlessdays.app.data.auth

import com.driftlessdays.app.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import androidx.credentials.GetCredentialRequest

object CalendarAuthHelper {

    val WEB_CLIENT_ID get() = BuildConfig.GOOGLE_WEB_CLIENT_ID

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