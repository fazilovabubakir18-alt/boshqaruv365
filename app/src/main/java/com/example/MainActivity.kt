package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppUser
import com.example.model.ProblemStatus
import com.example.model.ThemeMode
import com.example.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.components.CivicTopAppBar
import com.example.ui.components.ExecutiveSidebar
import com.example.ui.dialogs.*
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            MyApplicationTheme(
                themeMode = uiState.settings.themeMode,
                accentChoice = uiState.settings.accentChoice
            ) {
                Boshqaruv365App(viewModel = viewModel)
            }
        }
    }
}

data class NavTabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Boshqaruv365App(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val disciplinaryActions by viewModel.disciplinaryActions.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val offlineQueue by viewModel.offlineQueue.collectAsStateWithLifecycle()
    val usersList by viewModel.usersList.collectAsStateWithLifecycle()
    val departmentsList by viewModel.departmentsList.collectAsStateWithLifecycle()

    val healthWeights by viewModel.healthWeights.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val cityProblems by viewModel.cityProblems.collectAsStateWithLifecycle()
    val cityFeed by viewModel.cityFeed.collectAsStateWithLifecycle()
    val mahallas by viewModel.mahallas.collectAsStateWithLifecycle()
    val investments by viewModel.investments.collectAsStateWithLifecycle()
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val indicators by viewModel.indicators.collectAsStateWithLifecycle()
    val aiModules by viewModel.aiModules.collectAsStateWithLifecycle()
    val aiSignals by viewModel.aiSignals.collectAsStateWithLifecycle()
    val diagonalTransfers by viewModel.diagonalTransfers.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    val cityHealthScore = remember(healthWeights) {
        viewModel.computeCityHealthIndex(healthWeights)
    }

    val isDarkActive = when (uiState.settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var showRoleDialog by remember { mutableStateOf(false) }
    var userForPasswordReset by remember { mutableStateOf<AppUser?>(null) }
    var openTasksInDiagonal by remember { mutableStateOf(false) }

    val unreadNotifications = remember(notifications) {
        notifications.count { !it.isRead }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    val selectedTask = remember(tasks, uiState.selectedTaskId) {
        tasks.firstOrNull { it.id == uiState.selectedTaskId }
    }

    val taskReports = remember(reports, uiState.selectedTaskId) {
        reports.filter { it.taskId == uiState.selectedTaskId }
    }

    val isSupervisorOrAdminDock = uiState.currentRole == UserRole.HOKIM ||
        uiState.currentRole == UserRole.SUPER_ADMIN ||
        uiState.currentRole == UserRole.ORINBOSAR ||
        uiState.currentRole == UserRole.NAZORATCHI

    val navTabs = remember(isSupervisorOrAdminDock) {
        listOf(
            NavTabItem("Bosh sahifa", Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
            NavTabItem("Topshiriqlar", Icons.Filled.Assignment, Icons.Outlined.Assignment, "nav_tab_tasks"),
            NavTabItem("Nazorat (AI)", Icons.Filled.MonitorHeart, Icons.Outlined.MonitorHeart, "nav_tab_ai"),
            if (isSupervisorOrAdminDock) {
                NavTabItem("Xodimlar", Icons.Filled.Groups, Icons.Outlined.Groups, "nav_tab_rbac")
            } else {
                NavTabItem("Xarita", Icons.Filled.Map, Icons.Outlined.Map, "nav_tab_gis")
            },
            NavTabItem("Profil", Icons.Filled.Person, Icons.Outlined.Person, "nav_tab_profile")
        )
    }

    if (!uiState.isAuthenticated) {
        LoginScreen(
            availableUsers = usersList,
            onAttemptCredentialsLogin = { login, pass -> viewModel.attemptCredentialsLogin(login, pass) },
            onQuickUserLogin = { user -> viewModel.login(user) },
            errorMessage = uiState.authErrorMessage,
            onClearError = { viewModel.clearAuthError() }
        )
    } else {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useTabletSidebar = maxWidth >= 720.dp

            val activeSidebarRoute = when (uiState.secondaryView) {
                "ORG_STRUCTURE" -> "ORG_STRUCTURE"
                "PROJECTS" -> "PROJECTS"
                "PERMISSIONS" -> "PERMISSIONS"
                "EMPLOYEES" -> "EMPLOYEES"
                "GIS" -> "GIS"
                "ANALYTICS" -> "ANALYTICS"
                else -> when (uiState.selectedTab) {
                    0 -> "HOME"
                    1 -> "TASKS"
                    2 -> "AI"
                    3 -> if (isSupervisorOrAdminDock) "EMPLOYEES" else "GIS"
                    4 -> "PROFILE"
                    else -> "HOME"
                }
            }

            Row(modifier = Modifier.fillMaxSize()) {
                if (useTabletSidebar) {
                    ExecutiveSidebar(
                        activeRouteId = activeSidebarRoute,
                        tasksCount = tasks.size,
                        problemsCount = cityProblems.count { it.status == ProblemStatus.OCHIQ },
                        onSelectRoute = { routeId ->
                            viewModel.selectTask(null)
                            openTasksInDiagonal = false
                            when (routeId) {
                                "HOME" -> viewModel.selectTab(0)
                                "TASKS" -> viewModel.selectTab(1)
                                "AI" -> viewModel.selectTab(2)
                                "EMPLOYEES" -> {
                                    if (isSupervisorOrAdminDock) viewModel.selectTab(3)
                                    else viewModel.openSecondaryView("EMPLOYEES")
                                }
                                "PROFILE" -> viewModel.selectTab(4)
                                "ORG_STRUCTURE" -> viewModel.openSecondaryView("ORG_STRUCTURE")
                                "PROJECTS" -> viewModel.openSecondaryView("PROJECTS")
                                "PERMISSIONS" -> viewModel.openSecondaryView("PERMISSIONS")
                                "PROBLEMS", "GIS" -> viewModel.openSecondaryView("GIS")
                                "ANALYTICS" -> viewModel.openSecondaryView("ANALYTICS")
                            }
                        },
                        onLogoutClick = { viewModel.logout() }
                    )
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        if (selectedTask == null) {
                            if (uiState.secondaryView != null && !useTabletSidebar) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .statusBarsPadding()
                                            .padding(horizontal = 8.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.openSecondaryView(null) },
                                            modifier = Modifier.testTag("secondary_view_back_button")
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = secondaryViewTitle(uiState.secondaryView),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            } else {
                                CivicTopAppBar(
                                    currentRole = uiState.currentRole,
                                    userName = uiState.currentUserName,
                                    cityHealthScore = cityHealthScore,
                                    unreadNotificationsCount = unreadNotifications,
                                    isOffline = uiState.isOfflineMode,
                                    isDarkTheme = isDarkActive,
                                    onRoleClick = { showRoleDialog = true },
                                    onNotificationsClick = { viewModel.showNotificationsSheet(true) },
                                    onToggleTheme = {
                                        viewModel.setThemeMode(if (isDarkActive) ThemeMode.LIGHT else ThemeMode.DARK)
                                    },
                                    onOpenAiClick = { viewModel.selectTab(2) }
                                )
                            }
                        }
                    },
                    bottomBar = {
                        if (!useTabletSidebar && selectedTask == null && uiState.secondaryView == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                    tonalElevation = 8.dp,
                                    shadowElevation = 8.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    border = BorderStroke(
                                        width = 0.8.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                                    ),
                                    modifier = Modifier
                                        .widthIn(max = 600.dp)
                                        .fillMaxWidth()
                                ) {
                                    NavigationBar(
                                        containerColor = Color.Transparent,
                                        tonalElevation = 0.dp,
                                        windowInsets = WindowInsets(0.dp),
                                        modifier = Modifier.height(68.dp)
                                    ) {
                                        navTabs.forEachIndexed { index, item ->
                                            val isSelected = uiState.selectedTab == index
                                            NavigationBarItem(
                                                selected = isSelected,
                                                onClick = {
                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    openTasksInDiagonal = false
                                                    viewModel.selectTab(index)
                                                },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                        contentDescription = item.title,
                                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = item.title,
                                                        fontSize = 9.sp,
                                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                },
                                                colors = NavigationBarItemDefaults.colors(
                                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                                ),
                                                modifier = Modifier.testTag(item.testTag)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .then(if (useTabletSidebar) Modifier.fillMaxSize() else Modifier.widthIn(max = 600.dp).fillMaxSize())
                        ) {
                            if (selectedTask != null) {
                                BackHandler { viewModel.selectTask(null) }
                                TaskDetailScreen(
                                    task = selectedTask,
                                    reports = taskReports,
                                    comments = comments,
                                    auditLogs = auditLogs,
                                    allUsers = usersList,
                                    currentUser = uiState.currentUser,
                                    currentRole = uiState.currentRole,
                                    taskRole = viewModel.resolveTaskRoleForUser(uiState.currentUser, selectedTask),
                                    onBack = { viewModel.selectTask(null) },
                                    onAccept = { viewModel.acceptCurrentTask() },
                                    onRejectClick = { viewModel.showRejectDialog(true) },
                                    onSubmitReportClick = { viewModel.showReportDialog(true) },
                                    onVerifyReviewClick = { viewModel.verifyByReviewerCurrentTask() },
                                    onApproveClick = { viewModel.approveCurrentTask() },
                                    onReworkClick = { viewModel.showReworkDialog(true) },
                                    onDisciplineClick = { viewModel.showDisciplineDialog(true) },
                                    onTransitionStatus = { nextStatus, reason ->
                                        viewModel.transitionCurrentTask(nextStatus, reason)
                                    },
                                    onToggleSubtask = { subtaskId -> viewModel.toggleSubtask(subtaskId) },
                                    onAddComment = { type, text -> viewModel.addCommentToCurrentTask(type, text) },
                                    onUpdateProgress = { pct -> viewModel.updateCurrentTaskProgress(pct) },
                                    onInterveneTask = { hours, rev, ass, note, reopen ->
                                        viewModel.interveneCurrentTask(hours, rev, ass, note, reopen)
                                    },
                                    onRequestClarification = { q -> viewModel.requestClarificationForCurrentTask(q) },
                                    onRequestExtension = { hrs, rsn -> viewModel.requestExtensionForCurrentTask(hrs, rsn) },
                                    onResolveExtension = { approve, note -> viewModel.resolveExtensionForCurrentTask(approve, note) },
                                    onToggleMultiOrgPart = { org -> viewModel.toggleMultiOrgPartForCurrentTask(org) }
                                )
                            } else if (uiState.secondaryView == "ANALYTICS") {
                                BackHandler { viewModel.openSecondaryView(null) }
                                AnalyticsKpiScreen(
                                    tasks = tasks,
                                    disciplinaryActions = disciplinaryActions,
                                    cityHealthScore = cityHealthScore,
                                    healthWeights = healthWeights,
                                    mahallas = mahallas,
                                    investments = investments,
                                    onOpenHealthEditor = { viewModel.showHealthWeightsEditor(true) },
                                    onExportClick = { type -> viewModel.showExportDialog(true, type) },
                                    onIssueDisciplineClick = { viewModel.showDisciplineDialog(true) }
                                )
                            } else if (uiState.secondaryView == "GIS") {
                                BackHandler { viewModel.openSecondaryView(null) }
                                GisMapScreen(
                                    tasks = tasks,
                                    onSelectTask = { viewModel.selectTask(it) }
                                )
                            } else if (
                                uiState.secondaryView == "ORG_STRUCTURE" ||
                                uiState.secondaryView == "PROJECTS" ||
                                uiState.secondaryView == "PERMISSIONS" ||
                                uiState.secondaryView == "EMPLOYEES"
                            ) {
                                BackHandler { viewModel.openSecondaryView(null) }
                                val targetTab = when (uiState.secondaryView) {
                                    "ORG_STRUCTURE" -> 6
                                    "PROJECTS" -> 7
                                    "PERMISSIONS" -> 2
                                    else -> 0
                                }
                                ProfileAndAdminScreen(
                                    currentUser = uiState.currentUser,
                                    isOffline = uiState.isOfflineMode,
                                    offlineQueue = offlineQueue,
                                    auditLogs = auditLogs,
                                    allUsers = usersList,
                                    allDepartments = departmentsList,
                                    documents = documents,
                                    indicators = indicators,
                                    settings = uiState.settings,
                                    tasks = tasks,
                                    investments = investments,
                                    employeesDirectoryMode = uiState.secondaryView == "EMPLOYEES",
                                    initialAdminTab = targetTab,
                                    onThemeModeChange = { viewModel.setThemeMode(it) },
                                    onAccentChange = { viewModel.setAccentChoice(it) },
                                    onLanguageChange = { viewModel.setLanguage(it) },
                                    onDocumentClick = { viewModel.openDocumentPreview(it) },
                                    onOpenAiClick = { viewModel.selectTab(2) },
                                    onRoleChangeClick = { showRoleDialog = true },
                                    onToggleOffline = { viewModel.toggleOfflineMode() },
                                    onSyncOfflineClick = { viewModel.syncOffline() },
                                    onLogoutClick = { viewModel.logout() },
                                    onAddEmployeeClick = { viewModel.showAddUserDialog(true) },
                                    onAddDepartmentClick = { viewModel.showAddDeptDialog(true) },
                                    onToggleBlockEmployee = { userId -> viewModel.toggleBlockEmployee(userId) },
                                    onToggleBlockDepartment = { deptId -> viewModel.toggleBlockDepartment(deptId) },
                                    onEditPermissionsClick = { user -> viewModel.setUserForPermissionEdit(user) },
                                    onResetPasswordClick = { user -> userForPasswordReset = user },
                                    onEmployeeClick = { user -> viewModel.selectEmployeeProfile(user) },
                                    onSaveRolePermissions = { role, perms -> viewModel.updateRolePermissions(role, perms) },
                                    onAddProjectClick = { viewModel.showAddProjectDialog(true) },
                                    lastGeneratedOtpHint = uiState.lastGeneratedOtpHint,
                                    lastBackupVerification = uiState.lastBackupVerification,
                                    onRunBackupSnapshotTest = { viewModel.runBackupSnapshotTest() }
                                )
                            } else {
                                when (uiState.selectedTab) {
                                    0 -> DashboardScreen(
                                        tasks = tasks,
                                        currentUser = uiState.currentUser,
                                        currentRole = uiState.currentRole,
                                        allUsers = usersList,
                                        cityHealthScore = cityHealthScore,
                                        stories = stories,
                                        problems = cityProblems,
                                        cityFeed = cityFeed,
                                        diagonalTransfersCount = diagonalTransfers.size,
                                        aiAlertsCount = aiSignals.size + tasks.count { it.cycleCount >= 3 },
                                        departmentsCount = departmentsList.size,
                                        investmentsCount = investments.size,
                                        onStoryClick = { viewModel.openStory(it) },
                                        onProblemClick = { viewModel.openProblemDetail(it) },
                                        onFeedVerifyClick = { viewModel.toggleFeedVerify(it) },
                                        onSelectTask = { viewModel.selectTask(it) },
                                        onApproveTaskDirect = { viewModel.approveTaskDirect(it) },
                                        onNewTaskClick = { viewModel.showNewTaskDialog(true) },
                                        onOpenAiClick = { viewModel.selectTab(2) },
                                        onOpenBriefingClick = { viewModel.showFiveMinBriefing(true) },
                                        onOpenAnalyticsClick = { viewModel.openSecondaryView("ANALYTICS") },
                                        onOpenGisClick = { viewModel.openSecondaryView("GIS") },
                                        onOpenDiagonalClick = {
                                            openTasksInDiagonal = true
                                            viewModel.selectTab(1)
                                        },
                                        onOpenHealthEditorClick = { viewModel.showHealthWeightsEditor(true) },
                                        onOpenOrgStructureClick = { viewModel.openSecondaryView("ORG_STRUCTURE") },
                                        onOpenProjectsClick = { viewModel.openSecondaryView("PROJECTS") },
                                        onOpenEmployeesClick = {
                                            if (isSupervisorOrAdminDock) viewModel.selectTab(3)
                                            else viewModel.openSecondaryView("EMPLOYEES")
                                        },
                                        onOpenPermissionsClick = { viewModel.openSecondaryView("PERMISSIONS") },
                                        onAddProjectClick = { viewModel.showAddProjectDialog(true) }
                                    )
                                    1 -> TasksScreen(
                                        tasks = tasks,
                                        currentUser = uiState.currentUser,
                                        currentRole = uiState.currentRole,
                                        selectedFilter = uiState.taskFilter,
                                        searchQuery = uiState.searchQuery,
                                        diagonalTransfers = diagonalTransfers,
                                        departments = departmentsList,
                                        initialSubMode = if (openTasksInDiagonal) 1 else 0,
                                        resolveTaskRole = { user, task -> viewModel.resolveTaskRoleForUser(user, task) },
                                        onFilterSelected = { viewModel.setTaskFilter(it) },
                                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                        onSelectTask = { viewModel.selectTask(it) },
                                        onApproveTaskDirect = { viewModel.approveTaskDirect(it) },
                                        onNewTaskClick = { viewModel.showNewTaskDialog(true) },
                                        onCreateDiagonalTransfer = { type, dept, leader, subj, basis, deadline ->
                                            viewModel.createDiagonalTransfer(type, dept, leader, subj, basis, deadline)
                                        },
                                        onAcceptDiagonalTransfer = { id -> viewModel.acceptDiagonalTransfer(id) },
                                        onRespondDiagonalTransfer = { id, text -> viewModel.respondDiagonalTransfer(id, text) },
                                        onCreateTaskFromDiagonal = { transfer -> viewModel.createTaskFromDiagonal(transfer) }
                                    )
                                    2 -> AiAssistantScreen(
                                        tasks = tasks,
                                        auditLogs = auditLogs,
                                        aiModules = aiModules,
                                        aiSignals = aiSignals,
                                        messages = uiState.aiMessages,
                                        onSendMessage = { query -> viewModel.sendAiQuery(query) },
                                        onOpenBriefing = { viewModel.showFiveMinBriefing(true) },
                                        onSelectTask = { viewModel.selectTask(it) },
                                        onNotifySupervisor = { viewModel.notifySupervisorAboutTask(it) },
                                        onExportClick = { type -> viewModel.showExportDialog(true, type) }
                                    )
                                    3 -> if (isSupervisorOrAdminDock) {
                                        ProfileAndAdminScreen(
                                            currentUser = uiState.currentUser,
                                            isOffline = uiState.isOfflineMode,
                                            offlineQueue = offlineQueue,
                                            auditLogs = auditLogs,
                                            allUsers = usersList,
                                            allDepartments = departmentsList,
                                            documents = documents,
                                            indicators = indicators,
                                            settings = uiState.settings,
                                            tasks = tasks,
                                            investments = investments,
                                            employeesDirectoryMode = true,
                                            onThemeModeChange = { viewModel.setThemeMode(it) },
                                            onAccentChange = { viewModel.setAccentChoice(it) },
                                            onLanguageChange = { viewModel.setLanguage(it) },
                                            onDocumentClick = { viewModel.openDocumentPreview(it) },
                                            onOpenAiClick = { viewModel.selectTab(2) },
                                            onRoleChangeClick = { showRoleDialog = true },
                                            onToggleOffline = { viewModel.toggleOfflineMode() },
                                            onSyncOfflineClick = { viewModel.syncOffline() },
                                            onLogoutClick = { viewModel.logout() },
                                            onAddEmployeeClick = { viewModel.showAddUserDialog(true) },
                                            onAddDepartmentClick = { viewModel.showAddDeptDialog(true) },
                                            onToggleBlockEmployee = { userId -> viewModel.toggleBlockEmployee(userId) },
                                            onToggleBlockDepartment = { deptId -> viewModel.toggleBlockDepartment(deptId) },
                                            onEditPermissionsClick = { user -> viewModel.setUserForPermissionEdit(user) },
                                            onResetPasswordClick = { user -> userForPasswordReset = user },
                                            onEmployeeClick = { user -> viewModel.selectEmployeeProfile(user) },
                                            onSaveRolePermissions = { role, perms -> viewModel.updateRolePermissions(role, perms) },
                                            onAddProjectClick = { viewModel.showAddProjectDialog(true) },
                                            lastGeneratedOtpHint = uiState.lastGeneratedOtpHint,
                                            lastBackupVerification = uiState.lastBackupVerification,
                                            onRunBackupSnapshotTest = { viewModel.runBackupSnapshotTest() }
                                        )
                                    } else {
                                        GisMapScreen(
                                            tasks = tasks,
                                            onSelectTask = { viewModel.selectTask(it) }
                                        )
                                    }
                                    4 -> ProfileAndAdminScreen(
                                        currentUser = uiState.currentUser,
                                        isOffline = uiState.isOfflineMode,
                                        offlineQueue = offlineQueue,
                                        auditLogs = auditLogs,
                                        allUsers = usersList,
                                        allDepartments = departmentsList,
                                        documents = documents,
                                        indicators = indicators,
                                        settings = uiState.settings,
                                        tasks = tasks,
                                        investments = investments,
                                        employeesDirectoryMode = false,
                                        onThemeModeChange = { viewModel.setThemeMode(it) },
                                        onAccentChange = { viewModel.setAccentChoice(it) },
                                        onLanguageChange = { viewModel.setLanguage(it) },
                                        onDocumentClick = { viewModel.openDocumentPreview(it) },
                                        onOpenAiClick = { viewModel.selectTab(2) },
                                        onRoleChangeClick = { showRoleDialog = true },
                                        onToggleOffline = { viewModel.toggleOfflineMode() },
                                        onSyncOfflineClick = { viewModel.syncOffline() },
                                        onLogoutClick = { viewModel.logout() },
                                        onAddEmployeeClick = { viewModel.showAddUserDialog(true) },
                                        onAddDepartmentClick = { viewModel.showAddDeptDialog(true) },
                                        onToggleBlockEmployee = { userId -> viewModel.toggleBlockEmployee(userId) },
                                        onToggleBlockDepartment = { deptId -> viewModel.toggleBlockDepartment(deptId) },
                                        onEditPermissionsClick = { user -> viewModel.setUserForPermissionEdit(user) },
                                        onResetPasswordClick = { user -> userForPasswordReset = user },
                                        onEmployeeClick = { user -> viewModel.selectEmployeeProfile(user) },
                                        onSaveRolePermissions = { role, perms -> viewModel.updateRolePermissions(role, perms) },
                                        onAddProjectClick = { viewModel.showAddProjectDialog(true) },
                                        lastGeneratedOtpHint = uiState.lastGeneratedOtpHint,
                                        lastBackupVerification = uiState.lastBackupVerification,
                                        onRunBackupSnapshotTest = { viewModel.runBackupSnapshotTest() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Shahar 360° Story Viewer
    uiState.activeStory?.let { story ->
        StoryViewerDialog(
            story = story,
            onNavigateTarget = { target ->
                if (target == 4 || target == 2) viewModel.selectTab(2) else viewModel.selectTab(target)
            },
            onDismiss = { viewModel.openStory(null) }
        )
    }

    // Shahar 360° Problem Detail & 1-Click Task Creation
    uiState.selectedProblem?.let { prob ->
        ProblemDetailTaskDialog(
            problem = prob,
            canCreateTask = uiState.currentUser.permissions.canCreateTask,
            onCreateTask = { title, days -> viewModel.createTaskFromProblem(prob, title, days) },
            onOpenLinkedTask = { tid -> viewModel.selectTask(tid) },
            onDismiss = { viewModel.openProblemDetail(null) }
        )
    }

    // 5-Minute Hokim Briefing Modal
    if (uiState.showFiveMinBriefing) {
        FiveMinuteBriefingDialog(
            healthScore = cityHealthScore,
            onOpenAiCenter = { viewModel.selectTab(2) },
            onDismiss = { viewModel.showFiveMinBriefing(false) }
        )
    }

    // Health Index Weights Editor Modal
    if (uiState.showHealthWeightsEditor) {
        HealthWeightsEditorDialog(
            weights = healthWeights,
            computedScore = cityHealthScore,
            onUpdateWeight = { id, w -> viewModel.updateCategoryWeight(id, w) },
            onReset = { viewModel.resetHealthWeights() },
            onDismiss = { viewModel.showHealthWeightsEditor(false) }
        )
    }

    // Document Preview Modal
    uiState.activeDocumentPreview?.let { doc ->
        DocumentPreviewDialog(
            doc = doc,
            onDismiss = { viewModel.openDocumentPreview(null) }
        )
    }

    // Role Switcher Dialog
    if (showRoleDialog) {
        RoleSwitcherDialog(
            currentRole = uiState.currentRole,
            onRoleSelected = { viewModel.setRole(it) },
            onDismiss = { showRoleDialog = false }
        )
    }

    // New Task Dialog (3-Way Chain + Routing & Format)
    if (uiState.showNewTaskDialog) {
        NewTaskDialog(
            creatorName = uiState.currentUserName,
            availableUsers = usersList,
            onDismiss = { viewModel.showNewTaskDialog(false) },
            onSubmit = { title, desc, dept, assignee, priority, deadline, obj, addr, norm, kpi, subtasks, reviewer, approver, aiMode, routingType, reportFormat, multiOrgParts ->
                viewModel.createNewTask(
                    title = title,
                    description = desc,
                    department = dept,
                    assigneeName = assignee,
                    priority = priority,
                    deadlineMillis = deadline,
                    objectName = obj,
                    address = addr,
                    normativAsos = norm,
                    kpiPoints = kpi,
                    subtasks = subtasks,
                    reviewerName = reviewer,
                    finalApproverName = approver,
                    aiMonitoringMode = aiMode,
                    routingType = routingType,
                    reportFormat = reportFormat,
                    participatingOrgsRaw = multiOrgParts
                )
            }
        )
    }

    // Report Submission Dialog
    if (uiState.showReportDialog && selectedTask != null) {
        ReportSubmissionDialog(
            targetObjectName = selectedTask.objectName,
            targetAddress = selectedTask.address,
            onDismiss = { viewModel.showReportDialog(false) },
            onSubmit = { comment, lat, lng, photo ->
                viewModel.submitReportForCurrentTask(comment, lat, lng, photo)
            }
        )
    }

    // Rework Reason Dialog (Mandatory reason enforced)
    if (uiState.showReworkDialog) {
        ReasonInputDialog(
            title = "Qayta Ishlashga Qaytarish",
            label = "Kamchiliklar va tuzatish talabi *",
            confirmButtonText = "Izoh Bilan Qaytarish",
            confirmButtonColor = StatusYellow,
            onDismiss = { viewModel.showReworkDialog(false) },
            onSubmit = { reason -> viewModel.reworkCurrentTask(reason) }
        )
    }

    // Reject Reason Dialog (Mandatory reason enforced)
    if (uiState.showRejectDialog) {
        ReasonInputDialog(
            title = "Topshiriqni Asosli Rad Etish",
            label = "Rad etishning asosli sababi *",
            confirmButtonText = "Rad Etishni Tasdiqlash",
            confirmButtonColor = StatusRed,
            onDismiss = { viewModel.showRejectDialog(false) },
            onSubmit = { reason -> viewModel.rejectCurrentTask(reason) }
        )
    }

    // Disciplinary Action Dialog
    if (uiState.showDisciplineDialog) {
        DisciplineDialog(
            defaultEmployee = selectedTask?.assigneeName ?: "Xodim F.I.Sh.",
            defaultDept = selectedTask?.department ?: "Bo'lim nomi",
            onDismiss = { viewModel.showDisciplineDialog(false) },
            onSubmit = { emp, dept, type, reason ->
                viewModel.issueDiscipline(emp, dept, type, reason)
            }
        )
    }

    // Notifications Center Drawer / Sheet
    if (uiState.showNotificationsSheet) {
        NotificationsSheet(
            notifications = notifications,
            onMarkAllRead = { viewModel.markAllNotificationsRead() },
            onDismiss = { viewModel.showNotificationsSheet(false) }
        )
    }

    // Export Confirmation Dialog
    if (uiState.showExportDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showExportDialog(false) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.exportType == "EXCEL") Icons.Default.TableChart else Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = if (uiState.exportType == "EXCEL") Color(0xFF15803D) else Color(0xFFB91C1C)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.exportType == "EXCEL") "Excel Hisobot Yaratildi" else "PDF Rasmiy Hisobot Tayyor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Zarafshon shahar hokimligi ijro intizomi to'liq hisoboti ${if (uiState.exportType == "EXCEL") ".xlsx" else ".pdf"} formatida shakllantirildi.",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Fayl: Zarafshon_Boshqaruv365_${System.currentTimeMillis()}.${if (uiState.exportType == "EXCEL") "xlsx" else "pdf"}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TelegramBlue
                    )
                    Text(
                        text = "Jami topshiriqlar: ${tasks.size} ta, Yakunlangan: ${tasks.count { it.status == "COMPLETED" }} ta, 3+ qaytarish: ${tasks.count { it.cycleCount >= 3 }} ta",
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmExportReport(uiState.exportType) },
                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
                ) {
                    Text("Yuklab Olish")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showExportDialog(false) }) {
                    Text("Yopish")
                }
            }
        )
    }

    // Add Employee Dialog (10-arg 3-step modal with PickVisualMedia avatar)
    if (uiState.showAddUserDialog) {
        AddEmployeeDialog(
            departments = departmentsList,
            onDismiss = { viewModel.showAddUserDialog(false) },
            onSubmit = { name, login, pass, role, dept, pos, phone, superv, email, avatarUri ->
                viewModel.addEmployee(name, login, pass, role, dept, pos, phone, superv, email, avatarUri)
            }
        )
    }

    // Employee Profile Dialog
    uiState.selectedEmployeeProfile?.let { emp ->
        EmployeeProfileDialog(
            user = emp,
            tasks = tasks,
            onDismiss = { viewModel.selectEmployeeProfile(null) },
            onEditPermissions = {
                viewModel.selectEmployeeProfile(null)
                viewModel.setUserForPermissionEdit(emp)
            },
            onResetPassword = {
                viewModel.selectEmployeeProfile(null)
                userForPasswordReset = emp
            },
            onToggleBlock = {
                viewModel.toggleBlockEmployee(emp.id)
                viewModel.selectEmployeeProfile(null)
            }
        )
    }

    // Add Project Dialog
    if (uiState.showAddProjectDialog) {
        AddProjectDialog(
            onDismiss = { viewModel.showAddProjectDialog(false) },
            onSubmit = { title, sector, budgetUsdMillion, contractor, jobsPlanned ->
                viewModel.addInvestmentProject(title, sector, budgetUsdMillion, contractor, jobsPlanned)
            }
        )
    }

    // Add Department Dialog
    if (uiState.showAddDeptDialog) {
        AddDepartmentDialog(
            onDismiss = { viewModel.showAddDeptDialog(false) },
            onSubmit = { name, code, head, staffCount ->
                viewModel.addDepartment(name, code, head, staffCount)
            }
        )
    }

    // Edit Permissions Dialog
    uiState.userForPermissionEdit?.let { targetUser ->
        EditPermissionsDialog(
            user = targetUser,
            onDismiss = { viewModel.setUserForPermissionEdit(null) },
            onSave = { updatedPerms ->
                viewModel.updateUserPermissions(targetUser.id, updatedPerms)
            }
        )
    }

    // Reset Employee Password Dialog
    userForPasswordReset?.let { targetUser ->
        ResetPasswordDialog(
            user = targetUser,
            onDismiss = { userForPasswordReset = null },
            onSave = { newPass ->
                viewModel.updateEmployeePassword(targetUser.id, newPass)
                userForPasswordReset = null
            },
            onIssueOtp = {
                viewModel.issueOneTimeResetCode(targetUser.id)
                userForPasswordReset = null
            }
        )
    }
}

private fun secondaryViewTitle(view: String?): String = when (view) {
    "ANALYTICS" -> "Tahlil & KPI Hisobotlari"
    "GIS" -> "Xarita 360° GIS Monitoring"
    "ORG_STRUCTURE" -> "Tashkiliy tuzilma"
    "PROJECTS" -> "Loyihalar va dasturlar"
    "PERMISSIONS" -> "Vakolatlar matritsasi"
    "EMPLOYEES" -> "Xodimlar reyestri"
    else -> "Boshqaruv 365"
}
