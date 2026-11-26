package com.example.model

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Cryptographic salted password hashing utility (SHA-256 + per-user random salt).
 * Never stores or exposes plaintext passwords in user models or admin interfaces.
 */
object PasswordHasher {
    private val rng = SecureRandom()

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        rng.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(rawPassword: String, salt: String = generateSalt()): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$salt:${rawPassword.trim()}".toByteArray(Charsets.UTF_8)
        val hashed = digest.digest(input)
        val hex = hashed.joinToString("") { "%02x".format(it) }
        return "$salt$$hex"
    }

    fun verifyPassword(rawPassword: String, storedHash: String): Boolean {
        val parts = storedHash.split("$")
        if (parts.size != 2) return false
        val salt = parts[0]
        val computed = hashPassword(rawPassword, salt)
        return MessageDigest.isEqual(
            computed.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
    }

    fun generateOneTimeToken(): String {
        val code = 100000 + rng.nextInt(900000)
        return "B365-OTP-$code"
    }
}

sealed class AuthResult {
    data class Success(val user: AppUser) : AuthResult()
    data class InvalidCredentials(
        val failedCount: Int,
        val remainingAttempts: Int,
        val lockedUntilMillis: Long,
        val updatedUser: AppUser? = null
    ) : AuthResult()
    data class AccountLocked(
        val lockedUntilMillis: Long,
        val remainingSeconds: Long = ((lockedUntilMillis - System.currentTimeMillis()) / 1000L).coerceAtLeast(1L),
        val updatedUser: AppUser? = null
    ) : AuthResult()
    data class AccountInactive(
        val reason: String,
        val user: AppUser? = null
    ) : AuthResult()
}

/**
 * ABAC Scope defining organization, department, territory, and confidentiality clearance.
 */
data class UserScope(
    val organization: String = "Zarafshon shahar hokimligi",
    val department: String,
    val territory: String = "Zarafshon shahri",
    val maxConfidentialityLevel: Int = 2 // 1=Ochiq, 2=Xizmat, 3=Maxfiy, 4=O'ta maxfiy
)

