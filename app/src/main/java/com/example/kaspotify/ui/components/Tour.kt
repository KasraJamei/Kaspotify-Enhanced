package com.example.kaspotify.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** UI anchors the coach-mark tour can spotlight. Composables register their bounds via [tourTarget]. */
enum class TourTarget { TABS, SORT, NAV, SETTINGS, MINIPLAYER }

/** One step of the tour. A null [target] shows a centered caption with no spotlight. */
data class TourStep(val target: TourTarget?, val title: String, val body: String)

private val TOUR_STEPS = listOf(
    TourStep(
        TourTarget.TABS,
        "Swipe your library",
        "Swipe left and right across these tabs to move between Songs, Albums, Artists and Favorites."
    ),
    TourStep(
        TourTarget.SORT,
        "Sort & filter",
        "Reorder a list — A to Z, newest, oldest or longest — and jump down it with the A–Z rail."
    ),
    TourStep(
        TourTarget.SETTINGS,
        "Settings",
        "Turn features on or off, and replay this tour any time from here."
    ),
    TourStep(
        TourTarget.NAV,
        "Move around",
        "Tap to jump between Home, Search and Playlists — or just swipe the whole page sideways."
    ),
    TourStep(
        TourTarget.MINIPLAYER,
        "Now Playing",
        "Play a song, then tap the mini-player to open it. Double-tap the artwork to like a track."
    )
)

/** Holds the running tour: its current step and the measured bounds of each registered target. */
@Stable
class TourState {
    var step by mutableIntStateOf(-1)
        private set
    val bounds = mutableStateMapOf<TourTarget, Rect>()
    val steps: List<TourStep> = TOUR_STEPS

    val active: Boolean get() = step in steps.indices
    fun start() { step = 0 }
    fun next() { if (step < steps.lastIndex) step++ }
    fun stop() { step = -1 }
}

val LocalTour = staticCompositionLocalOf<TourState?> { null }

/** Reports this element's on-screen bounds to the active [TourState] so the tour can spotlight it. */
@Composable
fun Modifier.tourTarget(target: TourTarget): Modifier {
    val tour = LocalTour.current ?: return this
    return this.onGloballyPositioned { tour.bounds[target] = it.boundsInRoot() }
}

/**
 * Full-screen coach-mark overlay. Dims everything, punches a rounded spotlight over the current
 * step's target (if its bounds are known), and floats a caption with Skip / Next controls. Tapping
 * the dimmed area also advances. [onFinish] runs on Skip or after the last step.
 */
@Composable
fun TourOverlay(tour: TourState, onFinish: () -> Unit) {
    if (!tour.active) return
    val step = tour.steps[tour.step]
    val rect = step.target?.let { tour.bounds[it] }
    val isLast = tour.step >= tour.steps.lastIndex
    val advance: () -> Unit = { if (isLast) onFinish() else tour.next() }

    val primary = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(tour.step) { detectTapGestures { advance() } }
    ) {
        val heightPx = with(density) { maxHeight.toPx() }
        val pad = with(density) { 8.dp.toPx() }
        val radius = with(density) { 16.dp.toPx() }
        val stroke = with(density) { 2.dp.toPx() }

        // Scrim + spotlight cutout. graphicsLayer forces an offscreen layer so BlendMode.Clear can
        // actually punch a transparent hole through the dim.
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.99f)) {
            drawRect(Color.Black.copy(alpha = 0.80f))
            if (rect != null) {
                val tl = Offset(rect.left - pad, rect.top - pad)
                val sz = Size(rect.width + pad * 2, rect.height + pad * 2)
                drawRoundRect(Color.Black, tl, sz, CornerRadius(radius, radius), blendMode = BlendMode.Clear)
                drawRoundRect(primary, tl, sz, CornerRadius(radius, radius), style = Stroke(stroke))
            }
        }

        // Caption sits on the opposite half from the spotlight so they never overlap.
        val captionAtBottom = rect == null || rect.center.y < heightPx * 0.55f
        TourCaption(
            step = step,
            index = tour.step,
            total = tour.steps.size,
            isLast = isLast,
            onSkip = onFinish,
            onNext = advance,
            modifier = Modifier
                .align(if (captionAtBottom) Alignment.BottomCenter else Alignment.TopCenter)
                .padding(horizontal = 24.dp, vertical = 96.dp)
        )
    }
}

@Composable
private fun TourCaption(
    step: TourStep,
    index: Int,
    total: Int,
    isLast: Boolean,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), shape)
            // Consume taps so tapping the card (vs. the dimmed area) doesn't advance the tour.
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(20.dp)
    ) {
        Text(step.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.padding(top = 6.dp))
        Text(
            step.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.padding(top = 16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${index + 1} / $total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSkip) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onNext) {
                    Text(if (isLast) "Done" else "Next")
                }
            }
        }
    }
}
