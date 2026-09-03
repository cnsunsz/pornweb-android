package com.pornweb.android.ui.connect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pornweb.android.PornWebApp
import com.pornweb.android.data.ServerStore
import com.pornweb.android.ui.theme.PwAccent
import kotlinx.coroutines.launch

@Composable
fun ServerConnectScreen(onConnected: () -> Unit) {
    val app = LocalContext.current.applicationContext as PornWebApp
    val container = app.container
    var url by remember { mutableStateOf(container.serverStore.baseUrl.ifBlank { ServerStore.DEFAULT_URL }) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun run(connectAfter: Boolean) {
        scope.launch {
            busy = true
            message = null
            try {
                container.serverStore.baseUrl = url
                val health = container.api.health()
                val ok = health.status.equals("ok", true) || !health.app.isNullOrBlank()
                if (ok) {
                    message = "已连接到 ${health.app ?: "服务器"}"
                    if (connectAfter) {
                        container.serverStore.connected = true
                        onConnected()
                    }
                } else {
                    message = "服务器响应异常"
                }
            } catch (e: Exception) {
                message = container.parseError(e)
            } finally {
                busy = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PwAccent, modifier = Modifier.size(64.dp))
        Text("PornWeb", style = MaterialTheme.typography.headlineLarge, color = PwAccent)
        Text("连接媒体服务器", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("服务器地址") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(ServerStore.DEFAULT_URL) }
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { run(false) },
            enabled = !busy && url.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("测试连接") }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { run(true) },
            enabled = !busy && url.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("连接")
        }
        if (message != null) {
            Text(message!!, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
