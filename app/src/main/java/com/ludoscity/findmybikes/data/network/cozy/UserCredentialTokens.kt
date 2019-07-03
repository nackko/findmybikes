package com.ludoscity.findmybikes.data.network.cozy

/**
 * Created by F8Full on 2019-06-17. This file is part of #findmybikes
 *
 */
/**
 * Data class that captures user information for logged in users retrieved from Repository
 * // If user credentials will be cached in local storage, it is recommended it be encrypted
// @see https://developer.android.com/training/articles/keystore
 */
data class UserCredentialTokens(
        val accessToken: String,
        val refreshToken: String//,
        //val idToken: String
)
