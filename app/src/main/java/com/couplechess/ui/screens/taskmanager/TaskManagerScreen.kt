package com.couplechess.ui.screens.taskmanager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.couplechess.data.model.Task
import com.couplechess.data.model.TaskLevel
import com.couplechess.data.repository.TaskRepository
import com.couplechess.ui.theme.BackgroundCard
import com.couplechess.ui.theme.BackgroundElevated
import com.couplechess.ui.theme.BackgroundPrimary
import com.couplechess.ui.theme.ButtonDanger
import com.couplechess.ui.theme.ButtonPrimary
import com.couplechess.ui.theme.ButtonPrimaryText
import com.couplechess.ui.theme.DividerColor
import com.couplechess.ui.theme.Gold
import com.couplechess.ui.theme.LevelColors
import com.couplechess.ui.theme.TextHint
import com.couplechess.ui.theme.TextPrimary
import com.couplechess.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskManagerScreen(
    taskRepository: TaskRepository,
    onNavigateBack: () -> Unit
) {
    val viewModel: TaskManagerViewModel = viewModel(
        factory = TaskManagerViewModel.Factory(taskRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "任务管理",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Gold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = Gold,
                contentColor = ButtonPrimaryText,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加任务"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onClear = { viewModel.updateSearchQuery("") },
                onSearch = { focusManager.clearFocus() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Level filter chips
            LevelFilterRow(
                selectedLevel = uiState.selectedLevel,
                onLevelSelected = { viewModel.selectLevel(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Task count summary
            TaskCountSummary(
                totalCount = uiState.tasks.size,
                selectedLevel = uiState.selectedLevel
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Task list or loading/empty state
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                uiState.tasks.isEmpty() -> {
                    EmptyState(
                        hasFilters = uiState.selectedLevel != null || uiState.searchQuery.isNotBlank(),
                        onClearFilters = { viewModel.clearFilters() }
                    )
                }
                else -> {
                    TaskList(
                        tasks = uiState.tasks,
                        onEditTask = { viewModel.showEditDialog(it) },
                        onDeleteTask = { viewModel.showDeleteConfirmation(it) }
                    )
                }
            }
        }

        // Dialogs and bottom sheets
        when (val dialogState = uiState.dialogState) {
            is TaskDialogState.Add -> {
                TaskEditBottomSheet(
                    title = "添加任务",
                    form = dialogState.form,
                    onDescriptionChange = { viewModel.updateFormDescription(it) },
                    onLevelChange = { viewModel.updateFormLevel(it) },
                    onSave = { viewModel.saveTask() },
                    onDismiss = { viewModel.dismissDialog() }
                )
            }
            is TaskDialogState.Edit -> {
                TaskEditBottomSheet(
                    title = "编辑任务",
                    form = dialogState.form,
                    onDescriptionChange = { viewModel.updateFormDescription(it) },
                    onLevelChange = { viewModel.updateFormLevel(it) },
                    onSave = { viewModel.saveTask() },
                    onDismiss = { viewModel.dismissDialog() }
                )
            }
            is TaskDialogState.ConfirmDelete -> {
                DeleteConfirmDialog(
                    task = dialogState.task,
                    onConfirm = { viewModel.deleteTask(dialogState.task) },
                    onDismiss = { viewModel.dismissDialog() }
                )
            }
            TaskDialogState.Hidden -> {}
        }
    }
}

// ===== Search Bar =====

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        placeholder = {
            Text("搜索任务...", color = TextHint)
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TextHint
            )
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "清除",
                        tint = TextSecondary
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = Gold,
            focusedBorderColor = Gold,
            unfocusedBorderColor = DividerColor,
            focusedContainerColor = BackgroundCard,
            unfocusedContainerColor = BackgroundCard
        )
    )
}

// ===== Level Filter Row =====

@Composable
private fun LevelFilterRow(
    selectedLevel: TaskLevel?,
    onLevelSelected: (TaskLevel?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        // "All" chip
        item {
            LevelFilterChip(
                label = "全部",
                isSelected = selectedLevel == null,
                color = Gold,
                onClick = { onLevelSelected(null) }
            )
        }

        // Level chips
        items(TaskLevel.entries) { level ->
            LevelFilterChip(
                label = "${level.name} ${level.displayName}",
                isSelected = selectedLevel == level,
                color = LevelColors.getOrElse(level.levelInt - 1) { Gold },
                onClick = { onLevelSelected(level) }
            )
        }
    }
}

@Composable
private fun LevelFilterChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) color else BackgroundCard,
        label = "chipBgColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else TextSecondary,
        label = "chipTextColor"
    )

    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = BackgroundCard,
            selectedContainerColor = backgroundColor
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = if (isSelected) color else DividerColor,
            selectedBorderColor = color,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp
        )
    )
}

