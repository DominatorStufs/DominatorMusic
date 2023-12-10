package it.vfsfitvnm.vimusic.ui.modifiers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.spring
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.vimusic.utils.px
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration

class SwipeState internal constructor() {
    private companion object {
        private val animatables = mutableListOf<Animatable<Float, AnimationVector1D>>()
        private val coroutineScope = CoroutineScope(Dispatchers.IO)

        fun acquire() = animatables.removeFirstOrNull() ?: Animatable(0f)
        fun recycle(animatable: Animatable<Float, AnimationVector1D>) {
            coroutineScope.launch {
                animatable.snapTo(0f)
                animatables += animatable
            }
        }
    }

    internal val offset = acquire()

    @Composable
    fun calculateOffset(bounds: ClosedRange<Dp>? = null) =
        offset.value.px.dp.let { if (bounds != null) it.coerceIn(bounds) else it }

    internal fun recycle() = recycle(offset)
}

@Composable
fun rememberSwipeState(key: Any?) = remember(key) { SwipeState() }

fun Modifier.onSwipe(
    state: SwipeState? = null,
    animateOffset: Boolean = false,
    orientation: Orientation = Orientation.Horizontal,
    delay: Duration = Duration.ZERO,
    decay: Density.() -> DecayAnimationSpec<Float> = { splineBasedDecay(this) },
    animationSpec: AnimationSpec<Float> = spring(),
    bounds: ClosedRange<Dp>? = null,
    disposable: Boolean = false,
    onSwipeOut: () -> Unit
) = onSwipe(
    state = state,
    animateOffset = animateOffset,
    onSwipeLeft = onSwipeOut,
    onSwipeRight = onSwipeOut,
    orientation = orientation,
    delay = delay,
    decay = decay,
    animationSpec = animationSpec,
    bounds = bounds,
    disposable = disposable
)

@Suppress("CyclomaticComplexMethod")
fun Modifier.onSwipe(
    state: SwipeState? = null,
    animateOffset: Boolean = false,
    onSwipeLeft: () -> Unit = { },
    onSwipeRight: () -> Unit = { },
    orientation: Orientation = Orientation.Horizontal,
    delay: Duration = Duration.ZERO,
    decay: Density.() -> DecayAnimationSpec<Float> = { splineBasedDecay(this) },
    animationSpec: AnimationSpec<Float> = spring(),
    bounds: ClosedRange<Dp>? = null,
    disposable: Boolean = false
) = this.composed {
    val swipeState = state ?: rememberSwipeState(Unit)

    pointerInput(Unit) {
        coroutineScope {
            // fling loop, doesn't really offset anything but simulates the animation beforehand
            while (isActive) {
                val velocityTracker = VelocityTracker()

                awaitPointerEventScope {
                    val pointer = awaitFirstDown(requireUnconsumed = false).id
                    launch { swipeState.offset.snapTo(0f) }

                    val onDrag: (PointerInputChange) -> Unit = {
                        val change =
                            if (orientation == Orientation.Horizontal) it.positionChange().x
                            else it.positionChange().y

                        launch { swipeState.offset.snapTo(swipeState.offset.value + change) }

                        velocityTracker.addPosition(it.uptimeMillis, it.position)
                        if (change != 0f) it.consume()
                    }

                    if (orientation == Orientation.Horizontal) {
                        awaitHorizontalTouchSlopOrCancellation(pointer) { change, _ -> onDrag(change) }
                            ?: return@awaitPointerEventScope
                        horizontalDrag(pointer, onDrag)
                    } else {
                        awaitVerticalTouchSlopOrCancellation(pointer) { change, _ -> onDrag(change) }
                            ?: return@awaitPointerEventScope
                        verticalDrag(pointer, onDrag)
                    }
                }

                // drag completed, calculate velocity
                val targetOffset = decay().calculateTargetValue(
                    initialValue = swipeState.offset.value,
                    initialVelocity = velocityTracker.calculateVelocity()
                        .let { if (orientation == Orientation.Horizontal) it.x else it.y }
                )
                val size = if (orientation == Orientation.Horizontal) size.width else size.height

                launch animationEnd@{
                    when {
                        targetOffset >= size / 2 -> {
                            launch {
                                swipeState.offset.animateTo(
                                    targetValue = size.toFloat(),
                                    animationSpec = animationSpec
                                )
                            }
                            delay(delay)
                            onSwipeRight()
                            if (disposable) return@animationEnd swipeState.recycle()
                        }

                        targetOffset <= -size / 2 -> {
                            launch {
                                swipeState.offset.animateTo(
                                    targetValue = -size.toFloat(),
                                    animationSpec = animationSpec
                                )
                            }
                            delay(delay)
                            onSwipeLeft()
                            if (disposable) return@animationEnd swipeState.recycle()
                        }
                    }
                    swipeState.offset.animateTo(
                        targetValue = 0f,
                        animationSpec = animationSpec
                    )
                }
            }
        }
    }.let { modifier ->
        val offset = swipeState.calculateOffset(bounds = bounds)
        when {
            animateOffset && orientation == Orientation.Horizontal -> modifier.offset(x = offset)
            animateOffset && orientation == Orientation.Vertical -> modifier.offset(y = offset)
            else -> modifier
        }
    }
}

fun Modifier.swipeToClose(
    state: SwipeState? = null,
    onClose: () -> Unit,
    delay: Duration = Duration.ZERO,
    decay: Density.() -> DecayAnimationSpec<Float> = { splineBasedDecay(this) }
) = this.composed {
    val swipeState = state ?: rememberSwipeState(Unit)

    val density = LocalDensity.current

    var currentWidth by remember { mutableIntStateOf(0) }
    val currentWidthDp by remember { derivedStateOf { currentWidth.px.dp(density) } }

    val bounds by remember { derivedStateOf { -currentWidthDp..0.dp } }
    val offset = swipeState.calculateOffset(bounds = bounds)

    this
        .onSizeChanged { currentWidth = it.width }
        .alpha((currentWidthDp + offset) / currentWidthDp)
        .onSwipe(
            state = swipeState,
            animateOffset = true,
            disposable = true,
            onSwipeLeft = onClose,
            orientation = Orientation.Horizontal,
            delay = delay,
            decay = decay,
            bounds = bounds
        )
}
