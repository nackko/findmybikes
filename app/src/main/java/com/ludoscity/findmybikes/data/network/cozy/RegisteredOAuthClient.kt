package com.ludoscity.findmybikes.data.network.cozy

/**
 * Created by F8Full on 2019-06-17. This file is part of #findmybikes
 *
 */
/**
 * Data class that captures user information for logged in users retrieved from Repository
 */
data class RegisteredOAuthClient(
        val stackBaseUrl: String,
        val clientId: String,
        val clientSecret: String,
        val registrationAccessToken: String
)
