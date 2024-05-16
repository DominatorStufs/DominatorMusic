package app.vitune.android.lib.compose.reordering

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Modifier

context(LazyItemScope)
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.animateItemPlacement(reorderingState: ReorderingState) =
    if (reorderingState.draggingIndex == -1) animateItemPlacement() else this
