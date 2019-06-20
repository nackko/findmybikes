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

    init {

        //TODO: could being in a background thread lead to issues ?
        coroutineScopeIO.launch {
            if (repo.isCozyOAuthClientRegistered) {
                _OAuthClientRegistrationResult.postValue(OAuthClientRegistrationResult(
                        success = LoggedOAuthClientView(
                                stackBaseUrl = repo.cozyOAuthClient!!.stackBaseUrl,
                                registrationAccessToken = repo.cozyOAuthClient!!.registrationAccessToken,
                                clientId = repo.cozyOAuthClient!!.clientId,
                                clientSecret = repo.cozyOAuthClient!!.clientSecret
                        )
                ))

                if (repo.userCred != null) {
                    _authLoginResult.postValue(AuthLoginResult(
                            success = AuthLoggedInUserView(
                                    accesstoken = repo.userCred!!.accessToken,
                                    refreshToken = repo.userCred!!.refreshToken
                            )
                    ))
                }
            }
        }
    }

    fun registerOAuthClient(cozyUrlUserInput: String) {

        val finalUrl = getCozyUrl(cozyUrlUserInput)

        coroutineScopeIO.launch {
            val result = repo.registerCozyOAuthClient(finalUrl)

            if (result is Result.Success) {
                _OAuthClientRegistrationResult.postValue(
                        OAuthClientRegistrationResult(
                                success =
                                LoggedOAuthClientView(
                                        stackBaseUrl = result.data.stackBaseUrl,
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

        clientRegistrationResult.value?.let {
            coroutineScopeIO.launch {
                val result = repo.unregisterCozyOAuthClient(it.success?.stackBaseUrl!!)

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

        clientRegistrationResult.value?.let {
            coroutineScopeIO.launch {

                val result = repo.buildCozyAuthenticationUri(it.success?.stackBaseUrl!!, repo.cozyOAuthClient)

                if (result is Result.Success) {
                    _authenticationUri.postValue(result.data)
                }

            }
        }
    }

    fun retrieveAccessTokenAndRefreshToken(redirectIntentData: String) {

        clientRegistrationResult.value?.let {
            coroutineScopeIO.launch {

                val result = repo.exchangeCozyAuthCodeForTokenCouple(
                        it.success?.stackBaseUrl!!,
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
