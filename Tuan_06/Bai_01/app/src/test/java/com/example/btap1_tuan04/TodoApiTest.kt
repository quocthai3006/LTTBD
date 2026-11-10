package com.example.btap1_tuan04

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TodoApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TodoApi
    private val example = """{"id":2,"title":"Ôn chương 3","note":"Cây nhị phân","category":"study","date":"2026-10-20","time":"14:00","priority":"high","remind":true,"done":false,"subtasks":[{"title":"Đọc slide","done":true}]}"""
    @Before fun setup() { server = MockWebServer(); server.start(); api = TodoApi(server.url("/mock/teach/").toString(), "unit-test-token") }
    @After fun teardown() { server.shutdown() }
    private fun json(body: String, status: Int = 200) = MockResponse().setResponseCode(status)
        .setHeader("Content-Type", "application/json; charset=utf-8").setBody(body)

    @Test fun getListSendsBearerAndReadsFullTask() = runBlocking {
        server.enqueue(json("[$example]"))
        val tasks = api.getTasks()
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/mock/teach/tasks", request.path)
        assertEquals("Bearer unit-test-token", request.getHeader("Authorization"))
        assertEquals("Cây nhị phân", tasks.single().note)
        assertEquals(TodoPriority.HIGH, tasks.single().priority)
        assertEquals(listOf(TodoSubtask("Đọc slide", true)), tasks.single().subtasks)
        assertNull(tasks.single().completedDay)
    }
    @Test fun getDetailUsesSelectedId() = runBlocking {
        server.enqueue(json(example)); api.getTask("2")
        assertEquals("/mock/teach/tasks/2", server.takeRequest().path)
    }
    @Test fun completionPatchOnlySendsDone() = runBlocking {
        server.enqueue(json(example)); api.patchTask("2", JSONObject().put("done", true))
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        val body = JSONObject(request.body.readUtf8())
        assertEquals(1, body.length()); assertTrue(body.getBoolean("done"))
    }
    @Test fun subtaskPatchSendsWholeOrderedArray() = runBlocking {
        server.enqueue(json(example))
        api.patchTask("2", JSONObject().put("subtasks", subtasksJson(listOf(TodoSubtask("A", true), TodoSubtask("B")))))
        val body = JSONObject(server.takeRequest().body.readUtf8()).getJSONArray("subtasks")
        assertEquals("A", body.getJSONObject(0).getString("title"))
        assertFalse(body.getJSONObject(1).getBoolean("done"))
    }
    @Test fun createSendsRequiredFieldsAndAccepts201() = runBlocking {
        server.enqueue(json(example, 201))
        api.addTask(TaskDraft("Việc mới", "Ghi chú", TodoCategory.LIFE, "20261110", "19:00", TodoPriority.MEDIUM, true))
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        val body = JSONObject(request.body.readUtf8())
        assertEquals("2026-11-10", body.getString("date"))
        assertEquals("life", body.getString("category"))
        assertFalse(body.has("id"))
    }
    @Test fun deleteAcceptsEmpty204WithoutParsingJson() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))
        api.deleteTask("2")
        assertEquals("DELETE", server.takeRequest().method)
    }
    @Test fun failureKeepsVietnameseServerMessage() = runBlocking {
        server.enqueue(json("""{"message":"Không tìm thấy công việc."}""", 404))
        val error = runCatching { api.getTask("999") }.exceptionOrNull()
        assertTrue(error is TaskApiException)
        assertEquals(404, (error as TaskApiException).statusCode)
        assertEquals("Không tìm thấy công việc.", error.message)
    }
    @Test fun htmlServerErrorHasReadableFallback() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("<html>error</html>"))
        val error = runCatching { api.getTasks() }.exceptionOrNull()
        assertTrue(error is TaskApiException)
        assertTrue(error!!.message!!.contains("500"))
        assertFalse(error.message!!.contains("unit-test-token"))
    }
    @Test fun invalidSuccessBodyDoesNotLookLikeEmptySuccess() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("<html>login</html>"))
        assertTrue(runCatching { api.getTasks() }.exceptionOrNull() is java.io.IOException)
    }
    @Test fun missingTokenFailsBeforeNetworkCall() = runBlocking {
        val noToken = TodoApi(server.url("/").toString(), "")
        assertTrue(runCatching { noToken.getTasks() }.exceptionOrNull() is TaskApiException)
        assertEquals(0, server.requestCount)
    }
}
