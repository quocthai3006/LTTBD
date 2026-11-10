package com.example.btap1_tuan04

import android.util.Log
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import kotlinx.coroutines.delay

private val MainBlue = Color(0xFF3C5CE3)
private val LightBlue = Color(0xFFECEFFD)
private val DarkText = Color(0xFF202124)

@Composable
fun TodoNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var isSigningOut by remember { mutableStateOf(false) }
    val store = remember(currentUser?.uid) { TodoTaskStore(context, currentUser?.uid) }
    DisposableEffect(store) {
        store.startSync()
        onDispose { store.stopSync() }
    }
    val pages = TodoRepository.pages
    val introRoutes = arrayOf(Screen.INTRO1.route, Screen.INTRO2.route, Screen.INTRO3.route)

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { currentUser = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    // Hoàn tất đăng nhập/dùng thử: xóa các màn hình giới thiệu khỏi lịch sử.
    fun openHome() {
        navController.navigate(Screen.HOME.route) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    // Ngăn quay lại vào lúc đang xóa phiên đăng nhập.
    BackHandler(enabled = isSigningOut) { }

    UthTodoTheme {
        NavHost(navController = navController, startDestination = Screen.SPLASH.route) {
            composable(Screen.SPLASH.route) {
                SplashScreen(onFinish = {
                    val destination = if (auth.currentUser != null) Screen.HOME.route else introRoutes[0]
                    navController.navigate(destination) {
                        popUpTo(Screen.SPLASH.route) { inclusive = true }
                    }
                })
            }

            introRoutes.forEachIndexed { index, route ->
                composable(route) {
                    OnboardingScreen(
                        page = pages[index], pageIndex = index, pageCount = pages.size,
                        onNext = {
                            if (index < pages.lastIndex) {
                                navController.navigate(introRoutes[index + 1])
                            } else {
                                navController.navigate("login") {
                                    popUpTo(introRoutes[0]) { inclusive = true }
                                }
                            }
                        },
                        onSkip = {
                            navController.navigate("login") {
                                popUpTo(introRoutes[0]) { inclusive = true }
                            }
                        }
                    )
                }
            }

            composable("login") {
                LoginScreen(
                    onLoginSuccess = { currentUser = auth.currentUser; openHome() },
                    onGuest = { openHome() }
                )
            }

            composable(Screen.HOME.route) {
                HomeScreen(user = currentUser, store = store,
                    onOpenProfile = { navController.navigate("profile") { launchSingleTop = true } },
                    onOpenTask = { id -> navController.navigate("task/${Uri.encode(id)}") { launchSingleTop = true } },
                    onAddTask = { navController.navigate("task/new") { launchSingleTop = true } })
            }

            composable("task/new") {
                TaskFormScreen(store, editingId = null,
                    onCancel = { navController.popBackStack() },
                    onSaved = { id ->
                        navController.navigate("task/${Uri.encode(id)}") {
                            popUpTo("task/new") { inclusive = true }
                            launchSingleTop = true
                        }
                    })
            }
            composable("task/edit/{taskId}", arguments = listOf(navArgument("taskId") { type = NavType.StringType })) { entry ->
                TaskFormScreen(store, editingId = requireNotNull(entry.arguments?.getString("taskId")),
                    onCancel = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
            }
            composable("task/{taskId}", arguments = listOf(navArgument("taskId") { type = NavType.StringType })) { entry ->
                val id = requireNotNull(entry.arguments?.getString("taskId"))
                TaskDetailScreen(id, store, onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate("task/edit/${Uri.encode(id)}") { launchSingleTop = true } })
            }

            composable("profile") {
                // Giữ nguyên tên và thống kê đang xem trong lúc đăng xuất.
                val profileUser = remember { currentUser }
                val profileStore = remember { store }
                ProfileScreen(
                    user = profileUser, store = profileStore, isSigningOut = isSigningOut,
                    onBack = { navController.popBackStack() },
                    onSignOut = {
                        if (!isSigningOut) {
                            isSigningOut = true
                            val wasSignedIn = auth.currentUser != null
                            scope.launch {
                                try {
                                    if (wasSignedIn) profileStore.deactivateReminders()
                                    auth.signOut()
                                    currentUser = null
                                    try {
                                        withTimeoutOrNull(3000L) {
                                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                        }
                                    } catch (e: ClearCredentialException) {
                                        Log.w("UTH_AUTH", "Không thể xóa trạng thái Credential Manager", e)
                                    }
                                    if (wasSignedIn) Toast.makeText(context,
                                        "Đăng xuất thành công", Toast.LENGTH_SHORT).show()
                                    navController.navigate("login") {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } finally {
                                    isSigningOut = false
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SplashScreen(onFinish: () -> Unit) {

    LaunchedEffect(Unit) {
        delay(1000L)
        onFinish()
    }

    val app = TodoRepository.appInfo

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBlue)
            .padding(24.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.weight(1f))

        AppLogo(large = true)

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = app.name,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = app.slogan,
            color = Color.White,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .width(75.dp)
                .height(3.dp)
                .background(Color.White)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = app.version,
            color = Color.White,
            fontSize = 11.sp
        )
    }
}

@Composable
fun AppLogo(large: Boolean) {

    val size = if (large) 70.dp else 28.dp

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(if (large) 18.dp else 7.dp))
            .background(
                if (large) Color.White else MainBlue
            ),

        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✓",
            color = if (large) MainBlue else Color.White,
            fontSize = if (large) 36.sp else 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun OnboardingScreen(
    page: OnboardingItem,
    pageIndex: Int,
    pageCount: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 18.dp)
    ) {

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppLogo(large = false)

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = TodoRepository.appInfo.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
            }

            TextButton(onClick = onSkip) {
                Text(
                    text = TodoRepository.appInfo.skipText,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Hình minh họa
        OnboardingIllustration(page)

        Spacer(modifier = Modifier.weight(1f))

        // Tiêu đề
        Text(
            text = page.title,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 28.sp,
            color = DarkText
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Mô tả
        Text(
            text = page.description,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Thanh chỉ số trang
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(pageCount) { index ->
                Box(
                    modifier = Modifier
                        .width(
                            if (index == pageIndex) 26.dp else 7.dp
                        )
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pageIndex) MainBlue
                            else Color.LightGray
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Nút chuyển trang
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MainBlue
            )
        ) {
            Text(
                text = page.buttonText,
                color = Color.White,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun OnboardingIllustration(page: OnboardingItem) {

    Box(
        modifier = Modifier
            .size(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(LightBlue),
        contentAlignment = Alignment.Center
    ) {
        when (page.illustration) {

            IllustrationType.CHECKLIST -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    page.previewTexts.forEach { item ->
                        Text(
                            text = "☑  $item",
                            color = MainBlue,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            IllustrationType.CATEGORIES -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    page.previewTexts.forEach { item ->
                        Text(
                            text = item,
                            color = DarkText,
                            fontSize = 17.sp
                        )
                    }
                }
            }

            IllustrationType.REMINDER -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔔",
                        fontSize = 45.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = page.previewTexts.firstOrNull().orEmpty(),
                        color = DarkText,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
