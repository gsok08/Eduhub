package com.example.eduhub20.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class Particle(
    val initialX: Float,
    val initialY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val size: Float,
    val color: Color,
    val rotationSpeed: Float,
    val isCircle: Boolean
)

@Composable
fun ConfettiEffect(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {}
) {
    if (!visible) return

    val progress = remember { Animatable(0f) }

    val colors = listOf(
        Color(0xFFE07A5F), // Coral
        Color(0xFF3B82F6), // Blue
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899)  // Pink
    )

    val particles = remember {
        List(85) {
            Particle(
                initialX = Random.nextFloat(),
                initialY = Random.nextFloat() * 0.3f, // burst near top
                velocityX = (Random.nextFloat() - 0.5f) * 1.2f,
                velocityY = Random.nextFloat() * 0.8f + 0.4f,
                size = Random.nextFloat() * 14f + 8f,
                color = colors.random(),
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f,
                isCircle = Random.nextBoolean()
            )
        }
    }

    LaunchedEffect(visible) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2600, easing = LinearEasing)
        )
        onFinished()
    }

    val t = progress.value
    if (t in 0.01f..0.99f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            particles.forEach { p ->
                val currentX = (p.initialX + p.velocityX * t) * canvasWidth
                val currentY = (p.initialY + p.velocityY * t + 0.5f * 9.8f * t * t * 0.3f) * canvasHeight
                val alpha = (1f - (t * 1.1f)).coerceIn(0f, 1f)
                val rotation = p.rotationSpeed * t

                if (currentX in 0f..canvasWidth && currentY in 0f..canvasHeight && alpha > 0f) {
                    rotate(degrees = rotation, pivot = Offset(currentX, currentY)) {
                        if (p.isCircle) {
                            drawCircle(
                                color = p.color.copy(alpha = alpha),
                                radius = p.size / 2f,
                                center = Offset(currentX, currentY)
                            )
                        } else {
                            drawRect(
                                color = p.color.copy(alpha = alpha),
                                topLeft = Offset(currentX - p.size / 2f, currentY - p.size / 4f),
                                size = Size(p.size, p.size * 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}
