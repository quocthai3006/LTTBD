package com.example.btap1_tuan04

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    user: FirebaseUser?, store: TodoTaskStore, onOpenProfile: () -> Unit,
    onOpenTask: (String) -> Unit, onAddTask: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var category by rememberSaveable { mutableStateOf("ALL") }
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(selectedTab, store) {
        if (selectedTab == 0 || selectedTab == 1) store.refresh()
    }
    val canChange = store.isReady && !store.isSaving && !store.isLoading
    Scaffold(containerColor = TodoPalette.Background, contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = { TodoBottomBar(selectedTab, { selectedTab = it }, onAddTask) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (store.isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (store.loadError != null && selectedTab < 2) item {
                TaskErrorCard(store.loadError.orEmpty(), store.isLoading, store::refresh)
            }
            when (selectedTab) {
                0 -> {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = onOpenProfile)
                                .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                AccountAvatar(if (user == null) "Khách" else accountDisplayName(user), 42.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(homeDateLabel(), fontSize = 11.sp, color = TodoPalette.Muted)
                                    Text(if (user == null) "Chào bạn!" else "Chào ${accountDisplayName(user)}!",
                                        fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Box {
                                IconButton(onClick = { selectedTab = 2 },
                                    modifier = Modifier.background(Color.White, CircleShape)
                                        .semantics { contentDescription = "Mở thông báo" }) {
                                    TodoIcon(TodoSymbol.BELL, tint = TodoPalette.Ink)
                                }
                                if (store.notices.unreadCount > 0) Box(Modifier.align(Alignment.TopEnd)
                                    .offset((-8).dp, 8.dp).size(7.dp).background(TodoPalette.Red, CircleShape))
                            }
                        }
                    }
                    item { TodayProgressCard(store.todayCompleted, store.todayTasks.size) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TodoCategory.entries.forEach { item -> CategoryCard(item, store.pendingIn(item), Modifier.weight(1f)) {
                                category = item.name; query = ""; selectedTab = 1
                            } }
                        }
                    }
                    if (store.tasks.any { it.remind && !it.completed }) item { ReminderPermissionHint() }
                    item { SectionTitle("Việc hôm nay", "Xem tất cả") { category = "ALL"; query = ""; selectedTab = 1 } }
                    if (store.todayTasks.isEmpty() && !store.isLoading && store.loadError == null)
                        item { EmptyTasks("Hôm nay chưa có việc nào. Bấm + để thêm nhé.") }
                    items(store.todayTasks, key = { it.id }) { task ->
                        TaskRow(task, canChange, { store.toggleTask(task.id) }, { onOpenTask(task.id) })
                    }
                }
                1 -> {
                    item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Danh sách", fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${store.pendingCount} việc chưa xong", fontSize = 12.sp, color = TodoPalette.Muted)
                    } }
                    item { OutlinedTextField(value = query, onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(), placeholder = { Text("Tìm công việc") }, singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        trailingIcon = { if (query.isNotEmpty()) TextButton(onClick = { query = "" }) { Text("Xóa") } }) }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ALL" to "Tất cả", "STUDY" to "Học tập", "LIFE" to "Cuộc sống").forEach { (key, title) ->
                            FilterChip(selected = category == key, onClick = { category = key }, label = { Text(title, fontSize = 12.sp) })
                        }
                    } }
                    val filtered = store.tasks.filter {
                        (category == "ALL" || it.category.name == category) && it.title.contains(query.trim(), ignoreCase = true)
                    }
                    val pending = filtered.filterNot { it.completed }
                    val completed = filtered.filter { it.completed }
                    if (filtered.isEmpty() && !store.isLoading && store.loadError == null)
                        item { EmptyTasks("Không tìm thấy công việc nào.") }
                    if (pending.isNotEmpty()) item { GroupLabel("CẦN LÀM") }
                    items(pending, key = { "todo-${it.id}" }) { task ->
                        TaskRow(task, canChange, { store.toggleTask(task.id) }, { onOpenTask(task.id) }, showDay = true)
                    }
                    if (completed.isNotEmpty()) item { GroupLabel("ĐÃ XONG") }
                    items(completed, key = { "done-${it.id}" }) { task ->
                        TaskRow(task, canChange, { store.toggleTask(task.id) }, { onOpenTask(task.id) }, showDay = true)
                    }
                }
                2 -> {
                    item { SectionTitle("Thông báo", "Đánh dấu đã đọc", store.notices::markAllRead) }
                    item { ReminderPermissionHint() }
                    if (store.notices.items.isEmpty()) item { EmptyTasks("Chưa có thông báo nào.") }
                    items(store.notices.items, key = { it.id }) { notice ->
                        Surface(shape = RoundedCornerShape(16.dp), color = if (notice.read) Color.White else TodoPalette.BlueLight,
                            modifier = Modifier.fillMaxWidth().clickable {
                                store.notices.markRead(notice.id)
                                if (store.tasks.any { it.id == notice.taskId }) onOpenTask(notice.taskId)
                            }) {
                            Row(Modifier.padding(15.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TodoIcon(TodoSymbol.BELL, tint = if (notice.title == "Đã quá hạn") TodoPalette.Red else TodoPalette.Blue)
                                Column(Modifier.weight(1f)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(notice.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        Text(SimpleDateFormat("HH:mm", Locale("vi", "VN")).format(Date(notice.timestamp)),
                                            fontSize = 10.sp, color = TodoPalette.Muted)
                                    }
                                    Spacer(Modifier.height(5.dp))
                                    Text(notice.body, fontSize = 13.sp, color = TodoPalette.Muted)
                                }
                                if (!notice.read) Box(Modifier.size(6.dp).background(TodoPalette.Blue, CircleShape))
                            }
                        }
                    }
                }
                3 -> {
                    item { SectionTitle("Cài đặt") }
                    item { Surface(shape = RoundedCornerShape(16.dp), color = Color.White,
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenProfile)) {
                        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            AccountAvatar(if (user == null) "Khách" else accountDisplayName(user))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (user == null) "Đang dùng thử" else accountDisplayName(user), fontWeight = FontWeight.SemiBold)
                                Text("Xem hồ sơ và tài khoản", fontSize = 12.sp, color = TodoPalette.Muted)
                            }
                            TodoIcon(TodoSymbol.NEXT, tint = TodoPalette.Muted)
                        }
                    } }
                    item { ReminderPermissionHint() }
                    item { Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                        Column(Modifier.fillMaxWidth().padding(18.dp)) {
                            Text("UTH Todo", fontWeight = FontWeight.Bold)
                            Text("Quản lý công việc mỗi ngày", fontSize = 13.sp, color = TodoPalette.Muted)
                            Text(TodoRepository.appInfo.version, fontSize = 12.sp, color = TodoPalette.Muted)
                        }
                    } }
                }
            }
        }
    }
}

