package app.banafsh.android.ui.screens.mood

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.res.stringResource
import app.banafsh.android.R
import app.banafsh.android.lib.compose.persist.PersistMapCleanup
import app.banafsh.android.lib.compose.routing.RouteHandler
import app.banafsh.android.models.Mood
import app.banafsh.android.ui.components.themed.Scaffold
import app.banafsh.android.ui.screens.GlobalRoutes
import app.banafsh.android.ui.screens.Route

@Route
@Composable
fun MoodScreen(mood: Mood) {
    val saveableStateHolder = rememberSaveableStateHolder()

    PersistMapCleanup(prefix = "playlist/$DEFAULT_BROWSE_ID")

    RouteHandler(listenToGlobalEmitter = true) {
        GlobalRoutes()

        NavHost {
            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = 0,
                onTabChanged = { },
                tabColumnContent = { item ->
                    item(0, stringResource(R.string.mood), R.drawable.disc)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> MoodList(mood = mood)
                    }
                }
            }
        }
    }
}
