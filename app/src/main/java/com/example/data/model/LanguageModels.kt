package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupportedLanguage(
    val id: String,
    val name: String,
    val nativeName: String,
    val flag: String,
    val category: String, // "Global" or "Indian"
    val popularity: Int, // 1-100 rating for display
    val accents: List<String>,
    val scripts: List<String>
)

object LanguageCatalog {
    val languages = listOf(
        // GLOBAL LANGUAGES
        SupportedLanguage("english", "English", "English", "US", "Global", 98, listOf("American", "British", "Australian", "Canadian", "Indian"), listOf("Latin")),
        SupportedLanguage("spanish", "Spanish", "Español", "ES", "Global", 95, listOf("Spain", "Mexico", "Argentina"), listOf("Latin")),
        SupportedLanguage("french", "French", "Français", "FR", "Global", 90, listOf("Standard French", "Canadian French"), listOf("Latin")),
        SupportedLanguage("german", "German", "Deutsch", "DE", "Global", 85, listOf("Germany", "Austria", "Switzerland"), listOf("Latin")),
        SupportedLanguage("italian", "Italian", "Italiano", "IT", "Global", 80, listOf("Standard Italian"), listOf("Latin")),
        SupportedLanguage("portuguese", "Portuguese", "Português", "PT", "Global", 82, listOf("Portugal", "Brazil"), listOf("Latin")),
        SupportedLanguage("russian", "Russian", "Русский", "RU", "Global", 75, listOf("Standard Russian"), listOf("Cyrillic")),
        SupportedLanguage("arabic", "Arabic", "العربية", "SA", "Global", 88, listOf("Gulf", "Egyptian", "Levantine"), listOf("Arabic Script")),
        SupportedLanguage("chinese_mandarin", "Chinese (Mandarin)", "普通话", "CN", "Global", 92, listOf("Mainland China", "Taiwan"), listOf("Simplified", "Traditional")),
        SupportedLanguage("cantonese", "Cantonese", "廣東話", "HK", "Global", 70, listOf("Hong Kong", "Guangzhou"), listOf("Traditional", "Simplified")),
        SupportedLanguage("japanese", "Japanese", "日本語", "JP", "Global", 89, listOf("Standard Japanese", "Kansai Dialect"), listOf("Japanese Script")),
        SupportedLanguage("korean", "Korean", "한국어", "KR", "Global", 87, listOf("Seoul Dialect", "Busan Dialect"), listOf("Hangul")),
        SupportedLanguage("turkish", "Turkish", "Türkçe", "TR", "Global", 72, listOf("Standard Turkish"), listOf("Latin")),
        SupportedLanguage("dutch", "Dutch", "Nederlands", "NL", "Global", 65, listOf("Netherlands", "Belgium"), listOf("Latin")),
        SupportedLanguage("swedish", "Swedish", "Svenska", "SE", "Global", 60, listOf("Sweden"), listOf("Latin")),
        SupportedLanguage("norwegian", "Norwegian", "Norsk", "NO", "Global", 55, listOf("Norway"), listOf("Latin")),
        SupportedLanguage("danish", "Danish", "Dansk", "DK", "Global", 55, listOf("Denmark"), listOf("Latin")),
        SupportedLanguage("finnish", "Finnish", "Suomi", "FI", "Global", 50, listOf("Finland"), listOf("Latin")),
        SupportedLanguage("polish", "Polish", "Polski", "PL", "Global", 68, listOf("Poland"), listOf("Latin")),
        SupportedLanguage("greek", "Greek", "Ελληνικά", "GR", "Global", 58, listOf("Greece"), listOf("Greek")),
        SupportedLanguage("hebrew", "Hebrew", "עברית", "IL", "Global", 60, listOf("Standard Hebrew"), listOf("Hebrew Script")),
        SupportedLanguage("thai", "Thai", "ไทย", "TH", "Global", 66, listOf("Standard Thai"), listOf("Thai Script")),
        SupportedLanguage("vietnamese", "Vietnamese", "Tiếng Việt", "VN", "Global", 74, listOf("Northern", "Southern"), listOf("Latin")),
        SupportedLanguage("indonesian", "Indonesian", "Bahasa Indonesia", "ID", "Global", 78, listOf("Standard Indonesian"), listOf("Latin")),
        SupportedLanguage("malay", "Malay", "Bahasa Melayu", "MY", "Global", 64, listOf("Standard Malay"), listOf("Latin")),
        SupportedLanguage("filipino", "Filipino/Tagalog", "Wikang Filipino", "PH", "Global", 70, listOf("Manila Accent"), listOf("Latin")),
        SupportedLanguage("ukrainian", "Ukrainian", "Українська", "UA", "Global", 65, listOf("Standard Ukrainian"), listOf("Cyrillic")),
        SupportedLanguage("romanian", "Romanian", "Română", "RO", "Global", 54, listOf("Romania"), listOf("Latin")),
        SupportedLanguage("hungarian", "Hungarian", "Magyar", "HU", "Global", 52, listOf("Hungary"), listOf("Latin")),
        SupportedLanguage("czech", "Czech", "Čeština", "CZ", "Global", 53, listOf("Czech Republic"), listOf("Latin")),
        SupportedLanguage("slovak", "Slovak", "Slovenčina", "SK", "Global", 50, listOf("Slovakia"), listOf("Latin")),
        SupportedLanguage("serbian", "Serbian", "Српски", "RS", "Global", 50, listOf("Serbia"), listOf("Cyrillic", "Latin")),
        SupportedLanguage("croatian", "Croatian", "Hrvatski", "HR", "Global", 50, listOf("Croatia"), listOf("Latin")),
        SupportedLanguage("bulgarian", "Bulgarian", "Български", "BG", "Global", 50, listOf("Bulgaria"), listOf("Cyrillic")),
        SupportedLanguage("persian", "Persian (Farsi)", "فارسی", "IR", "Global", 60, listOf("Tehran Accent"), listOf("Persian Script")),
        SupportedLanguage("swahili", "Swahili", "Kiswahili", "KE", "Global", 63, listOf("Standard Swahili"), listOf("Latin")),

        // INDIAN LANGUAGES
        SupportedLanguage("hindi", "Hindi", "हिन्दी", "IN", "Indian", 97, listOf("Standard Hindi", "Casual Delhi Hindi", "Mumbai Hindi", "Lucknow Style"), listOf("Devanagari", "Latin")),
        SupportedLanguage("hinglish", "Hinglish", "Hinglish (Hindi written in English)", "IN", "Indian", 96, listOf("Casual Hinglish", "Delhi Hinglish", "Mumbai Tapori Hinglish"), listOf("Latin")),
        SupportedLanguage("punjabi", "Punjabi", "ਪੰਜਾਬੀ", "IN", "Indian", 82, listOf("Standard Punjabi", "Casual Punjabi"), listOf("Gurmukhi", "Shahmukhi", "Latin")),
        SupportedLanguage("bengali", "Bengali", "বাংলা", "IN", "Indian", 85, listOf("Standard Bengali", "Kolkata Bengali"), listOf("Bengali Script", "Latin")),
        SupportedLanguage("marathi", "Marathi", "मराठी", "IN", "Indian", 84, listOf("Standard Marathi", "Mumbai Marathi"), listOf("Devanagari", "Latin")),
        SupportedLanguage("gujarati", "Gujarati", "ગુજરાતી", "IN", "Indian", 80, listOf("Standard Gujarati", "Ahmedabad Accent"), listOf("Gujarati Script", "Latin")),
        SupportedLanguage("tamil", "Tamil", "தமிழ்", "IN", "Indian", 83, listOf("Standard Tamil", "Chennai Tamil"), listOf("Tamil Script", "Latin")),
        SupportedLanguage("telugu", "Telugu", "తెలుగు", "IN", "Indian", 82, listOf("Standard Telugu", "Hyderabad Telugu"), listOf("Telugu Script", "Latin")),
        SupportedLanguage("kannada", "Kannada", "ಕನ್ನಡ", "IN", "Indian", 78, listOf("Standard Kannada", "Bengaluru Kannada"), listOf("Kannada Script", "Latin")),
        SupportedLanguage("malayalam", "Malayalam", "മലയാളം", "IN", "Indian", 76, listOf("Standard Malayalam"), listOf("Malayalam Script", "Latin")),
        SupportedLanguage("odia", "Odia", "ଓଡ଼ିଆ", "IN", "Indian", 68, listOf("Standard Odia"), listOf("Odia Script", "Latin")),
        SupportedLanguage("assamese", "Assamese", "অসমীয়া", "IN", "Indian", 65, listOf("Standard Assamese"), listOf("Assamese Script", "Latin")),
        SupportedLanguage("kashmiri", "Kashmiri", "کٲشُر / कॉशुर", "IN", "Indian", 58, listOf("Standard Kashmiri"), listOf("Arabic Script", "Devanagari", "Latin")),
        SupportedLanguage("konkani", "Konkani", "कोंकणी", "IN", "Indian", 55, listOf("Goan Konkani", "Mangalorean Konkani"), listOf("Devanagari", "Latin")),
        SupportedLanguage("sindhi", "Sindhi", "سنڌي / सिंधी", "IN", "Indian", 54, listOf("Standard Sindhi"), listOf("Arabic Script", "Devanagari")),
        SupportedLanguage("sanskrit", "Sanskrit", "संस्कृतम्", "IN", "Indian", 60, listOf("Standard Sanskrit"), listOf("Devanagari")),
        SupportedLanguage("urdu", "Urdu", "اردو", "IN", "Indian", 80, listOf("Standard Urdu", "Hyderabadi Urdu"), listOf("Nastaliq", "Latin")),
        SupportedLanguage("dogri", "Dogri", "डोगरी", "IN", "Indian", 48, listOf("Standard Dogri"), listOf("Devanagari")),
        SupportedLanguage("manipuri", "Manipuri", "মণিপুরী", "IN", "Indian", 46, listOf("Standard Manipuri"), listOf("Bengali Script", "Meitei Mayek")),
        SupportedLanguage("maithili", "Maithili", "मैथिली", "IN", "Indian", 50, listOf("Standard Maithili"), listOf("Devanagari")),
        SupportedLanguage("bodo", "Bodo", "बर'", "IN", "Indian", 44, listOf("Standard Bodo"), listOf("Devanagari")),
        SupportedLanguage("santali", "Santali", "ᱥᱟᱱᱛᱟᱲᱤ", "IN", "Indian", 45, listOf("Standard Santali"), listOf("Ol Chiki")),
        SupportedLanguage("tulu", "Tulu", "ತುಳು", "IN", "Indian", 48, listOf("Standard Tulu"), listOf("Kannada Script", "Latin")),
        SupportedLanguage("bhojpuri", "Bhojpuri", "भोजपुरी", "IN", "Indian", 75, listOf("Standard Bhojpuri", "Casual Bhojpuri"), listOf("Devanagari", "Latin")),
        SupportedLanguage("haryanvi", "Haryanvi", "हरियाणवी", "IN", "Indian", 72, listOf("Standard Haryanvi"), listOf("Devanagari", "Latin")),
        SupportedLanguage("rajasthani", "Rajasthani", "राजस्थानी", "IN", "Indian", 70, listOf("Standard Rajasthani"), listOf("Devanagari", "Latin")),
        SupportedLanguage("chhattisgarhi", "Chhattisgarhi", "छत्तीसगढ़ी", "IN", "Indian", 60, listOf("Standard Chhattisgarhi"), listOf("Devanagari")),
        SupportedLanguage("garhwali", "Garhwali", "गढ़वाली", "IN", "Indian", 52, listOf("Standard Garhwali"), listOf("Devanagari")),
        SupportedLanguage("kumaoni", "Kumaoni", "कुमाऊँनी", "IN", "Indian", 50, listOf("Standard Kumaoni"), listOf("Devanagari")),
        SupportedLanguage("bundeli", "Bundeli", "बुंदेली", "IN", "Indian", 48, listOf("Standard Bundeli"), listOf("Devanagari")),
        SupportedLanguage("awadhi", "Awadhi", "अवधी", "IN", "Indian", 55, listOf("Standard Awadhi"), listOf("Devanagari")),
        SupportedLanguage("magahi", "Magahi", "मगही", "IN", "Indian", 50, listOf("Standard Magahi"), listOf("Devanagari"))
    )

    fun search(query: String): List<SupportedLanguage> {
        if (query.isBlank()) return languages
        return languages.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.nativeName.contains(query, ignoreCase = true) ||
            it.category.contains(query, ignoreCase = true)
        }
    }
}
