package com.pornweb.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pornweb.android.PornWebApp
import com.pornweb.android.ui.connect.ServerConnectScreen
import com.pornweb.android.ui.detail.DetailScreen
import com.pornweb.android.ui.home.HomeScreen
import com.pornweb.android.ui.library.LibraryScreen
import com.pornweb.android.ui.login.LoginScreen
import com.pornweb.android.ui.player.PlayerScreen
import com.pornweb.android.ui.search.SearchScreen
import com.pornweb.android.ui.settings.SettingsScreen
import com.pornweb.android.ui.theme.PwBg
import kotlinx.coroutines.flow.collectLatest

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("home", "首页", Icons.Default.Home),
    Tab("library", "媒体库", Icons.Default.GridView),
    Tab("search", "搜索", Icons.Default.Search),
    Tab("settings", "我的", Icons.Default.Person)
)

@Composable
fun PornWebRoot() {
    val app = LocalContext.current.applicationContext as PornWebApp
    val c = app.container
    val start = when {
        !c.serverStore.connected -> "connect"
        !c.tokenStore.hasToken() -> "login"
        else -> "home"
    }
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: start
    fun isTab(route: String?): Boolean {
        val r = route ?: return false
        return r == "home" || r.startsWith("library") || r == "search" || r == "settings"
    }

    LaunchedEffect(Unit) {
        c.unauthorized.collectLatest {
            nav.navigate("login") {
                popUpTo(nav.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(PwBg),
        containerColor = PwBg,
        bottomBar = {
            if (isTab(current)) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = if (tab.route == "library") current?.startsWith("library") == true else current == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(navController = nav, startDestination = start) {
                composable("connect") {
                    ServerConnectScreen(onConnected = {
                        nav.navigate("login") { popUpTo("connect") { inclusive = true } }
                    })
                }
                composable("login") {
                    LoginScreen(
                        registerMode = false,
                        onLoggedIn = {
                            nav.navigate("home") { popUpTo("login") { inclusive = true } }
                        },
                        onToggleRegister = { nav.navigate("register") },
                        onChangeServer = {
                            c.serverStore.connected = false
                            nav.navigate("connect") { popUpTo("login") { inclusive = true } }
                        }
                    )
                }
                composable("register") {
                    LoginScreen(
                        registerMode = true,
                        onLoggedIn = {
                            nav.navigate("home") { popUpTo("login") { inclusive = true } }
                        },
                        onToggleRegister = { nav.popBackStack() },
                        onChangeServer = {
                            c.serverStore.connected = false
                            nav.navigate("connect") { popUpTo("login") { inclusive = true } }
                        }
                    )
                }
                composable("home") {
                    HomeScreen(
                        onOpenMedia = { id -> nav.navigate("detail/$id") },
                        onOpenLibrary = { folder ->
                            val encoded = android.net.Uri.encode(folder ?: "")
                            nav.navigate("library?folder=$encoded")
                        }
                    )
                }
                composable(
                    route = "library?folder={folder}",
                    arguments = listOf(navArgument("folder") {
                        type = NavType.StringType
                        defaultValue = ""
                    })
                ) { entry ->
                    val folder = entry.arguments?.getString("folder")?.let {
                        android.net.Uri.decode(it).ifBlank { null }
                    }
                    LibraryScreen(initialFolder = folder, onOpenMedia = { id -> nav.navigate("detail/$id") })
                }
                composable("search") {
                    SearchScreen(onOpenMedia = { id -> nav.navigate("detail/$id") })
                }
                composable("settings") {
                    SettingsScreen(
                        onLoggedOut = {
                            nav.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        onEditServer = {
                            nav.navigate("connect")
                        }
                    )
                }
                composable(
                    route = "detail/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: 0L
                    DetailScreen(
                        id = id,
                        onBack = { nav.popBackStack() },
                        onPlay = { mediaId, part, resume ->
                            nav.navigate("player/$mediaId?part=$part&resume=${if (resume) 1 else 0}")
                        }
                    )
                }
                composable(
                    route = "player/{id}?part={part}&resume={resume}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType },
                        navArgument("part") { type = NavType.IntType; defaultValue = 0 },
                        navArgument("resume") { type = NavType.IntType; defaultValue = 1 }
                    )
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: 0L
                    val part = entry.arguments?.getInt("part") ?: 0
                    val resume = (entry.arguments?.getInt("resume") ?: 1) == 1
                    PlayerScreen(id = id, part = part, resume = resume, onBack = { nav.popBackStack() })
                }
            }
        }
    }
}
