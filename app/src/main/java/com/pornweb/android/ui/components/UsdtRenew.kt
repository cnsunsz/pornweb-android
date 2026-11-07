package com.pornweb.android.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.pornweb.android.data.PaymentPlan
import com.pornweb.android.ui.theme.PwMuted
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val POLL_INTERVAL_MS = 3_000L
private const val POLL_TIMEOUT_MS = 5 * 60_000L

private const val METHOD_USDT = "usdt"
private const val METHOD_PAYPAL = "paypal"

/** Normalize server methods; null/empty → usdt-only (old servers). */
private fun normalizeMethods(raw: List<String>?): List<String> {
    val cleaned = raw.orEmpty()
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .distinct()
    return if (cleaned.isEmpty()) listOf(METHOD_USDT) else cleaned
}

private fun renewTitle(methods: List<String>): String =
    if (METHOD_PAYPAL in methods) "在线续费" else "USDT 续费"

private fun renewBlurb(methods: List<String>, selected: String): String = when {
    METHOD_PAYPAL in methods && selected == METHOD_PAYPAL ->
        "使用 PayPal 付费开通/续期。下单后将打开支付页，到账后自动刷新权限。"
    METHOD_PAYPAL in methods ->
        "可选 USDT(TRC20) 或 PayPal。下单后将打开支付页，到账后自动刷新权限。"
    else ->
        "使用 USDT(TRC20) 付费开通/续期。下单后将打开支付页，到账后自动刷新权限。"
}

@Composable
fun UsdtRenewSection(
    onPaid: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    UsdtRenewContent(
        onPaid = onPaid,
        modifier = modifier.then(Modifier),
        showTitle = true
    )
}

