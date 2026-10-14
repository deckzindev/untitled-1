package com.example.model

import androidx.compose.runtime.Immutable

@Immutable
data class IconPackInfo(
    val packageName: String?,
    val label: String,
    val isSelected: Boolean
)
