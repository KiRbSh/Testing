package com.example.a1_.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed as lazyColumnItemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as lazyGridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a1_.data.ExamSettings
import com.example.a1_.data.PackInfo
import com.example.a1_.data.Question
import com.example.a1_.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    state: QuizState,
    stats: Map<String, Int>,
    packs: List<PackInfo>,
    onModeSelect: (QuizMode) -> Unit,
    onTopicClick: () -> Unit,
    onPackSelect: (String) -> Unit,
    onDeletePack: (String) -> Unit,
    onImportPack: () -> Unit,
    onUpdateSettings: (ExamSettings) -> Unit
) {
    var showPackDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.activePackName ?: "Выбрать Пак",
                        color = Accent1C,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.clickable { showPackDialog = true }
                    )
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки", tint = Accent1C)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceColor)
            )
        },
        containerColor = BgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val isPackSelected = state.activePackId != null
            val menuItems = remember(state.examSettings, stats) {
                listOf(
                    Triple("Экзамен", "${state.examSettings.questionCount} случайных билетов, 30 минут. Доп. вопросы: ${if(state.examSettings.penaltyEnabled) "Вкл" else "Выкл"}", "${state.examSettings.questionCount} вопросов"),
                    Triple("Марафон", "Сплошное тестирование по всей базе вопросов от начала до конца.", "${stats["total"] ?: 0} вопросов"),
                    Triple("По темам", "Прохождение с чётким разделением по тематическим разделам платформы.", "Выбрать тему →"),
                    Triple("Работа над ошибками", "Вопросы, на которых вы ответили неверно. Повторяйте до полного запоминания.", "${stats["errors"] ?: 0} ошибок"),
                    Triple("Избранное", "Вопросы, которые вы отметили флажком для персонального повторения.", "${stats["favorites"] ?: 0} отмечено")
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                lazyColumnItemsIndexed(menuItems) { index, item ->
                    MenuCard(
                        title = item.first,
                        description = item.second,
                        badge = item.third,
                        enabled = isPackSelected,
                        onClick = {
                            if (isPackSelected) {
                                when (index) {
                                    0 -> onModeSelect(QuizMode.EXAM)
                                    1 -> onModeSelect(QuizMode.MARATHON)
                                    2 -> onTopicClick()
                                    3 -> onModeSelect(QuizMode.ERRORS)
                                    4 -> onModeSelect(QuizMode.FAVORITES)
                                }
                            } else {
                                showPackDialog = true
                            }
                        }
                    )
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            settings = state.examSettings,
            onSave = onUpdateSettings,
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showPackDialog) {
        PackSelectionDialog(
            packs = packs,
            activePackId = state.activePackId,
            onPackSelect = {
                onPackSelect(it)
                showPackDialog = false
            },
            onImportClick = onImportPack,
            onDeletePack = onDeletePack,
            onDismiss = { showPackDialog = false }
        )
    }
}

@Composable
fun SettingsDialog(
    settings: ExamSettings,
    onSave: (ExamSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var questCount by remember { mutableStateOf(settings.questionCount.toString()) }
    var maxErrors by remember { mutableStateOf(settings.maxErrors.toString()) }
    var penalty by remember { mutableStateOf(settings.penaltyEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        title = { Text("Настройки экзамена", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = questCount,
                    onValueChange = { if (it.all { c -> c.isDigit() }) questCount = it },
                    label = { Text("Кол-во вопросов", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Accent1C,
                        unfocusedBorderColor = BorderColor
                    )
                )
                OutlinedTextField(
                    value = maxErrors,
                    onValueChange = { if (it.all { c -> c.isDigit() }) maxErrors = it },
                    label = { Text("Допустимо ошибок", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Accent1C,
                        unfocusedBorderColor = BorderColor
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = penalty,
                        onCheckedChange = { penalty = it },
                        colors = CheckboxDefaults.colors(checkedColor = Accent1C)
                    )
                    Text("Штрафные вопросы (+2 за ошибку)", color = TextPrimary, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(ExamSettings(
                        questionCount = questCount.toIntOrNull() ?: 14,
                        maxErrors = maxErrors.toIntOrNull() ?: 2,
                        penaltyEnabled = penalty
                    ))
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Accent1C)
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = TextMuted) }
        }
    )
}

@Composable
fun PackSelectionDialog(
    packs: List<PackInfo>,
    activePackId: String?,
    onPackSelect: (String) -> Unit,
    onImportClick: () -> Unit,
    onDeletePack: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        title = { Text("Выбор пака вопросов", color = Color.White) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (packs.isEmpty()) {
                    Text("Паки не найдены. Загрузите JSON файл.", color = TextMuted, modifier = Modifier.padding(bottom = 16.dp))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(packs) { pack ->
                            val isSelected = pack.id == activePackId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                color = if (isSelected) Accent1C.copy(alpha = 0.1f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isSelected) Accent1C else BorderColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPackSelect(pack.id) }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        pack.name,
                                        color = if (isSelected) Accent1C else TextPrimary,
                                        modifier = Modifier.weight(1f),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (pack.isExternal) {
                                        IconButton(
                                            onClick = { onDeletePack(pack.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Удалить пак",
                                                tint = TextMuted,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onImportClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceHover),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Accent1C)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Загрузить пак (JSON)", color = TextPrimary)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть", color = Accent1C) }
        }
    )
}

@Composable
fun MenuCard(title: String, description: String, badge: String, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = if (enabled) SurfaceColor else SurfaceColor.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (enabled) BorderColor else BorderColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                title, 
                color = if (enabled) Color.White else TextMuted, 
                fontSize = 20.sp, 
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                description, 
                color = if (enabled) TextMuted else TextMuted.copy(alpha = 0.5f), 
                fontSize = 14.sp, 
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                color = (if (enabled) Accent1C else TextMuted).copy(alpha = 0.12f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    badge,
                    color = if (enabled) Accent1C else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicSelectionScreen(
    topics: Map<String, String>,
    onTopicSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Выберите тему", color = Accent1C) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Accent1C)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceColor)
            )
        },
        containerColor = BgColor
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(
                "Вопросы сгруппированы по официальным разделам экзамена:",
                color = TextMuted,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(topics.keys.toList()) { topic ->
                    TopicItem(topic, topics[topic] ?: "", onClick = { onTopicSelected(topic) })
                }
            }
        }
    }
}

@Composable
fun TopicItem(title: String, count: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = SurfaceColor,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(count, color = Accent1C, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizTestingScreen(
    state: QuizState,
    onAnswer: (String, Int) -> Unit,
    onSelectIndex: (Int) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onResetSession: () -> Unit,
    onClearGlobal: () -> Unit,
    onNextTopic: () -> Unit,
    onBack: () -> Unit
) {
    val currentQuestion = state.questions.getOrNull(state.currentIndex)
    val scrollState = rememberScrollState()
    val gridState = rememberLazyGridState()
    var showResetDialog by remember { mutableStateOf(false) }
    var showClearGlobalDialog by remember { mutableStateOf(false) }
    var showTopicFinishedDialog by remember { mutableStateOf(false) }
    var showExamResultDialog by remember(state.isExamFinished) { mutableStateOf(state.isExamFinished) }

    BackHandler {
        onBack()
    }

    LaunchedEffect(state.currentIndex) {
        if (state.questions.isNotEmpty() && state.currentIndex >= 0) {
            gridState.animateScrollToItem(state.currentIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (state.mode) {
                        QuizMode.EXAM -> "Экзамен: ${formatTime(state.timeLeftSeconds)}"
                        QuizMode.TOPIC -> state.selectedTopic ?: "Тема"
                        QuizMode.ERRORS -> "Работа над ошибками"
                        QuizMode.FAVORITES -> "Избранные вопросы"
                        else -> "Марафон"
                    }
                    Text(title, color = Accent1C, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Accent1C)
                    }
                },
                actions = {
                    if (currentQuestion != null) {
                        val isFavorite = state.favoriteIds.contains(currentQuestion.id)
                        IconButton(onClick = { onToggleFavorite(currentQuestion.id) }) {
                            Icon(
                                if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (isFavorite) StarYellow else TextMuted
                            )
                        }
                    }
                    
                    if (state.mode == QuizMode.ERRORS || state.mode == QuizMode.FAVORITES) {
                        IconButton(onClick = { showClearGlobalDialog = true }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Очистить список", tint = Accent1C)
                        }
                    }

                    if (state.mode != QuizMode.EXAM && state.mode != QuizMode.ERRORS && state.mode != QuizMode.FAVORITES) {
                        IconButton(onClick = { showResetDialog = true }) {
                            Icon(Icons.Default.History, contentDescription = "Сбросить ответы", tint = Accent1C)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceColor)
            )
        },
        bottomBar = {
            Surface(color = BgColor, border = BorderStroke(1.dp, BorderColor)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onSelectIndex(state.currentIndex - 1) },
                        enabled = state.currentIndex > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceHover),
                        shape = RoundedCornerShape(7.dp)
                    ) {
                        Text("← Назад", color = if (state.currentIndex > 0) TextPrimary else TextMuted)
                    }
                    Text("${state.currentIndex + 1} из ${state.questions.size}", color = TextMuted, fontSize = 14.sp)
                    
                    val isLast = state.currentIndex == state.questions.size - 1
                    Button(
                        onClick = {
                            if (isLast && state.mode == QuizMode.TOPIC) {
                                showTopicFinishedDialog = true
                            } else if (state.currentIndex < state.questions.size - 1) {
                                onSelectIndex(state.currentIndex + 1)
                            }
                        },
                        enabled = !isLast || state.mode == QuizMode.TOPIC,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isLast && state.mode == QuizMode.TOPIC) Accent1C else SurfaceHover),
                        shape = RoundedCornerShape(7.dp)
                    ) {
                        val btnText = if (isLast && state.mode == QuizMode.TOPIC) "След. раздел →" else "Вперёд →"
                        Text(btnText, color = if (!isLast || state.mode == QuizMode.TOPIC) TextPrimary else TextMuted)
                    }
                }
            }
        },
        containerColor = BgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Question Grid
            Surface(
                color = SurfaceColor,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Сетка вопросов", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        val answered = state.userAnswers.size
                        Text("$answered / ${state.questions.size}", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(38.dp),
                        state = gridState,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier.heightIn(max = 140.dp)
                    ) {
                        lazyGridItemsIndexed(state.questions) { index, q ->
                            val userAnswer = state.userAnswers[q.id]
                            val isSelected = index == state.currentIndex
                            
                            val color = when {
                                state.mode == QuizMode.EXAM && !state.isExamFinished -> {
                                    if (userAnswer != null) Color(0xFFAAB7B8) else GridDefault
                                }
                                userAnswer == q.answer -> CorrectGreen
                                userAnswer != null -> WrongRed
                                else -> GridDefault
                            }

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(color, RoundedCornerShape(6.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Accent1C else BorderColor,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onSelectIndex(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (index + 1).toString(),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (currentQuestion != null) {
                if (state.mode == QuizMode.ERRORS || state.mode == QuizMode.FAVORITES) {
                    Button(
                        onClick = { onResetSession() },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceHover),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Accent1C.copy(alpha = 0.5f))
                    ) {
                        Text("Очистить ответы", color = Accent1C, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                QuestionCardView(
                    question = currentQuestion,
                    userAnswer = state.userAnswers[currentQuestion.id],
                    onAnswer = { onAnswer(currentQuestion.id, it) },
                    showCorrectness = state.mode != QuizMode.EXAM || state.isExamFinished,
                    isExamReview = state.mode == QuizMode.EXAM && state.isExamFinished
                )
            }
        }
    }
    
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = SurfaceColor,
            title = { Text("Сбросить ответы?", color = Color.White) },
            text = { Text("Все ваши ответы в текущей сессии будут удалены. Список вопросов останется прежним.", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    onResetSession()
                    showResetDialog = false
                }) {
                    Text("Сбросить", color = WrongRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Отмена", color = TextPrimary)
                }
            }
        )
    }

    if (showClearGlobalDialog) {
        val title = if (state.mode == QuizMode.ERRORS) "Очистить весь список ошибок?" else "Очистить избранное?"
        val text = if (state.mode == QuizMode.ERRORS) "Это полностью удалит все накопленные ошибки из базы. Вы вернетесь в меню." else "Это удалит все вопросы из избранного."
        
        AlertDialog(
            onDismissRequest = { showClearGlobalDialog = false },
            containerColor = SurfaceColor,
            title = { Text(title, color = Color.White) },
            text = { Text(text, color = TextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    onClearGlobal()
                    showClearGlobalDialog = false
                    onBack()
                }) {
                    Text("Очистить всё", color = WrongRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearGlobalDialog = false }) {
                    Text("Отмена", color = TextPrimary)
                }
            }
        )
    }

    if (showTopicFinishedDialog) {
        AlertDialog(
            onDismissRequest = { showTopicFinishedDialog = false },
            containerColor = SurfaceColor,
            title = { Text("Раздел завершен", color = CorrectGreen) },
            text = { Text("Вы дошли до конца раздела. Перейти к следующему?", color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        showTopicFinishedDialog = false
                        onNextTopic()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent1C)
                ) {
                    Text("Далее", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTopicFinishedDialog = false }) {
                    Text("В меню", color = TextMuted)
                }
            }
        )
    }

    if (showExamResultDialog && state.mode == QuizMode.EXAM) {
        val errorCount = state.questions.count { q ->
            val ans = state.userAnswers[q.id]
            ans != null && ans != q.answer
        }
        val isPassed = errorCount <= state.examSettings.maxErrors
        val correctCount = state.questions.size - errorCount
        
        AlertDialog(
            onDismissRequest = { showExamResultDialog = false },
            containerColor = SurfaceColor,
            title = {
                Text(
                    if (isPassed) "Экзамен сдан!" else "Экзамен не сдан",
                    color = if (isPassed) CorrectGreen else WrongRed,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("Ваш результат: $correctCount из ${state.questions.size}", color = TextPrimary, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (isPassed) "Поздравляем! Вы успешно прошли тестирование." 
                        else "К сожалению, этого недостаточно. Допустимо ошибок: ${state.examSettings.maxErrors}.",
                        color = TextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent1C)
                ) {
                    Text("В меню", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExamResultDialog = false }) {
                    Text("Просмотр ответов", color = TextMuted)
                }
            }
        )
    }
}

