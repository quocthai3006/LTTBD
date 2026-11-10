package com.example.btap1_tuan04

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
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

// Bài 02: công việc của tài khoản Google được đồng bộ với Realtime Database.
// Khách dùng thử vẫn lưu trên máy. Cache v2 tách riêng dữ liệu cũ của bài 1.
class TodoTaskStore(context: Context, private val accountId: String?) {
    private val appContext = context.applicationContext
    private val preferences = appContext
        .getSharedPreferences("uth_todo_tasks_v2", Context.MODE_PRIVATE)
    private val legacyPreferences = appContext
        .getSharedPreferences("uth_todo_tasks_v1", Context.MODE_PRIVATE)
    private val storageKey = "tasks_${accountId ?: "guest"}"

    var tasks: List<TodoTask> by mutableStateOf(
        readTasks(preferences) ?: readTasks(legacyPreferences) ?: sampleTasks()
    )
        private set

    private var cloudTasks: DatabaseReference? = null
    private var cloudListener: ValueEventListener? = null
    private var syncStarted = false
    private var cloudReady = false
    @Volatile private var syncVersion = 0

    init {
        persist()
    }

    // Gọi khi store đi vào giao diện; dừng bằng stopSync() khi rời phiên này.
    fun startSync() {
        val uid = accountId ?: return
        if (syncStarted) return
        if (FirebaseAuth.getInstance().currentUser?.uid != uid) return

        syncStarted = true
        cloudReady = false
        val version = ++syncVersion
        val userReference = FirebaseDatabase.getInstance(DATABASE_URL)
            .reference.child("users").child(uid)
        val initialTasks = tasks.associate { it.id to it.cloudValue() }

        // Transaction chỉ đưa dữ liệu cũ lên lần đầu. Nếu tài khoản đã có
        // dữ liệu cloud thì giữ dữ liệu đó, kể cả danh sách công việc trống.
        userReference.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                if (version != syncVersion ||
                    FirebaseAuth.getInstance().currentUser?.uid != uid) {
                    return Transaction.abort()
                }
                if (currentData.child("tasksInitialized").value == true) {
                    return Transaction.abort()
                }
                if (!currentData.hasChild("tasks")) {
                    currentData.child("tasks").value = initialTasks
                }
                currentData.child("tasksInitialized").value = true
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?
            ) {
                if (!isCurrentSession(version)) return
                if (error != null) {
                    syncStarted = false
                    reportError("Không thể đồng bộ công việc. Vui lòng đăng nhập lại.",
                        error.toException())
                    return
                }
                listenToTasks(userReference.child("tasks"), version)
            }
        }, false)
    }

    fun stopSync() {
        ++syncVersion
        syncStarted = false
        cloudReady = false
        cloudListener?.let { listener -> cloudTasks?.removeEventListener(listener) }
        cloudListener = null
        cloudTasks = null
    }

    private fun isCurrentSession(version: Int): Boolean =
        syncStarted && version == syncVersion &&
                FirebaseAuth.getInstance().currentUser?.uid == accountId

    private fun listenToTasks(reference: DatabaseReference, version: Int) {
        if (!isCurrentSession(version)) return
        cloudTasks = reference
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isCurrentSession(version)) return
                tasks = snapshot.children.mapNotNull { readCloudTask(it) }
                    .sortedWith(compareBy<TodoTask> { it.scheduledDay }
                        .thenBy { it.time }.thenBy { it.id })
                persist()
                if (!cloudReady) {
                    Toast.makeText(appContext, "Đã đồng bộ công việc với Firebase",
                        Toast.LENGTH_SHORT).show()
                }
                cloudReady = true
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isCurrentSession(version)) return
                cloudReady = false
                reportError("Không thể tải công việc. Kiểm tra kết nối và đăng nhập lại.",
                    error.toException())
            }
        }
        cloudListener = listener
        reference.addValueEventListener(listener)
    }

    private fun readCloudTask(snapshot: DataSnapshot): TodoTask? {
        val id = snapshot.key ?: return null
        val title = snapshot.child("title").value as? String ?: return null
        val categoryName = snapshot.child("category").value as? String ?: return null
        val category = TodoCategory.values().firstOrNull { it.name == categoryName }
            ?: return null
        val time = snapshot.child("time").value as? String ?: return null
        val scheduledDay = snapshot.child("scheduledDay").value as? String ?: return null
        val completed = snapshot.child("completed").value as? Boolean ?: return null
        val completedDay = snapshot.child("completedDay").value as? String
        return TodoTask(id, title, category, time, scheduledDay, completed, completedDay)
    }

    // Dùng Map để tên trường Firebase luôn trùng với tên trong Rules/Console.
    private fun TodoTask.cloudValue(): Map<String, Any> = mutableMapOf<String, Any>(
        "id" to id, "title" to title, "category" to category.name,
        "time" to time, "scheduledDay" to scheduledDay, "completed" to completed
    ).apply {
        if (completed) put("completedDay", completedDay ?: dayKey())
    }

    private fun readyToWrite(): Boolean {
        if (accountId == null) return true
        if (cloudReady && cloudTasks != null &&
            FirebaseAuth.getInstance().currentUser?.uid == accountId) return true
        Toast.makeText(appContext, "Đang kết nối dữ liệu. Vui lòng thử lại sau.",
            Toast.LENGTH_SHORT).show()
        return false
    }

    private fun reportError(message: String, error: Exception) {
        Log.e("UTH_DATABASE", message, error)
        Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
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
        if (!readyToWrite()) return
        val task = tasks.firstOrNull { it.id == id } ?: return
        val newCompleted = !task.completed
        val changed = task.copy(
            completed = newCompleted,
            completedDay = if (newCompleted) dayKey() else null
        )
        if (accountId == null) {
            tasks = tasks.map { if (it.id == id) changed else it }
            persist()
        } else {
            // Chỉ cập nhật hai trường trạng thái, không ghi đè cả danh sách.
            val version = syncVersion
            val changes: Map<String, Any?> = mapOf(
                "completed" to changed.completed,
                "completedDay" to changed.completedDay
            )
            cloudTasks?.child(id)?.updateChildren(changes)?.addOnFailureListener { error ->
                if (isCurrentSession(version)) {
                    reportError("Chưa lưu được thay đổi công việc.", error)
                }
            }
        }
    }

    fun addTask(title: String, category: TodoCategory, time: String) {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty())
        require(Regex("(?:[01]\\d|2[0-3]):[0-5]\\d").matches(time))
        if (!readyToWrite()) return
        val task = TodoTask(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            category = category,
            time = time,
            scheduledDay = dayKey()
        )
        if (accountId == null) {
            tasks = tasks + task
            persist()
        } else {
            val version = syncVersion
            cloudTasks?.child(task.id)?.setValue(task.cloudValue())
                ?.addOnFailureListener { error ->
                    if (isCurrentSession(version)) {
                        reportError("Chưa lưu được công việc mới.", error)
                    }
                }
        }
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

    private fun readTasks(source: SharedPreferences): List<TodoTask>? {
        val saved = source.getString(storageKey, null) ?: return null
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

    private companion object {
        const val DATABASE_URL =
            "https://uthtodo-70f71-default-rtdb.asia-southeast1.firebasedatabase.app"
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