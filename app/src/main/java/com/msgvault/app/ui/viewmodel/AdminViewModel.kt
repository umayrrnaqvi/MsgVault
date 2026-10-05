package com.msgvault.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.msgvault.app.data.auth.UserProfile
import com.msgvault.app.data.auth.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject

data class AdminUiState(
    val users: List<UserProfile> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null,
    val loginSuccess: Boolean = false
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private val client = OkHttpClient()

    companion object {
        const val API_URL = "https://msgvault.vercel.app/api/login-as-user"
    }

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            userProfileRepository.getAllUsers().collect { users ->
                _uiState.value = _uiState.value.copy(
                    users = users,
                    isLoading = false
                )
            }
        }
    }

    fun loginAsUser(targetUid: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val idToken = FirebaseAuth.getInstance().currentUser
                    ?.getIdToken(true)?.await()?.token
                    ?: throw Exception("Not authenticated")

                val jsonBody = JSONObject().put("targetUid", targetUid).toString()
                val request = Request.Builder()
                    .url(API_URL)
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer $idToken")
                    .build()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                val responseBody = response.body?.string() ?: throw Exception("Empty response")
                val json = JSONObject(responseBody)

                if (!response.isSuccessful) {
                    throw Exception(json.optString("error", "Request failed"))
                }

                val customToken = json.getString("token")
                val email = json.getString("email")

                FirebaseAuth.getInstance().signInWithCustomToken(customToken).await()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Logged in as $email",
                    loginSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to login as user"
                )
            }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            try {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
                _uiState.value = _uiState.value.copy(
                    successMessage = "Password reset email sent to $email"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun toggleDisabled(uid: String, disabled: Boolean) {
        viewModelScope.launch {
            try {
                userProfileRepository.toggleUserDisabled(uid, disabled)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteUser(uid: String) {
        viewModelScope.launch {
            try {
                userProfileRepository.deleteUserProfile(uid)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun clearLoginSuccess() {
        _uiState.value = _uiState.value.copy(loginSuccess = false)
    }
}
