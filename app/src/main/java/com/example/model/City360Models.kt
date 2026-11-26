package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.R

enum class ThemeMode(val labelUz: String) {
    LIGHT("Yorug' (Light)"),
    DARK("Tungi (Dark)"),
    SYSTEM("Tizim (Auto)")
}

enum class AccentChoice(val labelUz: String, val primaryColor: Color) {
    TELEGRAM_BLUE("Telegram Ko'k", Color(0xFF2481CC)),
    EXECUTIVE_NAVY("Davlat Ko'k", Color(0xFF1E3A8A)),
    EMERALD("Zumrad Yashil", Color(0xFF059669)),
    GOLD("Zarafshon Oltin", Color(0xFFD97706))
}

enum class AppLanguage(val code: String, val label: String) {
    UZ_LATIN("UZ", "O'zbekcha (Lotin)"),
    UZ_CYRILLIC("ЎЗ", "Ўзбекча (Кирилл)"),
    RU("RU", "Русский"),
    EN("EN", "English")
}

data class AppSettingsState(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val accentChoice: AccentChoice = AccentChoice.TELEGRAM_BLUE,
    val language: AppLanguage = AppLanguage.UZ_LATIN,
    val compactDensity: Boolean = false
)

enum class ProblemSeverity(val labelUz: String, val color: Color) {
    KRITIK("KRITIK", Color(0xFFEF4444)),
    YUQORI("YUQORI", Color(0xFFF97316)),
    ORTA("O'RTA", Color(0xFFF59E0B)),
    NAZORATDA("NAZORATDA", Color(0xFF229ED9))
}

enum class ProblemStatus(val labelUz: String, val color: Color) {
    OCHIQ("Ochiq muammo", Color(0xFFEF4444)),
    TOPSHIRIQ_BERILDI("Topshiriq berildi", Color(0xFF2481CC)),
    BARTARAF_ETILDI("Bartaraf etildi", Color(0xFF10B981))
}

data class HealthCategoryWeight(
    val id: String,
    val name: String,
    val weightPercent: Int,
    val scorePercent: Int
)

data class CityProblem(
    val id: String,
    val title: String,
    val description: String,
    val severity: ProblemSeverity,
    val sector: String,
    val mahalla: String,
    val startDate: String,
    val orgName: String,
    val leaderName: String,
    val appealsCount: Int,
    val status: ProblemStatus,
    val aiSuggestedTaskTitle: String,
    val indicatorDropNote: String,
    val xRatio: Float,
    val yRatio: Float,
    val imageResId: Int,
    val linkedTaskId: Long? = null
)

data class CityStoryHighlight(
    val id: String,
    val title: String,
    val badgeText: String,
    val headline: String,
    val summary: String,
    val metricValue: String,
    val ringColors: List<Color>,
    val isUnread: Boolean = true,
    val targetTab: Int = 0,
    val imageResId: Int
)

data class CityObjectFeedItem(
    val id: String,
    val channelName: String,
    val channelRole: String,
    val timestampText: String,
    val title: String,
    val caption: String,
    val location: String,
    val progressPercent: Int,
    val verifiedCount: Int,
    val isVerifiedByUser: Boolean = false,
    val statusTag: String,
    val statusColor: Color,
    val imageResId: Int,
    val linkedTaskId: Long
)

data class MahallaProfile(
    val id: String,
    val name: String,
    val coordinator: String,
    val population: Int,
    val healthScore: Int,
    val openIssues: Int,
    val completedTasks: Int,
    val appealsResolvedPct: Int
)

data class InvestmentProject(
    val id: String,
    val title: String,
    val sector: String,
    val budgetUsdMillion: Double,
    val progressPercent: Int,
    val jobsPlanned: Int,
    val contractor: String,
    val statusLabel: String,
    val isDelayed: Boolean,
    val imageResId: Int
)

data class DocumentRecord(
    val id: String,
    val docNumber: String,
    val title: String,
    val category: String,
    val date: String,
    val signedBy: String,
    val previewSummary: String,
    val status: String
)

data class IndicatorCatalogItem(
    val id: String,
    val name: String,
    val sector: String,
    val unit: String,
    val currentValue: String,
    val targetValue: String,
    val formula: String,
    val sourceOrg: String,
    val isHealthy: Boolean
)

