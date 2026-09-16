package com.pornweb.android.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.pornweb.android.BuildConfig
import com.pornweb.android.PornWebApp
import com.pornweb.android.data.PasswordChangeRequest
import com.pornweb.android.data.User
import com.pornweb.android.ui.components.AccessRenewDialog
import com.pornweb.android.ui.components.AccessRenewForm
import com.pornweb.android.ui.components.AccessStatusBanner
import com.pornweb.android.ui.theme.PwMuted
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    var showRenew by remember { mutableStateOf(false) }
    var showDeleteAccount by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        c.refreshCurrentUser()
    }

    if (showRenew) {
        AccessRenewDialog(
            onDismiss = { showRenew = false },
            onActivated = { scope.launch { c.refreshCurrentUser() } }
        )
    }

    if (showDeleteAccount) {
        DeleteAccountConfirmDialog(
            onDismiss = { showDeleteAccount = false },
            onDeleted = {
                c.tokenStore.clear()
                onLoggedOut()
            }
        )
    }

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
        Spacer(Modifier.height(12.dp))
        AccessStatusBanner(user = user, onRenew = { showRenew = true })
        if (user != null) {
            MembershipAuthBlock(user = user!!)
            if (user?.isAdmin != true) {
                AccessRenewForm(
                    onActivated = { scope.launch { c.refreshCurrentUser() } }
                )
            }
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
        if (user != null) {
            Spacer(Modifier.height(32.dp))
            Text("账户", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (user?.isAdmin == true) {
                Text(
                    "管理员账户不可自行注销",
                    color = PwMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                OutlinedButton(
                    onClick = { showDeleteAccount = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("注销账户") }
                Text(
                    "注销后无法恢复，请谨慎操作",
                    color = PwMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
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
        Text(
            "PornWeb Android ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "视觉 v2.0.1 管站风",
            color = PwMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun MembershipAuthBlock(user: User) {
    val status: String
    val daysLeft: String
    val expires: String

    if (user.isAdmin == true) {
        status = "永久"
        daysLeft = "永久"
        expires = "管理员 · 永久"
    } else {
        val left = user.accessDaysLeft
        val expired = user.accessActive == false || left == 0
        val permanent = !expired && left == null && user.accessExpiresAt.isNullOrBlank()
        status = when {
            expired -> "已过期"
            permanent || (left == null && user.accessActive == true) -> "永久"
            else -> "有效"
        }
        daysLeft = when {
            expired -> "已到期"
            permanent || left == null -> "永久"
            else -> "${left} 天"
        }
        expires = when {
            permanent || (user.isAdmin == true) -> "永久"
            user.accessExpiresAt.isNullOrBlank() -> "—"
            else -> formatAccessExpiresAt(user.accessExpiresAt)
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("会员授权", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text("状态：$status", color = PwMuted, style = MaterialTheme.typography.bodyMedium)
        Text("剩余天数：$daysLeft", color = PwMuted, style = MaterialTheme.typography.bodyMedium)
        Text("到期时间：$expires", color = PwMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

/** ISO8601 → local `yyyy-MM-dd HH:mm` (Asia/Shanghai); fallback to substring. */
private fun formatAccessExpiresAt(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return "—"
    return try {
        val instant = Instant.parse(trimmed)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.of("Asia/Shanghai"))
        formatter.format(instant)
    } catch (_: Exception) {
        try {
            // e.g. 2026-09-16T12:00:00+08:00 or without Z
            val normalized = when {
                trimmed.length >= 19 && (trimmed[10] == 'T' || trimmed[10] == ' ') ->
                    trimmed.substring(0, 19).replace('T', ' ').take(16)
                trimmed.length >= 16 -> trimmed.take(16).replace('T', ' ')
                else -> trimmed
            }
            normalized
        } catch (_: Exception) {
            trimmed
        }
    }
}


@Composable
private fun DeleteAccountConfirmDialog(
    onDismiss: () -> Unit,
    onDeleted: () -> Unit
) {
    val app = LocalContext.current.applicationContext as PornWebApp
    val c = app.container
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (password.isBlank()) {
            error = "请输入当前密码以确认注销"
            return
        }
        scope.launch {
            busy = true
            error = null
            try {
                val resp = c.deleteAccount(password)
                val msg = resp.message?.takeIf { it.isNotBlank() } ?: "账户已注销"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                onDeleted()
                onDismiss()
            } catch (e: Exception) {
                error = c.parseError(e)
            } finally {
                busy = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("注销账户") },
        text = {
            Column {
                Text(
                    "此操作不可恢复。请输入当前密码以确认注销。",
                    color = PwMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("当前密码") },
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { submit() },
                enabled = !busy,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("确认注销")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun TextButtonLike(onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text("重新测试连接")
    }
}
