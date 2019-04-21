package com.ludoscity.findmybikes.ui.sheet

import android.app.Application
import android.graphics.Typeface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import com.ludoscity.findmybikes.data.database.bikesystem.BikeSystem
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
                                 private val sheetEditInProgress: LiveData<Boolean>,
                                 curBikeSystemDataSource: LiveData<BikeSystem>,
                                 app: Application) : AndroidViewModel(app) {

    private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)
    private val coroutineScopeMain = CoroutineScope(Dispatchers.Main)

    //What we need to expose to fragment for consumption
    val sheetItemDataList: LiveData<List<FavoriteSheetItemData>>
        get() = sheetItemList

    private val sheetItemList: MutableLiveData<List<FavoriteSheetItemData>> = MutableLiveData()

    private lateinit var favoriteStationListDataSource: LiveData<List<FavoriteEntityStation>>
    private lateinit var favoritePlaceListDataSource: LiveData<List<FavoriteEntityPlace>>

    private lateinit var favoriteStationDataObserver: Observer<List<FavoriteEntityStation>>
    private lateinit var favoritePlaceDataObserver: Observer<List<FavoriteEntityPlace>>
    private val favoriteSheetInProgressObserver: Observer<Boolean>
    private val curBikeSystemObserver: Observer<BikeSystem>

    //convenience
    private val mergeFavoriteEntityList = MutableLiveData<List<FavoriteEntityBase>>()
    //TODO: replug selection ?
    private val selectedFavoriteEntityId = MutableLiveData<String>()

    init {

        curBikeSystemObserver = Observer { curBikeSystem ->
            curBikeSystem?.let {
                coroutineScopeIO.launch {

                    favoriteStationListDataSource = repo.getFavoriteStationList()
                    favoritePlaceListDataSource = repo.getFavoritePlaceList()

                    coroutineScopeMain.launch {
                        favoriteStationListDataSource.observeForever(favoriteStationDataObserver)
                        favoritePlaceListDataSource.observeForever(favoritePlaceDataObserver)
                    }
                }
            }
        }

        favoriteStationDataObserver = Observer { favStationData ->
            favStationData?.let {
                coroutineScopeIO.launch {

                    val merged = emptyList<FavoriteEntityBase>().toMutableList()

                    merged.addAll(it)
                    merged.addAll(favoritePlaceListDataSource.value ?: emptyList())

                    merged.sortByDescending { favorite -> favorite.uiIndex }

                    mergeFavoriteEntityList.postValue(merged)
                }
            }
        }

        favoritePlaceDataObserver = Observer {
            it?.let {
                coroutineScopeIO.launch {
                    val merged = emptyList<FavoriteEntityBase>().toMutableList()

                    merged.addAll(it)
                    merged.addAll(favoriteStationListDataSource.value ?: emptyList())

                    merged.sortByDescending { favorite -> favorite.uiIndex }

                    mergeFavoriteEntityList.postValue(merged)
                }
            }
        }

        curBikeSystemDataSource.observeForever(curBikeSystemObserver)

        favoriteSheetInProgressObserver = Observer {

            if (it == true) {
                coroutineScopeIO.launch {

                    val newFavoriteDisplayData = ArrayList<FavoriteSheetItemData>()

                    mergeFavoriteEntityList.value?.forEach { fav ->

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

                    sheetItemList.postValue(newFavoriteDisplayData)
                }
            }
        }

        sheetEditInProgress.observeForever(favoriteSheetInProgressObserver)

        mergeFavoriteEntityList.observeForever {


            coroutineScopeIO.launch {
                val newFavoriteDisplayData = ArrayList<FavoriteSheetItemData>()

                mergeFavoriteEntityList.value?.forEach { fav ->

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

                sheetItemList.postValue(newFavoriteDisplayData)
            }
        }
    }

    fun isFavorite(id: String): Boolean {
        return repo.isFavoriteId(id)
    }

    fun hasAtleastNValidFavorites(nearestBikeStation: BikeStation?, n: Int): Boolean {
        return repo.hasAtLeastNValidStationFavorites(nearestBikeStation?.locationHash ?: "null", n)
    }

    fun removeFavorite(favIdToRemove: String) {
        repo.removeFavoriteByFavoriteId(favIdToRemove)
    }

    fun updateFavoriteCustomNameByFavoriteId(favoriteIdToUpdate: String, newCustomName: String) {
        repo.updateFavoriteCustomNameByFavoriteId(favoriteIdToUpdate, newCustomName)
    }

    fun updateFavoriteUiIndexByFavoriteId(favoriteIdToUpdate: String, newUiIndex: Int) {
        repo.updateFavoriteUiIndexByFavoriteId(favoriteIdToUpdate, newUiIndex)
    }
}