enum class DiagonalTransferType(val labelUz: String) {
    RASMIY_SOROV("Rasmiy so'rov"),
    MALUMOT_UZATISH("Ma'lumot uzatish"),
    HAMKORLIK("Hamkorlikdagi topshiriq")
}

enum class DiagonalTransferStatus(val labelUz: String, val color: Color) {
    YUBORILDI("Yuborildi", Color(0xFF2481CC)),
    QABUL_QILINDI("Qabul qilindi", Color(0xFF8B5CF6)),
    BAJARILDI("Javob berildi", Color(0xFF10B981)),
    KECHIKMOQDA("Kechikmoqda", Color(0xFFEF4444))
}

data class DiagonalTransfer(
    val id: String,
    val docCode: String,
    val type: DiagonalTransferType,
    val senderDept: String,
    val senderLeader: String,
    val receiverDept: String,
    val receiverLeader: String,
    val subject: String,
    val basisDoc: String,
    val deadlineText: String,
    val supervisorName: String,
    val status: DiagonalTransferStatus,
    val responseNote: String? = null,
    val aiMonitoringNote: String,
    val createdAtText: String,
    val linkedTaskNumber: String? = null
)

data class AiControlModule(
    val id: String,
    val title: String,
    val subtitle: String,
    val statusLabel: String,
    val alertCount: Int,
    val accentColor: Color
)

data class AiAnomalySignal(
    val id: String,
    val moduleName: String,
    val title: String,
    val description: String,
    val forecastTag: String,
    val sourceLabel: String,
    val detectedTime: String,
    val confidencePercent: Int,
    val severity: ProblemSeverity,
    val sector: String,
    val recommendedAction: String
)

object City360SeedData {

    val initialHealthWeights = listOf(
        HealthCategoryWeight("eco", "Iqtisodiyot", 20, 91),
        HealthCategoryWeight("emp", "Bandlik", 15, 86),
        HealthCategoryWeight("soc", "Ijtimoiy soha", 15, 89),
        HealthCategoryWeight("inf", "Infratuzilma", 15, 81),
        HealthCategoryWeight("obo", "Obodonlashtirish", 10, 94),
        HealthCategoryWeight("inv", "Investitsiya", 10, 88),
        HealthCategoryWeight("mur", "Murojaatlar", 5, 79),
        HealthCategoryWeight("ekl", "Ekologiya", 5, 82),
        HealthCategoryWeight("oth", "Boshqa", 5, 92)
    )

