package com.ludoscity.findmybikes.utils

import android.app.Application
import android.arch.lifecycle.LiveData
import android.content.Context
import com.google.android.gms.location.places.Place
import com.google.android.gms.maps.model.LatLng
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import com.ludoscity.findmybikes.data.database.FindMyBikesDatabase
import com.ludoscity.findmybikes.data.database.bikesystem.BikeSystem
import com.ludoscity.findmybikes.data.database.favorite.FavoriteEntityBase
import com.ludoscity.findmybikes.data.database.station.BikeStation
import com.ludoscity.findmybikes.data.network.BikeSystemListNetworkDataSource
import com.ludoscity.findmybikes.data.network.BikeSystemStatusNetworkDataSource
import com.ludoscity.findmybikes.data.network.twitter.TwitterNetworkDataExhaust
import com.ludoscity.findmybikes.ui.main.FindMyBikesActivityViewModel
import com.ludoscity.findmybikes.ui.main.FindMyBikesModelFactory
import com.ludoscity.findmybikes.ui.map.MapFragmentModelFactory
import com.ludoscity.findmybikes.ui.sheet.FavoriteSheetListFragmentModelFactory
import com.ludoscity.findmybikes.ui.table.TableFragmentModelFactory
import com.ludoscity.findmybikes.ui.trip.TripFragmentModelFactory
import java.text.NumberFormat

/**
 * Created by F8Full on 2019-02-15. This file is part of #findmybikes
 * Enables dependency injection by providing static methods to retrieve components. TODO: Dagger2
 */
class InjectorUtils {

    companion object {

        fun provideBikeSystemStatusNetworkDataSource(ctx: Context): BikeSystemStatusNetworkDataSource {
            // This call to provide repository is necessary if the app starts from a service - in this
            // case the repository will not exist unless it is specifically created.
            provideRepository(ctx)
            return BikeSystemStatusNetworkDataSource.getInstance()
        }

        fun provideTwitterNetworkDataExhaust(ctx: Context): TwitterNetworkDataExhaust {
            provideRepository(ctx)
            return TwitterNetworkDataExhaust.getInstance()
        }

        fun provideBikeSystemListNetworkDataSource(ctx: Context): BikeSystemListNetworkDataSource {
            provideRepository(ctx)
            return BikeSystemListNetworkDataSource.getInstance()
        }

        fun provideRepository(ctx: Context): FindMyBikesRepository {
            val database = FindMyBikesDatabase.getDatabase(ctx)

            val systemListNetworkDataSource = BikeSystemListNetworkDataSource.getInstance()
            val systemStatusNetworkDataSource = BikeSystemStatusNetworkDataSource.getInstance()
            val twitterNetworkDataExhaust = TwitterNetworkDataExhaust.getInstance()
            return FindMyBikesRepository.getInstance(database.bikeSystemDao(),
                    database.bikeStationDao(),
                    database.favoriteEntityPlaceDao(),
                    database.favoriteEntityStationDao(),
                    systemListNetworkDataSource,
                    systemStatusNetworkDataSource,
                    twitterNetworkDataExhaust)
        }

        fun provideMainActivityViewModelFactory(app: Application): FindMyBikesModelFactory {
            val repository = provideRepository(app.applicationContext)
            return FindMyBikesModelFactory(repository, app)
        }

        fun provideMapFragmentViewModelFactory(app: Application,
                                               hasLocationPermission: LiveData<Boolean>,
                                               isLookingForBike: LiveData<Boolean>,
                                               isDataOutOfDate: LiveData<Boolean>,
                                               userLoc: LiveData<LatLng>,
                                               stationA: LiveData<BikeStation>,
                                               stationB: LiveData<BikeStation>,
                                               finalDestPlace: LiveData<Place>,
                                               finalDestFavorite: LiveData<FavoriteEntityBase>
        ): MapFragmentModelFactory {
            val repository = provideRepository(app.applicationContext)
            return MapFragmentModelFactory(
                    repository,
                    app,
                    hasLocationPermission,
                    isLookingForBike,
                    isDataOutOfDate,
                    userLoc,
                    stationA,
                    stationB,
                    finalDestPlace,
                    finalDestFavorite
            )
        }

        fun provideTableFragmentViewModelFactory(app: Application,
                                                 isDockTable: Boolean,
                                                 appBarExpanded: LiveData<Boolean>,
                                                 dataOutOfDate: LiveData<Boolean>,
                                                 showProximityColumn: LiveData<Boolean>,
                                                 proximityHeaderFromResId: LiveData<Int>,
                                                 proximityHeaderToResId: LiveData<Int>,
                                                 stationRecapDatasource: LiveData<BikeStation>,
                                                 stationSelectionDatasource: LiveData<BikeStation>,
                                                 distToUserComparatorSource: LiveData<FindMyBikesActivityViewModel.DistanceComparator>,
                                                 totalTripComparatorSource: LiveData<FindMyBikesActivityViewModel.TotalTripTimeComparator>,
                                                 numFormat: NumberFormat): TableFragmentModelFactory {
            val repository = provideRepository(app.applicationContext)
            return TableFragmentModelFactory(repository, app, isDockTable,
                    appBarExpanded,
                    stationRecapDatasource,
                    stationSelectionDatasource,
                    dataOutOfDate,
                    showProximityColumn,
                    proximityHeaderFromResId,
                    proximityHeaderToResId,
                    distToUserComparatorSource,
                    totalTripComparatorSource,
                    numFormat)
        }

        fun provideFavoriteSheetListFragmentViewModelFactory(app: Application,
                                                             isSheetEditInProgress: LiveData<Boolean>,
                                                             curBikeSystemDataSource: LiveData<BikeSystem>): FavoriteSheetListFragmentModelFactory {
            val repo = provideRepository(app.applicationContext)

            return FavoriteSheetListFragmentModelFactory(repo, app, isSheetEditInProgress, curBikeSystemDataSource)
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