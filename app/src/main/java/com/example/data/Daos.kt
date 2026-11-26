package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getTaskByIdempotencyKey(key: String): TaskEntity?

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTaskCountOnce(): Int

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY deadline ASC")
    fun getTasksByStatus(status: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE department = :dept ORDER BY createdAt DESC")
    fun getTasksByDepartment(dept: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE tasks SET progressPercent = :progress WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Int)

    @Query("UPDATE tasks SET subtasksRaw = :subtasksRaw WHERE id = :id")
    suspend fun updateSubtasks(id: Long, subtasksRaw: String)
}

@Dao
interface TaskCommentDao {
    @Query("SELECT * FROM task_comments ORDER BY timestamp ASC")
    fun getAllComments(): Flow<List<TaskCommentEntity>>

    @Query("SELECT * FROM task_comments WHERE taskId = :taskId ORDER BY timestamp ASC")
    fun getCommentsForTask(taskId: Long): Flow<List<TaskCommentEntity>>

    @Query("SELECT * FROM task_comments WHERE taskId = :taskId ORDER BY timestamp ASC")
    suspend fun getCommentsForTaskOnce(taskId: Long): List<TaskCommentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: TaskCommentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<TaskCommentEntity>)
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports WHERE taskId = :taskId ORDER BY timestamp DESC")
    fun getReportsForTask(taskId: Long): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReports(reports: List<ReportEntity>)
}

@Dao
interface DisciplinaryDao {
    @Query("SELECT * FROM disciplinary_actions ORDER BY issueDate DESC")
    fun getAllActions(): Flow<List<DisciplinaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: DisciplinaryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActions(actions: List<DisciplinaryEntity>)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    suspend fun getAllLogsOnce(): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<AuditLogEntity>)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()
}

@Dao
interface OfflineQueueDao {
    @Query("SELECT * FROM offline_queue ORDER BY createdAt ASC")
    fun getAllQueued(): Flow<List<OfflineQueueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: OfflineQueueEntity): Long

    @Query("DELETE FROM offline_queue WHERE id = :id")
    suspend fun remove(id: Long)

    @Query("DELETE FROM offline_queue")
    suspend fun clearAll()
}