    val initialStories = listOf(
        CityStoryHighlight(
            id = "st1",
            title = "Shahar 360°",
            badgeText = "87%",
            headline = "Zarafshon Shahar Sog'lig'i Indeksi: 87% (Yaxshi)",
            summary = "9 ta asosiy strategik yo'nalish bo'yicha bugungi holat barqaror. Iqtisodiyot (+4.2%) va Obodonlashtirish (94%) yetakchilik qilmoqda.",
            metricValue = "87 / 100 ball",
            ringColors = listOf(Color(0xFF229ED9), Color(0xFF10B981)),
            isUnread = true,
            targetTab = 2,
            imageResId = R.drawable.img_command_hero_1795671436085
        ),
        CityStoryHighlight(
            id = "st2",
            title = "Kritik (3)",
            badgeText = "LIVE",
            headline = "Tezkor E'tibor: 3 ta Kritik Muammo",
            summary = "12-kichik nohiyada suv bosimi tushishi (23 murojaat) va Yangi Zarafshon qurilishida jadvaldan 4 kun orqada qolish qayd etildi.",
            metricValue = "3 kritik signal",
            ringColors = listOf(Color(0xFFEF4444), Color(0xFFF59E0B)),
            isUnread = true,
            targetTab = 0,
            imageResId = R.drawable.img_water_infra_1795671435228
        ),
        CityStoryHighlight(
            id = "st3",
            title = "5 daq. Brifing",
            badgeText = "AI",
            headline = "Hokimga 5 Daqiqalik Strategik Brifing",
            summary = "1) Eng kritik: Suv bosimi. 2) Eng tez pasayish: Murojaatlar (-8%). 3) Eng kuchsiz: Ekologiya. 4) Bugun topshiriq: Suv ta'minoti ta'miri.",
            metricValue = "AI Tahlil Tayyor",
            ringColors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
            isUnread = true,
            targetTab = 4,
            imageResId = R.drawable.zarafshon_banner_1790585492174
        ),
        CityStoryHighlight(
            id = "st4",
            title = "Qurilish",
            badgeText = "78%",
            headline = "Yangi Zarafshon Turar-joy Majmuasi",
            summary = "420 ta xonadonli yangi ko'p qavatli uylar va markaziy shoh ko'cha asfaltlash ishlari 78% ga bajarildi.",
            metricValue = "18.4 mlrd so'm",
            ringColors = listOf(Color(0xFFF59E0B), Color(0xFF2481CC)),
            isUnread = false,
            targetTab = 2,
            imageResId = R.drawable.img_city_construction_1795671434776
        ),
        CityStoryHighlight(
            id = "st5",
            title = "Mahallalar",
            badgeText = "15 ta",
            headline = "15 ta MFY Real-Vaqt Reytingi",
            summary = "Navoiy va Alisher Navoiy MFY 95% natija bilan peshqadam. Geolog MFYda chiqindi tashish grafigini kuchaytirish tavsiya etiladi.",
            metricValue = "15 ta MFY faol",
            ringColors = listOf(Color(0xFF10B981), Color(0xFF229ED9)),
            isUnread = false,
            targetTab = 3,
            imageResId = R.drawable.zarafshon_banner_1790585492174
        )
    )

    val initialProblems = listOf(
        CityProblem(
            id = "prb-1",
            title = "12-kichik nohiyada ichimlik suvi bosimi pasayishi",
            description = "24-42 ko'p qavatli uylarda kechki soatlarda suv bosimi 1.2 bar ga tushgan. Nasos agregatida bosim datchigi nosozligi aniqlandi.",
            severity = ProblemSeverity.KRITIK,
            sector = "Infratuzilma & Suv",
            mahalla = "12-kichik nohiya MFY",
            startDate = "2 kun oldin",
            orgName = "Zarafshon Suv Ta'minoti",
            leaderName = "To'rayev Sherzod",
            appealsCount = 23,
            status = ProblemStatus.OCHIQ,
            aiSuggestedTaskTitle = "12-kichik nohiya nasos stansiyasini tezkor ta'mirlash va bosimni tiklash",
            indicatorDropNote = "Infratuzilma indeksi -7% ga pasaydi",
            xRatio = 0.35f,
            yRatio = 0.42f,
            imageResId = R.drawable.img_water_infra_1795671435228,
            linkedTaskId = 1L
        ),
        CityProblem(
            id = "prb-2",
            title = "Yangi Zarafshon 14-bino qurilish muddatidan kechikish",
            description = "Fasad issiqlik qoplamasi yetkazib berish logistikasida 4 kunlik uzilish sababli reja grafikdan ortda qolish kuzatilmoqda.",
            severity = ProblemSeverity.YUQORI,
            sector = "Qurilish & Uy-joy",
            mahalla = "Yangi Zarafshon MFY",
            startDate = "4 kun oldin",
            orgName = "Qurilish va arxitektura bo'limi",
            leaderName = "Raxmonov Otabek",
            appealsCount = 9,
            status = ProblemStatus.OCHIQ,
            aiSuggestedTaskTitle = "Yangi Zarafshon 14-bino qurilishiga qo'shimcha brigada jalb etish",
            indicatorDropNote = "Qurilish rejasi 84% -> 76%",
            xRatio = 0.68f,
            yRatio = 0.65f,
            imageResId = R.drawable.img_city_construction_1795671434776,
            linkedTaskId = 2L
        ),
        CityProblem(
            id = "prb-3",
            title = "Sanoat zonasi 7-sektorda havo changlanishi oshishi",
            description = "Shamol yo'nalishi o'zgarishi va suv sepish texnikasi grafigi buzilgani sababli PM2.5 ko'rsatkichi me'yordan 12% oshgan.",
            severity = ProblemSeverity.KRITIK,
            sector = "Ekologiya",
            mahalla = "Sanoat xabi 7-sektor",
            startDate = "Bugun, 07:30",
            orgName = "Ekologiya va atrof-muhit bo'limi",
            leaderName = "Boboyev Bobur",
            appealsCount = 14,
            status = ProblemStatus.OCHIQ,
            aiSuggestedTaskTitle = "Sanoat zonasi yo'llariga suv sepish va yashil belbog' nazoratini kuchaytirish",
            indicatorDropNote = "Ekologiya ko'rsatkichi -6% pasaygan",
            xRatio = 0.82f,
            yRatio = 0.18f,
            imageResId = R.drawable.img_command_hero_1795671436085,
            linkedTaskId = 4L
        ),
        CityProblem(
            id = "prb-4",
            title = "Markaziy bozor oldi avtobus bekatida tirbandlik",
            description = "Ertalabki tig'iz vaqtda 3-yo'nalish avtobuslari intervali 18 daqiqagacha cho'zilmoqda.",
            severity = ProblemSeverity.ORTA,
            sector = "Transport",
            mahalla = "Markaziy MFY",
            startDate = "3 kun oldin",
            orgName = "Shahar Transport Bo'limi",
            leaderName = "Karimov Jasur",
            appealsCount = 7,
            status = ProblemStatus.TOPSHIRIQ_BERILDI,
            aiSuggestedTaskTitle = "3-yo'nalishga qo'shimcha 2 ta avtobus qo'yish",
            indicatorDropNote = "Transport qulayligi 88%",
            xRatio = 0.50f,
            yRatio = 0.28f,
            imageResId = R.drawable.zarafshon_banner_1790585492174,
            linkedTaskId = 3L
        )
    )

