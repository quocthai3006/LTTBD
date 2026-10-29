package com.example.librarymanagement

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Class cha trừu tượng
abstract class LibraryEntity(val id: Int) {
    abstract fun showInfo(): String
}

// Class Sinh viên
class Student(
    id: Int,
    val name: String
) : LibraryEntity(id) {

    override fun showInfo(): String {
        return "Sinh viên: $name"
    }
}

// Class Sách
class Book(
    id: Int,
    val title: String
) : LibraryEntity(id) {

    var borrowerId: Int? by mutableStateOf(null)   //lưu mã sv mượn
        private set

    fun borrow(studentId: Int): Boolean {
        if (borrowerId != null) return false

        borrowerId = studentId
        return true
    }

    fun returnBook(studentId: Int): Boolean {
        if (borrowerId != studentId) return false

        borrowerId = null
        return true
    }

    override fun showInfo(): String {
        return "Sách: $title"
    }
}
