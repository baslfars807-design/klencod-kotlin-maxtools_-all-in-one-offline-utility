package com.example.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val wordPairs: List<Pair<String, String>>
)

object OfflineTranslator {

    // Built-in offline vocabulary and phrase model (English -> Arabic)
    private val enToArDict: Map<String, String> = mapOf(
        // High frequency sentences and phrases
        "hello" to "مرحباً",
        "hi" to "أهلاً",
        "welcome" to "أهلاً وسهلاً",
        "good morning" to "صباح الخير",
        "good evening" to "مساء الخير",
        "good night" to "تصبح على خير",
        "how are you" to "كيف حالك",
        "i am fine" to "أنا بخير",
        "thank you" to "شكراً لك",
        "thanks" to "شكراً",
        "please" to "من فضلك",
        "yes" to "نعم",
        "no" to "لا",
        "okay" to "حسناً",
        "ok" to "تمام",
        "sorry" to "آسف",
        "excuse me" to "عذراً",
        "goodbye" to "مع السلامة",
        "see you later" to "أراك لاحقاً",

        // Tech, Files & Mobile Terms
        "file" to "ملف",
        "files" to "ملفات",
        "folder" to "مجلد",
        "document" to "مستند",
        "documents" to "مستندات",
        "image" to "صورة",
        "images" to "صور",
        "photo" to "صورة",
        "photos" to "صور",
        "video" to "فيديو",
        "videos" to "فيديوهات",
        "audio" to "صوت",
        "sound" to "صوت",
        "music" to "موسيقى",
        "application" to "تطبيق",
        "app" to "تطبيق",
        "apps" to "تطبيقات",
        "storage" to "التخزين",
        "memory" to "الذاكرة",
        "settings" to "الإعدادات",
        "share" to "مشاركة",
        "send" to "إرسال",
        "receive" to "استقبال",
        "download" to "تنزيل",
        "upload" to "رفع",
        "convert" to "تحويل",
        "converter" to "محول",
        "filter" to "فلتر",
        "filters" to "فلاتر",
        "speed" to "السرعة",
        "player" to "مشغل",
        "play" to "تشغيل",
        "pause" to "إيقاف مؤقت",
        "stop" to "إيقاف",
        "scanner" to "ماسح",
        "scan" to "مسح",
        "generate" to "إنشاء",
        "create" to "إنشاء",
        "save" to "حفظ",
        "saved" to "تم الحفظ",
        "delete" to "حذف",
        "deleted" to "تم الحذف",
        "trash" to "سلة المهملات",
        "restore" to "استعادة",
        "favorite" to "مفضل",
        "favorites" to "المفضلة",
        "search" to "بحث",
        "history" to "السجل",
        "offline" to "دون إنترنت",
        "online" to "متصل",
        "network" to "شبكة",
        "camera" to "كاميرا",
        "quality" to "الجودة",
        "size" to "الحجم",
        "date" to "التاريخ",
        "name" to "الاسم",
        "tools" to "أدوات",
        "text" to "نص",
        "translate" to "ترجمة",
        "translator" to "مترجم",
        "recognition" to "تعرف",
        "success" to "نجاح",
        "failed" to "فشل",
        "error" to "خطأ",
        "done" to "تم",
        "cancel" to "إلغاء",
        "confirm" to "تأكيد",
        "details" to "تفاصيل",
        "info" to "معلومات",
        "close" to "إغلاق",
        "open" to "فتح",
        "select" to "تحديد",
        "all" to "الكل",
        "copy" to "نسخ",
        "copied" to "تم النسخ",

        // General nouns, verbs and adjectives
        "book" to "كتاب",
        "car" to "سيارة",
        "home" to "المنزل",
        "house" to "بيت",
        "work" to "عمل",
        "job" to "وظيفة",
        "school" to "مدرسة",
        "university" to "جامعة",
        "student" to "طالب",
        "teacher" to "معلم",
        "city" to "مدينة",
        "country" to "دولة",
        "world" to "العالم",
        "today" to "اليوم",
        "tomorrow" to "غداً",
        "yesterday" to "أمس",
        "now" to "الآن",
        "time" to "الوقت",
        "day" to "يوم",
        "night" to "ليل",
        "sun" to "شمس",
        "moon" to "قمر",
        "light" to "ضوء",
        "dark" to "ظلام",
        "big" to "كبير",
        "small" to "صغير",
        "new" to "جديد",
        "old" to "قديم",
        "good" to "جيد",
        "bad" to "سيئ",
        "fast" to "سريع",
        "slow" to "بطيء",
        "easy" to "سهل",
        "hard" to "صعب",
        "beautiful" to "جميل",
        "important" to "مهم",
        "free" to "مجاني",
        "clear" to "واضح",
        "true" to "صحيح",
        "false" to "خطأ",
        "one" to "واحد",
        "two" to "اثنان",
        "three" to "ثلاثة",
        "four" to "أربعة",
        "five" to "خمسة",
        "this" to "هذا",
        "that" to "ذلك",
        "here" to "هنا",
        "there" to "هناك",
        "where" to "أين",
        "what" to "ماذا",
        "why" to "لماذا",
        "when" to "متى",
        "who" to "من",
        "how" to "كيف"
    )