// ===== Task Count Summary =====

@Composable
private fun TaskCountSummary(
    totalCount: Int,
    selectedLevel: TaskLevel?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (selectedLevel != null) {
                "${selectedLevel.displayName} 任务"
            } else {
                "全部任务"
            },
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "共 $totalCount 条",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

// ===== Task List =====

@Composable
private fun TaskList(
    tasks: List<Task>,
    onEditTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tasks, key = { it.id }) { task ->
            TaskCard(
                task = task,
                onEdit = { onEditTask(task) },
                onDelete = { onDeleteTask(task) }
            )
        }
        // Bottom padding for FAB
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val levelColor = LevelColors.getOrElse(task.level.levelInt - 1) { Gold }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Level indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(levelColor.copy(alpha = 0.2f))
                        .border(1.dp, levelColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = task.level.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = levelColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (task.isCustom) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "自定义",
                        tint = Gold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Task description
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.level.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = levelColor
                )
            }

            // Action buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = ButtonDanger,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ===== Loading State =====

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Gold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "加载任务中...",
                color = TextSecondary
            )
        }
    }
}

// ===== Empty State =====

@Composable
private fun EmptyState(
    hasFilters: Boolean,
    onClearFilters: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = if (hasFilters) "未找到匹配的任务" else "暂无任务",
                style = MaterialTheme.typography.titleLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (hasFilters) {
                    "尝试调整筛选条件或搜索关键词"
                } else {
                    "点击右下角按钮添加新任务"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextHint,
                textAlign = TextAlign.Center
            )
            if (hasFilters) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onClearFilters,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Gold
                    )
                ) {
                    Text("清除筛选")
                }
            }
        }
    }
}

// ===== Task Edit Bottom Sheet =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditBottomSheet(
    title: String,
    form: TaskFormState,
    onDescriptionChange: (String) -> Unit,
    onLevelChange: (TaskLevel) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BackgroundElevated,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DividerColor)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Level selector
            Text(
                text = "任务等级",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LevelSelectorRow(
                selectedLevel = form.level,
                onLevelSelected = onLevelChange
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Description input
            Text(
                text = "任务描述",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = form.description,
                onValueChange = onDescriptionChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = {
                    Text("请输入任务内容...", color = TextHint)
                },
                isError = form.errorMessage != null,
                supportingText = form.errorMessage?.let { error ->
                    { Text(text = error, color = ButtonDanger) }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Gold,
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = DividerColor,
                    errorBorderColor = ButtonDanger,
                    focusedContainerColor = BackgroundCard,
                    unfocusedContainerColor = BackgroundCard
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    )
                ) {
                    Text("取消")
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    enabled = form.isValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = ButtonPrimaryText,
                        disabledContainerColor = Gold.copy(alpha = 0.3f),
                        disabledContentColor = ButtonPrimaryText.copy(alpha = 0.5f)
                    )
                ) {
                    Text("保存", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LevelSelectorRow(
    selectedLevel: TaskLevel,
    onLevelSelected: (TaskLevel) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TaskLevel.entries.forEach { level ->
            val levelColor = LevelColors.getOrElse(level.levelInt - 1) { Gold }
            val isSelected = level == selectedLevel
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) levelColor else BackgroundCard,
                label = "levelBgColor"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = levelColor,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onLevelSelected(level) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = level.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) Color.White else levelColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = level.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextHint,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ===== Delete Confirm Dialog =====

@Composable
private fun DeleteConfirmDialog(
    task: Task,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundElevated,
        title = {
            Text(
                text = "删除任务",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "确定要删除这条任务吗？",
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "「${task.description}」",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonDanger,
                    contentColor = Color.White
                )
            ) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}
