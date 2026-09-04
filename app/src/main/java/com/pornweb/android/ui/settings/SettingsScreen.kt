package com.pornweb.android.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pornweb.android.PornWebApp
import com.pornweb.android.data.PasswordChangeRequest
import com.pornweb.android.ui.theme.PwMuted
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onLoggedOut: () -> Unit, onEditServer: () -> Unit, onPlaybackSettings: () -> Unit = {}) {
    val app = LocalContext.current.applicationContext as PornWebApp
    val c = app.container
    val user by c.tokenStore.userFlow.collectAsState()
    var server by remember { mutableStateOf(c.serverStore.baseUrl) }
    var oldPw by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("我的", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(user?.username ?: "未登录", style = MaterialTheme.typography.titleMedium)
        if (!user?.email.isNullOrBlank()) {
            Text(user?.email ?: "", color = PwMuted, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))
        Text("服务器", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = server,
            onValueChange = { server = it },
            label = { Text("服务器地址") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                c.serverStore.baseUrl = server
                message = "已保存服务器地址"
                error = null
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("保存地址") }
        TextButtonLike(onEditServer)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onPlaybackSettings, modifier = Modifier.fillMaxWidth()) {
            Text("播放设置")
        }
        Spacer(Modifier.height(24.dp))
        Text("修改密码", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = oldPw,
            onValueChange = { oldPw = it },
            label = { Text("当前密码") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = newPw,
            onValueChange = { newPw = it },
            label = { Text("新密码（至少 6 位）") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                scope.launch {
                    error = null
                    message = null
                    if (newPw.length < 6) {
                        error = "新密码至少 6 位"
                        return@launch
                    }
                    try {
                        val resp = c.api.changePassword(PasswordChangeRequest(oldPw, newPw))
                        if (resp.isSuccessful) {
                            message = "密码已更新"
                            oldPw = ""
                            newPw = ""
                        } else {
                            error = "修改失败 (${resp.code()})"
                        }
                    } catch (e: Exception) {
                        error = c.parseError(e)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("更新密码") }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                c.tokenStore.clear()
                onLoggedOut()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("退出登录") }
        if (message != null) Text(message!!, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(24.dp))
        Text("PornWeb Android  1.0.6", color = PwMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TextButtonLike(onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text("重新测试连接")
    }
}
