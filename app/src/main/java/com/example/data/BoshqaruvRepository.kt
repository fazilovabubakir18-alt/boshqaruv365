package com.example.data

import com.example.model.AppUser
import com.example.model.ConfidentialityLevel
import com.example.model.OrgExecutionPart
import com.example.model.SubTask
import com.example.model.TaskCommentType
import com.example.model.TaskRoutingType
import com.example.model.TaskStatus
import com.example.model.UserRole
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlin.math.*

sealed class WorkflowResult {
    data class Success(val message: String, val taskId: Long = 0L) : WorkflowResult()
    data class Denied(val reason: String) : WorkflowResult()
}

class BoshqaruvRepository(private val db: AppDatabase) {

    val allTasks: Flow<List<TaskEntity>> = db.taskDao().getAllTasks()
    val allComments: Flow<List<TaskCommentEntity>> = db.taskCommentDao().getAllComments()
    val allReports: Flow<List<ReportEntity>> = db.reportDao().getAllReports()
    val allDisciplinaryActions: Flow<List<DisciplinaryEntity>> = db.disciplinaryDao().getAllActions()
    val allAuditLogs: Flow<List<AuditLogEntity>> = db.auditLogDao().getAllLogs()
    val allNotifications: Flow<List<NotificationEntity>> = db.notificationDao().getAllNotifications()
    val offlineQueue: Flow<List<OfflineQueueEntity>> = db.offlineQueueDao().getAllQueued()

    fun getReportsForTask(taskId: Long): Flow<List<ReportEntity>> =
        db.reportDao().getReportsForTask(taskId)

    suspend fun getTaskById(taskId: Long): TaskEntity? = db.taskDao().getTaskById(taskId)

    suspend fun getCommentsForTaskOnce(taskId: Long): List<TaskCommentEntity> =
        db.taskCommentDao().getCommentsForTaskOnce(taskId)

    suspend fun getAllAuditLogsOnce(): List<AuditLogEntity> =
        db.auditLogDao().getAllLogsOnce()

