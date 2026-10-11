import 'package:flutter/material.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      debugShowCheckedModeBanner: false,
      home: ProfilePage(),
    );
  }
}

class ProfilePage extends StatelessWidget {
  const ProfilePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      // SafeArea giúp giao diện không bị đè bởi tai thỏ, camera của điện thoại
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 10.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              // 1. Thanh điều hướng trên cùng (Nút Back và Nút Edit)
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  // Nút mũi tên quay lại bên trái
                  Container(
                    decoration: BoxDecoration(
                      border: Border.all(color: Colors.grey.shade300),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: IconButton(
                      icon: const Icon(Icons.arrow_back, color: Colors.black54),
                      onPressed: () {},
                    ),
                  ),
                  // Nút chỉnh sửa bên phải
                  Container(
                    decoration: BoxDecoration(
                      border: Border.all(color: Colors.grey.shade300),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: IconButton(
                      icon: const Icon(Icons.edit_outlined, color: Colors.green),
                      onPressed: () {},
                    ),
                  ),
                ],
              ),

              // Khoảng cách từ thanh điều hướng xuống ảnh đại diện
              const Spacer(flex: 2),

              // 2. Ảnh đại diện hình tròn (CircleAvatar)
              Center(
                  child: const CircleAvatar(
                    radius: 90, // Độ lớn của ảnh đại diện
                    backgroundImage: AssetImage('images/ngua.jpg'),
                  ),
              ),

              // Khoảng cách giữa ảnh và Tên
              const SizedBox(height: 26),

              // 3. Hiển thị Tên (Chữ đậm)
              const Text(
                'Nguyễn Quốc Thái',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 25,
                  fontWeight: FontWeight.bold,
                  color: Colors.black87,
                ),
              ),

              // Khoảng cách nhỏ giữa Tên và MSSV
              const SizedBox(height: 3),

              // 4. Hiển thị Mã số sinh viên
              const Text(
                'MSSV: 087206004822',
                style: TextStyle(
                  fontSize: 20,
                  color: Color.fromARGB(255, 88, 85, 85),
                  letterSpacing: 0.5,
                ),
              ),
              const Spacer(flex: 3),
            ],
          ),
        ),
      ),
    );
  }
}