data class UserPermissions(
    val canCreateTask: Boolean = false,
    val canAssignToOwnStaff: Boolean = false,
    val canAssignToAll: Boolean = false,
    val canViewReports: Boolean = true,
    val canReviewEvidence: Boolean = false,     // Stage 1: Nazorat tekshiruvi
    val canApproveReport: Boolean = false,      // Stage 2: Yakuniy tasdiq va yopish
    val canReworkReport: Boolean = false,
    val canRejectTask: Boolean = false,
    val canArchiveTask: Boolean = false,
    val canSendDiagonalTransfer: Boolean = true,
    val canManageStaff: Boolean = false,
    val canBlockStaff: Boolean = false,
    val canManagePermissions: Boolean = false,
    val canManageDepartments: Boolean = false,
    val canIssueDiscipline: Boolean = false,
    val canViewAllKpi: Boolean = false,
    val canUseAiAssistant: Boolean = false,
    val canExportReports: Boolean = false,
    val canModifySystemSettings: Boolean = false,
    val canResetPasswords: Boolean = false,
    val canEditDeadline: Boolean = false
) {
    companion object {
        val HOKIM_PERMISSIONS = UserPermissions(
            canCreateTask = true,
            canAssignToOwnStaff = true,
            canAssignToAll = true,
            canViewReports = true,
            canReviewEvidence = true,
            canApproveReport = true,
            canReworkReport = true,
            canRejectTask = false,
            canArchiveTask = true,
            canSendDiagonalTransfer = true,
            canManageStaff = true,
            canBlockStaff = true,
            canManagePermissions = false,
            canManageDepartments = true,
            canIssueDiscipline = true,
            canViewAllKpi = true,
            canUseAiAssistant = true,
            canExportReports = true,
            canModifySystemSettings = false,
            canResetPasswords = true,
            canEditDeadline = true
        )

        /**
         * TZ Section 4.2 & 13: Markaziy administrator manages technical setup, orgs, users,
         * permissions matrix, backup & audit — NEVER gains automatic task-approval/closing rights.
         */
        val SUPER_ADMIN_PERMISSIONS = UserPermissions(
            canCreateTask = false,
            canAssignToOwnStaff = false,
            canAssignToAll = false,
            canViewReports = true,
            canReviewEvidence = false,
            canApproveReport = false,
            canReworkReport = false,
            canRejectTask = false,
            canArchiveTask = false,
            canSendDiagonalTransfer = false,
            canManageStaff = true,
            canBlockStaff = true,
            canManagePermissions = true,
            canManageDepartments = true,
            canIssueDiscipline = false,
            canViewAllKpi = true,
            canUseAiAssistant = false,
            canExportReports = true,
            canModifySystemSettings = true,
            canResetPasswords = true,
            canEditDeadline = false
        )

        val ORINBOSAR_PERMISSIONS = UserPermissions(
            canCreateTask = true,
            canAssignToOwnStaff = true,
            canAssignToAll = false,
            canViewReports = true,
            canReviewEvidence = true,
            canApproveReport = true,
            canReworkReport = true,
            canRejectTask = false,
            canArchiveTask = true,
            canSendDiagonalTransfer = true,
            canManageStaff = false,
            canBlockStaff = false,
            canManagePermissions = false,
            canManageDepartments = false,
            canIssueDiscipline = true,
            canViewAllKpi = true,
            canUseAiAssistant = true,
            canExportReports = true,
            canModifySystemSettings = false,
            canResetPasswords = false,
            canEditDeadline = true
        )

        val BOLIM_RAHBARI_PERMISSIONS = UserPermissions(
            canCreateTask = true,
            canAssignToOwnStaff = true,
            canAssignToAll = false,
            canViewReports = true,
            canReviewEvidence = true,
            canApproveReport = true,
            canReworkReport = true,
            canRejectTask = false,
            canArchiveTask = false,
            canSendDiagonalTransfer = true,
            canManageStaff = false,
            canBlockStaff = false,
            canManagePermissions = false,
            canManageDepartments = false,
            canIssueDiscipline = false,
            canViewAllKpi = false,
            canUseAiAssistant = true,
            canExportReports = true,
            canModifySystemSettings = false
        )

        val IJROCHI_PERMISSIONS = UserPermissions(
            canCreateTask = false,
            canAssignToOwnStaff = false,
            canAssignToAll = false,
            canViewReports = true,
            canReviewEvidence = false,
            canApproveReport = false,
            canReworkReport = false,
            canRejectTask = true,
            canArchiveTask = false,
            canSendDiagonalTransfer = false,
            canManageStaff = false,
            canBlockStaff = false,
            canManagePermissions = false,
            canManageDepartments = false,
            canIssueDiscipline = false,
            canViewAllKpi = false,
            canUseAiAssistant = false,
            canExportReports = false,
            canModifySystemSettings = false
        )

        val NAZORATCHI_PERMISSIONS = UserPermissions(
            canCreateTask = true,
            canAssignToOwnStaff = false,
            canAssignToAll = false,
            canViewReports = true,
            canReviewEvidence = true,
            canApproveReport = false,
            canReworkReport = true,
            canRejectTask = false,
            canArchiveTask = false,
            canSendDiagonalTransfer = true,
            canManageStaff = false,
            canBlockStaff = false,
            canManagePermissions = false,
            canManageDepartments = false,
            canIssueDiscipline = true,
            canViewAllKpi = true,
            canUseAiAssistant = true,
            canExportReports = true,
            canModifySystemSettings = false
        )

        val TAHLILCHI_PERMISSIONS = UserPermissions(
            canCreateTask = false,
            canAssignToOwnStaff = false,
            canAssignToAll = false,
            canViewReports = true,
            canReviewEvidence = false,
            canApproveReport = false,
            canReworkReport = false,
            canRejectTask = false,
            canArchiveTask = false,
            canSendDiagonalTransfer = false,
            canManageStaff = false,
            canBlockStaff = false,
            canManagePermissions = false,
            canManageDepartments = false,
            canIssueDiscipline = false,
            canViewAllKpi = true,
            canUseAiAssistant = true,
            canExportReports = true,
            canModifySystemSettings = false
        )
    }
}

data class DepartmentInfo(
    val id: String,
    val name: String,
    val code: String,
    val headName: String,
    val staffCount: Int,
    val activeTasksCount: Int,
    val isBlocked: Boolean = false
)

