package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TaskEntity::class,
        TaskCommentEntity::class,
        ReportEntity::class,
        DisciplinaryEntity::class,
        AuditLogEntity::class,
        NotificationEntity::class,
        OfflineQueueEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskCommentDao(): TaskCommentDao
    abstract fun reportDao(): ReportDao
    abstract fun disciplinaryDao(): DisciplinaryDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun notificationDao(): NotificationDao
    abstract fun offlineQueueDao(): OfflineQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "boshqaruv365_zarafshon.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    seedInitialData(database)
                }
            }
        }
    }
}

private suspend fun seedInitialData(db: AppDatabase) {
    val now = System.currentTimeMillis()
    val hour = 3600 * 1000L
    val day = 24 * hour

    val initialTasks = listOf(
        TaskEntity(
            id = 1,
            taskNumber = "B365-2026-000101",
            title = "12-kichik nohiya ichimlik suvi va ko'cha yoritish tizimini yangilash",
            description = "Aholi murojaatlari asosida 12-kichik nohiya hududidagi 4 ta ko'chaga yangi LED chiroqlar o'rnatish va suv bosimini tiklash.",
            sourceType = "Shahar 360° signali",
            confidentialityLevel = "OCHIQ",
            department = "Obodonlashtirish boshqarmasi",
            responsibleLeaderName = "To'rayev Sherzod Mahmudovich",
            assigneeName = "Karimov Jasur Bahromovich",
            assigneeRole = "Bajaruvchi",
            coAssigneesRaw = "Haydarov O. T. (UJKX)",
            observersRaw = "Normurodov I. E. (O'rinbosar)",
            creatorName = "Shukurov Alisher (Rahbar)",
            creatorRole = "Rahbar (Super Admin)",
            reviewerName = "Saidov Mirzo O'ktamovich",
            finalApproverName = "Shukurov Alisher Davronovich",
            isReviewerVerified = false,
            priority = "HIGH",
            status = "IN_PROGRESS",
            progressPercent = 45,
            cycleCount = 1,
            aiMonitoringMode = "AI Nazorat + 24s SLA Radar",
            createdAt = now - 2 * day,
            interimCheckpoint = now + 6 * hour,
            deadline = now + 18 * hour,
            expectedOutcome = "42 dona LED chiroq ishga tushirilgani va suv bosimi 3.0 bar ga chiqqani haqida GPS/foto akt",
            objectName = "12-kichik nohiya, 24-42 uylar oralig'i",
            address = "Zarafshon sh., 12-kichik nohiya",
            latitude = 41.5725,
            longitude = 64.1985,
            geofenceRadius = 150,
            normativAsos = "Shahar hokimining 2026-yil 12-fevraldagi 44-sonli farmoyishi",
            kpiPoints = 15,
            subtasksRaw = "s1|Hududni qurilish chiqindilaridan tozalash|true;;s2|42 dona yangi tejamkor LED chiroqlarni o'rnatish|true;;s3|Daraxtlarni oqlash va shakl berish|false;;s4|Tungi sinov yoritish hisobotini tayyorlash|false",
            riskLevel = "HIGH"
        ),
        TaskEntity(
            id = 2,
            taskNumber = "B365-2026-000102",
            title = "Yangi Zarafshon massividagi 360 o'rinli bolalar bog'chasi qurilishi nazorati",
            description = "Qurilish obyektining 2-qavat temir-beton karkas quyish ishlari sifatini instrumental tekshirish va smeta intizomiga rioya etilishini ta'minlash.",
            sourceType = "Investitsiya dasturi",
            confidentialityLevel = "XIZMAT_UCHUN",
            department = "Qurilish va arxitektura",
            responsibleLeaderName = "Normurodov Ilhom Erkinovich",
            assigneeName = "Raxmonov Otabek Erkinovich",
            assigneeRole = "Bajaruvchi",
            coAssigneesRaw = "Qurilish nazorati inspeksiyasi",
            observersRaw = "Mansurov Q. A. (Moliya)",
            creatorName = "Normurodov Ilhom (O'rinbosar)",
            creatorRole = "Nazoratchi (O'rinbosar)",
            reviewerName = "Saidov Mirzo O'ktamovich",
            finalApproverName = "Shukurov Alisher Davronovich",
            isReviewerVerified = true,
            priority = "HIGH",
            status = "SUBMITTED",
            progressPercent = 85,
            cycleCount = 0,
            aiMonitoringMode = "AI monitoring + Haftalik tasdiq",
            createdAt = now - 4 * day,
            interimCheckpoint = now - 1 * day,
            deadline = now - 3 * hour,
            expectedOutcome = "B-25 beton markasi laboratoriya xulosasi va smeta moslik dalolatnomasi",
            objectName = "3-sonli yangi MTM binosi",
            address = "Zarafshon sh., Yangi Zarafshon massivi, 14-bino",
            latitude = 41.5640,
            longitude = 64.2150,
            geofenceRadius = 200,
            normativAsos = "Investitsiya dasturi doirasidagi Zarafshon bosh rejasi",
            kpiPoints = 20,
            subtasksRaw = "s1|Beton markasi laboratoriya tahlilini olish|true;;s2|Smeta xarajatlari mosligini tekshirish|true;;s3|Qurilish maydoni xavfsizlik kamarini ko'rikdan o'tkazish|true",
            riskLevel = "HIGH"
        ),
        TaskEntity(
            id = 3,
            taskNumber = "B365-2026-000103",
            title = "Markaziy istirohat bog'idagi favvoralar va suv tizimini ta'mirlash",
            description = "Bahorgi mavsumga tayyorgarlik ko'rish, nasos agregatlarini profilaktika qilish va yer osti quvurlarini gidravlik sinovdan o'tkazish.",
            sourceType = "Hokim farmoyishi",
            confidentialityLevel = "OCHIQ",
            department = "Uy-joy kommunal xizmat",
            responsibleLeaderName = "To'rayev Sherzod Mahmudovich",
            assigneeName = "To'rayev Sherzod Mahmudovich",
            assigneeRole = "Mas'ul ijrochi",
            coAssigneesRaw = "Suv ta'minoti brigadasi",
            observersRaw = "Alimov M. R. (Tahlilchi)",
            creatorName = "Shukurov Alisher (Rahbar)",
            creatorRole = "Rahbar (Super Admin)",
            reviewerName = "Saidov Mirzo O'ktamovich",
            finalApproverName = "Shukurov Alisher Davronovich",
            isReviewerVerified = true,
            priority = "MEDIUM",
            status = "COMPLETED",
            progressPercent = 100,
            cycleCount = 0,
            aiMonitoringMode = "AI monitoring + Yakuniy tasdiq",
            createdAt = now - 7 * day,
            interimCheckpoint = now - 3 * day,
            deadline = now - 1 * day,
            expectedOutcome = "Favvoralar gidravlik sinovdan o'tgani va ishga tushirilgani akti",
            objectName = "Zarafshon Markaziy Xiyoboni",
            address = "Zarafshon sh., Navoiy shoh ko'chasi",
            latitude = 41.5768,
            longitude = 64.2045,
            geofenceRadius = 120,
            normativAsos = "Obodonlashtirish haftaligi bayonnomasi",
            kpiPoints = 12,
            subtasksRaw = "s1|Nasos stansiyasi elektr dvigatellarini tekshirish|true;;s2|Quvurlardagi bosimni o'lchash|true;;s3|Suv purkagich nozzllarini tozalash|true;;s4|Hududiy qabul dalolatnomasini rasmiylashtirish|true",
            riskLevel = "LOW"
        ),
        TaskEntity(
            id = 4,
            taskNumber = "B365-2026-000104",
            title = "Sanoat zonasida chiqindi poligonini rekultivatsiya qilish",
            description = "Eski texnik chiqindi to'plangan hududda tuproq unumdorligini tiklash va sanitariya himoya doirasini shakllantirish.",
            sourceType = "Ekologiya nazorati",
            confidentialityLevel = "MAXFIY",
            department = "Ekologiya va atrof-muhit",
            responsibleLeaderName = "Sobirov N. J.",
            assigneeName = "Boboyev Bobur Saidovich",
            assigneeRole = "Bajaruvchi",
            coAssigneesRaw = "Maxsustrans Zarafshon",
            observersRaw = "Saidov Mirzo (Nazoratchi)",
            creatorName = "Normurodov Ilhom (O'rinbosar)",
            creatorRole = "Nazoratchi (O'rinbosar)",
            reviewerName = "Saidov Mirzo O'ktamovich",
            finalApproverName = "Normurodov Ilhom Erkinovich",
            isReviewerVerified = false,
            priority = "EMERGENCY",
            status = "REWORK",
            progressPercent = 40,
            cycleCount = 3,
            aiMonitoringMode = "AI Qayta-Qayta Aylanish Nazorati",
            createdAt = now - 5 * day,
            interimCheckpoint = now - 1 * day,
            deadline = now + 4 * hour,
            expectedOutcome = "Tuproq tahlili akti va rekultivatsiya qilingan maydon GPS xaritasi",
            objectName = "Sanoat zonasi 7-sektor",
            address = "Zarafshon sanoat xabi, Janubiy yo'nalish",
            latitude = 41.5890,
            longitude = 64.2310,
            geofenceRadius = 300,
            normativAsos = "Ekologiya davlat qo'mitasi talabnomasi",
            kpiPoints = 25,
            subtasksRaw = "s1|Tuproq tahlili aktini ilova qilish|true;;s2|Texnika jalb qilinganlik vedomostini tasdiqlash|false;;s3|Foto va GPS koordinatali yakuniy akt|false",
            riskLevel = "HIGH",
            reworkReason = "3-marta qaytarildi: Smeta bo'yicha texnika soatlari va tuproq qatlami dalolatnomasi mos kelmayapti."
        ),
        TaskEntity(
            id = 5,
            taskNumber = "B365-2026-000105",
            title = "Yoshlar markazida 'IT va Kasbga o'rgatish' bepul kurslarini yo'lga qo'yish",
            description = "Kam ta'minlangan oilalar farzandlari uchun 100 o'rinli zamonaviy kompyuter sinflarini jihozlash va o'quv dasturlarini tasdiqlash.",
            sourceType = "PQ-403 qarori ijrosi",
            confidentialityLevel = "OCHIQ",
            department = "Yoshlar siyosati va sport",
            responsibleLeaderName = "Boboyev D. R.",
            assigneeName = "Yusupova Madina Rustamovna",
            assigneeRole = "Bajaruvchi",
            coAssigneesRaw = "IT-Park Zarafshon filiali",
            observersRaw = "Alimov M. R. (Tahlilchi)",
            creatorName = "Shukurov Alisher (Rahbar)",
            creatorRole = "Rahbar (Super Admin)",
            reviewerName = "Saidov Mirzo O'ktamovich",
            finalApproverName = "Shukurov Alisher Davronovich",
            isReviewerVerified = false,
            priority = "MEDIUM",
            status = "ASSIGNED",
            progressPercent = 15,
            cycleCount = 0,
            aiMonitoringMode = "AI monitoring + Haftalik tasdiq",
            createdAt = now - 1 * day,
            interimCheckpoint = now + 1 * day,
            deadline = now + 3 * day,
            expectedOutcome = "100 o'rinli kompyuter sinfi va tasdiqlangan o'quv jadvali",
            objectName = "Zarafshon Yoshlar Ijodiyot Saroyi",
            address = "Zarafshon sh., Yoshlar ko'chasi 5-uy",
            latitude = 41.5710,
            longitude = 64.2090,
            geofenceRadius = 80,
            normativAsos = "Prezidentning PQ-403 sonli qarori ijrosi",
            kpiPoints = 10,
            subtasksRaw = "s1|Kompyuter texnikalarini qabul qilib olish|false;;s2|Internet optik tolasini ulash|false;;s3|Mentorlar bilan shartnoma imzolash|false",
            riskLevel = "LOW",
            routingType = "VERTIKAL",
            reportFormat = "HUJJAT"
        ),
        TaskEntity(
            id = 6,
            taskNumber = "B365-2026-000106",
            title = "Shahar markaziy magistral ko'chalarini kompleks yoritish va suv tarmoqlarini muvofiqlashtirish",
            description = "Obodonlashtirish va UJKX bo'limlari hamkorligida tarmoq sinovlari va tungi yoritish tizimini yakuniy qabul qilish.",
            sourceType = "Ko'p tashkilotli hamkorlik",
            confidentialityLevel = "XIZMAT_UCHUN",
            department = "Obodonlashtirish boshqarmasi",
            responsibleLeaderName = "To'rayev Sherzod Mahmudovich",
            assigneeName = "Karimov Jasur Bahromovich",
            assigneeRole = "Bajaruvchi",
            coAssigneesRaw = "Haydarov O. T. (UJKX)",
            observersRaw = "Normurodov I. E. (O'rinbosar)",
            creatorName = "Shukurov Alisher (Rahbar)",
            creatorRole = "Rahbar (Hokim)",
            reviewerName = "Saidov Mirzo O'ktamovich",
            finalApproverName = "Shukurov Alisher Davronovich",
            isReviewerVerified = true,
            priority = "HIGH",
            status = "APPROVAL_PENDING",
            progressPercent = 95,
            cycleCount = 0,
            aiMonitoringMode = "AI monitoring + Ko'p tashkilotli nazorat",
            createdAt = now - 3 * day,
            interimCheckpoint = now - 12 * hour,
            deadline = now + 2 * day,
            expectedOutcome = "Ikkala boshqarma bo'yicha yakuniy dalil va foto akt",
            objectName = "Navoiy shoh ko'chasi 1-3 sektor",
            address = "Zarafshon sh., Markaziy magistral",
            latitude = 41.5750,
            longitude = 64.2020,
            geofenceRadius = 150,
            normativAsos = "Shahar 360° qo'shma bayonnomasi №48",
            kpiPoints = 22,
            subtasksRaw = "s1|Obodonlashtirish elektr ustunlari akti|true;;s2|UJKX suv quvurlari bosim akti|true",
            riskLevel = "LOW",
            routingType = "KOP_TASHKILOTLI",
            reportFormat = "FOTO",
            participatingOrgsRaw = "Obodonlashtirish boshqarmasi|Karimov Jasur Bahromovich|100|true;;Uy-joy kommunal xizmat|Haydarov O. T.|100|true",
            sourceCommunicationId = "DIAG-2026-041"
        )
    )

    db.taskDao().insertTasks(initialTasks)

    val initialComments = listOf(
        // Task 1 Comment Loop
        TaskCommentEntity(1, 1, "Shukurov Alisher", "Rahbar (Super Admin)", "INSTRUCTION", "12-kichik nohiyada kechki chiroqlar va nasos bosimi ustuvor tartibda 18 soat ichida yopilsin.", null, now - 2 * day),
        TaskCommentEntity(2, 1, "Karimov Jasur", "Ijrochi", "PROGRESS_NOTE", "42 dona LED chiroq o'rnatildi. Hozir 2-brigada daraxtlar va suv bosimi datchigini sozlamoqda (45%).", "led_akt_12.pdf", now - 1 * day),
        TaskCommentEntity(3, 1, "To'rayev Sherzod", "Bo'lim Mas'uli", "DIAGONAL_SYNC", "UJKX bo'limi bilan quvur sinovi muddati bugun 17:00 gacha kelishildi (DIAG-2026-041).", "DIAG-2026-041", now - 6 * hour),

        // Task 2 Comment Loop
        TaskCommentEntity(4, 2, "Normurodov Ilhom", "Nazoratchi (O'rinbosar)", "INSTRUCTION", "2-qavat beton quyishdan oldin B-25 laboratoriya xulosasi ilova qilinishi shart.", null, now - 3 * day),
        TaskCommentEntity(5, 2, "Raxmonov Otabek", "Ijrochi", "SUBMISSION", "Laboratoriya xulosasi olindi (B-25 tasdiqlandi) va smeta moslik akti yuklandi. Tekshiruvga yuborildi.", "lab_b25_xulosa.pdf", now - 5 * hour),

        // Task 4 — 3-cycle Comment Loop example!
        TaskCommentEntity(6, 4, "Normurodov Ilhom", "Nazoratchi (O'rinbosar)", "INSTRUCTION", "Sanoat zonasi 7-sektorda tuproq tahlili va texnika soatlari dalolatnomasini topshiring.", null, now - 5 * day),
        TaskCommentEntity(7, 4, "Boboyev Bobur", "Ijrochi", "SUBMISSION", "Dastlabki foto va texnika ro'yxati yuborildi.", "foto_7sektor.jpg", now - 4 * day),
        TaskCommentEntity(8, 4, "Saidov Mirzo", "Nazoratchi", "REJECTION_COMMENT", "1-qaytarish: Ekskavator ishlagan soatlar GPS trek bilan tasdiqlanmagan.", null, now - 3 * day),
        TaskCommentEntity(9, 4, "Boboyev Bobur", "Ijrochi", "SUBMISSION", "GPS trek yuklandi, qayta tekshiruvga yuborildi.", "gps_trek_v2.xlsx", now - 2 * day),
        TaskCommentEntity(10, 4, "Saidov Mirzo", "Nazoratchi", "REJECTION_COMMENT", "2-qaytarish: Tuproq namunalari laboratoriya imzosi yo'q.", null, now - 1 * day),
        TaskCommentEntity(11, 4, "Saidov Mirzo", "Nazoratchi", "REJECTION_COMMENT", "3-qaytarish: Smeta hisob-kitobi va fakt soatlar 12% tafovut bermoqda. Qayta tekshiring!", null, now - 3 * hour)
    )
    db.taskCommentDao().insertComments(initialComments)

    val initialReports = listOf(
        ReportEntity(
            id = 1,
            taskId = 1,
            reporterName = "Karimov Jasur Bahromovich",
            timestamp = now - 6 * hour,
            comment = "12-kichik nohiyada 42 dona LED chiroqlar to'liq o'rnatildi. Sim yog'ochlar mustahkamlandi. Chiqindilar chiqarildi.",
            photoPath = "res/drawable/zarafshon_banner_1790585492174.jpg",
            latitude = 41.5724,
            longitude = 64.1986,
            distanceToTargetMeters = 14,
            gpsAccuracyMeters = 3.2f,
            isAntiFraudPassed = true,
            antiFraudWarning = null
        ),
        ReportEntity(
            id = 2,
            taskId = 2,
            reporterName = "Raxmonov Otabek Erkinovich",
            timestamp = now - 5 * hour,
            comment = "Bog'chaning 2-qavati karkas quyish ishlari bo'yicha laboratoriya sinov xulosasi olindi (B-25 markasi tasdiqlandi).",
            latitude = 41.5641,
            longitude = 64.2152,
            distanceToTargetMeters = 18,
            gpsAccuracyMeters = 4.0f,
            isAntiFraudPassed = true,
            antiFraudWarning = null
        )
    )
    db.reportDao().insertReports(initialReports)

    val initialDiscipline = listOf(
        DisciplinaryEntity(
            id = 1,
            employeeName = "Boboyev Bobur Saidovich",
            department = "Ekologiya va atrof-muhit",
            actionType = "OGOHLANTIRISH",
            reason = "B365-2026-000104 topshirig'i 3 marta qaytarilgani va smeta tafovuti uchun ogohlantirildi.",
            taskId = 4,
            issuedBy = "Shukurov Alisher (Rahbar)",
            issueDate = now - 1 * day,
            docNumber = "DISC-2026-012",
            status = "AMALDA"
        ),
        DisciplinaryEntity(
            id = 2,
            employeeName = "Qodirov Sardor Olimovich",
            department = "Qurilish va arxitektura",
            actionType = "TUSHUNTIRISH_XATI",
            reason = "Ob'ekt ko'rigida belgilangan 24 soatlik geofence muddatini buzganligi uchun tushuntirish xati talab qilindi.",
            taskId = 2,
            issuedBy = "Normurodov Ilhom (O'rinbosar)",
            issueDate = now - 3 * day,
            docNumber = "DISC-2026-009",
            status = "IJRODA"
        )
    )
    db.disciplinaryDao().insertActions(initialDiscipline)

    val initialAudit = listOf(
        AuditLogEntity(
            id = 1,
            userName = "Shukurov Alisher",
            userRole = "Rahbar (Hokim)",
            action = "CREATE_TASK",
            entity = "B365-2026-000101",
            oldValue = "DRAFT",
            newValue = "ASSIGNED -> Karimov Jasur",
            timestamp = now - 2 * day,
            reason = "Rejali shahar obodonlashtirish dasturi ijrosi",
            result = "SUCCESS",
            correlationId = "CORR-SEED-001"
        ),
        AuditLogEntity(
            id = 2,
            userName = "Saidov Mirzo",
            userRole = "Nazoratchi",
            action = "TASK_REWORK (3-tsikl)",
            entity = "B365-2026-000104",
            oldValue = "SUBMITTED",
            newValue = "REWORK (cycleCount=3)",
            timestamp = now - 3 * hour,
            reason = "Media va smeta hisoboti talabga javob bermadi",
            result = "SUCCESS",
            correlationId = "CORR-SEED-002"
        ),
        AuditLogEntity(
            id = 3,
            userName = "Raxmonov Otabek",
            userRole = "Ijrochi",
            action = "SUBMIT_FOR_REVIEW",
            entity = "B365-2026-000102",
            oldValue = "IN_PROGRESS",
            newValue = "SUBMITTED (85%)",
            timestamp = now - 5 * hour,
            reason = "B-25 beton xulosasi yuklandi",
            result = "SUCCESS",
            correlationId = "CORR-SEED-003"
        )
    )
    db.auditLogDao().insertLogs(initialAudit)

    val initialNotifications = listOf(
        NotificationEntity(
            id = 1,
            title = "AI Kechikish Radari: 18 soat qoldi (45%)",
            message = "B365-2026-000101 topshirig'ida bajarilish 50% dan past, kechikish xavfi yuqori.",
            category = "DEADLINE",
            taskId = 1,
            timestamp = now - 1 * hour,
            isRead = false
        ),
        NotificationEntity(
            id = 2,
            title = "Diqqat: 3+ marta qaytgan topshiriq!",
            message = "B365-2026-000104 topshirig'i 3 marta qaytarildi. Rahbar aralashuvi tavsiya etiladi.",
            category = "INTIZOM",
            taskId = 4,
            timestamp = now - 3 * hour,
            isRead = false
        ),
        NotificationEntity(
            id = 3,
            title = "Nazoratchi tekshiruvida (28 soatdan beri)",
            message = "B365-2026-000102 bo'yicha hisobot yuborilgan, yakuniy tasdiq kutilmoqda.",
            category = "HISOBOT",
            taskId = 2,
            timestamp = now - 5 * hour,
            isRead = true
        )
    )
    db.notificationDao().insertNotifications(initialNotifications)
}
