package com.example.btap1_tuan04

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class TodoCategory(val label: String) {
    STUDY("Học tập"), LIFE("Cuộc sống")
}

data class TodoTask(
    val id: String,
    val title: String,
    val category: TodoCategory,
    val time: String,
    val scheduledDay: String,
    val completed: Boolean = false,
    val completedDay: String? = null
)

// Dữ liệu công việc được đóng gói trong lớp này, lưu riêng cho từng tài khoản.
// Đây là lưu trữ trên thiết bị bằng SharedPreferences, chưa dùng Firestore.
class TodoTaskStore(context: Context, accountId: String?) {
    private val preferences = context.applicationContext
        .getSharedPreferences("uth_todo_tasks_v1", Context.MODE_PRIVATE)
    private val storageKey = "tasks_${accountId ?: "guest"}"

    var tasks: List<TodoTask> by mutableStateOf(readTasks() ?: sampleTasks())
        private set

    init {
        persist()
    }

    val todayTasks: List<TodoTask>
        get() = tasks.filter { it.scheduledDay == dayKey() }.sortedBy { it.time }

    val todayCompleted: Int
        get() = todayTasks.count { it.completed }

    val completedCount: Int
        get() = tasks.count { it.completed }

    val pendingCount: Int
        get() = tasks.size - completedCount

    val completionPercent: Int
        get() = if (tasks.isEmpty()) 0 else completedCount * 100 / tasks.size

    fun pendingIn(category: TodoCategory): Int =
        tasks.count { it.category == category && !it.completed }

    fun toggleTask(id: String) {
        tasks = tasks.map { task ->
            if (task.id == id) {
                val newCompleted = !task.completed
                task.copy(
                    completed = newCompleted,
                    completedDay = if (newCompleted) dayKey() else null
                )
            } else task
        }
        persist()
    }

    fun addTask(title: String, category: TodoCategory, time: String) {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty())
        require(Regex("(?:[01]\\d|2[0-3]):[0-5]\\d").matches(time))
        tasks = tasks + TodoTask(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            category = category,
            time = time,
            scheduledDay = dayKey()
        )
        persist()
    }

    // Chỉ đếm công việc đã hoàn thành trong tuần hiện tại, từ thứ Hai đến CN.
    fun weeklyCompleted(): List<Int> {
        val monday = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            val offset = (get(Calendar.DAY_OF_WEEK) + 5) % 7
            add(Calendar.DAY_OF_MONTH, -offset)
        }
        return List(7) { index ->
            val date = (monday.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, index)
            }
            val key = dayKey(date.time)
            tasks.count { it.completed && it.completedDay == key }
        }
    }

    private fun sampleTasks(): List<TodoTask> = listOf(
        TodoTask("sample-1", "Tập thể dục 30 phút", TodoCategory.LIFE,
            "06:30", dayKey(), true, dayKey()),
        TodoTask("sample-2", "Nộp bài tập Lập trình di động", TodoCategory.STUDY,
            "09:00", dayKey(), true, dayKey()),
        TodoTask("sample-3", "Ôn chương 3 – Cấu trúc dữ liệu", TodoCategory.STUDY,
            "14:00", dayKey()),
        TodoTask("sample-4", "Đi chợ mua đồ cho tuần", TodoCategory.LIFE,
            "17:30", dayKey()),
        TodoTask("sample-5", "Hoàn thành bài Firebase", TodoCategory.STUDY,
            "08:00", dayKeyAfter(1)),
        TodoTask("sample-6", "Sắp xếp góc học tập", TodoCategory.LIFE,
            "19:00", dayKeyAfter(2))
    )

    private fun persist() {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("category", task.category.name)
                put("time", task.time)
                put("scheduledDay", task.scheduledDay)
                put("completed", task.completed)
                put("completedDay", task.completedDay ?: JSONObject.NULL)
            })
        }
        preferences.edit().putString(storageKey, array.toString()).apply()
    }

    private fun readTasks(): List<TodoTask>? {
        val saved = preferences.getString(storageKey, null) ?: return null
        return runCatching {
            val array = JSONArray(saved)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                TodoTask(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    category = TodoCategory.valueOf(item.getString("category")),
                    time = item.getString("time"),
                    scheduledDay = item.getString("scheduledDay"),
                    completed = item.getBoolean("completed"),
                    completedDay = if (item.isNull("completedDay")) null
                        else item.getString("completedDay")
                )
            }
        }.getOrNull()
    }
}

internal fun dayKey(date: Date = Date()): String =
    SimpleDateFormat("yyyyMMdd", Locale.US).format(date)

private fun dayKeyAfter(offset: Int): String = dayKey(
    Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, offset) }.time
)

internal fun currentWeekday(): Int =
    (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7

internal fun homeDateLabel(): String {
    val text = SimpleDateFormat("EEEE, d 'tháng' M", Locale("vi", "VN"))
        .format(Date())
    return text.replaceFirstChar { it.titlecase(Locale("vi", "VN")) }
}

internal fun taskDateLabel(key: String): String = when (key) {
    dayKey() -> "Hôm nay"
    dayKeyAfter(1) -> "Ngày mai"
    else -> runCatching {
        val date = SimpleDateFormat("yyyyMMdd", Locale.US).parse(key)
        if (date == null) key else SimpleDateFormat("dd/MM", Locale.US).format(date)
    }.getOrDefault(key)
}
