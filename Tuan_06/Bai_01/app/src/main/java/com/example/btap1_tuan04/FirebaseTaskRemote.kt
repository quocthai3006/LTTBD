package com.example.btap1_tuan04

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Giữ users/{uid}/tasks và tasksInitialized của bài trước. */
internal class FirebaseTaskRemote(private val uid: String) {
    private val user = FirebaseDatabase.getInstance(
        "https://uthtodo-70f71-default-rtdb.asia-southeast1.firebasedatabase.app")
        .reference.child("users").child(uid)
    private val tasks = user.child("tasks")
    private var listener: ValueEventListener? = null
    private var version = 0

    fun start(initial: List<TodoTask>, onTasks: (List<TodoTask>) -> Unit, onError: (Exception) -> Unit) {
        stop()
        val expected = version
        if (!sameAccount()) { onError(IllegalStateException("Vui lòng đăng nhập lại để đồng bộ.")); return }
        user.runTransaction(object : Transaction.Handler {
            override fun doTransaction(current: MutableData): Transaction.Result {
                if (expected != version || !sameAccount()) return Transaction.abort()
                if (current.child("tasksInitialized").value == true) return Transaction.abort()
                if (!current.hasChild("tasks")) current.child("tasks").value = initial.associate { it.id to it.cloudValue() }
                current.child("tasksInitialized").value = true
                return Transaction.success(current)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (expected != version || !sameAccount()) return
                if (error != null) { onError(error.toException()); return }
                val observer = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (expected == version && sameAccount()) onTasks(snapshot.children.mapNotNull(::readTask)
                            .sortedWith(compareBy<TodoTask> { it.scheduledDay }.thenBy { it.time }.thenBy { it.id }))
                    }
                    override fun onCancelled(error: DatabaseError) {
                        if (expected == version && sameAccount()) onError(error.toException())
                    }
                }
                listener = observer
                tasks.addValueEventListener(observer)
            }
        }, false)
    }

    fun stop() { version++; listener?.let { tasks.removeEventListener(it) }; listener = null }
    suspend fun getTask(id: String): TodoTask? { checkAccount(); return readTask(tasks.child(id).get().awaitResult()) }
    suspend fun add(task: TodoTask) { checkAccount(); tasks.child(task.id).setValue(task.cloudValue()).awaitResult() }
    suspend fun update(id: String, changes: Map<String, Any?>) { checkAccount(); tasks.child(id).updateChildren(changes).awaitResult() }
    suspend fun delete(id: String) { checkAccount(); tasks.child(id).removeValue().awaitResult() }
    private fun sameAccount() = FirebaseAuth.getInstance().currentUser?.uid == uid
    private fun checkAccount() { check(sameAccount()) { "Phiên đăng nhập đã thay đổi. Vui lòng đăng nhập lại." } }

    private fun TodoTask.cloudValue(): Map<String, Any> = mutableMapOf<String, Any>(
        "id" to id, "title" to title, "category" to category.name, "time" to time,
        "scheduledDay" to scheduledDay, "completed" to completed, "note" to note,
        "priority" to priority.apiValue, "remind" to remind,
        "subtasks" to subtasks.map { mapOf("title" to it.title, "done" to it.done) }
    ).apply { completedDay?.let { put("completedDay", it) } }

    private fun readTask(snapshot: DataSnapshot): TodoTask? {
        if (!snapshot.exists()) return null
        return runCatching { TodoTask(
            id = requireNotNull(snapshot.key), title = snapshot.child("title").value as String,
            category = TodoCategory.fromValue(snapshot.child("category").value as String),
            time = snapshot.child("time").value as String, scheduledDay = snapshot.child("scheduledDay").value as String,
            completed = snapshot.child("completed").value as Boolean,
            completedDay = snapshot.child("completedDay").value as? String,
            note = snapshot.child("note").value as? String ?: "",
            priority = TodoPriority.fromValue(snapshot.child("priority").value as? String ?: "medium"),
            remind = snapshot.child("remind").value as? Boolean ?: false,
            subtasks = snapshot.child("subtasks").children.map { item ->
                TodoSubtask(item.child("title").value as? String ?: "", item.child("done").value as? Boolean ?: false)
            }
        ) }.getOrNull()
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (continuation.isActive) {
                if (task.isSuccessful) continuation.resume(task.result)
                else continuation.resumeWithException(task.exception ?: IllegalStateException("Không lưu được dữ liệu Firebase."))
            }
        }
    }
}
