package com.aegis.demo.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// Modern SaaS Light Color Palette (Indigo / Slate Studio)
// Inspired by Linear, Stripe, and modern B2B/B2C SaaS products
// =============================================================================

// Primary SaaS Brand: Indigo 600 / Deep Indigo
val primaryLight = Color(0xFF4F46E5)              // Indigo 600
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFEEF2FF)     // Indigo 50
val onPrimaryContainerLight = Color(0xFF312E81)   // Indigo 900

// Secondary SaaS Neutral: Slate 900 / Slate 100
val secondaryLight = Color(0xFF0F172A)            // Slate 900
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFF1F5F9)   // Slate 100
val onSecondaryContainerLight = Color(0xFF0F172A) // Slate 900

// Tertiary Accent: Teal 600 (Analytics & Verified Trust)
val tertiaryLight = Color(0xFF0D9488)             // Teal 600
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFF0FDFA)    // Teal 50
val onTertiaryContainerLight = Color(0xFF134E4A)  // Teal 900

// Error & Violation: Red 600 / Red 50
val errorLight = Color(0xFFDC2626)                // Red 600
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFEF2F2)        // Red 50
val onErrorContainerLight = Color(0xFF991B1B)      // Red 800

// SaaS Background & Surfaces
val backgroundLight = Color(0xFFF8FAFC)           // Slate 50 (Crisp SaaS Studio Canvas)
val onBackgroundLight = Color(0xFF0F172A)         // Slate 900 (High-contrast text)
val surfaceLight = Color(0xFFFFFFFF)              // Pure White for cards & sheets
val onSurfaceLight = Color(0xFF0F172A)            // Slate 900
val surfaceVariantLight = Color(0xFFF1F5F9)       // Slate 100
val onSurfaceVariantLight = Color(0xFF475569)     // Slate 600 (Clean secondary text)

// SaaS Elevated Surfaces (Crisp White & Off-White)
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = Color(0xFFFFFFFF)
val surfaceContainerLight = Color(0xFFFFFFFF)
val surfaceContainerHighLight = Color(0xFFF8FAFC)
val surfaceContainerHighestLight = Color(0xFFF1F5F9)

// SaaS Borders & Dividers (Subtle 1dp Slate Outlines)
val outlineLight = Color(0xFF94A3B8)              // Slate 400
val outlineVariantLight = Color(0xFFE2E8F0)       // Slate 200 (Signature SaaS 1dp border)

// =============================================================================
// Dark Palette (Preserved for system fallback)
// =============================================================================
val primaryDark = Color(0xFF818CF8)
val onPrimaryDark = Color(0xFF1E1B4B)
val primaryContainerDark = Color(0xFF312E81)
val onPrimaryContainerDark = Color(0xFFEEF2FF)

val secondaryDark = Color(0xFFCBD5E1)
val onSecondaryDark = Color(0xFF0F172A)
val secondaryContainerDark = Color(0xFF1E293B)
val onSecondaryContainerDark = Color(0xFFF1F5F9)

val tertiaryDark = Color(0xFF5EEAD4)
val onTertiaryDark = Color(0xFF134E4A)
val tertiaryContainerDark = Color(0xFF042F2E)
val onTertiaryContainerDark = Color(0xFFCCFBF1)

val errorDark = Color(0xFFF87171)
val onErrorDark = Color(0xFF450A0A)
val errorContainerDark = Color(0xFF7F1D1D)
val onErrorContainerDark = Color(0xFFFEE2E2)

val backgroundDark = Color(0xFF090D16)
val onBackgroundDark = Color(0xFFF8FAFC)
val surfaceDark = Color(0xFF0F172A)
val onSurfaceDark = Color(0xFFF8FAFC)
val surfaceVariantDark = Color(0xFF1E293B)
val onSurfaceVariantDark = Color(0xFF94A3B8)

val surfaceContainerLowestDark = Color(0xFF060910)
val surfaceContainerLowDark = Color(0xFF0B1120)
val surfaceContainerDark = Color(0xFF0F172A)
val surfaceContainerHighDark = Color(0xFF1E293B)
val surfaceContainerHighestDark = Color(0xFF334155)

val outlineDark = Color(0xFF64748B)
val outlineVariantDark = Color(0xFF1E293B)

// =============================================================================
// Semantic Safety & Harassment Risk Palette (SaaS Trust & Safety Tokens)
// =============================================================================
object SafetyTheme {
    // Low / Healthy (Green 600 / Green 50)
    val lowRisk = Color(0xFF16A34A)
    val lowRiskContainer = Color(0xFFF0FDF4)
    val lowRiskBorder = Color(0xFFBBF7D0)
    val onLowRiskContainer = Color(0xFF14532D)

    // Review / Watch (Amber 600 / Amber 50)
    val reviewRisk = Color(0xFFD97706)
    val reviewRiskContainer = Color(0xFFFFFBEB)
    val reviewRiskBorder = Color(0xFFFDE68A)
    val onReviewRiskContainer = Color(0xFF78350F)

    // Medium / Friction (Orange 600 / Orange 50)
    val mediumRisk = Color(0xFFEA580C)
    val mediumRiskContainer = Color(0xFFFFF7ED)
    val mediumRiskBorder = Color(0xFFFED7AA)
    val onMediumRiskContainer = Color(0xFF7C2D12)

    // High / Shielding (Red 600 / Red 50)
    val highRisk = Color(0xFFDC2626)
    val highRiskContainer = Color(0xFFFEF2F2)
    val highRiskBorder = Color(0xFFFECACA)
    val onHighRiskContainer = Color(0xFF991B1B)

    // Critical / Quarantined (Rose 700 / Rose 50)
    val criticalRisk = Color(0xFFBE123C)
    val criticalRiskContainer = Color(0xFFFFF1F2)
    val criticalRiskBorder = Color(0xFFFECDD3)
    val onCriticalRiskContainer = Color(0xFF881337)

    // Baseline Keyword (C1) missed vs caught
    val keywordMissed = Color(0xFF64748B)
    val keywordMissedContainer = Color(0xFFF1F5F9)
    val engineCaught = Color(0xFF4F46E5)
    val engineCaughtContainer = Color(0xFFEEF2FF)
}
