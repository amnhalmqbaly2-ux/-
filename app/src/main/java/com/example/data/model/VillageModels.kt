package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

enum class StoryType(val labelAr: String) {
    PRE_DIAGNOSTIC("الاختبار التشخيصي القبلي"),
    CURRICULUM("قصة من القرية العمانية"),
    POST_DIAGNOSTIC("الاختبار التشخيصي البعدي"),
    AI_GENERATED("قصة مبتكرة بالذكاء الاصطناعي")
}

enum class ComprehensionSkill(val labelAr: String, val descriptionAr: String) {
    LITERAL("الفهم المباشر", "استخراج المعلومات والتفاصيل المذكورة صراحة في القصة"),
    VOCABULARY("فهم المفردات", "معرفة معاني الكلمات العمانية والعربية من السياق"),
    SEQUENCING("تسلسل الأحداث", "ترتيب وقائع القصة زمنياً ومنطقياً"),
    INFERENCE("الاستنتاج والتفسير", "قراءة ما بين السطور وفهم دوافع الشخصيات"),
    MAIN_IDEA("الفكرة الرئيسة والمغزى", "إدراك الدرس التربوي والقيمة الأخلاقية للقصة")
}

data class OmaniWord(
    val word: String = "",
    val meaning: String = "",
    val exampleSentence: String = "",
    val culturalNote: String = ""
)

data class ComprehensionQuestion(
    val id: String = "",
    val questionText: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val skill: ComprehensionSkill = ComprehensionSkill.LITERAL,
    val explanation: String = ""
)

data class OmaniStory(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val villageName: String = "",
    val storyType: StoryType = StoryType.CURRICULUM,
    val readingTimeMinutes: Int = 4,
    val ageGroup: String = "٦ - ١٠ سنوات",
    val moral: String = "",
    val paragraphs: List<String> = emptyList(),
    val vocabulary: List<OmaniWord> = emptyList(),
    val questions: List<ComprehensionQuestion> = emptyList()
)

data class OmaniBadge(
    val id: String,
    val titleAr: String,
    val descriptionAr: String,
    val iconEmoji: String,
    val requiredPoints: Int = 0,
    val requiredConditionText: String,
    val heritageMeaningAr: String = ""
)

@Entity(tableName = "child_profiles")
data class ChildProfileEntity(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val age: Int = 8,
    val avatarId: Int = 0,
    val stars: Int = 0,
    val badgesCsv: String = "",
    val completedStoryIdsCsv: String = "",
    val preTestScore: Int = -1,
    val preTestTotal: Int = 5,
    val postTestScore: Int = -1,
    val postTestTotal: Int = 5,
    val postTestUnlockedByTeacher: Boolean = false,
    val updatedAtEpoch: Long = System.currentTimeMillis()
) {
    fun completedStoryIdsList(): List<String> =
        if (completedStoryIdsCsv.isBlank()) emptyList()
        else completedStoryIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun badgesList(): List<String> =
        if (badgesCsv.isBlank()) emptyList()
        else badgesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

@Entity(tableName = "assessment_records")
data class AssessmentRecordEntity(
    @PrimaryKey val id: String = "",
    val profileId: String = "",
    val storyId: String = "",
    val storyTitle: String = "",
    val assessmentType: String = "STORY_PRACTICE", // PRE_TEST, STORY_PRACTICE, POST_TEST
    val score: Int = 0,
    val totalQuestions: Int = 5,
    val literalScore: Int = 0,
    val vocabularyScore: Int = 0,
    val sequencingScore: Int = 0,
    val inferenceScore: Int = 0,
    val mainIdeaScore: Int = 0,
    val timestampEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_stories")
data class CustomStoryEntity(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val villageName: String = "",
    val moral: String = "",
    val paragraphsText: String = "", // Separated by "\n---\n"
    val vocabularyText: String = "", // Lines of "word|meaning|example"
    val questionsText: String = "",  // Lines of "question|opt0~opt1~opt2~opt3|correctIdx|skill|explanation"
    val createdAtEpoch: Long = System.currentTimeMillis()
) {
    fun toOmaniStory(): OmaniStory {
        val paragraphs = paragraphsText.split("\n---\n").map { it.trim() }.filter { it.isNotEmpty() }
        val vocabList = vocabularyText.lines().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 3) {
                OmaniWord(
                    word = parts[0].trim(),
                    meaning = parts[1].trim(),
                    exampleSentence = parts[2].trim(),
                    culturalNote = parts.getOrNull(3)?.trim() ?: "مفردة من التراث العماني"
                )
            } else null
        }
        val questionList = questionsText.lines().mapIndexedNotNull { idx, line ->
            val parts = line.split("|")
            if (parts.size >= 5) {
                val opts = parts[1].split("~").map { it.trim() }
                val correct = parts[2].trim().toIntOrNull() ?: 0
                val skill = runCatching { ComprehensionSkill.valueOf(parts[3].trim()) }
                    .getOrDefault(ComprehensionSkill.LITERAL)
                ComprehensionQuestion(
                    id = "${id}_q_$idx",
                    questionText = parts[0].trim(),
                    options = opts,
                    correctIndex = correct.coerceIn(0, (opts.size - 1).coerceAtLeast(0)),
                    skill = skill,
                    explanation = parts[4].trim()
                )
            } else null
        }
        return OmaniStory(
            id = id,
            title = title,
            subtitle = "قِصَّةٌ مُبْتَكَرَةٌ مِنْ $villageName",
            villageName = villageName,
            storyType = StoryType.AI_GENERATED,
            readingTimeMinutes = 4,
            ageGroup = "٦ - ١٠ سنوات",
            moral = moral,
            paragraphs = paragraphs,
            vocabulary = vocabList,
            questions = questionList
        )
    }
}

