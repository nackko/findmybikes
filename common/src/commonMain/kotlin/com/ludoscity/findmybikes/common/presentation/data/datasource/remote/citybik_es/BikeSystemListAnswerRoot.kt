package com.ludoscity.findmybikes.common.presentation.data.datasource.remote.citybik_es

import kotlinx.serialization.Serializable

/**
 * Created by F8Full on 2015-10-10.
 * Data model class for citybik.es API
 */
@Serializable
data class BikeSystemListAnswerRoot(val networks: List<BikeSystemDesc>?)