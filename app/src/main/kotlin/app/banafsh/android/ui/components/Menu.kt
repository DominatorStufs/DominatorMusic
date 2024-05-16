package app.banafsh.android.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.banafsh.android.ui.modifiers.pressable

val LocalMenuState = staticCompositionLocalOf { MenuState() }

@Stable
class MenuState {
    var isDisplayed by mutableStateOf(false)
        private set

    var content by mutableStateOf<@Composable () -> Unit>({})
        private set

    fun display(content: @Composable () -> Unit) {
        this.content = content
        isDisplayed = true
    }

    fun hide() {
        isDisplayed = false
    }
}

@Composable
fun BottomSheetMenu(
    modifier: Modifier = Modifier,
    state: MenuState = LocalMenuState.current
) {
    AnimatedVisibility(
        visible = state.isDisplayed,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        BackHandler(onBack = state::hide)

        Spacer(
            modifier = Modifier
                .pressable(onRelease = state::hide)
                .background(Color.Black.copy(alpha = 0.5f))
                .fillMaxSize()
        )
    }

    AnimatedVisibility(
        visible = state.isDisplayed,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier.padding(top = 48.dp)
    ) {
        state.content()
    }
}