    // Reverse dictionary (Arabic -> English)
    private val arToEnDict: Map<String, String> by lazy {
        val map = mutableMapOf<String, String>()
        for ((k, v) in enToArDict) {
            map[v] = k
        }
        // Additional common Arabic stems
        map["مرحبا"] = "hello"
        map["اهلا"] = "welcome"
        map["شكرا"] = "thank you"
        map["ملف"] = "file"
        map["صور"] = "photos"
        map["صورة"] = "photo"
        map["فيديو"] = "video"
        map["موسيقى"] = "music"
        map["تطبيق"] = "application"
        map["حفظ"] = "save"
        map["حذف"] = "delete"
        map["نسخ"] = "copy"
        map["تحويل"] = "convert"
        map["مشاركة"] = "share"
        map["بحث"] = "search"
        map["سريع"] = "fast"
        map["جديد"] = "new"
        map["الكل"] = "all"
        map["نعم"] = "yes"
        map["لا"] = "no"
        map
    }

    suspend fun translate(text: String, toArabic: Boolean = true): TranslationResult = withContext(Dispatchers.Default) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return@withContext TranslationResult(
                originalText = "",
                translatedText = "",
                sourceLang = if (toArabic) "English" else "Arabic",
                targetLang = if (toArabic) "Arabic" else "English",
                wordPairs = emptyList()
            )
        }

        val sourceDict = if (toArabic) enToArDict else arToEnDict
        val lines = trimmed.split("\n")
        val translatedLines = mutableListOf<String>()
        val pairs = mutableListOf<Pair<String, String>>()

        for (line in lines) {
            // First check if the entire line is a known phrase
            val cleanLine = line.trim().lowercase()
            if (sourceDict.containsKey(cleanLine)) {
                val trans = sourceDict[cleanLine]!!
                translatedLines.add(trans)
                pairs.add(line to trans)
            } else {
                // Word by word translation preserving punctuation
                val tokens = line.split(" ")
                val translatedTokens = mutableListOf<String>()
                for (token in tokens) {
                    val cleanToken = token.lowercase().replace("[^a-zA-Z0-9ء-ي]".toRegex(), "")
                    val trans = sourceDict[cleanToken]
                    if (trans != null) {
                        translatedTokens.add(trans)
                        pairs.add(token to trans)
                    } else {
                        translatedTokens.add(token)
                    }
                }
                translatedLines.add(translatedTokens.joinToString(" "))
            }
        }

        val resultText = translatedLines.joinToString("\n")
        TranslationResult(
            originalText = text,
            translatedText = resultText,
            sourceLang = if (toArabic) "English" else "Arabic",
            targetLang = if (toArabic) "Arabic" else "English",
            wordPairs = pairs
        )
    }
}
