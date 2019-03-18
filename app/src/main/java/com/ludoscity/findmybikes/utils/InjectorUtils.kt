package com.ludoscity.findmybikes.utils

import android.app.Application
import android.arch.lifecycle.LiveData
import com.google.android.gms.maps.model.LatLng
import com.ludoscity.findmybikes.citybik_es.model.BikeStation
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import com.ludoscity.findmybikes.data.network.BikeSystemNetworkDataSource
import com.ludoscity.findmybikes.helpers.DBHelper
import com.ludoscity.findmybikes.ui.main.FindMyBikesModelFactory
import com.ludoscity.findmybikes.ui.map.MapFragmentModelFactory
import com.ludoscity.findmybikes.ui.table.TableFragmentModelFactory
import java.text.NumberFormat

/**
 * Created by F8Full on 2019-02-15. This file is part of #findmybikes
 * Enables dependency injection by providing static methods to retrieve components. TODO: Dagger2
 */
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

        fun provideMainActivityViewModelFactory(app: Application): FindMyBikesModelFactory {
            val repository = provideRepository()
            return FindMyBikesModelFactory(repository, app)
        }

        fun provideMapFragmentViewModelFactory(app: Application,
                                               isLookingForBike: LiveData<Boolean>,
                                               isDataOutOfDate: LiveData<Boolean>,
                                               userLoc: LiveData<LatLng>,
                                               stationA: LiveData<BikeStation>,
                                               stationB: LiveData<BikeStation>,
                                               finalDestinationLoc: LiveData<LatLng>,
                                               isFinalDestinationFavorite: LiveData<Boolean>
        ): MapFragmentModelFactory {
            val repository = provideRepository()
            return MapFragmentModelFactory(
                    repository,
                    app,
                    isLookingForBike,
                    isDataOutOfDate,
                    userLoc,
                    stationA,
                    stationB,
                    finalDestinationLoc,
                    isFinalDestinationFavorite
            )
        }

        fun provideTableFragmentViewModelFactory(app: Application,
                                                 isDockTable: Boolean,
                                                 appBarExpanded: LiveData<Boolean>,
                                                 dataOutOfDate: LiveData<Boolean>,
                                                 stationRecapDatasource: LiveData<BikeStation>,
                                                 stationSelectionDatasource: LiveData<BikeStation>,
                                                 userLoc: LiveData<LatLng>,
                                                 numFormat: NumberFormat): TableFragmentModelFactory {
            val repository = provideRepository()
            return TableFragmentModelFactory(repository, app, isDockTable,
                    appBarExpanded,
                    stationRecapDatasource,
                    stationSelectionDatasource,
                    dataOutOfDate,
                    userLoc,
                    numFormat)
        }
    }
}