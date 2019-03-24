package com.ludoscity.findmybikes.data.network.citybik_es

/**
 * Created by F8Full on 2019-03-24.
 *   Created by F8Full on 2015-10-10.
 * Data model class for citybik.es API
 */
data class BikeSystemDesc(val id: String,
                          val href: String,
                          val name: String,
                          val location: BikeNetworkLocation)