    val initialCityFeed = listOf(
        CityObjectFeedItem(
            id = "feed-1",
            channelName = "Zarafshon Qurilish Nazorati",
            channelRole = "Shahar Arxitektura • Rasmiy Kanal",
            timestampText = "24 daqiqa oldin",
            title = "Yangi Zarafshon shoh ko'chasi va 14-turar-joy majmuasi",
            caption = "Asfalt yotqizish va LED ko'cha chiroqlarini o'rnatish ishlari yakunlanmoqda. 420 ta oila uchun zamonaviy infratuzilma tayyorlanmoqda.",
            location = "Yangi Zarafshon massivi",
            progressPercent = 78,
            verifiedCount = 142,
            statusTag = "JARAYONDA",
            statusColor = Color(0xFF2481CC),
            imageResId = R.drawable.img_city_construction_1795671434776,
            linkedTaskId = 2L
        ),
        CityObjectFeedItem(
            id = "feed-2",
            channelName = "Suv Ta'minoti va Kommunal 360°",
            channelRole = "Favqulodda Monitoring",
            timestampText = "1 soat oldin",
            title = "Markaziy suv inshooti va bosim monitoring stansiyasi",
            caption = "12-kichik nohiya hududida kechki bosim pasayishini bartaraf etish uchun zaxira nasosi sinovdan o'tkazilmoqda.",
            location = "12-kichik nohiya",
            progressPercent = 64,
            verifiedCount = 89,
            statusTag = "TEZKOR NAZORAT",
            statusColor = Color(0xFFEF4444),
            imageResId = R.drawable.img_water_infra_1795671435228,
            linkedTaskId = 1L
        ),
        CityObjectFeedItem(
            id = "feed-3",
            channelName = "Shahar 360° Raqamli Markaz",
            channelRole = "Hokimlik Matbuot va AI Tahlil",
            timestampText = "3 soat oldin",
            title = "Zarafshon tungi yoritish va aqlli svetoforlar integratsiyasi",
            caption = "Markaziy ko'chalarda energiya tejovchi aqlli sensorlar ishga tushirildi. Elektr sarfi 19% ga kamaydi.",
            location = "Navoiy shoh ko'chasi",
            progressPercent = 96,
            verifiedCount = 264,
            statusTag = "BAJARILDI",
            statusColor = Color(0xFF10B981),
            imageResId = R.drawable.img_command_hero_1795671436085,
            linkedTaskId = 3L
        )
    )

