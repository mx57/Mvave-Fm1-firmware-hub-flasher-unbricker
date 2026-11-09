package com.example.ui

enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
    EN("en", "English", "🇬🇧"),
    RU("ru", "Русский", "🇷🇺");

    fun toggled(): AppLanguage = if (this == EN) RU else EN
}
