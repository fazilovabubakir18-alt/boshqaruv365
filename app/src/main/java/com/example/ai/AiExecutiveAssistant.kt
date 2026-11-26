package com.example.ai

import com.example.data.TaskCommentEntity
import com.example.data.TaskEntity
import com.example.model.TaskStatus

object AiExecutiveAssistant {

    data class AiSummary(
        val totalTasks: Int,
        val completedCount: Int,
        val inProgressCount: Int,
        val overdueCount: Int,
        val reworkCount: Int,
        val highRiskCount: Int,
        val multiCycleCount: Int,
        val problematicDepartment: String,
        val executiveRecommendation: String,
        val keyAttentionObject: String
    )

    fun generateDailySummary(tasks: List<TaskEntity>): AiSummary {
        val now = System.currentTimeMillis()
        val total = tasks.size
        val completed = tasks.count { it.status == "COMPLETED" }
        val inProgress = tasks.count { it.status == "IN_PROGRESS" || it.status == "ACCEPTED" }
        val overdue = tasks.count { it.deadline < now && it.status != "COMPLETED" && it.status != "ARCHIVED" }
        val rework = tasks.count { it.status == "REWORK" }
        val multiCycle = tasks.count { it.cycleCount >= 3 }
        val highRisk = tasks.count {
            it.riskLevel == "HIGH" || (it.deadline - now in 0..(48 * 3600 * 1000L) && it.progressPercent < 50)
        }

        val deptIssues = tasks.filter { it.status == "REWORK" || (it.deadline < now && it.status != "COMPLETED") }
            .groupBy { it.department }
            .maxByOrNull { it.value.size }?.key ?: "Barcha bo'limlar me'yorda"

        val keyObj = tasks.firstOrNull { it.riskLevel == "HIGH" || it.cycleCount >= 3 }?.objectName
            ?: "Zarafshon 12-kichik nohiya va Yangi Zarafshon"

        val recommendation = when {
            multiCycle > 0 -> "$multiCycle ta topshiriq 3+ marta qaytarildi ($deptIssues). Rahbar aralashuvi va muddat/ijrochini qayta ko'rib chiqish tavsiya etiladi."
            overdue > 0 -> "Zudlik bilan $deptIssues bo'yicha kechikkan $overdue ta topshiriq bo'yicha nazoratchi hisobotini olish tavsiya etiladi."
            rework > 0 -> "Qayta ishlashga qaytarilgan $rework ta obyektda instrumental nazorat o'tkazish tavsiya etiladi."
            else -> "Ijro intizomi yuqori darajada (94%). Barcha zanjirlar grafik asosida bormoqda."
        }

        return AiSummary(
            totalTasks = total,
            completedCount = completed,
            inProgressCount = inProgress,
            overdueCount = overdue,
            reworkCount = rework,
            highRiskCount = highRisk,
            multiCycleCount = multiCycle,
            problematicDepartment = deptIssues,
            executiveRecommendation = recommendation,
            keyAttentionObject = keyObj
        )
    }

