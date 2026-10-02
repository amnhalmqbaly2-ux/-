package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.AssessmentRecordCloud
import com.example.data.model.AssessmentRecordEntity
import com.example.data.model.ChildProfileCloud
import com.example.data.model.ChildProfileEntity
import com.example.data.model.CustomStoryCloud
import com.example.data.model.CustomStoryEntity
import com.example.data.model.toFirestoreCreateMap
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class VillageCloudRepository(
    private val db: FirebaseFirestore
) {
    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun createChildProfile(profile: ChildProfileEntity): Result<String> {
        val uid = requireUserId()
        val path = "users/$uid/profiles/${profile.id}"
        return try {
            val payload = profile.toFirestoreCreateMap(uid)
            db.collection("users").document(uid).collection("profiles").document(profile.id)
                .set(payload)
                .await()
            Result.success(profile.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateChildProfile(profile: ChildProfileEntity): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/profiles/${profile.id}"
        return try {
            val updates = mapOf(
                "name" to profile.name.take(100),
                "age" to profile.age.coerceIn(4, 15),
                "avatarId" to profile.avatarId.coerceIn(0, 20),
                "stars" to profile.stars.coerceIn(0, 100000),
                "badges" to profile.badgesCsv.take(2000),
                "completedStoryIds" to profile.completedStoryIdsCsv.take(4000),
                "preTestScore" to profile.preTestScore.coerceIn(-1, 100),
                "preTestTotal" to profile.preTestTotal.coerceIn(0, 100),
                "postTestScore" to profile.postTestScore.coerceIn(-1, 100),
                "postTestTotal" to profile.postTestTotal.coerceIn(0, 100),
                "postTestUnlockedByTeacher" to profile.postTestUnlockedByTeacher,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("profiles").document(profile.id)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun getChildProfileById(targetUserId: String, profileId: String): Result<ChildProfileCloud?> {
        val path = "users/$targetUserId/profiles/$profileId"
        return try {
            val snapshot = db.collection("users").document(targetUserId)
                .collection("profiles").document(profileId)
                .get()
                .await()
            val item = snapshot.toObject(
                ChildProfileCloud::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(item)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    suspend fun getUserProfiles(): Result<List<ChildProfileCloud>> {
        val uid = requireUserId()
        val path = "users/$uid/profiles"
        return try {
            val snapshot = db.collection("users").document(uid).collection("profiles")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val list = snapshot.toObjects(
                ChildProfileCloud::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(list)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    fun observeProfiles(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<List<ChildProfileCloud>> = flow {
        val path = "users/$userId/profiles"
        emitAll(
            db.collection("users").document(userId).collection("profiles")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChildProfileCloud::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getUserAssessments(): Result<List<AssessmentRecordCloud>> {
        val uid = requireUserId()
        val path = "users/$uid/assessments"
        return try {
            val snapshot = db.collection("users").document(uid).collection("assessments")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val list = snapshot.toObjects(
                AssessmentRecordCloud::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(list)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun createAssessmentRecord(record: AssessmentRecordEntity): Result<String> {
        val uid = requireUserId()
        val path = "users/$uid/assessments/${record.id}"
        return try {
            val payload = record.toFirestoreCreateMap(uid)
            db.collection("users").document(uid).collection("assessments").document(record.id)
                .set(payload)
                .await()
            Result.success(record.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeAssessments(userId: String): Flow<List<AssessmentRecordCloud>> = flow {
        val path = "users/$userId/assessments"
        emitAll(
            db.collection("users").document(userId).collection("assessments")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AssessmentRecordCloud::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createCustomStory(story: CustomStoryEntity): Result<String> {
        val uid = requireUserId()
        val path = "users/$uid/custom_stories/${story.id}"
        return try {
            val payload = story.toFirestoreCreateMap(uid)
            db.collection("users").document(uid).collection("custom_stories").document(story.id)
                .set(payload)
                .await()
            Result.success(story.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeCustomStories(userId: String): Flow<List<CustomStoryCloud>> = flow {
        val path = "users/$userId/custom_stories"
        emitAll(
            db.collection("users").document(userId).collection("custom_stories")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CustomStoryCloud::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }
}
