package com.ludoscity.findmybikes.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.lifecycle.ViewModelProviders
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.utils.InjectorUtils

/**
 * Created by F8Full on 2015-08-10.
 * Activity used to display Settings fragment
 */
class SettingsActivity : AppCompatActivity() {

    //capturing intent targeting custom URL scheme defined in manifest
    //for OAuth login flow
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val action = intent?.action
        val data = intent?.dataString

        if (action == Intent.ACTION_VIEW && data != null) {
            //Log.e("SettingsActivity", "Intent data string : $data")

            ViewModelProviders.of(this, InjectorUtils.provideSettingsActivityViewModelFactory(application))
                    .get(SettingsActivityViewModel::class.java).retrieveAccessTokenAndRefreshToken(data)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        setSupportActionBar(findViewById(R.id.toolbar_main))


        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)


        supportActionBar!!.subtitle = getString(R.string.hashtag_settings)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Respond to the action bar's Up/Home button
        if (item.itemId == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