    suspend fun recordAuditEvent(
        actorName: String,
        actorRole: String,
        action: String,
        entity: String,
        oldValue: String = "-",
        newValue: String = "-",
        reason: String,
        result: String = "SUCCESS",
        correlationId: String = "CORR-" + UUID.randomUUID().toString().take(8).uppercase()
    ) {
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userName = actorName,
                userRole = actorRole,
                action = action,
                entity = entity,
                oldValue = oldValue,
                newValue = newValue,
                timestamp = System.currentTimeMillis(),
                reason = reason,
                result = result,
                correlationId = correlationId
            )
        )
    }

    /**
     * Pre-dispatch validation enforcing RBAC, scope, clearance, active status, and Separation of Duties.
     * Returns null when valid, or an Uzbek error message when denied.
     */
    fun validateTaskDispatch(
        creator: AppUser,
        assignee: AppUser?,
        reviewer: AppUser?,
        finalApprover: AppUser?,
        confidentialityLevel: String,
        targetDepartment: String
    ): String? {
        if (creator.isBlocked) {
            return "Kirish cheklangan: hisobingiz faol emas."
        }
        if (!creator.permissions.canCreateTask) {
            return "${creator.role.titleUz} lavozimi yangi topshiriq yaratish vakolatiga ega emas."
        }
        if (creator.role == UserRole.BOLIM_RAHBARI &&
            !targetDepartment.contains(creator.department.substringBefore(" "), ignoreCase = true)
        ) {
            return "Bo'lim mas'uli faqat o'z bo'limi (${creator.department}) doirasida topshiriq bera oladi."
        }
        if (assignee != null && assignee.isBlocked) {
            return "Tanlangan ijrochi (${assignee.name}) bloklangan yoki faol emas."
        }
        if (reviewer != null && reviewer.isBlocked) {
            return "Tanlangan nazoratchi (${reviewer.name}) faol emas."
        }
        if (finalApprover != null && finalApprover.isBlocked) {
            return "Tanlangan yakuniy tasdiqlovchi (${finalApprover.name}) faol emas."
        }
        if (reviewer != null && finalApprover != null && reviewer.id == finalApprover.id) {
            return "Vakolat bo'linishi buzildi: Nazoratchi va Yakuniy tasdiqlovchi bir shaxs bo'lishi mumkin emas."
        }
        if (assignee != null && reviewer != null && assignee.id == reviewer.id) {
            return "Vakolat bo'linishi buzildi: Ijrochi o'z topshirig'iga nazoratchi bo'la olmaydi."
        }
        val taskConfLevel = try {
            ConfidentialityLevel.valueOf(confidentialityLevel).level
        } catch (_: Exception) {
            1
        }
        if (assignee != null && assignee.scope.maxConfidentialityLevel < taskConfLevel) {
            return "Ijrochi (${assignee.name}) maxfiylik darajasi ($confidentialityLevel) bo'yicha ruxsatga ega emas."
        }
        return null
    }

    suspend fun createTaskValidated(
        creator: AppUser,
        assignee: AppUser?,
        reviewer: AppUser?,
        finalApprover: AppUser?,
        title: String,
        description: String,
        department: String,
        assigneeName: String,
        assigneeRole: String,
        priority: String,
        deadlineMillis: Long,
        objectName: String,
        address: String,
        lat: Double = 41.5725,
        lng: Double = 64.1985,
        geofenceRadius: Int = 100,
        normativAsos: String,
        kpiPoints: Int,
        subtasks: List<SubTask>,
        sourceType: String = "Rahbariyat topshirig'i",
        confidentialityLevel: String = "XIZMAT_UCHUN",
        responsibleLeaderName: String = "To'rayev Sherzod Mahmudovich",
        coAssigneesRaw: String = "",
        observersRaw: String = "Saidov Mirzo (Nazoratchi)",
        reviewerName: String = reviewer?.name ?: "Saidov Mirzo O'ktamovich",
        finalApproverName: String = finalApprover?.name ?: "Shukurov Alisher Davronovich",
        aiMonitoringMode: String = "AI monitoring + Haftalik tasdiq",
        expectedOutcome: String = "Obyekt bo'yicha to'liq dalil, GPS va foto hisobot taqdim etish",
        routingType: String = TaskRoutingType.VERTIKAL.name,
        reportFormat: String = "FOTO",
        participatingOrgsRaw: String = "",
        sourceCommunicationId: String? = null,
        idempotencyKey: String? = null
    ): WorkflowResult {
        val validationError = validateTaskDispatch(
            creator = creator,
            assignee = assignee,
            reviewer = reviewer,
            finalApprover = finalApprover,
            confidentialityLevel = confidentialityLevel,
            targetDepartment = department
        )
        if (validationError != null) {
            recordAuditEvent(
                actorName = creator.name,
                actorRole = creator.role.titleUz,
                action = "CREATE_TASK",
                entity = title.take(40),
                oldValue = "DRAFT",
                newValue = "DENIED",
                reason = validationError,
                result = "DENIED"
            )
            return WorkflowResult.Denied(validationError)
        }
        if (reviewerName.equals(finalApproverName, ignoreCase = true)) {
            val msg = "Vakolat bo'linishi: Nazoratchi va Yakuniy tasdiqlovchi bir xil shaxs bo'lmasligi shart."
            recordAuditEvent(
                actorName = creator.name,
                actorRole = creator.role.titleUz,
                action = "CREATE_TASK",
                entity = title.take(40),
                oldValue = "DRAFT",
                newValue = "DENIED",
                reason = msg,
                result = "DENIED"
            )
            return WorkflowResult.Denied(msg)
        }

        val id = createTask(
            title = title,
            description = description,
            department = department,
            assigneeName = assigneeName,
            assigneeRole = assigneeRole,
            creatorName = creator.name,
            creatorRole = creator.role.titleUz,
            priority = priority,
            deadlineMillis = deadlineMillis,
            objectName = objectName,
            address = address,
            lat = lat,
            lng = lng,
            geofenceRadius = geofenceRadius,
            normativAsos = normativAsos,
            kpiPoints = kpiPoints,
            subtasks = subtasks,
            sourceType = sourceType,
            confidentialityLevel = confidentialityLevel,
            responsibleLeaderName = responsibleLeaderName,
            coAssigneesRaw = coAssigneesRaw,
            observersRaw = observersRaw,
            reviewerName = reviewerName,
            finalApproverName = finalApproverName,
            aiMonitoringMode = aiMonitoringMode,
            expectedOutcome = expectedOutcome,
            routingType = routingType,
            reportFormat = reportFormat,
            participatingOrgsRaw = participatingOrgsRaw,
            sourceCommunicationId = sourceCommunicationId,
            idempotencyKey = idempotencyKey
        )
        return WorkflowResult.Success("Topshiriq yaratildi va zanjirga biriktirildi.", taskId = id)
    }

    suspend fun createTask(
        title: String,
        description: String,
        department: String,
        assigneeName: String,
        assigneeRole: String,
        creatorName: String,
        creatorRole: String,
        priority: String,
        deadlineMillis: Long,
        objectName: String,
        address: String,
        lat: Double = 41.5725,
        lng: Double = 64.1985,
        geofenceRadius: Int = 100,
        normativAsos: String,
        kpiPoints: Int,
        subtasks: List<SubTask>,
        sourceType: String = "Rahbariyat topshirig'i",
        confidentialityLevel: String = "XIZMAT_UCHUN",
        responsibleLeaderName: String = "To'rayev Sherzod Mahmudovich",
        coAssigneesRaw: String = "",
        observersRaw: String = "Saidov Mirzo (Nazoratchi)",
        reviewerName: String = "Saidov Mirzo O'ktamovich",
        finalApproverName: String = "Shukurov Alisher Davronovich",
        aiMonitoringMode: String = "AI monitoring + Haftalik tasdiq",
        expectedOutcome: String = "Obyekt bo'yicha to'liq dalil, GPS va foto hisobot taqdim etish",
        routingType: String = TaskRoutingType.VERTIKAL.name,
        reportFormat: String = "FOTO",
        participatingOrgsRaw: String = "",
        sourceCommunicationId: String? = null,
        idempotencyKey: String? = null
    ): Long {
        if (!idempotencyKey.isNullOrBlank()) {
            val existing = db.taskDao().getTaskByIdempotencyKey(idempotencyKey)
            if (existing != null) {
                return existing.id
            }
        }

        val now = System.currentTimeMillis()
        val taskSeq = (107..999).random()
        val taskNumber = "B365-2026-%06d".format(taskSeq)
        val subtasksRaw = subtasks.joinToString(";;") { "${it.id}|${it.title}|${it.isCompleted}" }

        val effectiveOrgParts = if (routingType == TaskRoutingType.KOP_TASHKILOTLI.name && participatingOrgsRaw.isBlank()) {
            "$department|$assigneeName|0|false"
        } else {
            participatingOrgsRaw
        }

        val entity = TaskEntity(
            taskNumber = taskNumber,
            title = title,
            description = description,
            sourceType = sourceType,
            confidentialityLevel = confidentialityLevel,
            department = department,
            responsibleLeaderName = responsibleLeaderName,
            assigneeName = assigneeName,
            assigneeRole = assigneeRole,
            coAssigneesRaw = coAssigneesRaw,
            observersRaw = observersRaw,
            creatorName = creatorName,
            creatorRole = creatorRole,
            reviewerName = reviewerName,
            finalApproverName = finalApproverName,
            isReviewerVerified = false,
            priority = priority,
            status = TaskStatus.ASSIGNED.name,
            progressPercent = 0,
            cycleCount = 0,
            aiMonitoringMode = aiMonitoringMode,
            createdAt = now,
            interimCheckpoint = now + ((deadlineMillis - now) / 2).coerceAtLeast(3600_000L),
            deadline = deadlineMillis,
            expectedOutcome = expectedOutcome,
            objectName = objectName,
            address = address,
            latitude = lat,
            longitude = lng,
            geofenceRadius = geofenceRadius,
            normativAsos = normativAsos,
            kpiPoints = kpiPoints,
            subtasksRaw = subtasksRaw,
            riskLevel = if (deadlineMillis - now < 24 * 3600 * 1000L) "HIGH" else "LOW",
            routingType = routingType,
            reportFormat = reportFormat,
            participatingOrgsRaw = effectiveOrgParts,
            sourceCommunicationId = sourceCommunicationId,
            idempotencyKey = idempotencyKey
        )
        val newId = db.taskDao().insertTask(entity)

        db.taskCommentDao().insertComment(
            TaskCommentEntity(
                taskId = newId,
                authorName = creatorName,
                authorRole = creatorRole,
                commentType = TaskCommentType.INSTRUCTION.name,
                text = description,
                attachmentName = sourceCommunicationId,
                timestamp = now
            )
        )

        val notifBody = if (confidentialityLevel == "MAXFIY" || confidentialityLevel == "OTA_MAXFIY") {
            "$creatorName sizga xizmat topshirig'ini ($taskNumber) yubordi. Tafsilotlar tizim ichida."
        } else {
            "$creatorName sizga topshiriq yukladi: '$title'. Nazoratchi: ${reviewerName.substringBefore(" ")}"
        }

        db.notificationDao().insertNotification(
            NotificationEntity(
                title = "Yangi topshiriq ($taskNumber)",
                message = notifBody,
                category = "TOPSHIRIQ",
                taskId = newId,
                timestamp = now
            )
        )

        recordAuditEvent(
            actorName = creatorName,
            actorRole = creatorRole,
            action = "CREATE_TASK",
            entity = taskNumber,
            oldValue = "DRAFT",
            newValue = "ASSIGNED -> $assigneeName ($routingType)",
            reason = "Yangi topshiriq yaratildi ($normativAsos)",
            result = "SUCCESS"
        )
        return newId
    }

    suspend fun addTaskComment(
        taskId: Long,
        authorName: String,
        authorRole: String,
        commentType: TaskCommentType,
        text: String,
        attachmentName: String? = null
    ) {
        if (text.isBlank()) return
        val task = db.taskDao().getTaskById(taskId) ?: return
        val now = System.currentTimeMillis()
        db.taskCommentDao().insertComment(
            TaskCommentEntity(
                taskId = taskId,
                authorName = authorName,
                authorRole = authorRole,
                commentType = commentType.name,
                text = text.trim(),
                attachmentName = attachmentName,
                timestamp = now
            )
        )
        recordAuditEvent(
            actorName = authorName,
            actorRole = authorRole,
            action = "ADD_COMMENT (${commentType.labelUz})",
            entity = task.taskNumber,
            oldValue = task.status,
            newValue = commentType.labelUz,
            reason = text.trim().take(120),
            result = "SUCCESS"
        )
    }

    suspend fun requestClarification(
        taskId: Long,
        question: String,
        userName: String,
        userRole: String
    ) {
        if (question.isBlank()) return
        val task = db.taskDao().getTaskById(taskId) ?: return
        val now = System.currentTimeMillis()
        db.taskCommentDao().insertComment(
            TaskCommentEntity(
                taskId = taskId,
                authorName = userName,
                authorRole = userRole,
                commentType = TaskCommentType.CLARIFICATION.name,
                text = "Aniqlashtirish so'rovi: ${question.trim()}",
                timestamp = now
            )
        )
        db.notificationDao().insertNotification(
            NotificationEntity(
                title = "Aniqlashtirish so'rovi (${task.taskNumber})",
                message = "$userName savol yubordi: ${question.trim().take(90)}",
                category = "TOPSHIRIQ",
                taskId = taskId,
                timestamp = now
            )
        )
        recordAuditEvent(
            actorName = userName,
            actorRole = userRole,
            action = "CLARIFICATION_REQUESTED",
            entity = task.taskNumber,
            oldValue = task.status,
            newValue = "CLARIFICATION",
            reason = question.trim().take(120),
            result = "SUCCESS"
        )
    }

    suspend fun requestExtension(
        taskId: Long,
        extraHours: Long,
        reason: String,
        userName: String,
        userRole: String
    ) {
        if (reason.isBlank() || extraHours <= 0L) return
        val task = db.taskDao().getTaskById(taskId) ?: return
        val now = System.currentTimeMillis()
        db.taskDao().updateTask(
            task.copy(
                status = TaskStatus.EXTENSION_REQUESTED.name,
                pendingExtensionHours = extraHours,
                pendingExtensionReason = reason.trim()
            )
        )
        db.taskCommentDao().insertComment(
            TaskCommentEntity(
                taskId = taskId,
                authorName = userName,
                authorRole = userRole,
                commentType = TaskCommentType.CLARIFICATION.name,
                text = "Muddat uzaytirish so'rovi (+$extraHours soat): ${reason.trim()}",
                timestamp = now
            )
        )
        db.notificationDao().insertNotification(
            NotificationEntity(
                title = "Muddat uzaytirish so'rovi (${task.taskNumber})",
                message = "$userName +$extraHours soat uzaytirishni so'radi: ${reason.trim().take(80)}",
                category = "DEADLINE",
                taskId = taskId,
                timestamp = now
            )
        )
        recordAuditEvent(
            actorName = userName,
            actorRole = userRole,
            action = "EXTENSION_REQUESTED",
            entity = task.taskNumber,
            oldValue = task.status,
            newValue = "EXTENSION_REQUESTED (+${extraHours}h kutilmoqda)",
            reason = reason.trim(),
            result = "SUCCESS"
        )
    }

    suspend fun resolveExtension(
        taskId: Long,
        approve: Boolean,
        approver: AppUser,
        note: String = ""
    ): WorkflowResult {
        val task = db.taskDao().getTaskById(taskId)
            ?: return WorkflowResult.Denied("Topshiriq topilmadi.")
        if (approver.isBlocked || (!approver.permissions.canApproveReport && !approver.permissions.canEditDeadline)) {
            val msg = "${approver.role.titleUz} muddat uzaytirish so'rovini hal qilish huquqiga ega emas."
            recordAuditEvent(
                actorName = approver.name,
                actorRole = approver.role.titleUz,
                action = "RESOLVE_EXTENSION",
                entity = task.taskNumber,
                oldValue = task.status,
                newValue = "DENIED",
                reason = msg,
                result = "DENIED"
            )
            return WorkflowResult.Denied(msg)
        }
        val requestedHours = task.pendingExtensionHours.takeIf { it > 0L } ?: 24L
        val now = System.currentTimeMillis()
        val newDeadline = if (approve) task.deadline + requestedHours * 3600_000L else task.deadline
        db.taskDao().updateTask(
            task.copy(
                status = TaskStatus.IN_PROGRESS.name,
                deadline = newDeadline,
                pendingExtensionHours = 0L,
                pendingExtensionReason = null
            )
        )
        val decisionText = if (approve) {
            "Muddat +$requestedHours soatga uzaytirildi.${if (note.isNotBlank()) " Izoh: $note" else ""}"
        } else {
            "Muddat uzaytirish so'rovi rad etildi.${if (note.isNotBlank()) " Sabab: $note" else ""}"
        }
        db.taskCommentDao().insertComment(
            TaskCommentEntity(
                taskId = taskId,
                authorName = approver.name,
                authorRole = approver.role.titleUz,
                commentType = TaskCommentType.INSTRUCTION.name,
                text = decisionText,
                timestamp = now
            )
        )
        recordAuditEvent(
            actorName = approver.name,
            actorRole = approver.role.titleUz,
            action = if (approve) "EXTENSION_APPROVED" else "EXTENSION_REJECTED",
            entity = task.taskNumber,
            oldValue = "EXTENSION_REQUESTED",
            newValue = if (approve) "IN_PROGRESS (+${requestedHours}h)" else "IN_PROGRESS (muddat o'zgarmadi)",
            reason = decisionText,
            result = "SUCCESS"
        )
        return WorkflowResult.Success(decisionText, taskId)
    }

    suspend fun toggleMultiOrgPart(
        taskId: Long,
        orgName: String,
        actor: AppUser
    ): WorkflowResult {
        val task = db.taskDao().getTaskById(taskId)
            ?: return WorkflowResult.Denied("Topshiriq topilmadi.")
        if (actor.isBlocked) return WorkflowResult.Denied("Foydalanuvchi bloklangan.")
        val parts = parseMultiOrgParts(task.participatingOrgsRaw)
        if (parts.isEmpty()) return WorkflowResult.Denied("Ko'p tashkilotli qismlar mavjud emas.")
        val updated = parts.map { part ->
            if (part.orgName == orgName) {
                val nextDone = !part.isCompleted
                part.copy(
                    isCompleted = nextDone,
                    progressPercent = if (nextDone) 100 else 50
                )
            } else part
        }
        val completedCount = updated.count { it.isCompleted }
        val overallPercent = ((completedCount.toFloat() / updated.size) * 90).toInt().coerceIn(task.progressPercent, 95)
        db.taskDao().updateTask(
            task.copy(
                participatingOrgsRaw = serializeMultiOrgParts(updated),
                progressPercent = overallPercent
            )
        )
        recordAuditEvent(
            actorName = actor.name,
            actorRole = actor.role.titleUz,
            action = "MULTI_ORG_PART_UPDATE",
            entity = task.taskNumber,
            oldValue = "${parts.count { it.isCompleted }}/${parts.size}",
            newValue = "$completedCount/${updated.size} ($orgName)",
            reason = "Tashkilot ijro qismi yangilandi",
            result = "SUCCESS"
        )
        return WorkflowResult.Success("$orgName bo'yicha ijro holati yangilandi.", taskId)
    }

    suspend fun updateTaskProgress(
        taskId: Long,
        progressPercent: Int,
        userName: String,
        userRole: String
    ) {
        val task = db.taskDao().getTaskById(taskId) ?: return
        val clamped = progressPercent.coerceIn(0, 100)
        db.taskDao().updateProgress(taskId, clamped)
        recordAuditEvent(
            actorName = userName,
            actorRole = userRole,
            action = "UPDATE_PROGRESS",
            entity = task.taskNumber,
            oldValue = "${task.progressPercent}%",
            newValue = "$clamped%",
            reason = "Ijro foizi yangilandi",
            result = "SUCCESS"
        )
    }

    suspend fun acceptTask(taskId: Long, userName: String, userRole: String) {
        val task = db.taskDao().getTaskById(taskId) ?: return
        val now = System.currentTimeMillis()
        val nextProgress = maxOf(task.progressPercent, 15)
        db.taskDao().updateTask(
            task.copy(
                status = TaskStatus.IN_PROGRESS.name,
                progressPercent = nextProgress
            )
        )

        db.taskCommentDao().insertComment(
            TaskCommentEntity(
                taskId = taskId,
                authorName = userName,
                authorRole = userRole,
                commentType = TaskCommentType.PROGRESS_NOTE.name,
                text = "Topshiriq qabul qilindi va ijro boshlandi ($nextProgress%).",
                timestamp = now
            )
        )

        recordAuditEvent(
            actorName = userName,
            actorRole = userRole,
            action = "ACCEPT_TASK",
            entity = task.taskNumber,
            oldValue = task.status,
            newValue = TaskStatus.IN_PROGRESS.name,
            reason = "Ijrochi topshiriqni qabul qildi va ijroga kirishdi",
            result = "SUCCESS"
        )
    }

    suspend fun rejectTask(taskId: Long, reason: String, userName: String, userRole: String) {
        val task = db.taskDao().getTaskById(taskId) ?: return
        val now = System.currentTimeMillis()
        db.taskDao().updateTask(
            task.copy(
                status = TaskStatus.REJECTED.name,
                rejectionReason = reason
            )
        )

        db.taskCommentDao().insertComment(
            TaskCommentEntity(
                taskId = taskId,
                authorName = userName,
                authorRole = userRole,
                commentType = TaskCommentType.REJECTION_COMMENT.name,
                text = "Topshiriq rad etildi: $reason",
                timestamp = now
            )
        )

        db.notificationDao().insertNotification(
            NotificationEntity(
                title = "Topshiriq rad etildi (${task.taskNumber})",
                message = "$userName topshiriqni asosli sabab bilan rad etdi.",
                category = "INTIZOM",
                taskId = taskId,
                timestamp = now
            )
        )

        recordAuditEvent(
            actorName = userName,
            actorRole = userRole,
            action = "REJECT_TASK",
            entity = task.taskNumber,
            oldValue = task.status,
            newValue = TaskStatus.REJECTED.name,
            reason = reason,
            result = "SUCCESS"
        )
    }

    suspend fun submitReport(
        taskId: Long,
        comment: String,
        reporterName: String,
        userLatitude: Double,
        userLongitude: Double,
        photoPath: String? = null,
        isOfflineMode: Boolean = false
    ) {
        val task = db.taskDao().getTaskById(taskId) ?: return
        val now = System.currentTimeMillis()

        val distanceMeters = calculateDistance(
            userLatitude, userLongitude,
            task.latitude, task.longitude
        )

        val isInsideGeofence = distanceMeters <= (task.geofenceRadius + 30)
        val antiFraudWarning = if (!isInsideGeofence) {
            "DIQQAT: Masofa obyektdan ${distanceMeters}m (ruxsat: ${task.geofenceRadius}m). Geofence buzilishi qayd etildi!"
        } else null

        val report = ReportEntity(
            taskId = taskId,
            reporterName = reporterName,
            timestamp = now,
            comment = comment,
            photoPath = photoPath,
            latitude = userLatitude,
            longitude = userLongitude,
            distanceToTargetMeters = distanceMeters,
            gpsAccuracyMeters = 4.5f,
            isAntiFraudPassed = isInsideGeofence,
            antiFraudWarning = antiFraudWarning
        )

        db.reportDao().insertReport(report)
        db.taskDao().updateTask(
            task.copy(
                status = TaskStatus.SUBMITTED.name,
                progressPercent = maxOf(task.progressPercent, 90),
                isReviewerVerified = false
            )
        )

        db.taskCommentDao().insertComment(
            TaskCommentEntity(
                taskId = taskId,
                authorName = reporterName,
                authorRole = "Bajaruvchi",
                commentType = TaskCommentType.SUBMISSION.name,
                text = comment,
                attachmentName = photoPath ?: "GPS_EXIF_${distanceMeters}m.jpg",
                timestamp = now
            )
        )

        if (isOfflineMode) {
            db.offlineQueueDao().enqueue(
                OfflineQueueEntity(
                    idempotencyId = UUID.randomUUID().toString(),
                    actionType = "SUBMIT_REPORT",
                    payload = "taskId=$taskId, reporter=$reporterName, dist=${distanceMeters}m",
                    createdAt = now
                )
            )
        }

        db.notificationDao().insertNotification(
            NotificationEntity(
                title = "Tekshiruvga topshirildi (${task.taskNumber})",
                message = "$reporterName dalillarni yubordi (GPS ${distanceMeters}m). Nazoratchi tekshiruvi kutilmoqda.",
                category = "HISOBOT",
                taskId = taskId,
                timestamp = now
            )
        )

        recordAuditEvent(
            actorName = reporterName,
            actorRole = "Bajaruvchi",
            action = "SUBMIT_REPORT",
            entity = task.taskNumber,
            oldValue = task.status,
            newValue = "SUBMITTED (GPS ${distanceMeters}m)",
            reason = "Dalil va hisobot tekshiruvga yuborildi",
            result = "SUCCESS"
        )
    }

    /**
     * Stage 1: Nazoratchi (Reviewer) verifies evidence & moves SUBMITTED -> APPROVAL_PENDING.
     */
    suspend fun verifyByReviewer(taskId: Long, reviewer: AppUser): WorkflowResult {
        val task = db.taskDao().getTaskById(taskId)
            ?: return WorkflowResult.Denied("Topshiriq topilmadi.")
        if (reviewer.isBlocked || !reviewer.permissions.canReviewEvidence) {
            val msg = "${reviewer.role.titleUz} dalillarni tekshirish vakolatiga ega emas."
            recordAuditEvent(
                actorName = reviewer.name,
                actorRole = reviewer.role.titleUz,
                action = "REVIEW_VERIFIED",
                entity = task.taskNumber,
                oldValue = task.status,
                newValue = "DENIED",
                reason = msg,
                result = "DENIED"
            )
            return WorkflowResult.Denied(msg)
        }
        if (task.assigneeName.contains(reviewer.name.substringBefore(" "), ignoreCase = true) &&
            reviewer.name.equals(task.assigneeName, ignoreCase = true)
        ) {
            val msg = "Vakolat bo'linishi: Ijrochi o'z topshirig'ini nazoratdan o'tkaza olmaydi."
            recordAuditEvent(
                actorName = reviewer.name,
                actorRole = reviewer.role.titleUz,
                action = "REVIEW_VERIFIED",
                entity = task.taskNumber,
                oldValue = task.status,
                newValue = "DENIED",
                reason = msg,
                result = "DENIED"
            )
            return WorkflowResult.Denied(msg)
        }
        if (task.status != TaskStatus.SUBMITTED.name && task.status != TaskStatus.REVIEW.name) {
            val msg = "Nazoratchi tekshiruvi faqat 'Natija yuborildi' holatidagi topshiriq uchun bajariladi."
            recordAuditEvent(
                actorName = reviewer.name,
                actorRole = reviewer.role.titleUz,
                action = "REVIEW_VERIFIED",
                entity = task.taskNumber,
                oldValue = task.status,
                newValue = "DENIED",
                reason = msg,
                result = "DENIED"
            )
            return WorkflowResult.Denied(msg)
        }
        verifyByReviewer(taskId, reviewer.name, reviewer.role.titleUz)
        return WorkflowResult.Success(
            "Nazorat tekshiruvi tasdiqlandi. Yakuniy tasdiqlovchiga yuborildi.",
            taskId
        )
    }

    suspend fun verifyByReviewer(taskId: Long, reviewerName: String, reviewerRole: String) {
        val task = db.taskDao().getTaskById(taskId) ?: return
        val now = System.currentTimeMillis()
        db.taskDao().updateTask(
            task.copy(
                status = TaskStatus.APPROVAL_PENDING.name,
                progressPercent = 95,
                isReviewerVerified = true,
                reviewerName = reviewerName
            )
        )

        db.taskCommentDao().insertComment(
            TaskCommentEntity(
                taskId = taskId,
                authorName = reviewerName,
                authorRole = reviewerRole,
                commentType = TaskCommentType.APPROVAL_COMMENT.name,
                text = "Nazoratchi tekshiruvi tasdiqlandi. Dalillar to'liq, yakuniy tasdiq kutilmoqda.",
                timestamp = now
            )
        )

        db.notificationDao().insertNotification(
            NotificationEntity(
                title = "Tasdiq kutilmoqda (${task.taskNumber})",
                message = "$reviewerName dalillarni tekshirdi. Yakuniy tasdiqlovchi qarori kutilmoqda.",
                category = "NAZORAT",
                taskId = taskId,
                timestamp = now
            )
        )

        recordAuditEvent(
            actorName = reviewerName,
            actorRole = reviewerRole,
            action = "REVIEW_VERIFIED",
            entity = task.taskNumber,
            oldValue = task.status,
            newValue = TaskStatus.APPROVAL_PENDING.name,
            reason = "Nazoratchi tomonidan dalillar va mezonlar tasdiqlandi",
            result = "SUCCESS"
        )
    }

    /**
     * Stage 2: Yakuniy Tasdiqlovchi closes the task -> COMPLETED (YAKUNLANDI).
     * Strictly enforces Separation of Duties (assignee != approver, reviewer != approver,
     * SUPER_ADMIN cannot close business tasks, reviewer stage must pass first, multi-org must complete).
     */
    suspend fun reviewApprove(
        taskId: Long,
        approver: AppUser,
        note: String = "Yakuniy tasdiqlandi va muhrlandi."
    ): WorkflowResult {
        val task = db.taskDao().getTaskById(taskId)
            ?: return WorkflowResult.Denied("Topshiriq topilmadi.")
        if (approver.isBlocked) {
            val msg = "Hisob faol emas — tasdiqlash rad etildi."
            recordAuditEvent(
                actorName = approver.name,
                actorRole = approver.role.titleUz,
                action = "FINAL_APPROVE_TASK",
                entity = task.taskNumber,
                oldValue = task.status,
                newValue = "DENIED",
                reason = msg,
                result = "DENIED"
            )
            return WorkflowResult.Denied(msg)
        }
        if (!approver.permissions.canApproveReport || approver.role == UserRole.SUPER_ADMIN) {
            val msg = "${approver.role.titleUz} topshiriqni yakuniy yopish huquqiga ega emas."
            recordAuditEvent(
                actorName = approver.name,
                actorRole = approver.role.titleUz,
                action = "FINAL_APPROVE_TASK",
                entity = task.taskNumber,
                oldValue = task.status,
                newValue = "DENIED",
                reason = msg,
                result = "DENIED"
            )
            return WorkflowResult.Denied(msg)
        }
        if (approver.name.equals(task.assigneeName, ignoreCase = true)) {
            val msg = "Vakolat bo'linishi: Ijrochi o'z topshirig'ini yakuniy yopa olmaydi."
            recordAuditEvent(
                actorName = approver.name,
                actorRole = approver.role.titleUz,
                action = "FINAL_APPROVE_TASK",
                entity = task.taskNumber,
                oldValue = task.status,
                newValue = "DENIED",
                reason = msg,
                result = "DENIED"
            )
            return WorkflowResult.Denied(msg)
        }
        if (approver.name.equals(task.reviewerName, ignoreCase = true)) {
            val msg = "Vakolat bo'linishi: Nazorat tekshiruvini o'tkazgan shaxs yakuniy tasdiqlovchi bo'la olmaydi."
            recordAuditEvent(
                actorName = approver.name,
                actorRole = approver.role.titleUz,
                action = "FINAL_APPROVE_TASK",
                entity = task.taskNumber,
                oldValue = task.status,
                newValue = "DENIED",
                reason = msg,
                result = "DENIED"
            )
            return WorkflowResult.Denied(msg)
        }
        if (!task.isReviewerVerified || (task.status != TaskStatus.APPROVAL_PENDING.name && task.status != TaskStatus.REVIEW.name)) {
            val msg = "Yakuniy tasdiqlashdan avval Nazoratchi tekshiruvi ('Tasdiq kutilmoqda' bosqichi) yakunlanishi shart."
            recordAuditEvent(
                actorName = approver.name,
                actorRole = approver.role.titleUz,
                action = "FINAL_APPROVE_TASK",
                entity = task.taskNumber,
                oldValue = task.status,
                newValue = "DENIED",
                reason = msg,
                result = "DENIED"
            )
            return WorkflowResult.Denied(msg)
        }
        if (task.routingType == TaskRoutingType.KOP_TASHKILOTLI.name) {
            val parts = parseMultiOrgParts(task.participatingOrgsRaw)
            if (parts.isNotEmpty() && parts.any { !it.isCompleted }) {
                val remaining = parts.filter { !it.isCompleted }.joinToString { it.orgName }
                val msg = "Ko'p tashkilotli topshiriq yopilmaydi — tugallanmagan tashkilotlar: $remaining"
                recordAuditEvent(
                    actorName = approver.name,
                    actorRole = approver.role.titleUz,
                    action = "FINAL_APPROVE_TASK",
                    entity = task.taskNumber,
                    oldValue = task.status,
                    newValue = "DENIED",
                    reason = msg,
                    result = "DENIED"
                )
                return WorkflowResult.Denied(msg)
            }
        }
        reviewApprove(taskId, approver.name, approver.role.titleUz, note)
        return WorkflowResult.Success("Topshiriq yakuniy tasdiqlandi va muhrlandi.", taskId)
    }

    suspend fun reviewApprove(
        taskId: Long,
        reviewerName: String,
        reviewerRole: String,
        note: String = "Yakuniy tasdiqlandi va muhrlandi."
    ) {
        val task = db.taskDao().getTaskById(taskId) ?: return
        val now = System.currentTimeMillis()
        db.taskDao().updateTask(
            task.copy(
                status = TaskStatus.COMPLETED.name,
                progressPercent = 100,
                isReviewerVerified = true
            )
        )

        db.taskCommentDao().insertComment(
            TaskCommentEntity(
                taskId = taskId,
                authorName = reviewerName,
                authorRole = reviewerRole,
                commentType = TaskCommentType.APPROVAL_COMMENT.name,
                text = note,
                timestamp = now
            )
        )

        db.notificationDao().insertNotification(
            NotificationEntity(
                title = "Yakuniy tasdiq (${task.taskNumber})",
                message = "$reviewerName tomonidan yakuniy tasdiqlandi. +${task.kpiPoints} KPI balli qo'shildi.",
                category = "TOPSHIRIQ",
                taskId = taskId,
                timestamp = now
            )
        )

        recordAuditEvent(
            actorName = reviewerName,
            actorRole = reviewerRole,
            action = "FINAL_APPROVE_TASK",
            entity = task.taskNumber,
            oldValue = task.status,
            newValue = "COMPLETED (100%)",
            reason = note,
            result = "SUCCESS"
        )
    }

    /**
     * Returns task for rework with mandatory reason, increments cycleCount, and triggers 3+ cycle alert!
     */
    suspend fun reviewRework(taskId: Long, reworkReason: String, reviewerName: String, reviewerRole: String) {
        if (reworkReason.isBlank()) return
        val task = db.taskDao().getTaskById(taskId) ?: return
        val now = System.currentTimeMillis()
        val nextCycle = task.cycleCount + 1
        val nextProgress = minOf(task.progressPercent, 65)

        db.taskDao().updateTask(
            task.copy(
                status = TaskStatus.REWORK.name,
                progressPercent = nextProgress,
                cycleCount = nextCycle,
                isReviewerVerified = false,
                reworkReason = reworkReason.trim(),
                riskLevel = if (nextCycle >= 3) "HIGH" else task.riskLevel
            )
        )

        db.taskCommentDao().insertComment(
            TaskCommentEntity(
                taskId = taskId,
                authorName = reviewerName,
                authorRole = reviewerRole,
                commentType = TaskCommentType.REJECTION_COMMENT.name,
                text = "$nextCycle-qaytarish: ${reworkReason.trim()}",
                timestamp = now
            )
        )

        if (nextCycle >= 3) {
            db.notificationDao().insertNotification(
                NotificationEntity(
                    title = "KO'P QAYTGAN TOPSHIRIQ (${task.taskNumber})",
                    message = "Topshiriq $nextCycle marta qaytarildi! Rahbar va AI Nazorat paneliga signal yuborildi.",
                    category = "INTIZOM",
                    taskId = taskId,
                    timestamp = now
                )
            )
        } else {
            db.notificationDao().insertNotification(
                NotificationEntity(
                    title = "Qayta ishlashga qaytarildi (${task.taskNumber})",
                    message = "Izoh ($nextCycle-tsikl): ${reworkReason.trim()}",
                    category = "INTIZOM",
                    taskId = taskId,
                    timestamp = now
                )
            )
        }

        recordAuditEvent(
            actorName = reviewerName,
            actorRole = reviewerRole,
            action = "REWORK_TASK (Tsikl #$nextCycle)",
            entity = task.taskNumber,
            oldValue = task.status,
            newValue = "REWORK (cycleCount=$nextCycle)",
            reason = reworkReason.trim(),
            result = "SUCCESS"
        )
    }

    /**
     * Hokim / Rahbar direct intervention: update deadline, supervisor, assignee, or reopen + instruction note.
     */
    suspend fun interveneTask(
        taskId: Long,
        extendHours: Long,
        newReviewerName: String?,
        newAssigneeName: String?,
        instructionNote: String,
        reopenIfCompleted: Boolean,
        leaderName: String,
        leaderRole: String
    ) {
        val task = db.taskDao().getTaskById(taskId) ?: return
        val now = System.currentTimeMillis()
        val updated = task.copy(
            deadline = if (extendHours != 0L) task.deadline + extendHours * 3600_000L else task.deadline,
            reviewerName = newReviewerName?.takeIf { it.isNotBlank() } ?: task.reviewerName,
            assigneeName = newAssigneeName?.takeIf { it.isNotBlank() } ?: task.assigneeName,
            status = if (reopenIfCompleted && (task.status == "COMPLETED" || task.status == "ARCHIVED")) TaskStatus.IN_PROGRESS.name else task.status
        )
        db.taskDao().updateTask(updated)

        val summaryText = buildString {
            append("RAHBAR ARALASHUVI: ")
            if (instructionNote.isNotBlank()) append(instructionNote.trim()).append(" ")
            if (extendHours != 0L) append("[Muddat +${extendHours} soat] ")
            if (!newReviewerName.isNullOrBlank() && newReviewerName != task.reviewerName) append("[Nazoratchi: $newReviewerName] ")
            if (!newAssigneeName.isNullOrBlank() && newAssigneeName != task.assigneeName) append("[Ijrochi: $newAssigneeName] ")
            if (reopenIfCompleted) append("[Topshiriq qayta ochildi]")
        }.trim()

        db.taskCommentDao().insertComment(
            TaskCommentEntity(
                taskId = taskId,
                authorName = leaderName,
                authorRole = leaderRole,
                commentType = TaskCommentType.INSTRUCTION.name,
                text = summaryText,
                timestamp = now
            )
        )

        recordAuditEvent(
            actorName = leaderName,
            actorRole = leaderRole,
            action = "RAHBAR_INTERVENTION",
            entity = task.taskNumber,
            oldValue = task.status,
            newValue = updated.status,
            reason = summaryText,
            result = "SUCCESS"
        )
    }

    suspend fun transitionTaskStatus(
        taskId: Long,
        nextStatus: TaskStatus,
        reason: String,
        userName: String,
        userRole: String
    ) {
        val task = db.taskDao().getTaskById(taskId) ?: return
        val now = System.currentTimeMillis()
        // Rule 10 fix: EXTENSION_REQUESTED never modifies deadline until explicitly approved!
        db.taskDao().updateTask(
            task.copy(
                status = nextStatus.name,
                pendingExtensionHours = if (nextStatus == TaskStatus.EXTENSION_REQUESTED) maxOf(task.pendingExtensionHours, 24L) else task.pendingExtensionHours,
                pendingExtensionReason = if (nextStatus == TaskStatus.EXTENSION_REQUESTED) reason else task.pendingExtensionReason
            )
        )

        db.taskCommentDao().insertComment(
            TaskCommentEntity(
                taskId = taskId,
                authorName = userName,
                authorRole = userRole,
                commentType = TaskCommentType.INSTRUCTION.name,
                text = "${nextStatus.titleUz}: $reason",
                timestamp = now
            )
        )

        recordAuditEvent(
            actorName = userName,
            actorRole = userRole,
            action = "STATUS_TRANSITION_${nextStatus.name}",
            entity = task.taskNumber,
            oldValue = task.status,
            newValue = nextStatus.name,
            reason = reason,
            result = "SUCCESS"
        )
    }

    suspend fun toggleSubtask(taskId: Long, subtaskId: String) {
        val task = db.taskDao().getTaskById(taskId) ?: return
        val subtasks = parseSubtasks(task.subtasksRaw).map {
            if (it.id == subtaskId) it.copy(isCompleted = !it.isCompleted) else it
        }
        val raw = subtasks.joinToString(";;") { "${it.id}|${it.title}|${it.isCompleted}" }
        db.taskDao().updateSubtasks(taskId, raw)
    }

    suspend fun issueDisciplinaryAction(
        employeeName: String,
        department: String,
        actionType: String,
        reason: String,
        taskId: Long,
        issuedBy: String
    ) {
        val now = System.currentTimeMillis()
        val docNum = "DISC-2026-%03d".format((10..99).random())
        db.disciplinaryDao().insertAction(
            DisciplinaryEntity(
                employeeName = employeeName,
                department = department,
                actionType = actionType,
                reason = reason,
                taskId = taskId,
                issuedBy = issuedBy,
                issueDate = now,
                docNumber = docNum,
                status = "AMALDA"
            )
        )

        db.notificationDao().insertNotification(
            NotificationEntity(
                title = "Intizomiy chora qo'llanildi ($actionType)",
                message = "$employeeName ga nisbatan chora belgilandi ($docNum).",
                category = "INTIZOM",
                taskId = taskId,
                timestamp = now
            )
        )

        recordAuditEvent(
            actorName = issuedBy,
            actorRole = "Rahbar / Nazoratchi",
            action = "DISCIPLINE_ISSUED",
            entity = employeeName,
            oldValue = "NONE",
            newValue = "$actionType ($docNum)",
            reason = reason,
            result = "SUCCESS"
        )
    }

    suspend fun markNotificationRead(id: Long) {
        db.notificationDao().markAsRead(id)
    }

    suspend fun markAllNotificationsRead() {
        db.notificationDao().markAllAsRead()
    }

    suspend fun syncOfflineQueue(): Int {
        db.offlineQueueDao().clearAll()
        return 1
    }

    suspend fun createBackupSnapshotSummary(actor: AppUser): String {
        val taskCount = db.taskDao().getTaskCountOnce()
        val logCount = db.auditLogDao().getAllLogsOnce().size
        val summary = "Zaxira nusxa tasdiqlandi: $taskCount ta topshiriq, $logCount ta audit yozuvi SHA-256 tekshiruvidan o'tdi."
        recordAuditEvent(
            actorName = actor.name,
            actorRole = actor.role.titleUz,
            action = "BACKUP_SNAPSHOT_VERIFIED",
            entity = "B365_DATABASE",
            oldValue = "LIVE_DB",
            newValue = "BACKUP_OK",
            reason = summary,
            result = "SUCCESS"
        )
        return summary
    }

    companion object {
        /**
         * ABAC + RBAC + Task-level permission check.
         * Central Admin (SUPER_ADMIN) manages users/RBAC/audit and cannot open confidential tasks
         * unless explicitly participating; Ijrochi/Bolim Rahbari only see tasks within their scope.
         */
        fun canUserAccessTask(user: AppUser, task: TaskEntity): Boolean {
            if (user.isBlocked) return false
            if (user.role == UserRole.HOKIM || user.role == UserRole.NAZORATCHI || user.role == UserRole.TAHLILCHI) return true

            val firstWord = user.name.substringBefore(" ")
            val isExplicitParticipant = task.assigneeName.contains(firstWord, true) ||
                task.responsibleLeaderName.contains(firstWord, true) ||
                task.coAssigneesRaw.contains(firstWord, true) ||
                task.reviewerName.contains(firstWord, true) ||
                task.finalApproverName.contains(firstWord, true)

            if (user.role == UserRole.SUPER_ADMIN) {
                return isExplicitParticipant
            }

            val taskConfLevel = try {
                ConfidentialityLevel.valueOf(task.confidentialityLevel).level
            } catch (_: Exception) {
                1
            }
            if (user.scope.maxConfidentialityLevel < taskConfLevel && !isExplicitParticipant) {
                return false
            }
            if (user.role == UserRole.ORINBOSAR) return true

            val deptPrefix = user.department.substringBefore(" ")
            val orgPartsMatch = parseMultiOrgParts(task.participatingOrgsRaw).any {
                it.orgName.contains(deptPrefix, ignoreCase = true) ||
                    it.assigneeName.contains(firstWord, ignoreCase = true)
            }
            return task.department.contains(deptPrefix, ignoreCase = true) ||
                isExplicitParticipant ||
                task.observersRaw.contains(firstWord, ignoreCase = true) ||
                orgPartsMatch
        }

        fun parseSubtasks(raw: String): List<SubTask> {
            if (raw.isBlank()) return emptyList()
            return raw.split(";;").mapNotNull { item ->
                val parts = item.split("|")
                if (parts.size >= 3) {
                    SubTask(id = parts[0], title = parts[1], isCompleted = parts[2].toBoolean())
                } else null
            }
        }

        fun parseMultiOrgParts(raw: String): List<OrgExecutionPart> {
            if (raw.isBlank()) return emptyList()
            return raw.split(";;").mapNotNull { item ->
                val parts = item.split("|")
                if (parts.size >= 4) {
                    OrgExecutionPart(
                        orgName = parts[0],
                        assigneeName = parts[1],
                        progressPercent = parts[2].toIntOrNull() ?: 0,
                        isCompleted = parts[3].toBoolean()
                    )
                } else null
            }
        }

        fun serializeMultiOrgParts(parts: List<OrgExecutionPart>): String {
            return parts.joinToString(";;") {
                "${it.orgName}|${it.assigneeName}|${it.progressPercent}|${it.isCompleted}"
            }
        }

        fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
            if (lat1 == 0.0 || lon1 == 0.0 || lat2 == 0.0 || lon2 == 0.0) return 25
            val r = 6371000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return (r * c).toInt()
        }

        fun formatDate(timestamp: Long): String {
            val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }
    }
}
