package com.antialarm.ui.firing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antialarm.alarm.MathProblemGenerator
import com.antialarm.ui.theme.AccentGreen
import com.antialarm.ui.theme.AccentOrange
import com.antialarm.ui.theme.AccentRed
import com.antialarm.ui.theme.DarkCard
import com.antialarm.ui.theme.DarkSurface
import com.antialarm.ui.theme.TextMuted
import com.antialarm.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun MathChallengeScreen(
    difficulty: Int,
    onSolved: () -> Unit,
    onCancel: () -> Unit
) {
    var problem by remember { mutableStateOf(MathProblemGenerator.generate(difficulty)) }
    var answer by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var attempts by remember { mutableIntStateOf(0) }

    LaunchedEffect(showError) {
        if (showError) {
            delay(1500)
            showError = false
        }
    }

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(800)
            onSolved()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkSurface,
                        AccentOrange.copy(alpha = 0.08f),
                        DarkSurface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Calculate,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = AccentOrange
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Solve to Dismiss",
                style = MaterialTheme.typography.titleLarge,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Problem
            Text(
                text = problem.question,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Answer input
            OutlinedTextField(
                value = answer,
                onValueChange = {
                    answer = it.filter { c -> c.isDigit() || c == '-' }
                    showError = false
                },
                placeholder = { Text("Your answer", color = TextMuted) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (showError) AccentRed else AccentOrange,
                    unfocusedBorderColor = if (showError) AccentRed else DarkCard,
                    focusedContainerColor = DarkCard,
                    unfocusedContainerColor = DarkCard,
                    cursorColor = AccentOrange
                ),
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val userAnswer = answer.toIntOrNull()
                        if (userAnswer == problem.answer) {
                            showSuccess = true
                        } else {
                            showError = true
                            attempts++
                            answer = ""
                            problem = MathProblemGenerator.generate(difficulty)
                        }
                    }
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Error message
            AnimatedVisibility(
                visible = showError,
                enter = fadeIn() + scaleIn(tween(200)),
                exit = scaleOut(tween(200))
            ) {
                Text(
                    "❌ Wrong! Try again...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AccentRed,
                    fontWeight = FontWeight.Bold
                )
            }

            // Success message
            AnimatedVisibility(
                visible = showSuccess,
                enter = fadeIn() + scaleIn(tween(300))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = AccentGreen
                    )
                    Text(
                        "✓ Correct!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit button
            Button(
                onClick = {
                    val userAnswer = answer.toIntOrNull()
                    if (userAnswer == problem.answer) {
                        showSuccess = true
                    } else {
                        showError = true
                        attempts++
                        answer = ""
                        problem = MathProblemGenerator.generate(difficulty)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentOrange
                ),
                enabled = answer.isNotBlank() && !showSuccess
            ) {
                Text(
                    "Submit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (attempts > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Attempts: $attempts",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onCancel) {
                Text(
                    "← Back to alarm",
                    color = TextMuted
                )
            }
        }
    }
}
