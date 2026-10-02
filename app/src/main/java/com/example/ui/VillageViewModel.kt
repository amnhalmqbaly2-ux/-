package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.OmaniStoryGeneratorService
import com.example.data.local.VillageDao
import com.example.data.model.AssessmentRecordEntity
import com.example.data.model.ChildProfileEntity
import com.example.data.model.ComprehensionSkill
import com.example.data.model.OmaniCurriculumData
import com.example.data.model.OmaniStory
import com.example.data.model.StoryType
import com.example.data.remote.ConnectivityObserver
import com.example.data.remote.VillageCloudRepository
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SyncSummary(
    val profilesCount: Int,
    val assessmentsCount: Int,
    val timestamp: Long
)

private data class SyncUiExtra(
    val isOnline: Boolean,
    val isSyncing: Boolean,
    val lastSyncTimestamp: Long?,
    val syncMessage: String?,
    val isGeneratingStory: Boolean,
    val aiErrorMessage: String?,
    val newlyEarnedBadges: List<String>
)

data class VillageUiState(
    val profiles: List<ChildProfileEntity> = emptyList(),
    val activeProfile: ChildProfileEntity? = null,
    val assessments: List<AssessmentRecordEntity> = emptyList(),
    val customStories: List<OmaniStory> = emptyList(),
    val isCloudSynced: Boolean = false,
    val cloudUserEmail: String? = null,
    val syncStatusMessage: String? = null,
    val isGeneratingStory: Boolean = false,
    val aiErrorMessage: String? = null,
    val newlyEarnedBadgeTitles: List<String> = emptyList(),
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
    val lastSyncTimestamp: Long? = null
) {
    val preDiagnosticStory: OmaniStory get() = OmaniCurriculumData.preDiagnosticStory
    val curriculumStories: List<OmaniStory> get() = OmaniCurriculumData.curriculumStories
    val postDiagnosticStory: OmaniStory get() = OmaniCurriculumData.postDiagnosticStory

    val isPreTestCompleted: Boolean
        get() = (activeProfile?.preTestScore ?: -1) >= 0

    val completedCurriculumCount: Int
        get() {
            val completedIds = activeProfile?.completedStoryIdsList().orEmpty().toSet()
            return curriculumStories.count { it.id in completedIds }
        }

    val isPostTestUnlocked: Boolean
        get() = completedCurriculumCount >= curriculumStories.size ||
                (activeProfile?.postTestUnlockedByTeacher == true)

    val isPostTestCompleted: Boolean
        get() = (activeProfile?.postTestScore ?: -1) >= 0

    val activeProfileAssessments: List<AssessmentRecordEntity>
        get() = assessments.filter { it.profileId == activeProfile?.id }

    val totalTestPoints: Int
        get() = activeProfileAssessments.sumOf { it.score }

    val readerLevel: OmaniCurriculumData.ReaderLevel
        get() = OmaniCurriculumData.getReaderLevel(activeProfile?.stars ?: 0)

    val earnedBadgesCount: Int
        get() = activeProfile?.badgesList().orEmpty().size

    val latestPreTestRecord: AssessmentRecordEntity?
        get() = activeProfileAssessments.firstOrNull { it.assessmentType == "PRE_TEST" }

    val latestPostTestRecord: AssessmentRecordEntity?
        get() = activeProfileAssessments.firstOrNull { it.assessmentType == "POST_TEST" }
}

