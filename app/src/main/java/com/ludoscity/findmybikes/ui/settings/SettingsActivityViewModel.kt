package com.ludoscity.findmybikes.ui.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import com.ludoscity.findmybikes.data.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URI

/**
 * Created by F8Full on 2019-06-16. This file is part of #findmybikes
 * ViewModel for handling SettingsActivity data prep for UI and business logic
 */

class SettingsActivityViewModel(private val repo: FindMyBikesRepository, application: Application)//,
    : AndroidViewModel(application) {

    private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)

    private val _autoUpdate = MutableLiveData<Boolean>()
    val isAutoUpdate: LiveData<Boolean> = _autoUpdate


    private val _authLoginResult = MutableLiveData<AuthLoginResult>()
    val authLoginResult: LiveData<AuthLoginResult> = _authLoginResult

    @Suppress("PrivatePropertyName")
    private val _OAuthClientRegistrationResult = MutableLiveData<OAuthClientRegistrationResult>()
    val clientRegistrationResult: LiveData<OAuthClientRegistrationResult> = _OAuthClientRegistrationResult

    private val _authenticationUri = MutableLiveData<URI>()
    val authenticationUri: LiveData<URI> = _authenticationUri

    //Maybe this should also live in repo after repos fusion ?
    private val _cozyBaseUrlString = MutableLiveData<String>()
    val cozyBaseUrlString: LiveData<String> = _cozyBaseUrlString

    fun registerOAuthClient(cozyUrlUserInput: String) {

        val finalUrl = getCozyUrl(cozyUrlUserInput)

        _cozyBaseUrlString.value = finalUrl

        coroutineScopeIO.launch {
            val result = repo.registerCozyOAuthClient(finalUrl)

            if (result is Result.Success) {
                _OAuthClientRegistrationResult.postValue(
                        OAuthClientRegistrationResult(
                                success =
                                LoggedOAuthClientView(
                                        registrationAccessToken = result.data.registrationAccessToken,
                                        clientId = result.data.clientId,
                                        clientSecret = result.data.clientSecret
                                )
                        )
                )
            } else {
                _OAuthClientRegistrationResult.postValue(
                        OAuthClientRegistrationResult(
                                error =
                                R.string.registration_failed
                        )
                )
            }
        }
    }

    private fun getCozyUrl(userInput: String): String {
        return if (!userInput.contains(".")) {
            "https://$userInput.mycozy.cloud"
        } else if (!userInput.contains("https://") && !userInput.contains("http://")) {
            "https://$userInput"
        } else {
            userInput
        }
    }

    fun unregisterAuthclient() {

        cozyBaseUrlString.value?.let {
            coroutineScopeIO.launch {
                val result = repo.unregisterCozyOAuthClient(it)

                if (result is Result.Success) {
                    _OAuthClientRegistrationResult.postValue(null)
                    Log.i("TAG", "OAuth client deleted")
                }
            }
        }
    }

    fun isRegistered(): Boolean {
        return repo.isCozyOAuthClientRegistered
    }

    fun authenticate() {

        _cozyBaseUrlString.value?.let {
            coroutineScopeIO.launch {

                val result = repo.buildCozyAuthenticationUri(it, repo.cozyOAuthClient)

                if (result is Result.Success) {
                    _authenticationUri.postValue(result.data)
                }

            }
        }
    }

    fun retrieveAccessTokenAndRefreshToken(redirectIntentData: String) {

        cozyBaseUrlString.value?.let {
            coroutineScopeIO.launch {

                //TODO: merge everything to have a single repo and a single data source (which is cozy data source)
                val result = repo.exchangeCozyAuthCodeForTokenCouple(
                        it,
                        redirectIntentData,
                        repo.cozyOAuthClient?.clientId!!,
                        repo.cozyOAuthClient?.clientSecret!!
                )

                if (result is Result.Success) {
                    _authLoginResult.postValue(AuthLoginResult(AuthLoggedInUserView(result.data.accessToken, result.data.refreshToken)))
                }
            }
        }
    }
}
