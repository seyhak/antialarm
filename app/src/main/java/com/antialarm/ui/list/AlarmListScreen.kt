package com.antialarm.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.antialarm.data.model.Alarm
import com.antialarm.ui.theme.AccentOrange
import com.antialarm.ui.theme.AccentRed
import com.antialarm.ui.theme.DarkCard
import com.antialarm.ui.theme.DarkCardVariant
import com.antialarm.ui.theme.TextMuted
import com.antialarm.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
        onAddAlarm: () -> Unit,
        onEditAlarm: (Int) -> Unit,
        viewModel: AlarmListViewModel = hiltViewModel()
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(60000) // Update every minute
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                        Icons.Default.Alarm,
                                        contentDescription = null,
                                        tint = AccentOrange,
                                        modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("AntiAlarm", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            }
                        },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background,
                                        titleContentColor = MaterialTheme.colorScheme.onBackground
                                )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                        onClick = onAddAlarm,
                        containerColor = AccentOrange,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                ) { Icon(Icons.Default.Add, contentDescription = "Add alarm") }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (alarms.isEmpty()) {
            Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                            Icons.Default.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = TextMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                            "No alarms yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                            "Tap + to create your first alarm",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                items(alarms, key = { it.id }) { alarm ->
                    SwipeToDeleteAlarmItem(
                            alarm = alarm,
                            currentTimeMillis = currentTimeMillis,
                            onToggle = { enabled -> viewModel.toggleAlarm(alarm.id, enabled) },
                            onClick = { onEditAlarm(alarm.id) },
                            onDelete = {
                                viewModel.deleteAlarm(alarm)
                                scope.launch {
                                    val result =
                                            snackbarHostState.showSnackbar(
                                                    message = "Alarm deleted",
                                                    actionLabel = "Undo",
                                                    duration = SnackbarDuration.Short
                                            )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.undoDelete()
                                    }
                                }
                            }
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteAlarmItem(
        alarm: Alarm,
        currentTimeMillis: Long,
        onToggle: (Boolean) -> Unit,
        onClick: () -> Unit,
        onDelete: () -> Unit
) {
    var isRemoved by remember { mutableStateOf(false) }
    val dismissState =
            rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            isRemoved = true
                            true
                        } else false
                    }
            )

    LaunchedEffect(isRemoved) {
        if (isRemoved) {
            delay(300)
            onDelete()
        }
    }

    AnimatedVisibility(
            visible = !isRemoved,
            exit = shrinkVertically(tween(300)) + fadeOut(tween(300))
    ) {
        SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    Box(
                            modifier =
                                    Modifier.fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(AccentRed),
                            contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.padding(end = 24.dp),
                                tint = MaterialTheme.colorScheme.onError
                        )
                    }
                },
                enableDismissFromStartToEnd = false
        ) {
            AlarmCard(
                    alarm = alarm,
                    currentTimeMillis = currentTimeMillis,
                    onToggle = onToggle,
                    onClick = onClick
            )
        }
    }
}

@Composable
private fun AlarmCard(
        alarm: Alarm,
        currentTimeMillis: Long,
        onToggle: (Boolean) -> Unit,
        onClick: () -> Unit
) {
    Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    if (alarm.isEnabled) DarkCard
                                    else DarkCardVariant.copy(alpha = 0.5f)
                    ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = alarm.getTimeString(),
                        style =
                                MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 42.sp
                                ),
                        color =
                                if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface
                                else TextMuted
                )

                if (alarm.label.isNotBlank()) {
                    Text(
                            text = alarm.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (alarm.isEnabled) TextSecondary else TextMuted
                    )
                }

                val timeUntilNext = alarm.getTimeUntilNext(currentTimeMillis)
                if (timeUntilNext != null) {
                    Text(
                            text = timeUntilNext,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AccentOrange.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                    )
                }

                val dayNames = alarm.getEnabledDayNames()
                if (dayNames.isNotEmpty()) {
                    Text(
                            text = dayNames.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (alarm.isEnabled) AccentOrange else TextMuted
                    )
                } else {
                    Text(
                            text = "One-time",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                    )
                }
            }

            Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    colors =
                            SwitchDefaults.colors(
                                    checkedThumbColor = AccentOrange,
                                    checkedTrackColor = AccentOrange.copy(alpha = 0.3f),
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = DarkCardVariant
                            )
            )
        }
    }
}
