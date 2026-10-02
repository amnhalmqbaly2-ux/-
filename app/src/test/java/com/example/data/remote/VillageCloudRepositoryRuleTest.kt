package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AssessmentRecordEntity
import com.example.data.model.ChildProfileEntity
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class VillageCloudRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun Throwable.findFirestoreException(): FirebaseFirestoreException? {
        var current: Throwable? = this
        while (current != null) {
            if (current is FirebaseFirestoreException) return current
            current = current.cause
        }
        return null
    }

    @Test
    fun createAndObserveChildProfile_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = VillageCloudRepository(firestore)
        val profileId = "prof_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val profile = ChildProfileEntity(
            id = profileId,
            name = "سَالِمٌ العُمَانِيُّ",
            age = 8,
            avatarId = 1,
            stars = 15,
            badgesCsv = "badge_pre_test",
            completedStoryIdsCsv = "pre_diagnostic_story",
            preTestScore = 4,
            preTestTotal = 5,
            postTestScore = -1,
            postTestTotal = 5,
            postTestUnlockedByTeacher = false
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createChildProfile(profile) }
        assertTrue(createResult.isSuccess)

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfiles(uid).first { list -> list.any { it.id == profileId } }
        }
        assertTrue(emitted.any { it.id == profileId && it.preTestScore == 4L })
    }

    @Test
    fun createAssessmentRecord_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = VillageCloudRepository(firestore)
        val recordId = "assess_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val record = AssessmentRecordEntity(
            id = recordId,
            profileId = "prof_1",
            storyId = "pre_diagnostic_story",
            storyTitle = "صَبَاحٌ فِي قَرْيَةِ النَّخِيلِ",
            assessmentType = "PRE_TEST",
            score = 4,
            totalQuestions = 5,
            literalScore = 1,
            vocabularyScore = 1,
            sequencingScore = 1,
            inferenceScore = 0,
            mainIdeaScore = 1
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createAssessmentRecord(record) }
        assertTrue(result.isSuccess)

        val assessments = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeAssessments(uid).first { list -> list.any { it.id == recordId } }
        }
        assertTrue(assessments.any { it.id == recordId && it.score == 4L })
    }

    @Test
    fun getChildProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = VillageCloudRepository(firestore)
        val profileId = "prof_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createChildProfile(
                ChildProfileEntity(id = profileId, name = "مَرْيَمُ", age = 7)
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = VillageCloudRepository(firestore)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getChildProfileById(aliceUid, profileId)
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
        } catch (e: Throwable) {
            val firestoreEx = e.findFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException in cause chain", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun observeProfiles_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = VillageCloudRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeProfiles("unauthenticated_user").first()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
        } catch (e: Throwable) {
            val firestoreEx = e.findFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException in cause chain", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@omanivillage.test"
        const val BOB_EMAIL = "bob@omanivillage.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
