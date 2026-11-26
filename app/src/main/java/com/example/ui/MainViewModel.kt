package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiExecutiveAssistant
import com.example.data.AppDatabase
import com.example.data.AuditLogEntity
import com.example.data.BoshqaruvRepository
import com.example.data.DisciplinaryEntity
import com.example.data.NotificationEntity
import com.example.data.OfflineQueueEntity
import com.example.data.ReportEntity
import com.example.data.TaskCommentEntity
import com.example.data.TaskEntity
import com.example.data.WorkflowResult
import com.example.model.AccentChoice
import com.example.model.AiAnomalySignal
import com.example.model.AiControlModule
import com.example.model.AppLanguage
import com.example.model.AppSettingsState
import com.example.model.AppUser
import com.example.model.AuthResult
import com.example.model.City360SeedData
import com.example.model.CityObjectFeedItem
import com.example.model.CityProblem
import com.example.model.CityStoryHighlight
import com.example.model.DepartmentInfo
import com.example.model.DiagonalTransfer
import com.example.model.DiagonalTransferStatus
import com.example.model.DiagonalTransferType
import com.example.model.DocumentRecord
import com.example.model.HealthCategoryWeight
import com.example.model.IndicatorCatalogItem
import com.example.model.InvestmentProject
import com.example.model.MahallaProfile
import com.example.model.PasswordHasher
import com.example.model.PresetUsers
import com.example.model.ProblemStatus
import com.example.model.SubTask
import com.example.model.TaskCommentType
import com.example.model.TaskRole
import com.example.model.TaskRoutingType
import com.example.model.TaskStatus
import com.example.model.ThemeMode
import com.example.model.UserPermissions
import com.example.model.UserRole
import com.example.model.UserScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class UiState(
    val isAuthenticated: Boolean = true,
    val currentUser: AppUser = PresetUsers.HOKIM,
    val currentRole: UserRole = UserRole.HOKIM,
    val currentUserName: String = PresetUsers.HOKIM.name,
    val selectedTab: Int = 0, // 0: Bosh sahifa, 1: Topshiriqlar, 2: Nazorat (AI), 3: Xodimlar / Xarita, 4: Profil
    val secondaryView: String? = null, // "ANALYTICS" or "GIS" when opened via Squircle tiles
    val selectedTaskId: Long? = null,
    val taskFilter: String = "BARCHASI",
    val searchQuery: String = "",
    val isOfflineMode: Boolean = false,
    val showNewTaskDialog: Boolean = false,
    val showReportDialog: Boolean = false,
    val showReworkDialog: Boolean = false,
    val showRejectDialog: Boolean = false,
    val showDisciplineDialog: Boolean = false,
    val showNotificationsSheet: Boolean = false,
    val showExportDialog: Boolean = false,
    val exportType: String = "EXCEL",
    val snackbarMessage: String? = null,
    val authErrorMessage: String? = null,
    val showAddUserDialog: Boolean = false,
    val showAddDeptDialog: Boolean = false,
    val showAddProjectDialog: Boolean = false,
    val userForPermissionEdit: AppUser? = null,
    val selectedEmployeeProfile: AppUser? = null,
    val settings: AppSettingsState = AppSettingsState(),
    val showAiCommandOverlay: Boolean = false,
    val showFiveMinBriefing: Boolean = false,
    val activeStory: CityStoryHighlight? = null,
    val selectedProblem: CityProblem? = null,
    val activeDocumentPreview: DocumentRecord? = null,
    val showHealthWeightsEditor: Boolean = false,
    val lastGeneratedOtpHint: Pair<String, String>? = null, // (userName, otpCode)
    val lastBackupVerification: String? = null,
    val aiMessages: List<Pair<Boolean, String>> = listOf(
        Pair(
            false,
            "Assalomu alaykum! Zarafshon 'Boshqaruv 365' AI Nazorat Qatlami (On-device Advisory) xizmatingizda. Barcha tahlillar manba, vaqt va ishonchlilik darajasi bilan taqdim etiladi; yakuniy qaror vakolatli rahbar tomonidan qabul qilinadi."
        )
    )
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = BoshqaruvRepository(db)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _usersList = MutableStateFlow<List<AppUser>>(PresetUsers.ALL_PRESETS)
    val usersList: StateFlow<List<AppUser>> = _usersList.asStateFlow()

    private val _departmentsList = MutableStateFlow<List<DepartmentInfo>>(PresetUsers.INITIAL_DEPARTMENTS)
    val departmentsList: StateFlow<List<DepartmentInfo>> = _departmentsList.asStateFlow()

    private val _healthWeights = MutableStateFlow<List<HealthCategoryWeight>>(City360SeedData.initialHealthWeights)
    val healthWeights: StateFlow<List<HealthCategoryWeight>> = _healthWeights.asStateFlow()

    private val _stories = MutableStateFlow<List<CityStoryHighlight>>(City360SeedData.initialStories)
    val stories: StateFlow<List<CityStoryHighlight>> = _stories.asStateFlow()

    private val _cityProblems = MutableStateFlow<List<CityProblem>>(City360SeedData.initialProblems)
    val cityProblems: StateFlow<List<CityProblem>> = _cityProblems.asStateFlow()

    private val _cityFeed = MutableStateFlow<List<CityObjectFeedItem>>(City360SeedData.initialCityFeed)
    val cityFeed: StateFlow<List<CityObjectFeedItem>> = _cityFeed.asStateFlow()

    private val _mahallas = MutableStateFlow<List<MahallaProfile>>(City360SeedData.initialMahallas)
    val mahallas: StateFlow<List<MahallaProfile>> = _mahallas.asStateFlow()

    private val _investments = MutableStateFlow<List<InvestmentProject>>(City360SeedData.initialInvestments)
    val investments: StateFlow<List<InvestmentProject>> = _investments.asStateFlow()

    private val _documents = MutableStateFlow<List<DocumentRecord>>(City360SeedData.initialDocuments)
    val documents: StateFlow<List<DocumentRecord>> = _documents.asStateFlow()

    private val _indicators = MutableStateFlow<List<IndicatorCatalogItem>>(City360SeedData.initialIndicators)
    val indicators: StateFlow<List<IndicatorCatalogItem>> = _indicators.asStateFlow()

    private val _aiModules = MutableStateFlow<List<AiControlModule>>(City360SeedData.initialAiModules)
    val aiModules: StateFlow<List<AiControlModule>> = _aiModules.asStateFlow()

    private val _aiSignals = MutableStateFlow<List<AiAnomalySignal>>(City360SeedData.initialAiSignals)
    val aiSignals: StateFlow<List<AiAnomalySignal>> = _aiSignals.asStateFlow()

    private val _diagonalTransfers = MutableStateFlow<List<DiagonalTransfer>>(City360SeedData.initialDiagonalTransfers)
    val diagonalTransfers: StateFlow<List<DiagonalTransfer>> = _diagonalTransfers.asStateFlow()

    val tasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val comments: StateFlow<List<TaskCommentEntity>> = repository.allComments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reports: StateFlow<List<ReportEntity>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val disciplinaryActions: StateFlow<List<DisciplinaryEntity>> = repository.allDisciplinaryActions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offlineQueue: StateFlow<List<OfflineQueueEntity>> = repository.offlineQueue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun resolveTaskRoleForUser(user: AppUser, task: TaskEntity): TaskRole {
        val short = user.name.substringBefore(" ")
        return when {
            task.finalApproverName.contains(short, ignoreCase = true) && user.role == UserRole.HOKIM -> TaskRole.YAKUNIY_TASDIQLOVCHI
            task.reviewerName.contains(short, ignoreCase = true) || user.role == UserRole.NAZORATCHI -> TaskRole.NAZORATCHI
            task.assigneeName.contains(short, ignoreCase = true) -> TaskRole.IJROCHI
            task.coAssigneesRaw.contains(short, ignoreCase = true) -> TaskRole.HAMIJROCHI
            task.responsibleLeaderName.contains(short, ignoreCase = true) -> TaskRole.MASUL_RAHBAR
            task.creatorName.contains(short, ignoreCase = true) -> TaskRole.BERUVCHI
            else -> TaskRole.KUZATUVCHI
        }
    }

    fun computeCityHealthIndex(weights: List<HealthCategoryWeight>): Int {
        val totalWeight = weights.sumOf { it.weightPercent }.coerceAtLeast(1)
        val weightedSum = weights.sumOf { it.weightPercent * it.scorePercent }
        return (weightedSum.toFloat() / totalWeight).roundToInt()
    }

    fun updateCategoryWeight(id: String, newWeight: Int) {
        _healthWeights.value = _healthWeights.value.map {
            if (it.id == id) it.copy(weightPercent = newWeight.coerceIn(0, 50)) else it
        }
    }

    fun resetHealthWeights() {
        _healthWeights.value = City360SeedData.initialHealthWeights
        showSnackbar("Shahar Sog'lig'i Indeksi vaznlari standart holatga qaytarildi.")
    }

    // Diagonal Communication Actions
    fun createDiagonalTransfer(
        type: DiagonalTransferType,
        receiverDept: String,
        receiverLeader: String,
        subject: String,
        basisDoc: String,
        deadlineText: String
    ) {
        val state = _uiState.value
        if (!state.currentUser.permissions.canSendDiagonalTransfer) {
            showSnackbar("Diagonal rasmiy uzatish yuborish vakolati cheklangan.")
            return
        }
        val seq = (43..99).random()
        val transfer = DiagonalTransfer(
            id = "diag-${System.currentTimeMillis()}",
            docCode = "DIAG-2026-%03d".format(seq),
            type = type,
            senderDept = state.currentUser.department,
            senderLeader = state.currentUserName,
            receiverDept = receiverDept.trim(),
            receiverLeader = receiverLeader.trim(),
            subject = subject.trim(),
            basisDoc = basisDoc.trim().ifBlank { "Bo'limlararo xizmat xati" },
            deadlineText = deadlineText.trim().ifBlank { "Ertaga, 18:00 gacha" },
            supervisorName = "Saidov Mirzo (Nazoratchi)",
            status = DiagonalTransferStatus.YUBORILDI,
            responseNote = null,
            aiMonitoringNote = "AI Nazorat: Yangi rasmiy uzatish ro'yxatga olindi, muddat kuzatuvda.",
            createdAtText = "Hozirgina"
        )
        _diagonalTransfers.value = listOf(transfer) + _diagonalTransfers.value
        viewModelScope.launch {
            repository.recordAuditEvent(
                actorName = state.currentUserName,
                actorRole = state.currentRole.titleUz,
                action = "DIAGONAL_TRANSFER_SENT",
                entity = transfer.docCode,
                oldValue = "DRAFT",
                newValue = "${transfer.receiverDept} (${transfer.type.labelUz})",
                reason = transfer.subject,
                result = "SUCCESS"
            )
        }
        showSnackbar("Rasmiy diagonal uzatish (${transfer.docCode}) yuborildi!")
    }

    fun acceptDiagonalTransfer(id: String) {
        _diagonalTransfers.value = _diagonalTransfers.value.map { item ->
            if (item.id == id) {
                item.copy(
                    status = DiagonalTransferStatus.QABUL_QILINDI,
                    aiMonitoringNote = "AI Nazorat: Qabul qilindi, ijro jarayoni nazorat ostida."
                )
            } else item
        }
        showSnackbar("Diagonal so'rov qabul qilindi.")
    }

    fun respondDiagonalTransfer(id: String, responseText: String) {
        if (responseText.isBlank()) return
        _diagonalTransfers.value = _diagonalTransfers.value.map { item ->
            if (item.id == id) {
                item.copy(
                    status = DiagonalTransferStatus.BAJARILDI,
                    responseNote = responseText.trim(),
                    aiMonitoringNote = "AI Nazorat: Rasmiy javob va asos hujjat ilova qilindi."
                )
            } else item
        }
        showSnackbar("Rasmiy javob yuborildi va uzatish yopildi.")
    }

    fun createTaskFromDiagonal(transfer: DiagonalTransfer) {
        val state = _uiState.value
        if (!state.currentUser.permissions.canCreateTask) {
            showSnackbar("Topshiriq yaratish uchun vakolat talab etiladi.")
            return
        }
        viewModelScope.launch {
            val assignee = _usersList.value.firstOrNull {
                it.name.contains(transfer.receiverLeader.substringBefore(" "), ignoreCase = true) ||
                    it.department.contains(transfer.receiverDept.substringBefore(" "), ignoreCase = true)
            }
            val reviewer = _usersList.value.firstOrNull { it.role == UserRole.NAZORATCHI }
            val approver = _usersList.value.firstOrNull { it.role == UserRole.HOKIM }
            val result = repository.createTaskValidated(
                creator = state.currentUser,
                assignee = assignee,
                reviewer = reviewer,
                finalApprover = approver,
                title = transfer.subject,
                description = "Diagonal xizmat xati (${transfer.docCode}) asosida yaratilgan topshiriq.\nAsos: ${transfer.basisDoc}",
                department = transfer.receiverDept,
                assigneeName = assignee?.name ?: transfer.receiverLeader,
                assigneeRole = "Bajaruvchi",
                priority = "HIGH",
                deadlineMillis = System.currentTimeMillis() + 48 * 3600_000L,
                objectName = "${transfer.receiverDept} muvofiqlashtiruvi",
                address = "Zarafshon sh., Zarafshon shahar hokimligi",
                normativAsos = transfer.basisDoc,
                kpiPoints = 15,
                subtasks = listOf(
                    SubTask("d1", "${transfer.docCode} bo'yicha masalani kelishish", false),
                    SubTask("d2", "Yakuniy ijro natijasini yuklash", false)
                ),
                sourceType = "Diagonal aloqa (${transfer.docCode})",
                confidentialityLevel = "XIZMAT_UCHUN",
                responsibleLeaderName = transfer.receiverLeader,
                coAssigneesRaw = "${transfer.senderDept} (${transfer.senderLeader})",
                reviewerName = reviewer?.name ?: "Saidov Mirzo O'ktamovich",
                finalApproverName = approver?.name ?: "Shukurov Alisher Davronovich",
                routingType = TaskRoutingType.TOGRIDAN_TOGRI.name,
                reportFormat = "HUJJAT",
                sourceCommunicationId = transfer.docCode,
                idempotencyKey = "diag-${transfer.docCode}"
            )
            when (result) {
                is WorkflowResult.Success -> {
                    _diagonalTransfers.value = _diagonalTransfers.value.map { item ->
                        if (item.id == transfer.id) item.copy(linkedTaskNumber = "Bog'langan (#${result.taskId})") else item
                    }
                    _uiState.value = _uiState.value.copy(selectedTaskId = result.taskId, selectedTab = 1, secondaryView = null)
                    showSnackbar("${transfer.docCode} asosida topshiriq yaratildi!")
                }
                is WorkflowResult.Denied -> showSnackbar(result.reason)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(themeMode = mode)
        )
    }

    fun setAccentChoice(accent: AccentChoice) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(accentChoice = accent)
        )
    }

    fun setLanguage(language: AppLanguage) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(language = language)
        )
        showSnackbar("Interfeys tili tanlandi: ${language.label}")
    }

    fun openStory(story: CityStoryHighlight?) {
        if (story != null) {
            _stories.value = _stories.value.map {
                if (it.id == story.id) it.copy(isUnread = false) else it
            }
        }
        _uiState.value = _uiState.value.copy(activeStory = story)
    }

    fun openProblemDetail(problem: CityProblem?) {
        _uiState.value = _uiState.value.copy(selectedProblem = problem)
    }

    fun showAiOverlay(show: Boolean) {
        if (show) {
            _uiState.value = _uiState.value.copy(selectedTab = 2, secondaryView = null, showAiCommandOverlay = false)
        } else {
            _uiState.value = _uiState.value.copy(showAiCommandOverlay = false)
        }
    }

    fun openSecondaryView(view: String?) {
        _uiState.value = _uiState.value.copy(secondaryView = view)
    }

    fun showFiveMinBriefing(show: Boolean) {
        _uiState.value = _uiState.value.copy(showFiveMinBriefing = show)
    }

    fun showHealthWeightsEditor(show: Boolean) {
        _uiState.value = _uiState.value.copy(showHealthWeightsEditor = show)
    }

    fun openDocumentPreview(doc: DocumentRecord?) {
        _uiState.value = _uiState.value.copy(activeDocumentPreview = doc)
    }

    fun toggleFeedVerify(feedId: String) {
        _cityFeed.value = _cityFeed.value.map { item ->
            if (item.id == feedId) {
                val next = !item.isVerifiedByUser
                item.copy(
                    isVerifiedByUser = next,
                    verifiedCount = if (next) item.verifiedCount + 1 else (item.verifiedCount - 1).coerceAtLeast(0)
                )
            } else item
        }
    }

    fun createTaskFromProblem(problem: CityProblem, customTitle: String, deadlineDays: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            val deadlineMillis = System.currentTimeMillis() + deadlineDays * 24L * 3600L * 1000L
            val newTaskId = repository.createTask(
                title = customTitle.ifBlank { problem.aiSuggestedTaskTitle },
                description = "${problem.description}\n\nAI / Shahar 360° signali: ${problem.indicatorDropNote}. Murojaatlar: ${problem.appealsCount} ta.",
                department = problem.orgName,
                assigneeName = problem.leaderName,
                assigneeRole = "Bajaruvchi",
                creatorName = state.currentUserName,
                creatorRole = state.currentRole.titleUz,
                priority = if (problem.severity.name == "KRITIK") "EMERGENCY" else "HIGH",
                deadlineMillis = deadlineMillis,
                objectName = problem.mahalla,
                address = "Zarafshon sh., ${problem.mahalla}",
                normativAsos = "Shahar 360° Tezkor Monitoring Signali (${problem.id.uppercase()})",
                kpiPoints = if (problem.severity.name == "KRITIK") 25 else 15,
                subtasks = listOf(
                    SubTask("st-1", "Obyektga chiqib holatni joyida o'rganish", false),
                    SubTask("st-2", "Kamchilikni bartaraf etish va foto/GPS hisobot yuborish", false)
                ),
                sourceType = "Shahar 360° signali",
                confidentialityLevel = if (problem.severity.name == "KRITIK") "XIZMAT_UCHUN" else "OCHIQ",
                responsibleLeaderName = problem.leaderName,
                routingType = TaskRoutingType.HUDUDIY.name,
                idempotencyKey = "problem-${problem.id}"
            )
            _cityProblems.value = _cityProblems.value.map {
                if (it.id == problem.id) {
                    it.copy(status = ProblemStatus.TOPSHIRIQ_BERILDI, linkedTaskId = newTaskId)
                } else it
            }
            _uiState.value = _uiState.value.copy(selectedProblem = null)
            showSnackbar("Muammo bo'yicha B365 topshirig'i yaratildi va ${problem.leaderName} ga yuborildi!")
        }
    }

    fun attemptCredentialsLogin(loginOrPhone: String, password: String) {
        val result = PresetUsers.authenticate(loginOrPhone, password, _usersList.value)
        when (result) {
            is AuthResult.Success -> {
                _usersList.value = _usersList.value.map {
                    if (it.id == result.user.id) result.user else it
                }
                login(result.user)
            }
            is AuthResult.InvalidCredentials -> {
                val updated = result.updatedUser
                if (updated != null) {
                    _usersList.value = _usersList.value.map {
                        if (it.id == updated.id) updated else it
                    }
                    viewModelScope.launch {
                        repository.recordAuditEvent(
                            actorName = updated.name,
                            actorRole = updated.role.titleUz,
                            action = "LOGIN_FAILED",
                            entity = "@${updated.login}",
                            oldValue = "ATTEMPT_${updated.failedLoginCount - 1}",
                            newValue = "ATTEMPT_${updated.failedLoginCount}",
                            reason = "Noto'g'ri parol kiritildi (Qolgan urinish: ${result.remainingAttempts})",
                            result = "FAILED"
                        )
                    }
                }
                _uiState.value = _uiState.value.copy(
                    authErrorMessage = "Login yoki parol noto'g'ri! Qolgan urinishlar: ${result.remainingAttempts}."
                )
            }
            is AuthResult.AccountLocked -> {
                val updated = result.updatedUser
                if (updated != null) {
                    _usersList.value = _usersList.value.map {
                        if (it.id == updated.id) updated else it
                    }
                    viewModelScope.launch {
                        repository.recordAuditEvent(
                            actorName = updated.name,
                            actorRole = updated.role.titleUz,
                            action = "ACCOUNT_LOCKED",
                            entity = "@${updated.login}",
                            oldValue = "ACTIVE",
                            newValue = "LOCKED_${result.remainingSeconds.coerceAtLeast(1L)}s",
                            reason = "5 marta noto'g'ri parol sababli vaqtincha bloklandi",
                            result = "DENIED"
                        )
                    }
                }
                val sec = result.remainingSeconds.coerceAtLeast(1L)
                _uiState.value = _uiState.value.copy(
                    authErrorMessage = "5 marta noto'g'ri urinish! Akkaunt ${sec} soniyaga vaqtincha bloklandi."
                )
            }
            is AuthResult.AccountInactive -> {
                val inactiveUser = result.user
                if (inactiveUser != null) {
                    viewModelScope.launch {
                        repository.recordAuditEvent(
                            actorName = inactiveUser.name,
                            actorRole = inactiveUser.role.titleUz,
                            action = "LOGIN_DENIED",
                            entity = "@${inactiveUser.login}",
                            oldValue = "BLOCKED",
                            newValue = "DENIED",
                            reason = result.reason,
                            result = "DENIED"
                        )
                    }
                }
                _uiState.value = _uiState.value.copy(authErrorMessage = result.reason)
            }
        }
    }

    fun login(user: AppUser) {
        if (user.isBlocked) {
            _uiState.value = _uiState.value.copy(
                authErrorMessage = "Ushbu xodim akkaunti bloklangan! Tizimga kirish uchun Markaziy administratorga murojaat qiling."
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            isAuthenticated = true,
            currentUser = user,
            currentRole = user.role,
            currentUserName = user.name,
            authErrorMessage = null,
            selectedTab = 0,
            secondaryView = null
        )
        viewModelScope.launch {
            repository.recordAuditEvent(
                actorName = user.name,
                actorRole = user.role.titleUz,
                action = "LOGIN_SUCCESS",
                entity = "@${user.login}",
                oldValue = "LOGGED_OUT",
                newValue = "SESSION_ACTIVE",
                reason = "Foydalanuvchi tizimga muvaffaqiyatli kirdi",
                result = "SUCCESS"
            )
        }
        showSnackbar("Xush kelibsiz! ${user.name} (${user.role.titleUz}) sifatida kirdingiz.")
    }

    fun logout() {
        val state = _uiState.value
        viewModelScope.launch {
            repository.recordAuditEvent(
                actorName = state.currentUserName,
                actorRole = state.currentRole.titleUz,
                action = "LOGOUT",
                entity = "@${state.currentUser.login}",
                oldValue = "SESSION_ACTIVE",
                newValue = "LOGGED_OUT",
                reason = "Foydalanuvchi sessiyani yakunladi",
                result = "SUCCESS"
            )
        }
        _uiState.value = state.copy(
            isAuthenticated = false,
            authErrorMessage = null,
            showAiCommandOverlay = false,
            secondaryView = null,
            selectedTaskId = null
        )
        showSnackbar("Sessiya yakunlandi.")
    }

    fun clearAuthError() {
        _uiState.value = _uiState.value.copy(authErrorMessage = null)
    }

    fun setRole(role: UserRole) {
        val matchingUser = _usersList.value.firstOrNull { it.role == role && !it.isBlocked }
            ?: PresetUsers.ALL_PRESETS.firstOrNull { it.role == role }
            ?: PresetUsers.HOKIM
        _uiState.value = _uiState.value.copy(
            currentRole = role,
            currentUser = matchingUser,
            currentUserName = matchingUser.name,
            secondaryView = null
        )
        showSnackbar("Global rol: ${role.titleUz} (${matchingUser.name})")
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(
            selectedTab = index,
            secondaryView = null,
            showAiCommandOverlay = false
        )
    }

    fun selectTask(taskId: Long?) {
        _uiState.value = _uiState.value.copy(selectedTaskId = taskId)
    }

    fun setTaskFilter(filter: String) {
        _uiState.value = _uiState.value.copy(taskFilter = filter)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleOfflineMode() {
        val next = !_uiState.value.isOfflineMode
        _uiState.value = _uiState.value.copy(isOfflineMode = next)
        showSnackbar(if (next) "Offline-first rejim faollashdi. Ma'lumotlar lokal xotiraga yoziladi." else "Online sinxronizatsiya tiklandi.")
    }

    fun showNewTaskDialog(show: Boolean) {
        if (show && !_uiState.value.currentUser.permissions.canCreateTask) {
            showSnackbar("${_uiState.value.currentRole.titleUz} lavozimida topshiriq yaratish ruxsati cheklangan.")
            return
        }
        _uiState.value = _uiState.value.copy(showNewTaskDialog = show)
    }

    fun showReportDialog(show: Boolean) {
        if (_uiState.value.currentRole == UserRole.TAHLILCHI || _uiState.value.currentRole == UserRole.SUPER_ADMIN) {
            showSnackbar("${_uiState.value.currentRole.titleUz} hisobot yuklay olmaydi.")
            return
        }
        _uiState.value = _uiState.value.copy(showReportDialog = show)
    }

    fun showReworkDialog(show: Boolean) {
        if (show && !_uiState.value.currentUser.permissions.canReworkReport) {
            showSnackbar("Qayta ishlashga qaytarish vakolati yo'q.")
            return
        }
        _uiState.value = _uiState.value.copy(showReworkDialog = show)
    }

    fun showRejectDialog(show: Boolean) {
        val taskId = _uiState.value.selectedTaskId
        val task = tasks.value.firstOrNull { it.id == taskId }
        val role = _uiState.value.currentRole

        if (role == UserRole.BOLIM_RAHBARI && task != null && (task.creatorRole.contains("Hokim") || task.creatorRole.contains("Rahbar") || task.creatorName.contains("Shukurov"))) {
            showSnackbar("Rahbar topshirig'ini rad etish qat'iyan taqiqlangan!")
            return
        }

        if (!_uiState.value.currentUser.permissions.canRejectTask && role != UserRole.HOKIM && role != UserRole.ORINBOSAR) {
            showSnackbar("Sizning lavozimingizda ushbu topshiriqni rad etish vakolati yo'q.")
            return
        }

        _uiState.value = _uiState.value.copy(showRejectDialog = show)
    }

    fun showDisciplineDialog(show: Boolean) {
        if (show && !_uiState.value.currentUser.permissions.canIssueDiscipline) {
            showSnackbar("Intizomiy chora taklifi kiritish faqat Rahbariyat va Nazoratchi vakolatida.")
            return
        }
        _uiState.value = _uiState.value.copy(showDisciplineDialog = show)
    }

    fun showNotificationsSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showNotificationsSheet = show)
    }

    fun showExportDialog(show: Boolean, type: String = "EXCEL") {
        if (show && !_uiState.value.currentUser.permissions.canExportReports) {
            showSnackbar("Hisobotlarni eksport qilish uchun vakolat darajasi yetarli emas.")
            return
        }
        _uiState.value = _uiState.value.copy(showExportDialog = show, exportType = type)
    }

    fun confirmExportReport(type: String) {
        val state = _uiState.value
        if (!state.currentUser.permissions.canExportReports) {
            showSnackbar("Eksport vakolati yo'q.")
            return
        }
        viewModelScope.launch {
            repository.recordAuditEvent(
                actorName = state.currentUserName,
                actorRole = state.currentRole.titleUz,
                action = "EXPORT_REPORT",
                entity = type,
                oldValue = "NONE",
                newValue = "EXPORTED_$type",
                reason = "Hisobot ($type) foydalanuvchi tomonidan yuklab olindi",
                result = "SUCCESS"
            )
        }
        _uiState.value = state.copy(showExportDialog = false)
        showSnackbar("$type hisobot tayyorlandi va audit jurnaliga yozildi!")
    }

    fun showAddUserDialog(show: Boolean) {
        if (show && !_uiState.value.currentUser.permissions.canManageStaff) {
            showSnackbar("Yangi xodim qo'shish faqat Markaziy administrator yoki Rahbar vakolatida!")
            return
        }
        _uiState.value = _uiState.value.copy(showAddUserDialog = show)
    }

    fun showAddDeptDialog(show: Boolean) {
        if (show && !_uiState.value.currentUser.permissions.canManageDepartments) {
            showSnackbar("Bo'linma qo'shish uchun tashkilot tuzilmasini boshqarish vakolati zarur.")
            return
        }
        _uiState.value = _uiState.value.copy(showAddDeptDialog = show)
    }

    fun showAddProjectDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddProjectDialog = show)
    }

    fun selectEmployeeProfile(user: AppUser?) {
        _uiState.value = _uiState.value.copy(selectedEmployeeProfile = user)
    }

    fun setUserForPermissionEdit(user: AppUser?) {
        if (user != null && !_uiState.value.currentUser.permissions.canManagePermissions) {
            showSnackbar("Vakolatlarni tahrirlash faqat Markaziy administrator vakolatida.")
            return
        }
        _uiState.value = _uiState.value.copy(userForPermissionEdit = user)
    }

    fun showSnackbar(message: String?) {
        _uiState.value = _uiState.value.copy(snackbarMessage = message)
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun createNewTask(
        title: String,
        description: String,
        department: String,
        assigneeName: String,
        priority: String,
        deadlineMillis: Long,
        objectName: String,
        address: String,
        normativAsos: String,
        kpiPoints: Int,
        subtasks: List<SubTask>,
        reviewerName: String = "Saidov Mirzo O'ktamovich",
        finalApproverName: String = "Shukurov Alisher Davronovich",
        aiMonitoringMode: String = "AI monitoring + Haftalik tasdiq",
        routingType: String = TaskRoutingType.VERTIKAL.name,
        reportFormat: String = "FOTO",
        participatingOrgsRaw: String = "",
        sourceCommunicationId: String? = null
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            val assignee = _usersList.value.firstOrNull { it.name.equals(assigneeName, ignoreCase = true) }
            val reviewer = _usersList.value.firstOrNull { it.name.equals(reviewerName, ignoreCase = true) }
            val approver = _usersList.value.firstOrNull { it.name.equals(finalApproverName, ignoreCase = true) }
            val result = repository.createTaskValidated(
                creator = state.currentUser,
                assignee = assignee,
                reviewer = reviewer,
                finalApprover = approver,
                title = title,
                description = description,
                department = department,
                assigneeName = assigneeName,
                assigneeRole = "Bajaruvchi",
                priority = priority,
                deadlineMillis = deadlineMillis,
                objectName = objectName,
                address = address,
                normativAsos = normativAsos,
                kpiPoints = kpiPoints,
                subtasks = subtasks,
                reviewerName = reviewerName,
                finalApproverName = finalApproverName,
                aiMonitoringMode = aiMonitoringMode,
                routingType = routingType,
                reportFormat = reportFormat,
                participatingOrgsRaw = participatingOrgsRaw,
                sourceCommunicationId = sourceCommunicationId
            )
            when (result) {
                is WorkflowResult.Success -> {
                    _uiState.value = _uiState.value.copy(showNewTaskDialog = false)
                    showSnackbar(result.message)
                }
                is WorkflowResult.Denied -> showSnackbar(result.reason)
            }
        }
    }

    fun addCommentToCurrentTask(type: TaskCommentType, text: String, attachmentName: String? = null) {
        val taskId = _uiState.value.selectedTaskId ?: return
        if (text.isBlank()) {
            showSnackbar("Izoh matni bo'sh bo'lmasligi kerak!")
            return
        }
        if (_uiState.value.currentRole == UserRole.TAHLILCHI) {
            showSnackbar("Tahlilchi (Read-Only) izoh qo'sha olmaydi.")
            return
        }
        viewModelScope.launch {
            val state = _uiState.value
            repository.addTaskComment(
                taskId = taskId,
                authorName = state.currentUserName,
                authorRole = state.currentRole.titleUz,
                commentType = type,
                text = text,
                attachmentName = attachmentName
            )
            showSnackbar("${type.labelUz} izoh lentasiga qo'shildi.")
        }
    }

    fun requestClarificationForCurrentTask(question: String) {
        val taskId = _uiState.value.selectedTaskId ?: return
        if (question.isBlank()) {
            showSnackbar("Aniqlashtirish savoli bo'sh bo'lmasligi kerak.")
            return
        }
        viewModelScope.launch {
            val state = _uiState.value
            repository.requestClarification(taskId, question, state.currentUserName, state.currentRole.titleUz)
            showSnackbar("Aniqlashtirish so'rovi beruvchi va nazoratchiga yuborildi.")
        }
    }

    fun requestExtensionForCurrentTask(extraHours: Long, reason: String) {
        val taskId = _uiState.value.selectedTaskId ?: return
        if (reason.isBlank()) {
            showSnackbar("Muddat uzaytirish sababini ko'rsatish majburiy.")
            return
        }
        viewModelScope.launch {
            val state = _uiState.value
            repository.requestExtension(taskId, extraHours, reason, state.currentUserName, state.currentRole.titleUz)
            showSnackbar("Muddat uzaytirish so'rovi yuborildi (joriy muddat tasdiqdan keyin o'zgaradi).")
        }
    }

    fun resolveExtensionForCurrentTask(approve: Boolean, note: String = "") {
        val taskId = _uiState.value.selectedTaskId ?: return
        viewModelScope.launch {
            val res = repository.resolveExtension(taskId, approve, _uiState.value.currentUser, note)
            when (res) {
                is WorkflowResult.Success -> showSnackbar(res.message)
                is WorkflowResult.Denied -> showSnackbar(res.reason)
            }
        }
    }

    fun toggleMultiOrgPartForCurrentTask(orgName: String) {
        val taskId = _uiState.value.selectedTaskId ?: return
        viewModelScope.launch {
            val res = repository.toggleMultiOrgPart(taskId, orgName, _uiState.value.currentUser)
            when (res) {
                is WorkflowResult.Success -> showSnackbar(res.message)
                is WorkflowResult.Denied -> showSnackbar(res.reason)
            }
        }
    }

    fun updateCurrentTaskProgress(progressPercent: Int) {
        val taskId = _uiState.value.selectedTaskId ?: return
        if (_uiState.value.currentRole == UserRole.TAHLILCHI) return
        viewModelScope.launch {
            val state = _uiState.value
            repository.updateTaskProgress(taskId, progressPercent, state.currentUserName, state.currentRole.titleUz)
        }
    }

    fun interveneCurrentTask(
        extendHours: Long,
        newReviewerName: String?,
        newAssigneeName: String?,
        instructionNote: String,
        reopenIfCompleted: Boolean
    ) {
        val taskId = _uiState.value.selectedTaskId ?: return
        if (_uiState.value.currentRole != UserRole.HOKIM) {
            showSnackbar("To'g'ridan-to'g'ri aralashuv faqat Rahbar (Hokim) uchun.")
            return
        }
        viewModelScope.launch {
            val state = _uiState.value
            repository.interveneTask(
                taskId = taskId,
                extendHours = extendHours,
                newReviewerName = newReviewerName,
                newAssigneeName = newAssigneeName,
                instructionNote = instructionNote,
                reopenIfCompleted = reopenIfCompleted,
                leaderName = state.currentUserName,
                leaderRole = state.currentRole.titleUz
            )
            showSnackbar("Rahbar ko'rsatmasi va o'zgarishlar qo'llanildi!")
        }
    }

    fun notifySupervisorAboutTask(task: TaskEntity) {
        showSnackbar("Nazoratchi ${task.reviewerName.substringBefore(" ")} ga proaktiv ogohlantirish yuborildi!")
    }

    fun acceptCurrentTask() {
        val taskId = _uiState.value.selectedTaskId ?: return
        viewModelScope.launch {
            val state = _uiState.value
            repository.acceptTask(taskId, state.currentUserName, state.currentRole.titleUz)
            showSnackbar("Topshiriq qabul qilindi va ijro boshlandi.")
        }
    }

    fun rejectCurrentTask(reason: String) {
        if (reason.isBlank()) {
            showSnackbar("Rad etish sababini yozish majburiy!")
            return
        }
        val taskId = _uiState.value.selectedTaskId ?: return
        val task = tasks.value.firstOrNull { it.id == taskId }
        val role = _uiState.value.currentRole

        if (role == UserRole.BOLIM_RAHBARI && task != null && (task.creatorRole.contains("Hokim") || task.creatorRole.contains("Rahbar") || task.creatorName.contains("Shukurov"))) {
            showSnackbar("Rahbar topshirig'ini rad etish qat'iyan taqiqlangan!")
            _uiState.value = _uiState.value.copy(showRejectDialog = false)
            return
        }

        viewModelScope.launch {
            val state = _uiState.value
            repository.rejectTask(taskId, reason, state.currentUserName, state.currentRole.titleUz)
            _uiState.value = _uiState.value.copy(showRejectDialog = false)
            showSnackbar("Topshiriq asosli rad etildi.")
        }
    }

    fun submitReportForCurrentTask(
        comment: String,
        userLat: Double,
        userLng: Double,
        photoPath: String?
    ) {
        val taskId = _uiState.value.selectedTaskId ?: return
        viewModelScope.launch {
            val state = _uiState.value
            repository.submitReport(
                taskId = taskId,
                comment = comment,
                reporterName = state.currentUserName,
                userLatitude = userLat,
                userLongitude = userLng,
                photoPath = photoPath,
                isOfflineMode = state.isOfflineMode
            )
            _uiState.value = _uiState.value.copy(showReportDialog = false)
            showSnackbar("Dalillar va izoh Tekshiruvga (Nazoratchiga) yuborildi!")
        }
    }

    fun verifyByReviewerCurrentTask() {
        val taskId = _uiState.value.selectedTaskId ?: return
        viewModelScope.launch {
            val res = repository.verifyByReviewer(taskId, _uiState.value.currentUser)
            when (res) {
                is WorkflowResult.Success -> showSnackbar(res.message)
                is WorkflowResult.Denied -> showSnackbar(res.reason)
            }
        }
    }

    fun approveCurrentTask() {
        val taskId = _uiState.value.selectedTaskId ?: return
        viewModelScope.launch {
            val res = repository.reviewApprove(taskId, _uiState.value.currentUser)
            when (res) {
                is WorkflowResult.Success -> showSnackbar(res.message)
                is WorkflowResult.Denied -> showSnackbar(res.reason)
            }
        }
    }

    fun approveTaskDirect(taskId: Long) {
        val user = _uiState.value.currentUser
        viewModelScope.launch {
            val res = if (user.permissions.canApproveReport && user.role != UserRole.SUPER_ADMIN) {
                repository.reviewApprove(taskId, user)
            } else {
                repository.verifyByReviewer(taskId, user)
            }
            when (res) {
                is WorkflowResult.Success -> showSnackbar(res.message)
                is WorkflowResult.Denied -> showSnackbar(res.reason)
            }
        }
    }

    fun reworkCurrentTask(reason: String) {
        if (reason.isBlank()) {
            showSnackbar("Qoida: Izoh yozilmasdan topshiriqni qaytarib bo'lmaydi!")
            return
        }
        val taskId = _uiState.value.selectedTaskId ?: return
        val permissions = _uiState.value.currentUser.permissions
        if (!permissions.canReworkReport) {
            showSnackbar("Qayta ishlashga yuborish uchun ruxsat berilmagan.")
            return
        }
        viewModelScope.launch {
            val state = _uiState.value
            repository.reviewRework(taskId, reason, state.currentUserName, state.currentRole.titleUz)
            _uiState.value = _uiState.value.copy(showReworkDialog = false)
            showSnackbar("Topshiriq izoh bilan qaytarildi (aylanish tsikli +1).")
        }
    }

    fun transitionCurrentTask(nextStatus: TaskStatus, reason: String) {
        val taskId = _uiState.value.selectedTaskId ?: return
        viewModelScope.launch {
            val state = _uiState.value
            repository.transitionTaskStatus(taskId, nextStatus, reason, state.currentUserName, state.currentRole.titleUz)
            showSnackbar("Topshiriq holati yangilandi: ${nextStatus.titleUz}")
        }
    }

    fun toggleSubtask(subtaskId: String) {
        val taskId = _uiState.value.selectedTaskId ?: return
        viewModelScope.launch {
            repository.toggleSubtask(taskId, subtaskId)
        }
    }

    fun issueDiscipline(
        employeeName: String,
        dept: String,
        type: String,
        reason: String
    ) {
        val taskId = _uiState.value.selectedTaskId ?: 1L
        viewModelScope.launch {
            val state = _uiState.value
            repository.issueDisciplinaryAction(
                employeeName = employeeName,
                department = dept,
                actionType = type,
                reason = reason,
                taskId = taskId,
                issuedBy = state.currentUserName
            )
            _uiState.value = _uiState.value.copy(showDisciplineDialog = false)
            showSnackbar("Intizomiy chora ($type) rasmiylashtirildi va auditga kiritildi.")
        }
    }

    fun addEmployee(
        name: String,
        login: String,
        passwordPlain: String,
        role: UserRole,
        department: String,
        position: String,
        phone: String,
        supervisorName: String,
        email: String = "",
        avatarUri: String? = null
    ) {
        val state = _uiState.value
        if (!state.currentUser.permissions.canManageStaff) {
            showSnackbar("Yangi xodim qo'shish vakolati cheklangan.")
            return
        }
        val cleanLogin = login.trim().lowercase()
        val defaultPerms = when (role) {
            UserRole.SUPER_ADMIN -> UserPermissions.SUPER_ADMIN_PERMISSIONS
            UserRole.HOKIM -> UserPermissions.HOKIM_PERMISSIONS
            UserRole.NAZORATCHI -> UserPermissions.NAZORATCHI_PERMISSIONS
            UserRole.ORINBOSAR -> UserPermissions.ORINBOSAR_PERMISSIONS
            UserRole.BOLIM_RAHBARI -> UserPermissions.BOLIM_RAHBARI_PERMISSIONS
            UserRole.TAHLILCHI -> UserPermissions.TAHLILCHI_PERMISSIONS
            UserRole.IJROCHI -> UserPermissions.IJROCHI_PERMISSIONS
        }
        val newUser = AppUser(
            id = "usr-${System.currentTimeMillis() % 10000}",
            login = cleanLogin,
            passwordHash = PasswordHasher.hashPassword(passwordPlain.trim()),
            mustChangePassword = true,
            name = name.trim(),
            role = role,
            department = department.trim(),
            position = position.trim(),
            phone = phone.trim(),
            email = email.trim().ifBlank { "$cleanLogin@zarafshon.gov.uz" },
            avatarUri = avatarUri,
            supervisorName = supervisorName.trim(),
            subordinatesCount = 0,
            securityLevel = "${role.titleUz} (Yangi qo'shilgan)",
            scope = UserScope(
                department = department.trim(),
                territory = if (role == UserRole.HOKIM || role == UserRole.NAZORATCHI || role == UserRole.ORINBOSAR) "Butun tizim (100%)" else department.trim(),
                maxConfidentialityLevel = if (role == UserRole.HOKIM || role == UserRole.SUPER_ADMIN) 4 else 2
            ),
            permissions = defaultPerms
        )
        _usersList.value = _usersList.value + newUser
        _uiState.value = _uiState.value.copy(showAddUserDialog = false)
        viewModelScope.launch {
            repository.recordAuditEvent(
                actorName = state.currentUserName,
                actorRole = state.currentRole.titleUz,
                action = "USER_CREATED",
                entity = "@${newUser.login}",
                oldValue = "NONE",
                newValue = "${newUser.name} (${newUser.role.titleUz})",
                reason = "Yangi xodim profili yaratildi va SHA-256 parol o'rnatildi",
                result = "SUCCESS"
            )
        }
        showSnackbar("Yangi xodim '${newUser.name}' (@${newUser.login}) qo'shildi!")
    }

    fun updateEmployeePassword(userId: String, newPassword: String) {
        if (newPassword.isBlank()) return
        val state = _uiState.value
        val target = _usersList.value.firstOrNull { it.id == userId } ?: return
        val hashed = PasswordHasher.hashPassword(newPassword.trim())
        _usersList.value = _usersList.value.map {
            if (it.id == userId) {
                it.copy(
                    passwordHash = hashed,
                    mustChangePassword = true,
                    failedLoginCount = 0,
                    lockedUntilMillis = 0L,
                    oneTimeResetTokenHash = null
                )
            } else it
        }
        viewModelScope.launch {
            repository.recordAuditEvent(
                actorName = state.currentUserName,
                actorRole = state.currentRole.titleUz,
                action = "RESET_PASSWORD",
                entity = "@${target.login}",
                oldValue = "HASHED_OLD",
                newValue = "HASHED_NEW (mustChange=true)",
                reason = "Xodim paroli xavfsiz xeshlangan holda yangilandi",
                result = "SUCCESS"
            )
        }
        showSnackbar("Xodim paroli yangilandi (xeshlangan formatda saqlandi)!")
    }

    fun issueOneTimeResetCode(userId: String) {
        val state = _uiState.value
        if (!state.currentUser.permissions.canResetPasswords) {
            showSnackbar("Bir martalik tiklash kodini berish vakolati yo'q.")
            return
        }
        val target = _usersList.value.firstOrNull { it.id == userId } ?: return
        val otp = PasswordHasher.generateOneTimeToken()
        val otpHash = PasswordHasher.hashPassword(otp)
        _usersList.value = _usersList.value.map {
            if (it.id == userId) {
                it.copy(
                    oneTimeResetTokenHash = otpHash,
                    mustChangePassword = true,
                    failedLoginCount = 0,
                    lockedUntilMillis = 0L
                )
            } else it
        }
        _uiState.value = _uiState.value.copy(lastGeneratedOtpHint = target.name to otp)
        viewModelScope.launch {
            repository.recordAuditEvent(
                actorName = state.currentUserName,
                actorRole = state.currentRole.titleUz,
                action = "ISSUE_ONE_TIME_RESET_TOKEN",
                entity = "@${target.login}",
                oldValue = "NONE",
                newValue = "OTP_ISSUED (expires on first use)",
                reason = "Xodimga bir martalik kirish/tiklash kodi berildi",
                result = "SUCCESS"
            )
        }
        showSnackbar("Bir martalik kod tayyor: $otp (${target.name} uchun)")
    }

    fun toggleBlockEmployee(userId: String) {
        val state = _uiState.value
        if (!state.currentUser.permissions.canManageStaff) {
            showSnackbar("Xodimni bloklash vakolati yo'q.")
            return
        }
        val target = _usersList.value.firstOrNull { it.id == userId } ?: return
        val nextBlocked = !target.isBlocked
        _usersList.value = _usersList.value.map { user ->
            if (user.id == userId) user.copy(isBlocked = nextBlocked) else user
        }
        viewModelScope.launch {
            repository.recordAuditEvent(
                actorName = state.currentUserName,
                actorRole = state.currentRole.titleUz,
                action = if (nextBlocked) "BLOCK_USER" else "UNBLOCK_USER",
                entity = "@${target.login}",
                oldValue = if (target.isBlocked) "BLOCKED" else "ACTIVE",
                newValue = if (nextBlocked) "BLOCKED" else "ACTIVE",
                reason = "Xodim kirish holati o'zgartirildi",
                result = "SUCCESS"
            )
        }
        showSnackbar("Xodim kirish maqomi yangilandi.")
    }

    fun updateUserPermissions(userId: String, newPermissions: UserPermissions) {
        val state = _uiState.value
        if (!state.currentUser.permissions.canManagePermissions) {
            showSnackbar("Vakolatlarni tahrirlash faqat Markaziy administrator vakolatida.")
            return
        }
        val target = _usersList.value.firstOrNull { it.id == userId }
        _usersList.value = _usersList.value.map { user ->
            if (user.id == userId) user.copy(permissions = newPermissions) else user
        }
        if (_uiState.value.currentUser.id == userId) {
            _uiState.value = _uiState.value.copy(
                currentUser = _uiState.value.currentUser.copy(permissions = newPermissions)
            )
        }
        _uiState.value = _uiState.value.copy(userForPermissionEdit = null)
        viewModelScope.launch {
            repository.recordAuditEvent(
                actorName = state.currentUserName,
                actorRole = state.currentRole.titleUz,
                action = "UPDATE_USER_PERMISSIONS",
                entity = target?.login?.let { "@$it" } ?: userId,
                oldValue = "CUSTOM_TEMPLATE",
                newValue = "UPDATED_RBAC",
                reason = "Foydalanuvchi individual vakolatlari yangilandi",
                result = "SUCCESS"
            )
        }
        showSnackbar("Xodimning vakolatlar shabloni saqlandi!")
    }

    fun updateRolePermissions(role: UserRole, newPermissions: UserPermissions) {
        val state = _uiState.value
        if (!state.currentUser.permissions.canManagePermissions) {
            showSnackbar("Rol matritsasini o'zgartirish faqat Markaziy administrator vakolatida.")
            return
        }
        _usersList.value = _usersList.value.map { user ->
            if (user.role == role) user.copy(permissions = newPermissions) else user
        }
        if (_uiState.value.currentUser.role == role) {
            _uiState.value = _uiState.value.copy(
                currentUser = _uiState.value.currentUser.copy(permissions = newPermissions)
            )
        }
        viewModelScope.launch {
            repository.recordAuditEvent(
                actorName = state.currentUserName,
                actorRole = state.currentRole.titleUz,
                action = "UPDATE_ROLE_MATRIX",
                entity = role.name,
                oldValue = "ROLE_TEMPLATE",
                newValue = "UPDATED_MATRIX",
                reason = "${role.titleUz} uchun vakolatlar matritsasi yangilandi",
                result = "SUCCESS"
            )
        }
        showSnackbar("${role.titleUz} uchun vakolatlar matritsasi saqlandi!")
    }

    fun runBackupSnapshotTest() {
        val state = _uiState.value
        if (!state.currentUser.permissions.canModifySystemSettings) {
            showSnackbar("Zaxira nusxa va tiklash sinovi faqat Markaziy administrator uchun.")
            return
        }
        viewModelScope.launch {
            val summary = repository.createBackupSnapshotSummary(state.currentUser)
            _uiState.value = _uiState.value.copy(lastBackupVerification = summary)
            showSnackbar(summary)
        }
    }

    fun addInvestmentProject(
        title: String,
        sector: String,
        budgetUsdMillion: Double,
        contractor: String,
        jobsPlanned: Int
    ) {
        val project = InvestmentProject(
            id = "inv-${System.currentTimeMillis() % 10000}",
            title = title.trim(),
            sector = sector.trim().ifBlank { "Shahar infratuzilmasi" },
            budgetUsdMillion = budgetUsdMillion,
            progressPercent = 15,
            jobsPlanned = jobsPlanned,
            contractor = contractor.trim().ifBlank { "Zarafshon Qurilish" },
            statusLabel = "Rejalashtirilgan",
            isDelayed = false,
            imageResId = com.example.R.drawable.img_city_construction_1795671434776
        )
        _investments.value = listOf(project) + _investments.value
        _uiState.value = _uiState.value.copy(showAddProjectDialog = false)
        showSnackbar("Yangi loyiha '${project.title}' dasturga qo'shildi!")
    }

    fun addDepartment(
        name: String,
        code: String,
        headName: String,
        staffCount: Int
    ) {
        val state = _uiState.value
        if (!state.currentUser.permissions.canManageDepartments) {
            showSnackbar("Bo'linma qo'shish vakolati cheklangan.")
            return
        }
        val newDept = DepartmentInfo(
            id = "dept-${System.currentTimeMillis() % 1000}",
            name = name.trim(),
            code = code.trim().uppercase(),
            headName = headName.trim(),
            staffCount = staffCount,
            activeTasksCount = 0
        )
        _departmentsList.value = _departmentsList.value + newDept
        _uiState.value = _uiState.value.copy(showAddDeptDialog = false)
        viewModelScope.launch {
            repository.recordAuditEvent(
                actorName = state.currentUserName,
                actorRole = state.currentRole.titleUz,
                action = "DEPARTMENT_CREATED",
                entity = newDept.code,
                oldValue = "NONE",
                newValue = newDept.name,
                reason = "Yangi tashkiliy bo'linma qo'shildi",
                result = "SUCCESS"
            )
        }
        showSnackbar("Yangi bo'linma '${newDept.name}' ro'yxatga olindi!")
    }

    fun toggleBlockDepartment(deptId: String) {
        val state = _uiState.value
        if (!state.currentUser.permissions.canManageDepartments) {
            showSnackbar("Bo'linma holatini o'zgartirish vakolati yo'q.")
            return
        }
        _departmentsList.value = _departmentsList.value.map { dept ->
            if (dept.id == deptId) dept.copy(isBlocked = !dept.isBlocked) else dept
        }
        showSnackbar("Bo'linma holati o'zgartirildi.")
    }

    fun sendAiQuery(query: String) {
        if (query.isBlank()) return
        val currentMsgs = _uiState.value.aiMessages.toMutableList()
        currentMsgs.add(Pair(true, query))
        _uiState.value = _uiState.value.copy(aiMessages = currentMsgs)

        viewModelScope.launch {
            val currentTasks = tasks.value
            val response = AiExecutiveAssistant.answerExecutiveQuery(query, currentTasks)
            val updated = _uiState.value.aiMessages.toMutableList()
            updated.add(Pair(false, response))
            _uiState.value = _uiState.value.copy(aiMessages = updated)
        }
    }

    fun syncOffline() {
        viewModelScope.launch {
            repository.syncOfflineQueue()
            showSnackbar("Offline navbat server bilan to'liq sinxronlashtirildi!")
        }
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
            showSnackbar("Barcha bildirishnomalar o'qilgan deb belgilandi.")
        }
    }
}
