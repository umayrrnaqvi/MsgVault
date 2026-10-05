package com.msgvault.app.data.auth

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val createdAt: Long = 0L,
    val lastSignIn: Long = 0L,
    val disabled: Boolean = false
)

@Singleton
class UserProfileRepository @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    suspend fun saveUserProfile(uid: String, email: String) {
        val existing = usersCollection.document(uid).get().await()
        if (existing.exists()) {
            usersCollection.document(uid).update("lastSignIn", System.currentTimeMillis()).await()
        } else {
            val profile = mapOf(
                "uid" to uid,
                "email" to email,
                "createdAt" to System.currentTimeMillis(),
                "lastSignIn" to System.currentTimeMillis(),
                "disabled" to false
            )
            usersCollection.document(uid).set(profile).await()
        }
    }

    fun getAllUsers(): Flow<List<UserProfile>> = callbackFlow {
        val listener = usersCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { doc ->
                    UserProfile(
                        uid = doc.getString("uid") ?: "",
                        email = doc.getString("email") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        lastSignIn = doc.getLong("lastSignIn") ?: 0L,
                        disabled = doc.getBoolean("disabled") ?: false
                    )
                } ?: emptyList()
                trySend(users)
            }
        awaitClose { listener.remove() }
    }

    suspend fun isUserDisabled(uid: String): Boolean {
        return try {
            val doc = usersCollection.document(uid).get().await()
            doc.getBoolean("disabled") ?: false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun toggleUserDisabled(uid: String, disabled: Boolean) {
        usersCollection.document(uid).update("disabled", disabled).await()
    }

    suspend fun deleteUserProfile(uid: String) {
        usersCollection.document(uid).delete().await()
    }

    suspend fun getUserCount(): Int {
        return usersCollection.get().await().size()
    }
}
