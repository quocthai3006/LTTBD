package com.example.btap1_tuan04

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TaskDetailScreen(taskId: String, store: TodoTaskStore, onBack: () -> Unit, onEdit: () -> Unit) {
    val task = store.tasks.firstOrNull { it.id == taskId }
    var confirmDelete by rememberSaveable(taskId) { mutableStateOf(false) }
    var newSubtask by rememberSaveable(taskId) { mutableStateOf("") }
    var editingIndex by rememberSaveable(taskId) { mutableStateOf(-1) }
    var editingText by rememberSaveable(taskId) { mutableStateOf("") }
    LaunchedEffect(taskId) { store.loadTask(taskId, onBack) }
    DisposableEffect(taskId) { onDispose { store.cancelDetailLoad() } }
    BackHandler(enabled = store.isSaving) { }
    val enabled = store.isReady && !store.isSaving && !store.isLoading

    Scaffold(containerColor = TodoPalette.Background, contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (task != null) Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp)) {
                Button(onClick = { store.toggleTask(task.id) }, enabled = enabled,
                    shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    Text(if (task.completed) "Đánh dấu chưa xong" else "Đánh dấu hoàn thành", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { confirmDelete = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                    Text("Xóa công việc", color = TodoPalette.Red)
                }
            }
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, enabled = !store.isSaving) {
                    TodoIcon(TodoSymbol.BACK, size = 15.dp); Text("Quay lại")
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onEdit, enabled = task != null && enabled) { Text("Sửa", fontWeight = FontWeight.SemiBold) }
            }
            if (store.detailLoading || store.isSaving) LinearProgressIndicator(Modifier.fillMaxWidth())
            store.detailError?.let { message -> TaskErrorCard(message, store.detailLoading) { store.loadTask(taskId, onBack) } }
            if (task == null) {
                if (store.detailLoading) Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    DetailChip(task.category.label, TodoPalette.Blue, TodoPalette.BlueLight)
                    DetailChip("Ưu tiên ${task.priority.label.lowercase()}",
                        if (task.priority == TodoPriority.HIGH) TodoPalette.Red else TodoPalette.Muted,
                        if (task.priority == TodoPriority.HIGH) Color(0xFFFFE8E4) else TodoPalette.Line)
                    if (task.completed) DetailChip("Đã xong", TodoPalette.Green, TodoPalette.GreenLight)
                }
                Text(task.title, fontSize = 27.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None)
                Surface(color = Color.White, shape = RoundedCornerShape(16.dp)) {
                    Column {
                        InfoRow("Hạn chót", "${taskDateLabel(task.scheduledDay)}, ${task.time}")
                        HorizontalDivider(color = TodoPalette.Line)
                        InfoRow("Nhắc nhở", if (task.remind) "Trước 15 phút" else "Tắt")
                    }
                }
                if (task.note.isNotBlank()) {
                    SmallTitle("Ghi chú")
                    Surface(color = Color.White, shape = RoundedCornerShape(15.dp)) {
                        Text(task.note, fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.fillMaxWidth().padding(16.dp))
                    }
                }
                val done = task.subtasks.count { it.done }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SmallTitle("Việc nhỏ")
                    Text("$done/${task.subtasks.size}", color = TodoPalette.Muted, fontSize = 13.sp)
                }
                LinearProgressIndicator(progress = { if (task.subtasks.isEmpty()) 0f else done.toFloat() / task.subtasks.size },
                    modifier = Modifier.fillMaxWidth().height(5.dp), trackColor = TodoPalette.Line)
                Surface(color = Color.White, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth()) {
                        if (task.subtasks.isEmpty()) Text("Chưa có việc nhỏ nào. Chia nhỏ để dễ bắt đầu hơn.",
                            fontSize = 13.sp, color = TodoPalette.Muted, modifier = Modifier.padding(16.dp))
                        task.subtasks.forEachIndexed { index, item ->
                            Row(Modifier.fillMaxWidth().padding(start = 2.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = item.done, enabled = enabled, onCheckedChange = { checked ->
                                    store.saveSubtasks(task.id, task.subtasks.mapIndexed { i, sub -> if (i == index) sub.copy(done = checked) else sub })
                                })
                                Text(item.title, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                                    color = if (item.done) TodoPalette.Muted else TodoPalette.Ink,
                                    textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None)
                                TextButton(onClick = { editingIndex = index; editingText = item.title }, enabled = enabled,
                                    contentPadding = PaddingValues(5.dp), modifier = Modifier.widthIn(min = 40.dp)) { Text("Sửa", fontSize = 11.sp) }
                                TextButton(onClick = { store.saveSubtasks(task.id, task.subtasks.filterIndexed { i, _ -> i != index }) },
                                    enabled = enabled, contentPadding = PaddingValues(5.dp), modifier = Modifier.widthIn(min = 40.dp)) {
                                    Text("Xóa", color = TodoPalette.Red, fontSize = 11.sp)
                                }
                            }
                            HorizontalDivider(color = TodoPalette.Line)
                        }
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = newSubtask, onValueChange = { if (it.length <= 200) newSubtask = it },
                                enabled = !store.isSaving, modifier = Modifier.weight(1f), placeholder = { Text("Thêm việc nhỏ", fontSize = 12.sp) },
                                singleLine = true, shape = RoundedCornerShape(10.dp))
                            TextButton(onClick = {
                                store.saveSubtasks(task.id, task.subtasks + TodoSubtask(newSubtask.trim())) { newSubtask = "" }
                            }, enabled = enabled && newSubtask.isNotBlank()) { Text("Thêm") }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
    if (confirmDelete && task != null) AlertDialog(
        onDismissRequest = { if (!store.isSaving) confirmDelete = false },
        title = { Text("Xóa việc này?") }, text = { Text("Không thể hoàn tác.") },
        confirmButton = { TextButton(enabled = enabled, onClick = {
            store.deleteTask(taskId) { confirmDelete = false; onBack() }
        }) { Text(if (store.isSaving) "Đang xóa..." else "Xóa", color = TodoPalette.Red) } },
        dismissButton = { TextButton(enabled = !store.isSaving, onClick = { confirmDelete = false }) { Text("Hủy") } }
    )
    if (editingIndex >= 0 && task != null) AlertDialog(
        onDismissRequest = { if (!store.isSaving) editingIndex = -1 }, title = { Text("Sửa việc nhỏ") },
        text = { OutlinedTextField(value = editingText, onValueChange = { if (it.length <= 200) editingText = it },
            enabled = !store.isSaving, label = { Text("Tên việc nhỏ") }) },
        confirmButton = { TextButton(enabled = enabled && editingText.isNotBlank(), onClick = {
            store.saveSubtasks(taskId, task.subtasks.mapIndexed { i, item ->
                if (i == editingIndex) item.copy(title = editingText.trim()) else item
            }) { editingIndex = -1 }
        }) { Text("Lưu") } },
        dismissButton = { TextButton(enabled = !store.isSaving, onClick = { editingIndex = -1 }) { Text("Hủy") } }
    )
}

@Composable
private fun DetailChip(text: String, color: Color, background: Color) {
    Text(text, color = color, fontSize = 10.sp, modifier = Modifier.background(background, RoundedCornerShape(6.dp))
        .padding(horizontal = 7.dp, vertical = 5.dp))
}
@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = TodoPalette.Muted)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
@Composable
internal fun SmallTitle(text: String) { Text(text, fontSize = 13.sp, color = TodoPalette.Muted, fontWeight = FontWeight.SemiBold) }
