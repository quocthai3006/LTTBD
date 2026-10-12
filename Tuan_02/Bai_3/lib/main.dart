
import 'package:flutter/material.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  final double diem = 8.5; //gián giá trị 1 lần kiểu số thực

  String xepLoai(double diem) {
    if (diem < 0 || diem > 10) {
      return 'Không hợp lệ';
    } else if (diem >= 8.0) {
      return 'Giỏi';
    } else if (diem >= 6.5) {
      return 'Khá';
    } else if (diem >= 5.0) {
      return 'Trung bình';
    } else {
      return 'Yếu';
    }
  }
Color mauXepLoai(double diem) {
  if (diem < 0 || diem > 10) {
    return Colors.grey;
  } else if (diem >= 8.0) {
    return Colors.green;
  } else if (diem >= 6.5) {
    return Colors.blue;
  } else if (diem >= 5.0) {
    return Colors.orange;
  } else {
    return Colors.red;
  }
}

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Xếp loại học lực'),
          backgroundColor: Colors.blue,
        ),
        
body: Column(
  children: [
    Container(
      margin: const EdgeInsets.all(16),   // kc bên ngoài thẻ
      padding: const EdgeInsets.all(16),  //kc viền đến nd

      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),   //bo gốc
      ),

      child: Column(
        children: [
          const Text(
            'Điểm trung bình',
            style: TextStyle(
              color: Color.fromARGB(255, 112, 108, 108),
              fontSize: 14,
            ),
          ),

          const SizedBox(height: 8),

          Text(
            diem.toString(),
            style: const TextStyle(
              fontSize: 48,
              fontWeight: FontWeight.bold,
            ),
          ),

          const SizedBox(height: 12),

          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.green.shade50,
              borderRadius: BorderRadius.circular(8),
            ),
            child: Text(
              xepLoai(diem),
              textAlign: TextAlign.center,
              style: TextStyle(
                color: mauXepLoai(diem),
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ],
      ),
    ),
    
const SizedBox(height: 4),

Container(      //ktra thêm
  width: double.infinity,
  margin: const EdgeInsets.symmetric(horizontal: 16),
  child: const Text(
    'KIỂM TRA THÊM',
    style: TextStyle(
      fontSize: 13,
      color: Colors.grey,
      fontWeight: FontWeight.bold,
    ),
  ),
),

const SizedBox(height: 8),

Container(
  margin: const EdgeInsets.symmetric(horizontal: 16),
  padding: const EdgeInsets.symmetric(
    horizontal: 12,
    vertical: 4,
  ),
  decoration: BoxDecoration(
    color: Colors.white,
    borderRadius: BorderRadius.circular(12),
  ),

  child: Column(
    children: [
      for (final diemThu in <double>[9, 7, 5.5, 3, 11])  // vòng lặp tạo 5 dd tươn ứng với điểm
        Container(
          padding: const EdgeInsets.symmetric(vertical: 12),
          decoration: const BoxDecoration(
            border: Border(
              bottom: BorderSide(
                color: Color(0xFFEEEEEE),
              ),
            ),
          ),

          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Điểm ${diemThu.toString().replaceAll('.0', '')}',
              ),

              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 10,
                  vertical: 5,
                ),
                decoration: BoxDecoration(
                  color: mauXepLoai(diemThu).withValues(alpha: 0.12), //tạo nền màu nhạt cho từng xl
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text(
                  xepLoai(diemThu),    //chon màu tương ứng vs điểm
                  style: TextStyle(
                    color: mauXepLoai(diemThu),    
                    fontWeight: FontWeight.bold,
                    fontSize: 12,
                  ),
                ),
              ),
            ],
          ),
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