@Composable
fun QuestionCardView(
    question: Question,
    userAnswer: Int?,
    onAnswer: (Int) -> Unit,
    showCorrectness: Boolean = true,
    isExamReview: Boolean = false
) {
    Surface(
        color = SurfaceColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth().wrapContentHeight()
    ) {
        Column(
            modifier = Modifier.padding(top = 12.dp, start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Surface(
                color = SurfaceHover,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    question.section,
                    color = Accent1C,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = question.question,
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                question.options.forEachIndexed { index, option ->
                    val isCorrect = index == question.answer
                    val isSelected = index == userAnswer
                    
                    val stateColor = when {
                        userAnswer == null -> if (isExamReview && isCorrect) CorrectGreen else null
                        !showCorrectness -> if (isSelected) Accent1C else null
                        isCorrect -> CorrectGreen
                        isSelected && !isCorrect -> WrongRed
                        else -> null
                    }
                    
                    val bg = when {
                        stateColor == Accent1C -> stateColor.copy(alpha = 0.1f)
                        stateColor != null -> stateColor.copy(alpha = 0.25f)
                        else -> SurfaceHover
                    }
                    val borderColor = stateColor ?: BorderColor
                    val circleBg = stateColor ?: BorderColor
                    val textColor = stateColor ?: if (userAnswer != null) TextMuted else TextPrimary

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = userAnswer == null && !isExamReview) { onAnswer(index) },
                        color = bg,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(13.dp, 16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                modifier = Modifier.size(24.dp),
                                color = circleBg,
                                shape = CircleShape
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text((index + 1).toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = option,
                                color = textColor,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                    }
                }
            }

            if ((showCorrectness && userAnswer != null || isExamReview) && !question.explanation.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    color = Accent1C.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Accent1C.copy(alpha = 0.2f))
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(IntrinsicSize.Max)
                                .background(Accent1C, RoundedCornerShape(2.dp))
                                .align(Alignment.CenterVertically)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Пояснение:", color = Accent1C, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = question.explanation ?: "",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
