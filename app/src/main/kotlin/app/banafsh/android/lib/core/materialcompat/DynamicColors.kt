package app.banafsh.android.lib.core.materialcompat

import app.banafsh.android.lib.core.ui.utils.isAtLeastAndroid12

@Suppress("unused")
object DynamicColors {
    @JvmStatic
    fun isDynamicColorAvailable() = isAtLeastAndroid12
}
