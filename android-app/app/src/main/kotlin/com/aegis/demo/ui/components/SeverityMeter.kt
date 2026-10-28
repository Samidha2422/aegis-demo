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
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SeverityMeter(
    severity: Float,
    tier: String,
    modifier: Modifier = Modifier
) {
    val normalized = (severity / 100f).coerceIn(0f, 1f)
    val (meterColor, tierLabel, containerColor) = when {
        severity >= 70f || tier == "high" || tier == "urgent" -> Triple(
            MaterialTheme.colorScheme.error,
            tier.uppercase(),
            MaterialTheme.colorScheme.errorContainer
        )
        severity >= 30f || tier == "review" -> Triple(
            Color(0xFFE65100), // Vibrant Amber/Orange
            "REVIEW",
            Color(0xFFFFECB3)
        )
        else -> Triple(
            Color(0xFF2E7D32), // Green
            "LOW RISK",
            Color(0xFFE8F5E9)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor.copy(alpha = 0.3f))
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
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = tierLabel,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Aegis Risk Engine",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
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
                .height(10.dp),
            color = meterColor,
            trackColor = meterColor.copy(alpha = 0.2f),
            amplitude = { 1.2f },
            wavelength = 24.dp
        )
    }
}
