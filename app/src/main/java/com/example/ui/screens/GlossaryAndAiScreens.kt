package com.example.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OmaniCurriculumData
import com.example.data.model.OmaniStory
import java.util.Locale

@Composable
fun OmaniGlossaryScreen(
    customStories: List<OmaniStory>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var ttsReady by remember { mutableStateOf(false) }

    val tts = remember {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale("ar")
                ttsReady = true
            }
        }
        instance
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val allWords = remember(customStories) {
        OmaniCurriculumData.getAllGlossaryWords(customStories)
    }

    val filteredWords = remember(allWords, searchQuery) {
        if (searchQuery.isBlank()) allWords
        else allWords.filter { (word, storyTitle) ->
            word.word.contains(searchQuery, ignoreCase = true) ||
                word.meaning.contains(searchQuery, ignoreCase = true) ||
                storyTitle.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "مُعْجَمُ القَرْيَةِ العُمَانِيَّةِ المُصَوَّرُ",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "تَعَرَّفْ عَلَى مَعَانِي الكَلِمَاتِ وَالمُفْرَدَاتِ التُّرَاثِيَّةِ العُمَانِيَّةِ الَّتِي وَرَدَتْ فِي القِصَصِ وَاسْتَمِعْ إِلَى نُطْقِهَا الصَّحِيحِ.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث عن كلمة عمانية (مثل: الفلج، السبلة، القفير)...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("glossary_search_input")
                    )
                }
            }
        }

        items(filteredWords) { (word, storyTitle) ->
            ElevatedCard(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = word.word,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "وردت في: $storyTitle",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        FilledTonalIconButton(
                            onClick = {
                                if (ttsReady) {
                                    tts?.speak(
                                        "${word.word}. ${word.meaning}",
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "vocab_${word.word}"
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "نطق الكلمة"
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = word.meaning,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "مثال: «${word.exampleSentence}»",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    if (word.culturalNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "🌴 إضاءة تراثية: ${word.culturalNote}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiStoryGeneratorScreen(
    isGenerating: Boolean,
    errorMessage: String?,
    customStories: List<OmaniStory>,
    onGenerateStory: (topic: String, village: String, moral: String) -> Unit,
    onOpenStory: (OmaniStory) -> Unit,
    modifier: Modifier = Modifier
) {
    val presetTopics = listOf(
        "مُسَاعَدَةُ الجَدِّ فِي سَقْيِ النَّخِيلِ بِالفَلَجِ",
        "صِنَاعَةُ الفَخَّارِ الجَمِيلِ فِي قَرْيَةِ بَهْلَاءَ",
        "رِحْلَةُ صَيْدِ الأَسْمَاكِ مَعَ النُّوخَذَةِ",
        "إِكْرَامُ الضَّيْفِ فِي السَّبْلَةِ العُمَانِيَّةِ",
        "يَوْمُ الهَبْطَةِ وَفَرْحَةُ العِيدِ فِي الحَارَةِ"
    )
    val presetVillages = listOf(
        "قَرْيَةُ الجَبَلِ الأَخْضَرِ",
        "وَاحَةُ نَزْوَى العَرِيقَةُ",
        "قَرْيَةُ العِيجَةِ فِي صُورَ",
        "قَرْيَةُ مِسْفَاةِ العَبْرِيِّينَ",
        "سُهُولُ ظَفَارَ الخَضْرَاءُ"
    )
    val presetMorals = listOf(
        "التَّعَاوُنُ وَالعَمَلُ الجَمَاعِيُّ",
        "الصِّدْقُ وَالأَمَانَةُ",
        "إِكْرَامُ الضَّيْفِ وَالكَرَمُ العُمَانِيُّ",
        "بِرُّ الوَالِدَيْنِ وَاحْتِرَامُ الكِبَارِ",
        "المُحَافَظَةُ عَلَى المَاءِ وَالبيئةِ"
    )

    var selectedTopic by remember { mutableStateOf(presetTopics.first()) }
    var selectedVillage by remember { mutableStateOf(presetVillages.first()) }
    var selectedMoral by remember { mutableStateOf(presetMorals.first()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "حَكَوَاتِي القَرْيَةِ الذَّكِيُّ (AI)",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ابْتَكِرْ قِصَّةً عُمَانِيَّةً جَدِيدَةً مُشَكَّلَةً بِالكَامِلِ مَعَ مُفْرَدَاتِهَا التُّرَاثِيَّةِ وَأَسْئِلَةِ الفَهْمِ المَقْرُوءِ الخَمْسَةِ!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        item {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "١. اختر أو اكتب موضوع الحكاية العمانية:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetTopics.forEach { topic ->
                            FilterChip(
                                selected = selectedTopic == topic,
                                onClick = { selectedTopic = topic },
                                label = { Text(topic) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = selectedTopic,
                        onValueChange = { selectedTopic = it },
                        label = { Text("موضوع القصة") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_topic_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "٢. اختر القرية أو البيئة العمانية:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetVillages.forEach { village ->
                            FilterChip(
                                selected = selectedVillage == village,
                                onClick = { selectedVillage = village },
                                label = { Text(village) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "٣. اختر القيمة التربوية والأخلاقية:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetMorals.forEach { moral ->
                            FilterChip(
                                selected = selectedMoral == moral,
                                onClick = { selectedMoral = moral },
                                label = { Text(moral) }
                            )
                        }
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            onGenerateStory(selectedTopic, selectedVillage, selectedMoral)
                        },
                        enabled = !isGenerating && selectedTopic.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 15.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_ai_story_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("جارٍ تأليف القصة وتشكيلها بالذكاء الاصطناعي...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ألّف لي قصة عمانية مشكولة الآن!",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }

        if (customStories.isNotEmpty()) {
            item {
                Text(
                    text = "مكتبة القصص العمانية المبتكرة (${customStories.size}):",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(customStories) { story ->
                ElevatedCard(
                    onClick = { onOpenStory(story) },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = story.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${story.villageName} • ${story.questions.size} أسئلة فهم",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "اقرأ القصة ←",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
