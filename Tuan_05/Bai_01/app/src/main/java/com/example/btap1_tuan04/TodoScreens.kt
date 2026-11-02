package com.example.btap1_tuan04

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseUser

internal object TodoPalette {
    val Blue = Color(0xFF3C56E5)
    val BlueLight = Color(0xFFECEFFD)
    val Background = Color(0xFFF5F5FA)
    val Ink = Color(0xFF202124)
    val Muted = Color(0xFF85868D)
    val Line = Color(0xFFE9E9EF)
    val Green = Color(0xFF438966)
    val GreenLight = Color(0xFFE9F6EE)
    val Red = Color(0xFFBA5946)
    val Sand = Color(0xFFFFF0DE)
    val Brown = Color(0xFFAB7D4E)
}

@Composable
internal fun UthTodoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = TodoPalette.Blue,
            background = TodoPalette.Background,
            surface = Color.White,
            onSurface = TodoPalette.Ink,
            onBackground = TodoPalette.Ink,
            error = TodoPalette.Red
        ),
        content = content
    )
}

internal enum class TodoSymbol { HOME, LIST, PLUS, BELL, SETTINGS, BACK, NEXT, CHECK, EXIT, STUDY }

// Các biểu tượng được vẽ bằng Canvas; không cần thêm thư viện icon.
@Composable
internal fun TodoIcon(
    kind: TodoSymbol,
    modifier: Modifier = Modifier,
    tint: Color = TodoPalette.Blue,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        scale(this.size.width / 24f, this.size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(width = 1.65f, cap = StrokeCap.Round)
            fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
                drawLine(tint, Offset(x1, y1), Offset(x2, y2), 1.65f, StrokeCap.Round)
            }
            fun path(vararg points: Pair<Float, Float>, closed: Boolean = false) {
                val p = Path().apply {
                    moveTo(points[0].first, points[0].second)
                    points.drop(1).forEach { lineTo(it.first, it.second) }
                    if (closed) close()
                }
                drawPath(p, tint, style = stroke)
            }
            when (kind) {
                TodoSymbol.HOME -> {
                    path(3f to 10f, 12f to 3f, 21f to 10f)
                    path(5f to 9f, 5f to 21f, 10f to 21f, 10f to 14f,
                        14f to 14f, 14f to 21f, 19f to 21f, 19f to 9f)
                }
                TodoSymbol.LIST -> {
                    listOf(6f, 12f, 18f).forEach { y ->
                        drawCircle(tint, 1.1f, Offset(4f, y))
                        line(8f, y, 21f, y)
                    }
                }
                TodoSymbol.PLUS -> { line(12f, 4f, 12f, 20f); line(4f, 12f, 20f, 12f) }
                TodoSymbol.BACK -> path(14f to 5f, 7f to 12f, 14f to 19f)
                TodoSymbol.NEXT -> path(9f to 6f, 15f to 12f, 9f to 18f)
                TodoSymbol.CHECK -> path(5f to 12f, 10f to 17f, 19f to 7f)
                TodoSymbol.EXIT -> {
                    path(10f to 3f, 4f to 3f, 4f to 21f, 10f to 21f)
                    line(9f, 12f, 21f, 12f)
                    path(17f to 8f, 21f to 12f, 17f to 16f)
                }
                TodoSymbol.SETTINGS -> {
                    line(3f, 7f, 21f, 7f); line(3f, 17f, 21f, 17f)
                    drawCircle(Color.White, 2.4f, Offset(9f, 7f))
                    drawCircle(tint, 2.4f, Offset(9f, 7f), style = stroke)
                    drawCircle(Color.White, 2.4f, Offset(16f, 17f))
                    drawCircle(tint, 2.4f, Offset(16f, 17f), style = stroke)
                }
                TodoSymbol.BELL -> {
                    val p = Path().apply {
                        moveTo(5f, 18f); lineTo(7f, 15f); lineTo(7f, 10f)
                        cubicTo(7f, 3f, 17f, 3f, 17f, 10f)
                        lineTo(17f, 15f); lineTo(19f, 18f); close()
                    }
                    drawPath(p, tint, style = stroke)
                    line(12f, 3f, 12f, 4f)
                    drawArc(tint, 0f, 180f, false, Offset(10f, 19f),
                        androidx.compose.ui.geometry.Size(4f, 3f), style = stroke)
                }
                TodoSymbol.STUDY -> {
                    path(2f to 9f, 12f to 4f, 22f to 9f, 12f to 14f, closed = true)
                    path(6f to 11f, 6f to 17f, 12f to 20f, 18f to 17f, 18f to 11f)
                    line(22f, 9f, 22f, 16f)
                }
            }
        }
    }
}

