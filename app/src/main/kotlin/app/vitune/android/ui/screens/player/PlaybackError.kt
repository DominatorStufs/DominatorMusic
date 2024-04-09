package app.vitune.android.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.vitune.android.utils.center
import app.vitune.android.utils.color
import app.vitune.android.utils.medium
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.PureBlackColorPalette

@Composable
fun PlaybackError(
    isDisplayed: Boolean,
    messageProvider: @Composable () -> String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) = Box(modifier = modifier) {
    val message by rememberUpdatedState(newValue = messageProvider())

    AnimatedVisibility(
        visible = isDisplayed,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Spacer(
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                }
                .fillMaxSize()
                .background(Color.Black.copy(0.8f))
        )
    }

    AnimatedContent(
        targetState = message.takeIf { isDisplayed },
        transitionSpec = {
            ContentTransform(
                targetContentEnter = slideInVertically { -it },
                initialContentExit = slideOutVertically { -it },
                sizeTransform = null
            )
        },
        label = "",
        modifier = Modifier.fillMaxWidth()
    ) { currentMessage ->
        if (currentMessage != null) BasicText(
            text = currentMessage,
            style = LocalAppearance.current.typography.xs.center.medium.color(PureBlackColorPalette().text),
            modifier = Modifier
                .background(Color.Black.copy(0.4f))
                .padding(all = 8.dp)
                .fillMaxWidth()
        )
    }
}
