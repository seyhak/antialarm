package com.antialarm.ui.firing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antialarm.ui.theme.AccentRed
import com.antialarm.ui.theme.DarkSurface
import com.antialarm.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun AlarmFiringScreen(
        alarmLabel: String,
        alarmHour: Int,
        alarmMinute: Int,
        mathDifficulty: Int,
        snoozeMinutes: Int,
        onDismiss: () -> Unit,
        onSnooze: () -> Unit
) {
    var showMathChallenge by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            currentTime = sdf.format(Date())
            delay(1000)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by
            infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.15f,
                    animationSpec =
                            infiniteRepeatable(
                                    animation = tween(800),
                                    repeatMode = RepeatMode.Reverse
                            ),
                    label = "pulseScale"
            )

    AnimatedContent(
            targetState = showMathChallenge,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            label = "firingContent"
    ) { showMath ->
        if (showMath) {
            MathChallengeScreen(
                    difficulty = mathDifficulty,
                    onSolved = onDismiss,
                    onCancel = { showMathChallenge = false }
            )
        } else {
            Box(
                    modifier =
                            Modifier.fillMaxSize()
                                    .background(
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    DarkSurface,
                                                                    AccentRed.copy(alpha = 0.15f),
                                                                    DarkSurface
                                                            )
                                            )
                                    )
            ) {
                Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                ) {
                    // Pulsating alarm icon
                    Box(
                            modifier =
                                    Modifier.size(120.dp)
                                            .scale(pulseScale)
                                            .background(
                                                    MaterialTheme.colorScheme.primary.copy(
                                                            alpha = 0.15f
                                                    ),
                                                    CircleShape
                                            ),
                            contentAlignment = Alignment.Center
                    ) {
                        Icon(
                                Icons.Default.Alarm,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Current time
                    Text(
                            text = currentTime,
                            style =
                                    MaterialTheme.typography.displayLarge.copy(
                                            fontSize = 64.sp,
                                            fontWeight = FontWeight.Bold
                                    ),
                            color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                            text = alarmLabel.ifBlank { "Alarm" },
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                            text = "Set for ${String.format("%02d:%02d", alarmHour, alarmMinute)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(64.dp))

                    // Dismiss button
                    Button(
                            onClick = {
                                if (mathDifficulty > 0) {
                                    showMathChallenge = true
                                } else {
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors =
                                    ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                    )
                    ) {
                        Text(
                                "Dismiss",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Snooze button
                    OutlinedButton(
                            onClick = onSnooze,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors =
                                    ButtonDefaults.outlinedButtonColors(
                                            contentColor = TextSecondary
                                    )
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                                "Snooze ($snoozeMinutes min)",
                                style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}
