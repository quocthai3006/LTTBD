package com.example.btap1_tuan04

import android.content.MutableContextWrapper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.compose.ui.res.stringResource

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onGuest: () -> Unit) {
    val context = LocalContext.current
    val webClientId = stringResource(R.string.default_web_client_id)
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White)
            .systemBarsPadding().padding(horizontal = 26.dp, vertical = 20.dp)
    ) {
        Spacer(Modifier.weight(1f))
        ChecklistLogo()
        Spacer(Modifier.height(25.dp))
        Text("Chào mừng đến với\nUTH Todo", fontSize = 27.sp, lineHeight = 33.sp,
            fontWeight = FontWeight.Bold, color = TodoPalette.Ink)
        Spacer(Modifier.height(11.dp))
        Text("Đăng nhập để quản lý công việc và theo dõi tiến độ của bạn.",
            fontSize = 13.sp, lineHeight = 20.sp, color = TodoPalette.Muted)
        Spacer(Modifier.weight(1.25f))

        if (errorMessage != null) {
            Text(errorMessage.orEmpty(), color = TodoPalette.Red, fontSize = 12.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }

        OutlinedButton(
            onClick = {
                if (!isLoading) {
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val googleOption = GetSignInWithGoogleOption.Builder(
                                webClientId
                            ).build()
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleOption).build()
                            val response = credentialManager.getCredential(
                                context = MutableContextWrapper(context), request = request)
                            val credential = response.credential
                            if (credential !is CustomCredential || credential.type !=
                                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                error("Thông tin đăng nhập không hợp lệ.")
                            }
                            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            authenticateGoogleWithFirebase(googleCredential.idToken)
                            Toast.makeText(context, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        } catch (e: GetCredentialCancellationException) {
                            errorMessage = "Bạn đã hủy đăng nhập."
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e("UTH_AUTH", "Đăng nhập thất bại", e)
                            errorMessage = "Đăng nhập thất bại. Vui lòng thử lại."
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(51.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, TodoPalette.Line),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TodoPalette.Ink)
        ) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Đang đăng nhập...", fontSize = 13.sp)
            } else {
                Box(Modifier.size(23.dp).background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text("G", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
                }
                Spacer(Modifier.width(11.dp))
                Text("Tiếp tục với Google", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(15.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = TodoPalette.Line)
            Text("hoặc", color = TodoPalette.Muted, fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 15.dp))
            HorizontalDivider(Modifier.weight(1f), color = TodoPalette.Line)
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onGuest, enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(45.dp)) {
            Text("Dùng thử không cần tài khoản", color = TodoPalette.Blue,
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(26.dp))
        Text("UTH Todo\nSắp xếp công việc, dành thời gian cho điều bạn yêu thích.",
            color = TodoPalette.Muted, fontSize = 9.sp, lineHeight = 14.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

private suspend fun authenticateGoogleWithFirebase(idToken: String) {
    val credential = GoogleAuthProvider.getCredential(idToken, null)
    suspendCancellableCoroutine<Unit> { continuation ->
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (continuation.isActive) {
                    if (task.isSuccessful) continuation.resume(Unit)
                    else continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Firebase không thể xác thực tài khoản.")
                    )
                }
            }
    }
}