package com.example.btap1_tuan04

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** apiDebug dùng HTTP mock + cache; firebaseDebug giữ dữ liệu Realtime Database. */
class TodoTaskStore(context: Context, private val accountId: String?) {
    private val appContext = context.applicationContext
    private val isApi = BuildConfig.USE_TEACHING_API
    private val preferences = appContext.getSharedPreferences(
        if (isApi) "uth_todo_api_v1" else "uth_todo_tasks_v2", Context.MODE_PRIVATE)
    private val storageKey = "tasks_${accountId ?: "guest"}"
    private val changesKey = "changes_${accountId ?: "guest"}"
    private val deletionsKey = "deletions_${accountId ?: "guest"}"
    private var modifiedIds = preferences.getStringSet(changesKey, emptySet()).orEmpty().toSet()
    private var deletedIds = preferences.getStringSet(deletionsKey, emptySet()).orEmpty().toSet()
    private val api = TodoApi()
    private val cloud = if (isApi) null else accountId?.let { FirebaseTaskRemote(it) }
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var started = false
    private var generation = 0
    private var detailRequest = 0
    private var detailJob: Job? = null
    val noticeScope = "${if (isApi) "api" else "firebase"}_${accountId ?: "guest"}"
    val notices = TaskNotices(appContext, noticeScope)
    private val reminders = TaskReminderManager(appContext, noticeScope)

    var tasks: List<TodoTask> by mutableStateOf(readCache() ?: if (isApi) emptyList() else sampleTasks())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isReady by mutableStateOf(false)
        private set
    var loadError: String? by mutableStateOf(null)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var actionError: String? by mutableStateOf(null)
        private set
    var detailLoading by mutableStateOf(false)
        private set
    var detailError: String? by mutableStateOf(null)
        private set

    val todayTasks get() = tasks.filter { it.scheduledDay == dayKey() }.sortedBy { it.time }
    val todayCompleted get() = todayTasks.count { it.completed }
    val completedCount get() = tasks.count { it.completed }
    val pendingCount get() = tasks.count { !it.completed }
    val completionPercent get() = if (tasks.isEmpty()) 0 else completedCount * 100 / tasks.size
    fun pendingIn(category: TodoCategory) = tasks.count { it.category == category && !it.completed }
    fun weeklyCompleted() = completedThisWeek(tasks)
    fun clearActionError() { actionError = null }

    fun startSync() {
        if (started) return
        started = true
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        notices.start()
        reminders.activate()
        refresh()
    }

    fun stopSync() {
        started = false
        generation++
        detailRequest++
        cloud?.stop()
        scope.cancel()
        notices.stop()
        // Không hủy alarm khi Activity đóng: nhắc việc phải tiếp tục chạy.
    }

    fun deactivateReminders() = reminders.deactivate()

    fun cancelDetailLoad() {
        detailRequest++
        detailJob?.cancel()
        detailJob = null
        detailLoading = false
    }

