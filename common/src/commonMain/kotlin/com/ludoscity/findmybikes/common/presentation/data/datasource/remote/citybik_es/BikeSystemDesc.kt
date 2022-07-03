package com.ludoscity.findmybikes.common.presentation.data.datasource.remote.citybik_es

import kotlinx.serialization.Serializable

/**
 * Created by F8Full on 2019-03-24.
 *   Created by F8Full on 2015-10-10.
 * Data model class for citybik.es API
 */
@Serializable
data class BikeSystemDesc(val id: String,
                          val href: String,
                          val name: String,
                          val location: BikeSystemLocation
)