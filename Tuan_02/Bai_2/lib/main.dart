import 'package:flutter/material.dart';

class Student {
  String name;
  String? email;
  String? phone;

  Student({
    required this.name,
    this.email,
    this.phone,
  });
}

final List<Student> students = [
  Student(
    name: 'Nguyễn Văn An',
    email: 'an@sv.edu.vn',
    phone: '0901 234 567',
  ),
  Student(
    name: 'Trần Thị Bình',
    email: null,
    phone: '0912 888 999',
  ),
  Student(
    name: 'Lê Minh Chi',
    email: 'chi@sv.edu.vn',
    phone: null,
  ),
  Student(
    name: 'Phạm Quốc Dũng',
    email: null,
    phone: null,
  ),
  Student(
    name: 'Võ Thanh Em',
    email: 'em@sv.edu.vn',
    phone: '0933 111 222',
  ),
];


void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget { //ko qly tt thay doi
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    
int missingEmailCount = 0;

for (final student in students) {
  if (student.email == null) {
    missingEmailCount++;
  }
}
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(    //ctruc
        backgroundColor: const Color(0xFFF5F5F9),
        appBar: AppBar(  //thanh tilte màu xanh
          backgroundColor: const Color(0xFF2D4778),
          title: const Text(
            'Danh sách sinh viên',
            style: TextStyle(
              color: Colors.white,
              fontSize: 18,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
        
body: Column(
  children: [
    const SizedBox(height: 14),

    for (final student in students)
      Container(
        width: double.infinity,
        margin: const EdgeInsets.only(
          left: 16,
          right: 16,
          bottom: 10,
        ),
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              student.name,
              style: const TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.bold,
              ),
            ),

            const SizedBox(height: 5),

            Row(
              children: [
                const Text('Email: '),
                Text(
                  student.email ?? 'Chưa cập nhật',
                  style: TextStyle(
                    color: student.email == null
                        ? Colors.red
                        : Colors.grey,
                    fontStyle: student.email == null
                        ? FontStyle.italic
                        : FontStyle.normal,
                  ),
                ),
              ],
            ),

            const SizedBox(height: 3),

            Row(
              children: [
                const Text('SĐT: '),
                Text(
                  student.phone ?? 'Chưa cập nhật',
                  style: TextStyle(
                    color: student.phone == null
                        ? Colors.red
                        : Colors.grey,
                    fontStyle: student.phone == null
                        ? FontStyle.italic
                        : FontStyle.normal,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
     Container(
  width: double.infinity,
  margin: const EdgeInsets.symmetric(horizontal: 16),
  padding: const EdgeInsets.all(14),
  color: const Color.fromARGB(255, 223, 230, 241),

child: Row(
  mainAxisAlignment: MainAxisAlignment.spaceBetween,
  children: [
    Row(
      children: [
        const Text('Tổng: '),
        Text(
          '${students.length} sinh viên',
          style: const TextStyle(
            fontWeight: FontWeight.bold,
          ),
        ),
      ],
    ),

    Row(
      children: [
        const Text('Thiếu email: '),
        Text(
          '$missingEmailCount',
          style: const TextStyle(
            color: Colors.red,
            fontWeight: FontWeight.bold,
          ),
        ),
      ],
    ),
  ],
),
),
  ],
),
      ),
    );
  }
  
}
