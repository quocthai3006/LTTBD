package com.example.btap1_tuan04

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Locale

@Composable
fun TaskFormScreen(store: TodoTaskStore, editingId: String?, onCancel: () -> Unit, onSaved: (String) -> Unit) {
    val context = LocalContext.current
    val task = store.tasks.firstOrNull { it.id == editingId }
    var initialized by rememberSaveable(editingId) { mutableStateOf(editingId == null) }
    var title by rememberSaveable(editingId) { mutableStateOf("") }
    var note by rememberSaveable(editingId) { mutableStateOf("") }
    var category by rememberSaveable(editingId) { mutableStateOf(TodoCategory.STUDY.name) }
    var date by rememberSaveable(editingId) { mutableStateOf(dayKey()) }
    var time by rememberSaveable(editingId) { mutableStateOf("19:00") }
    var priority by rememberSaveable(editingId) { mutableStateOf(TodoPriority.MEDIUM.name) }
    var remind by rememberSaveable(editingId) { mutableStateOf(true) }
    LaunchedEffect(editingId) {
        store.clearActionError()
        if (editingId != null && task == null) store.loadTask(editingId, onCancel)
    }
    LaunchedEffect(task?.id) {
        if (!initialized && task != null) {
            title = task.title; note = task.note; category = task.category.name; date = task.scheduledDay
            time = task.time; priority = task.priority.name; remind = task.remind; initialized = true
        }
    }
    DisposableEffect(editingId) { onDispose { if (editingId != null) store.cancelDetailLoad() } }
    BackHandler(enabled = store.isSaving) { }
    val enabled = initialized && store.isReady && !store.isSaving && !store.isLoading
    val canSave = enabled && title.isNotBlank()
    val save: () -> Unit = {
        store.saveTask(TaskDraft(title, note, TodoCategory.valueOf(category), date, time,
            TodoPriority.valueOf(priority), remind), editingId, onSaved)
    }
    Scaffold(modifier = Modifier.imePadding(), containerColor = TodoPalette.Background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = { Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Button(onClick = save, enabled = canSave, shape = RoundedCornerShape(15.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp)) {
                if (store.isSaving) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)) }
                Text(if (store.isSaving) "Đang lưu..." else "Lưu công việc", fontWeight = FontWeight.Bold)
            }
        } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCancel, enabled = !store.isSaving) { Text("Hủy") }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(if (editingId == null) "Việc mới" else "Sửa công việc", fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = save, enabled = canSave) { Text("Lưu") }
            }
            if (store.isLoading || store.detailLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            store.loadError?.let { TaskErrorCard(it, store.isLoading, store::refresh) }
            store.actionError?.let { Text(it, color = TodoPalette.Red, fontSize = 13.sp) }
            OutlinedTextField(value = title, onValueChange = { if (it.length <= 200) title = it }, enabled = !store.isSaving,
                label = { Text("Tên công việc *") }, placeholder = { Text("Ví dụ: Ôn chương 4 – Giải thuật") },
                modifier = Modifier.fillMaxWidth(), maxLines = 3, shape = RoundedCornerShape(14.dp))
            OutlinedTextField(value = note, onValueChange = { if (it.length <= 5000) note = it }, enabled = !store.isSaving,
                label = { Text("Ghi chú") }, placeholder = { Text("Thêm ghi chú (không bắt buộc)") },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5, shape = RoundedCornerShape(14.dp))
            SmallTitle("Nhóm")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TodoCategory.entries.forEach { item -> FilterChip(selected = category == item.name,
                    onClick = { category = item.name }, enabled = !store.isSaving, modifier = Modifier.weight(1f),
                    label = { Text(item.label, fontSize = 13.sp, modifier = Modifier.padding(vertical = 7.dp)) }) }
            }
            SmallTitle("Ngày")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(dayKey() to "Hôm nay", dayKeyAfter(1) to "Ngày mai", endOfWeekKey() to "Tuần này")
                    .forEach { (key, label) -> FilterChip(selected = date == key, onClick = { date = key },
                        enabled = !store.isSaving, modifier = Modifier.weight(1f), label = { Text(label, fontSize = 12.sp, maxLines = 1) }) }
            }
            OutlinedButton(onClick = {
                val calendar = vietnamCalendar(parseDay(date))
                DatePickerDialog(context, { _, year, month, day ->
                    date = String.format(Locale.US, "%04d%02d%02d", year, month + 1, day)
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
            }, enabled = !store.isSaving, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("Chọn ngày: ${dayKeyToApiDate(date)}")
            }
            Surface(color = Color.White, shape = RoundedCornerShape(16.dp)) {
                Column {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Giờ", modifier = Modifier.weight(1f))
                        TextButton(enabled = !store.isSaving, onClick = {
                            val values = time.split(':')
                            TimePickerDialog(context, { _, hour, minute -> time = String.format(Locale.US, "%02d:%02d", hour, minute) },
                                values[0].toInt(), values[1].toInt(), true).show()
                        }) { Text(time) }
                    }
                    HorizontalDivider(color = TodoPalette.Line)
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Nhắc tôi")
                            Text("Trước 15 phút", fontSize = 12.sp, color = TodoPalette.Muted)
                        }
                        Switch(checked = remind, onCheckedChange = { remind = it }, enabled = !store.isSaving)
                    }
                }
            }
            if (remind) ReminderPermissionHint()
            SmallTitle("Mức ưu tiên")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TodoPriority.entries.forEach { item -> FilterChip(selected = priority == item.name,
                    onClick = { priority = item.name }, enabled = !store.isSaving, modifier = Modifier.weight(1f),
                    label = { Text(item.label, modifier = Modifier.padding(vertical = 5.dp)) }) }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
