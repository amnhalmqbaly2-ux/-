package com.example.ui.screens

import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.OmaniCurriculumData
import com.example.data.model.OmaniStory
import com.example.data.model.OmaniWord
import com.example.data.model.StoryType
import com.example.ui.theme.AmiriFontFamily
import java.util.Locale

private val ARABIC_DIACRITICS_REGEX = Regex("[\\u064B-\\u065F\\u0670]")

private fun Char.isArabicDiacritic(): Boolean {
    val code = this.code
    return (code in 0x064B..0x065F) || code == 0x0670
}

/**
 * Normalizes an Arabic word by stripping diacritics, punctuation, and common prefixes/articles
 * so inline words in a story paragraph accurately match Omani glossary entries.
 */
private fun normalizeArabicForMatch(input: String): String {
    val stripped = input
        .replace(ARABIC_DIACRITICS_REGEX, "")
        .replace(Regex("[^\\u0621-\\u064A]"), "")
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace('ٱ', 'ا')
        .replace('ة', 'ه')
        .replace('ى', 'ي')

    // Strip common proclitics (و، ف، ب، ك، ل) + definite article (ال)
    val withoutPrefix = when {
        stripped.startsWith("وبال") && stripped.length > 6 -> stripped.removePrefix("وبال")
        stripped.startsWith("فبال") && stripped.length > 6 -> stripped.removePrefix("فبال")
        stripped.startsWith("وال") && stripped.length > 5 -> stripped.removePrefix("وال")
        stripped.startsWith("فال") && stripped.length > 5 -> stripped.removePrefix("فال")
        stripped.startsWith("بال") && stripped.length > 5 -> stripped.removePrefix("بال")
        stripped.startsWith("كال") && stripped.length > 5 -> stripped.removePrefix("كال")
        stripped.startsWith("لل") && stripped.length > 4 -> stripped.removePrefix("لل")
        stripped.startsWith("ال") && stripped.length > 4 -> stripped.removePrefix("ال")
        else -> stripped
    }
    return withoutPrefix
}

/**
 * Matches a token from a paragraph against the story's vocabulary and the full Omani glossary.
 */
private fun findMatchingOmaniWord(
    token: String,
    storyVocabulary: List<OmaniWord>,
    allGlossaryWords: List<OmaniWord>
): OmaniWord? {
    val normToken = normalizeArabicForMatch(token)
    if (normToken.length < 3) return null

    val candidates = storyVocabulary + allGlossaryWords
    for (entry in candidates) {
        // An entry like "الطَّلْعُ (الصُّوعُ)" or "خُبْزُ الرُّخَالِ" may have multiple words
        val entryParts = entry.word
            .replace("(", " ")
            .replace(")", " ")
            .split(" ")
            .map { normalizeArabicForMatch(it) }
            .filter { it.length >= 3 && it != "خبز" && it != "عين" && it != "يوم" }

        for (stem in entryParts) {
            if (normToken == stem ||
                (stem.length >= 4 && normToken.contains(stem)) ||
                (normToken.length >= 4 && stem.contains(normToken))
            ) {
                return entry
            }
        }
    }
    return null
}

/**
 * Builds an interactive AnnotatedString for a vocalized Arabic paragraph:
 * 1. Colors Arabic diacritics (الحركات والتشكيل) distinctly when `colorizeDiacritics` is active.
 * 2. Detects Omani heritage words inline and attaches a `LinkAnnotation.Clickable` so tapping
 *    any heritage word directly in the paragraph opens its definition popup dialog!
 */
