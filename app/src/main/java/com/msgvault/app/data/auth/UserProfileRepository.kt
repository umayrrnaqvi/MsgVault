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
    val displayName: String = "",
    val createdAt: Long = 0L,
    val lastSeen: Long = 0L,
    val disabled: Boolean = false
)

@Singleton
class UserProfileRepository @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    suspend fun saveUserProfile(uid: String) {
        val existing = usersCollection.document(uid).get().await()
        if (existing.exists()) {
            usersCollection.document(uid).update("lastSeen", System.currentTimeMillis()).await()
        } else {
            val randomName = "User-${uid.take(6).uppercase()}"
            val profile = mapOf(
                "uid" to uid,
                "displayName" to randomName,
                "createdAt" to System.currentTimeMillis(),
                "lastSeen" to System.currentTimeMillis(),
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
                        displayName = doc.getString("displayName") ?: "Unknown",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        lastSeen = doc.getLong("lastSeen") ?: 0L,
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
