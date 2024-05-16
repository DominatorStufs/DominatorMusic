package app.banafsh.android.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastForEach

private val LazyGridLayoutInfo.singleAxisViewportSize: Int
    get() = if (orientation == Orientation.Vertical) viewportSize.height else viewportSize.width

context(Density)
@OptIn(ExperimentalFoundationApi::class)
private fun SnapLayoutInfoProvider(
    lazyGridState: LazyGridState,
    positionInLayout: Density.(layoutSize: Float, itemSize: Float) -> Float =
        { layoutSize, itemSize -> (layoutSize / 2f - itemSize / 2f) }
): SnapLayoutInfoProvider = object : SnapLayoutInfoProvider {
    private val layoutInfo: LazyGridLayoutInfo
        get() = lazyGridState.layoutInfo

    // Single page snapping is the default
    override fun calculateApproachOffset(initialVelocity: Float) = 0f

    // ignoring the velocity for now since there is no animation spec in this provider
    override fun calculateSnappingOffset(currentVelocity: Float): Float {
        var lowerBoundOffset = Float.NEGATIVE_INFINITY
        var upperBoundOffset = Float.POSITIVE_INFINITY

        layoutInfo.visibleItemsInfo.fastForEach { item ->
            val offset = calculateDistanceToDesiredSnapPosition(layoutInfo, item, positionInLayout)
            // Find item that is closest to the center
            if (offset <= 0 && offset > lowerBoundOffset) lowerBoundOffset = offset
            // Find item that is closest to center, but after it
            if (offset >= 0 && offset < upperBoundOffset) upperBoundOffset = offset
        }

        return if ((lowerBoundOffset * -1f) > upperBoundOffset) upperBoundOffset else lowerBoundOffset
    }
}

private fun Density.calculateDistanceToDesiredSnapPosition(
    layoutInfo: LazyGridLayoutInfo,
    item: LazyGridItemInfo,
    positionInLayout: Density.(layoutSize: Float, itemSize: Float) -> Float
): Float {
    val containerSize =
        with(layoutInfo) { singleAxisViewportSize - beforeContentPadding - afterContentPadding }

    val desiredDistance = positionInLayout(containerSize.toFloat(), item.size.width.toFloat())
    val itemCurrentPosition = item.offset.x.toFloat()

    return itemCurrentPosition - desiredDistance
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberSnapLayoutInfoProvider(
    lazyGridState: LazyGridState,
    positionInLayout: Density.(layoutSize: Float, itemSize: Float) -> Float =
        { layoutSize, itemSize -> (layoutSize / 2f - itemSize / 2f) }
): SnapLayoutInfoProvider {
    val density = LocalDensity.current

    return remember(lazyGridState, density) {
        with(density) {
            SnapLayoutInfoProvider(lazyGridState, positionInLayout)
        }
    }
}
