package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.OmaniBadge
import com.example.data.model.OmaniCurriculumData
import com.example.data.model.OmaniStory
import com.example.ui.VillageUiState
import com.example.ui.auth.GoogleSignInButton
import com.example.ui.auth.signOutGoogle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VillageJourneyScreen(
    uiState: VillageUiState,
    onOpenStoryReader: (OmaniStory) -> Unit,
    onOpenStoryQuiz: (OmaniStory) -> Unit,
    onAuthChanged: () -> Unit,
    onSyncNow: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var authErrorText by remember { mutableStateOf<String?>(null) }
    var selectedBadgeForDialog by remember { mutableStateOf<OmaniBadge?>(null) }
    var showAllBadgesDialog by remember { mutableStateOf(false) }

    val activeProfile = uiState.activeProfile
    val completedSet = activeProfile?.completedStoryIdsList().orEmpty().toSet()
    val earnedBadgesSet = activeProfile?.badgesList().orEmpty().toSet()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Omani Village Hero Banner + Reader Status
        item {
            ElevatedCard(
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(205.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_omani_village_hero),
                        contentDescription = "قريتي كتابي الصغير",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.15f),
                                        Color.Black.copy(alpha = 0.72f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${activeProfile?.name ?: "القارئ الصغير"} • ${activeProfile?.stars ?: 0} نجمة",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "قَرْيَتِي كِتَابِي الصَّغِيرُ",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                        Text(
                            text = "حِكَايَاتٌ مُشَكَّلَةٌ مِنَ القَرْيَةِ العُمَانِيَّةِ مَعَ التَّشْخِيصِ القَبْلِيِّ وَالبَعْدِيِّ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }
                }

                // Hybrid Local + Cloud Sync Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp)
                ) {
                    // Connectivity & Manual Sync Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (uiState.isOnline) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (uiState.isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                                    contentDescription = null,
                                    tint = if (uiState.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.isOnline) "متصل بالإنترنت" else "غير متصل • حفظ محلي في Room",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.isOnline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

                        if (uiState.isCloudSynced) {
                            FilledTonalButton(
                                onClick = onSyncNow,
                                enabled = !uiState.isSyncing && uiState.isOnline,
                                modifier = Modifier.testTag("manual_sync_button")
                            ) {
                                if (uiState.isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("جاري المزامنة...", style = MaterialTheme.typography.labelSmall)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "مزامنة الآن",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مزامنة الآن", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (uiState.isCloudSynced) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "المزامنة السحابية وقاعدة البيانات مفعّلة",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = uiState.cloudUserEmail ?: "متصل بحساب Google",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            TextButton(
                                onClick = {
                                    signOutGoogle(context, onSignOutComplete = onAuthChanged, scope = scope)
                                },
                                modifier = Modifier.testTag("sign_out_button")
                            ) {
                                Text("تسجيل الخروج")
                            }
                        }
                    } else {
                        Text(
                            text = "💾 الحفظ المحلي مفعّل تلقائياً • سجل الدخول لمزامنة التقدم سحابياً:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GoogleSignInButton(
                            onAuthSuccess = {
                                authErrorText = null
                                onAuthChanged()
                            },
                            onAuthError = { err -> authErrorText = err },
                            modifier = Modifier.fillMaxWidth()
                        )
                        authErrorText?.let { err ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    uiState.syncStatusMessage?.let { statusMsg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (uiState.isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (uiState.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = statusMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Omani Heritage Stars & Badges System Showcase (نظام النجوم والشارات التراثية العمانية)
        item {
            OmaniHeritageStarsAndBadgesCard(
                uiState = uiState,
                earnedBadgesSet = earnedBadgesSet,
                onBadgeClick = { badge -> selectedBadgeForDialog = badge },
                onViewAllBadges = { showAllBadgesDialog = true }
            )
        }

        // 3. STAGE 1: Pre-Diagnostic Story & Test (قبل القصص المعروضة)
        item {
            StageHeaderBanner(
                stageNumber = "١",
                title = "المرحلة الأولى: الاختبار التشخيصي القبلي",
                subtitle = "قصة تشخيصية خاصة قبل البدء بالقصص المعروضة لقياس الفهم المقروء الأساسي"
            )
            Spacer(modifier = Modifier.height(8.dp))
            DiagnosticStoryCard(
                story = uiState.preDiagnosticStory,
                isUnlocked = true,
                isCompleted = uiState.isPreTestCompleted,
                score = activeProfile?.preTestScore ?: -1,
                total = activeProfile?.preTestTotal ?: 5,
                badgeText = "قصة الاختبار القبلي الخاصة",
                lockedReason = "",
                onReadStory = { onOpenStoryReader(uiState.preDiagnosticStory) },
                onTakeQuiz = { onOpenStoryQuiz(uiState.preDiagnosticStory) },
                testTagPrefix = "pre_diagnostic"
            )
        }

        // 3. STAGE 2: The 5 Curated Omani Village Stories (القصص المعروضة للفهم والتدريب)
        item {
            StageHeaderBanner(
                stageNumber = "٢",
                title = "المرحلة الثانية: قصص القرية العمانية للفهم",
                subtitle = "اقرأ حكايات القرية المشكولة وحل أسئلة الفهم (${uiState.completedCurriculumCount} من ${uiState.curriculumStories.size} مكتملة)"
            )
        }

        itemsIndexed(uiState.curriculumStories) { index, story ->
            val isCompleted = story.id in completedSet
            CurriculumStoryCard(
                storyNumber = index + 1,
                story = story,
                isCompleted = isCompleted,
                onReadStory = { onOpenStoryReader(story) },
                onTakeQuiz = { onOpenStoryQuiz(story) }
            )
        }

        // 4. STAGE 3: Post-Diagnostic Story & Test (بعد الانتهاء من حل القصص المعروضة)
        item {
            StageHeaderBanner(
                stageNumber = "٣",
                title = "المرحلة الثالثة: الاختبار التشخيصي البعدي",
                subtitle = "قصة تشخيصية ختامية خاصة بعد الانتهاء من حل القصص المعروضة لقياس مقدار التحسن"
            )
            Spacer(modifier = Modifier.height(8.dp))
            DiagnosticStoryCard(
                story = uiState.postDiagnosticStory,
                isUnlocked = uiState.isPostTestUnlocked,
                isCompleted = uiState.isPostTestCompleted,
                score = activeProfile?.postTestScore ?: -1,
                total = activeProfile?.postTestTotal ?: 5,
                badgeText = "قصة الاختبار البعدي الخاصة",
                lockedReason = "أكمل حل قصص القرية العمانية الخمس أعلاه لفتح الاختبار البعدي تلقائياً (أو افتحه من لوحة التشخيص للمعلم).",
                onReadStory = { onOpenStoryReader(uiState.postDiagnosticStory) },
                onTakeQuiz = { onOpenStoryQuiz(uiState.postDiagnosticStory) },
                testTagPrefix = "post_diagnostic"
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Selected Omani Badge Detail Dialog
    selectedBadgeForDialog?.let { badge ->
        OmaniBadgeDetailDialog(
            badge = badge,
            isEarned = badge.id in earnedBadgesSet,
            currentPoints = uiState.totalTestPoints,
            onDismiss = { selectedBadgeForDialog = null }
        )
    }

    // All Omani Heritage Badges Full Gallery Dialog
    if (showAllBadgesDialog) {
        AllBadgesGalleryDialog(
            badges = OmaniCurriculumData.badges,
            earnedBadgeIds = earnedBadgesSet,
            currentPoints = uiState.totalTestPoints,
            currentStars = activeProfile?.stars ?: 0,
            readerLevel = uiState.readerLevel,
            onBadgeClick = { badge -> selectedBadgeForDialog = badge },
            onDismiss = { showAllBadgesDialog = false }
        )
    }
}

@Composable
private fun StageHeaderBanner(
    stageNumber: String,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stageNumber,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DiagnosticStoryCard(
    story: OmaniStory,
    isUnlocked: Boolean,
    isCompleted: Boolean,
    score: Int,
    total: Int,
    badgeText: String,
    lockedReason: String,
    onReadStory: () -> Unit,
    onTakeQuiz: () -> Unit,
    testTagPrefix: String
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            width = 2.dp,
            color = if (isUnlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isUnlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                if (isCompleted && score >= 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "الدرجة: $score / $total",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = story.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${story.villageName} • ${story.readingTimeMinutes} دقائق قراءة • ${story.questions.size} أسئلة تشخيصية",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = story.paragraphs.firstOrNull().orEmpty(),
                maxLines = 2,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(12.dp))
            if (isUnlocked) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onReadStory,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${testTagPrefix}_read_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اقرأ القصة المشكولة")
                    }
                    OutlinedButton(
                        onClick = onTakeQuiz,
                        modifier = Modifier.testTag("${testTagPrefix}_quiz_button")
                    ) {
                        Icon(Icons.Default.Quiz, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("الاختبار")
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = lockedReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CurriculumStoryCard(
    storyNumber: Int,
    story: OmaniStory,
    isCompleted: Boolean,
    onReadStory: () -> Unit,
    onTakeQuiz: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$storyNumber",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = story.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = story.villageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                if (isCompleted) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "مكتملة",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = story.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "المفردات العمانية: " + story.vocabulary.joinToString(" • ") { it.word },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onReadStory,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("story_${story.id}_read_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("قراءة واستماع")
                }
                OutlinedButton(
                    onClick = onTakeQuiz,
                    modifier = Modifier.testTag("story_${story.id}_quiz_button")
                ) {
                    Icon(Icons.Default.Quiz, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("أسئلة الفهم")
                }
            }
        }
    }
}

/**
 * قسم نظام النجوم والشارات التراثية العمانية (تحفيز الطفل بمجموع النقاط وإتمام القصص)
 */
@Composable
fun OmaniHeritageStarsAndBadgesCard(
    uiState: VillageUiState,
    earnedBadgesSet: Set<String>,
    onBadgeClick: (OmaniBadge) -> Unit,
    onViewAllBadges: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeProfile = uiState.activeProfile
    val currentStars = activeProfile?.stars ?: 0
    val totalPoints = uiState.totalTestPoints
    val level = uiState.readerLevel
    val allBadges = OmaniCurriculumData.badges

    // Calculate stars progress towards next rank
    val progressToNextLevel = if (level.maxStars > level.minStars) {
        ((currentStars - level.minStars).toFloat() / (level.maxStars - level.minStars).toFloat()).coerceIn(0f, 1f)
    } else 1f

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("stars_and_badges_section_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "نِظَامُ النُّجُومِ وَالشَّارَاتِ التُّرَاثِيَّةِ",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "اربح النجوم بعد كل قصة والشارات بمجموع نقاط الاختبارات!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Motivational Metric Boxes (الرتبة التراثية • النجوم • نقاط الاختبارات)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 1: Reader Heritage Rank
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "الرُّتْبَةُ القِرَائِيَّةُ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = level.titleWithEmoji,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }

                // Metric 2: Stars Earned
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(0.95f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "رَصِيدُ النُّجُومِ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "⭐ $currentStars",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                // Metric 3: Total Cumulative Quiz Points
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(0.95f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "نِقَاطُ الاِخْتِبَارَاتِ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "🎯 $totalPoints نُقْطَة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Level Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "التقدم نحو الترقية التراثية التالية:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currentStars / ${level.maxStars} ⭐",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progressToNextLevel },
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Badges Carousel Title & Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الشارات التراثية (${earnedBadgesSet.size} من ${allBadges.size} مكتسبة):",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "انقر الشارة للتفاصيل 👆",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges Preview Carousel
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(allBadges) { badge ->
                    val isEarned = badge.id in earnedBadgesSet
                    OmaniBadgeSummaryCard(
                        badge = badge,
                        isEarned = isEarned,
                        currentPoints = totalPoints,
                        onClick = { onBadgeClick(badge) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button: View Full Badges Hall of Fame
            FilledTonalButton(
                onClick = onViewAllBadges,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("view_all_badges_button")
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("عَرْضُ سِجِلِّ الشَّارَاتِ التُّرَاثِيَّةِ كَامِلاً 🏆")
            }
        }
    }
}

@Composable
fun OmaniBadgeSummaryCard(
    badge: OmaniBadge,
    isEarned: Boolean,
    currentPoints: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        border = if (isEarned) {
            BorderStroke(2.dp, Color(0xFFD4AF37)) // Gold border
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isEarned) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        modifier = modifier
            .width(135.dp)
            .clickable(onClick = onClick)
            .testTag("badge_card_${badge.id}")
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Emoji Icon with background
            Surface(
                shape = CircleShape,
                color = if (isEarned) Color(0xFFFFECC0) else MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = badge.iconEmoji,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = badge.titleAr,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = if (isEarned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = if (isEarned) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = if (isEarned) "مُكْتَسَبَةٌ ⭐" else if (badge.requiredPoints > 0) "🔒 ${badge.requiredPoints} ن" else "🔒 غير مكتمل",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isEarned) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

/**
 * نافذة تفاصيل الشارة التراثية العمانية المنبثقة
 */
@Composable
fun OmaniBadgeDetailDialog(
    badge: OmaniBadge,
    isEarned: Boolean,
    currentPoints: Int,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("badge_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (isEarned) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (isEarned) "🎉 وِسَامٌ عُمَانِيٌّ مُكْتَسَبٌ" else "🔒 قَيْدَ التَّحْقِيقِ",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isEarned) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Big Icon with Gold Aura
                Surface(
                    shape = CircleShape,
                    color = if (isEarned) Color(0xFFFFECC0) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(3.dp, if (isEarned) Color(0xFFD4AF37) else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.size(90.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = badge.iconEmoji,
                            fontSize = 48.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = badge.titleAr,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = badge.descriptionAr,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Points & Unlock Condition Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "شَرْطُ الحُصُولِ عَلَى الشَّارَةِ:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = badge.requiredConditionText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (badge.requiredPoints > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val progress = (currentPoints.toFloat() / badge.requiredPoints.toFloat()).coerceIn(0f, 1f)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "نقاطك الحالية في الاختبارات:",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "$currentPoints / ${badge.requiredPoints} نقطة",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }

                if (badge.heritageMeaningAr.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "🌴 الأَصَالَةُ وَالمَعْنَى التُّرَاثِيُّ فِي عُمَانَ:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = badge.heritageMeaningAr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إِغْلَاقٌ — مُتَابَعَةُ رِحْلَةِ القَرْيَةِ")
                }
            }
        }
    }
}

/**
 * نافذة عرض جميع الأوسمة والشارات التراثية العمانية (سجل الشارات الكامل)
 */
@Composable
fun AllBadgesGalleryDialog(
    badges: List<OmaniBadge>,
    earnedBadgeIds: Set<String>,
    currentPoints: Int,
    currentStars: Int,
    readerLevel: OmaniCurriculumData.ReaderLevel,
    onBadgeClick: (OmaniBadge) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxSize(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "سِجِلُّ الشَّارَاتِ التُّرَاثِيَّةِ العُمَانِيَّةِ",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "المكتسبة: ${earnedBadgeIds.size} من ${badges.size} • $currentStars ⭐ • $currentPoints 🎯",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable List of Badges
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(badges) { badge ->
                        val isEarned = badge.id in earnedBadgeIds
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            border = if (isEarned) BorderStroke(1.5.dp, Color(0xFFD4AF37)) else BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isEarned) {
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBadgeClick(badge) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isEarned) Color(0xFFFFECC0) else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = badge.iconEmoji, fontSize = 28.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = badge.titleAr,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEarned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isEarned) badge.descriptionAr else badge.requiredConditionText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isEarned) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = if (isEarned) "مكتسب 🌟" else "مغلق 🔒",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isEarned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إغلاق السجل")
                }
            }
        }
    }
}

