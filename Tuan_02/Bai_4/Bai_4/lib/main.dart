import 'package:flutter/material.dart';
class Student {
  String name;
  double? score; // khai báo điểm có thể null
  Student(this.name, this.score); //tạo đtg Stu gán tên, điểm cho đối tg đó
}
List<Student> students = [
  Student('Nguyễn Văn An', 9.0),
  Student('Trần Thị Bình', 7.2),
  Student('Lê Minh Chi', null),
  Student('Phạm Quốc Dũng', 4.5),
  Student('Võ Thanh Em', 5.8),
];
String xepLoai(double? score) {
  if (score == null) {
    return 'Chưa có điểm';
  } else if (score >= 8.0) {
    return 'Giỏi';
  } else if (score >= 6.5) {
    return 'Khá';
  } else if (score >= 5.0) {
    return 'Trung bình';
  } else {
    return 'Yếu';
  }
}
Color mauXepLoai(double? score) {
  if (score == null) {
    return Colors.grey;
  } else if (score >= 8.0) {
    return Colors.green;
  } else if (score >= 6.5) {
    return Colors.blue;
  } else if (score >= 5.0) {
    return Colors.orange;
  } else {
    return Colors.red;
  }
}
void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    double tongDiem = 0;
int soCoDiem = 0;
int soGioi = 0;
int soChuaCoDiem = 0;
double? diemCaoNhat;
for (Student student in students) {  //lấy từng dsach students để ktra
  double? diem = student.score;  //lấy điểm

  if (diem == null) {
    soChuaCoDiem++;      //nếu null đếm lên 1
  } else {
    tongDiem += diem; //tổng = tổng + điểm
    soCoDiem++;    //nếu có tăng lên 1

    if (diem >= 8.0) {
      soGioi++;
    }

    if (diemCaoNhat == null || diem > diemCaoNhat) {
      diemCaoNhat = diem;
    }
  }
}
double? diemTrungBinh = soCoDiem > 0
    ? tongDiem / soCoDiem
    : null;

return MaterialApp(
  debugShowCheckedModeBanner: false,
  home: Scaffold(
    backgroundColor: const Color(0xFFF5F5F9),

    appBar: AppBar(
      backgroundColor: const Color(0xFF304775),
      title: const Text(
        'Bảng điểm lớp',
        style: TextStyle(
          color: Colors.white,
          fontWeight: FontWeight.bold,
        ),
      ),
    ),

    body: Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
  width: double.infinity,
  margin: const EdgeInsets.only(bottom: 20),
  padding: const EdgeInsets.all(16),

  decoration: BoxDecoration(
    color: const Color(0xFF304775),
    borderRadius: BorderRadius.circular(12),   //bo góc và màu
  ),

  child: Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      const Text(
        'Điểm trung bình lớp',
        style: TextStyle(
          color: Colors.white70,
          fontSize: 12,
        ),
      ),

      const SizedBox(height: 4),

      Text(diemTrungBinh?.toStringAsFixed(2) ?? '--',   //nếu có gtri hiện 2 số thập phân nếu ko=null
        style: const TextStyle(
          color: Colors.white,
          fontSize: 32,
          fontWeight: FontWeight.bold,),),

      const SizedBox(height: 16),

      Container(
        height: 1,
        color: Colors.white24,
      ),

      const SizedBox(height: 12),

      Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween, //kc giữa widget phải đến trái, trục chínhd
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,  //căn lề trái, trục phụ
            children: [
              const Text(
                'Giỏi',
                style: TextStyle(color: Colors.white70, fontSize: 11),
              ),
              Text(
                '$soGioi', //đưa gtri vào chuỗi
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),

          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Chưa có điểm',
                style: TextStyle(color: Colors.white70, fontSize: 11),
              ),
              Text(
                '$soChuaCoDiem',
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),

          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Cao nhất',
                style: TextStyle(color: Colors.white70, fontSize: 11),
              ),
              Text(
                diemCaoNhat?.toStringAsFixed(1) ?? '--',
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,),),
            ],
          ),
        ],
      ),
    ],
  ),
),

          Text(
  'DANH SÁCH · ${students.length} SINH VIÊN',
  style: const TextStyle(
    color: Color(0xFF6B7280),
    fontSize: 11,
    fontWeight: FontWeight.bold,
    letterSpacing: 0.5,),),

          const SizedBox(height: 12), //them kc

          for (Student student in students) //tạo container cho sv
  Container(
    width: double.infinity,

    margin: const EdgeInsets.only(bottom: 8), //kc giữa 2 thẻ sv
    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10,), //kc chữ bên trong cách mép trái/phai

    decoration: BoxDecoration(
      color: Colors.white,
      borderRadius: BorderRadius.circular(10),
      border: Border.all(
        color: const Color(0xFFE5E7EB),
      ),
    ),

    child: Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [

        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [

            Text(student.name,      //hiển thị tên sv 
              style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.bold,
              ),
            ),

            const SizedBox(height: 4),

            Text(
              xepLoai(student.score),
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.bold,
                color: mauXepLoai(student.score),),),
          ],
        ),

        Text(
          student.score?.toStringAsFixed(1) //chỉ gọi hàm khi biến khác null hiển thị 1 tphan
          ?? '—', // nếu null thì -
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.bold,
            color: student.score == null
                ? Colors.grey
                : const Color(0xFF222631),),),
      ],
    ),
  ),
        ],
      ),
    ),
  ),
);
  }
}