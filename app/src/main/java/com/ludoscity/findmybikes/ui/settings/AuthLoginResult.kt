package com.ludoscity.findmybikes.ui.settings

/**
 * Created by F8Full on 2019-06-17. This file is part of #findmybikes
 *
 */
/**
 * Authentication result : success (user details) or error message.
 */
data class AuthLoginResult(
        val success: AuthLoggedInUserView? = null,
        val error: Int? = null
)