    val initialMahallas = listOf(
        MahallaProfile("m1", "Alisher Navoiy MFY", "S. Mirzayev", 6420, 95, 0, 28, 98),
        MahallaProfile("m2", "Markaziy MFY", "D. Normatova", 5810, 92, 1, 24, 94),
        MahallaProfile("m3", "Yangi Zarafshon MFY", "O. Raxmonov", 7150, 84, 2, 19, 86),
        MahallaProfile("m4", "12-kichik nohiya MFY", "Sh. To'rayev", 6890, 76, 3, 15, 79),
        MahallaProfile("m5", "Geologlar MFY", "B. Boboyev", 4930, 81, 2, 17, 83),
        MahallaProfile("m6", "Do'stlik MFY", "M. Yusupova", 5340, 90, 1, 22, 91)
    )

    val initialInvestments = listOf(
        InvestmentProject(
            id = "inv-1",
            title = "Aqlli Issiqxona va Agro-Logistika Markazi",
            sector = "Sanoat & Eksport",
            budgetUsdMillion = 12.5,
            progressPercent = 82,
            jobsPlanned = 180,
            contractor = "Zarafshon Gold Agro MCHJ",
            statusLabel = "Grafik bo'yicha",
            isDelayed = false,
            imageResId = R.drawable.img_city_construction_1795671434776
        ),
        InvestmentProject(
            id = "inv-2",
            title = "Ichimlik Suvini Qayta Tozalash Stansiyasi (2-bosqich)",
            sector = "Kommunal Investitsiya",
            budgetUsdMillion = 8.4,
            progressPercent = 65,
            jobsPlanned = 65,
            contractor = "Navoiy Hydro Build",
            statusLabel = "Tezlashtirish kerak",
            isDelayed = true,
            imageResId = R.drawable.img_water_infra_1795671435228
        ),
        InvestmentProject(
            id = "inv-3",
            title = "IT-Park va Yoshlar Texnoparki Binosi",
            sector = "Raqamli Iqtisodiyot",
            budgetUsdMillion = 4.2,
            progressPercent = 94,
            jobsPlanned = 240,
            contractor = "Digital City Construction",
            statusLabel = "Topshirish arafasida",
            isDelayed = false,
            imageResId = R.drawable.img_command_hero_1795671436085
        )
    )

    val initialDocuments = listOf(
        DocumentRecord(
            id = "doc-1",
            docNumber = "HQ-2026/114",
            title = "Zarafshon shahrida qishki mavsumga kommunal tayyorgarlik qarori",
            category = "Hokim Qarori",
            date = "24.11.2026",
            signedBy = "Shukurov A.K. (Shahar Hokimi)",
            previewSummary = "Barcha kichik nohiyalar va MFYlarda ichimlik suvi, issiqlik tarmoqlari hamda elektr podstansiyalarini 100% zaxira quvvati bilan ta'minlash to'g'risida.",
            status = "Imzolangan (ERI)"
        ),
        DocumentRecord(
            id = "doc-2",
            docNumber = "PR-2026/089",
            title = "Haftalik ijro intizomi va kechikkan topshiriqlar bayonnomasi",
            category = "Bayonnoma",
            date = "22.11.2026",
            signedBy = "Nazorat bo'limi",
            previewSummary = "2026-yil noyabr oyi 3-haftasi yakunlari bo'yicha 5 ta bo'linma KPI ko'rsatkichlari tahlili va intizomiy ogohlantirishlar.",
            status = "Tasdiqlangan"
        ),
        DocumentRecord(
            id = "doc-3",
            docNumber = "INV-2026/041",
            title = "2026-2027 Investitsiya dasturi choraklik ijro hisoboti",
            category = "Hisobot",
            date = "19.11.2026",
            signedBy = "Iqtisodiyot va investitsiya bo'limi",
            previewSummary = "Jami $25.1 mln qiymatidagi 3 ta strategik loyihaning o'zlashtirilish holati va yaratilgan yangi ish o'rinlari hisoboti.",
            status = "Ko'rib chiqilmoqda"
        )
    )

