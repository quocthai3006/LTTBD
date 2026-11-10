package com.example.btap1_tuan04

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject

data class TaskNotice(
    val id: String, val taskId: String, val title: String, val body: String,
    val timestamp: Long = System.currentTimeMillis(), val read: Boolean = false
)

class TaskNotices(context: Context, scope: String) {
    private val preferences = context.getSharedPreferences("uth_todo_notices", Context.MODE_PRIVATE)
    private val key = "notices_$scope"
    var items by mutableStateOf(load())
        private set
    val unreadCount get() = items.count { !it.read }
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
        if (changed == key) items = load()
    }
    fun start() { items = load(); preferences.registerOnSharedPreferenceChangeListener(listener) }
    fun stop() = preferences.unregisterOnSharedPreferenceChangeListener(listener)
    fun add(id: String, taskId: String, title: String, body: String) {
        val latest = load()
        if (latest.any { it.id == id }) return
        items = (listOf(TaskNotice(id, taskId, title, body)) + latest).take(100)
        save()
    }
    fun markAllRead() { items = items.map { it.copy(read = true) }; save() }
    fun markRead(id: String) { items = items.map { if (it.id == id) it.copy(read = true) else it }; save() }
    private fun save() {
        val array = JSONArray().apply {
            items.forEach { notice -> put(JSONObject().apply {
                put("id", notice.id); put("taskId", notice.taskId); put("title", notice.title)
                put("body", notice.body); put("timestamp", notice.timestamp); put("read", notice.read)
            }) }
        }
        preferences.edit().putString(key, array.toString()).apply()
    }
    private fun load(): List<TaskNotice> = runCatching {
        val array = JSONArray(preferences.getString(key, "[]"))
        List(array.length()) { index -> array.getJSONObject(index).let {
            TaskNotice(it.getString("id"), it.getString("taskId"), it.getString("title"),
                it.getString("body"), it.getLong("timestamp"), it.optBoolean("read", false))
        } }
    }.getOrDefault(emptyList())
}

internal fun notificationsAllowed(context: Context): Boolean =
    (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
        Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        NotificationManagerCompat.from(context).areNotificationsEnabled()

internal class TaskReminderManager(private val context: Context, private val scope: String) {
    private val preferences = context.getSharedPreferences("uth_todo_reminders", Context.MODE_PRIVATE)
    private val alarm = requireNotNull(context.getSystemService(AlarmManager::class.java))

    fun activate() {
        val previous = preferences.getString("active_scope", null)
        if (previous != null && previous != scope) cancelScope(previous)
        preferences.edit().putString("active_scope", scope).apply()
    }
    fun deactivate() {
        cancelScope(scope)
        if (preferences.getString("active_scope", null) == scope)
            preferences.edit().remove("active_scope").apply()
        context.getSystemService(NotificationManager::class.java)?.cancelAll()
    }
    fun sync(tasks: List<TodoTask>) {
        if (preferences.getString("active_scope", null) != scope) return
        cancelScope(scope)
        val scheduled = mutableSetOf<String>()
        tasks.filter { it.remind && !it.completed }.forEach { task ->
            val at = runCatching { taskDeadlineMillis(task) - 15 * 60_000L }.getOrNull() ?: return@forEach
            if (at <= System.currentTimeMillis()) return@forEach
            val intent = intentFor(scope, task.id).putExtra("scope", scope)
                .putExtra("taskId", task.id).putExtra("title", task.title)
            val pending = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            // Alarm inexact: Android có thể trì hoãn khi tiết kiệm pin. Không xin quyền alarm chính xác.
            runCatching { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending) }
                .onSuccess { scheduled += task.id }
        }
        preferences.edit().putStringSet("scheduled_$scope", scheduled).apply()
    }
    private fun cancelScope(target: String) {
        preferences.getStringSet("scheduled_$target", emptySet()).orEmpty().forEach { id ->
            PendingIntent.getBroadcast(context, 0, intentFor(target, id),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
                alarm.cancel(it); it.cancel()
            }
        }
        preferences.edit().remove("scheduled_$target").apply()
    }
    private fun intentFor(target: String, id: String) = Intent(context, TaskReminderReceiver::class.java).apply {
        data = Uri.Builder().scheme("uth-todo").authority("reminder").appendPath(target).appendPath(id).build()
    }
}

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scope = intent.getStringExtra("scope") ?: return
        val active = context.getSharedPreferences("uth_todo_reminders", Context.MODE_PRIVATE)
            .getString("active_scope", null)
        if (scope != active) return
        val taskId = intent.getStringExtra("taskId") ?: return
        val title = intent.getStringExtra("title") ?: return
        TaskNotices(context, scope).add("reminder-$taskId-${System.currentTimeMillis()}",
            taskId, "Nhắc việc", "$title đang chờ bạn hoàn thành.")
        if (!notificationsAllowed(context)) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(
            "task_reminders", "Nhắc công việc", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, "task_reminders")
            .setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle("Sắp đến hạn")
            .setContentText(title).setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setContentIntent(open).setAutoCancel(true).build()
        try { manager.notify("$scope/$taskId".hashCode(), notification) } catch (_: SecurityException) { }
    }
}
