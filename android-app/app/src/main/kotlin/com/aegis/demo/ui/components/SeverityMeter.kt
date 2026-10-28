package com.aegis.demo.ui.components

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SeverityMeter(
    severity: Float,
    tier: String,
    modifier: Modifier = Modifier
) {
    val normalized = (severity / 100f).coerceIn(0f, 1f)
    val (meterColor, tierLabel, containerColor) = when {
        severity >= 85f || tier == "urgent" || tier == "critical" -> Triple(
            SafetyTheme.criticalRisk,
            "CRITICAL",
            SafetyTheme.criticalRiskContainer
        )
        severity >= 70f || tier == "high" -> Triple(
            SafetyTheme.highRisk,
            "HIGH RISK",
            SafetyTheme.highRiskContainer
        )
        severity >= 30f || tier == "review" -> Triple(
            SafetyTheme.reviewRisk,
            "REVIEW",
            SafetyTheme.reviewRiskContainer
        )
        else -> Triple(
            SafetyTheme.lowRisk,
            "LOW RISK",
            SafetyTheme.lowRiskContainer
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor.copy(alpha = 0.4f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    text = "Behavioral Risk Score",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "${severity.toInt()}/100",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = meterColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearWavyProgressIndicator(
            progress = { normalized },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = meterColor,
            trackColor = meterColor.copy(alpha = 0.2f),
            amplitude = { 1.0f },
            wavelength = 20.dp
        )
    }
}