    val initialIndicators = listOf(
        IndicatorCatalogItem("ind-1", "Yangi ish o'rinlari", "Bandlik", "ta", "1,420", "1,600", "Rasmiy mehnat shartnomalari + Tadbirkorlik reyestri", "Bandlik bo'limi", true),
        IndicatorCatalogItem("ind-2", "Ichimlik suvi uzluksizligi", "Infratuzilma", "%", "91.4%", "98.0%", "(Normal bosimdagi soatlar / 24) * 100", "Zarafshon Suv Ta'minoti", false),
        IndicatorCatalogItem("ind-3", "Murojaatlar o'z vaqtida yopilishi", "Murojaatlar", "%", "79.2%", "95.0%", "(Muddatida yopilgan / Jami) * 100", "Xalq qabulxonasi", false),
        IndicatorCatalogItem("ind-4", "Sanoat mahsulotlari o'sishi", "Iqtisodiyot", "%", "+11.4%", "+10.0%", "Joriy davr / O'tgan yil mos davri", "Statistika bo'limi", true),
        IndicatorCatalogItem("ind-5", "Havo sifati indeksi (AQI)", "Ekologiya", "AQI", "62", "< 50", "PM2.5 va PM10 sensorlar o'rtacha qiymati", "Ekologiya inspeksiyasi", false)
    )

    val initialAiModules = listOf(
        AiControlModule("m1", "Deadline Radar", "Muddat buzilishi xavfini oldindan aniqlash", "2 ta xavf", 2, Color(0xFFEF4444)),
        AiControlModule("m2", "Review Monitor", "Nazoratchi tekshiruvining cho'zilish nazorati", "1 ta kechikish", 1, Color(0xFFF59E0B)),
        AiControlModule("m3", "Evidence AI", "Foto/GPS dalillar mosligi va anti-fraud", "98% toza", 0, Color(0xFF10B981)),
        AiControlModule("m4", "Conflict Detector", "Hisobotlar orasidagi ziddiyatlarni topish", "1 ta ziddiyat", 1, Color(0xFF8B5CF6)),
        AiControlModule("m5", "Workload Intelligence", "Xodimlar yuklamasining muvozanati", "Barqaror", 0, Color(0xFF2481CC)),
        AiControlModule("m6", "Department Analytics", "Bo'limlar samaradorligi va dinamikasi", "Real-vaqt", 0, Color(0xFF0EA5E9)),
        AiControlModule("m7", "Escalation Predictor", "Muammoning yuqori bosqichga chiqish ehtimoli", "1 ta signal", 1, Color(0xFFF97316)),
        AiControlModule("m8", "Executive Briefing", "Rahbar uchun 5 daqiqalik tahliliy xulosa", "Tayyor", 0, Color(0xFF1E3A8A))
    )

    val initialAiSignals = listOf(
        AiAnomalySignal(
            id = "sig-1",
            moduleName = "Review Monitor",
            title = "Nazoratda kechikish: Tekshiruv 5 soatdan oshdi",
            description = "Yangi Zarafshon 14-bino bo'yicha ijrochi hisobot yuborgan, lekin nazoratchi ko'rib chiqishi me'yoriy 4 soatlik vaqtdan oshmoqda.",
            forecastTag = "prognoz • Yuqori xavf",
            sourceLabel = "Manba: B365-2026-000102 • Review Queue",
            detectedTime = "Bugun, 14:20",
            confidencePercent = 94,
            severity = ProblemSeverity.KRITIK,
            sector = "Qurilish & Nazorat",
            recommendedAction = "Nazoratchi Saidov M. ga ko'rib chiqish eslatmasini yuborish"
        ),
        AiAnomalySignal(
            id = "sig-2",
            moduleName = "Deadline Radar",
            title = "Muddat buzilishi ehtimoli: 12-kichik nohiya",
            description = "Oraliq natijalar (2/4) va qolgan 18 soat vaqt o'rtasida nomutanosiblik aniqlandi. Suv bosimi murojaatlari +38% oshgan.",
            forecastTag = "prognoz • O'rta/Yuqori xavf",
            sourceLabel = "Manba: B365-2026-000101 • Shahar 360°",
            detectedTime = "Bugun, 13:45",
            confidencePercent = 91,
            severity = ProblemSeverity.YUQORI,
            sector = "Infratuzilma",
            recommendedAction = "Qo'shimcha brigada jalb etish bo'yicha topshiriq berish"
        ),
        AiAnomalySignal(
            id = "sig-3",
            moduleName = "Conflict Detector",
            title = "Hisobotlar ziddiyati: Texnika soatlari tafovuti",
            description = "Sanoat zonasi 7-sektor bo'yicha smeta hisoboti va GPS marshrut loglari orasida 12% tafovut qayd etildi. Qayta tekshirish zarur.",
            forecastTag = "prognoz • Tekshirish kerak",
            sourceLabel = "Manba: B365-2026-000104 • EXIF + Smeta",
            detectedTime = "Bugun, 11:10",
            confidencePercent = 88,
            severity = ProblemSeverity.ORTA,
            sector = "Ekologiya",
            recommendedAction = "Nazoratchi tomonidan joyida qayta dalil so'rash"
        )
    )