data class AppUser(
    val id: String = java.util.UUID.randomUUID().toString(),
    val login: String,
    val passwordHash: String,
    val name: String,
    val role: UserRole,
    val department: String,
    val position: String,
    val phone: String,
    val email: String = "${login}@zarafshon.gov.uz",
    val avatarUri: String? = null,
    val supervisorName: String = "Shukurov A. D.",
    val subordinatesCount: Int = 0,
    val isBlocked: Boolean = false,
    val isOnline: Boolean = true,
    val mustChangePassword: Boolean = false,
    val oneTimeResetTokenHash: String? = null,
    val failedLoginCount: Int = 0,
    val lockedUntilMillis: Long = 0L,
    val validFromMillis: Long = 0L,
    val validToMillis: Long? = null,
    val digitalSignatureId: String = "ERI-ZRF-${login.uppercase()}-2026",
    val securityLevel: String = "2-Daraja (Himoyalangan)",
    val scope: UserScope = UserScope(
        department = department,
        territory = when (role) {
            UserRole.HOKIM, UserRole.SUPER_ADMIN, UserRole.ORINBOSAR, UserRole.NAZORATCHI -> "Butun tizim (100%)"
            UserRole.TAHLILCHI -> "Tahliliy bo'limlar (Read-Only)"
            UserRole.BOLIM_RAHBARI -> "$department + Diagonal"
            UserRole.IJROCHI -> "Faqat shaxsiy vazifalar"
        },
        maxConfidentialityLevel = when (role) {
            UserRole.HOKIM -> 4
            UserRole.SUPER_ADMIN -> 2
            UserRole.ORINBOSAR, UserRole.NAZORATCHI -> 3
            UserRole.BOLIM_RAHBARI, UserRole.TAHLILCHI -> 2
            UserRole.IJROCHI -> 2
        }
    ),
    val permissions: UserPermissions = when (role) {
        UserRole.HOKIM -> UserPermissions.HOKIM_PERMISSIONS
        UserRole.SUPER_ADMIN -> UserPermissions.SUPER_ADMIN_PERMISSIONS
        UserRole.ORINBOSAR -> UserPermissions.ORINBOSAR_PERMISSIONS
        UserRole.BOLIM_RAHBARI -> UserPermissions.BOLIM_RAHBARI_PERMISSIONS
        UserRole.IJROCHI -> UserPermissions.IJROCHI_PERMISSIONS
        UserRole.NAZORATCHI -> UserPermissions.NAZORATCHI_PERMISSIONS
        UserRole.TAHLILCHI -> UserPermissions.TAHLILCHI_PERMISSIONS
    },
    val loginHistory: List<String> = listOf(
        "Bugun, 08:30 - Mobil ilova (Android / Zarafshon)",
        "Kecha, 17:45 - Yopiq Web Portal (Zarafshon Hokimlik tarmog'i)",
        "26.09.2026, 09:12 - Mobil ilova"
    ),
    val activeDevices: List<String> = listOf(
        "Samsung Galaxy Tab S9 (Zarafshon Hokimlik maxsus plansheti)",
        "Xiaomi 13 Pro (Ishchi mobil qurilma)"
    )
)

object PresetUsers {
    const val MAX_FAILED_ATTEMPTS = 5
    const val LOCKOUT_DURATION_MS = 60_000L

    private val DEMO_PASSWORDS = mapOf(
        "hokim" to "admin365",
        "admin" to "sysadmin365",
        "orinbosar" to "deputy365",
        "boshliq" to "leader365",
        "ijrochi" to "xodim365",
        "nazorat" to "audit365",
        "tahlilchi" to "analyst365"
    )

    val HOKIM = AppUser(
        id = "usr-001",
        login = "hokim",
        passwordHash = PasswordHasher.hashPassword("admin365", "salt_usr001"),
        name = "Shukurov Alisher Davronovich",
        role = UserRole.HOKIM,
        department = "Zarafshon shahar hokimligi apparati",
        position = "Zarafshon shahar hokimi (Rahbar)",
        phone = "+998 79 573-00-01",
        email = "hokim@zarafshon.gov.uz",
        supervisorName = "Navoiy viloyati hokimi",
        subordinatesCount = 48,
        securityLevel = "1-toifali Maxsus Rahbar",
        permissions = UserPermissions.HOKIM_PERMISSIONS
    )

