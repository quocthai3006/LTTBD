# Kết quả kiểm tra — 10/11/2026

| Kiểm tra | Kết quả |
|---|---|
| `:app:assembleApiDebug` | Thành công, tạo được APK debug |
| `:app:testApiDebugUnitTest` | 21 bài kiểm tra, 0 thất bại, 0 lỗi |
| `:app:compileFirebaseDebugKotlin` | Thành công |
| Phân tích XML manifest/resources và JSON quy tắc mẫu | Hợp lệ |
| Checksum bản Gradle dùng kiểm tra so với wrapper của project | Trùng khớp |
| `LoginScreen.kt`, `MainActivity.kt`, `google-services.json` so với file gốc | Giữ nguyên từng byte |

Trong 21 bài kiểm tra có 10 bài dữ liệu/ngày/cache, 10 bài HTTP với MockWebServer và 1 bài unit test sẵn có.
HTTP tests kiểm tra Bearer header, đường dẫn, các phương thức GET/POST/PATCH/DELETE, body việc nhỏ,
phản hồi 201/204/404/500 và dữ liệu sai định dạng. Token trong tests là token giả, dùng máy chủ cục bộ.

Build kiểm tra để trống token API. Khi dùng mã nguồn trên máy của bạn, điền `MOCK_API_TOKEN` vào
`local.properties`, Sync và chọn `apiDebug` như hướng dẫn trong README.

Chưa chạy UI trên điện thoại/máy ảo trong phiên này, chưa xác nhận Google Sign-In và ghi dữ liệu Firebase trên thiết bị.
Thử gọi API thật trực tiếp từ môi trường kiểm tra nhận HTTP 403, nên không dùng kết quả đó để kết luận token của bạn đúng hay sai.
Ảnh bạn gửi cho thấy một request đã nhận 200 OK. Các bước kiểm tra trên thiết bị nằm ở mục 8 trong README.

Những điểm cần phân biệt khi trình bày bài:

- `apiDebug` gọi mock API và giữ thay đổi trong cache cục bộ; không trình bày cache đó như dữ liệu đã lưu trên server.
- `firebaseDebug` dùng Realtime Database theo tài khoản Google; dữ liệu cũ vẫn được đọc theo đường dẫn cũ.
- Nhắc việc dùng alarm cục bộ không chính xác; không phải Firebase Cloud Messaging.
- Analytics, Remote Config và Crashlytics là phần hướng dẫn tiếp theo, chưa được thêm vào bản tuần 04.
