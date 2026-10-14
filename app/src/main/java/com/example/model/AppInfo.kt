package com.example.model

import androidx.compose.runtime.Immutable

@Immutable
data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val lastUpdateTime: Long = 0L,
    val normalizedLabel: String = normalize(label)
) {
    companion object {
        fun normalize(text: String): String {
            if (text.isEmpty()) return ""
            // Strips accents/diacritics and converts to lower case for ultra-fast, zero-alloc matching
            val decomposed = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
            val builder = StringBuilder(decomposed.length)
            for (ch in decomposed) {
                if (java.lang.Character.getType(ch) != java.lang.Character.NON_SPACING_MARK.toInt()) {
                    builder.append(ch.lowercaseChar())
                }
            }
            return builder.toString()
        }
    }
}
