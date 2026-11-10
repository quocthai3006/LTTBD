package com.example.btap1_tuan04

// Các loại hình minh họa
enum class IllustrationType {
    CHECKLIST,
    CATEGORIES,
    REMINDER
}

// Lớp lưu thông tin chung của ứng dụng
data class AppInfo(
    val name: String,
    val slogan: String,
    val version: String,
    val skipText: String,
    val homeTitle: String,
    val homeMessage: String
)

// Lớp mô tả một màn hình giới thiệu
data class OnboardingItem(
    val title: String,
    val description: String,
    val buttonText: String,
    val illustration: IllustrationType,
    val previewTexts: List<String>
)

// Đối tượng quản lý toàn bộ dữ liệu
object TodoRepository {

    val appInfo = AppInfo(
        name = "UTH Todo",
        slogan = "Việc học, việc nhà – gọn gàng mỗi ngày",
        version = "Phiên bản 2.1.0",
        skipText = "Bỏ qua",
        homeTitle = "Trang chủ UTH Todo",
        homeMessage = "Bạn đã hoàn thành phần giới thiệu!"
    )

    // Mảng chứa 3 đối tượng OnboardingItem
    val pages: Array<OnboardingItem> = arrayOf(

        OnboardingItem(
            title = "Mọi việc, gọn trong một chỗ",
            description = "Ghi lại bài tập, việc nhà, lịch hẹn. Không còn quên việc nào.",
            buttonText = "Tiếp tục",
            illustration = IllustrationType.CHECKLIST,
            previewTexts = listOf(
                "Làm bài tập",
                "Việc nhà",
                "Lịch hẹn"
            )
        ),

        OnboardingItem(
            title = "Chia theo Học tập và Cuộc sống",
            description = "Mỗi việc có nhóm riêng, nhìn là biết hôm nay cần lo gì.",
            buttonText = "Tiếp tục",
            illustration = IllustrationType.CATEGORIES,
            previewTexts = listOf(
                "Học tập",
                "Cuộc sống"
            )
        ),

        OnboardingItem(
            title = "Nhắc đúng lúc",
            description = "Bật thông báo để UTH Todo nhắc bạn trước hạn chót.",
            buttonText = "Bắt đầu ngay",
            illustration = IllustrationType.REMINDER,
            previewTexts = listOf(
                "Ôn bài Toán lúc 19:00"
            )
        )
    )
}

// Các màn hình có thể điều hướng đến
enum class Screen(val route: String) {
    SPLASH("splash"),
    INTRO1("intro1"),
    INTRO2("intro2"),
    INTRO3("intro3"),
    HOME("home")
}
