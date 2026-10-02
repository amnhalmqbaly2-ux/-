package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.VillageDatabase
import com.example.data.model.OmaniStory
import com.example.data.remote.VillageCloudRepository
import com.example.ui.VillageViewModel
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.authStateFlow
import com.example.ui.screens.AiStoryGeneratorScreen
import com.example.ui.screens.DiagnosticDashboardScreen
import com.example.ui.screens.OmaniGlossaryScreen
import com.example.ui.screens.StoryQuizScreen
import com.example.ui.screens.StoryReaderScreen
import com.example.ui.screens.VillageJourneyScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class MainTab(val labelAr: String, val tag: String) {
    JOURNEY("رحلة القرية", "tab_journey"),
    GLOSSARY("معجم القرية", "tab_glossary"),
    AI_STORY("حكواتي الذكاء", "tab_ai_story"),
    DASHBOARD("لوحة التشخيص", "tab_dashboard")
}

sealed interface ActiveSubScreen {
    data object None : ActiveSubScreen
    data class Reader(val story: OmaniStory) : ActiveSubScreen
    data class Quiz(val story: OmaniStory) : ActiveSubScreen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                OmaniVillageStorybookApp()
            }
        }
    }
}

@Composable
fun OmaniVillageStorybookApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel: VillageViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(databaseId)
                val roomDb = VillageDatabase.getInstance(app)
                val networkMonitor = com.example.data.remote.NetworkMonitor(app)
                VillageViewModel(
                    dao = roomDb.villageDao(),
                    cloudRepository = VillageCloudRepository(firestore),
                    networkMonitor = networkMonitor
                )
            }
        }
    )

    val auth = remember {
        try {
            Firebase.auth
        } catch (e: Exception) {
            null
        }
    }

    val currentUser by if (auth != null) {
        auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    } else {
        remember { mutableStateOf(null) }
    }

    LaunchedEffect(Unit) {
        if (auth != null && auth.currentUser == null) {
            val credentialManager = CredentialManager.create(context)
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    viewModel.onAuthUserChanged(auth.currentUser?.uid, auth.currentUser?.email)
                },
                onUnauthenticated = {
                    viewModel.onAuthUserChanged(null, null)
                },
                scope = scope
            )
        }
    }

    LaunchedEffect(currentUser?.uid) {
        viewModel.onAuthUserChanged(currentUser?.uid, currentUser?.email)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(MainTab.JOURNEY) }
    var subScreen by remember { mutableStateOf<ActiveSubScreen>(ActiveSubScreen.None) }

    // Badge Celebration Dialog (احتفال الفوز بوسام وشارة تراثية جديدة)
    if (uiState.newlyEarnedBadgeTitles.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBadgeCelebration() },
            title = {
                Text(
                    text = "🏆 مُبَارَكٌ! وِسَامٌ عُمَانِيٌّ جَدِيدٌ!",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = "أَحْسَنْتَ صُنْعًا يَا بَطَلَ القِرَاءَةِ العُمَانِيَّ! لَقَدْ حَقَّقْتَ إِنجَازًا رَائِعًا وَحَصَلْتَ عَلَى:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    uiState.newlyEarnedBadgeTitles.forEach { badgeTitle ->
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "🌟 $badgeTitle",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissBadgeCelebration() },
                    modifier = Modifier.testTag("dismiss_celebration_button")
                ) {
                    Text("مُتَابَعَةُ رِحْلَةِ التَّحَدِّي 🚀")
                }
            }
        )
    }

    when (val active = subScreen) {
        is ActiveSubScreen.Reader -> {
            val isRead = active.story.id in (uiState.activeProfile?.completedStoryIdsList().orEmpty().toSet())
            StoryReaderScreen(
                story = active.story,
                isAlreadyRead = isRead,
                onCompleteReading = {
                    viewModel.markStoryReadingCompleted(active.story)
                },
                onBack = { subScreen = ActiveSubScreen.None },
                onStartQuiz = { subScreen = ActiveSubScreen.Quiz(active.story) }
            )
        }

        is ActiveSubScreen.Quiz -> {
            StoryQuizScreen(
                story = active.story,
                onBackToStory = { subScreen = ActiveSubScreen.Reader(active.story) },
                onQuizFinished = { answers ->
                    viewModel.submitStoryAssessment(active.story, answers)
                    subScreen = ActiveSubScreen.None
                }
            )
        }

        ActiveSubScreen.None -> {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentTab == MainTab.JOURNEY,
                            onClick = { currentTab = MainTab.JOURNEY },
                            icon = { Icon(Icons.Default.Explore, contentDescription = MainTab.JOURNEY.labelAr) },
                            label = { Text(MainTab.JOURNEY.labelAr) },
                            modifier = Modifier.testTag(MainTab.JOURNEY.tag)
                        )
                        NavigationBarItem(
                            selected = currentTab == MainTab.GLOSSARY,
                            onClick = { currentTab = MainTab.GLOSSARY },
                            icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = MainTab.GLOSSARY.labelAr) },
                            label = { Text(MainTab.GLOSSARY.labelAr) },
                            modifier = Modifier.testTag(MainTab.GLOSSARY.tag)
                        )
                        NavigationBarItem(
                            selected = currentTab == MainTab.AI_STORY,
                            onClick = { currentTab = MainTab.AI_STORY },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = MainTab.AI_STORY.labelAr) },
                            label = { Text(MainTab.AI_STORY.labelAr) },
                            modifier = Modifier.testTag(MainTab.AI_STORY.tag)
                        )
                        NavigationBarItem(
                            selected = currentTab == MainTab.DASHBOARD,
                            onClick = { currentTab = MainTab.DASHBOARD },
                            icon = { Icon(Icons.Default.Analytics, contentDescription = MainTab.DASHBOARD.labelAr) },
                            label = { Text(MainTab.DASHBOARD.labelAr) },
                            modifier = Modifier.testTag(MainTab.DASHBOARD.tag)
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        MainTab.JOURNEY -> {
                            VillageJourneyScreen(
                                uiState = uiState,
                                onOpenStoryReader = { story -> subScreen = ActiveSubScreen.Reader(story) },
                                onOpenStoryQuiz = { story -> subScreen = ActiveSubScreen.Quiz(story) },
                                onAuthChanged = {
                                    viewModel.onAuthUserChanged(auth?.currentUser?.uid, auth?.currentUser?.email)
                                },
                                onSyncNow = {
                                    viewModel.syncNow()
                                }
                            )
                        }

                        MainTab.GLOSSARY -> {
                            OmaniGlossaryScreen(
                                customStories = uiState.customStories
                            )
                        }

                        MainTab.AI_STORY -> {
                            AiStoryGeneratorScreen(
                                isGenerating = uiState.isGeneratingStory,
                                errorMessage = uiState.aiErrorMessage,
                                customStories = uiState.customStories,
                                onGenerateStory = { topic, village, moral ->
                                    viewModel.generateNewAiStory(topic, village, moral) { generatedStory ->
                                        subScreen = ActiveSubScreen.Reader(generatedStory)
                                    }
                                },
                                onOpenStory = { story -> subScreen = ActiveSubScreen.Reader(story) }
                            )
                        }

                        MainTab.DASHBOARD -> {
                            DiagnosticDashboardScreen(
                                uiState = uiState,
                                onSelectProfile = { id -> viewModel.selectProfile(id) },
                                onAddProfile = { name, age, avatar -> viewModel.addChildProfile(name, age, avatar) },
                                onToggleTeacherUnlockPostTest = { viewModel.toggleTeacherUnlockPostTest() },
                                onStartPreTest = {
                                    subScreen = ActiveSubScreen.Reader(uiState.preDiagnosticStory)
                                },
                                onStartPostTest = {
                                    subScreen = ActiveSubScreen.Reader(uiState.postDiagnosticStory)
                                },
                                onSyncNow = {
                                    viewModel.syncNow()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
