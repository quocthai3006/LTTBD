package com.example.dataflow

import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private val MainBlue = Color(0xFF4899EA)

// Đối tượng chứa dữ liệu của bài tập
data class ResetInfo(
    val email: String = "",
    val otp: String = "",
    val password: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ResetFlowApp()
            }
        }
    }
}

// Quản lý dữ liệu và điều hướng
@Composable
fun ResetFlowApp() {
    val navController = rememberNavController()

    var info by remember { mutableStateOf(ResetInfo()) }
    var finished by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = "email"
    ) {
        composable("email") {
            EmailScreen(
                info = info,
                finished = finished,
                onEmailChange = {
                    info = info.copy(email = it)
                    finished = false
                },
                onNext = {
                    navController.navigate("verify")
                }
            )
        }

        composable("verify") {
            OtpScreen(
                otp = info.otp,
                onOtpChange = {
                    info = info.copy(otp = it)
                },
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate("reset") }
            )
        }

        composable("reset") {
            PasswordScreen(
                password = info.password,
                onPasswordChange = {
                    info = info.copy(password = it)
                },
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate("confirm") }
            )
        }

        composable("confirm") {
            ConfirmScreen(
                info = info,
                onBack = { navController.popBackStack() },
                onSubmit = {
                    finished = true

                    navController.navigate("email") {
                        popUpTo("email") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

// Phần logo và tên ứng dụng dùng chung
@Composable
fun BrandHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "UTH",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3A8580)
        )
        Text(
            text = "UNIVERSITY OF TRANSPORT\nHO CHI MINH CITY",
            fontSize = 9.sp,
            lineHeight = 11.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFFC64740)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "SmartTasks",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = MainBlue
        )
    }
}

// Giao diện cơ bản dùng chung
@Composable
fun ScreenLayout(
    title: String,
    description: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (onBack != null) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text("❮ Quay lại", color = MainBlue)
            }
        } else {
            Spacer(Modifier.height(40.dp))
        }

        Spacer(Modifier.height(35.dp))
        BrandHeader()
        Spacer(Modifier.height(35.dp))

        Text(
            text = title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))
        Text(
            text = description,
            fontSize = 13.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        content()
    }
}

@Composable
fun BlueButton(
    title: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(30.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MainBlue
        )
    ) {
        Text(title)
    }
}

// MÀN HÌNH 1: NHẬP EMAIL
@Composable
fun EmailScreen(
    info: ResetInfo,
    finished: Boolean,
    onEmailChange: (String) -> Unit,
    onNext: () -> Unit
) {
    val validEmail =
        Patterns.EMAIL_ADDRESS.matcher(info.email.trim()).matches()
                && info.email.trim().endsWith(
            "@ut.edu.vn",
            ignoreCase = true
        )

    ScreenLayout(
        title = "Forget Password?",
        description = "Enter your Email, we will send you a verification code."
    ) {
        OutlinedTextField(
            value = info.email,
            onValueChange = onEmailChange,
            label = { Text("Your Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(22.dp))
        BlueButton(
            title = "Next",
            enabled = validEmail,
            onClick = onNext
        )

        // Hiển thị dữ liệu sau khi Submit
        if (finished) {
            Spacer(Modifier.height(35.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFFDFECFF),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(20.dp)
            ) {
                Text(
                    "Thông tin đã thực hiện",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Spacer(Modifier.height(14.dp))
                Text("Email: ${info.email}")
                Text("OTP: ${info.otp}")
                Text(
                    "Mật khẩu: ${"●".repeat(info.password.length)}"
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Trạng thái: Hoàn thành",
                    color = Color(0xFF218148)
                )
            }
        }
    }
}

// MÀN HÌNH 2: XÁC THỰC OTP
@Composable
fun OtpScreen(
    otp: String,
    onOtpChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    ScreenLayout(
        title = "Verify Code",
        description = "Enter any 5-digit verification code.",
        onBack = onBack
    ) {
        OutlinedTextField(
            value = otp,
            onValueChange = {
                onOtpChange(it.filter { char -> char.isDigit() }.take(5))
            },
            label = { Text("OTP (5 chữ số)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(22.dp))
        BlueButton(
            title = "Next",
            enabled = otp.length == 5,
            onClick = onNext
        )
    }
}

// MÀN HÌNH 3: NHẬP MẬT KHẨU

@Composable
fun PasswordScreen(
    password: String,
    onPasswordChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    var confirmPassword by remember {
        mutableStateOf("")
    }

    // Trạng thái hiện / ẩn mật khẩu
    var showPassword by remember {
        mutableStateOf(false)
    }

    var showConfirmPassword by remember {
        mutableStateOf(false)
    }

    // Kiểm tra mật khẩu
    val validPassword =
        password.length >= 6 &&
                password == confirmPassword

    ScreenLayout(
        title = "Create new password",
        description = "Your new password must be different from previously used password",
        onBack = onBack
    ) {

        // Ô nhập mật khẩu mới
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },

            visualTransformation =
                if (showPassword)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),

            trailingIcon = {
                IconButton(
                    onClick = {
                        showPassword = !showPassword
                    }
                ) {
                    Icon(
                        imageVector =
                            if (showPassword)
                                Icons.Default.VisibilityOff
                            else
                                Icons.Default.Visibility,
                        contentDescription =
                            if (showPassword)
                                "Ẩn mật khẩu"
                            else
                                "Hiện mật khẩu"
                    )
                }
            },

            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        // Ô xác nhận mật khẩu
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
            },
            label = { Text("Confirm Password") },

            visualTransformation =
                if (showConfirmPassword)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),

            trailingIcon = {
                IconButton(
                    onClick = {
                        showConfirmPassword = !showConfirmPassword
                    }
                ) {
                    Icon(
                        imageVector =
                            if (showConfirmPassword)
                                Icons.Default.VisibilityOff
                            else
                                Icons.Default.Visibility,
                        contentDescription =
                            if (showConfirmPassword)
                                "Ẩn mật khẩu"
                            else
                                "Hiện mật khẩu"
                    )
                }
            },

            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Thông báo khi mật khẩu không trùng
        if (
            confirmPassword.isNotEmpty() &&
            password != confirmPassword
        ) {
            Text(
                text = "Mật khẩu xác nhận không trùng khớp",
                color = Color.Red,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(22.dp))

        BlueButton(
            title = "Next",
            enabled = validPassword,
            onClick = onNext
        )
    }
}


// MÀN HÌNH 4: XÁC NHẬN
@Composable
fun ConfirmScreen(
    info: ResetInfo,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    ScreenLayout(
        title = "Confirm",
        description = "We are here to help you!",
        onBack = onBack
    ) {
        OutlinedTextField(
            value = info.email,
            onValueChange = {},
            readOnly = true,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = info.otp,
            onValueChange = {},
            readOnly = true,
            label = { Text("OTP") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = "●".repeat(info.password.length),
            onValueChange = {},
            readOnly = true,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(22.dp))
        BlueButton(
            title = "Submit",
            onClick = onSubmit
        )
    }
}
