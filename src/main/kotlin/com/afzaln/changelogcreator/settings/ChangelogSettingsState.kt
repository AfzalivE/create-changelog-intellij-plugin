package com.afzaln.changelogcreator.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*

/**
 * Supports storing the application settings in a persistent way.
 * The [State] and [Storage] annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 */
@Service
@State(
    name = "com.github.afzalive.createchangelogintellijplugin.settings.ChangelogState",
    storages = [Storage("changelogcreator.xml")]
)
class ChangelogSettings : SimplePersistentStateComponent<ChangelogState>(ChangelogState()) {
    companion object {
        fun getInstance(): ChangelogSettings {
            return ApplicationManager.getApplication().getService(ChangelogSettings::class.java)
        }
    }
}

class ChangelogState : BaseState() {
    var changelogTemplate by string("🐛🕶⭐⛳️")
}
