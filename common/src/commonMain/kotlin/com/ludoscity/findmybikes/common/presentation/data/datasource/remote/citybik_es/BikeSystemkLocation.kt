package com.ludoscity.findmybikes.common.presentation.data.datasource.remote.citybik_es

import kotlinx.serialization.Serializable
/**
 * Created by F8Full on 2015-07-01.
 *   Created by F8Full on 2015-10-10.
 * Data model class for citybik.es API
 */
@Serializable
data class BikeSystemLocation(val latitude: Double,
                              val longitude: Double,
                              val city: String)
