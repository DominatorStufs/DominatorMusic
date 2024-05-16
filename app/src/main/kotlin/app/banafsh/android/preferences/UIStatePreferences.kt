package app.banafsh.android.preferences

import app.banafsh.android.GlobalPreferencesHolder

object UIStatePreferences : GlobalPreferencesHolder() {
    var homeScreenTabIndex by int(0)
    var searchResultScreenTabIndex by int(0)

    var artistScreenTabIndexProperty = int(0)
    var artistScreenTabIndex by artistScreenTabIndexProperty
}