    val SUPER_ADMIN = AppUser(
        id = "usr-000",
        login = "admin",
        passwordHash = PasswordHasher.hashPassword("sysadmin365", "salt_usr000"),
        name = "Toshmatov Azizbek Karimovich",
        role = UserRole.SUPER_ADMIN,
        department = "Axborot xavfsizligi va tizim boshqaruvi",
        position = "Markaziy Tizim Administratori",
        phone = "+998 79 573-00-00",
        email = "sysadmin@zarafshon.gov.uz",
        supervisorName = "Shukurov Alisher Davronovich (Rahbar)",
        subordinatesCount = 2,
        securityLevel = "Markaziy Texnik Admin (RBAC/Audit)",
        permissions = UserPermissions.SUPER_ADMIN_PERMISSIONS
    )

    val ORINBOSAR = AppUser(
        id = "usr-002",
        login = "orinbosar",
        passwordHash = PasswordHasher.hashPassword("deputy365", "salt_usr002"),
        name = "Normurodov Ilhom Erkinovich",
        role = UserRole.ORINBOSAR,
        department = "Qurilish, kommunikatsiyalar va ekologiya kompleksi",
        position = "Hokim o'rinbosari (Nazoratchi)",
        phone = "+998 79 573-00-02",
        email = "deputy.normurodov@zarafshon.gov.uz",
        supervisorName = "Shukurov Alisher Davronovich (Rahbar)",
        subordinatesCount = 18,
        securityLevel = "2-toifali Nazoratchi-Rahbar",
        permissions = UserPermissions.ORINBOSAR_PERMISSIONS
    )

    val BOLIM_RAHBARI = AppUser(
        id = "usr-003",
        login = "boshliq",
        passwordHash = PasswordHasher.hashPassword("leader365", "salt_usr003"),
        name = "To'rayev Sherzod Mahmudovich",
        role = UserRole.BOLIM_RAHBARI,
        department = "Obodonlashtirish boshqarmasi",
        position = "Boshqarma boshlig'i (Bo'lim mas'uli)",
        phone = "+998 79 573-10-15",
        email = "sherzod.turayev@zarafshon.gov.uz",
        supervisorName = "Normurodov I. E. (Nazoratchi)",
        subordinatesCount = 14,
        securityLevel = "3-toifali Bo'lim Mas'uli",
        permissions = UserPermissions.BOLIM_RAHBARI_PERMISSIONS
    )

    val IJROCHI = AppUser(
        id = "usr-004",
        login = "ijrochi",
        passwordHash = PasswordHasher.hashPassword("xodim365", "salt_usr004"),
        name = "Karimov Jasur Bahromovich",
        role = UserRole.IJROCHI,
        department = "Obodonlashtirish boshqarmasi",
        position = "Bosh muhandis / Bajaruvchi",
        phone = "+998 93 456-78-90",
        email = "j.karimov@zarafshon.gov.uz",
        supervisorName = "To'rayev Sherzod Mahmudovich",
        subordinatesCount = 0,
        securityLevel = "Ijrochi xodim",
        permissions = UserPermissions.IJROCHI_PERMISSIONS
    )

    val NAZORATCHI = AppUser(
        id = "usr-005",
        login = "nazorat",
        passwordHash = PasswordHasher.hashPassword("audit365", "salt_usr005"),
        name = "Saidov Mirzo O'ktamovich",
        role = UserRole.NAZORATCHI,
        department = "Ijro intizomi va nazorat bo'limi",
        position = "Bosh nazoratchi-inspektor",
        phone = "+998 79 573-00-11",
        email = "m.saidov@zarafshon.gov.uz",
        supervisorName = "Shukurov Alisher Davronovich (Rahbar)",
        subordinatesCount = 3,
        securityLevel = "Nazoratchi-Inspektor",
        permissions = UserPermissions.NAZORATCHI_PERMISSIONS
    )

    val TAHLILCHI = AppUser(
        id = "usr-006",
        login = "tahlilchi",
        passwordHash = PasswordHasher.hashPassword("analyst365", "salt_usr006"),
        name = "Alimov Murod Rustamovich",
        role = UserRole.TAHLILCHI,
        department = "Axborot-tahlil sho'basi",
        position = "Bosh tahlilchi (Auditor)",
        phone = "+998 79 573-00-19",
        email = "m.alimov@zarafshon.gov.uz",
        supervisorName = "Shukurov Alisher Davronovich (Rahbar)",
        subordinatesCount = 0,
        securityLevel = "Tahlilchi (Read-Only)",
        permissions = UserPermissions.TAHLILCHI_PERMISSIONS
    )

