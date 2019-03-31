package com.ludoscity.findmybikes.ui.sheet

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.graphics.Typeface
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import com.ludoscity.findmybikes.data.database.favorite.FavoriteEntityBase
import com.ludoscity.findmybikes.data.database.favorite.FavoriteEntityPlace
import com.ludoscity.findmybikes.data.database.favorite.FavoriteEntityStation
import com.ludoscity.findmybikes.data.database.station.BikeStation
import com.ludoscity.findmybikes.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by F8Full on 2017-12-24. This file is part of #findmybikes
 * ViewModel for handling favoritelistFragment data prep for UI and business logic
 */

class FavoriteSheetListViewModel(private val repo: FindMyBikesRepository,
                                 val sheetEditInProgress: LiveData<Boolean>,
                                 app: Application) : AndroidViewModel(app) {

    private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)

    //What we need to expose to fragment for consumption
    val sheetItemDataList: LiveData<List<FavoriteSheetItemData>>
        get() = sheetItemList

    private val sheetItemList: MutableLiveData<List<FavoriteSheetItemData>> = MutableLiveData()

    private val favoriteStationListDataSource: LiveData<List<FavoriteEntityStation>>
    private val favoritePlaceListDataSource: LiveData<List<FavoriteEntityPlace>>

    private val favoriteStationDataObserver: Observer<List<FavoriteEntityStation>>
    private val favoritePlaceDataObserver: Observer<List<FavoriteEntityPlace>>
    private val favoriteSheetInProgressObserver: Observer<Boolean>

    //convenience
    private val mergeFavoriteEntityList = MutableLiveData<List<FavoriteEntityBase>>()
    private val selectedFavoriteEntityId = MutableLiveData<String>()

    ////////////////////////////////////////////
    //old code
    private val mFavoriteList = MutableLiveData<List<FavoriteEntityBase>>()

    val favoriteEntityList: LiveData<List<FavoriteEntityBase>>
        get() = mFavoriteList

    //TODO: this bugs me, I hope setValue is thread safe. Revisit this
    //android app architecture guide : https://developer.android.com/topic/libraries/architecture/guide.html
    init {

        //sheetEditInProgress.value = false

        favoriteSheetInProgressObserver = Observer {

            val sortedFavoriteList = mergeFavoriteEntityList.value?.toMutableList()
                    ?: emptyList<FavoriteEntityBase>().sortedWith(
                            compareBy {
                                it.uiIndex
                            }
                    )

            val newFavoriteDisplayData = ArrayList<FavoriteSheetItemData>()
            //TODO: in backgorund

            sortedFavoriteList.forEach { fav ->

                val sheeEditInProgress = it == true

                newFavoriteDisplayData.add(FavoriteSheetItemData(
                        R.color.theme_accent_transparent,
                        sheeEditInProgress,
                        !sheeEditInProgress,
                        sheeEditInProgress,
                        when (sheeEditInProgress) {
                            true -> Utils.getPercentResource(getApplication(),
                                    R.dimen.favorite_name_width_sheet_editing,
                                    true)
                            false -> Utils.getPercentResource(getApplication(),
                                    R.dimen.favorite_name_width_no_sheet_editing,
                                    true)
                        },
                        fav.displayName,
                        if (fav.isDisplayNameDefault) Typeface.ITALIC else Typeface.BOLD,
                        fav.id
                ))
            }

            sheetItemList.value = newFavoriteDisplayData

        }

        sheetEditInProgress.observeForever(favoriteSheetInProgressObserver)



        mergeFavoriteEntityList.observeForever { newData ->
            //TODO: emit data for display
            val sortedFavoriteList = newData?.toMutableList()
                    ?: emptyList<FavoriteEntityBase>().sortedWith(
                            compareBy {
                                it.uiIndex
                            }
                    )

            val newFavoriteDisplayData = ArrayList<FavoriteSheetItemData>()
            //TODO: in backgorund

            sortedFavoriteList.forEach { fav ->

                val sheeEditInProgress = sheetEditInProgress.value == true

                newFavoriteDisplayData.add(FavoriteSheetItemData(
                        R.color.theme_accent_transparent,
                        sheeEditInProgress,
                        !sheeEditInProgress,
                        sheeEditInProgress,
                        when (sheeEditInProgress) {
                            true -> Utils.getPercentResource(getApplication(),
                                    R.dimen.favorite_name_width_sheet_editing,
                                    true)
                            false -> Utils.getPercentResource(getApplication(),
                                    R.dimen.favorite_name_width_no_sheet_editing,
                                    true)
                        },
                        fav.displayName,
                        if (fav.isDisplayNameDefault) Typeface.ITALIC else Typeface.BOLD,
                        fav.id
                ))
            }

            sheetItemList.value = newFavoriteDisplayData
        }

        favoriteStationListDataSource = repo.getFavoriteStationList()
        favoritePlaceListDataSource = repo.getFavoritePlaceList()

        favoriteStationDataObserver = Observer {
            coroutineScopeIO.launch {
                //TODO: do merging

                mergeFavoriteEntityList.postValue(it)
            }
        }

        favoriteStationListDataSource.observeForever(favoriteStationDataObserver)

        favoritePlaceDataObserver = Observer {
            coroutineScopeIO.launch {
                //TODO: merging

                mergeFavoriteEntityList.postValue(it)
            }
        }

        favoritePlaceListDataSource.observeForever(favoritePlaceDataObserver)


        //////////////////////3
        //old code
        /*FavoriteRepository.getInstance().favoriteStationList.observeForever { favoriteEntityStations ->
            val oldList = mFavoriteList.value

            val mergedList = ArrayList<FavoriteEntityBase>()

            val toPurge = ArrayList<FavoriteEntityBase>()

            /*if (oldList != null) {
                for (fav in oldList) {

                    if (!favoriteEntityStations!!.contains(fav) && !fav.id.startsWith(FavoriteEntityPlace.PLACE_ID_PREFIX)) {
                        toPurge.add(fav)
                    }
                }

                oldList.removeAll(toPurge)
                mergedList.addAll(oldList)
            }*/

            //mergedList.addAll(favoriteEntityStations)

            val Unique_set = HashSet(mergedList)

            val mergedListUnique = ArrayList(Unique_set)

            val mergedUniqueListForCurrentBikeSystem = ArrayList<FavoriteEntityBase>()

            for (fav in mergedListUnique) {
                //TODO: replug merging with current bike system id from REPO
                if (fav.bikeSystemId.equals(mNearbyActivityViewModel!!.getCurrentBikeSytemId().value!!, ignoreCase = true)) {
                    mergedUniqueListForCurrentBikeSystem.add(fav)
                }
            }

            mFavoriteList.setValue(mergedUniqueListForCurrentBikeSystem)
        }*/

        /*FavoriteRepository.getInstance().favoritePlaceList.observeForever { favoriteEntityPlaces ->
            val oldList = mFavoriteList.value

            val mergedList = ArrayList<FavoriteEntityBase>()

            val toPurge = ArrayList<FavoriteEntityBase>()

            /*if (oldList != null) {
                for (fav in oldList) {
                    if (!favoriteEntityPlaces!!.contains(fav) && fav.id.startsWith(FavoriteEntityPlace.PLACE_ID_PREFIX)) {
                        toPurge.add(fav)
                    }
                }
                oldList.removeAll(toPurge)
                mergedList.addAll(oldList)
            }

            mergedList.addAll(favoriteEntityPlaces)*/

            val Unique_set = HashSet(mergedList)

            val mergedListUnique = ArrayList(Unique_set)

            val mergedUniqueListForCurrentBikeSystem = ArrayList<FavoriteEntityBase>()

            for (fav in mergedListUnique) {
                //TODO: replug merging with current bike system id from REPO
                //if (fav.bikeSystemId.equals(mNearbyActivityViewModel!!.getCurrentBikeSytemId().value!!, ignoreCase = true)) {
                    mergedUniqueListForCurrentBikeSystem.add(fav)
                //}
            }

            mFavoriteList.setValue(mergedUniqueListForCurrentBikeSystem)
        }*/
    }

    /*fun getFavoriteEntityStationLiveDataForId(favoriteId: String): LiveData<FavoriteEntityStation>? {
        return repo.getFavoriteStationByFavoriteId(favoriteId)
    }

    fun getFavoriteEntityPlaceLiveDataForId(favoriteId: String): LiveData<FavoriteEntityPlace>? {
        return repo.getFavoritePlaceByFavoriteId(favoriteId)
    }*/

    fun isFavorite(id: String): Boolean {
        return repo.isFavoriteId(id)
    }

    fun hasAtleastNValidFavorites(nearestBikeStation: BikeStation?, n: Int): Boolean {
        return repo.hasAtLeastNValidStationFavorites(nearestBikeStation?.locationHash ?: "null", n)
    }

    fun removeFavorite(favIdToRemove: String) {
        repo.removeFavoriteByFavoriteId(favIdToRemove)
    }

    fun addFavorite(toAdd: FavoriteEntityBase) {

        if (toAdd.uiIndex == -1)
            toAdd.uiIndex = mFavoriteList.value!!.size

        repo.addOrUpdateFavorite(toAdd)
    }

    fun updateFavoriteCustomNameByFavoriteId(favoriteIdToUpdate: String, newCustomName: String) {
        repo.updateFavoriteCustomNameByFavoriteId(favoriteIdToUpdate, newCustomName)
    }

    fun updateFavoriteUiIndexByFavoriteId(favoriteIdToUpdate: String, newUiIndex: Int) {
        repo.updateFavoriteUiIndexByFavoriteId(favoriteIdToUpdate, newUiIndex)
    }
}
