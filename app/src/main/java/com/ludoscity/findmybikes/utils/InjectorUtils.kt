package com.ludoscity.findmybikes.utils

import android.app.Application
import android.arch.lifecycle.LiveData
import com.google.android.gms.maps.model.LatLng
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import com.ludoscity.findmybikes.data.database.BikeStation
import com.ludoscity.findmybikes.data.network.BikeSystemListNetworkDataSource
import com.ludoscity.findmybikes.data.network.BikeSystemStatusNetworkDataSource
import com.ludoscity.findmybikes.helpers.DBHelper
import com.ludoscity.findmybikes.ui.main.FindMyBikesModelFactory
import com.ludoscity.findmybikes.ui.map.MapFragmentModelFactory
import com.ludoscity.findmybikes.ui.table.TableFragmentModelFactory
import com.ludoscity.findmybikes.ui.trip.TripFragmentModelFactory
import java.text.NumberFormat

/**
 * Created by F8Full on 2019-02-15. This file is part of #findmybikes
 * Enables dependency injection by providing static methods to retrieve components. TODO: Dagger2
 */
class InjectorUtils {

    companion object {

        fun provideBikeSystemStatusNetworkDataSource(): BikeSystemStatusNetworkDataSource {
            // This call to provide repository is necessary if the app starts from a service - in this
            // case the repository will not exist unless it is specifically created.
            provideRepository()
            return BikeSystemStatusNetworkDataSource.getInstance()
        }

        fun provideBikeSystemListNetworkDataSource(): BikeSystemListNetworkDataSource {
            provideRepository()
            return BikeSystemListNetworkDataSource.getInstance()
        }

        fun provideRepository(): FindMyBikesRepository {
            //TODO: retrieve DAO by retrieving db from here instead of going to DBHelper class
            //val database = d.getInstance(context.applicationContext)
            val systemListNetworkDataSource = BikeSystemListNetworkDataSource.getInstance()
            val systemStatusNetworkDataSource = BikeSystemStatusNetworkDataSource.getInstance()
            return FindMyBikesRepository.getInstance(DBHelper.getInstance().database.bikeSystemDao(),
                    DBHelper.getInstance().database.bikeStationDao(),
                    systemListNetworkDataSource,
                    systemStatusNetworkDataSource)
        }

        fun provideMainActivityViewModelFactory(app: Application): FindMyBikesModelFactory {
            val repository = provideRepository()
            return FindMyBikesModelFactory(repository, app)
        }

        fun provideMapFragmentViewModelFactory(app: Application,
                                               hasLocationPermission: LiveData<Boolean>,
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
                    hasLocationPermission,
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

        fun provideTripFragmentViewModelFactory(app: Application,
                                                userLoc: LiveData<LatLng>,
                                                stationALoc: LiveData<LatLng>,
                                                stationBLoc: LiveData<LatLng>,
                                                finalDestLoc: LiveData<LatLng>,
                                                isFinalDestFavorite: LiveData<Boolean>,
                                                numFormat: NumberFormat): TripFragmentModelFactory {
            return TripFragmentModelFactory(app,
                    userLoc,
                    stationALoc,
                    stationBLoc,
                    finalDestLoc,
                    isFinalDestFavorite,
                    numFormat)
        }
    }
}