    val ALL_PRESETS = listOf(HOKIM, SUPER_ADMIN, ORINBOSAR, BOLIM_RAHBARI, IJROCHI, NAZORATCHI, TAHLILCHI)

    val INITIAL_DEPARTMENTS = listOf(
        DepartmentInfo("dept-1", "Obodonlashtirish boshqarmasi", "OBOD", "To'rayev Sh. M.", 14, 6),
        DepartmentInfo("dept-2", "Qurilish va arxitektura bo'limi", "QUR", "Raxmonov B. S.", 9, 4),
        DepartmentInfo("dept-3", "Uy-joy kommunal xizmat ko'rsatish (UJKX)", "UJKX", "Haydarov O. T.", 12, 5),
        DepartmentInfo("dept-4", "Iqtisodiyot va moliya bo'limi", "IQT", "Mansurov Q. A.", 8, 3),
        DepartmentInfo("dept-5", "Yoshlar siyosati va sport bo'limi", "YOSH", "Boboyev D. R.", 6, 2),
        DepartmentInfo("dept-6", "Ekologiya va atrof-muhitni muhofaza qilish", "EKO", "Sobirov N. J.", 5, 2)
    )

    fun demoPasswordHintForLoginScreen(login: String): String =
        DEMO_PASSWORDS[login.lowercase()] ?: ""

    fun findUserByIdentifier(loginOrPhone: String, extraUsers: List<AppUser> = emptyList()): AppUser? {
        val clean = loginOrPhone.trim().lowercase()
        if (clean.isBlank()) return null
        val pool = (extraUsers + ALL_PRESETS).distinctBy { it.id }
        return pool.firstOrNull {
            it.login.lowercase() == clean || it.phone.replace(" ", "").contains(clean)
        }
    }

    fun authenticate(
        loginOrPhone: String,
        password: String,
        usersPool: List<AppUser>,
        nowMillis: Long = System.currentTimeMillis()
    ): AuthResult {
        val user = findUserByIdentifier(loginOrPhone, usersPool)
            ?: return AuthResult.InvalidCredentials(1, MAX_FAILED_ATTEMPTS - 1, 0L)

        if (user.isBlocked) {
            return AuthResult.AccountInactive("Ushbu xodim akkaunti bloklangan.", user)
        }
        if (user.validToMillis != null && nowMillis > user.validToMillis) {
            return AuthResult.AccountInactive("Akkauntning amal qilish muddati tugagan.", user)
        }
        if (user.lockedUntilMillis > nowMillis) {
            val remSec = ((user.lockedUntilMillis - nowMillis) / 1000L).coerceAtLeast(1L)
            return AuthResult.AccountLocked(user.lockedUntilMillis, remSec, user)
        }

        val pass = password.trim()
        val hashMatched = pass.isNotBlank() && PasswordHasher.verifyPassword(pass, user.passwordHash)
        val otpMatched = pass.isNotBlank() && user.oneTimeResetTokenHash != null &&
            PasswordHasher.verifyPassword(pass, user.oneTimeResetTokenHash)

        if (hashMatched || otpMatched) {
            return AuthResult.Success(
                user.copy(
                    failedLoginCount = 0,
                    lockedUntilMillis = 0L,
                    oneTimeResetTokenHash = if (otpMatched) null else user.oneTimeResetTokenHash
                )
            )
        }

        val nextFailed = user.failedLoginCount + 1
        val lockedUntil = if (nextFailed >= MAX_FAILED_ATTEMPTS) nowMillis + LOCKOUT_DURATION_MS else 0L
        val updatedUser = user.copy(
            failedLoginCount = nextFailed,
            lockedUntilMillis = lockedUntil
        )
        if (lockedUntil > nowMillis) {
            val remSec = ((lockedUntil - nowMillis) / 1000L).coerceAtLeast(1L)
            return AuthResult.AccountLocked(lockedUntil, remSec, updatedUser)
        }
        return AuthResult.InvalidCredentials(
            failedCount = nextFailed,
            remainingAttempts = (MAX_FAILED_ATTEMPTS - nextFailed).coerceAtLeast(0),
            lockedUntilMillis = lockedUntil,
            updatedUser = updatedUser
        )
    }

    fun findByCredentials(loginOrPhone: String, password: String, extraUsers: List<AppUser> = emptyList()): AppUser? {
        return when (val res = authenticate(loginOrPhone, password, extraUsers)) {
            is AuthResult.Success -> res.user
            else -> null
        }
    }
}
