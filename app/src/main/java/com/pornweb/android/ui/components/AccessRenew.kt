package com.pornweb.android.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.pornweb.android.PornWebApp
import com.pornweb.android.data.User
import com.pornweb.android.ui.theme.PwMuted
import kotlinx.coroutines.launch

@Composable
fun AccessStatusBanner(
    user: User?,
    onRenew: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (user == null || user.isAdmin == true) return
    val expired = user.needsAccessRenew()
    val soon = user.accessExpiringSoon()
    if (!expired && !soon) return

    val bg = if (expired) Color(0xFF4A1515) else Color(0xFF3D2E0A)
    val title = if (expired) {
        "授权已过期"
    } else {
        val days = user.accessDaysLeft ?: 0
        "授权即将到期（剩余 $days 天）"
    }
    val subtitle = if (expired) {
        "媒体无法播放，请使用新的授权码续期"
    } else {
        "建议尽快续期，以免影响观看"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
        }
        Button(onClick = onRenew) {
            Text(if (expired) "续期" else "续期")
        }
    }
}

@Composable
fun AccessRenewDialog(
    onDismiss: () -> Unit,
    onActivated: () -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as PornWebApp
    val c = app.container
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val normalized = c.normalizeInviteCode(code)
        if (normalized.isBlank()) {
            error = "请输入授权码"
            return
        }
        scope.launch {
            busy = true
            error = null
            try {
                val resp = c.activateAccess(normalized)
                val msg = resp.message?.takeIf { it.isNotBlank() } ?: "续期成功"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                onActivated()
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
        title = { Text("授权续期") },
        text = {
            Column {
                Text(
                    "输入管理员发放的授权码以续期访问权限",
                    color = PwMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("授权码") },
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (visible) "隐藏授权码" else "显示授权码"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "无连字符大小写均可",
                    color = PwMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
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
            Button(onClick = { submit() }, enabled = !busy) {
                if (busy) CircularProgressIndicator(modifier = Modifier.padding(0.dp).then(Modifier), strokeWidth = 2.dp)
                else Text("续期")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") }
        }
    )
}

/** Inline renew CTA when a screen got an access-expired error. */
@Composable
fun AccessExpiredErrorPanel(
    message: String,
    onRenew: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedButton(onClick = onRenew) { Text("使用授权码续期") }
    }
}
