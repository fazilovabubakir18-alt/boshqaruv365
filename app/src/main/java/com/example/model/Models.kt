package com.example.model

enum class UserRole(val titleUz: String, val badgeColorHex: Long) {
    HOKIM("Rahbar (Hokim)", 0xFF1E3A8A),
    SUPER_ADMIN("Markaziy administrator", 0xFF0F766E),
    ORINBOSAR("Nazoratchi (O'rinbosar)", 0xFF2481CC),
    BOLIM_RAHBARI("Bo'lim Mas'uli", 0xFF0D7A5F),
    IJROCHI("Ijrochi", 0xFF475569),
    NAZORATCHI("Nazoratchi", 0xFFB45309),
    TAHLILCHI("Tahlilchi (Auditor)", 0xFF7C3AED)
}

/**
 * Rule 1: Global Role != Task Role.
 * Any user can hold a specific functional role inside a given task.
 */
enum class TaskRole(val titleUz: String) {
    BERUVCHI("Beruvchi"),
    MASUL_RAHBAR("Mas'ul rahbar"),
    IJROCHI("Bajaruvchi"),
    HAMIJROCHI("Hamijrochi"),
    KUZATUVCHI("Kuzatuvchi"),
    NAZORATCHI("Nazoratchi"),
    YAKUNIY_TASDIQLOVCHI("Yakuniy tasdiqlovchi")
}

enum class TaskCommentType(val labelUz: String, val colorHex: Long) {
    INSTRUCTION("Ko'rsatma", 0xFF2481CC),
    PROGRESS_NOTE("Jarayon hisoboti", 0xFF0EA5E9),
    CLARIFICATION("Aniqlashtirish so'rovi", 0xFFF59E0B),
    SUBMISSION("Tekshiruvga topshirish", 0xFF8B5CF6),
    REJECTION_COMMENT("Qaytarish sababi", 0xFFEF4444),
    APPROVAL_COMMENT("Tasdiq izohi", 0xFF10B981),
    DIAGONAL_SYNC("Diagonal izoh", 0xFFD97706)
}

enum class TaskRoutingType(val labelUz: String) {
    VERTIKAL("Vertikal (yuqoridan pastga)"),
    TOGRIDAN_TOGRI("To'g'ridan-to'g'ri"),
    KOP_TASHKILOTLI("Ko'p tashkilotli"),
    LOYIHA("Loyiha / dastur"),
    HUDUDIY("Hududiy / MFY")
}

enum class ReportFormat(val labelUz: String) {
    MATN("Matn hisoboti"),
    HUJJAT("Rasmiy hujjat"),
    FOTO("Foto dalil"),
    VIDEO("Video dalil"),
    KOORDINATA("GPS koordinata"),
    JADVAL("Jadval / Smeta")
}

/**
 * ABAC Confidentiality clearance levels
 */
enum class ConfidentialityLevel(val titleUz: String, val level: Int, val colorHex: Long) {
    OCHIQ("Ochiq", 1, 0xFF10B981),
    XIZMAT_UCHUN("Xizmat doirasida", 2, 0xFF2481CC),
    MAXFIY("Maxfiy", 3, 0xFFF59E0B),
    OTA_MAXFIY("O'ta maxfiy", 4, 0xFFEF4444)
}

enum class TaskStatus(val titleUz: String, val colorHex: Long) {
    DRAFT("Qoralama", 0xFF94A3B8),
    ASSIGNED("Biriktirildi", 0xFF3B82F6),
    NOTIFIED("Xabardor qilindi", 0xFF6366F1),
    ACCEPTED("Qabul qilindi", 0xFF0EA5E9),
    IN_PROGRESS("Jarayonda", 0xFFF59E0B),
    SUBMITTED("Natija yuborildi", 0xFF8B5CF6),
    REVIEW("Tekshiruvda", 0xFFD97706),
    APPROVAL_PENDING("Tasdiq kutilmoqda", 0xFF7C3AED),
    REWORK("Qayta ishlashda", 0xFFEF4444),
    ON_HOLD("To'xtatib turilgan", 0xFF64748B),
    EXTENSION_REQUESTED("Muddat so'ralgan", 0xFFF97316),
    ESCALATED("Eskalatsiya", 0xFFDC2626),
    REJECTED("Rad etilgan", 0xFF991B1B),
    CANCELLED("Bekor qilingan", 0xFF475569),
    COMPLETED("Yakunlandi", 0xFF10B981),
    ARCHIVED("Arxivlandi", 0xFF64748B)
}

enum class TaskPriority(val titleUz: String, val colorHex: Long) {
    LOW("Past", 0xFF64748B),
    MEDIUM("O'rta", 0xFF3B82F6),
    HIGH("Yuqori", 0xFFF97316),
    EMERGENCY("Favqulodda", 0xFFDC2626)
}

enum class Department(val titleUz: String, val shortCode: String) {
    OBODONLASHTIRISH("Obodonlashtirish boshqarmasi", "OBOD"),
    QURILISH("Qurilish va arxitektura", "QUR"),
    UY_JOY("Uy-joy kommunal xizmat", "UJKX"),
    IQTISODIYOT("Iqtisodiyot va moliya", "IQT"),
    YOSHLAR("Yoshlar siyosati va sport", "YOSH"),
    EKOLOGIYA("Ekologiya va atrof-muhit", "EKO")
}

data class SubTask(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false
)

data class OrgExecutionPart(
    val orgName: String,
    val assigneeName: String,
    val progressPercent: Int = 0,
    val isCompleted: Boolean = false
)

data class GisLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val radiusMeters: Int = 100
)

data class AntiFraudCheck(
    val isPassed: Boolean = true,
    val exifVerified: Boolean = true,
    val isDuplicateMedia: Boolean = false,
    val gpsMatchMeters: Int = 25,
    val notes: String = "EXIF va GPS koordinata to'liq mos keldi"
)