@Composable
internal fun ChecklistLogo() {
    Surface(
        modifier = Modifier.size(76.dp),
        shape = RoundedCornerShape(23.dp),
        color = TodoPalette.Blue,
        shadowElevation = 12.dp
    ) {
        Canvas(Modifier.fillMaxSize().padding(17.dp)) {
            val w = size.width
            val h = size.height
            listOf(0.25f, 0.68f).forEachIndexed { index, fraction ->
                val y = h * fraction
                drawCircle(Color.White.copy(alpha = if (index == 0) 1f else 0.65f),
                    w * 0.105f, Offset(w * 0.15f, y), style = Stroke(2.3.dp.toPx()))
                drawLine(Color.White.copy(alpha = if (index == 0) 1f else 0.65f),
                    Offset(w * 0.43f, y), Offset(w * 0.94f, y),
                    3.5.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}

internal fun accountDisplayName(user: FirebaseUser?): String =
    user?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.email?.substringBefore('@')?.takeIf { it.isNotBlank() }
        ?: "Bạn"

private fun accountInitials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return if (words.size >= 2) "${words.first().first()}${words.last().first()}".uppercase()
        else words.firstOrNull()?.take(2)?.uppercase() ?: "UT"
}

@Composable
internal fun AccountAvatar(name: String, size: Dp = 40.dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(TodoPalette.BlueLight),
        contentAlignment = Alignment.Center
    ) {
        Text(accountInitials(name), color = TodoPalette.Blue,
            fontSize = if (size > 60.dp) 27.sp else 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HomeScreen(
    user: FirebaseUser?,
    store: TodoTaskStore,
    onOpenProfile: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var selectedCategory by rememberSaveable { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }
    var detailTaskId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = TodoPalette.Background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            TodoBottomBar(
                selectedTab = selectedTab,
                onTab = { selectedTab = it },
                onAdd = { showAddDialog = true }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .clickable(onClickLabel = "Mở thông tin cá nhân", onClick = onOpenProfile)
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AccountAvatar(accountDisplayName(user), 38.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(homeDateLabel(), fontSize = 10.sp, color = TodoPalette.Muted)
                            Text("Chào ${accountDisplayName(user)}!", fontSize = 18.sp,
                                fontWeight = FontWeight.Bold, color = TodoPalette.Ink,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box {
                        IconButton(
                            onClick = { selectedTab = 2 },
                            modifier = Modifier.size(38.dp).background(Color.White, CircleShape)
                                .semantics { contentDescription = "Thông báo công việc" }
                        ) { TodoIcon(TodoSymbol.BELL, tint = TodoPalette.Ink, size = 20.dp) }
                        if (store.pendingCount > 0) Box(
                            Modifier.align(Alignment.TopEnd).offset((-7).dp, 6.dp)
                                .size(5.dp).background(TodoPalette.Red, CircleShape))
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    item { TodayProgressCard(store.todayCompleted, store.todayTasks.size) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TodoCategory.values().forEach { category ->
                                CategoryCard(
                                    category, store.pendingIn(category), Modifier.weight(1f)
                                ) {
                                    selectedCategory = category.name
                                    selectedTab = 1
                                }
                            }
                        }
                    }
                    item {
                        SectionTitle("Việc hôm nay", "Xem tất cả") {
                            selectedCategory = "ALL"
                            selectedTab = 1
                        }
                    }
                    if (store.todayTasks.isEmpty()) item { EmptyTasks("Hôm nay chưa có công việc. Nhấn + để thêm nhé.") }
                    items(store.todayTasks, key = { it.id }) { task ->
                        TaskRow(task, { store.toggleTask(task.id) }, { detailTaskId = task.id })
                    }
                }
                1 -> {
                    item { SectionTitle("Danh sách công việc") }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf("ALL" to "Tất cả", "STUDY" to "Học tập", "LIFE" to "Cuộc sống")
                                .forEach { (key, label) ->
                                    FilterChip(selected = selectedCategory == key,
                                        onClick = { selectedCategory = key },
                                        label = { Text(label, fontSize = 12.sp) })
                                }
                        }
                    }
                    val filtered = store.tasks.filter {
                        selectedCategory == "ALL" || it.category.name == selectedCategory
                    }.sortedWith(compareBy<TodoTask> { it.scheduledDay }.thenBy { it.time })
                    if (filtered.isEmpty()) item { EmptyTasks("Chưa có công việc trong danh mục này.") }
                    items(filtered, key = { it.id }) { task ->
                        TaskRow(task, { store.toggleTask(task.id) }, { detailTaskId = task.id }, true)
                    }
                }
                2 -> {
                    item { SectionTitle("Thông báo") }
                    item { Text("Những công việc đang chờ bạn hoàn thành.",
                        color = TodoPalette.Muted, fontSize = 12.sp) }
                    val pending = store.tasks.filter { !it.completed }
                        .sortedWith(compareBy<TodoTask> { it.scheduledDay }.thenBy { it.time })
                    if (pending.isEmpty()) item { EmptyTasks("Bạn đã hoàn thành tất cả công việc!") }
                    items(pending, key = { it.id }) { task ->
                        TaskRow(task, { store.toggleTask(task.id) }, { detailTaskId = task.id }, true)
                    }
                }
                3 -> {
                    item { SectionTitle("Cài đặt") }
                    item {
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.White,
                            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenProfile)) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                AccountAvatar(accountDisplayName(user))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(if (user == null) "Đang dùng thử" else accountDisplayName(user),
                                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Xem hồ sơ và tài khoản", fontSize = 12.sp, color = TodoPalette.Muted)
                                }
                                TodoIcon(TodoSymbol.NEXT, tint = TodoPalette.Muted, size = 17.dp)
                            }
                        }
                    }
                    item {
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                                Text("UTH Todo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(Modifier.height(5.dp))
                                Text("Quản lý công việc mỗi ngày", color = TodoPalette.Muted, fontSize = 12.sp)
                                Text(TodoRepository.appInfo.version, color = TodoPalette.Muted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) AddTaskDialog(onDismiss = { showAddDialog = false }) { title, category, time ->
        store.addTask(title, category, time)
        showAddDialog = false
    }
    val detailTask = store.tasks.firstOrNull { it.id == detailTaskId }
    if (detailTask != null) AlertDialog(
        onDismissRequest = { detailTaskId = null },
        title = { Text(detailTask.title, fontSize = 19.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Danh mục: ${detailTask.category.label}")
                Text("Thời gian: ${taskDateLabel(detailTask.scheduledDay)} · ${detailTask.time}")
                Text(if (detailTask.completed) "Đã hoàn thành" else "Chưa hoàn thành")
            }
        },
        confirmButton = {
            TextButton(onClick = { store.toggleTask(detailTask.id); detailTaskId = null }) {
                Text(if (detailTask.completed) "Đánh dấu chưa xong" else "Hoàn thành")
            }
        },
        dismissButton = { TextButton(onClick = { detailTaskId = null }) { Text("Đóng") } }
    )
}

@Composable
private fun TodayProgressCard(done: Int, total: Int) {
    val fraction = if (total == 0) 0f else done.toFloat() / total
    Surface(color = TodoPalette.Blue, shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Tiến độ hôm nay", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text("$done/$total việc", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(if (total > 0 && done == total) "Hoàn thành rồi, giỏi lắm!" else "Đã nỗ lực từng chút, cố lên!",
                    color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().padding(3.dp)) {
                    val stroke = Stroke(5.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(Color.White.copy(alpha = 0.26f), -90f, 360f, false, style = stroke)
                    if (fraction > 0f) drawArc(Color.White, -90f, 360f * fraction, false, style = stroke)
                }
                Text("${(fraction * 100).toInt()}%", color = Color.White,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CategoryCard(category: TodoCategory, pending: Int, modifier: Modifier, onClick: () -> Unit) {
    val study = category == TodoCategory.STUDY
    Surface(modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(17.dp), color = Color.White) {
        Column(Modifier.padding(15.dp)) {
            Box(Modifier.size(30.dp).background(
                if (study) TodoPalette.BlueLight else TodoPalette.Sand,
                RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                TodoIcon(if (study) TodoSymbol.STUDY else TodoSymbol.HOME,
                    tint = if (study) TodoPalette.Blue else TodoPalette.Brown, size = 18.dp)
            }
            Spacer(Modifier.height(9.dp))
            Text(category.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("$pending việc còn lại", fontSize = 11.sp, color = TodoPalette.Muted)
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TodoPalette.Ink)
        if (action != null) Text(action, fontSize = 11.sp, fontWeight = FontWeight.Medium,
            color = TodoPalette.Blue, modifier = Modifier.clickable(onClick = onAction).padding(7.dp))
    }
}

@Composable
private fun TaskRow(task: TodoTask, onToggle: () -> Unit, onDetails: () -> Unit, showDay: Boolean = false) {
    val study = task.category == TodoCategory.STUDY
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onDetails)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp)
                    .semantics { contentDescription = if (task.completed)
                        "Đánh dấu chưa hoàn thành: ${task.title}" else "Hoàn thành: ${task.title}" }
                    .clickable(role = Role.Checkbox, onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(23.dp).clip(CircleShape)
                    .background(if (task.completed) TodoPalette.Blue else Color.White)
                    .border(1.3.dp, if (task.completed) TodoPalette.Blue else Color(0xFFD5D5DB), CircleShape),
                    contentAlignment = Alignment.Center) {
                    if (task.completed) TodoIcon(TodoSymbol.CHECK, tint = Color.White, size = 14.dp)
                }
            }
            Spacer(Modifier.width(5.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = if (task.completed) Color(0xFF9C9CA3) else TodoPalette.Ink,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.category.label, fontSize = 9.sp,
                        color = if (study) TodoPalette.Blue else TodoPalette.Brown,
                        modifier = Modifier.background(if (study) TodoPalette.BlueLight else TodoPalette.Sand,
                            RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp))
                    Spacer(Modifier.width(7.dp))
                    Text((if (showDay) taskDateLabel(task.scheduledDay) + " · " else "") + task.time,
                        fontSize = 10.sp, color = TodoPalette.Muted)
                }
            }
            Spacer(Modifier.width(6.dp))
            TodoIcon(TodoSymbol.NEXT, tint = Color(0xFFBCBDC3), size = 15.dp)
        }
    }
}

@Composable
private fun EmptyTasks(message: String) {
    Text(message, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        fontSize = 13.sp, color = TodoPalette.Muted, textAlign = TextAlign.Center)
}

@Composable
private fun TodoBottomBar(selectedTab: Int, onTab: (Int) -> Unit, onAdd: () -> Unit) {
    Surface(color = Color.White, shadowElevation = 5.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp)
            .height(76.dp), verticalAlignment = Alignment.CenterVertically) {
            BottomItem("Trang chủ", TodoSymbol.HOME, selectedTab == 0, Modifier.weight(1f)) { onTab(0) }
            BottomItem("Danh sách", TodoSymbol.LIST, selectedTab == 1, Modifier.weight(1f)) { onTab(1) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                FloatingActionButton(
                    onClick = onAdd, containerColor = TodoPalette.Blue, contentColor = Color.White,
                    shape = CircleShape, modifier = Modifier.size(50.dp)
                        .semantics { contentDescription = "Thêm công việc" }
                ) { TodoIcon(TodoSymbol.PLUS, tint = Color.White, size = 25.dp) }
            }
            BottomItem("Thông báo", TodoSymbol.BELL, selectedTab == 2, Modifier.weight(1f)) { onTab(2) }
            BottomItem("Cài đặt", TodoSymbol.SETTINGS, selectedTab == 3, Modifier.weight(1f)) { onTab(3) }
        }
    }
}

@Composable
private fun BottomItem(label: String, icon: TodoSymbol, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val tint = if (selected) TodoPalette.Blue else TodoPalette.Muted
    Column(modifier.fillMaxHeight().clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        TodoIcon(icon, tint = tint, size = 21.dp)
        Spacer(Modifier.height(5.dp))
        Text(label, fontSize = 9.sp, color = tint, maxLines = 1,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String, TodoCategory, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TodoCategory.STUDY) }
    var time by remember { mutableStateOf("18:00") }
    var showError by remember { mutableStateOf(false) }
    val validTime = Regex("(?:[01]\\d|2[0-3]):[0-5]\\d").matches(time)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm việc hôm nay", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Tên công việc") }, modifier = Modifier.fillMaxWidth(),
                    isError = showError && title.isBlank(), maxLines = 3)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TodoCategory.values().forEach { item ->
                        FilterChip(selected = category == item, onClick = { category = item },
                            label = { Text(item.label) })
                    }
                }
                OutlinedTextField(value = time, onValueChange = { time = it },
                    label = { Text("Giờ (HH:mm)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), isError = showError && !validTime)
                if (showError) Text("Nhập tên việc và giờ hợp lệ, ví dụ 18:00.",
                    color = TodoPalette.Red, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank() && validTime) onAdd(title.trim(), category, time)
                else showError = true
            }) { Text("Thêm việc") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
fun ProfileScreen(
    user: FirebaseUser?,
    store: TodoTaskStore,
    isSigningOut: Boolean,
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    val name = if (user == null) "Khách" else accountDisplayName(user)
    val isGoogle = user?.providerData?.any { it.providerId == "google.com" } == true
    Scaffold(
        containerColor = TodoPalette.Background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            Box(Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 16.dp)) {
                Button(
                    onClick = onSignOut, enabled = !isSigningOut,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White,
                        contentColor = if (user == null) TodoPalette.Blue else TodoPalette.Red),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isSigningOut) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else TodoIcon(TodoSymbol.EXIT,
                        tint = if (user == null) TodoPalette.Blue else TodoPalette.Red, size = 17.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isSigningOut) "Đang chuyển..." else if (user == null)
                        "Đăng nhập với Google" else "Đăng xuất", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(58.dp), contentAlignment = Alignment.Center) {
                Text("Hồ sơ", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onBack, enabled = !isSigningOut,
                    modifier = Modifier.align(Alignment.CenterStart)) {
                    TodoIcon(TodoSymbol.BACK, size = 14.dp)
                    Text("Quay lại", fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            AccountAvatar(name, 82.dp)
            Spacer(Modifier.height(15.dp))
            Text(name, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = TodoPalette.Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(5.dp))
            Text(user?.email ?: "Đang dùng thử không cần tài khoản", fontSize = 12.sp,
                color = TodoPalette.Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(9.dp))
            Row(Modifier.background(TodoPalette.GreenLight, RoundedCornerShape(5.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (user != null) TodoIcon(TodoSymbol.CHECK, tint = TodoPalette.Green, size = 12.dp)
                Spacer(Modifier.width(3.dp))
                Text(if (isGoogle) "Đã liên kết tài khoản Google" else if (user == null)
                    "Đang dùng thử" else "Đã đăng nhập", color = TodoPalette.Green,
                    fontWeight = FontWeight.Medium, fontSize = 10.sp)
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(store.completedCount.toString(), "Việc đã xong", Modifier.weight(1f))
                StatCard(store.pendingCount.toString(), "Đang chờ", Modifier.weight(1f))
                StatCard("${store.completionPercent}%", "Hoàn thành", Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            WeeklyChart(store.weeklyCompleted())
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier) {
    Surface(modifier = modifier, color = Color.White, shape = RoundedCornerShape(13.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 16.dp)) {
            Text(value, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = TodoPalette.Ink)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 10.sp, color = TodoPalette.Muted, maxLines = 1)
        }
    }
}

@Composable
private fun WeeklyChart(counts: List<Int>) {
    val maximum = maxOf(3, counts.maxOrNull() ?: 0)
    val days = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
    Surface(shape = RoundedCornerShape(15.dp), color = Color.White) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Tuần này", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Việc hoàn thành mỗi ngày", fontSize = 9.sp, color = TodoPalette.Muted)
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                days.forEachIndexed { index, day ->
                    Column(Modifier.weight(1f).semantics {
                        contentDescription = "$day: ${counts[index]} việc hoàn thành"
                    }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.height(72.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                            Box(Modifier.width(19.dp)
                                .height(if (counts[index] == 0) 4.dp else (counts[index] * 65f / maximum).dp)
                                .background(when {
                                    counts[index] == 0 -> TodoPalette.Line
                                    index == currentWeekday() -> TodoPalette.Blue
                                    else -> Color(0xFFB7C1F6)
                                }, RoundedCornerShape(4.dp)))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(day, fontSize = 9.sp, color = TodoPalette.Muted)
                    }
                }
            }
        }
    }
}
