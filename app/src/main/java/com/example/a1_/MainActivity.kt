package com.example.a1_

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.a1_.data.AppDatabase
import com.example.a1_.data.PackManager
import com.example.a1_.repository.QuizRepository
import com.example.a1_.ui.*
import com.example.a1_.ui.theme.TestTrainerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = QuizRepository(applicationContext, database.progressDao())
        val packManager = PackManager(applicationContext)
        val viewModelFactory = QuizViewModel.Factory(repository, packManager)

        setContent {
            TestTrainerTheme {
                val navController = rememberNavController()
                val viewModel: QuizViewModel = viewModel(factory = viewModelFactory)
                
                val state by viewModel.quizState.collectAsState()
                val stats by viewModel.dashboardStats.collectAsState()
                val sections by viewModel.sections.collectAsState()
                val packs by viewModel.availablePacks.collectAsState()

                val pickJsonLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let {
                        val fileName = getFileName(it) ?: "unknown_pack.json"
                        viewModel.importAndSelectPack(it, fileName)
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "home",
                    enterTransition = { androidx.compose.animation.EnterTransition.None },
                    exitTransition = { androidx.compose.animation.ExitTransition.None },
                    popEnterTransition = { androidx.compose.animation.EnterTransition.None },
                    popExitTransition = { androidx.compose.animation.ExitTransition.None }
                ) {
                    composable("home") {
                        MainDashboard(
                            state = state,
                            stats = stats,
                            packs = packs,
                            onModeSelect = { mode ->
                                if (state.activePackId != null) {
                                    viewModel.startQuiz(mode)
                                    navController.navigate("quiz")
                                }
                            },
                            onTopicClick = {
                                if (state.activePackId != null) {
                                    navController.navigate("topics")
                                }
                            },
                            onPackSelect = { packId -> viewModel.selectPack(packId) },
                            onDeletePack = { packId -> viewModel.deletePack(packId) },
                            onImportPack = { pickJsonLauncher.launch("application/json") },
                            onUpdateSettings = { viewModel.updateExamSettings(it) }
                        )
                    }
                    composable("topics") {
                        TopicSelectionScreen(
                            topics = sections,
                            onTopicSelected = { topic ->
                                viewModel.startQuiz(QuizMode.TOPIC, topic)
                                navController.navigate("quiz")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("quiz") {
                        QuizTestingScreen(
                            state = state,
                            onAnswer = { qId, ans -> viewModel.onAnswerSelected(qId, ans) },
                            onSelectIndex = { viewModel.selectQuestion(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onResetSession = { viewModel.resetSessionAnswers() },
                            onClearGlobal = {
                                if (state.mode == QuizMode.ERRORS) viewModel.clearErrorList()
                                if (state.mode == QuizMode.FAVORITES) viewModel.clearGlobalFavorites()
                            },
                            onNextTopic = { viewModel.nextTopic() },
                            onBack = {
                                if (state.mode == QuizMode.EXAM) {
                                    viewModel.resetSessionAnswers()
                                }
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) result = cursor.getString(nameIndex)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) result = result?.substring(cut + 1)
        }
        return result
    }
}
