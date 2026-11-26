package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.BoshqaruvRepository
import com.example.data.WorkflowResult
import com.example.model.AuthResult
import com.example.model.PasswordHasher
import com.example.model.PresetUsers
import com.example.model.SubTask
import com.example.model.TaskRoutingType
import com.example.model.TaskStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BoshqaruvBusinessRulesTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: BoshqaruvRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = BoshqaruvRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `password hashing verifies valid password and rejects wrong password`() {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword("Zarafshon#365", salt)

        assertTrue(hash.contains("$"))
        assertFalse(hash.contains("Zarafshon#365"))
        assertTrue(PasswordHasher.verifyPassword("Zarafshon#365", hash))
        assertFalse(PasswordHasher.verifyPassword("WrongPass", hash))
        assertTrue(PasswordHasher.verifyPassword("admin365", PresetUsers.HOKIM.passwordHash))
        assertTrue(PasswordHasher.verifyPassword("sysadmin365", PresetUsers.SUPER_ADMIN.passwordHash))
    }

    @Test
    fun `brute force protection locks account after 5 failed attempts and throttles within lock window`() {
        var pool = PresetUsers.ALL_PRESETS
        val now = 1_700_000_000_000L

        repeat(PresetUsers.MAX_FAILED_ATTEMPTS - 1) { index ->
            val res = PresetUsers.authenticate("ijrochi", "bad_$index", pool, now)
            assertTrue(res is AuthResult.InvalidCredentials)
            val invalid = res as AuthResult.InvalidCredentials
            assertEquals(index + 1, invalid.failedCount)
            val updated = invalid.updatedUser!!
            pool = pool.map { if (it.id == updated.id) updated else it }
        }

        // 5th failure triggers AccountLocked
        val lockedRes = PresetUsers.authenticate("ijrochi", "bad_5", pool, now)
        assertTrue(lockedRes is AuthResult.AccountLocked)
        val lockedUser = (lockedRes as AuthResult.AccountLocked).updatedUser!!
        pool = pool.map { if (it.id == lockedUser.id) lockedUser else it }

        // Even with correct password inside 60s lock window, login stays locked
        val blockedAttempt = PresetUsers.authenticate("ijrochi", "xodim365", pool, now + 10_000L)
        assertTrue(blockedAttempt is AuthResult.AccountLocked)

        // After 60s lock expires, valid password succeeds and resets failedLoginCount
        val unlockedAttempt = PresetUsers.authenticate(
            "ijrochi",
            "xodim365",
            pool,
            now + PresetUsers.LOCKOUT_DURATION_MS + 1_000L
        )
        assertTrue(unlockedAttempt is AuthResult.Success)
        assertEquals(0, (unlockedAttempt as AuthResult.Success).user.failedLoginCount)
    }

    @Test
    fun `one time reset token works once and is cleared after login`() {
        val otp = PasswordHasher.generateOneTimeToken()
        val otpHash = PasswordHasher.hashPassword(otp)
        val target = PresetUsers.IJROCHI.copy(
            oneTimeResetTokenHash = otpHash,
            mustChangePassword = true
        )
        val pool = PresetUsers.ALL_PRESETS.map { if (it.id == target.id) target else it }

        val firstTry = PresetUsers.authenticate("ijrochi", otp, pool)
        assertTrue(firstTry is AuthResult.Success)
        val loggedInUser = (firstTry as AuthResult.Success).user
        assertNull(loggedInUser.oneTimeResetTokenHash)

        val updatedPool = pool.map { if (it.id == loggedInUser.id) loggedInUser else it }
        val secondTry = PresetUsers.authenticate("ijrochi", otp, updatedPool)
        assertTrue(secondTry is AuthResult.InvalidCredentials)
    }

    @Test
    fun `blocked or expired account returns AccountInactive`() {
        val blockedUser = PresetUsers.IJROCHI.copy(isBlocked = true)
        val expiredUser = PresetUsers.BOLIM_RAHBARI.copy(validToMillis = 1_000L)
        val pool = listOf(blockedUser, expiredUser)

        val blockedRes = PresetUsers.authenticate("ijrochi", "xodim365", pool, 2_000_000L)
        assertTrue(blockedRes is AuthResult.AccountInactive)

        val expiredRes = PresetUsers.authenticate("boshliq", "leader365", pool, 2_000_000L)
        assertTrue(expiredRes is AuthResult.AccountInactive)
    }

    @Test
    fun `createTaskValidated generates B365 prefix and enforces idempotency key deduplication`() = runBlocking {
        val key = "idem-create-001"
        val first = repo.createTaskValidated(
            creator = PresetUsers.HOKIM,
            assignee = PresetUsers.IJROCHI,
            reviewer = PresetUsers.NAZORATCHI,
            finalApprover = PresetUsers.HOKIM,
            title = "Markaziy ko'cha yoritgichlari",
            description = "42 ta ustunni tekshirish",
            department = PresetUsers.IJROCHI.department,
            assigneeName = PresetUsers.IJROCHI.name,
            assigneeRole = "Ijrochi",
            priority = "HIGH",
            deadlineMillis = System.currentTimeMillis() + 86_400_000L,
            objectName = "12-kichik nohiya",
            address = "Zarafshon sh.",
            normativAsos = "Farmoyish #10",
            kpiPoints = 15,
            subtasks = listOf(SubTask("s1", "Tekshiruv", false)),
            idempotencyKey = key
        )
        assertTrue(first is WorkflowResult.Success)
        val firstId = (first as WorkflowResult.Success).taskId

        val duplicate = repo.createTaskValidated(
            creator = PresetUsers.HOKIM,
            assignee = PresetUsers.IJROCHI,
            reviewer = PresetUsers.NAZORATCHI,
            finalApprover = PresetUsers.HOKIM,
            title = "Markaziy ko'cha yoritgichlari",
            description = "42 ta ustunni tekshirish",
            department = PresetUsers.IJROCHI.department,
            assigneeName = PresetUsers.IJROCHI.name,
            assigneeRole = "Ijrochi",
            priority = "HIGH",
            deadlineMillis = System.currentTimeMillis() + 86_400_000L,
            objectName = "12-kichik nohiya",
            address = "Zarafshon sh.",
            normativAsos = "Farmoyish #10",
            kpiPoints = 15,
            subtasks = listOf(SubTask("s1", "Tekshiruv", false)),
            idempotencyKey = key
        )
        assertTrue(duplicate is WorkflowResult.Success)
        assertEquals(firstId, (duplicate as WorkflowResult.Success).taskId)
        assertEquals(1, db.taskDao().getTaskCountOnce())

        val saved = repo.getTaskById(firstId)!!
        assertTrue(saved.taskNumber.startsWith("B365-2026-"))
    }

    @Test
    fun `createTaskValidated denies SoD violations blocked users scope and clearance violations`() = runBlocking {
        // 1. Reviewer == Final Approver denied
        val sameRevApprover = repo.createTaskValidated(
            creator = PresetUsers.HOKIM,
            assignee = PresetUsers.IJROCHI,
            reviewer = PresetUsers.NAZORATCHI,
            finalApprover = PresetUsers.NAZORATCHI,
            title = "SoD test 1",
            description = "Desc",
            department = PresetUsers.IJROCHI.department,
            assigneeName = PresetUsers.IJROCHI.name,
            assigneeRole = "Ijrochi",
            priority = "HIGH",
            deadlineMillis = System.currentTimeMillis() + 3_600_000L,
            objectName = "Obj",
            address = "Addr",
            normativAsos = "Asos",
            kpiPoints = 10,
            subtasks = emptyList()
        )
        assertTrue(sameRevApprover is WorkflowResult.Denied)

        // 2. Assignee == Reviewer denied
        val selfReview = repo.createTaskValidated(
            creator = PresetUsers.HOKIM,
            assignee = PresetUsers.NAZORATCHI,
            reviewer = PresetUsers.NAZORATCHI,
            finalApprover = PresetUsers.HOKIM,
            title = "SoD test 2",
            description = "Desc",
            department = PresetUsers.NAZORATCHI.department,
            assigneeName = PresetUsers.NAZORATCHI.name,
            assigneeRole = "Nazoratchi",
            priority = "HIGH",
            deadlineMillis = System.currentTimeMillis() + 3_600_000L,
            objectName = "Obj",
            address = "Addr",
            normativAsos = "Asos",
            kpiPoints = 10,
            subtasks = emptyList()
        )
        assertTrue(selfReview is WorkflowResult.Denied)

        // 3. Blocked assignee denied
        val blockedAssignee = repo.createTaskValidated(
            creator = PresetUsers.HOKIM,
            assignee = PresetUsers.IJROCHI.copy(isBlocked = true),
            reviewer = PresetUsers.NAZORATCHI,
            finalApprover = PresetUsers.HOKIM,
            title = "Blocked assignee",
            description = "Desc",
            department = PresetUsers.IJROCHI.department,
            assigneeName = PresetUsers.IJROCHI.name,
            assigneeRole = "Ijrochi",
            priority = "MEDIUM",
            deadlineMillis = System.currentTimeMillis() + 3_600_000L,
            objectName = "Obj",
            address = "Addr",
            normativAsos = "Asos",
            kpiPoints = 10,
            subtasks = emptyList()
        )
        assertTrue(blockedAssignee is WorkflowResult.Denied)

        // 4. Out-of-scope BOLIM_RAHBARI target department denied
        val outOfScope = repo.createTaskValidated(
            creator = PresetUsers.BOLIM_RAHBARI,
            assignee = PresetUsers.IJROCHI,
            reviewer = PresetUsers.NAZORATCHI,
            finalApprover = PresetUsers.HOKIM,
            title = "Cross dept attempt",
            description = "Desc",
            department = "Ekologiya va atrof-muhit",
            assigneeName = PresetUsers.IJROCHI.name,
            assigneeRole = "Ijrochi",
            priority = "MEDIUM",
            deadlineMillis = System.currentTimeMillis() + 3_600_000L,
            objectName = "Obj",
            address = "Addr",
            normativAsos = "Asos",
            kpiPoints = 10,
            subtasks = emptyList()
        )
        assertTrue(outOfScope is WorkflowResult.Denied)

        // 5. Insufficient confidentiality clearance denied
        val lowClearance = repo.createTaskValidated(
            creator = PresetUsers.HOKIM,
            assignee = PresetUsers.IJROCHI, // maxConfidentialityLevel = 2
            reviewer = PresetUsers.NAZORATCHI,
            finalApprover = PresetUsers.HOKIM,
            title = "Top secret task",
            description = "Desc",
            department = PresetUsers.IJROCHI.department,
            assigneeName = PresetUsers.IJROCHI.name,
            assigneeRole = "Ijrochi",
            priority = "EMERGENCY",
            deadlineMillis = System.currentTimeMillis() + 3_600_000L,
            objectName = "Obj",
            address = "Addr",
            normativAsos = "Asos",
            kpiPoints = 20,
            subtasks = emptyList(),
            confidentialityLevel = "OTA_MAXFIY"
        )
        assertTrue(lowClearance is WorkflowResult.Denied)

        val deniedAudits = repo.getAllAuditLogsOnce().filter { it.result == "DENIED" }
        assertEquals(5, deniedAudits.size)
        assertEquals(0, db.taskDao().getTaskCountOnce())
    }

    @Test
    fun `two stage review workflow enforces verifyByReviewer and blocks reviewer self approval and super admin`() = runBlocking {
        val created = repo.createTaskValidated(
            creator = PresetUsers.HOKIM,
            assignee = PresetUsers.IJROCHI,
            reviewer = PresetUsers.NAZORATCHI,
            finalApprover = PresetUsers.HOKIM,
            title = "Ikki bosqichli tasdiq testi",
            description = "Desc",
            department = PresetUsers.IJROCHI.department,
            assigneeName = PresetUsers.IJROCHI.name,
            assigneeRole = "Ijrochi",
            priority = "HIGH",
            deadlineMillis = System.currentTimeMillis() + 86_400_000L,
            objectName = "Obj",
            address = "Zarafshon",
            normativAsos = "Asos",
            kpiPoints = 15,
            subtasks = emptyList()
        ) as WorkflowResult.Success
        val taskId = created.taskId

        repo.acceptTask(taskId, PresetUsers.IJROCHI.name, PresetUsers.IJROCHI.role.titleUz)
        repo.submitReport(taskId, "Hisobot yuborildi", PresetUsers.IJROCHI.name, 41.5725, 64.1985)

        // Hokim cannot finalize BEFORE Stage 1 Nazoratchi verification
        val prematureApprove = repo.reviewApprove(taskId, PresetUsers.HOKIM)
        assertTrue(prematureApprove is WorkflowResult.Denied)

        // Ijrochi cannot verify or approve own task
        val ijrochiVerify = repo.verifyByReviewer(taskId, PresetUsers.IJROCHI)
        assertTrue(ijrochiVerify is WorkflowResult.Denied)

        // Stage 1: Nazoratchi verifies SUBMITTED -> APPROVAL_PENDING
        val stage1 = repo.verifyByReviewer(taskId, PresetUsers.NAZORATCHI)
        assertTrue(stage1 is WorkflowResult.Success)
        val afterStage1 = repo.getTaskById(taskId)!!
        assertEquals(TaskStatus.APPROVAL_PENDING.name, afterStage1.status)
        assertTrue(afterStage1.isReviewerVerified)

        // Nazoratchi cannot act as final approver after reviewing
        val reviewerApprove = repo.reviewApprove(taskId, PresetUsers.NAZORATCHI)
        assertTrue(reviewerApprove is WorkflowResult.Denied)

        // SUPER_ADMIN cannot close business tasks
        val adminApprove = repo.reviewApprove(taskId, PresetUsers.SUPER_ADMIN)
        assertTrue(adminApprove is WorkflowResult.Denied)

        // Stage 2: Hokim closes task -> COMPLETED (100%)
        val finalApprove = repo.reviewApprove(taskId, PresetUsers.HOKIM)
        assertTrue(finalApprove is WorkflowResult.Success)
        val completed = repo.getTaskById(taskId)!!
        assertEquals(TaskStatus.COMPLETED.name, completed.status)
        assertEquals(100, completed.progressPercent)
    }

    @Test
    fun `extension request preserves original deadline until resolved`() = runBlocking {
        val originalDeadline = System.currentTimeMillis() + 86_400_000L
        val created = repo.createTaskValidated(
            creator = PresetUsers.HOKIM,
            assignee = PresetUsers.IJROCHI,
            reviewer = PresetUsers.NAZORATCHI,
            finalApprover = PresetUsers.HOKIM,
            title = "Muddat uzaytirish testi",
            description = "Desc",
            department = PresetUsers.IJROCHI.department,
            assigneeName = PresetUsers.IJROCHI.name,
            assigneeRole = "Ijrochi",
            priority = "MEDIUM",
            deadlineMillis = originalDeadline,
            objectName = "Obj",
            address = "Zarafshon",
            normativAsos = "Asos",
            kpiPoints = 10,
            subtasks = emptyList()
        ) as WorkflowResult.Success
        val taskId = created.taskId

        repo.acceptTask(taskId, PresetUsers.IJROCHI.name, PresetUsers.IJROCHI.role.titleUz)
        repo.requestExtension(
            taskId = taskId,
            extraHours = 24L,
            reason = "Laboratoriya xulosasi kutilmoqda",
            userName = PresetUsers.IJROCHI.name,
            userRole = PresetUsers.IJROCHI.role.titleUz
        )

        val pending = repo.getTaskById(taskId)!!
        assertEquals(TaskStatus.EXTENSION_REQUESTED.name, pending.status)
        assertEquals(originalDeadline, pending.deadline)
        assertEquals(24L, pending.pendingExtensionHours)

        // Rejecting extension restores IN_PROGRESS and keeps originalDeadline
        val rejected = repo.resolveExtension(taskId, approve = false, approver = PresetUsers.HOKIM)
        assertTrue(rejected is WorkflowResult.Success)
        val afterReject = repo.getTaskById(taskId)!!
        assertEquals(TaskStatus.IN_PROGRESS.name, afterReject.status)
        assertEquals(originalDeadline, afterReject.deadline)

        // Request again and approve -> deadline shifts by +48h
        repo.requestExtension(taskId, 48L, "Texnika kechikdi", PresetUsers.IJROCHI.name, PresetUsers.IJROCHI.role.titleUz)
        val approved = repo.resolveExtension(taskId, approve = true, approver = PresetUsers.HOKIM)
        assertTrue(approved is WorkflowResult.Success)
        val afterApprove = repo.getTaskById(taskId)!!
        assertEquals(TaskStatus.IN_PROGRESS.name, afterApprove.status)
        assertEquals(originalDeadline + 48L * 3600_000L, afterApprove.deadline)
    }

    @Test
    fun `multi org task blocks final approval until all participating organizations complete`() = runBlocking {
        val created = repo.createTaskValidated(
            creator = PresetUsers.HOKIM,
            assignee = PresetUsers.IJROCHI,
            reviewer = PresetUsers.NAZORATCHI,
            finalApprover = PresetUsers.HOKIM,
            title = "Ko'p tashkilotli magistrallar",
            description = "2 ta bo'lim hamkorligi",
            department = PresetUsers.IJROCHI.department,
            assigneeName = PresetUsers.IJROCHI.name,
            assigneeRole = "Ijrochi",
            priority = "HIGH",
            deadlineMillis = System.currentTimeMillis() + 86_400_000L,
            objectName = "Navoiy shoh ko'chasi",
            address = "Zarafshon",
            normativAsos = "Bayonnoma #48",
            kpiPoints = 20,
            subtasks = emptyList(),
            routingType = TaskRoutingType.KOP_TASHKILOTLI.name,
            participatingOrgsRaw = "Obodonlashtirish boshqarmasi|Karimov Jasur|50|false;;Uy-joy kommunal xizmat|Haydarov O.|50|false"
        ) as WorkflowResult.Success
        val taskId = created.taskId

        repo.acceptTask(taskId, PresetUsers.IJROCHI.name, PresetUsers.IJROCHI.role.titleUz)
        repo.submitReport(taskId, "Dastlabki dalil", PresetUsers.IJROCHI.name, 41.5725, 64.1985)
        repo.verifyByReviewer(taskId, PresetUsers.NAZORATCHI)

        // Toggle only first organization -> second is still incomplete
        repo.toggleMultiOrgPart(taskId, "Obodonlashtirish boshqarmasi", PresetUsers.IJROCHI)
        val blockedApprove = repo.reviewApprove(taskId, PresetUsers.HOKIM)
        assertTrue(blockedApprove is WorkflowResult.Denied)

        // Toggle second organization -> all parts completed -> Hokim can close
        repo.toggleMultiOrgPart(taskId, "Uy-joy kommunal xizmat", PresetUsers.HOKIM)
        val successApprove = repo.reviewApprove(taskId, PresetUsers.HOKIM)
        assertTrue(successApprove is WorkflowResult.Success)
        assertEquals(TaskStatus.COMPLETED.name, repo.getTaskById(taskId)!!.status)
    }

    @Test
    fun `abac task isolation restricts super admin and out of scope users while allowing multi org participants`() = runBlocking {
        val created = repo.createTaskValidated(
            creator = PresetUsers.HOKIM,
            assignee = PresetUsers.BOLIM_RAHBARI,
            reviewer = PresetUsers.NAZORATCHI,
            finalApprover = PresetUsers.HOKIM,
            title = "Maxfiy qurilish obyektlari",
            description = "Faqat ruxsatli shaxslar uchun",
            department = "Qurilish va arxitektura",
            assigneeName = PresetUsers.BOLIM_RAHBARI.name,
            assigneeRole = "Mas'ul",
            priority = "HIGH",
            deadlineMillis = System.currentTimeMillis() + 86_400_000L,
            objectName = "7-sektor",
            address = "Zarafshon",
            normativAsos = "Qaror",
            kpiPoints = 15,
            subtasks = emptyList(),
            confidentialityLevel = "XIZMAT_UCHUN",
            routingType = TaskRoutingType.KOP_TASHKILOTLI.name,
            participatingOrgsRaw = "Qurilish va arxitektura|To'rayev Sherzod|0|false;;Uy-joy kommunal xizmat|Haydarov O.|0|false"
        ) as WorkflowResult.Success
        val task = repo.getTaskById(created.taskId)!!

        // Hokim and Nazoratchi have full oversight access
        assertTrue(BoshqaruvRepository.canUserAccessTask(PresetUsers.HOKIM, task))
        assertTrue(BoshqaruvRepository.canUserAccessTask(PresetUsers.NAZORATCHI, task))

        // Central Admin (SUPER_ADMIN) is NOT a participant in this confidential business task -> denied
        assertFalse(BoshqaruvRepository.canUserAccessTask(PresetUsers.SUPER_ADMIN, task))

        // Unrelated Ijrochi from Ekologiya cannot access this task
        val ecoIjrochi = PresetUsers.IJROCHI.copy(
            id = "usr-eco",
            name = "Sobirov Dilshod",
            department = "Ekologiya va atrof-muhit"
        )
        assertFalse(BoshqaruvRepository.canUserAccessTask(ecoIjrochi, task))

        // Participant organization user (UJKX) CAN access this multi-org task
        val ujkxIjrochi = PresetUsers.IJROCHI.copy(
            id = "usr-ujkx",
            name = "Haydarov Olim",
            department = "Uy-joy kommunal xizmat"
        )
        assertTrue(BoshqaruvRepository.canUserAccessTask(ujkxIjrochi, task))
    }

    @Test
    fun `full 15 step acceptance scenario end to end lifecycle and audit completeness`() = runBlocking {
        // Steps 1-3: Hokim authenticates & dispatches validated task to Ijrochi with Nazoratchi reviewer
        val authHokim = PresetUsers.authenticate("hokim", "admin365", PresetUsers.ALL_PRESETS)
        assertTrue(authHokim is AuthResult.Success)
        val dispatch = repo.createTaskValidated(
            creator = PresetUsers.HOKIM,
            assignee = PresetUsers.IJROCHI,
            reviewer = PresetUsers.NAZORATCHI,
            finalApprover = PresetUsers.HOKIM,
            title = "15-bosqichli qabul qilish ssenariysi",
            description = "To'liq hayotiy tsikl tekshiruvi",
            department = PresetUsers.IJROCHI.department,
            assigneeName = PresetUsers.IJROCHI.name,
            assigneeRole = "Ijrochi",
            priority = "HIGH",
            deadlineMillis = System.currentTimeMillis() + 86_400_000L,
            objectName = "Zarafshon markazi",
            address = "Navoiy ko'chasi",
            normativAsos = "Qabul protokoli #1",
            kpiPoints = 25,
            subtasks = listOf(SubTask("s1", "Fotodalil tayyorlash", false)),
            idempotencyKey = "e2e-15-step"
        ) as WorkflowResult.Success
        val taskId = dispatch.taskId

        // Steps 4-6: Ijrochi asks clarification -> accepts task -> submits GPS report
        repo.requestClarification(
            taskId = taskId,
            question = "Foto hisobot necha rakursda bo'lishi kerak?",
            userName = PresetUsers.IJROCHI.name,
            userRole = PresetUsers.IJROCHI.role.titleUz
        )
        repo.acceptTask(taskId, PresetUsers.IJROCHI.name, PresetUsers.IJROCHI.role.titleUz)
        assertEquals(TaskStatus.IN_PROGRESS.name, repo.getTaskById(taskId)!!.status)

        repo.submitReport(taskId, "1-hisobot yuborildi", PresetUsers.IJROCHI.name, 41.5725, 64.1985)
        assertEquals(TaskStatus.SUBMITTED.name, repo.getTaskById(taskId)!!.status)

        // Steps 7-9: Nazoratchi sends for rework #1 -> cycleCount=1 -> Ijrochi re-submits
        repo.reviewRework(
            taskId = taskId,
            reworkReason = "Foto burchagi aniq emas, qayta yuklang",
            reviewerName = PresetUsers.NAZORATCHI.name,
            reviewerRole = PresetUsers.NAZORATCHI.role.titleUz
        )
        val reworked = repo.getTaskById(taskId)!!
        assertEquals(TaskStatus.REWORK.name, reworked.status)
        assertEquals(1, reworked.cycleCount)
        assertFalse(reworked.isReviewerVerified)

        repo.submitReport(taskId, "2-hisobot (to'liq GPS dalil)", PresetUsers.IJROCHI.name, 41.5725, 64.1985)

        // Steps 10-12: Nazoratchi verifies Stage 1 -> APPROVAL_PENDING; self-approve denied
        val stage1 = repo.verifyByReviewer(taskId, PresetUsers.NAZORATCHI)
        assertTrue(stage1 is WorkflowResult.Success)
        assertEquals(TaskStatus.APPROVAL_PENDING.name, repo.getTaskById(taskId)!!.status)
        assertTrue(repo.reviewApprove(taskId, PresetUsers.NAZORATCHI) is WorkflowResult.Denied)

        // Steps 13-15: Hokim final approves -> COMPLETED (100%), comments & append-only audit verified
        val finalApprove = repo.reviewApprove(taskId, PresetUsers.HOKIM)
        assertTrue(finalApprove is WorkflowResult.Success)
        val finalTask = repo.getTaskById(taskId)!!
        assertEquals(TaskStatus.COMPLETED.name, finalTask.status)
        assertEquals(100, finalTask.progressPercent)

        val comments = repo.getCommentsForTaskOnce(taskId)
        assertTrue(comments.any { it.commentType == "CLARIFICATION" })
        assertTrue(comments.any { it.commentType == "REJECTION_COMMENT" })
        assertTrue(comments.any { it.commentType == "APPROVAL_COMMENT" })

        val logs = repo.getAllAuditLogsOnce()
        assertTrue(logs.any { it.action == "CREATE_TASK" && it.result == "SUCCESS" })
        assertTrue(logs.any { it.action == "CLARIFICATION_REQUESTED" && it.result == "SUCCESS" })
        assertTrue(logs.any { it.action.startsWith("REWORK_TASK") && it.result == "SUCCESS" })
        assertTrue(logs.any { it.action == "REVIEW_VERIFIED" && it.result == "SUCCESS" })
        assertTrue(logs.any { it.action == "FINAL_APPROVE_TASK" && it.result == "DENIED" })
        assertTrue(logs.any { it.action == "FINAL_APPROVE_TASK" && it.result == "SUCCESS" })
        assertTrue(logs.all { it.correlationId.isNotBlank() })
    }
}
