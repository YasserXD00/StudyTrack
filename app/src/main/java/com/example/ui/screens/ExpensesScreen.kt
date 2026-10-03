package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Currency
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseItem
import com.example.ui.components.StudyTrackCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.RoseDanger
import com.example.ui.viewmodel.StudyTrackViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: StudyTrackViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val expenses by viewModel.expenses.collectAsState()
    val profile by viewModel.profile.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    val cal = Calendar.getInstance()

    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis

    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    val weekStart = cal.timeInMillis

    cal.set(Calendar.DAY_OF_MONTH, 1)
    val monthStart = cal.timeInMillis

    val spentToday = expenses.filter { it.dateTimestamp >= todayStart }.sumOf { it.amount }
    val spentWeek = expenses.filter { it.dateTimestamp >= weekStart }.sumOf { it.amount }
    val spentMonth = expenses.filter { it.dateTimestamp >= monthStart }.sumOf { it.amount }

    val monthlyBudget = profile.monthlyBudget
    val currency = Currency.fromCode(profile.currencyCode)
    val curSym = currency.symbol
    val remainingBudget = (monthlyBudget - spentMonth).coerceAtLeast(0.0)
    val budgetUsedRatio = if (monthlyBudget > 0) (spentMonth / monthlyBudget).toFloat().coerceIn(0f, 1f) else 0f

    val categoryBreakdown = expenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }

    Scaffold(
        modifier = modifier.testTag("expenses_screen"),
        topBar = {
            TopAppBar(
                title = { Text("${strings.expensesTitle} 💰", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = strings.close)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.expense)
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
            // Budget Summary Card
            item {
                StudyTrackCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.monthlyBudget,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$curSym${String.format(Locale.US, "%.2f", spentMonth)} / $curSym${String.format(Locale.US, "%.2f", monthlyBudget)}",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            color = if (remainingBudget > 0) EmeraldSuccess.copy(alpha = 0.15f) else RoseDanger.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${strings.remainingBudget}: $curSym${String.format(Locale.US, "%.2f", remainingBudget)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (remainingBudget > 0) EmeraldSuccess else RoseDanger,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { budgetUsedRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (budgetUsedRatio >= 0.9f) RoseDanger else IndigoLight,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Stat Cards for Today & Week
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        icon = "📅",
                        label = strings.today,
                        value = "$curSym${String.format(Locale.US, "%.2f", spentToday)}",
                        accentColor = IndigoLight,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = "📊",
                        label = strings.thisWeek,
                        value = "$curSym${String.format(Locale.US, "%.2f", spentWeek)}",
                        accentColor = CyanAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Spending by Category
            item {
                StudyTrackCard {
                    Text(
                        text = "Spending by Category",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ExpenseCategory.values().forEach { cat ->
                        val catTotal = categoryBreakdown[cat] ?: 0.0
                        val catRatio = if (spentMonth > 0) (catTotal / spentMonth).toFloat().coerceIn(0f, 1f) else 0f

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
                                    text = "${cat.emoji} ${cat.label}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$curSym${String.format(Locale.US, "%.2f", catTotal)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { catRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }

            // Recent Transactions
            item {
                SectionHeader(title = "Recent Transactions (${expenses.size})")
            }

            if (expenses.isEmpty()) {
                item {
                    StudyTrackCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No expenses logged yet. Tap '+' to record your purchases!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(expenses, key = { it.id }) { expense ->
                    ExpenseRow(
                        expense = expense,
                        curSym = curSym,
                        onDelete = { viewModel.deleteExpense(expense) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(
            curSym = curSym,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, amount, category, notes ->
                viewModel.addExpense(title, amount, category, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ExpenseRow(
    expense: ExpenseItem,
    curSym: String,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.US) }
    val dateDisplay = remember(expense.dateTimestamp) { dateFormat.format(Date(expense.dateTimestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = expense.category.emoji, fontSize = 26.sp)
                Column {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (expense.notes.isNotBlank()) {
                        Text(
                            text = expense.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = dateDisplay,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "-$curSym${String.format(Locale.US, "%.2f", expense.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = RoseDanger
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AddExpenseDialog(
    curSym: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, category: ExpenseCategory, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.FOOD) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Expense Title *") },
                    placeholder = { Text("e.g., Campus Lunch") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($curSym) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExpenseCategory.values().forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.emoji) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0) {
                        onConfirm(title.trim(), amt, selectedCategory, notes.trim())
                    }
                },
                enabled = title.isNotBlank() && amountText.isNotBlank()
            ) {
                Text("Add Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
