package com.example.btap1_tuan04

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TaskDataTest {
    private fun utc(text: String): Date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(text)!!
    private fun task(id: String, title: String = "Việc $id") =
        TodoTask(id, title, TodoCategory.STUDY, "14:00", "20261110")

    @Test fun vietnamDateChangesAt1700Utc() {
        assertEquals("20261109", dayKey(utc("2026-11-09 16:59")))
        assertEquals("20261110", dayKey(utc("2026-11-09 17:00")))
    }
    @Test fun sundayAndTomorrowCrossWeekCorrectly() {
        val sunday = utc("2026-11-15 05:00")
        assertEquals("20261115", endOfWeekKey(sunday))
        assertEquals("Ngày mai", taskDateLabel("20261116", sunday))
        assertEquals("17/11", taskDateLabel("20261117", sunday))
        assertEquals("Tuần này", taskDateLabel("20261115", utc("2026-11-10 05:00")))
    }
    @Test fun invalidCalendarDatesAreRejected() {
        assertTrue(runCatching { apiDateToDayKey("2026-02-30") }.isFailure)
        assertTrue(runCatching { apiDateToDayKey("2026-2-3") }.isFailure)
        assertEquals("20240229", apiDateToDayKey("2024-02-29"))
    }
    @Test fun oldFirebaseCacheStillLoads() {
        val legacy = JSONObject("""{"id":"sample-3","title":"Ôn bài","category":"STUDY","time":"14:00","scheduledDay":"20261110","completed":false,"completedDay":null}""")
        val loaded = taskFromCache(legacy)
        assertEquals("sample-3", loaded.id)
        assertEquals("", loaded.note)
        assertEquals(TodoPriority.MEDIUM, loaded.priority)
        assertTrue(loaded.subtasks.isEmpty())
    }
    @Test fun mockRefreshKeepsChangesAndDoesNotResurrectDeletedTasks() {
        val server = listOf(task("1"), task("2"), task("3"))
        val local = listOf(task("1").copy(completed = true, completedDay = "20261110"), task("3"), task("7", "Mới thêm"))
        val merged = mergeTeachingSnapshot(server, local, setOf("1", "7"), setOf("2"))
        assertEquals(listOf("1", "3", "7"), merged.map { it.id })
        assertTrue(merged.first().completed)
        assertEquals("Mới thêm", merged.last().title)
    }
    @Test fun emptyListAfterDeletingEveryFixtureStaysEmpty() {
        assertTrue(mergeTeachingSnapshot(listOf(task("1"), task("2")), emptyList(), emptySet(), setOf("1", "2")).isEmpty())
    }
    @Test fun editingFieldsPreservesCompletionAndSubtasks() {
        val old = task("2").copy(completed = true, completedDay = "20261110", subtasks = listOf(TodoSubtask("Đọc slide", true)))
        val draft = TaskDraft(" Đổi tên ", " Ghi chú ", TodoCategory.LIFE, "20261112", "19:05", TodoPriority.HIGH, true)
        val changed = draft.applyTo(old)
        assertEquals("Đổi tên", changed.title)
        assertEquals(old.subtasks, changed.subtasks)
        assertEquals(old.completedDay, changed.completedDay)
        assertTrue(changed.completed)
        assertEquals("2026-11-12", draft.toApiJson().getString("date"))
        assertEquals("life", draft.toApiJson().getString("category"))
    }
    @Test fun weeklyChartDoesNotInventCompletionDate() {
        val unknown = task("1").copy(completed = true)
        val known = task("2").copy(completed = true, completedDay = "20261110")
        assertEquals(listOf(0, 1, 0, 0, 0, 0, 0), completedThisWeek(listOf(unknown, known), utc("2026-11-10 05:00")))
    }
    @Test fun cacheRoundTripKeepsFullTask() {
        val original = task("2").copy(note = "Tiếng Việt có dấu", priority = TodoPriority.HIGH,
            remind = true, subtasks = listOf(TodoSubtask("Đọc", true), TodoSubtask("Làm bài")))
        assertEquals(original, taskFromCache(taskToCache(original)))
    }
    @Test fun blankTitleAndInvalidTimeAreRejected() {
        val draft = TaskDraft(" ", "", TodoCategory.STUDY, "20261110", "25:00", TodoPriority.MEDIUM, true)
        assertTrue(runCatching { draft.validate() }.isFailure)
        assertTrue(runCatching { draft.copy(title = "Việc").validate() }.isFailure)
    }
}
