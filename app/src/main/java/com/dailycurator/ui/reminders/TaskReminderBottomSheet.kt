package com.dailycurator.ui.reminders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dailycurator.data.pomodoro.PomodoroLaunchRequest
import com.dailycurator.data.repository.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TaskReminderSheetEntryPoint {
    fun taskRepository(): TaskRepository
    fun pomodoroNavBridge(): com.dailycurator.data.pomodoro.PomodoroNavBridge
    fun taskReminderScheduler(): com.dailycurator.reminders.TaskReminderScheduler
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskReminderBottomSheet(
    taskId: Long,
    onDismiss: () -> Unit,
    onNavigateToPomodoro: () -> Unit,
) {
    val context = LocalContext.current
    val entry = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, TaskReminderSheetEntryPoint::class.java)
    }
    val repo = entry.taskRepository()
    val bridge = entry.pomodoroNavBridge()
    val scheduler = entry.taskReminderScheduler()

    var title by remember { mutableStateOf("…") }
    var slider by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(taskId) {
        val t = repo.getById(taskId)
        title = t?.title ?: "Task"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Slide all the way right to mark done (optional), or use the buttons below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text("Mark done", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = slider,
                onValueChange = { slider = it },
                onValueChangeFinished = {
                    if (slider >= 0.97f) {
                        scope.launch {
                            repo.setDoneById(taskId, true)
                            scheduler.cancel(taskId)
                            onDismiss()
                        }
                    } else {
                        slider = 0f
                    }
                },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    scope.launch {
                        val t = repo.getById(taskId) ?: return@launch
                        bridge.push(
                            PomodoroLaunchRequest(
                                entityType = PomodoroLaunchRequest.TYPE_TASK,
                                entityId = t.id,
                                title = t.title,
                            ),
                        )
                        onDismiss()
                        onNavigateToPomodoro()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Start Pomodoro")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Dismiss")
            }
        }
    }
}
