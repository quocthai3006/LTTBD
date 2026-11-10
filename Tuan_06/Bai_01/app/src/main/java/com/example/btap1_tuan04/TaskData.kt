package com.example.btap1_tuan04

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class TodoCategory(val label: String, val apiValue: String) {
    STUDY("Học tập", "study"), LIFE("Cuộc sống", "life");

    companion object {
        fun fromValue(value: String): TodoCategory = entries.firstOrNull {
            it.name.equals(value, true) || it.apiValue == value
        } ?: throw IllegalArgumentException("Nhóm công việc không hợp lệ.")
    }
}

enum class TodoPriority(val label: String, val apiValue: String) {
    LOW("Thấp", "low"), MEDIUM("Vừa", "medium"), HIGH("Cao", "high");

    companion object {
        fun fromValue(value: String): TodoPriority = entries.firstOrNull {
            it.apiValue == value || it.name.equals(value, true)
        } ?: MEDIUM
    }
}

data class TodoSubtask(val title: String, val done: Boolean = false)

data class TodoTask(
    val id: String,
    val title: String,
    val category: TodoCategory,
    val time: String,
    val scheduledDay: String,
    val completed: Boolean = false,
    val completedDay: String? = null,
    val note: String = "",
    val priority: TodoPriority = TodoPriority.MEDIUM,
    val remind: Boolean = false,
    val subtasks: List<TodoSubtask> = emptyList()
)

data class TaskDraft(
    val title: String,
    val note: String,
    val category: TodoCategory,
    val scheduledDay: String,
    val time: String,
    val priority: TodoPriority,
    val remind: Boolean
) {
    fun validate() {
        require(title.isNotBlank()) { "Vui lòng nhập tên công việc." }
        require(title.trim().length <= 200) { "Tên công việc tối đa 200 ký tự." }
        require(note.length <= 5000) { "Ghi chú tối đa 5000 ký tự." }
        require(Regex("(?:[01]\\d|2[0-3]):[0-5]\\d").matches(time)) { "Giờ không hợp lệ." }
        parseDay(scheduledDay)
    }

    fun applyTo(task: TodoTask): TodoTask = task.copy(
        title = title.trim(), note = note.trim(), category = category,
        scheduledDay = scheduledDay, time = time, priority = priority, remind = remind
    )

    fun toApiJson(): JSONObject = JSONObject().apply {
        put("title", title.trim()); put("note", note.trim()); put("category", category.apiValue)
        put("date", dayKeyToApiDate(scheduledDay)); put("time", time)
        put("priority", priority.apiValue); put("remind", remind)
    }
}

internal fun subtasksJson(items: List<TodoSubtask>): JSONArray = JSONArray().apply {
    items.forEach { item -> put(JSONObject().put("title", item.title).put("done", item.done)) }
}

internal fun readSubtasks(array: JSONArray?): List<TodoSubtask> =
    if (array == null) emptyList() else List(array.length()) { index ->
        val item = array.getJSONObject(index)
        TodoSubtask(item.getString("title"), item.optBoolean("done", false))
    }

internal fun taskFromApi(json: JSONObject): TodoTask = TodoTask(
    id = json.getLong("id").toString(),
    title = json.getString("title"),
    category = TodoCategory.fromValue(json.getString("category")),
    time = json.getString("time"),
    scheduledDay = apiDateToDayKey(json.getString("date")),
    completed = json.getBoolean("done"),
    // API không có ngày hoàn thành. Không suy đoán ngày đó từ hạn chót.
    note = json.optString("note", ""),
    priority = TodoPriority.fromValue(json.optString("priority", "medium")),
    remind = json.optBoolean("remind", false),
    subtasks = readSubtasks(json.optJSONArray("subtasks"))
)

internal fun taskToCache(task: TodoTask): JSONObject = JSONObject().apply {
    put("id", task.id); put("title", task.title); put("category", task.category.name)
    put("time", task.time); put("scheduledDay", task.scheduledDay)
    put("completed", task.completed); put("completedDay", task.completedDay ?: JSONObject.NULL)
    put("note", task.note); put("priority", task.priority.apiValue); put("remind", task.remind)
    put("subtasks", subtasksJson(task.subtasks))
}

