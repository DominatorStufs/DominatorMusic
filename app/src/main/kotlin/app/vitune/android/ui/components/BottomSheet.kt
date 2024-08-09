package app.vitune.android.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun BottomSheet(
    state: BottomSheetState,
    collapsedContent: @Composable BoxScope.(Modifier) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    indication: Indication? = LocalIndication.current,
    content: @Composable BoxScope.() -> Unit
) = Box(
    modifier = modifier
        .offset {
            IntOffset(
                x = 0,
                y = (state.expandedBound - state.value)
                    .roundToPx()
                    .coerceAtLeast(0)
            )
        }
        .pointerInput(state) {
            val velocityTracker = VelocityTracker()

            detectVerticalDragGestures(
                onVerticalDrag = { change, dragAmount ->
                    velocityTracker.addPointerInputChange(change)
                    state.dispatchRawDelta(dragAmount)
                },
                onDragCancel = {
                    velocityTracker.resetTracking()
                    state.snapTo(state.collapsedBound)
                },
                onDragEnd = {
                    val velocity = -velocityTracker.calculateVelocity().y
                    velocityTracker.resetTracking()
                    state.fling(velocity, onDismiss)
                }
            )
        }
        .fillMaxSize()
) {
    if (state.expanding) BackHandler(onBack = state::collapseSoft)
    if (!state.dismissed) content()

    if (!state.expanded && (onDismiss == null || !state.dismissed)) Box(
        modifier = Modifier
            .graphicsLayer {
                alpha = 1f - (state.progress * 16).coerceAtMost(1f)
            }
            .fillMaxWidth()
            .height(state.collapsedBound),
        content = {
            collapsedContent(
                Modifier.clickable(
                    onClick = state::expandSoft,
                    indication = indication,
                    interactionSource = remember { MutableInteractionSource() }
                )
            )
        }
    )
}

@Stable
class BottomSheetState internal constructor(
    draggableState: DraggableState,
    private val coroutineScope: CoroutineScope,
    private val animatable: Animatable<Dp, AnimationVector1D>,
    private val onAnchorChanged: (Anchor) -> Unit,
    val collapsedBound: Dp
) : DraggableState by draggableState {
    val value by animatable.asState()

    val dismissedBound by derivedStateOf { animatable.lowerBound!! }
    val expandedBound by derivedStateOf { animatable.upperBound!! }

    val dismissed by derivedStateOf { value == dismissedBound }
    val collapsed by derivedStateOf { value == collapsedBound }
    val expanded by derivedStateOf { value == expandedBound }
    val expanding by derivedStateOf { animatable.targetValue == expandedBound }
    val progress by derivedStateOf { 1f - (expandedBound - value) / (expandedBound - collapsedBound) }

    private fun deferAnimateTo(
        newValue: Dp,
        spec: AnimationSpec<Dp> = spring()
    ) = coroutineScope.launch {
        animatable.animateTo(newValue, spec)
    }

    private fun collapse(spec: AnimationSpec<Dp> = spring()) {
        onAnchorChanged(Anchor.Collapsed)
        deferAnimateTo(collapsedBound, spec)
    }

    private fun expand(spec: AnimationSpec<Dp> = spring()) {
        onAnchorChanged(Anchor.Expanded)
        deferAnimateTo(expandedBound, spec)
    }

    private fun dismiss(spec: AnimationSpec<Dp> = spring()) {
        onAnchorChanged(Anchor.Dismissed)
        deferAnimateTo(dismissedBound, spec)
    }

    fun collapseSoft() = collapse(tween(300))
    fun expandSoft() = expand(tween(300))
    fun dismissSoft() = dismiss(tween(300))

    fun snapTo(value: Dp) = coroutineScope.launch {
        animatable.snapTo(value)
    }

    fun fling(velocity: Float, onDismiss: (() -> Unit)?) = when {
        velocity > 250 -> expand()
        velocity < -250 -> {
            if (value < collapsedBound && onDismiss != null) {
                dismiss()
                onDismiss()
            } else collapse()
        }

        else -> {
            val l1 = (collapsedBound - dismissedBound) / 2
            val l2 = (expandedBound - collapsedBound) / 2

            when (value) {
                in dismissedBound..l1 -> {
                    if (onDismiss != null) {
                        dismiss()
                        onDismiss()
                    } else collapse()
                }

                in l1..l2 -> collapse()
                in l2..expandedBound -> expand()

                else -> Unit
            }
        }
    }

    val preUpPostDownNestedScrollConnection
        get() = object : NestedScrollConnection {
            var isTopReached = false

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (expanded && available.y < 0) isTopReached = false

                return if (isTopReached && available.y < 0 && source == NestedScrollSource.Drag) {
                    dispatchRawDelta(available.y)
                    available
                } else Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!isTopReached) isTopReached = consumed.y == 0f && available.y > 0

                return if (isTopReached && source == NestedScrollSource.Drag) {
                    dispatchRawDelta(available.y)
                    available
                } else Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity) = if (isTopReached) {
                val velocity = -available.y
                fling(velocity, null)

                available
            } else Velocity.Zero

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                isTopReached = false
                return Velocity.Zero
            }
        }

    @JvmInline
    value class Anchor private constructor(internal val value: Int) {
        companion object {
            val Dismissed = Anchor(value = 0)
            val Collapsed = Anchor(value = 1)
            val Expanded = Anchor(value = 2)
        }

        object Saver : androidx.compose.runtime.saveable.Saver<Anchor, Int> {
            override fun restore(value: Int) = when (value) {
                0 -> Dismissed
                1 -> Collapsed
                2 -> Expanded
                else -> error("Anchor $value does not exist!")
            }

            override fun SaverScope.save(value: Anchor) = value.value
        }
    }
}

@Composable
fun rememberBottomSheetState(
    key: Any? = Unit,
    dismissedBound: Dp,
    expandedBound: Dp,
    collapsedBound: Dp = dismissedBound,
    initialAnchor: BottomSheetState.Anchor = BottomSheetState.Anchor.Dismissed
): BottomSheetState {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var previousAnchor by rememberSaveable(stateSaver = BottomSheetState.Anchor.Saver) {
        mutableStateOf(initialAnchor)
    }

    return remember(key, dismissedBound, expandedBound, collapsedBound, coroutineScope) {
        val initialValue = when (previousAnchor) {
            BottomSheetState.Anchor.Dismissed -> dismissedBound
            BottomSheetState.Anchor.Collapsed -> collapsedBound
            BottomSheetState.Anchor.Expanded -> expandedBound
            else -> error("Unknown BottomSheet anchor")
        }

        val animatable = Animatable(
            initialValue = initialValue,
            typeConverter = Dp.VectorConverter
        ).also {
            it.updateBounds(dismissedBound.coerceAtMost(expandedBound), expandedBound)
        }

        BottomSheetState(
            draggableState = DraggableState { delta ->
                coroutineScope.launch {
                    animatable.snapTo(animatable.value - with(density) { delta.toDp() })
                }
            },
            onAnchorChanged = { previousAnchor = it },
            coroutineScope = coroutineScope,
            animatable = animatable,
            collapsedBound = collapsedBound
        )
    }
}
