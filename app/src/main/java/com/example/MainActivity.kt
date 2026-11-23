package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.WorkHistory
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.entity.AdvanceEntity
import com.example.data.entity.WorkEntryEntity
import com.example.data.entity.WorkerEntity
import com.example.ui.components.AddAdvanceDialog
import com.example.ui.components.AddEditWorkDialog
import com.example.ui.components.AddEditWorkerDialog
import com.example.ui.components.AddPaymentDialog
import com.example.ui.components.AdvanceReceiptDialog
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.HisabNavyTopBar
import com.example.ui.components.QuickActionChooserSheet
import com.example.ui.i18n.LocalStrings
import com.example.ui.i18n.stringsFor
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GlobalSearchDialog
import com.example.ui.screens.PaymentsScreen
import com.example.ui.screens.PinLockScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WorkRegisterScreen
import com.example.ui.screens.WorkerDetailScreen
import com.example.ui.screens.WorkersScreen
import com.example.ui.theme.GoldBright
import com.example.ui.theme.HisabBookTheme
import com.example.ui.theme.LocalHisabColors
import com.example.ui.theme.NavyDeep
import com.example.ui.viewmodel.HisabViewModel
import com.example.util.IndiaFormatters

object AppRoutes {
    const val HOME = "home"
    const val WORKERS = "workers"
    const val WORK_REGISTER = "work_register"
    const val PAYMENTS = "payments"
    const val REPORTS = "reports"
    const val ATTENDANCE = "attendance"
    const val SETTINGS = "settings"
    const val REMINDERS = "reminders"
    const val WORKER_DETAIL = "worker_detail/{workerId}"

    fun workerDetail(workerId: Long): String = "worker_detail/$workerId"
}

private data class BottomTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: HisabViewModel = viewModel()
            HisabBookRoot(viewModel = viewModel)
        }
    }
}

