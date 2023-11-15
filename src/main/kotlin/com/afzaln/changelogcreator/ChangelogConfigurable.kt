package com.afzaln.changelogcreator

import com.afzaln.changelogcreator.components.AppSettingsComponent
import com.afzaln.changelogcreator.settings.ChangelogSettings
import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.Nls.Capitalization
import org.jetbrains.annotations.Nullable
import javax.swing.JComponent

/**
 * Provides controller functionality for application settings.
 */
internal class ChangelogConfigurable : Configurable {
    private var mySettingsComponent: AppSettingsComponent? = null

    @Nls(capitalization = Capitalization.Title)
    override fun getDisplayName(): String {
        return "Changelog Creator"
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return mySettingsComponent!!.preferredFocusedComponent
    }

    @Nullable
    override fun createComponent(): JComponent {
        mySettingsComponent = AppSettingsComponent()
        return mySettingsComponent!!.panel
    }

    override fun isModified(): Boolean {
        val settings = ChangelogSettings.getInstance().state
        val modified: Boolean =
            !mySettingsComponent?.changelogText.equals(settings.changelogTemplate)
        return modified
    }

    override fun apply() {
        val settings = ChangelogSettings.getInstance().state
        settings.changelogTemplate = mySettingsComponent?.changelogText
    }

    override fun reset() {
        val settings = ChangelogSettings.getInstance().state
        mySettingsComponent?.changelogText = settings.changelogTemplate
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}