private fun buildInteractiveVocalizedParagraph(
    paragraph: String,
    storyVocabulary: List<OmaniWord>,
    allGlossaryWords: List<OmaniWord>,
    fontSizeSp: Float,
    colorizeDiacritics: Boolean,
    baseTextColor: Color,
    diacriticColor: Color,
    heritageWordColor: Color,
    heritageWordBgColor: Color,
    onWordClick: (OmaniWord) -> Unit
): AnnotatedString = buildAnnotatedString {
    pushStyle(
        SpanStyle(
            fontFamily = AmiriFontFamily,
            fontSize = fontSizeSp.sp,
            color = baseTextColor
        )
    )

    // Split keeping whitespace so character offsets are exact
    val tokenRegex = Regex("\\S+")
    var lastIndex = 0

    for (match in tokenRegex.findAll(paragraph)) {
        // Append any preceding whitespace
        if (match.range.first > lastIndex) {
            append(paragraph.substring(lastIndex, match.range.first))
        }

        val tokenText = match.value
        val matchedHeritageWord = findMatchingOmaniWord(
            token = tokenText,
            storyVocabulary = storyVocabulary,
            allGlossaryWords = allGlossaryWords
        )

        if (matchedHeritageWord != null) {
            // Wrap the inline Omani heritage word in a clickable LinkAnnotation
            val link = LinkAnnotation.Clickable(
                tag = "omani_word_${matchedHeritageWord.word}",
                styles = TextLinkStyles(
                    style = SpanStyle(
                        fontFamily = AmiriFontFamily,
                        fontSize = fontSizeSp.sp,
                        fontWeight = FontWeight.Bold,
                        color = heritageWordColor,
                        background = heritageWordBgColor,
                        textDecoration = TextDecoration.Underline
                    )
                ),
                linkInteractionListener = {
                    onWordClick(matchedHeritageWord)
                }
            )
            pushLink(link)
            append(tokenText)
            pop()
        } else if (colorizeDiacritics) {
            // Render base letters and colorize Arabic diacritics (الحركات)
            for (ch in tokenText) {
                if (ch.isArabicDiacritic()) {
                    pushStyle(
                        SpanStyle(
                            color = diacriticColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    append(ch)
                    pop()
                } else {
                    append(ch)
                }
            }
        } else {
            append(tokenText)
        }

        lastIndex = match.range.last + 1
    }

    if (lastIndex < paragraph.length) {
        append(paragraph.substring(lastIndex))
    }

    pop()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StoryReaderScreen(
    story: OmaniStory,
    isAlreadyRead: Boolean = false,
    onCompleteReading: () -> Unit = {},
    onBack: () -> Unit,
    onStartQuiz: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var fontSizeSp by remember { mutableFloatStateOf(24f) }
    var speechRate by remember { mutableFloatStateOf(0.85f) }
    var colorizeDiacritics by remember { mutableStateOf(true) }
    var isPagedBookMode by remember { mutableStateOf(false) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var activeSpeakingParagraph by remember { mutableIntStateOf(-1) }
    var selectedWordForPopup by remember { mutableStateOf<OmaniWord?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var readingCompletedCelebrated by remember { mutableStateOf(isAlreadyRead) }

    val allGlossaryWords = remember {
        OmaniCurriculumData.getAllGlossaryWords().map { it.first }
    }

    val tts = remember {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale.forLanguageTag("ar")
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

    fun speakText(text: String, paragraphIndex: Int = -1) {
        if (!ttsReady) return
        activeSpeakingParagraph = paragraphIndex
        tts?.setSpeechRate(speechRate)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "story_utt_$paragraphIndex")
    }

    fun stopSpeaking() {
        tts?.stop()
        activeSpeakingParagraph = -1
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = story.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Text(
                            text = "${story.villageName} • ${story.storyType.labelAr}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            stopSpeaking()
                            onBack()
                        },
                        modifier = Modifier.testTag("reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { colorizeDiacritics = !colorizeDiacritics },
                        modifier = Modifier.testTag("toggle_diacritics_color_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "تلوين التشكيل والحركات",
                            tint = if (colorizeDiacritics) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    IconButton(
                        onClick = { fontSizeSp = (fontSizeSp - 2f).coerceAtLeast(18f) },
                        modifier = Modifier.testTag("decrease_font_button")
                    ) {
                        Icon(Icons.Default.TextDecrease, contentDescription = "تصغير الخط")
                    }
                    IconButton(
                        onClick = { fontSizeSp = (fontSizeSp + 2f).coerceAtMost(34f) },
                        modifier = Modifier.testTag("increase_font_button")
                    ) {
                        Icon(Icons.Default.TextIncrease, contentDescription = "تكبير الخط")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Story Illustration & Interactive Controls Header
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box {
                        Image(
                            painter = painterResource(id = R.drawable.img_omani_story_header),
                            contentDescription = story.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
                            shape = RoundedCornerShape(bottomEnd = 16.dp),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Text(
                                text = story.storyType.labelAr,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = story.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = story.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Audio Narration & Reading Speed Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (activeSpeakingParagraph == -1) {
                                FilledTonalButton(
                                    onClick = {
                                        val fullStory = story.title + ". " + story.paragraphs.joinToString(" ")
                                        speakText(fullStory, 999)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("listen_full_story_button")
                                ) {
                                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("استمع للقصة كاملةً")
                                }
                            } else {
                                Button(
                                    onClick = { stopSpeaking() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("stop_listening_button")
                                ) {
                                    Icon(Icons.Default.StopCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("إيقاف القراءة الصوتية")
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    speechRate = if (speechRate < 0.9f) 1.0f else 0.75f
                                }
                            ) {
                                Text(if (speechRate < 0.9f) "السرعة: هادئة" else "السرعة: عادية")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Reading Mode & Diacritics Toolbar Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = colorizeDiacritics,
                                onClick = { colorizeDiacritics = !colorizeDiacritics },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AutoFixHigh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = {
                                    Text(if (colorizeDiacritics) "تلوين الحركات: مفعّل" else "تلوين الحركات: عادي")
                                }
                            )
                            FilterChip(
                                selected = isPagedBookMode,
                                onClick = {
                                    isPagedBookMode = !isPagedBookMode
                                    currentPageIndex = 0
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isPagedBookMode) Icons.Default.ViewCarousel else Icons.Default.ViewAgenda,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = {
                                    Text(if (isPagedBookMode) "وضع صفحات الكتاب" else "وضع القصة الكاملة")
                                },
                                modifier = Modifier.testTag("toggle_reading_mode_chip")
                            )
                        }
                    }
                }
            }

            // 2. Interactive Guide Banner & Quick Vocabulary Chips
            if (story.vocabulary.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "انْقُرْ عَلَى الكَلِمَاتِ العُمَانِيَّةِ المُلَوَّنَةِ دَاخِلَ النَّصِّ أَوْ هُنَا لِعَرْضِ نَافِذَةِ مَعْنَاهَا:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                story.vocabulary.forEachIndexed { idx, vocab ->
                                    AssistChip(
                                        onClick = { selectedWordForPopup = vocab },
                                        label = {
                                            Text(
                                                text = vocab.word,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        modifier = Modifier.testTag("vocab_chip_$idx")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Vocalized Interactive Story Paragraphs (Paged Book Mode OR Full Scroll Mode)
            if (isPagedBookMode && story.paragraphs.isNotEmpty()) {
                val safePageIdx = currentPageIndex.coerceIn(0, story.paragraphs.size - 1)
                val paragraph = story.paragraphs[safePageIdx]
                item {
                    InteractiveParagraphCard(
                        index = safePageIdx,
                        totalParagraphs = story.paragraphs.size,
                        paragraph = paragraph,
                        storyVocabulary = story.vocabulary,
                        allGlossaryWords = allGlossaryWords,
                        fontSizeSp = fontSizeSp,
                        colorizeDiacritics = colorizeDiacritics,
                        isSpeakingThis = activeSpeakingParagraph == safePageIdx,
                        onToggleSpeak = {
                            if (activeSpeakingParagraph == safePageIdx) {
                                stopSpeaking()
                            } else {
                                speakText(paragraph, safePageIdx)
                            }
                        },
                        onWordClick = { word -> selectedWordForPopup = word }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                            enabled = currentPageIndex > 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("الصفحة السابقة")
                        }
                        Text(
                            text = "صفحة ${safePageIdx + 1} من ${story.paragraphs.size}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FilledTonalButton(
                            onClick = {
                                if (currentPageIndex < story.paragraphs.size - 1) currentPageIndex++
                            },
                            enabled = currentPageIndex < story.paragraphs.size - 1
                        ) {
                            Text("الصفحة التالية")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            } else {
                itemsIndexed(story.paragraphs) { index, paragraph ->
                    InteractiveParagraphCard(
                        index = index,
                        totalParagraphs = story.paragraphs.size,
                        paragraph = paragraph,
                        storyVocabulary = story.vocabulary,
                        allGlossaryWords = allGlossaryWords,
                        fontSizeSp = fontSizeSp,
                        colorizeDiacritics = colorizeDiacritics,
                        isSpeakingThis = activeSpeakingParagraph == index,
                        onToggleSpeak = {
                            if (activeSpeakingParagraph == index) {
                                stopSpeaking()
                            } else {
                                speakText(paragraph, index)
                            }
                        },
                        onWordClick = { word -> selectedWordForPopup = word }
                    )
                }
            }

            // 4. Story Moral / Wisdom Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "حِكْمَةُ الحِكَايَةِ:",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = story.moral,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // 5. Star Rewards for Reading Completion (نظام نجوم القراءة بعد إتمام قراءة القصة)
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (readingCompletedCelebrated) {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        }
                    ),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (readingCompletedCelebrated) {
                                    "🌟 أَحْسَنْتَ! حَصَلْتَ عَلَى نُجُومِ القِرَاءَةِ التُّرَاثِيَّةِ"
                                } else {
                                    "🌟 هَلْ أَتْمَمْتَ قِرَاءَةَ هَذِهِ القِصَّةِ كَامِلَةً؟"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (readingCompletedCelebrated) {
                                "تمت إضافة نجوم إتمام القراءة لرصيدك! انتقل الآن لاختبار الفهم المقروء لجمع المزيد من النقاط والشارات."
                            } else {
                                "اضغط الزر بالأسفل لاستلام +١٠ نجوم قراءة تضاف إلى رصيدك التراثي وتساعدك في فتح شارات جديدة!"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        if (!readingCompletedCelebrated) {
                            Spacer(modifier = Modifier.height(12.dp))
                            FilledTonalButton(
                                onClick = {
                                    readingCompletedCelebrated = true
                                    onCompleteReading()
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.testTag("claim_reading_stars_button")
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("اسْتَلِمْ +١٠ نُجُومِ القِرَاءَةِ الآنَ! 🌟")
                            }
                        }
                    }
                }
            }

            // 6. Primary Action CTA: Start Comprehension Test
            item {
                Button(
                    onClick = {
                        stopSpeaking()
                        onStartQuiz()
                    },
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_quiz_button")
                ) {
                    Icon(Icons.Default.Quiz, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    val buttonLabel = when (story.storyType) {
                        StoryType.PRE_DIAGNOSTIC -> "ابدأ الاختبار التشخيصي القبلي (٥ أسئلة)"
                        StoryType.POST_DIAGNOSTIC -> "ابدأ الاختبار التشخيصي البعدي (٥ أسئلة)"
                        else -> "ابدأ أسئلة الفهم المقروء للقصة (${story.questions.size} أسئلة)"
                    }
                    Text(
                        text = buttonLabel,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Interactive Popup Window (نافذة منبثقة لتعريف الكلمة العمانية التراثية من المعجم)
    selectedWordForPopup?.let { word ->
        OmaniWordDefinitionPopupDialog(
            word = word,
            storyTitle = story.title,
            onSpeakWord = {
                speakText("${word.word}. ${word.meaning}. مِثَالٌ: ${word.exampleSentence}")
            },
            onDismiss = { selectedWordForPopup = null }
        )
    }
}

@Composable
private fun InteractiveParagraphCard(
    index: Int,
    totalParagraphs: Int,
    paragraph: String,
    storyVocabulary: List<OmaniWord>,
    allGlossaryWords: List<OmaniWord>,
    fontSizeSp: Float,
    colorizeDiacritics: Boolean,
    isSpeakingThis: Boolean,
    onToggleSpeak: () -> Unit,
    onWordClick: (OmaniWord) -> Unit
) {
    val baseTextColor = MaterialTheme.colorScheme.onSurface
    val diacriticColor = MaterialTheme.colorScheme.secondary
    val heritageWordColor = MaterialTheme.colorScheme.primary
    val heritageWordBgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)

    val annotatedParagraph = remember(
        paragraph,
        storyVocabulary,
        fontSizeSp,
        colorizeDiacritics,
        baseTextColor,
        diacriticColor,
        heritageWordColor,
        heritageWordBgColor
    ) {
        buildInteractiveVocalizedParagraph(
            paragraph = paragraph,
            storyVocabulary = storyVocabulary,
            allGlossaryWords = allGlossaryWords,
            fontSizeSp = fontSizeSp,
            colorizeDiacritics = colorizeDiacritics,
            baseTextColor = baseTextColor,
            diacriticColor = diacriticColor,
            heritageWordColor = heritageWordColor,
            heritageWordBgColor = heritageWordBgColor,
            onWordClick = onWordClick
        )
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        border = if (isSpeakingThis) {
            BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isSpeakingThis) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("story_paragraph_card_$index")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = "الفقرة ${index + 1} من $totalParagraphs",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onToggleSpeak,
                    modifier = Modifier.testTag("speak_paragraph_button_$index")
                ) {
                    Icon(
                        imageVector = if (isSpeakingThis) Icons.Default.StopCircle else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "نطق الفقرة صوتياً",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = annotatedParagraph,
                lineHeight = (fontSizeSp * 1.92f).sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * نافذة منبثقة تفاعلية (Popup Dialog) تعرض تعريف الكلمة العمانية التراثية من معجم القرية
 * مع التشكيل الكامل، النطق الصوتي، المثال السياقي، والإضاءة التراثية.
 */
@Composable
fun OmaniWordDefinitionPopupDialog(
    word: OmaniWord,
    storyTitle: String,
    onSpeakWord: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("omani_word_popup_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Dialog Header Badge + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مِنْ مُعْجَمِ القَرْيَةِ العُمَانِيَّةِ",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_word_popup_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق النافذة"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Vocalized Word Title + Audio Pronunciation Button
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = word.word,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 34.sp
                                ),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "وردت في قصة: $storyTitle",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        FilledTonalButton(
                            onClick = onSpeakWord,
                            modifier = Modifier.testTag("speak_popup_word_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "نطق الكلمة")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استمع للنطق")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Definition Section
                Text(
                    text = "التَّعْرِيفُ وَالمَعْنَى:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = word.meaning,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 22.sp,
                        lineHeight = 36.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Contextual Example Sentence from Story
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "مِثَالٌ مِنْ سِيَاقِ القِصَّةِ:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "«${word.exampleSentence}»",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 20.sp,
                                lineHeight = 34.sp
                            )
                        )
                    }
                }

                if (word.culturalNote.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "🌴 إِضَاءَةٌ مِنَ التُّرَاثِ العُمَانِيِّ: ${word.culturalNote}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("فَهِمْتُ مَعْنَى الكَلِمَةِ — مُتَابَعَةُ القِرَاءَةِ")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryQuizScreen(
    story: OmaniStory,
    onBackToStory: () -> Unit,
    onQuizFinished: (Map<String, Int>) -> Unit
) {
    BackHandler { onBackToStory() }

    val questions = story.questions
    var currentIndex by remember { mutableIntStateOf(0) }
    val selectedAnswers = remember { mutableStateMapOf<String, Int>() }
    var showExplanationForCurrent by remember { mutableStateOf(false) }
    var isQuizCompleted by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale.forLanguageTag("ar")
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (story.storyType) {
                                StoryType.PRE_DIAGNOSTIC -> "الاختبار التشخيصي القبلي"
                                StoryType.POST_DIAGNOSTIC -> "الاختبار التشخيصي البعدي"
                                else -> "اختبار فهم القصة"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = story.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToStory,
                        modifier = Modifier.testTag("quiz_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "العودة للقصة"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (questions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد أسئلة متاحة لهذه القصة.")
            }
            return@Scaffold
        }

        if (isQuizCompleted) {
            QuizResultSummaryView(
                story = story,
                selectedAnswers = selectedAnswers,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onConfirmFinish = { onQuizFinished(selectedAnswers.toMap()) }
            )
        } else {
            val currentQuestion = questions[currentIndex]
            val chosenOption = selectedAnswers[currentQuestion.id]

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "السؤال ${currentIndex + 1} من ${questions.size}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "مهارة: ${currentQuestion.skill.labelAr}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (currentIndex + 1).toFloat() / questions.size.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                        )
                    }
                }

                // Question Card
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentQuestion.skill.descriptionAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        if (ttsReady) {
                                            tts?.speak(
                                                currentQuestion.questionText,
                                                TextToSpeech.QUEUE_FLUSH,
                                                null,
                                                "q_${currentQuestion.id}"
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "قراءة السؤال صوتياً",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentQuestion.questionText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Options List
                itemsIndexed(currentQuestion.options) { optIdx, optionText ->
                    val isSelected = chosenOption == optIdx
                    val isCorrectOption = optIdx == currentQuestion.correctIndex

                    val borderColor = when {
                        showExplanationForCurrent && isCorrectOption -> MaterialTheme.colorScheme.primary
                        showExplanationForCurrent && isSelected && !isCorrectOption -> MaterialTheme.colorScheme.error
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }

                    val containerColor = when {
                        showExplanationForCurrent && isCorrectOption -> MaterialTheme.colorScheme.primaryContainer
                        showExplanationForCurrent && isSelected && !isCorrectOption -> MaterialTheme.colorScheme.errorContainer
                        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, borderColor),
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_option_$optIdx")
                            .clickable(enabled = !showExplanationForCurrent) {
                                selectedAnswers[currentQuestion.id] = optIdx
                                showExplanationForCurrent = true
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    val arabicLetters = listOf("أ", "ب", "ج", "د")
                                    Text(
                                        text = arabicLetters.getOrElse(optIdx) { "${optIdx + 1}" },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Educational Feedback Card
                item {
                    AnimatedVisibility(visible = showExplanationForCurrent && chosenOption != null) {
                        val isCorrect = chosenOption == currentQuestion.correctIndex
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCorrect) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                }
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (isCorrect) {
                                        "🌟 أَحْسَنْتَ يَا بَطَلَ القِرَاءَةِ! إِجَابَةٌ صَحِيحَةٌ."
                                    } else {
                                        "💡 لِنَتَعَلَّمْ مَعًا الإِجَابَةَ الأَدَقَّ:"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentQuestion.explanation,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp)
                                )
                            }
                        }
                    }
                }

                // Next / Finish Button
                item {
                    Button(
                        onClick = {
                            if (currentIndex < questions.size - 1) {
                                currentIndex++
                                showExplanationForCurrent = false
                            } else {
                                isQuizCompleted = true
                            }
                        },
                        enabled = chosenOption != null,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 15.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_next_button")
                    ) {
                        Text(
                            text = if (currentIndex < questions.size - 1) {
                                "السؤال التالي"
                            } else {
                                "عرض النتيجة التشخيصية"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizResultSummaryView(
    story: OmaniStory,
    selectedAnswers: Map<String, Int>,
    modifier: Modifier = Modifier,
    onConfirmFinish: () -> Unit
) {
    val total = story.questions.size.coerceAtLeast(1)
    val correctCount = story.questions.count { q -> selectedAnswers[q.id] == q.correctIndex }
    val percentage = (correctCount * 100) / total
    val earnedStars = correctCount * 3 + 5

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ElevatedCard(
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when (story.storyType) {
                            StoryType.PRE_DIAGNOSTIC -> "اكتمل الاختبار التشخيصي القبلي!"
                            StoryType.POST_DIAGNOSTIC -> "اكتمل الاختبار التشخيصي البعدي!"
                            else -> "أحسنت! أتممت فهم القصة"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "النتيجة: $correctCount من $total ($percentage%)",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ربحت +$earnedStars نجمة عمانية!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Breakdown by skill
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "تفصيل مهارات الفهم المقروء في هذا الاختبار:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    story.questions.forEach { q ->
                        val ok = selectedAnswers[q.id] == q.correctIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "• ${q.skill.labelAr}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (ok) "متقن ✅" else "يحتاج تدريباً 🌱",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onConfirmFinish,
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("finish_quiz_confirm_button")
            ) {
                Text(
                    text = "حفظ النتيجة والعودة إلى رحلة القرية",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
