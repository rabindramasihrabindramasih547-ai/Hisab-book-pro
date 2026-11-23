package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.entity.WorkerEntity
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.GoldBright
import com.example.ui.theme.LocalHisabColors
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyPrimary
import com.example.util.IndiaFormatters
import java.io.File
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HisabNavyTopBar(
    title: String,
    subtitle: String? = null,
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null,
    reminderCount: Int = 0,
    onRemindersClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onLanguageToggle: (() -> Unit)? = null
) {
    val strings = LocalStrings.current
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = NavyDeep,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        navigationIcon = {
            if (showBack && onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.close
                    )
                }
            }
        },
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldBright,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            if (onLanguageToggle != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = NavyPrimary,
                    border = BorderStroke(1.dp, GoldBright.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onLanguageToggle)
                        .testTag("language_toggle_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = strings.languageLabel,
                            tint = GoldBright,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (strings.language == AppLanguage.HINDI) "हिं / EN" else "EN / हिं",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (onSearchClick != null) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.testTag("top_search_button")
                ) {
                    Icon(Icons.Default.Search, contentDescription = strings.search)
                }
            }
            if (onRemindersClick != null) {
                IconButton(
                    onClick = onRemindersClick,
                    modifier = Modifier.testTag("top_reminders_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (reminderCount > 0) {
                                Badge(containerColor = GoldBright, contentColor = NavyDeep) {
                                    Text(reminderCount.toString())
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = strings.remindersTitle)
                    }
                }
            }
            if (onSettingsClick != null) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.testTag("top_settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = strings.settingsTitle)
                }
            }
        }
    )
}

@Composable
fun WorkerAvatar(
    name: String,
    photoPath: String?,
    size: Dp = 48.dp
) {
    val extra = LocalHisabColors.current
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "W"
    val fileExists = remember(photoPath) {
        !photoPath.isNullOrBlank() && File(photoPath).exists()
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(NavyPrimary)
            .border(1.5.dp, GoldBright.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (fileExists) {
            AsyncImage(
                model = File(photoPath!!),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                color = extra.gold,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val extra = LocalHisabColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extra.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(extra.goldContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = extra.gold,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = extra.textMuted
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text(actionLabel, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmText: String,
    isDestructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) extra.crimson else NavyPrimary
                ),
                modifier = Modifier.testTag("dialog_confirm_button")
            ) {
                Text(confirmText, color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDropdownSelector(
    workers: List<WorkerEntity>,
    selectedWorkerId: Long?,
    label: String,
    onWorkerSelected: (WorkerEntity) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedWorker = workers.firstOrNull { it.id == selectedWorkerId }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedWorker?.let { "${it.fullName} (${it.workerCode})" } ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag("worker_dropdown_field")
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }
        ) {
            workers.forEach { w ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(w.fullName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${w.workerCode} • ${w.workCategory}",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalHisabColors.current.textMuted
                            )
                        }
                    },
                    onClick = {
                        onWorkerSelected(w)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DateInputRow(
    label: String,
    dateText: String,
    onDateTextChange: (String) -> Unit,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    OutlinedTextField(
        value = dateText,
        onValueChange = onDateTextChange,
        label = { Text(label) },
        isError = isError,
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = {
                val key = IndiaFormatters.parseDdMmYyyyToDateKey(dateText) ?: IndiaFormatters.todayDateKey()
                val cal = IndiaFormatters.dateKeyToCalendar(key)
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val newKey = year * 10000L + (month + 1) * 100L + dayOfMonth
                        onDateTextChange(IndiaFormatters.formatDateKey(newKey))
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }) {
                Icon(Icons.Default.CalendarToday, contentDescription = label)
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun TimeInputField(
    label: String,
    timeText: String,
    onTimeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    OutlinedTextField(
        value = timeText,
        onValueChange = onTimeChange,
        label = { Text(label) },
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = {
                val parts = timeText.split(":")
                val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
                val min = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
                TimePickerDialog(
                    context,
                    { _, h, m ->
                        onTimeChange(String.format(Locale.US, "%02d:%02d", h, m))
                    },
                    hour,
                    min,
                    true
                ).show()
            }) {
                Icon(Icons.Default.Schedule, contentDescription = label)
            }
        },
        modifier = modifier
    )
}