    fun refresh() {
        if (!started || isLoading || isSaving) return
        val version = ++generation
        loadError = null
        isLoading = true
        if (isApi) scope.launch {
            try {
                val incoming = api.getTasks()
                if (version != generation) return@launch
                tasks = mergeTeachingSnapshot(incoming, tasks, modifiedIds, deletedIds)
                isReady = true
                persistAndSchedule()
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { if (version == generation) loadError = readable(error) }
            finally { if (version == generation) isLoading = false }
        } else if (cloud == null) {
            isReady = true
            isLoading = false
            persistAndSchedule()
        } else cloud.start(tasks, onTasks = { incoming ->
            if (version == generation && started) {
                tasks = incoming; isReady = true; isLoading = false; loadError = null
                persistAndSchedule()
            }
        }, onError = { error ->
            if (version == generation && started) {
                isReady = false; isLoading = false; loadError = readable(error)
            }
        })
    }

    fun loadTask(id: String, onNotFound: () -> Unit) {
        val request = ++detailRequest
        detailJob?.cancel()
        detailLoading = true; detailError = null; actionError = null
        detailJob = scope.launch {
            try {
                val incoming = when {
                    isApi -> api.getTask(id)
                    cloud != null -> cloud.getTask(id)
                    else -> tasks.firstOrNull { it.id == id }
                }
                if (request != detailRequest || !started) return@launch
                val current = tasks.firstOrNull { it.id == id }
                if (incoming == null || (isApi && incoming.id != id && current == null)) {
                    toast("Không tìm thấy công việc."); onNotFound()
                } else if (incoming.id == id && (!isApi || id !in modifiedIds)) {
                    upsert(incoming); persistAndSchedule()
                }
                // GET mock có thể trả cùng một fixture cho mọi id: giữ đúng việc đang chọn.
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                if (request == detailRequest && started) {
                    if (error is TaskApiException && error.statusCode == 404) {
                        toast(readable(error)); onNotFound()
                    } else detailError = readable(error)
                }
            } finally { if (request == detailRequest) { detailLoading = false; detailJob = null } }
        }
    }

    fun saveTask(draft: TaskDraft, editingId: String?, onSaved: (String) -> Unit) {
        mutate(if (editingId == null) "Đã thêm công việc" else "Đã lưu thay đổi") {
            draft.validate()
            if (editingId == null) {
                val task = if (isApi) {
                    val fixture = api.addTask(draft)
                    // POST mock luôn có id 7. Dùng id riêng nếu thêm nhiều lần.
                    val id = if (tasks.none { it.id == fixture.id } && fixture.id !in deletedIds) fixture.id
                    else ((tasks.mapNotNull { it.id.toLongOrNull() } +
                        deletedIds.mapNotNull { it.toLongOrNull() } + (fixture.id.toLongOrNull() ?: 0L))
                        .maxOrNull()!! + 1L).toString()
                    draft.applyTo(fixture).copy(id = id, completed = false,
                        completedDay = null, subtasks = emptyList())
                } else draft.applyTo(TodoTask(UUID.randomUUID().toString(), "", draft.category,
                    draft.time, draft.scheduledDay))
                if (!isApi && cloud != null) cloud.add(task)
                else { upsert(task); if (isApi) modifiedIds = modifiedIds + task.id; persistAndSchedule() }
                onSaved(task.id)
            } else {
                val old = requireNotNull(tasks.firstOrNull { it.id == editingId }) { "Không tìm thấy công việc." }
                val changed = draft.applyTo(old)
                update(changed, draft.toApiJson(), mapOf(
                    "title" to changed.title, "note" to changed.note, "category" to changed.category.name,
                    "scheduledDay" to changed.scheduledDay, "time" to changed.time,
                    "priority" to changed.priority.apiValue, "remind" to changed.remind))
                onSaved(editingId)
            }
        }
    }

    fun toggleTask(id: String) {
        val old = tasks.firstOrNull { it.id == id } ?: return
        val done = !old.completed
        mutate(if (done) "Tuyệt, đã xong một việc!" else "Đã đánh dấu chưa xong") {
            val changed = old.copy(completed = done, completedDay = if (done) dayKey() else null)
            update(changed, JSONObject().put("done", done), mapOf(
                "completed" to done, "completedDay" to changed.completedDay))
            if (done) notices.add("done-${old.id}-${System.currentTimeMillis()}", old.id,
                "Làm tốt lắm!", "Bạn đã hoàn thành ${old.title}.")
        }
    }

    fun saveSubtasks(id: String, items: List<TodoSubtask>, onSaved: () -> Unit = {}) {
        mutate("Đã cập nhật việc nhỏ") {
            require(items.all { it.title.isNotBlank() && it.title.length <= 200 }) { "Tên việc nhỏ không hợp lệ." }
            val old = requireNotNull(tasks.firstOrNull { it.id == id }) { "Không tìm thấy công việc." }
            val clean = items.map { it.copy(title = it.title.trim()) }
            update(old.copy(subtasks = clean), JSONObject().put("subtasks", subtasksJson(clean)),
                mapOf("subtasks" to clean.map { mapOf("title" to it.title, "done" to it.done) }))
            onSaved()
        }
    }

    fun deleteTask(id: String, onDeleted: () -> Unit) {
        mutate("Đã xóa công việc") {
            if (isApi) api.deleteTask(id) else cloud?.delete(id)
            if (isApi || cloud == null) {
                tasks = tasks.filterNot { it.id == id }
                if (isApi) { deletedIds = deletedIds + id; modifiedIds = modifiedIds - id }
                persistAndSchedule()
            }
            onDeleted()
        }
    }

    private suspend fun update(task: TodoTask, apiChanges: JSONObject, firebaseChanges: Map<String, Any?>) {
        when {
            isApi -> {
                api.patchTask(task.id, apiChanges)
                // Server mock không lưu body. Chỉ áp dụng thay đổi sau khi HTTP thành công.
                upsert(task); modifiedIds = modifiedIds + task.id; persistAndSchedule()
            }
            cloud != null -> cloud.update(task.id, firebaseChanges)
            else -> { upsert(task); persistAndSchedule() }
        }
    }

    private fun mutate(successMessage: String, block: suspend () -> Unit) {
        if (isSaving) return
        if (!isReady || isLoading) { actionError = "Dữ liệu chưa tải xong. Vui lòng thử lại."; toast(actionError!!); return }
        val version = generation
        isSaving = true; actionError = null
        scope.launch {
            try {
                block()
                if (started && version == generation) toast(successMessage)
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                if (started && version == generation) { actionError = readable(error); toast(actionError!!) }
            } finally { isSaving = false }
        }
    }

    private fun upsert(task: TodoTask) {
        tasks = if (tasks.any { it.id == task.id }) tasks.map { if (it.id == task.id) task else it }
        else tasks + task
    }

    private fun persistAndSchedule() {
        val array = JSONArray().apply { tasks.forEach { put(taskToCache(it)) } }
        preferences.edit().putString(storageKey, array.toString())
            .putStringSet(changesKey, modifiedIds).putStringSet(deletionsKey, deletedIds).apply()
        reminders.sync(tasks)
        tasks.filter { !it.completed }.forEach { task ->
            if (runCatching { taskDeadlineMillis(task) < System.currentTimeMillis() }.getOrDefault(false))
                notices.add("overdue-${task.id}-${task.scheduledDay}-${task.time}", task.id,
                    "Đã quá hạn", "${task.title} chưa hoàn thành.")
        }
    }

    private fun readCache(): List<TodoTask>? {
        val old = appContext.getSharedPreferences("uth_todo_tasks_v1", Context.MODE_PRIVATE)
        val text = preferences.getString(storageKey, null)
            ?: if (!isApi) old.getString(storageKey, null) else null
        return text?.let { runCatching {
            val array = JSONArray(it)
            List(array.length()) { index -> taskFromCache(array.getJSONObject(index)) }
        }.getOrNull() }
    }

    private fun toast(message: String) = Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
    private fun readable(error: Exception) = when (error) {
        is java.net.SocketTimeoutException -> "Kết nối quá lâu. Kiểm tra mạng và thử lại."
        is java.net.UnknownHostException -> "Không kết nối được máy chủ. Kiểm tra mạng và thử lại."
        else -> error.message?.takeIf { it.isNotBlank() } ?: "Có lỗi xảy ra. Vui lòng thử lại."
    }

    private fun sampleTasks() = listOf(
        TodoTask("sample-1", "Tập thể dục 30 phút", TodoCategory.LIFE, "06:30", dayKey(), true, dayKey()),
        TodoTask("sample-2", "Nộp bài tập Lập trình di động", TodoCategory.STUDY, "09:00", dayKey(), true, dayKey()),
        TodoTask("sample-3", "Ôn chương 3 – Cấu trúc dữ liệu", TodoCategory.STUDY, "14:00", dayKey(),
            note = "Tập trung phần cây nhị phân và bảng băm.", remind = true,
            subtasks = listOf(TodoSubtask("Đọc lại slide bài giảng", true), TodoSubtask("Làm 10 bài tập cuối chương"))),
        TodoTask("sample-4", "Đi chợ mua đồ cho tuần", TodoCategory.LIFE, "17:30", dayKey()),
        TodoTask("sample-5", "Hoàn thành bài Firebase", TodoCategory.STUDY, "08:00", dayKeyAfter(1), priority = TodoPriority.HIGH),
        TodoTask("sample-6", "Sắp xếp góc học tập", TodoCategory.LIFE, "19:00", dayKeyAfter(2))
    )
}
