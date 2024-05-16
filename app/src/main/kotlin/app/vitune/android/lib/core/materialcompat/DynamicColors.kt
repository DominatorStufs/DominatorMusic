package app.vitune.android.lib.core.materialcompat

import app.vitune.android.lib.core.ui.utils.isAtLeastAndroid12

@Suppress("unused")
object DynamicColors {
    @JvmStatic
    fun isDynamicColorAvailable() = isAtLeastAndroid12
}
