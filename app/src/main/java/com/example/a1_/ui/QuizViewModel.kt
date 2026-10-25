package com.example.a1_.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.a1_.data.*
import com.example.a1_.repository.QuizRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class QuizMode {
    EXAM, MARATHON, TOPIC, ERRORS, FAVORITES
}

data class QuizState(
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val userAnswers: Map<String, Int> = emptyMap(),
    val favoriteIds: Set<String> = emptySet(),
    val errorIds: Set<String> = emptySet(),
    val timeLeftSeconds: Int = 1800,
    val isExamFinished: Boolean = false,
    val isAllTopicsFinished: Boolean = false,
    val selectedTopic: String? = null,
    val mode: QuizMode = QuizMode.MARATHON,
    val activePackName: String? = null,
    val activePackId: String? = null,
    val examSettings: ExamSettings = ExamSettings()
)

class QuizViewModel(
    private val repository: QuizRepository,
    private val packManager: PackManager
) : ViewModel() {

    private val _allQuestions = MutableStateFlow<List<Question>>(emptyList())
    private val _quizState = MutableStateFlow(QuizState())
    val quizState = _quizState.asStateFlow()

    val availablePacks = MutableStateFlow<List<PackInfo>>(emptyList())

    private val _currentPackProgress = MutableStateFlow<List<ModeProgress>>(emptyList())
    private val _currentPackFlags = MutableStateFlow<List<QuestionFlags>>(emptyList())

    private var progressJob: Job? = null
    private var timerJob: Job? = null

    init {
        refreshPacks()
        viewModelScope.launch {
            packManager.settings.collect { settings ->
                _quizState.value = _quizState.value.copy(examSettings = settings)
            }
        }
        viewModelScope.launch {
            packManager.activePackId.collect { packId ->
                if (packId != null) {
                    loadPack(packId)
                } else {
                    // Try to load default pack if nothing selected
                    val available = packManager.getAvailablePacks()
                    if (available.isNotEmpty()) {
                        selectPack(available.first().id)
                    } else {
                        _allQuestions.value = emptyList()
                        _currentPackProgress.value = emptyList()
                        _currentPackFlags.value = emptyList()
                        _quizState.value = QuizState()
                    }
                }
            }
        }
    }

    private fun loadPack(packId: String) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            val file = packManager.getPackFile(packId)
            val questions = repository.loadQuestions(file)
            _allQuestions.value = questions
            
            combine(
                repository.getAllModeProgress(packId),
                repository.getAllFlags(packId)
            ) { progress, flags ->
                _currentPackProgress.value = progress
                _currentPackFlags.value = flags
                Pair(progress, flags)
            }.collect { (progress, flags) ->
                _quizState.update { currentState ->
                    val topicId = currentState.selectedTopic ?: ""
                    val relevantAnswers = progress.filter { 
                        it.mode == currentState.mode.name && (currentState.mode != QuizMode.TOPIC || it.topicId == topicId)
                    }.associate { it.questionId to it.selectedOption }

                    val newAnswers = if (currentState.mode == QuizMode.EXAM) {
                        currentState.userAnswers
                    } else {
                        relevantAnswers
                    }

                    currentState.copy(
                        activePackId = packId,
                        activePackName = packId.removeSuffix(".json"),
                        userAnswers = newAnswers,
                        favoriteIds = flags.filter { it.isFavorite }.map { it.questionId }.toSet(),
                        errorIds = flags.filter { it.isError }.map { it.questionId }.toSet()
                    )
                }
            }
        }
    }

    fun updateExamSettings(settings: ExamSettings) {
        viewModelScope.launch {
            packManager.updateSettings(settings)
        }
    }

    fun refreshPacks() {
        availablePacks.value = packManager.getAvailablePacks()
    }

    fun importAndSelectPack(uri: Uri, fileName: String) {
        viewModelScope.launch {
            val packId = packManager.importPack(uri, fileName)
            if (packId != null) {
                refreshPacks()
                packManager.setActivePack(packId)
            }
        }
    }

    fun selectPack(packId: String) {
        viewModelScope.launch {
            packManager.setActivePack(packId)
        }
    }

    fun deletePack(packId: String) {
        viewModelScope.launch {
            // 1. Delete progress from DB
            repository.deletePackData(packId)
            // 2. Delete file
            packManager.deletePack(packId)
            // 3. Refresh list
            refreshPacks()
            // 4. If active pack was deleted, switch to default
            if (_quizState.value.activePackId == packId) {
                val nextPack = availablePacks.value.firstOrNull()?.id
                if (nextPack != null) {
                    selectPack(nextPack)
                } else {
                    packManager.setActivePack(null)
                    _allQuestions.value = emptyList()
                    _quizState.value = QuizState()
                }
            }
        }
    }

    val dashboardStats = combine(_allQuestions, _currentPackFlags) { questions, flags ->
        val total = questions.size
        val errors = flags.count { it.isError }
        val favorites = flags.count { it.isFavorite }
        mapOf("total" to total, "errors" to errors, "favorites" to favorites)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val sections = combine(_allQuestions, _currentPackProgress) { questions, progress ->
        questions.groupBy { it.section }.toSortedMap().mapValues { entry ->
            val sectionName = entry.key
            val sectionQuestions = entry.value.map { it.id }
            val completed = progress.count { 
                it.mode == QuizMode.TOPIC.name && it.topicId == sectionName && it.questionId in sectionQuestions 
            }
            val total = sectionQuestions.size
            "$completed/$total"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun startQuiz(mode: QuizMode, topic: String? = null) {
        val packId = _quizState.value.activePackId ?: return
        
        timerJob?.cancel()

        _quizState.update { currentState ->
            val settings = currentState.examSettings
            
            val filtered = when (mode) {
                QuizMode.EXAM -> _allQuestions.value.shuffled().take(settings.questionCount)
                QuizMode.MARATHON -> _allQuestions.value
                QuizMode.TOPIC -> _allQuestions.value.filter { it.section == topic }
                QuizMode.ERRORS -> {
                    _allQuestions.value.filter { it.id in currentState.errorIds }
                }
                QuizMode.FAVORITES -> {
                    _allQuestions.value.filter { it.id in currentState.favoriteIds }
                }
            }

            // Pre-load user answers from the progress state for the selected mode
            val topicId = topic ?: ""
            val relevantAnswers = _currentPackProgress.value.filter { 
                it.mode == mode.name && (mode != QuizMode.TOPIC || it.topicId == topicId)
            }.associate { it.questionId to it.selectedOption }

            val targetIndex = filtered.indexOfFirst { it.id !in relevantAnswers }.let { if (it == -1) 0 else it }

            currentState.copy(
                questions = filtered,
                currentIndex = targetIndex,
                userAnswers = if (mode == QuizMode.EXAM) emptyMap() else relevantAnswers,
                mode = mode,
                selectedTopic = topic,
                timeLeftSeconds = if (mode == QuizMode.EXAM) 1800 else 0,
                isExamFinished = false,
                isAllTopicsFinished = false
            )
        }

        if (mode == QuizMode.EXAM) {
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (_quizState.value.timeLeftSeconds > 0 && !_quizState.value.isExamFinished) {
                delay(1000)
                _quizState.update { it.copy(timeLeftSeconds = it.timeLeftSeconds - 1) }
            }
            if (_quizState.value.timeLeftSeconds == 0) {
                _quizState.update { it.copy(isExamFinished = true) }
            }
        }
    }

    fun selectQuestion(index: Int) {
        _quizState.update { it.copy(currentIndex = index) }
    }

    fun onAnswerSelected(questionId: String, optionIndex: Int) {
        val currentState = _quizState.value
        val packId = currentState.activePackId ?: return
        val question = currentState.questions.find { it.id == questionId } ?: return
        if (currentState.userAnswers.containsKey(questionId) || currentState.isExamFinished) return

        val isCorrect = optionIndex == question.answer
        
        _quizState.update { state ->
            if (state.userAnswers.containsKey(questionId) || state.isExamFinished) return@update state

            // Handle Exam penalty logic
            var updatedQuestions = state.questions
            if (state.mode == QuizMode.EXAM && !isCorrect && state.examSettings.penaltyEnabled) {
                val penalty = _allQuestions.value.filter { q -> 
                    q.id !in updatedQuestions.map { it.id } 
                }.shuffled().take(2)
                updatedQuestions = updatedQuestions + penalty
            }

            var isFinished = state.isExamFinished
            if (state.mode == QuizMode.EXAM) {
                val currentAnswersWithNew = state.userAnswers + (questionId to optionIndex)
                val errorCount = updatedQuestions.count { q ->
                    val ans = currentAnswersWithNew[q.id]
                    ans != null && ans != q.answer
                }
                val totalAnswered = currentAnswersWithNew.size
                
                if (errorCount > state.examSettings.maxErrors || totalAnswered == updatedQuestions.size) {
                    isFinished = true
                }
            }

            state.copy(
                questions = updatedQuestions,
                userAnswers = state.userAnswers + (questionId to optionIndex),
                isExamFinished = isFinished
            )
        }

        viewModelScope.launch {
            if (currentState.mode != QuizMode.EXAM) {
                repository.saveModeProgress(
                    ModeProgress(
                        packId = packId,
                        mode = currentState.mode.name,
                        topicId = currentState.selectedTopic ?: "",
                        questionId = questionId,
                        selectedOption = optionIndex,
                        isCorrect = isCorrect
                    )
                )
            }
            
            repository.updateError(packId, questionId, !isCorrect)
        }
    }

    fun toggleFavorite(questionId: String) {
        val packId = _quizState.value.activePackId ?: return
        val isFavorite = _quizState.value.favoriteIds.contains(questionId)
        viewModelScope.launch {
            repository.updateFavorite(packId, questionId, !isFavorite)
        }
    }

    fun resetSessionAnswers() {
        val state = _quizState.value
        val packId = state.activePackId ?: return
        timerJob?.cancel()
        viewModelScope.launch {
            // Local immediate reset of answers
            _quizState.update { it.copy(userAnswers = emptyMap(), isExamFinished = false, currentIndex = 0) }
            
            // Persistent reset - clear progress for current mode in DB.
            if (state.mode != QuizMode.EXAM) {
                repository.clearModeProgress(packId, state.mode.name, state.selectedTopic ?: "")
            }
        }
    }

    fun clearErrorList() {
        val packId = _quizState.value.activePackId ?: return
        viewModelScope.launch {
            repository.clearAllErrors(packId)
            repository.clearModeProgress(packId, QuizMode.ERRORS.name, "")
            _quizState.update { it.copy(errorIds = emptySet(), questions = emptyList(), userAnswers = emptyMap()) }
        }
    }

    fun clearGlobalFavorites() {
        val packId = _quizState.value.activePackId ?: return
        viewModelScope.launch {
            repository.clearAllFavorites(packId)
            repository.clearModeProgress(packId, QuizMode.FAVORITES.name, "")
            _quizState.update { it.copy(favoriteIds = emptySet(), questions = emptyList(), userAnswers = emptyMap()) }
        }
    }

    fun nextTopic() {
        val currentTopic = _quizState.value.selectedTopic ?: return
        val allSections = _allQuestions.value.map { it.section }.distinct().sorted()
        val currentIndex = allSections.indexOf(currentTopic)
        if (currentIndex != -1) {
            if (currentIndex < allSections.size - 1) {
                startQuiz(QuizMode.TOPIC, allSections[currentIndex + 1])
            } else {
                _quizState.update { it.copy(isAllTopicsFinished = true) }
            }
        }
    }

    class Factory(private val repository: QuizRepository, private val packManager: PackManager) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return QuizViewModel(repository, packManager) as T
        }
    }
}
