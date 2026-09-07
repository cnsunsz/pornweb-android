package com.pornweb.android.ui.login

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pornweb.android.PornWebApp
import com.pornweb.android.data.LoginRequest
import com.pornweb.android.data.RegisterRequest
import com.pornweb.android.ui.components.BrandLogo
import com.pornweb.android.ui.theme.PwAccent
import com.pornweb.android.ui.theme.PwBg
import com.pornweb.android.ui.theme.PwMuted
import com.pornweb.android.ui.theme.PwSurface
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    registerMode: Boolean,
    onLoggedIn: () -> Unit,
    onToggleRegister: () -> Unit,
    onChangeServer: () -> Unit
) {
    val app = LocalContext.current.applicationContext as PornWebApp
    val container = app.container
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val cardAlpha = remember { Animatable(0f) }
    val cardOffsetY = remember { Animatable(14f) }
    LaunchedEffect(Unit) {
        launch {
            cardAlpha.animateTo(
                1f,
                animationSpec = tween(550, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f))
            )
        }
        launch {
            cardOffsetY.animateTo(
                0f,
                animationSpec = tween(550, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f))
            )
        }
    }

    fun submit() {
        if (username.isBlank() || password.isBlank()) {
            error = "请输入用户名和密码"
            return
        }
        if (registerMode && email.isBlank()) {
            error = "请输入邮箱"
            return
        }
        if (password.length < 6 && registerMode) {
            error = "密码至少 6 位"
            return
        }
        scope.launch {
            busy = true
            error = null
            try {
                val resp = if (registerMode) {
                    container.api.register(RegisterRequest(username.trim(), email.trim(), password))
                } else {
                    container.api.login(LoginRequest(username.trim(), password))
                }
                val token = resp.accessToken
                if (token.isNullOrBlank()) {
                    error = "服务器未返回令牌"
                } else {
                    container.tokenStore.token = token
                    container.tokenStore.user = resp.user
                    container.serverStore.connected = true
                    onLoggedIn()
                }
            } catch (e: Exception) {
                error = container.parseError(e)
            } finally {
                busy = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PwBg)
    ) {
        // Subtle amber radial glow behind the card
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PwAccent.copy(alpha = 0.16f),
                            PwAccent.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        radius = 900f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(cardAlpha.value)
                    .offset(y = cardOffsetY.value.dp)
                    .background(PwSurface, RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(10.dp))
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BrandLogo(size = 40.dp, breathe = true)
                Text(
                    if (registerMode) "创建账户" else "登录媒体库",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PwMuted,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (registerMode) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("邮箱") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { submit() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(if (registerMode) "注册并登录" else "登录")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (registerMode) "已有账户？" else "没有账户？",
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = onToggleRegister) {
                        Text(if (registerMode) "去登录" else "注册")
                    }
                }
                TextButton(onClick = onChangeServer) { Text("更换服务器") }
                if (error != null) {
                    Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
