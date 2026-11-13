package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.EndoPulseDatabase
import com.example.data.local.EndoPulseRepository
import com.example.ui.screens.ChallengesScreen
import com.example.ui.screens.DailyLogScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ResearchLabScreen
import com.example.ui.screens.TrendsSimulatorScreen
import com.example.ui.theme.EndoPulseTheme
import com.example.ui.viewmodel.EndoPulseViewModel

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_CHALLENGES = "challenges"
const val ROUTE_LOG = "daily_log"
const val ROUTE_TRENDS = "trends_simulator"
const val ROUTE_RESEARCH = "research_lab"

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val appDestinations = listOf(
    NavDestinationItem(
        route = ROUTE_DASHBOARD,
        labelRes = R.string.nav_dashboard,
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder
    ),
    NavDestinationItem(
        route = ROUTE_CHALLENGES,
        labelRes = R.string.nav_challenges,
        selectedIcon = Icons.Filled.EmojiEvents,
        unselectedIcon = Icons.Outlined.EmojiEvents
    ),
    NavDestinationItem(
        route = ROUTE_LOG,
        labelRes = R.string.nav_log,
        selectedIcon = Icons.Filled.AddCircle,
        unselectedIcon = Icons.Outlined.AddCircleOutline
    ),
    NavDestinationItem(
        route = ROUTE_TRENDS,
        labelRes = R.string.nav_trends,
        selectedIcon = Icons.Filled.Insights,
        unselectedIcon = Icons.Outlined.Insights
    ),
    NavDestinationItem(
        route = ROUTE_RESEARCH,
        labelRes = R.string.nav_research,
        selectedIcon = Icons.Filled.Science,
        unselectedIcon = Icons.Outlined.Science
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = EndoPulseDatabase.getInstance(applicationContext)
        val repository = EndoPulseRepository(database.endoPulseDao())
        val factory = EndoPulseViewModel.Factory(repository)

        setContent {
            EndoPulseTheme {
                val vm: EndoPulseViewModel = viewModel(factory = factory)
                EndoPulseApp(viewModel = vm)
            }
        }
    }
}

@Composable
fun EndoPulseApp(viewModel: EndoPulseViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    LaunchedEffect(state.exportedJson) {
        state.exportedJson?.let { jsonPayload ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "EndoPulse Local Backup")
                putExtra(Intent.EXTRA_TEXT, jsonPayload)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export EndoPulse Local Data"))
            viewModel.consumeExportedJson()
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        appDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        appDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_DASHBOARD,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_DASHBOARD) {
                        DashboardScreen(
                            state = state,
                            onSelectDate = viewModel::selectDate,
                            onNavigateToCheckIn = { navigateTo(ROUTE_LOG) },
                            onNavigateToChallenges = { navigateTo(ROUTE_CHALLENGES) },
                            onNavigateToSimulator = { navigateTo(ROUTE_TRENDS) }
                        )
                    }
                    composable(ROUTE_CHALLENGES) {
                        ChallengesScreen(
                            state = state,
                            onNavigateToCheckIn = { navigateTo(ROUTE_LOG) }
                        )
                    }
                    composable(ROUTE_LOG) {
                        DailyLogScreen(
                            state = state,
                            onUpdateDraft = viewModel::updateDraft,
                            onUpdateBedtime = viewModel::updateDraftBedtime,
                            onUpdateWakeTime = viewModel::updateDraftWakeTime,
                            onUpdateLastCaffeineTime = viewModel::updateDraftLastCaffeineTime,
                            onSaveDraft = {
                                viewModel.saveCurrentDraft()
                                navigateTo(ROUTE_DASHBOARD)
                            },
                            onToggleCustomHabit = viewModel::toggleCustomHabit,
                            onAddCustomHabit = { title, pathway, weight, reminder, hour, min ->
                                viewModel.addCustomHabit(context, title, pathway, weight, reminder, hour, min)
                            },
                            onUpdateCustomHabit = { habit ->
                                viewModel.updateCustomHabit(context, habit)
                            },
                            onDeleteCustomHabit = { habitId ->
                                viewModel.deleteCustomHabit(context, habitId)
                            },
                            onShowMessage = viewModel::showMessage
                        )
                    }
                    composable(ROUTE_TRENDS) {
                        TrendsSimulatorScreen(
                            state = state,
                            onSetTrendRange = viewModel::setTrendRange,
                            onUpdateSimulation = viewModel::updateSimulation,
                            onApplyPreset = viewModel::applySimulationPreset
                        )
                    }
                    composable(ROUTE_RESEARCH) {
                        ResearchLabScreen(
                            loggedDaysCount = state.allLogs.size,
                            onExportJson = viewModel::requestJsonExport,
                            onClearAllData = { viewModel.clearAllLocalData(context) }
                        )
                    }
                }
            }
        }
    }
}