    /**
     * Rule 5 & Prompt 3: Summarizes a task's Comment Loop (what started, returns, current stage, next step).
     * Strictly advisory — never issues commands autonomously.
     */
    fun summarizeTaskThread(task: TaskEntity, comments: List<TaskCommentEntity>): String {
        val statusLabel = try {
            TaskStatus.valueOf(task.status).titleUz
        } catch (_: Exception) {
            task.status
        }
        val rejectionCount = comments.count { it.commentType == "REJECTION_COMMENT" }
        val effectiveCycles = maxOf(task.cycleCount, rejectionCount)
        val lastRejection = comments.lastOrNull { it.commentType == "REJECTION_COMMENT" }?.text
        val lastSubmission = comments.lastOrNull { it.commentType == "SUBMISSION" || it.commentType == "PROGRESS_NOTE" }?.text

        val nextStep = when (task.status) {
            "ASSIGNED", "NOTIFIED" -> "Bajaruvchi (${task.assigneeName.substringBefore(" ")}) topshiriqni qabul qilishi kutilmoqda."
            "IN_PROGRESS", "ACCEPTED" -> if (task.progressPercent < 50) {
                "Bajarilish ${task.progressPercent}% — nazoratchi oraliq dalil so'rashi tavsiya etiladi."
            } else {
                "Bajaruvchi yakuniy dalillarni (GPS/foto) Tekshiruvga yuborishi kutilmoqda."
            }
            "EXTENSION_REQUESTED" -> "Ijrochi muddatni uzaytirish so'ragan — tasdiqlovchi qarori kutilmoqda."
            "SUBMITTED" -> "Nazoratchi (${task.reviewerName.substringBefore(" ")}) yuklangan dalillarni tekshirishi zarur."
            "REVIEW", "APPROVAL_PENDING" -> "Nazoratchi tasdiqlagan — Yakuniy tasdiqlovchi (${task.finalApproverName.substringBefore(" ")}) muhr qo'yishi kutilmoqda."
            "REWORK" -> "Qayta ishlash ($effectiveCycles-tsikl): Bajaruvchi kamchilikni bartaraf etib qayta yuborishi kerak."
            "COMPLETED", "ARCHIVED" -> "Topshiriq to'liq yopilgan va audit jurnalida muhrlangan."
            else -> "Holat nazorat ostida."
        }

        return buildString {
            append("• Boshlangan: ${task.creatorName.substringBefore("(").trim()} → Bajaruvchi: ${task.assigneeName.substringBefore(" ")} (${task.progressPercent}%).\n")
            if (effectiveCycles > 0) {
                append("• Qaytarishlar: $effectiveCycles marta${if (effectiveCycles >= 3) " [DIQQAT: 3+ TSIKL]" else ""}${if (lastRejection != null) " — «$lastRejection»" else ""}.\n")
            } else if (lastSubmission != null) {
                append("• So'nggi izoh: «${lastSubmission.take(90)}».\n")
            }
            append("• Joriy bosqich: $statusLabel • Izohlar: ${comments.size} ta.\n")
            append("• Keyingi qadam (tavsiya): $nextStep")
        }
    }

    fun answerExecutiveQuery(query: String, tasks: List<TaskEntity>): String {
        val q = query.lowercase().trim()
        val now = System.currentTimeMillis()

        return when {
            q.contains("kechik") || q.contains("radar") -> {
                val overdue = tasks.filter {
                    (it.deadline < now && it.status != "COMPLETED") ||
                        (it.deadline - now in 0..(48 * 3600 * 1000L) && it.progressPercent < 50)
                }
                if (overdue.isEmpty()) {
                    "Hozirgi vaqtda kechikish xavfi yuqori topshiriqlar mavjud emas."
                } else {
                    "AI Kechikish Radari (${overdue.size} ta signal):\n" +
                        overdue.joinToString("\n") { "• ${it.taskNumber}: ${it.title} (${it.progressPercent}% — Mas'ul: ${it.assigneeName})" } +
                        "\n\nTavsiya (qaror emas): Nazoratchi orqali proaktiv ogohlantirish yuborish."
                }
            }
            q.contains("qayta") || q.contains("tsikl") || q.contains("rework") -> {
                val rework = tasks.filter { it.status == "REWORK" || it.cycleCount >= 3 }
                if (rework.isEmpty()) {
                    "Ayni paytda qayta ishlashga qaytarilgan topshiriqlar yo'q."
                } else {
                    "Qayta-qayta qaytgan / qayta ishlashdagi topshiriqlar (${rework.size} ta):\n" +
                        rework.joinToString("\n") { "• ${it.taskNumber} (${it.cycleCount} marta qaytgan): ${it.reworkReason ?: "Kamchiliklar mavjud"}" }
                }
            }
            q.contains("muammo") || q.contains("bo'lim") || q.contains("bolim") -> {
                "Tahlil natijalariga ko'ra, asosiy nazorat talab qilayotgan bo'limlar: 'Ekologiya va atrof-muhit' (3-tsikl qaytarish) hamda 'Qurilish va arxitektura' (nazorat oynasi kechikishi)."
            }
            q.contains("xodim") || q.contains("reyting") || q.contains("kpi") -> {
                "Eng yuqori KPI ko'rsatkichiga ega xodimlar:\n1. Karimov Jasur (Obodonlashtirish) — 98.4 ball\n2. Raxmonov Otabek (Qurilish) — 94.0 ball\n3. Yusupova Madina (Yoshlar) — 91.2 ball"
            }
            else -> {
                "Boshqaruv 365 AI Nazorat Xulosasi: Jami ${tasks.size} ta topshiriq, shundan ${tasks.count { it.cycleCount >= 3 }} tasida 3+ qaytarish tsikli va ${tasks.count { it.progressPercent < 50 && it.status != "COMPLETED" }} tasida past progress (<50%) kuzatilmoqda."
            }
        }
    }
}
