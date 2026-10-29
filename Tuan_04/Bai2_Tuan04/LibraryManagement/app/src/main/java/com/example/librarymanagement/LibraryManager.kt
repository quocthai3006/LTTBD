
package com.example.librarymanagement

import androidx.compose.runtime.mutableStateListOf

class LibraryManager {

    // Danh sách sinh viên
    val students = mutableStateListOf(
        Student(1, "Nguyễn Văn A"),
        Student(2, "Trần Thị B")
    )

    // Danh sách sách
    val books = mutableStateListOf(
        Book(1, "Sách 01"),
        Book(2, "Sách 02")
    )

    // Thêm sinh viên
    fun addStudent(name: String) {
        if (name.isBlank()) return

        val newId = (students.maxOfOrNull { it.id } ?: 0) + 1

        students.add(Student(newId, name.trim()))
    }

    // Thêm sách
    fun addBook(title: String) {
        if (title.isBlank()) return

        val newId = (books.maxOfOrNull { it.id } ?: 0) + 1

        books.add(Book(newId, title.trim()))
    }

    // Xác định ai đang mượn một cuốn sách
    fun studentOf(book: Book): Student? {
        return students.find { it.id == book.borrowerId }
    }

    // Tìm tất cả sách một sinh viên đang mượn
    fun booksOf(student: Student): List<Book> {
        return books.filter { it.borrowerId == student.id }
    }

    // Xử lý khi sinh viên tích hoặc bỏ tích sách
    fun setBorrowed(
        book: Book,
        student: Student,
        checked: Boolean
    ) {
        if (checked) {
            book.borrow(student.id)
        } else {
            book.returnBook(student.id)
        }
    }
}
