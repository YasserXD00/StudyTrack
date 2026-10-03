package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EnglishSkill
import com.example.data.model.VocabularyItem
import com.example.ui.components.StudyTrackCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.StudyTrackViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnglishScreen(
    viewModel: StudyTrackViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vocabulary by viewModel.vocabulary.collectAsState()
    val strings = LocalAppStrings.current

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedSkillFilter by remember { mutableStateOf<EnglishSkill?>(null) }
    var showFlashcardPractice by remember { mutableStateOf(false) }

    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    val weekStart = cal.timeInMillis

    val wordsThisWeek = vocabulary.count { it.dateAdded >= weekStart }
    val masteredWords = vocabulary.count { it.isMastered }

    val filteredList = if (selectedSkillFilter == null) vocabulary else vocabulary.filter { it.skill == selectedSkillFilter }

    Scaffold(
        modifier = modifier.testTag("english_screen"),
        topBar = {
            TopAppBar(
                title = { Text("${strings.englishTitle} 🇬🇧", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = strings.cancel)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_vocab_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.addWord)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Stats
            item {
                StudyTrackCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.weeklyGrowth,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "+$wordsThisWeek ${strings.wordsLearnedThisWeek} • $masteredWords ${strings.masteredLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showFlashcardPractice = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.practiceLabel)
                        }
                    }
                }
            }

            // Skills Trackers Grid
            item {
                StudyTrackCard {
                    Text(
                        text = strings.coreLanguageSkills,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    EnglishSkill.values().forEach { skill ->
                        val count = vocabulary.count { it.skill == skill }
                        val masteredCount = vocabulary.count { it.skill == skill && it.isMastered }
                        val ratio = if (count > 0) (masteredCount.toFloat() / count).coerceIn(0.1f, 1f) else 0.05f

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${skill.emoji} ${skill.label}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$masteredCount/$count ${strings.masteredLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = IndigoPrimary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }

            // Skill Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedSkillFilter == null,
                        onClick = { selectedSkillFilter = null },
                        label = { Text("${strings.all} (${vocabulary.size})") }
                    )
                    EnglishSkill.values().forEach { skill ->
                        FilterChip(
                            selected = selectedSkillFilter == skill,
                            onClick = { selectedSkillFilter = if (selectedSkillFilter == skill) null else skill },
                            label = { Text(skill.emoji) }
                        )
                    }
                }
            }

            // Word List
            item {
                SectionHeader(title = "${strings.vocabularyWords} (${filteredList.size})")
            }

            if (filteredList.isEmpty()) {
                item {
                    StudyTrackCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.noVocabFound,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { item ->
                    VocabularyRow(
                        item = item,
                        onToggleMastered = { viewModel.toggleVocabularyMastered(item) },
                        onDelete = { viewModel.deleteVocabulary(item) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddVocabularyDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { word, meaning, example, skill ->
                viewModel.addVocabulary(word, meaning, example, skill)
                showAddDialog = false
            }
        )
    }

    if (showFlashcardPractice && vocabulary.isNotEmpty()) {
        FlashcardPracticeDialog(
            words = vocabulary,
            onDismiss = { showFlashcardPractice = false },
            onMastered = { viewModel.toggleVocabularyMastered(it) }
        )
    }
}

@Composable
fun VocabularyRow(
    item: VocabularyItem,
    onToggleMastered: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.word,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${item.skill.emoji} ${item.skill.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.meaning,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (item.example.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "“${item.example}”",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val strings = LocalAppStrings.current
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = item.isMastered,
                        onClick = onToggleMastered,
                        label = { Text(if (item.isMastered) strings.masteredLabel else strings.learningLabel) },
                        leadingIcon = if (item.isMastered) {
                            { Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp)) }
                        } else null
                    )

                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = strings.delete,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FlashcardPracticeDialog(
    words: List<VocabularyItem>,
    onDismiss: () -> Unit,
    onMastered: (VocabularyItem) -> Unit
) {
    val strings = LocalAppStrings.current
    var currentIndex by remember { mutableStateOf(0) }
    var showMeaning by remember { mutableStateOf(false) }

    val currentWord = words[currentIndex % words.size]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.flashcardsTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${strings.steps} ${currentIndex + 1} ${strings.ofPages} ${words.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showMeaning = !showMeaning },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentWord.word,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (showMeaning) {
                                Text(
                                    text = currentWord.meaning,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = IndigoPrimary
                                )
                                if (currentWord.example.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "“${currentWord.example}”",
                                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Text(
                                    text = "(${strings.revealMeaning})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        onMastered(currentWord)
                        currentIndex = (currentIndex + 1) % words.size
                        showMeaning = false
                    }
                ) {
                    Text("${strings.masteredLabel}! ⭐")
                }
                Button(
                    onClick = {
                        currentIndex = (currentIndex + 1) % words.size
                        showMeaning = false
                    }
                ) {
                    Text(strings.nextCard)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close)
            }
        }
    )
}

@Composable
fun AddVocabularyDialog(
    onDismiss: () -> Unit,
    onConfirm: (word: String, meaning: String, example: String, skill: EnglishSkill) -> Unit
) {
    val strings = LocalAppStrings.current
    var word by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var example by remember { mutableStateOf("") }
    var selectedSkill by remember { mutableStateOf(EnglishSkill.VOCABULARY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addWord, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = word,
                    onValueChange = { word = it },
                    label = { Text(strings.wordLabel) },
                    placeholder = { Text("e.g., Improve") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = meaning,
                    onValueChange = { meaning = it },
                    label = { Text(strings.meaningLabel) },
                    placeholder = { Text("e.g., Améliorer / To make better") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = example,
                    onValueChange = { example = it },
                    label = { Text(strings.exampleSentenceLabel) },
                    placeholder = { Text("e.g., I want to improve my English.") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("${strings.category}:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EnglishSkill.values().forEach { skill ->
                        FilterChip(
                            selected = selectedSkill == skill,
                            onClick = { selectedSkill = skill },
                            label = { Text("${skill.emoji} ${skill.label.take(4)}") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (word.isNotBlank() && meaning.isNotBlank()) {
                        onConfirm(word.trim(), meaning.trim(), example.trim(), selectedSkill)
                    }
                },
                enabled = word.isNotBlank() && meaning.isNotBlank()
            ) {
                Text("${strings.addWord} (+10 XP)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
