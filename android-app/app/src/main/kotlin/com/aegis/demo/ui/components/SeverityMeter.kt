package com.aegis.demo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegis.demo.ui.theme.SafetyTheme

@Composable
fun SeverityMeter(
    severity: Float,
    tier: String,
    modifier: Modifier = Modifier
) {
    val (meterColor, tierLabel, containerColor, borderColor) = when {
        severity >= 85f || tier == "urgent" || tier == "critical" -> Quad(
            SafetyTheme.criticalRisk,
            "CRITICAL",
            SafetyTheme.criticalRiskContainer,
            SafetyTheme.criticalRiskBorder
        )
        severity >= 70f || tier == "high" -> Quad(
            SafetyTheme.highRisk,
            "HIGH RISK",
            SafetyTheme.highRiskContainer,
            SafetyTheme.highRiskBorder
        )
        severity >= 30f || tier == "review" -> Quad(
            SafetyTheme.reviewRisk,
            "REVIEW",
            SafetyTheme.reviewRiskContainer,
            SafetyTheme.reviewRiskBorder
        )
        else -> Quad(
            SafetyTheme.lowRisk,
            "LOW RISK",
            SafetyTheme.lowRiskContainer,
            SafetyTheme.lowRiskBorder
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor.copy(alpha = 0.5f))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Surface(
                    color = meterColor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = tierLabel,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Risk Score",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${severity.toInt()}/100",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = meterColor,
                maxLines = 1,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5-Segment Graduated SaaS Risk Gauge (replaces wavy pattern)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val segments = listOf(
                Pair(0f..25f, SafetyTheme.lowRisk),
                Pair(25f..50f, Color(0xFF0D9488)), // Guard / Teal
                Pair(50f..70f, SafetyTheme.reviewRisk), // Amber
                Pair(70f..85f, SafetyTheme.mediumRisk), // Orange
                Pair(85f..100f, SafetyTheme.criticalRisk) // Red
            )

            segments.forEach { (range, segColor) ->
                val isFilled = severity >= range.start
                val isCurrentTier = severity >= range.start && severity <= range.endInclusive
                val alpha = if (severity >= range.endInclusive) 1f else if (isCurrentTier) 0.85f else 0.15f

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isFilled && severity > 0f) segColor.copy(alpha = alpha)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Threshold Scale Markers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text("25", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text("50", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text("75", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text("100", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
