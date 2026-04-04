package com.dailycurator.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailycurator.ui.theme.NowRed

@Composable
fun NowIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "scale"
    )
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .scale(scale)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(NowRed)
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(NowRed)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text("NOW",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp))
        }
        Spacer(Modifier.width(6.dp))
        HorizontalDivider(color = NowRed.copy(alpha = 0.4f), thickness = 1.dp)
    }
}