@Composable
fun UsdtRenewDialog(
    onDismiss: () -> Unit,
    onPaid: () -> Unit = {}
) {
    var title by remember { mutableStateOf("USDT 续费") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            UsdtRenewContent(
                onPaid = {
                    onPaid()
                    onDismiss()
                },
                showTitle = false,
                scrollable = true,
                onTitleChange = { title = it }
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun UsdtRenewContent(
    onPaid: () -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    scrollable: Boolean = false,
    onTitleChange: ((String) -> Unit)? = null
) {
    val app = LocalContext.current.applicationContext as PornWebApp
    val c = app.container
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var plans by remember { mutableStateOf<List<PaymentPlan>>(emptyList()) }
    var currency by remember { mutableStateOf("USDT") }
    var methods by remember { mutableStateOf(listOf(METHOD_USDT)) }
    var selectedMethod by remember { mutableStateOf(METHOD_USDT) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var orderingPlanId by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var pollJob by remember { mutableStateOf<Job?>(null) }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    DisposableEffect(Unit) {
        onDispose { stopPolling() }
    }

    fun applyMethods(raw: List<String>?) {
        val m = normalizeMethods(raw)
        methods = m
        if (selectedMethod !in m) {
            selectedMethod = m.first()
        }
        onTitleChange?.invoke(renewTitle(m))
    }

    fun loadPlans() {
        scope.launch {
            loading = true
            loadError = null
            try {
                val resp = c.fetchPaymentPlans()
                plans = resp.plans.orEmpty()
                currency = resp.currency?.takeIf { it.isNotBlank() } ?: "USDT"
                applyMethods(resp.methods)
                if (plans.isEmpty()) {
                    loadError = "暂无可用套餐"
                }
            } catch (e: Exception) {
                loadError = c.parseError(e)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadPlans() }

    fun startPolling(orderId: String, method: String) {
        stopPolling()
        statusText = "等待支付确认中…"
        val started = System.currentTimeMillis()
        pollJob = scope.launch {
            while (isActive) {
                if (System.currentTimeMillis() - started > POLL_TIMEOUT_MS) {
                    statusText = "支付超时，请稍后在「我的」查看或重新下单"
                    orderingPlanId = null
                    break
                }
                try {
                    val st = c.paymentOrderStatus(orderId)
                    val status = st.status?.lowercase()?.trim().orEmpty()
                    when (status) {
                        "paid" -> {
                            statusText = "支付成功"
                            val toast = if (method == METHOD_PAYPAL) "PayPal 续费成功" else "USDT 续费成功"
                            Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
                            c.refreshCurrentUser()
                            orderingPlanId = null
                            onPaid()
                            break
                        }
                        "expired" -> {
                            statusText = "订单已过期，请重新下单"
                            orderingPlanId = null
                            break
                        }
                        else -> {
                            statusText = "订单状态：${st.status ?: "pending"}（轮询中）"
                        }
                    }
                } catch (e: Exception) {
                    statusText = "查询订单失败：${c.parseError(e)}"
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun createOrder(plan: PaymentPlan, method: String) {
        val planId = plan.id?.trim().orEmpty()
        if (planId.isEmpty()) {
            statusText = "套餐无效"
            return
        }
        scope.launch {
            orderingPlanId = planId
            statusText = "正在创建订单…"
            try {
                val resp = c.createPaymentOrder(planId, method = method)
                val orderId = resp.orderId?.trim().orEmpty()
                val payUrl = resp.payUrl?.trim().orEmpty()
                if (orderId.isEmpty() || payUrl.isEmpty()) {
                    statusText = "下单失败：未返回支付链接"
                    orderingPlanId = null
                    return@launch
                }
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(payUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                } catch (e: Exception) {
                    statusText = "无法打开支付页：${e.message ?: "未知错误"}"
                    orderingPlanId = null
                    return@launch
                }
                statusText = if (method == METHOD_PAYPAL) {
                    "已打开支付页，等待 PayPal 确认…"
                } else {
                    "已打开支付页，等待链上确认…"
                }
                startPolling(orderId, method)
            } catch (e: Exception) {
                statusText = c.parseError(e)
                orderingPlanId = null
            }
        }
    }

    val baseModifier = if (scrollable) {
        modifier.fillMaxWidth().verticalScroll(rememberScrollState())
    } else {
        modifier.fillMaxWidth()
    }
    val showMethodPicker = methods.size > 1
    val title = renewTitle(methods)

    Column(modifier = baseModifier) {
        if (showTitle) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
        }
        Text(
            renewBlurb(methods, selectedMethod),
            color = PwMuted,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))

        if (showMethodPicker && !loading) {
            Text("支付方式", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (METHOD_USDT in methods) {
                    FilterChip(
                        selected = selectedMethod == METHOD_USDT,
                        onClick = { selectedMethod = METHOD_USDT },
                        label = { Text("USDT") },
                        enabled = orderingPlanId == null
                    )
                }
                if (METHOD_PAYPAL in methods) {
                    FilterChip(
                        selected = selectedMethod == METHOD_PAYPAL,
                        onClick = { selectedMethod = METHOD_PAYPAL },
                        label = { Text("PayPal") },
                        enabled = orderingPlanId == null
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        when {
            loading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("加载套餐…", color = PwMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            loadError != null && plans.isEmpty() -> {
                Text(
                    loadError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { loadPlans() }, modifier = Modifier.fillMaxWidth()) {
                    Text("重试")
                }
            }
            else -> {
                val amountCurrency = if (selectedMethod == METHOD_PAYPAL && currency.equals("USDT", true)) {
                    // Server may still report USDT currency for plans; PayPal checkout uses its own currency.
                    currency
                } else {
                    currency
                }
                plans.forEach { plan ->
                    val planId = plan.id.orEmpty()
                    val days = plan.days ?: 0
                    val amount = plan.amount
                    val amountLabel = formatAmount(amount)
                    val methodTag = if (showMethodPicker) {
                        if (selectedMethod == METHOD_PAYPAL) " · PayPal" else " · USDT"
                    } else {
                        ""
                    }
                    val label = "$planId · ${days}天 · $amountLabel $amountCurrency$methodTag"
                    val busy = orderingPlanId != null
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = { createOrder(plan, selectedMethod) },
                        enabled = !busy && planId.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (orderingPlanId == planId) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(label)
                        }
                    }
                }
            }
        }

        if (statusText != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                statusText!!,
                color = PwMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatAmount(amount: Double?): String {
    if (amount == null) return "—"
    val asLong = amount.toLong()
    return if (amount == asLong.toDouble()) asLong.toString() else amount.toString()
}