class VillageViewModel(
    private val dao: VillageDao,
    private val cloudRepository: VillageCloudRepository,
    private val networkMonitor: ConnectivityObserver? = null,
    private val aiGenerator: OmaniStoryGeneratorService = OmaniStoryGeneratorService()
) : ViewModel() {

    private val _selectedProfileId = MutableStateFlow<String?>(null)
    private val _cloudUid = MutableStateFlow<String?>(null)
    private val _cloudEmail = MutableStateFlow<String?>(null)
    private val _syncMessage = MutableStateFlow<String?>(null)
    private val _isGeneratingStory = MutableStateFlow(false)
    private val _aiError = MutableStateFlow<String?>(null)
    private val _newlyEarnedBadges = MutableStateFlow<List<String>>(emptyList())
    private val _isOnline = MutableStateFlow(networkMonitor?.isConnected ?: true)
    private val _isSyncing = MutableStateFlow(false)
    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)

    private var cloudSyncJobs: List<Job> = emptyList()

    private val syncExtraFlow = combine(
        _isOnline,
        _isSyncing,
        _lastSyncTimestamp,
        _syncMessage
    ) { online, syncing, syncTime, msg ->
        Triple(online, syncing, Pair(syncTime, msg))
    }

    val uiState: StateFlow<VillageUiState> = combine(
        dao.observeAllProfiles(),
        dao.observeAllAssessments(),
        dao.observeCustomStories(),
        combine(_selectedProfileId, _cloudUid, _cloudEmail) { id, uid, email -> Triple(id, uid, email) },
        combine(
            syncExtraFlow,
            _isGeneratingStory,
            _aiError,
            _newlyEarnedBadges
        ) { syncTrip, gen, err, badges ->
            val (online, syncing, syncInfo) = syncTrip
            val (syncTime, msg) = syncInfo
            SyncUiExtra(
                isOnline = online,
                isSyncing = syncing,
                lastSyncTimestamp = syncTime,
                syncMessage = msg,
                isGeneratingStory = gen,
                aiErrorMessage = err,
                newlyEarnedBadges = badges
            )
        }
    ) { profiles, assessments, customEntities, authTriple, extra ->
        val (selectedId, uid, email) = authTriple
        val active = profiles.firstOrNull { it.id == selectedId } ?: profiles.firstOrNull()
        VillageUiState(
            profiles = profiles,
            activeProfile = active,
            assessments = assessments,
            customStories = customEntities.map { it.toOmaniStory() },
            isCloudSynced = uid != null,
            cloudUserEmail = email,
            syncStatusMessage = extra.syncMessage,
            isGeneratingStory = extra.isGeneratingStory,
            aiErrorMessage = extra.aiErrorMessage,
            newlyEarnedBadgeTitles = extra.newlyEarnedBadges,
            isOnline = extra.isOnline,
            isSyncing = extra.isSyncing,
            lastSyncTimestamp = extra.lastSyncTimestamp
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = VillageUiState()
    )

    init {
        ensureDefaultReaderProfile()
        observeNetworkAndAutoSync()
    }

    private fun ensureDefaultReaderProfile() {
        viewModelScope.launch {
            val existing = dao.getAllProfilesOnce()
            if (existing.isEmpty()) {
                val defaultProfile = ChildProfileEntity(
                    id = "reader_default_1",
                    name = "سَالِمٌ (القَارِئُ الصَّغِيرُ)",
                    age = 8,
                    avatarId = 0,
                    stars = 5,
                    badgesCsv = "",
                    completedStoryIdsCsv = "",
                    preTestScore = -1,
                    preTestTotal = 5,
                    postTestScore = -1,
                    postTestTotal = 5,
                    postTestUnlockedByTeacher = false
                )
                dao.upsertProfile(defaultProfile)
                _selectedProfileId.value = defaultProfile.id
            } else if (_selectedProfileId.value == null) {
                _selectedProfileId.value = existing.first().id
            }
        }
    }

    /**
     * يراقب حالة الاتصال بالإنترنت تلقائياً:
     * عند توفر الاتصال، يقوم فوراً بمزامنة تقدم الطفل (النجوم، الشارات، ونتائج الاختبارات)
     * بين قاعدة البيانات المحلية Room وسحابة Firestore.
     */
    private fun observeNetworkAndAutoSync() {
        val monitor = networkMonitor ?: return
        viewModelScope.launch {
            monitor.isOnline.collect { online ->
                _isOnline.value = online
                if (online) {
                    val uid = _cloudUid.value
                    if (uid != null) {
                        Log.i("VillageVM", "Network restored: Auto-syncing child progress with Firestore...")
                        syncChildProgressWithFirestore()
                    } else {
                        _syncMessage.value = "متصل بالإنترنت • سجّل الدخول بحساب Google لمزامنة تقدم الطفل سحابياً"
                    }
                } else {
                    _syncMessage.value = "لا يوجد اتصال بالإنترنت • يتم حفظ التقدم في التخزين المحلي مؤقتاً"
                }
            }
        }
    }

    /**
     * دالة برمجية رئيسية تضمن مزامنة بيانات تقدم الطفل (نتائج الاختبارات والنجوم)
     * بين التخزين المحلي (Room) وقاعدة بيانات Firestore عند توفر اتصال بالإنترنت.
     * تقوم بدمج ذكي ثنائي الاتجاه يمنع ضياع أي إنجاز تم تحقيقه بدون اتصال.
     */
    suspend fun syncChildProgressWithFirestore(): Result<SyncSummary> {
        val isConnected = networkMonitor?.isConnected ?: true
        if (!isConnected) {
            val offlineMsg = "لا يوجد اتصال بالإنترنت • تم حفظ تقدم الطفل محلياً لحين عودة الاتصال"
            _syncMessage.value = offlineMsg
            return Result.failure(IllegalStateException(offlineMsg))
        }

        val uid = _cloudUid.value
        if (uid == null) {
            val unauthMsg = "الحفظ المحلي مفعّل • يرجى تسجيل الدخول بحساب Google للمزامنة السحابية"
            _syncMessage.value = unauthMsg
            return Result.failure(IllegalStateException(unauthMsg))
        }

        _isSyncing.value = true
        _syncMessage.value = "جاري مزامنة بيانات تقدم الطفل (النجوم والاختبارات) مع Firestore..."

        return try {
            // 1. قراءة البيانات المحلية من Room
            val localProfiles = dao.getAllProfilesOnce()
            val localAssessments = dao.getAllAssessmentsOnce()

            // 2. قراءة البيانات السحابية من Firestore
            val cloudProfilesResult = cloudRepository.getUserProfiles()
            val cloudAssessmentsResult = cloudRepository.getUserAssessments()

            val cloudProfiles = cloudProfilesResult.getOrDefault(emptyList())
            val cloudAssessments = cloudAssessmentsResult.getOrDefault(emptyList())

            val cloudProfileMap = cloudProfiles.associateBy { it.id }
            val localProfileMap = localProfiles.associateBy { it.id }

            var syncedProfilesCount = 0
            var syncedAssessmentsCount = 0

            // 3. دمج ومزامنة ملفات الأطفال وتقدمهم (النجوم، الشارات، القصص المنجزة، درجات الاختبارات)
            val allProfileIds = (localProfileMap.keys + cloudProfileMap.keys).distinct()

            for (profileId in allProfileIds) {
                val local = localProfileMap[profileId]
                val cloud = cloudProfileMap[profileId]

                when {
                    local != null && cloud != null -> {
                        val cloudEntity = cloud.toEntity()
                        // دمج النجوم (الأعلى بين المحلي والسحابي)
                        val mergedStars = maxOf(local.stars, cloudEntity.stars)
                        // دمج الشارات التراثية
                        val mergedBadges = (local.badgesList() + cloudEntity.badgesList())
                            .distinct().joinToString(",")
                        // دمج القصص المكتملة
                        val mergedCompletedStories = (local.completedStoryIdsList() + cloudEntity.completedStoryIdsList())
                            .distinct().joinToString(",")
                        // دمج درجات الاختبار القبلي والبعدي
                        val mergedPreTestScore = maxOf(local.preTestScore, cloudEntity.preTestScore)
                        val mergedPreTestTotal = maxOf(local.preTestTotal, cloudEntity.preTestTotal)
                        val mergedPostTestScore = maxOf(local.postTestScore, cloudEntity.postTestScore)
                        val mergedPostTestTotal = maxOf(local.postTestTotal, cloudEntity.postTestTotal)
                        val mergedTeacherUnlock = local.postTestUnlockedByTeacher || cloudEntity.postTestUnlockedByTeacher

                        val mergedProfile = local.copy(
                            stars = mergedStars,
                            badgesCsv = mergedBadges,
                            completedStoryIdsCsv = mergedCompletedStories,
                            preTestScore = mergedPreTestScore,
                            preTestTotal = mergedPreTestTotal,
                            postTestScore = mergedPostTestScore,
                            postTestTotal = mergedPostTestTotal,
                            postTestUnlockedByTeacher = mergedTeacherUnlock,
                            updatedAtEpoch = System.currentTimeMillis()
                        )

                        // حفظ النسخة المدمجة في Room ثم Firestore
                        dao.upsertProfile(mergedProfile)
                        cloudRepository.createChildProfile(mergedProfile)
                        syncedProfilesCount++
                    }
                    local != null && cloud == null -> {
                        // رفع الملف المحلي غير الموجود سحابياً
                        cloudRepository.createChildProfile(local)
                        syncedProfilesCount++
                    }
                    local == null && cloud != null -> {
                        // تنزيل الملف السحابي وحفظه محلياً في Room
                        dao.upsertProfile(cloud.toEntity())
                        syncedProfilesCount++
                    }
                }
            }

            // 4. مزامنة نتائج وسجلات الاختبارات (Assessments)
            val cloudAssessmentIds = cloudAssessments.map { it.id }.toSet()
            val localAssessmentIds = localAssessments.map { it.id }.toSet()

            // رفع الاختبارات المحلية الجديدة إلى السحابة
            for (localAssessment in localAssessments) {
                if (localAssessment.id !in cloudAssessmentIds) {
                    cloudRepository.createAssessmentRecord(localAssessment)
                    syncedAssessmentsCount++
                }
            }

            // حفظ الاختبارات السحابية الجديدة في التخزين المحلي Room
            for (cloudAssessment in cloudAssessments) {
                if (cloudAssessment.id !in localAssessmentIds) {
                    dao.insertAssessment(cloudAssessment.toEntity())
                    syncedAssessmentsCount++
                }
            }

            val timestamp = System.currentTimeMillis()
            _lastSyncTimestamp.value = timestamp
            _isSyncing.value = false
            val successMessage = "تمت المزامنة بنجاح: تم تحديث النجوم والاختبارات سحابياً ومحلياً ⭐"
            _syncMessage.value = successMessage

            Result.success(SyncSummary(syncedProfilesCount, syncedAssessmentsCount, timestamp))
        } catch (e: Exception) {
            Log.e("VillageVM", "Sync error: ${e.message}", e)
            _isSyncing.value = false
            val errorMsg = "تعذر إكمال المزامنة السحابية (${e.message ?: "خطأ في الشبكة"}). يتم الحفظ محلياً."
            _syncMessage.value = errorMsg
            Result.failure(e)
        }
    }

    /**
     * إتاحة بدء المزامنة يدوياً من واجهة المستخدم
     */
    fun syncNow() {
        viewModelScope.launch {
            syncChildProgressWithFirestore()
        }
    }

    /**
     * Called when FirebaseAuth state changes. Only attaches Firestore snapshot listeners
     * when `uid != null` (strictly auth-gated).
     */
    fun onAuthUserChanged(uid: String?, email: String?) {
        if (_cloudUid.value == uid) return
        _cloudUid.value = uid
        _cloudEmail.value = email
        cloudSyncJobs.forEach { it.cancel() }
        cloudSyncJobs = emptyList()

        if (uid != null) {
            startCloudSynchronization(uid)
        } else {
            _syncMessage.value = "الحفظ المحلي مفعّل على الجهاز"
        }
    }

    private fun startCloudSynchronization(uid: String) {
        // 1. إجراء مزامنة فورية كاملة لبيانات التقدم والنجوم والاختبارات
        val initialSyncJob = viewModelScope.launch {
            syncChildProgressWithFirestore()
        }

        // 2. Observe cloud profiles and merge into local Room database
        val profilesJob = viewModelScope.launch {
            cloudRepository.observeProfiles(uid)
                .catch { e ->
                    Log.w("VillageVM", "Error observing cloud profiles", e)
                    _syncMessage.value = "تعذر مزامنة السحابة: يتم الحفظ محلياً"
                }
                .collect { cloudProfiles ->
                    if (cloudProfiles.isNotEmpty()) {
                        dao.upsertProfiles(cloudProfiles.map { it.toEntity() })
                    }
                }
        }

        // 3. Observe cloud assessments and merge into Room
        val assessmentsJob = viewModelScope.launch {
            cloudRepository.observeAssessments(uid)
                .catch { e -> Log.w("VillageVM", "Error observing cloud assessments", e) }
                .collect { cloudAssessments ->
                    if (cloudAssessments.isNotEmpty()) {
                        dao.insertAssessments(cloudAssessments.map { it.toEntity() })
                    }
                }
        }

        // 4. Observe cloud custom AI stories and merge into Room
        val storiesJob = viewModelScope.launch {
            cloudRepository.observeCustomStories(uid)
                .catch { e -> Log.w("VillageVM", "Error observing cloud custom stories", e) }
                .collect { cloudStories ->
                    if (cloudStories.isNotEmpty()) {
                        dao.insertCustomStories(cloudStories.map { it.toEntity() })
                    }
                }
        }

        cloudSyncJobs = listOf(initialSyncJob, profilesJob, assessmentsJob, storiesJob)
    }

    fun selectProfile(profileId: String) {
        _selectedProfileId.value = profileId
    }

    fun addChildProfile(name: String, age: Int, avatarId: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = "reader_${UUID.randomUUID().toString().replace("-", "").take(10)}"
            val newProfile = ChildProfileEntity(
                id = id,
                name = name.trim(),
                age = age.coerceIn(6, 10),
                avatarId = avatarId,
                stars = 5
            )
            dao.upsertProfile(newProfile)
            _selectedProfileId.value = id
            if (_cloudUid.value != null) {
                cloudRepository.createChildProfile(newProfile)
            }
        }
    }

    fun toggleTeacherUnlockPostTest() {
        val current = uiState.value.activeProfile ?: return
        viewModelScope.launch {
            val updated = current.copy(
                postTestUnlockedByTeacher = !current.postTestUnlockedByTeacher,
                updatedAtEpoch = System.currentTimeMillis()
            )
            dao.upsertProfile(updated)
            if (_cloudUid.value != null) {
                cloudRepository.createChildProfile(updated)
            }
        }
    }

    fun markStoryReadingCompleted(story: OmaniStory) {
        val currentProfile = uiState.value.activeProfile ?: return
        viewModelScope.launch {
            val completedSet = currentProfile.completedStoryIdsList().toMutableSet()
            val alreadyRead = story.id in completedSet
            completedSet.add(story.id)

            // Award 10 reading stars if completing reading for the first time, or 2 refresher stars
            val earnedStars = if (!alreadyRead) 10 else 2
            val newStars = currentProfile.stars + earnedStars

            val oldBadges = currentProfile.badgesList().toSet()
            val updatedBadges = oldBadges.toMutableSet()

            // Unlock first story badge
            if (completedSet.isNotEmpty()) {
                updatedBadges.add("badge_first_story_read")
            }

            // Check if all curriculum stories + pre & post are read
            val allCurriculumDone = OmaniCurriculumData.curriculumStories.all { it.id in completedSet }
            if (allCurriculumDone && currentProfile.preTestScore >= 0 && currentProfile.postTestScore >= 0) {
                updatedBadges.add("badge_all_stories")
            }

            val newlyUnlockedIds = updatedBadges - oldBadges
            if (newlyUnlockedIds.isNotEmpty()) {
                val titles = OmaniCurriculumData.badges
                    .filter { it.id in newlyUnlockedIds }
                    .map { "${it.iconEmoji} ${it.titleAr}" }
                _newlyEarnedBadges.value = titles
            }

            val updatedProfile = currentProfile.copy(
                stars = newStars,
                badgesCsv = updatedBadges.joinToString(","),
                completedStoryIdsCsv = completedSet.joinToString(","),
                updatedAtEpoch = System.currentTimeMillis()
            )
            dao.upsertProfile(updatedProfile)

            if (_cloudUid.value != null) {
                cloudRepository.createChildProfile(updatedProfile)
            }
        }
    }

    fun submitStoryAssessment(
        story: OmaniStory,
        selectedAnswers: Map<String, Int>
    ) {
        val currentProfile = uiState.value.activeProfile ?: return
        viewModelScope.launch {
            var totalCorrect = 0
            var literal = 0
            var vocab = 0
            var seq = 0
            var inference = 0
            var mainIdea = 0

            story.questions.forEach { q ->
                val chosen = selectedAnswers[q.id]
                val isCorrect = chosen == q.correctIndex
                if (isCorrect) {
                    totalCorrect++
                    when (q.skill) {
                        ComprehensionSkill.LITERAL -> literal++
                        ComprehensionSkill.VOCABULARY -> vocab++
                        ComprehensionSkill.SEQUENCING -> seq++
                        ComprehensionSkill.INFERENCE -> inference++
                        ComprehensionSkill.MAIN_IDEA -> mainIdea++
                    }
                }
            }

            val assessmentType = when (story.storyType) {
                StoryType.PRE_DIAGNOSTIC -> "PRE_TEST"
                StoryType.POST_DIAGNOSTIC -> "POST_TEST"
                StoryType.CURRICULUM, StoryType.AI_GENERATED -> "STORY_PRACTICE"
            }

            val recordId = "assess_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val record = AssessmentRecordEntity(
                id = recordId,
                profileId = currentProfile.id,
                storyId = story.id,
                storyTitle = story.title,
                assessmentType = assessmentType,
                score = totalCorrect,
                totalQuestions = story.questions.size.coerceAtLeast(1),
                literalScore = literal,
                vocabularyScore = vocab,
                sequencingScore = seq,
                inferenceScore = inference,
                mainIdeaScore = mainIdea
            )
            dao.insertAssessment(record)

            // Update profile progress, stars, and badges
            val completedSet = currentProfile.completedStoryIdsList().toMutableSet()
            completedSet.add(story.id)

            val isPerfectScore = totalCorrect == story.questions.size && story.questions.isNotEmpty()
            val bonusStars = if (isPerfectScore) 5 else 0
            val earnedStars = (totalCorrect * 3) + 5 + bonusStars
            val newStars = currentProfile.stars + earnedStars

            val newPreScore = if (story.storyType == StoryType.PRE_DIAGNOSTIC) totalCorrect else currentProfile.preTestScore
            val newPostScore = if (story.storyType == StoryType.POST_DIAGNOSTIC) totalCorrect else currentProfile.postTestScore

            // Cumulative test points achieved across all assessments (including this one)
            val pastAssessments = dao.getAssessmentsForProfileOnce(currentProfile.id)
            val cumulativeTestPoints: Int = pastAssessments.sumOf { it.score } + totalCorrect

            val oldBadges = currentProfile.badgesList().toSet()
            val updatedBadges = oldBadges.toMutableSet()

            // Unlock point-based badges according to total quiz points
            if (completedSet.isNotEmpty()) updatedBadges.add("badge_first_story_read")
            if (cumulativeTestPoints >= 5) updatedBadges.add("badge_points_5")
            if (cumulativeTestPoints >= 10) updatedBadges.add("badge_points_10")
            if (cumulativeTestPoints >= 15) updatedBadges.add("badge_points_15")
            if (cumulativeTestPoints >= 20) updatedBadges.add("badge_points_20")
            if (cumulativeTestPoints >= 25) updatedBadges.add("badge_points_25")
            if (cumulativeTestPoints >= 30) updatedBadges.add("badge_points_30")

            // Perfect score badge (5/5)
            if (isPerfectScore) updatedBadges.add("badge_perfect_score")

            // Diagnostic test milestones
            if (story.storyType == StoryType.PRE_DIAGNOSTIC && totalCorrect >= 1) {
                updatedBadges.add("badge_pre_test")
            }
            if (story.storyType == StoryType.POST_DIAGNOSTIC && totalCorrect >= 1) {
                updatedBadges.add("badge_diagnostic_champion")
            }

            // All stories completion badge
            val allCurriculumDone = OmaniCurriculumData.curriculumStories.all { it.id in completedSet }
            if (allCurriculumDone && newPreScore >= 0 && newPostScore >= 0) {
                updatedBadges.add("badge_all_stories")
            }

            val newlyUnlockedIds = updatedBadges - oldBadges
            if (newlyUnlockedIds.isNotEmpty()) {
                val titles = OmaniCurriculumData.badges
                    .filter { it.id in newlyUnlockedIds }
                    .map { "${it.iconEmoji} ${it.titleAr}" }
                _newlyEarnedBadges.value = titles
            }

            val updatedProfile = currentProfile.copy(
                stars = newStars,
                badgesCsv = updatedBadges.joinToString(","),
                completedStoryIdsCsv = completedSet.joinToString(","),
                preTestScore = newPreScore,
                preTestTotal = if (story.storyType == StoryType.PRE_DIAGNOSTIC) story.questions.size else currentProfile.preTestTotal,
                postTestScore = newPostScore,
                postTestTotal = if (story.storyType == StoryType.POST_DIAGNOSTIC) story.questions.size else currentProfile.postTestTotal,
                updatedAtEpoch = System.currentTimeMillis()
            )
            dao.upsertProfile(updatedProfile)

            if (_cloudUid.value != null) {
                cloudRepository.createAssessmentRecord(record)
                cloudRepository.createChildProfile(updatedProfile)
            }
        }
    }

    fun dismissBadgeCelebration() {
        _newlyEarnedBadges.value = emptyList()
    }

    fun generateNewAiStory(
        topic: String,
        villageSetting: String,
        moralValue: String,
        onStoryReady: (OmaniStory) -> Unit
    ) {
        if (_isGeneratingStory.value) return
        _isGeneratingStory.value = true
        _aiError.value = null

        viewModelScope.launch {
            val result = aiGenerator.generateVocalizedOmaniStory(
                topic = topic,
                villageSetting = villageSetting,
                moralValue = moralValue
            )
            _isGeneratingStory.value = false
            result.onSuccess { entity ->
                dao.insertCustomStory(entity)
                if (_cloudUid.value != null) {
                    cloudRepository.createCustomStory(entity)
                }
                onStoryReady(entity.toOmaniStory())
            }.onFailure { error ->
                _aiError.value = error.message ?: "حدث خطأ أثناء توليد القصة"
            }
        }
    }

    fun clearAiError() {
        _aiError.value = null
    }
}
