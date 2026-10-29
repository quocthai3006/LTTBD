
package com.example.librarymanagement

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.List

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                LibraryApp()
            }
        }
    }
}


@Composable
fun LibraryApp() {
    val manager = remember { LibraryManager() }

    var selectedTab by rememberSaveable {
        mutableIntStateOf(0)
    }

    var selectedStudentId by rememberSaveable {
        mutableIntStateOf(1)
    }

    val blue = Color(0xFF2B56A7)
    val gray = Color(0xFFBDBDBD)

    val labels = listOf("Quản lý", "DS Sách", "Sinh viên")
    val icons = listOf(
        Icons.Default.Home,
        Icons.AutoMirrored.Filled.List,
        Icons.Default.Person
    )

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp
            ) {
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                        },
                        icon = {
                            Icon(
                                imageVector = icons[index],
                                contentDescription = label
                            )
                        },
                        label = {
                            Text(label)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = blue,
                            selectedTextColor = blue,
                            unselectedIconColor = gray,
                            unselectedTextColor = gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> ManagementTab(
                    manager = manager,
                    selectedStudentId = selectedStudentId,
                    onSelectStudent = {
                        selectedStudentId = it
                    }
                )

                1 -> BookTab(
                    manager = manager,
                    selectedStudentId = selectedStudentId,
                    onSelectStudent = {
                        selectedStudentId = it
                    }
                )

                2 -> StudentTab(manager)
            }
        }
    }
}

// Hiển thị thông tin đối tượng - ĐA HÌNH
@Composable
fun EntityTitle(entity: LibraryEntity) {
    Text(
        text = entity.showInfo(),
        fontWeight = FontWeight.Bold
    )
}

// TAB SÁCH
@Composable
fun BookTab(
    manager: LibraryManager,
    selectedStudentId: Int,
    onSelectStudent: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var newTitle by rememberSaveable { mutableStateOf("") }

    val selectedStudent = manager.students.find {
        it.id == selectedStudentId
    } ?: manager.students.firstOrNull()

    Column {
        Text("Sinh viên đang chọn", fontWeight = FontWeight.Bold)

        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(selectedStudent?.name ?: "Chưa có sinh viên")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                manager.students.forEach { student ->
                    DropdownMenuItem(
                        text = { Text(student.name) },
                        onClick = {
                            onSelectStudent(student.id)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = { Text("Tên sách") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    manager.addBook(newTitle)
                    newTitle = ""
                },
                enabled = newTitle.isNotBlank()
            ) {
                Text("Thêm")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Danh sách sách", fontWeight = FontWeight.Bold)

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(manager.books, key = { it.id }) { book ->
                val borrower = manager.studentOf(book)

                val borrowedByOther =
                    borrower != null &&
                            borrower.id != selectedStudent?.id

                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedStudent != null &&
                                    book.borrowerId == selectedStudent.id,
                            enabled = selectedStudent != null &&
                                    !borrowedByOther,
                            onCheckedChange = { checked ->
                                selectedStudent?.let {
                                    manager.setBorrowed(book, it, checked)
                                }
                            }
                        )

                        Column {
                            EntityTitle(book)

                            Text(
                                text = if (borrower == null) {
                                    "Chưa được mượn"
                                } else {
                                    "Đang mượn: ${borrower.name}"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

// TAB SINH VIÊN
@Composable
fun StudentTab(manager: LibraryManager) {
    var newName by rememberSaveable { mutableStateOf("") }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Tên sinh viên") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    manager.addStudent(newName)
                    newName = ""
                },
                enabled = newName.isNotBlank()
            ) {
                Text("Thêm")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Danh sách sinh viên", fontWeight = FontWeight.Bold)

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(manager.students, key = { it.id }) { student ->
                val borrowedBooks = manager.booksOf(student)

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        EntityTitle(student)

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("Mã sinh viên nội bộ: ${student.id}")

                        Text(
                            "Số sách đang mượn: ${borrowedBooks.size}"
                        )

                        if (borrowedBooks.isEmpty()) {
                            Text("Chưa mượn sách")
                        } else {
                            borrowedBooks.forEach { book ->
                                Text("• ${book.title}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManagementTab(
    manager: LibraryManager,
    selectedStudentId: Int,
    onSelectStudent: (Int) -> Unit
) {
    val blue = Color(0xFF2B56A7)
    val red = Color(0xFF9F2542)
    val lightGray = Color(0xFFD9D9D9)

    var showStudentPicker by remember {
        mutableStateOf(false)
    }

    var showAddBook by remember {
        mutableStateOf(false)
    }

    var newBookTitle by rememberSaveable {
        mutableStateOf("")
    }

    val currentStudent = manager.students.find {
        it.id == selectedStudentId
    } ?: manager.students.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 1. Tiêu đề
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Hệ thống\nQuản lý Thư viện",
            fontSize = 23.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(65.dp))

        // 2. Sinh viên
        Text(
            text = "Sinh viên",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .border(
                        1.dp,
                        Color.Gray,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = currentStudent?.name
                        ?: "Chưa có sinh viên",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = {
                    showStudentPicker = true
                },
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = blue
                ),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("Thay đổi", fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 3. Danh sách sách
        Text(
            text = "Danh sách sách",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(205.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(lightGray)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(manager.books, key = { it.id }) { book ->

                val isMine = currentStudent != null &&
                        book.borrowerId == currentStudent.id

                val borrowedByOther =
                    book.borrowerId != null && !isMine

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(49.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isMine,
                            enabled = currentStudent != null &&
                                    !borrowedByOther,
                            onCheckedChange = { checked ->
                                currentStudent?.let {
                                    manager.setBorrowed(
                                        book,
                                        it,
                                        checked
                                    )
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = red,
                                uncheckedColor = red,
                                checkmarkColor = Color.White
                            )
                        )

                        Text(
                            text = book.title,
                            fontSize = 15.sp,
                            color = if (borrowedByOther)
                                Color.Gray
                            else Color.DarkGray
                        )

                        if (borrowedByOther) {
                            Text(
                                text = " · Đã mượn",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4. Nút Thêm
        Button(
            onClick = {
                showAddBook = true
            },
            modifier = Modifier
                .width(160.dp)
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = blue
            )
        ) {
            Text(
                text = "Thêm",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    // 5. Hộp thoại thay đổi sinh viên
    if (showStudentPicker) {
        AlertDialog(
            onDismissRequest = {
                showStudentPicker = false
            },
            title = {
                Text("Chọn sinh viên")
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    manager.students.forEach { student ->
                        TextButton(
                            onClick = {
                                onSelectStudent(student.id)
                                showStudentPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(student.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStudentPicker = false
                    }
                ) {
                    Text("Đóng")
                }
            }
        )
    }

    // 6. Hộp thoại thêm sách
    if (showAddBook) {
        AlertDialog(
            onDismissRequest = {
                showAddBook = false
            },
            title = {
                Text("Thêm sách mới")
            },
            text = {
                OutlinedTextField(
                    value = newBookTitle,
                    onValueChange = {
                        newBookTitle = it
                    },
                    label = {
                        Text("Tên sách")
                    },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        manager.addBook(newBookTitle)
                        newBookTitle = ""
                        showAddBook = false
                    },
                    enabled = newBookTitle.isNotBlank()
                ) {
                    Text("Thêm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddBook = false
                    }
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}