// Firestore Cloud Data Classes with default values for synthetic no-arg constructor
data class ChildProfileCloud(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val age: Long = 8L,
    val avatarId: Long = 0L,
    val stars: Long = 0L,
    val badges: String = "",
    val completedStoryIds: String = "",
    val preTestScore: Long = -1L,
    val preTestTotal: Long = 5L,
    val postTestScore: Long = -1L,
    val postTestTotal: Long = 5L,
    val postTestUnlockedByTeacher: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toEntity(): ChildProfileEntity = ChildProfileEntity(
        id = id,
        name = name,
        age = age.toInt(),
        avatarId = avatarId.toInt(),
        stars = stars.toInt(),
        badgesCsv = badges,
        completedStoryIdsCsv = completedStoryIds,
        preTestScore = preTestScore.toInt(),
        preTestTotal = preTestTotal.toInt(),
        postTestScore = postTestScore.toInt(),
        postTestTotal = postTestTotal.toInt(),
        postTestUnlockedByTeacher = postTestUnlockedByTeacher,
        updatedAtEpoch = updatedAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

data class AssessmentRecordCloud(
    val id: String = "",
    val userId: String = "",
    val profileId: String = "",
    val storyId: String = "",
    val storyTitle: String = "",
    val assessmentType: String = "STORY_PRACTICE",
    val score: Long = 0L,
    val totalQuestions: Long = 5L,
    val literalScore: Long = 0L,
    val vocabularyScore: Long = 0L,
    val sequencingScore: Long = 0L,
    val inferenceScore: Long = 0L,
    val mainIdeaScore: Long = 0L,
    val createdAt: Timestamp? = null
) {
    fun toEntity(): AssessmentRecordEntity = AssessmentRecordEntity(
        id = id,
        profileId = profileId,
        storyId = storyId,
        storyTitle = storyTitle,
        assessmentType = assessmentType,
        score = score.toInt(),
        totalQuestions = totalQuestions.toInt(),
        literalScore = literalScore.toInt(),
        vocabularyScore = vocabularyScore.toInt(),
        sequencingScore = sequencingScore.toInt(),
        inferenceScore = inferenceScore.toInt(),
        mainIdeaScore = mainIdeaScore.toInt(),
        timestampEpoch = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

data class CustomStoryCloud(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val villageName: String = "",
    val moral: String = "",
    val paragraphsText: String = "",
    val vocabularyText: String = "",
    val questionsText: String = "",
    val createdAt: Timestamp? = null
) {
    fun toEntity(): CustomStoryEntity = CustomStoryEntity(
        id = id,
        title = title,
        villageName = villageName,
        moral = moral,
        paragraphsText = paragraphsText,
        vocabularyText = vocabularyText,
        questionsText = questionsText,
        createdAtEpoch = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun ChildProfileEntity.toFirestoreCreateMap(userId: String): Map<String, Any> = mapOf(
    "id" to id,
    "userId" to userId,
    "name" to name.take(100),
    "age" to age.coerceIn(4, 15),
    "avatarId" to avatarId.coerceIn(0, 20),
    "stars" to stars.coerceIn(0, 100000),
    "badges" to badgesCsv.take(2000),
    "completedStoryIds" to completedStoryIdsCsv.take(4000),
    "preTestScore" to preTestScore.coerceIn(-1, 100),
    "preTestTotal" to preTestTotal.coerceIn(0, 100),
    "postTestScore" to postTestScore.coerceIn(-1, 100),
    "postTestTotal" to postTestTotal.coerceIn(0, 100),
    "postTestUnlockedByTeacher" to postTestUnlockedByTeacher,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
)

fun AssessmentRecordEntity.toFirestoreCreateMap(userId: String): Map<String, Any> = mapOf(
    "id" to id,
    "userId" to userId,
    "profileId" to profileId.take(128),
    "storyId" to storyId.take(128),
    "storyTitle" to storyTitle.take(200),
    "assessmentType" to assessmentType,
    "score" to score.coerceIn(0, 100),
    "totalQuestions" to totalQuestions.coerceIn(1, 100),
    "literalScore" to literalScore.coerceIn(0, 20),
    "vocabularyScore" to vocabularyScore.coerceIn(0, 20),
    "sequencingScore" to sequencingScore.coerceIn(0, 20),
    "inferenceScore" to inferenceScore.coerceIn(0, 20),
    "mainIdeaScore" to mainIdeaScore.coerceIn(0, 20),
    "createdAt" to FieldValue.serverTimestamp()
)

fun CustomStoryEntity.toFirestoreCreateMap(userId: String): Map<String, Any> = mapOf(
    "id" to id,
    "userId" to userId,
    "title" to title.take(300),
    "villageName" to villageName.take(200),
    "moral" to moral.take(500),
    "paragraphsText" to paragraphsText.take(20000),
    "vocabularyText" to vocabularyText.take(10000),
    "questionsText" to questionsText.take(15000),
    "createdAt" to FieldValue.serverTimestamp()
)
