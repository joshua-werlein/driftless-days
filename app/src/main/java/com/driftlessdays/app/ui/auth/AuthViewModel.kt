package com.driftlessdays.app.ui.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.driftlessdays.app.data.auth.CalendarAuthHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.core.content.edit

data class AuthUiState(
    val isSignedIn: Boolean = false,
    val isLoading: Boolean = false,
    val email: String? = null,
    val displayName: String? = null,
    val errorMessage: String? = null,
    val skipAuth: Boolean = false
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        val prefs = getApplication<Application>().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val name = prefs.getString("display_name", null)
        if (email != null) {
            _uiState.value = _uiState.value.copy(
                isSignedIn = true,
                email = email,
                displayName = name
            )
        }
    }

    fun signIn(activity: Activity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val credentialManager = CredentialManager.create(activity)
                val result = credentialManager.getCredential(
                    context = activity,
                    request = CalendarAuthHelper.buildCredentialRequest()
                )
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val email = googleCredential.id
                    val name = googleCredential.displayName

                    // Persist so init block can restore on next launch
                    getApplication<Application>()
                        .getSharedPreferences("auth", Context.MODE_PRIVATE)
                        .edit {
                            putString("email", email)
                                .putString("display_name", name)
                        }

                    _uiState.value = _uiState.value.copy(
                        isSignedIn = true,
                        isLoading = false,
                        email = email,
                        displayName = name,
                        errorMessage = null
                    )
                }
            } catch (e: GetCredentialException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Sign in failed: ${e.message}"
                )
            }
        }
    }
    fun setLoading() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
    }

    fun skipAuth() {
        _uiState.value = _uiState.value.copy(skipAuth = true)
    }

    fun signOut() {
        viewModelScope.launch {
            val credentialManager = CredentialManager.create(getApplication())
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            getApplication<Application>()
                .getSharedPreferences("auth", Context.MODE_PRIVATE)
                .edit { clear() }
            _uiState.value = AuthUiState()
        }
    }
}