package it.vfsfitvnm.vimusic

import it.vfsfitvnm.vimusic.preferences.PreferencesHolder

object Dependencies {
    lateinit var application: MainApplication
        private set

    internal fun init(application: MainApplication) {
        this.application = application
    }
}

open class GlobalPreferencesHolder : PreferencesHolder(Dependencies.application, "preferences")
