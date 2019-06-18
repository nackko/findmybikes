package com.ludoscity.findmybikes.ui.settings

/**
 * Created by F8Full on 2019-06-17. This file is part of #findmybikes
 *
 */
/**
 * User details post authentication that is exposed to the UI
 */
data class AuthLoggedInUserView(
        val accesstoken: String,
        val refreshToken: String
        //... other data fields that may be accessible to the UI
)
