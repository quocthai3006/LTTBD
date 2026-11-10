import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

val localConfig = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
val mockToken = providers.environmentVariable("MOCK_API_TOKEN")
    .orElse(localConfig.getProperty("MOCK_API_TOKEN", "")).get()

android {
    namespace = "com.example.btap1_tuan04"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.btap1_tuan04"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "2.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "MOCK_API_TOKEN", quoted(mockToken))
        buildConfigField("String", "TASK_API_BASE_URL", quoted("https://share.tryapps.store/mock/teach/"))
    }

    // Hai bản dùng chung UI và đăng nhập Google; dữ liệu được lưu riêng.
    flavorDimensions += "dataSource"
    productFlavors {
        create("api") {
            dimension = "dataSource"
            buildConfigField("boolean", "USE_TEACHING_API", "true")
        }
        create("firebase") {
            dimension = "dataSource"
            buildConfigField("boolean", "USE_TEACHING_API", "false")
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(
        "androidx.navigation:navigation-compose:2.10.2"
    )
    // Quản lý phiên bản các thư viện Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
// Xác thực tài khoản bằng Firebase
    implementation("com.google.firebase:firebase-auth")

// Đăng nhập Google bằng Credential Manager
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.firebase:firebase-database")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    // Android cung cấp org.json khi chạy app; JVM tests dùng bản độc lập này.
    testImplementation("org.json:json:20240303")
}
