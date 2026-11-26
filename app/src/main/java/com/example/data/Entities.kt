package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskNumber: String,
    val title: String,
    val description: String,
    val sourceType: String = "Hokimlik topshirig'i",
    val confidentialityLevel: String = "XIZMAT_UCHUN",
    val department: String,
    val responsibleLeaderName: String = "To'rayev Sherzod Mahmudovich",
    val assigneeName: String,
    val assigneeRole: String,
    val coAssigneesRaw: String = "",
    val observersRaw: String = "",
    val creatorName: String,
    val creatorRole: String,
    val reviewerName: String = "Saidov Mirzo O'ktamovich",
    val finalApproverName: String = "Shukurov Alisher Davronovich",
    val isReviewerVerified: Boolean = false,
    val priority: String,
    val status: String,
    val progressPercent: Int = 0,
    val cycleCount: Int = 0,
    val aiMonitoringMode: String = "AI monitoring + Haftalik tasdiq",
    val createdAt: Long,
    val interimCheckpoint: Long = 0L,
    val deadline: Long,
    val expectedOutcome: String = "Obyekt 100% talabga muvofiq topshirilishi va foto/GPS dalil yuklanishi",
    val objectName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val geofenceRadius: Int = 100,
    val normativAsos: String = "Zarafshon shahar hokimi farmoyishi",
    val kpiPoints: Int = 10,
    val subtasksRaw: String = "",
    val riskLevel: String = "LOW",
    val rejectionReason: String? = null,
    val reworkReason: String? = null,
    val routingType: String = "VERTIKAL",
    val reportFormat: String = "FOTO",
    val participatingOrgsRaw: String = "",
    val sourceCommunicationId: String? = null,
    val pendingExtensionHours: Long = 0L,
    val pendingExtensionReason: String? = null,
    val idempotencyKey: String? = null
)

@Entity(tableName = "task_comments")
data class TaskCommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val authorName: String,
    val authorRole: String,
    val commentType: String, // TaskCommentType.name
    val text: String,
    val attachmentName: String? = null,
    val timestamp: Long
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val reporterName: String,
    val timestamp: Long,
    val comment: String,
    val photoPath: String? = null,
    val videoPath: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distanceToTargetMeters: Int = 0,
    val gpsAccuracyMeters: Float = 5.0f,
    val isAntiFraudPassed: Boolean = true,
    val antiFraudWarning: String? = null
)

@Entity(tableName = "disciplinary_actions")
data class DisciplinaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeName: String,
    val department: String,
    val actionType: String,
    val reason: String,
    val taskId: Long,
    val issuedBy: String,
    val issueDate: Long,
    val docNumber: String,
    val status: String = "AMALDA"
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userName: String,
    val userRole: String,
    val action: String,
    val entity: String,
    val oldValue: String,
    val newValue: String,
    val timestamp: Long,
    val reason: String,
    val result: String = "SUCCESS",
    val correlationId: String = ""
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val category: String,
    val taskId: Long? = null,
    val timestamp: Long,
    val isRead: Boolean = false
)

@Entity(tableName = "offline_queue")
data class OfflineQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idempotencyId: String,
    val actionType: String,
    val payload: String,
    val createdAt: Long
)