    val initialDiagonalTransfers = listOf(
        DiagonalTransfer(
            id = "diag-1",
            docCode = "DIAG-2026-041",
            type = DiagonalTransferType.HAMKORLIK,
            senderDept = "Obodonlashtirish boshqarmasi",
            senderLeader = "To'rayev Sh. M.",
            receiverDept = "Uy-joy kommunal xizmat (UJKX)",
            receiverLeader = "Haydarov O. T.",
            subject = "12-kichik nohiyada suv quvuri qazilgan joyni asfaltlash uchun quvur sinovini yakunlash",
            basisDoc = "Hokim farmoyishi №44 va B365-2026-000101",
            deadlineText = "Ertaga, 18:00 gacha",
            supervisorName = "Saidov Mirzo (Nazoratchi)",
            status = DiagonalTransferStatus.QABUL_QILINDI,
            responseNote = "Bosim sinovi bugun 17:00 da tugatiladi, asfaltlashga ruxsat akti beriladi.",
            aiMonitoringNote = "AI Nazorat: Qabul qilish o'z vaqtida (22 daq.), javob muddati nazoratda.",
            createdAtText = "Bugun, 09:15"
        ),
        DiagonalTransfer(
            id = "diag-2",
            docCode = "DIAG-2026-042",
            type = DiagonalTransferType.RASMIY_SOROV,
            senderDept = "Qurilish va arxitektura bo'limi",
            senderLeader = "Raxmonov B. S.",
            receiverDept = "Ekologiya va atrof-muhit",
            receiverLeader = "Sobirov N. J.",
            subject = "Yangi Zarafshon 14-bino atrofidagi yashil hudud bo'yicha ekologik xulosa taqdim etish",
            basisDoc = "Bosh reja loyihasi #INV-14",
            deadlineText = "Bugun, 17:00 gacha",
            supervisorName = "Normurodov I. E. (O'rinbosar)",
            status = DiagonalTransferStatus.KECHIKMOQDA,
            responseNote = null,
            aiMonitoringNote = "AI Nazorat: Ekologiya bo'limida ko'rib chiqish 3 soatga kechikdi.",
            createdAtText = "Kecha, 15:40"
        ),
        DiagonalTransfer(
            id = "diag-3",
            docCode = "DIAG-2026-039",
            type = DiagonalTransferType.MALUMOT_UZATISH,
            senderDept = "Iqtisodiyot va moliya bo'limi",
            senderLeader = "Mansurov Q. A.",
            receiverDept = "Yoshlar siyosati va sport bo'limi",
            receiverLeader = "Boboyev D. R.",
            subject = "IT va Kasbga o'rgatish markazi kompyuter xaridi smeta tasdig'i uzatildi",
            basisDoc = "PQ-403 qarori moliya rejasi",
            deadlineText = "Bajarilgan",
            supervisorName = "Saidov Mirzo (Nazoratchi)",
            status = DiagonalTransferStatus.BAJARILDI,
            responseNote = "Moliya smetasi qabul qilindi va tender e'lon qilindi.",
            aiMonitoringNote = "AI Nazorat: Uzilishsiz 100% muddatida yakunlandi.",
            createdAtText = "2 kun oldin"
        )
    )
}
