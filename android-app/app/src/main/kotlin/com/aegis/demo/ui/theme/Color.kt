package com.aegis.demo.ui.theme

import androidx.compose.ui.graphics.Color

// Light Colors
val primaryLight = Color(0xFF00506E)
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFF23759B)
val onPrimaryContainerLight = Color(0xFFFFFFFF)

val secondaryLight = Color(0xFF486271)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFCFEAFD)
val onSecondaryContainerLight = Color(0xFF021E2C)

val tertiaryLight = Color(0xFF613A73)
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFF885E9A)
val onTertiaryContainerLight = Color(0xFFFFFFFF)

val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF410002)

val backgroundLight = Color(0xFFF7F9FD)
val onBackgroundLight = Color(0xFF191C1F)
val surfaceLight = Color(0xFFF7F9FD)
val onSurfaceLight = Color(0xFF191C1F)
val surfaceVariantLight = Color(0xFFDBE4EB)
val onSurfaceVariantLight = Color(0xFF40484E)

val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = Color(0xFFF1F4F8)
val surfaceContainerLight = Color(0xFFEBEEF2)
val surfaceContainerHighLight = Color(0xFFE5E8EC)
val surfaceContainerHighestLight = Color(0xFFDFE3E7)

val outlineLight = Color(0xFF70787E)
val outlineVariantLight = Color(0xFFBFC8CF)

// Dark Colors
val primaryDark = Color(0xFF87CFF9)
val onPrimaryDark = Color(0xFF00344A)
val primaryContainerDark = Color(0xFF004C69)
val onPrimaryContainerDark = Color(0xFFC7E7FF)

val secondaryDark = Color(0xFFB0CADC)
val onSecondaryDark = Color(0xFF1B3341)
val secondaryContainerDark = Color(0xFF314A58)
val onSecondaryContainerDark = Color(0xFFCFEAFD)

val tertiaryDark = Color(0xFFE6B5F8)
val onTertiaryDark = Color(0xFF471F5A)
val tertiaryContainerDark = Color(0xFF5F3772)
val onTertiaryContainerDark = Color(0xFFF8D8FF)

val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)

val backgroundDark = Color(0xFF111417)
val onBackgroundDark = Color(0xFFE0E3E7)
val surfaceDark = Color(0xFF111417)
val onSurfaceDark = Color(0xFFE0E3E7)
val surfaceVariantDark = Color(0xFF40484E)
val onSurfaceVariantDark = Color(0xFFBFC8CF)

val surfaceContainerLowestDark = Color(0xFF0C0F12)
val surfaceContainerLowDark = Color(0xFF191C1F)
val surfaceContainerDark = Color(0xFF1D2024)
val surfaceContainerHighDark = Color(0xFF272A2E)
val surfaceContainerHighestDark = Color(0xFF323539)

val outlineDark = Color(0xFF8A9298)
val outlineVariantDark = Color(0xFF40484E)

// Semantic Safety & Harassment Risk Palette
object SafetyTheme {
    // Low / Healthy
    val lowRisk = Color(0xFF1E7E34)
    val lowRiskContainer = Color(0xFFE8F5E9)
    val onLowRiskContainer = Color(0xFF0F401B)

    // Review / Watch
    val reviewRisk = Color(0xFFC26E00)
    val reviewRiskContainer = Color(0xFFFFF3E0)
    val onReviewRiskContainer = Color(0xFF663800)

    // Medium / Friction
    val mediumRisk = Color(0xFFD35400)
    val mediumRiskContainer = Color(0xFFFFE0B2)
    val onMediumRiskContainer = Color(0xFF6E2800)

    // High / Shielding
    val highRisk = Color(0xFFC0392B)
    val highRiskContainer = Color(0xFFFFEBEE)
    val onHighRiskContainer = Color(0xFF641E16)

    // Critical / Quarantined
    val criticalRisk = Color(0xFF880E4F)
    val criticalRiskContainer = Color(0xFFFCE4EC)
    val onCriticalRiskContainer = Color(0xFF4A0028)

    // Baseline Keyword (C1) missed vs caught
    val keywordMissed = Color(0xFF616161)
    val keywordMissedContainer = Color(0xFFEEEEEE)
    val engineCaught = Color(0xFF0D47A1)
    val engineCaughtContainer = Color(0xFFE3F2FD)
}
