package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookItem
import com.example.data.model.BookStatus
import com.example.ui.components.StudyTrackCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.StudyTrackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
    viewModel: StudyTrackViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val books by viewModel.books.collectAsState()
    val strings = LocalAppStrings.current

    var statusFilter by remember { mutableStateOf<BookStatus?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var bookToUpdatePages by remember { mutableStateOf<BookItem?>(null) }

    val filteredBooks = if (statusFilter == null) books else books.filter { it.status == statusFilter }
    val readingCount = books.count { it.status == BookStatus.READING }
    val finishedCount = books.count { it.status == BookStatus.FINISHED }

    Scaffold(
        modifier = modifier.testTag("books_screen"),
        topBar = {
            TopAppBar(
                title = { Text("${strings.booksTitle} 📖", fontWeight = FontWeight.Bold) },
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
                modifier = Modifier.testTag("add_book_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.addBook)
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
                                text = strings.myLibrary,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$readingCount ${strings.currentlyReading} • $finishedCount ${strings.finished}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = AmberAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "📚 ${books.size} ${strings.booksCountLabel}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = AmberAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Status Filter Tabs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = statusFilter == null,
                        onClick = { statusFilter = null },
                        label = { Text("${strings.all} (${books.size})") }
                    )
                    BookStatus.values().forEach { status ->
                        val count = books.count { it.status == status }
                        FilterChip(
                            selected = statusFilter == status,
                            onClick = { statusFilter = if (statusFilter == status) null else status },
                            label = { Text("${status.emoji} ${status.label} ($count)") }
                        )
                    }
                }
            }

            if (filteredBooks.isEmpty()) {
                item {
                    StudyTrackCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.noBooksFound,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredBooks, key = { it.id }) { book ->
                    BookCard(
                        book = book,
                        onUpdateProgress = { bookToUpdatePages = book },
                        onDelete = { viewModel.deleteBook(book) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddBookDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, author, totalPages, pagesRead, rating, status ->
                viewModel.addBook(title, author, totalPages, pagesRead, rating, status)
                showAddDialog = false
            }
        )
    }

    bookToUpdatePages?.let { book ->
        UpdatePagesDialog(
            book = book,
            onDismiss = { bookToUpdatePages = null },
            onConfirm = { newPages ->
                viewModel.updateBookPages(book, newPages)
                bookToUpdatePages = null
            }
        )
    }
}

@Composable
fun BookCard(
    book: BookItem,
    onUpdateProgress: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = book.progressPercent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = when (book.status) {
                            BookStatus.FINISHED -> EmeraldSuccess.copy(alpha = 0.15f)
                            BookStatus.READING -> IndigoPrimary.copy(alpha = 0.15f)
                            BookStatus.TO_READ -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${book.status.emoji} ${book.status.label}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when (book.status) {
                                BookStatus.FINISHED -> EmeraldSuccess
                                BookStatus.READING -> IndigoPrimary
                                BookStatus.TO_READ -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "by ${book.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rating Stars
            if (book.rating > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= book.rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Progress bar and page stats
            val strings = LocalAppStrings.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${book.pagesRead} ${strings.ofPages} ${book.totalPages} ${strings.pages} ($progress%)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = onUpdateProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = strings.logPages,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (book.status == BookStatus.FINISHED) EmeraldSuccess else IndigoPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun UpdatePagesDialog(
    book: BookItem,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    var pagesText by remember { mutableStateOf(book.pagesRead.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.updatePagesRead) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${book.title} (${strings.totalPagesLabel}: ${book.totalPages} ${strings.pages})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pagesText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) pagesText = it },
                    label = { Text(strings.currentPage) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cur = pagesText.toIntOrNull() ?: 0
                            pagesText = (cur + 10).coerceAtMost(book.totalPages).toString()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+10 ${strings.pages.take(3)}")
                    }
                    OutlinedButton(
                        onClick = {
                            val cur = pagesText.toIntOrNull() ?: 0
                            pagesText = (cur + 25).coerceAtMost(book.totalPages).toString()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+25 ${strings.pages.take(3)}")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = pagesText.toIntOrNull() ?: book.pagesRead
                    onConfirm(p)
                }
            ) {
                Text("${strings.save} (+15 XP)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun AddBookDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, author: String, totalPages: Int, pagesRead: Int, rating: Int, status: BookStatus) -> Unit
) {
    val strings = LocalAppStrings.current
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var totalPagesText by remember { mutableStateOf("300") }
    var pagesReadText by remember { mutableStateOf("0") }
    var rating by remember { mutableStateOf(5) }
    var selectedStatus by remember { mutableStateOf(BookStatus.READING) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addBookTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.bookTitleLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(strings.authorLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = totalPagesText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) totalPagesText = it },
                        label = { Text(strings.totalPagesLabel) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pagesReadText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) pagesReadText = it },
                        label = { Text(strings.pagesReadLabel) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("${strings.category}:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BookStatus.values().forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = st },
                            label = { Text("${st.emoji} ${st.label}") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tot = totalPagesText.toIntOrNull() ?: 100
                    val cur = pagesReadText.toIntOrNull() ?: 0
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), author.trim(), tot, cur, rating, selectedStatus)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(strings.addBook)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
