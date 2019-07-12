package com.ludoscity.findmybikes.ui.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.data.Result
import com.ludoscity.findmybikes.utils.InjectorUtils
import com.ludoscity.findmybikes.utils.afterTextChanged

/**
 * Created by F8Full on 2016-04-10. This file is part of #findmybikes
 * Provides a Setting dialog to configure map marker colors availability values
 * http://stackoverflow.com/questions/4505845/concise-way-of-writing-new-dialogpreference-classes
 */
class SettingsCozyDialogPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs) {

    private var username: EditText? = null
    private var cozyUrl: TextView? = null
    private var clientInfo: TextView? = null
    private var accessToken: TextView? = null
    private var refreshToken: TextView? = null
    private var registering: Button? = null
    private var authenticate: Button? = null
    private var cozyTest: Button? = null
    private var loading: ProgressBar? = null
    private var root: View? = null

    private var model: SettingsActivityViewModel? = null

    init {

        //TODO: deprecated in API level 29. Now in Jetpack. See branch settings-from-jetpack
        isPersistent = false
        dialogLayoutResource = R.layout.settings_cozy_dialog_content
        positiveButtonText = null
        negativeButtonText = null
    }

    @SuppressLint("StringFormatInvalid")
    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        root = view.findViewById(R.id.cozy_root)
        username = view.findViewById(R.id.username)
        cozyUrl = view.findViewById(R.id.final_url)
        clientInfo = view.findViewById(R.id.client_info)
        accessToken = view.findViewById(R.id.access_token)
        refreshToken = view.findViewById(R.id.refresh_token)
        registering = view.findViewById(R.id.registering)
        authenticate = view.findViewById(R.id.authenticate)
        cozyTest = view.findViewById(R.id.cozy_test)
        registering?.isEnabled = true
        loading = view.findViewById(R.id.loading)

        username?.apply {

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        model?.registerOAuthClient()
                }
                false
            }
            afterTextChanged {
                model?.urlChanged(username?.text.toString())
            }
        }

        model = ViewModelProviders.of(context as SettingsActivity, InjectorUtils.provideSettingsActivityViewModelFactory((context as SettingsActivity).application))
                .get(SettingsActivityViewModel::class.java)

        model?.liveCozyUrl?.observe(context as SettingsActivity, Observer {
            cozyUrl?.text = it
        })

        registering?.setOnClickListener {
            loading?.visibility = View.VISIBLE
            if (model?.isRegistered() != true) {
                model?.registerOAuthClient()

            } else
                model?.unregisterAuthclient()
        }

        authenticate?.setOnClickListener {
            if (model?.isRegistered() == true)
                model?.authenticate()
        }

        cozyTest?.setOnClickListener {
            //if (model?.isAuthorized() == true){
            loading?.visibility = View.VISIBLE
            model?.testCozyCloud()
            //}
        }

        model?.clientRegistrationResult?.observe(context as SettingsActivity, Observer {
            if (it == null) {
                registering?.text = (context as Activity).getString(R.string.action_registering)
                username?.isEnabled = true
                cozyUrl?.text = ""
                authenticate?.isEnabled = false
                cozyTest?.isEnabled = false
                clientInfo?.text = (context as Activity).getString(R.string.client_info_default)
                accessToken?.text = ""
                refreshToken?.text = ""
                loading?.visibility = View.INVISIBLE
            }

            val registrationResult = it ?: return@Observer

            loading?.visibility = View.GONE

            if (registrationResult.error != null) {
                registering?.text = (context as Activity).getString(R.string.action_registering)
                authenticate?.isEnabled = false
            }

            if (registrationResult.success != null) {
                clientInfo?.text = "OAuth client registration token : ${registrationResult.success.registrationAccessToken}"
                cozyUrl?.text = registrationResult.success.stackBaseUrl
                accessToken?.text = (context as Activity).getString(R.string.tap_authenticate)
                refreshToken?.text = (context as Activity).getString(R.string.tap_authenticate)
                registering?.text = (context as Activity).getString(R.string.action_unregistering)
                authenticate?.isEnabled = true
                username?.isEnabled = false
                val imm = (context as Activity).getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(username?.windowToken, 0)
            }
        })

        model?.authLoginResult?.observe(context as SettingsActivity, Observer {

            if (it == null) {
                accessToken?.text = ""
                refreshToken?.text = ""
            }

            val authLoginResult = it ?: return@Observer

            accessToken?.text = "access token : ${authLoginResult.success?.accesstoken}"
            refreshToken?.text = "refresh token : ${authLoginResult.success?.refreshToken}"
            authenticate?.isEnabled = false
            cozyTest?.isEnabled = true
        })

        model?.cozyTestResult?.observe(context as SettingsActivity, Observer {
            it?.let { testResult ->
                loading?.visibility = View.GONE
                if (testResult is Result.Success) {
                    Snackbar.make(root!!, "Test successful", Snackbar.LENGTH_LONG)
                            .setAction("View in Cozy") {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse(model?.getCozyDriveUrl())
                                if (intent.resolveActivity((context as Activity).packageManager) != null) {
                                    (context as Activity).startActivity(intent)
                                }
                            }
                            .show()
                } else {
                    Snackbar.make(root!!, "Test failed", Snackbar.LENGTH_LONG)
                            .show()
                }
            }
        })

        model?.authenticationUri?.observe(context as SettingsActivity, Observer {
            it?.let { authURI ->
                val connection = object : CustomTabsServiceConnection() {
                    override fun onCustomTabsServiceConnected(componentName: ComponentName, client: CustomTabsClient) {
                        val builder = CustomTabsIntent.Builder()
                        val intent = builder.build()
                        client.warmup(0L) // This prevents backgrounding after redirection
                        intent.launchUrl(
                                context as SettingsActivity,
                                Uri.parse(authURI.toURL().toString())
                        )//pass the url you need to open
                    }

                    override fun onServiceDisconnected(name: ComponentName) {

                    }
                }

                if (!CustomTabsClient.bindCustomTabsService(
                                context as SettingsActivity,
                                "com.brave.browser",
                                //"com.android.chrome",
                                connection
                        )
                ) {
                    Snackbar.make(root!!, "Brave browser recommended", Snackbar.LENGTH_INDEFINITE)
                            .setAction("Download") {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse("market://details?id=com.brave.browser")
                                if (intent.resolveActivity((context as Activity).packageManager) != null) {
                                    (context as Activity).startActivity(intent)
                                }
                            }
                            .show()
                }
            }
        })

        /*username.afterTextChanged {
            ActivityViewModel.loginDataChanged(
                username.text.toString(),
                "12345"//password.text.toString()
            )
        }*/
    }


    override fun onDialogClosed(positiveResult: Boolean) {

        /*if (positiveResult) {
            SharedPrefHelper.getInstance().saveCriticalAvailabilityMax(context, mCriticalMaxPicker!!.value)
            SharedPrefHelper.getInstance().saveBadAvailabilityMax(context, mBadMaxPicker!!.value)
        }*/
        super.onDialogClosed(positiveResult)
    }
}
