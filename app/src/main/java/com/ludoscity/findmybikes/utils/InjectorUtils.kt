package com.ludoscity.findmybikes.utils

import android.content.Context
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import com.ludoscity.findmybikes.data.network.BikeSystemNetworkDataSource
import com.ludoscity.findmybikes.helpers.DBHelper
import com.ludoscity.findmybikes.ui.main.FindMyBikesModelFactory

class InjectorUtils {

    companion object {

        fun provideNetworkDataSource(): BikeSystemNetworkDataSource {
            // This call to provide repository is necessary if the app starts from a service - in this
            // case the repository will not exist unless it is specifically created.
            provideRepository()
            return BikeSystemNetworkDataSource.getInstance()
        }

        fun provideRepository(): FindMyBikesRepository {
            //val database = d.getInstance(context.applicationContext)
            val networkDataSource = BikeSystemNetworkDataSource.getInstance()
            return FindMyBikesRepository.getInstance(DBHelper.getInstance().database.bikeStationDao(), networkDataSource)
        }

        fun provideMainActivityViewModelFactory(context: Context): FindMyBikesModelFactory {
            val repository = provideRepository()
            return FindMyBikesModelFactory(repository, context.applicationContext)
        }
    }

}