internal fun taskFromCache(json: JSONObject): TodoTask = TodoTask(
    id = json.getString("id"), title = json.getString("title"),
    category = TodoCategory.fromValue(json.getString("category")), time = json.getString("time"),
    scheduledDay = json.getString("scheduledDay"), completed = json.getBoolean("completed"),
    completedDay = if (json.isNull("completedDay")) null else json.getString("completedDay"),
    note = json.optString("note", ""),
    priority = TodoPriority.fromValue(json.optString("priority", "medium")),
    remind = json.optBoolean("remind", false), subtasks = readSubtasks(json.optJSONArray("subtasks"))
)

/** Mock của lớp trả mẫu cố định. Giữ thay đổi trên máy sau khi HTTP thành công. */
internal fun mergeTeachingSnapshot(
    serverTasks: List<TodoTask>, localTasks: List<TodoTask>,
    modifiedIds: Set<String>, deletedIds: Set<String>
): List<TodoTask> {
    val localById = localTasks.associateBy { it.id }
    val merged = linkedMapOf<String, TodoTask>()
    serverTasks.filterNot { it.id in deletedIds }.forEach { task ->
        merged[task.id] = if (task.id in modifiedIds) localById[task.id] ?: task else task
    }
    localTasks.filter { it.id in modifiedIds && it.id !in deletedIds }.forEach { task ->
        merged[task.id] = task
    }
    return merged.values.toList()
}

private val vietnamZone: TimeZone get() = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
internal fun vietnamCalendar(date: Date = Date()): Calendar = Calendar.getInstance(vietnamZone).apply { time = date }
private fun formatter(pattern: String, locale: Locale = Locale.US) =
    SimpleDateFormat(pattern, locale).apply { timeZone = vietnamZone; isLenient = false }

internal fun parseDay(key: String): Date {
    require(Regex("\\d{8}").matches(key)) { "Ngày không hợp lệ." }
    return requireNotNull(formatter("yyyyMMdd").parse(key)) { "Ngày không hợp lệ." }
}

internal fun dayKey(date: Date = Date()): String = formatter("yyyyMMdd").format(date)
internal fun dayKeyAfter(offset: Int, now: Date = Date()): String = dayKey(
    vietnamCalendar(now).apply { add(Calendar.DAY_OF_MONTH, offset) }.time
)
internal fun apiDateToDayKey(date: String): String {
    require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(date)) { "Ngày API không hợp lệ." }
    return dayKey(requireNotNull(formatter("yyyy-MM-dd").parse(date)))
}
internal fun dayKeyToApiDate(key: String): String = formatter("yyyy-MM-dd").format(parseDay(key))
internal fun currentWeekday(now: Date = Date()): Int =
    (vietnamCalendar(now).get(Calendar.DAY_OF_WEEK) + 5) % 7
internal fun endOfWeekKey(now: Date = Date()): String = dayKeyAfter(6 - currentWeekday(now), now)
internal fun homeDateLabel(now: Date = Date()): String =
    formatter("EEEE, d 'tháng' M", Locale("vi", "VN")).format(now)
        .replaceFirstChar { it.titlecase(Locale("vi", "VN")) }

internal fun taskDateLabel(key: String, now: Date = Date()): String = when {
    key == dayKey(now) -> "Hôm nay"
    key == dayKeyAfter(1, now) -> "Ngày mai"
    key > dayKey(now) && key <= endOfWeekKey(now) -> "Tuần này"
    else -> runCatching { formatter("dd/MM").format(parseDay(key)) }.getOrDefault(key)
}

internal fun taskDeadlineMillis(task: TodoTask): Long =
    requireNotNull(formatter("yyyyMMdd HH:mm").parse("${task.scheduledDay} ${task.time}")).time

internal fun completedThisWeek(tasks: List<TodoTask>, now: Date = Date()): List<Int> {
    val monday = vietnamCalendar(now).apply { add(Calendar.DAY_OF_MONTH, -currentWeekday(now)) }
    return List(7) { index ->
        val key = dayKey((monday.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, index) }.time)
        tasks.count { it.completed && it.completedDay == key }
    }
}
