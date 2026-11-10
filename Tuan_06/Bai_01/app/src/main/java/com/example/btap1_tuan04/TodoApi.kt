package com.example.btap1_tuan04

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TaskApiException(val statusCode: Int, message: String) : IOException(message)

/** Chỉ xử lý HTTP/JSON. Không biết Compose, Firebase hoặc trạng thái màn hình. */
class TodoApi(
    baseUrl: String = BuildConfig.TASK_API_BASE_URL,
    private val token: String = BuildConfig.MOCK_API_TOKEN,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS).followRedirects(false).build()
) {
    private val base = (baseUrl.trimEnd('/') + "/").toHttpUrl()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun getTasks(): List<TodoTask> = decode {
        val array = JSONArray(request("GET", "tasks"))
        List(array.length()) { taskFromApi(array.getJSONObject(it)) }
    }

    suspend fun getTask(id: String): TodoTask = decode {
        taskFromApi(JSONObject(request("GET", taskPath(id))))
    }

    suspend fun addTask(draft: TaskDraft): TodoTask = decode {
        draft.validate()
        taskFromApi(JSONObject(request("POST", "tasks", draft.toApiJson())))
    }

    suspend fun patchTask(id: String, changes: JSONObject): TodoTask = decode {
        taskFromApi(JSONObject(request("PATCH", taskPath(id), changes)))
    }

    suspend fun deleteTask(id: String) {
        request("DELETE", taskPath(id), expectedNoContent = true)
    }

    private fun taskPath(id: String): String {
        require(id.toLongOrNull()?.let { it > 0 } == true) { "Mã công việc API không hợp lệ." }
        return "tasks/$id"
    }

    private suspend fun request(
        method: String, path: String, body: JSONObject? = null, expectedNoContent: Boolean = false
    ): String {
        if (token.isBlank()) throw TaskApiException(401,
            "Chưa cấu hình MOCK_API_TOKEN trong local.properties. Điền token rồi Sync và chạy lại app.")
        val url = requireNotNull(base.resolve(path))
        val request = Request.Builder().url(url)
            .header("Authorization", "Bearer ${token.trim()}")
            .header("Accept", "application/json")
            .method(method, body?.toString()?.toRequestBody(jsonType)).build()
        return client.newCall(request).await().use { response -> withContext(Dispatchers.IO) {
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val serverMessage = runCatching { JSONObject(text).optString("message") }
                    .getOrNull()?.takeIf { it.isNotBlank() }
                throw TaskApiException(response.code, serverMessage ?: when (response.code) {
                    401, 403 -> "API từ chối truy cập. Kiểm tra token API và chạy lại app."
                    404 -> "Không tìm thấy công việc."
                    else -> "Không tải được dữ liệu (HTTP ${response.code}). Vui lòng thử lại."
                })
            }
            if (expectedNoContent) {
                if (response.code != 204) throw TaskApiException(response.code,
                    "Phản hồi xóa không đúng định dạng 204 No Content.")
                ""
            } else text
        } }
    }

    private suspend fun <T> decode(block: suspend () -> T): T = try {
        block()
    } catch (error: org.json.JSONException) {
        throw IOException("API trả dữ liệu JSON không đúng định dạng công việc.", error)
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }
            override fun onResponse(call: Call, response: Response) {
                if (continuation.isActive) continuation.resume(response) { _, value, _ -> value.close() }
                else response.close()
            }
        })
    }
}
