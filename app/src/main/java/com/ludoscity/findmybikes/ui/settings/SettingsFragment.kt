package com.ludoscity.findmybikes.ui.settings

import android.os.Bundle
import android.preference.PreferenceFragment

import com.ludoscity.findmybikes.R

/**
 * Created by Looney on 2015-04-19. This file is part of #findmybikes
 * Used to handle the Settings section
 */
class SettingsFragment : PreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.user_settings)
    }
}
