package com.msgvault.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    data class Success(val user: FirebaseUser?) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun signInAnonymously(): AuthResult {
        return try {
            val user = auth.currentUser ?: auth.signInAnonymously().await().user
            user?.let {
                userProfileRepository.saveUserProfile(it.uid)
                AuthResult.Success(it)
            } ?: AuthResult.Error("Anonymous sign in failed")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign in failed")
        }
    }

    suspend fun signInAdmin(email: String, password: String): AuthResult {
        return try {
            auth.signOut()
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                AuthResult.Success(user)
            } ?: AuthResult.Error("Admin sign in failed")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign in failed")
        }
    }

    fun signOutAdmin() {
        auth.signOut()
    }
}
