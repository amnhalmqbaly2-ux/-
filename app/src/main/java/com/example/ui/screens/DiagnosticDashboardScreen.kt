package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AssessmentRecordEntity
import com.example.data.model.ComprehensionSkill
import com.example.data.model.OmaniCurriculumData
import com.example.ui.VillageUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiagnosticDashboardScreen(
    uiState: VillageUiState,
    onSelectProfile: (String) -> Unit,
    onAddProfile: (name: String, age: Int, avatarId: Int) -> Unit,
    onToggleTeacherUnlockPostTest: () -> Unit,
    onStartPreTest: () -> Unit,
    onStartPostTest: () -> Unit,
    onSyncNow: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddProfileDialog by remember { mutableStateOf(false) }
    val activeProfile = uiState.activeProfile
    val preRecord = uiState.latestPreTestRecord
    val postRecord = uiState.latestPostTestRecord

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "لَوْحَةُ المُتَابَعَةِ وَالتَّشْخِيصِ التَّرْبَوِيِّ",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "مُقَارَنَةُ الفَهْمِ المَقْرُوءِ (قَبْلَ القِصَصِ وَبَعْدَهَا) لِلْأَهْلِ وَالمُعَلِّمِينَ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Sync & Connectivity Info Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (uiState.isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = if (uiState.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isOnline) "متصل بالإنترنت" else "غير متصل (حفظ محلي)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        if (uiState.isCloudSynced) {
                            FilledTonalButton(
                                onClick = onSyncNow,
                                enabled = !uiState.isSyncing && uiState.isOnline,
                                modifier = Modifier.testTag("dashboard_sync_button")
                            ) {
                                if (uiState.isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مزامنة...", style = MaterialTheme.typography.labelSmall)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مزامنة سحابية", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    uiState.syncStatusMessage?.let { statusMsg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(10.dp)
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
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Reader Profile Switcher Card
        item {
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
                        Text(
                            text = "ملفات القرّاء الصغار:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FilledTonalButton(
                            onClick = { showAddProfileDialog = true },
                            modifier = Modifier.testTag("add_child_profile_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة طفل")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.profiles.forEach { profile ->
                            FilterChip(
                                selected = profile.id == activeProfile?.id,
                                onClick = { onSelectProfile(profile.id) },
                                label = {
                                    Text("${profile.name} (${profile.age} سنوات • ⭐${profile.stars})")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Pre vs Post Diagnostic Comparison Card
        item {
            ElevatedCard(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "مقارنة الاختبار التشخيصي (القبلي ↔ البعدي)",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val preScore = activeProfile?.preTestScore ?: -1
                    val preTotal = (activeProfile?.preTestTotal ?: 5).coerceAtLeast(1)
                    val postScore = activeProfile?.postTestScore ?: -1
                    val postTotal = (activeProfile?.postTestTotal ?: 5).coerceAtLeast(1)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Pre-test box
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "الاختبار القبلي",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (preScore >= 0) "$preScore / $preTotal" else "لم يُختبر بعد",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = if (preScore >= 0) "${(preScore * 100) / preTotal}%" else "قصة: صباح في قرية النخيل",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                                if (preScore < 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    TextButton(onClick = onStartPreTest) {
                                        Text("ابدأ الآن")
                                    }
                                }
                            }
                        }

                        // Post-test box
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "الاختبار البعدي",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (postScore >= 0) "$postScore / $postTotal" else "لم يُختبر بعد",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (postScore >= 0) "${(postScore * 100) / postTotal}%" else "قصة: ضيافة القلعة العريقة",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                                if (postScore < 0 && uiState.isPostTestUnlocked) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    TextButton(onClick = onStartPostTest) {
                                        Text("ابدأ الآن")
                                    }
                                }
                            }
                        }
                    }

                    if (preScore >= 0 && postScore >= 0) {
                        val prePct = (preScore * 100) / preTotal
                        val postPct = (postScore * 100) / postTotal
                        val diff = postPct - prePct
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (diff >= 0) {
                                    "📈 مؤشر النمو القرائي: تحسّن بنسبة +$diff% بعد قراءة قصص القرية العمانية!"
                                } else {
                                    "📊 مؤشر الأداء: ننصح بإعادة قراءة القصص التدريبية لتعزيز مهارات الاستنتاج."
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Teacher manual unlock switch for Post-Test
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
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "فتح الاختبار البعدي يدوياً (للمعلم / ولي الأمر)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "يُفتح تلقائياً بعد إتمام القصص الـ ٥ (${uiState.completedCurriculumCount}/٥ منجزة حالياً)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = activeProfile?.postTestUnlockedByTeacher == true,
                            onCheckedChange = { onToggleTeacherUnlockPostTest() },
                            modifier = Modifier.testTag("teacher_unlock_post_test_switch")
                        )
                    }
                }
            }
        }

        // Skill-by-Skill Breakdown Card
        item {
            SkillComparisonCard(
                preRecord = preRecord,
                postRecord = postRecord
            )
        }

        // Omani Heritage Badges Card
        item {
            val earnedIds = activeProfile?.badgesList().orEmpty().toSet()
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الأوسمة والشارات التراثية العمانية (${earnedIds.size}/${OmaniCurriculumData.badges.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OmaniCurriculumData.badges.forEach { badge ->
                        val isEarned = badge.id in earnedIds
                        Surface(
                            color = if (isEarned) {
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = badge.iconEmoji,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = badge.titleAr,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isEarned) badge.descriptionAr else "الشرط: ${badge.requiredConditionText}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = if (isEarned) "مكتسب 🌟" else "مغلق 🔒",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isEarned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Assessment History Log
        if (uiState.activeProfileAssessments.isNotEmpty()) {
            item {
                Text(
                    text = "سجل التقييمات والاختبارات السابقة:",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(uiState.activeProfileAssessments) { rec ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rec.storyTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val typeLabel = when (rec.assessmentType) {
                                "PRE_TEST" -> "اختبار تشخيصي قبلي"
                                "POST_TEST" -> "اختبار تشخيصي بعدي"
                                else -> "تدريب فهم مقروء"
                            }
                            Text(
                                text = typeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${rec.score} / ${rec.totalQuestions}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddProfileDialog) {
        AddChildProfileDialog(
            onDismiss = { showAddProfileDialog = false },
            onConfirm = { name, age ->
                onAddProfile(name, age, 0)
                showAddProfileDialog = false
            }
        )
    }
}

@Composable
private fun SkillComparisonCard(
    preRecord: AssessmentRecordEntity?,
    postRecord: AssessmentRecordEntity?
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "تحليل مهارات الفهم المقروء الخمس (قبلي مقابل بعدي):",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))

            val skillRows = listOf(
                Triple(ComprehensionSkill.LITERAL.labelAr, preRecord?.literalScore, postRecord?.literalScore),
                Triple(ComprehensionSkill.VOCABULARY.labelAr, preRecord?.vocabularyScore, postRecord?.vocabularyScore),
                Triple(ComprehensionSkill.SEQUENCING.labelAr, preRecord?.sequencingScore, postRecord?.sequencingScore),
                Triple(ComprehensionSkill.INFERENCE.labelAr, preRecord?.inferenceScore, postRecord?.inferenceScore),
                Triple(ComprehensionSkill.MAIN_IDEA.labelAr, preRecord?.mainIdeaScore, postRecord?.mainIdeaScore)
            )

            skillRows.forEach { (skillName, preVal, postVal) ->
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = skillName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val preStr = if (preVal != null) (if (preVal > 0) "متقن ✓" else "غير متقن") else "—"
                        val postStr = if (postVal != null) (if (postVal > 0) "متقن 🌟" else "غير متقن") else "—"
                        Text(
                            text = "قبلي: $preStr  |  بعدي: $postStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val progress = when {
                        postVal != null && postVal > 0 -> 1f
                        preVal != null && preVal > 0 -> 0.6f
                        preVal != null || postVal != null -> 0.25f
                        else -> 0.05f
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun AddChildProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, age: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableIntStateOf(8) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة ملف قارئ صغير جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الطفل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("العمر: $age سنوات", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (6..10).forEach { yr ->
                        FilterChip(
                            selected = age == yr,
                            onClick = { age = yr },
                            label = { Text("$yr") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, age) },
                enabled = name.isNotBlank()
            ) {
                Text("حفظ الملف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