@Composable
internal fun TaskErrorCard(message: String, loading: Boolean = false, onRetry: () -> Unit) {
    Surface(color = Color(0xFFFFECE8), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(message, color = TodoPalette.Red, fontSize = 13.sp)
            TextButton(onClick = onRetry, enabled = !loading) { Text("Thử lại") }
        }
    }
}

@Composable
private fun TodayProgressCard(done: Int, total: Int) {
    val fraction = if (total == 0) 0f else done.toFloat() / total
    Surface(color = TodoPalette.Blue, shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Tiến độ hôm nay", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                Spacer(Modifier.height(5.dp))
                Text("$done/$total việc", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(when { total > 0 && done == total -> "Xong hết rồi, nghỉ ngơi thôi!"
                    fraction >= 0.5f -> "Đã hơn nửa chặng, cố lên!"
                    else -> "Bắt đầu từ việc nhỏ nhất nhé." }, color = Color.White, fontSize = 11.sp)
            }
            Box(Modifier.size(68.dp).semantics { contentDescription = "Hoàn thành ${(fraction * 100).toInt()} phần trăm" },
                contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                    val stroke = Stroke(6.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(Color.White.copy(alpha = 0.28f), -90f, 360f, false, style = stroke)
                    if (fraction > 0) drawArc(Color.White, -90f, fraction * 360, false, style = stroke)
                }
                Text("${(fraction * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CategoryCard(category: TodoCategory, count: Int, modifier: Modifier, onClick: () -> Unit) {
    val study = category == TodoCategory.STUDY
    Surface(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(17.dp), color = Color.White) {
        Column(Modifier.padding(17.dp)) {
            Box(Modifier.size(34.dp).background(if (study) TodoPalette.BlueLight else TodoPalette.Sand,
                RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                TodoIcon(if (study) TodoSymbol.STUDY else TodoSymbol.HOME,
                    tint = if (study) TodoPalette.Blue else TodoPalette.Brown, size = 20.dp)
            }
            Spacer(Modifier.height(10.dp))
            Text(category.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("$count việc còn lại", fontSize = 12.sp, color = TodoPalette.Muted)
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (action != null) TextButton(onClick = onAction) { Text(action, fontSize = 12.sp) }
    }
}

@Composable
private fun GroupLabel(text: String) { Text(text, fontSize = 12.sp, color = TodoPalette.Muted, fontWeight = FontWeight.SemiBold) }

@Composable
private fun EmptyTasks(message: String) {
    Text(message, Modifier.fillMaxWidth().padding(vertical = 24.dp), fontSize = 14.sp,
        color = TodoPalette.Muted, textAlign = TextAlign.Center)
}

@Composable
private fun TaskRow(task: TodoTask, enabled: Boolean, onToggle: () -> Unit, onDetails: () -> Unit, showDay: Boolean = false) {
    val study = task.category == TodoCategory.STUDY
    Surface(Modifier.fillMaxWidth().clickable(onClick = onDetails), shape = RoundedCornerShape(16.dp), color = Color.White) {
        Row(Modifier.padding(start = 7.dp, end = 13.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).semantics { contentDescription =
                if (task.completed) "Đánh dấu chưa xong: ${task.title}" else "Hoàn thành: ${task.title}" }
                .clickable(enabled = enabled, role = Role.Checkbox, onClick = onToggle), contentAlignment = Alignment.Center) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(if (task.completed) TodoPalette.Blue else Color.White)
                    .border(1.5.dp, if (task.completed) TodoPalette.Blue else Color(0xFFCACAD2), CircleShape), contentAlignment = Alignment.Center) {
                    if (task.completed) TodoIcon(TodoSymbol.CHECK, tint = Color.White, size = 15.dp)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(task.title, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = if (task.completed) TodoPalette.Muted else TodoPalette.Ink,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None)
                if (!task.completed || !showDay) {
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(task.category.label, fontSize = 10.sp, color = if (study) TodoPalette.Blue else TodoPalette.Brown,
                            modifier = Modifier.background(if (study) TodoPalette.BlueLight else TodoPalette.Sand,
                                RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp))
                        Spacer(Modifier.width(7.dp))
                        Text((if (showDay) taskDateLabel(task.scheduledDay) + ", " else "") + task.time,
                            fontSize = 11.sp, color = TodoPalette.Muted)
                    }
                }
            }
            if (!task.completed && task.priority == TodoPriority.HIGH) Text("Gấp", fontSize = 10.sp,
                color = TodoPalette.Red, modifier = Modifier.padding(start = 4.dp).background(Color(0xFFFFE8E4),
                    RoundedCornerShape(5.dp)).padding(5.dp))
            else TodoIcon(TodoSymbol.NEXT, tint = Color(0xFFBCBDC3), size = 15.dp)
        }
    }
}

@Composable
private fun TodoBottomBar(selected: Int, onTab: (Int) -> Unit, onAdd: () -> Unit) {
    Surface(color = Color.White, shadowElevation = 4.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(76.dp).padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically) {
            BottomItem("Trang chủ", TodoSymbol.HOME, selected == 0, Modifier.weight(1f)) { onTab(0) }
            BottomItem("Danh sách", TodoSymbol.LIST, selected == 1, Modifier.weight(1f)) { onTab(1) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                FloatingActionButton(onClick = onAdd, containerColor = TodoPalette.Blue, contentColor = Color.White,
                    shape = CircleShape, modifier = Modifier.size(54.dp).semantics { contentDescription = "Thêm công việc" }) {
                    TodoIcon(TodoSymbol.PLUS, tint = Color.White, size = 26.dp)
                }
            }
            BottomItem("Thông báo", TodoSymbol.BELL, selected == 2, Modifier.weight(1f)) { onTab(2) }
            BottomItem("Cài đặt", TodoSymbol.SETTINGS, selected == 3, Modifier.weight(1f)) { onTab(3) }
        }
    }
}

@Composable
private fun BottomItem(label: String, icon: TodoSymbol, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color = if (selected) TodoPalette.Blue else TodoPalette.Muted
    Column(modifier.fillMaxHeight().clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        TodoIcon(icon, tint = color, size = 22.dp)
        Spacer(Modifier.height(5.dp))
        Text(label, fontSize = 10.sp, maxLines = 1, color = color)
    }
}
