package it.vfsfitvnm.vimusic.ui.components.themed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.medium

@Composable
fun TextToggle(
    state: Boolean,
    toggleState: () -> Unit,
    name: String,
    modifier: Modifier = Modifier,
    onLabel: String = stringResource(R.string.on_label),
    offLabel: String = stringResource(R.string.off_label)
) {
    val (colorPalette, typography) = LocalAppearance.current

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { toggleState() }
            .background(colorPalette.background1)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize()
    ) {
        BasicText(
            text = "$name ",
            style = typography.xxs.medium
        )

        AnimatedContent(
            targetState = state,
            transitionSpec = {
                val slideDirection =
                    if (targetState) AnimatedContentTransitionScope.SlideDirection.Up
                    else AnimatedContentTransitionScope.SlideDirection.Down

                ContentTransform(
                    targetContentEnter = slideIntoContainer(slideDirection) + fadeIn(),
                    initialContentExit = slideOutOfContainer(slideDirection) + fadeOut()
                )
            },
            label = ""
        ) {
            BasicText(
                text = if (it) onLabel else offLabel,
                style = typography.xxs.medium
            )
        }
    }
}
