package com.apptolast.greenhousefronts.presentation.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.apptolast.greenhousefronts.data.remote.websocket.GreenhouseStatusWebSocket
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

private val HeartbeatRed = Color(0xFFFF1744)
private val HeartbeatRedSoft = Color(0xFFFF5252)

private val BorderThicknessMin = 5.dp
private val BorderThicknessMax = 10.dp

/**
 * Pulsing red glow framing the screen whenever the user has at least one unresolved
 * CRITICAL alert. Subscribes to the shared WebSocket flow (zero extra connections;
 * emissions are gated on AuthState so on Splash/Login this stays silent). Single mount
 * point at the App root.
 *
 * The intensity follows a slow, calm lub‑dub cadence (~45 bpm) driven by chained
 * `spring()` animations; both the alpha and the border thickness (5 → 10 dp) are
 * modulated so the frame visibly "breathes" with each beat. Each side of the screen is
 * drawn as a linear gradient fading from the outer edge (opaque red) inward to
 * transparent, leaving the rest of the UI untouched.
 */
@Composable
fun CriticalAlertHeartbeat(modifier: Modifier = Modifier) {
    val webSocket: GreenhouseStatusWebSocket = koinInject()
    val status by webSocket.statusFlow().collectAsState(initial = null)

    val hasCritical = status?.tenants.orEmpty().any { tenant ->
        tenant.greenhouses.any { gh ->
            gh.sectors.any { sector ->
                sector.alerts.any { alert ->
                    !alert.isResolved && alert.severity?.name?.equals("CRITICAL", true) == true
                }
            }
        }
    }
    if (!hasCritical) return

    val intensity = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            // S1 — "lub": measured contraction
            intensity.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            // partial relaxation between beats
            intensity.animateTo(
                targetValue = 0.30f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
            // S2 — "dub": second, softer contraction
            intensity.animateTo(
                targetValue = 0.80f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            // long diastolic decay
            intensity.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessVeryLow,
                ),
            )
            // rest before the next beat (~45 bpm overall)
            delay(900)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val a = intensity.value
                if (a <= 0f) return@drawBehind

                val minPx = BorderThicknessMin.toPx()
                val maxPx = BorderThicknessMax.toPx()
                val thickness = minPx + (maxPx - minPx) * a

                val edge = HeartbeatRed.copy(alpha = (a * 0.95f).coerceIn(0f, 1f))
                val mid = HeartbeatRedSoft.copy(alpha = (a * 0.45f).coerceIn(0f, 1f))
                val inner = Color.Transparent

                // Top
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to edge,
                        0.55f to mid,
                        1f to inner,
                        startY = 0f,
                        endY = thickness,
                    ),
                    topLeft = Offset.Zero,
                    size = Size(size.width, thickness),
                )
                // Bottom
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to inner,
                        0.45f to mid,
                        1f to edge,
                        startY = size.height - thickness,
                        endY = size.height,
                    ),
                    topLeft = Offset(0f, size.height - thickness),
                    size = Size(size.width, thickness),
                )
                // Left
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to edge,
                        0.55f to mid,
                        1f to inner,
                        startX = 0f,
                        endX = thickness,
                    ),
                    topLeft = Offset.Zero,
                    size = Size(thickness, size.height),
                )
                // Right
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to inner,
                        0.45f to mid,
                        1f to edge,
                        startX = size.width - thickness,
                        endX = size.width,
                    ),
                    topLeft = Offset(size.width - thickness, 0f),
                    size = Size(thickness, size.height),
                )
            },
    )
}
