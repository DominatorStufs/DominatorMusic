package app.vitune.compose.reordering

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Modifier

context(LazyItemScope)
fun Modifier.animateItemPlacement(reorderingState: ReorderingState) =
    if (reorderingState.draggingIndex == -1) this.animateItem(
        fadeInSpec = null,
        fadeOutSpec = null
    ) else this