@Composable
fun HisabBookRoot(viewModel: HisabViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    val strings = remember(settings.language) { stringsFor(settings.language) }

    HisabBookTheme(themeMode = settings.themeMode) {
        CompositionLocalProvider(LocalStrings provides strings) {
            if (isLocked && settings.pinEnabled) {
                PinLockScreen(
                    businessName = settings.businessName,
                    biometricEnabled = settings.biometricEnabled,
                    onVerifyPin = { viewModel.unlockWithPin(it) },
                    onBiometricSuccess = { viewModel.unlockWithBiometricSuccess() }
                )
            } else {
                HisabMainScaffold(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun HisabMainScaffold(viewModel: HisabViewModel) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current
    val context = LocalContext.current

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val workers by viewModel.workers.collectAsStateWithLifecycle()
    val workEntries by viewModel.workEntries.collectAsStateWithLifecycle()
    val attendanceRecords by viewModel.attendanceRecords.collectAsStateWithLifecycle()
    val advances by viewModel.advances.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val ledgers by viewModel.workerLedgers.collectAsStateWithLifecycle()
    val dashboardSummary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppRoutes.HOME

    // Global modal states
    var showQuickSheet by remember { mutableStateOf(false) }
    var showGlobalSearch by remember { mutableStateOf(false) }
    var editingWorker by remember { mutableStateOf<WorkerEntity?>(null) }
    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var editingWorkEntry by remember { mutableStateOf<WorkEntryEntity?>(null) }
    var showAddWorkDialogForWorker by remember { mutableStateOf<Pair<Boolean, Long?>>(false to null) }
    var showAddAdvanceForWorker by remember { mutableStateOf<Pair<Boolean, Long?>>(false to null) }
    var showAddPaymentForWorker by remember { mutableStateOf<Pair<Boolean, Long?>>(false to null) }
    var viewingReceiptAdvance by remember { mutableStateOf<AdvanceEntity?>(null) }

    var pendingDeleteWorkId by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteAdvanceId by remember { mutableStateOf<Long?>(null) }
    var pendingDeletePaymentId by remember { mutableStateOf<Long?>(null) }

    fun ensureWorkerOrPrompt(action: () -> Unit) {
        if (workers.isEmpty()) {
            Toast.makeText(context, strings.noWorkersTitle, Toast.LENGTH_SHORT).show()
            editingWorker = null
            showAddWorkerDialog = true
        } else {
            action()
        }
    }

    val bottomTabs = remember(strings) {
        listOf(
            BottomTabItem(AppRoutes.HOME, strings.navHome, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
            BottomTabItem(AppRoutes.WORKERS, strings.navWorkers, Icons.Filled.Groups, Icons.Outlined.Groups, "nav_workers"),
            BottomTabItem(AppRoutes.WORK_REGISTER, strings.navWork, Icons.Filled.WorkHistory, Icons.Outlined.WorkHistory, "nav_work"),
            BottomTabItem(AppRoutes.PAYMENTS, strings.navPayments, Icons.Filled.Payments, Icons.Outlined.Payments, "nav_payments"),
            BottomTabItem(AppRoutes.REPORTS, strings.navReports, Icons.Filled.Assessment, Icons.Outlined.Assessment, "nav_reports")
        )
    }

    val isTopLevelTab = currentRoute in bottomTabs.map { it.route }
    val pendingAlertCount = remember(workers, ledgers, reminders) {
        val pendingBalWorkers = ledgers.values.count { it.remainingBalancePaise > 0L }
        val activeRem = reminders.count { !it.isCompleted }
        pendingBalWorkers + activeRem
    }

    val topBarTitle = when {
        currentRoute == AppRoutes.HOME -> settings.businessName
        currentRoute == AppRoutes.WORKERS -> strings.workersTitle
        currentRoute == AppRoutes.WORK_REGISTER -> strings.workRegisterTitle
        currentRoute == AppRoutes.PAYMENTS -> strings.paymentsTitle
        currentRoute == AppRoutes.REPORTS -> strings.reportsTitle
        currentRoute == AppRoutes.ATTENDANCE -> strings.attendanceTitle
        currentRoute == AppRoutes.SETTINGS -> strings.settingsTitle
        currentRoute == AppRoutes.REMINDERS -> strings.remindersTitle
        currentRoute.startsWith("worker_detail/") -> strings.workerProfile
        else -> strings.appName
    }

    val topBarSubtitle = if (currentRoute == AppRoutes.HOME) {
        "${strings.appName} • INR (₹)"
    } else null

    Scaffold(
        topBar = {
            HisabNavyTopBar(
                title = topBarTitle,
                subtitle = topBarSubtitle,
                showBack = !isTopLevelTab,
                onBack = { navController.popBackStack() },
                reminderCount = if (isTopLevelTab) pendingAlertCount else 0,
                onRemindersClick = if (isTopLevelTab) {
                    { navController.navigate(AppRoutes.REMINDERS) }
                } else null,
                onSettingsClick = if (isTopLevelTab) {
                    { navController.navigate(AppRoutes.SETTINGS) }
                } else null,
                onSearchClick = if (isTopLevelTab) {
                    { showGlobalSearch = true }
                } else null,
                onLanguageToggle = { viewModel.toggleLanguage() }
            )
        },
        bottomBar = {
            if (isTopLevelTab) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    bottomTabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NavyDeep,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = GoldBright.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (isTopLevelTab) {
                FloatingActionButton(
                    onClick = { showQuickSheet = true },
                    shape = CircleShape,
                    containerColor = GoldBright,
                    contentColor = NavyDeep,
                    modifier = Modifier.testTag("main_fab_add")
                ) {
                    Icon(Icons.Default.Add, contentDescription = strings.quickActions)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = AppRoutes.HOME
            ) {
                composable(AppRoutes.HOME) {
                    val todayKey = IndiaFormatters.todayDateKey()
                    val todayWorks = remember(workEntries, todayKey) {
                        workEntries.filter { it.dateKey == todayKey }
                    }
                    DashboardScreen(
                        settings = settings,
                        summary = dashboardSummary,
                        workers = workers,
                        todayWorkEntries = todayWorks,
                        recentTransactions = recentTransactions,
                        onQuickAttendance = { navController.navigate(AppRoutes.ATTENDANCE) },
                        onQuickAddWork = {
                            ensureWorkerOrPrompt { showAddWorkDialogForWorker = true to null }
                        },
                        onQuickGiveAdvance = {
                            ensureWorkerOrPrompt { showAddAdvanceForWorker = true to null }
                        },
                        onQuickRecordPayment = {
                            ensureWorkerOrPrompt { showAddPaymentForWorker = true to null }
                        },
                        onQuickAddWorker = {
                            editingWorker = null
                            showAddWorkerDialog = true
                        },
                        onOpenWorker = { wId -> navController.navigate(AppRoutes.workerDetail(wId)) },
                        onOpenWorkTab = { navController.navigate(AppRoutes.WORK_REGISTER) },
                        onOpenPaymentsTab = { navController.navigate(AppRoutes.PAYMENTS) }
                    )
                }

                composable(AppRoutes.WORKERS) {
                    WorkersScreen(
                        workers = workers,
                        ledgers = ledgers,
                        onWorkerClick = { wId -> navController.navigate(AppRoutes.workerDetail(wId)) },
                        onAddWorkerClick = {
                            editingWorker = null
                            showAddWorkerDialog = true
                        },
                        onEditWorkerClick = { w ->
                            editingWorker = w
                            showAddWorkerDialog = true
                        },
                        onDeleteWorkerConfirmed = { wId -> viewModel.deleteWorker(wId) }
                    )
                }

                composable(AppRoutes.WORK_REGISTER) {
                    WorkRegisterScreen(
                        workers = workers,
                        workEntries = workEntries,
                        attendanceRecords = attendanceRecords,
                        onAddWorkClick = {
                            ensureWorkerOrPrompt { showAddWorkDialogForWorker = true to null }
                        },
                        onEditWorkClick = { entry ->
                            editingWorkEntry = entry
                            showAddWorkDialogForWorker = true to entry.workerId
                        },
                        onDeleteWorkConfirmed = { id -> viewModel.deleteWorkEntry(id) },
                        onMarkAttendance = { wId, dateKey, status, hrs, notes ->
                            viewModel.markAttendance(wId, dateKey, status, hrs, notes)
                        },
                        onClearAttendance = { wId, dateKey ->
                            viewModel.clearAttendance(wId, dateKey)
                        }
                    )
                }

                composable(AppRoutes.PAYMENTS) {
                    PaymentsScreen(
                        workers = workers,
                        payments = payments,
                        advances = advances,
                        onAddPaymentClick = {
                            ensureWorkerOrPrompt { showAddPaymentForWorker = true to null }
                        },
                        onAddAdvanceClick = {
                            ensureWorkerOrPrompt { showAddAdvanceForWorker = true to null }
                        },
                        onDeletePaymentConfirmed = { id -> viewModel.deletePayment(id) },
                        onDeleteAdvanceConfirmed = { id -> viewModel.deleteAdvance(id) },
                        onViewAdvanceReceipt = { adv -> viewingReceiptAdvance = adv },
                        onOpenWorker = { wId -> navController.navigate(AppRoutes.workerDetail(wId)) }
                    )
                }

                composable(AppRoutes.REPORTS) {
                    ReportsScreen(
                        workers = workers,
                        onBuildReport = { type, startKey, endKey, wId, s ->
                            viewModel.buildReportTable(type, startKey, endKey, wId, s)
                        }
                    )
                }

                composable(AppRoutes.ATTENDANCE) {
                    AttendanceScreen(
                        workers = workers,
                        attendanceRecords = attendanceRecords,
                        onMarkAttendance = { wId, dateKey, status, hrs, notes ->
                            viewModel.markAttendance(wId, dateKey, status, hrs, notes)
                        },
                        onClearAttendance = { wId, dateKey ->
                            viewModel.clearAttendance(wId, dateKey)
                        }
                    )
                }

                composable(AppRoutes.REMINDERS) {
                    RemindersScreen(
                        businessName = settings.businessName,
                        workers = workers,
                        ledgers = ledgers,
                        reminders = reminders,
                        onSaveReminder = { viewModel.saveReminder(it) },
                        onToggleCompleted = { viewModel.toggleReminderCompleted(it) },
                        onDeleteReminder = { viewModel.deleteReminder(it) }
                    )
                }

                composable(AppRoutes.SETTINGS) {
                    SettingsScreen(
                        settings = settings,
                        onUpdateBusinessProfile = { name, owner, mobile ->
                            viewModel.updateBusinessProfile(name, owner, mobile)
                        },
                        onUpdateBusinessLogo = { uri -> viewModel.updateBusinessLogo(uri) },
                        onSetLanguage = { viewModel.setLanguage(it) },
                        onSetThemeMode = { viewModel.setThemeMode(it) },
                        onSetDefaultPaymentMethod = { viewModel.setDefaultPaymentMethod(it) },
                        onSetPin = { viewModel.setPin(it) },
                        onDisablePin = { viewModel.disablePin() },
                        onSetBiometric = { viewModel.setBiometricEnabled(it) },
                        onExportBackupJson = { viewModel.createBackupJson() },
                        onRestoreBackupJson = { viewModel.restoreBackupJson(it) },
                        onClearAllData = { viewModel.clearAllData() }
                    )
                }

                composable(
                    route = AppRoutes.WORKER_DETAIL,
                    arguments = listOf(navArgument("workerId") { type = NavType.LongType })
                ) { entry ->
                    val wId = entry.arguments?.getLong("workerId") ?: return@composable
                    val worker = workers.firstOrNull { it.id == wId }
                    if (worker == null) {
                        Box(modifier = Modifier.fillMaxSize())
                    } else {
                        WorkerDetailScreen(
                            worker = worker,
                            allWorkEntries = workEntries,
                            allAttendance = attendanceRecords,
                            allAdvances = advances,
                            allPayments = payments,
                            onEditWorker = {
                                editingWorker = worker
                                showAddWorkerDialog = true
                            },
                            onDeleteWorker = {
                                viewModel.deleteWorker(worker.id)
                                navController.popBackStack()
                            },
                            onAddWork = {
                                editingWorkEntry = null
                                showAddWorkDialogForWorker = true to worker.id
                            },
                            onEditWork = { work ->
                                editingWorkEntry = work
                                showAddWorkDialogForWorker = true to worker.id
                            },
                            onDeleteWork = { id -> pendingDeleteWorkId = id },
                            onGiveAdvance = { showAddAdvanceForWorker = true to worker.id },
                            onDeleteAdvance = { id -> pendingDeleteAdvanceId = id },
                            onRecordPayment = { showAddPaymentForWorker = true to worker.id },
                            onDeletePayment = { id -> pendingDeletePaymentId = id },
                            onShowAdvanceReceipt = { adv -> viewingReceiptAdvance = adv },
                            onBuildLedgerPdfTable = { startKey, endKey ->
                                viewModel.buildWorkerLedgerReportTable(worker, startKey, endKey, strings)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showQuickSheet) {
        QuickActionChooserSheet(
            onDismiss = { showQuickSheet = false },
            onAddWork = {
                ensureWorkerOrPrompt {
                    editingWorkEntry = null
                    showAddWorkDialogForWorker = true to null
                }
            },
            onMarkAttendance = { navController.navigate(AppRoutes.ATTENDANCE) },
            onGiveAdvance = {
                ensureWorkerOrPrompt { showAddAdvanceForWorker = true to null }
            },
            onRecordPayment = {
                ensureWorkerOrPrompt { showAddPaymentForWorker = true to null }
            },
            onAddWorker = {
                editingWorker = null
                showAddWorkerDialog = true
            }
        )
    }

    if (showGlobalSearch) {
        GlobalSearchDialog(
            workers = workers,
            ledgers = ledgers,
            transactions = recentTransactions,
            onSelectWorker = { wId -> navController.navigate(AppRoutes.workerDetail(wId)) },
            onDismiss = { showGlobalSearch = false }
        )
    }

    if (showAddWorkerDialog) {
        AddEditWorkerDialog(
            initialWorker = editingWorker,
            onCopyPhoto = { uri, prefix -> viewModel.copyPhotoToInternal(uri, prefix) },
            onSave = { worker ->
                viewModel.saveWorker(worker)
                showAddWorkerDialog = false
                editingWorker = null
                Toast.makeText(context, strings.savedSuccess, Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                showAddWorkerDialog = false
                editingWorker = null
            }
        )
    }

    if (showAddWorkDialogForWorker.first && workers.isNotEmpty()) {
        AddEditWorkDialog(
            workers = workers,
            preselectedWorkerId = showAddWorkDialogForWorker.second,
            initialEntry = editingWorkEntry,
            onSave = { workEntry ->
                viewModel.saveWorkEntry(workEntry)
                showAddWorkDialogForWorker = false to null
                editingWorkEntry = null
                Toast.makeText(context, strings.savedSuccess, Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                showAddWorkDialogForWorker = false to null
                editingWorkEntry = null
            }
        )
    }

    if (showAddAdvanceForWorker.first && workers.isNotEmpty()) {
        AddAdvanceDialog(
            workers = workers,
            defaultMethod = settings.defaultPaymentMethod,
            preselectedWorkerId = showAddAdvanceForWorker.second,
            onSave = { advance ->
                viewModel.saveAdvance(advance)
                showAddAdvanceForWorker = false to null
                Toast.makeText(context, strings.savedSuccess, Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showAddAdvanceForWorker = false to null }
        )
    }

    if (showAddPaymentForWorker.first && workers.isNotEmpty()) {
        AddPaymentDialog(
            workers = workers,
            ledgers = ledgers,
            defaultMethod = settings.defaultPaymentMethod,
            preselectedWorkerId = showAddPaymentForWorker.second,
            onSave = { payment ->
                viewModel.savePayment(payment)
                showAddPaymentForWorker = false to null
                Toast.makeText(context, strings.savedSuccess, Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showAddPaymentForWorker = false to null }
        )
    }

    viewingReceiptAdvance?.let { adv ->
        val wName = workers.firstOrNull { it.id == adv.workerId }?.fullName ?: "Worker"
        val ownerContact = listOfNotNull(
            settings.ownerName.takeIf { it.isNotBlank() },
            settings.ownerMobile.takeIf { it.isNotBlank() }
        ).joinToString(" • ")
        AdvanceReceiptDialog(
            advance = adv,
            workerName = wName,
            businessName = settings.businessName,
            ownerContact = ownerContact,
            onDismiss = { viewingReceiptAdvance = null }
        )
    }

    pendingDeleteWorkId?.let { id ->
        ConfirmActionDialog(
            title = strings.delete,
            message = strings.deleteWorkConfirm,
            confirmText = strings.delete,
            onConfirm = {
                viewModel.deleteWorkEntry(id)
                pendingDeleteWorkId = null
            },
            onDismiss = { pendingDeleteWorkId = null }
        )
    }

    pendingDeleteAdvanceId?.let { id ->
        ConfirmActionDialog(
            title = strings.delete,
            message = strings.deleteAdvanceConfirm,
            confirmText = strings.delete,
            onConfirm = {
                viewModel.deleteAdvance(id)
                pendingDeleteAdvanceId = null
            },
            onDismiss = { pendingDeleteAdvanceId = null }
        )
    }

    pendingDeletePaymentId?.let { id ->
        ConfirmActionDialog(
            title = strings.delete,
            message = strings.deletePaymentConfirm,
            confirmText = strings.delete,
            onConfirm = {
                viewModel.deletePayment(id)
                pendingDeletePaymentId = null
            },
            onDismiss = { pendingDeletePaymentId = null }
        )
    }
}
