package com.ludoscity.findmybikes.viewmodels;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.Nullable;

import com.ludoscity.findmybikes.datamodel.FavoriteEntityBase;
import com.ludoscity.findmybikes.datamodel.FavoriteEntityPlace;
import com.ludoscity.findmybikes.datamodel.FavoriteEntityStation;
import com.ludoscity.findmybikes.helpers.DBHelper;
import com.ludoscity.findmybikes.helpers.FavoriteRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by F8Full on 2017-12-24. This file is part of #findmybikes
 * ViewModel for handling favoritelistFragment data prep for UI and business logic
 */

public class FavoriteListViewModel extends ViewModel {

    private MutableLiveData<List<? extends FavoriteEntityBase>> mFavoriteList = new MutableLiveData<>();

    //TODO: this bugs me, I hope setValue is thread safe. Revisit this
    //android app architecture guide : https://developer.android.com/topic/libraries/architecture/guide.html
    public FavoriteListViewModel(){
        FavoriteRepository.getInstance().getFavoriteStationList().observeForever(new Observer<List<FavoriteEntityStation>>() {
            @Override
            public void onChanged(@Nullable List<FavoriteEntityStation> favoriteEntityStations) {

                List<? extends FavoriteEntityBase> oldList = mFavoriteList.getValue();

                List<FavoriteEntityBase> mergedList = new ArrayList<>();

                List<FavoriteEntityBase> toPurge = new ArrayList<>();

                if (oldList != null) {
                    for (FavoriteEntityBase fav : oldList)
                    {
                        if (!favoriteEntityStations.contains(fav) && !fav.getId().startsWith(FavoriteEntityPlace.PLACE_ID_PREFIX))
                        {
                            toPurge.add(fav);
                        }
                    }
                    oldList.removeAll(toPurge);
                    mergedList.addAll(oldList);
                }

                mergedList.addAll(favoriteEntityStations);

                Set<FavoriteEntityBase> Unique_set = new HashSet<>(mergedList);

                mFavoriteList.setValue(new ArrayList<>(Unique_set));
            }
        });

        FavoriteRepository.getInstance().getFavoritePlaceList().observeForever(new Observer<List<FavoriteEntityPlace>>() {
            @Override
            public void onChanged(@Nullable List<FavoriteEntityPlace> favoriteEntityPlaces) {
                List<? extends FavoriteEntityBase> oldList = mFavoriteList.getValue();

                List<FavoriteEntityBase> mergedList = new ArrayList<>();

                List<FavoriteEntityBase> toPurge = new ArrayList<>();

                if (oldList != null) {
                    for (FavoriteEntityBase fav : oldList)
                    {
                        if (!favoriteEntityPlaces.contains(fav) && fav.getId().startsWith(FavoriteEntityPlace.PLACE_ID_PREFIX))
                        {
                            toPurge.add(fav);
                        }
                    }
                    oldList.removeAll(toPurge);
                    mergedList.addAll(oldList);
                }

                mergedList.addAll(favoriteEntityPlaces);

                Set<FavoriteEntityBase> Unique_set = new HashSet<>(mergedList);

                mFavoriteList.setValue(new ArrayList<>(Unique_set));
            }
        });
    }

    public @Nullable LiveData<FavoriteEntityStation> getFavoriteEntityStationLiveDataForId(String favoriteId) {
        return DBHelper.getInstance().getDatabase().favoriteEntityStationDao().getForId(favoriteId);
    }

    public @Nullable LiveData<FavoriteEntityPlace> getFavoriteEntityPlaceLiveDataForId(String favoriteId) {
        return DBHelper.getInstance().getDatabase().favoriteEntityPlaceDao().getForId(favoriteId);
    }


    public @Nullable FavoriteEntityBase getFavoriteEntityForId(String favoriteId){
        FavoriteEntityBase toReturn = null;

        for (FavoriteEntityBase fav : mFavoriteList.getValue()){
            if (fav.getId().equalsIgnoreCase(favoriteId)){
                toReturn = fav;
                break;
            }
        }

        return toReturn;
    }

    //TODO: Build a cache of bikeStationList that have already been checked. Repo is perfect place !
    //This is called every time the list binds a station - There is a hidden 'favorite' button on each
    //BikeStation list element displayed in tab 'a' and tab 'B'
    //it happens a lot (every time user location is updated).
    //getFavoriteAll loads data from db
    public boolean isFavorite(String id) {

        boolean toReturn = false;

        for (FavoriteEntityBase fav : mFavoriteList.getValue()){

            if (fav.getId().equalsIgnoreCase(id)){
                toReturn = true;
                break;
            }
        }

        return toReturn;
    }

    public LiveData<List<? extends FavoriteEntityBase>> getFavoriteEntityList(){
        return mFavoriteList;
    }

    public void removeFavorite(String favIdToRemove){
        FavoriteRepository.getInstance().removeFavorite(favIdToRemove);
    }

    public void addFavorite(final FavoriteEntityBase toAdd){

        if(toAdd.getUiIndex() == -1)
            toAdd.setUiIndex(mFavoriteList.getValue().size());

        FavoriteRepository.getInstance().addOrUpdateFavorite(toAdd);
    }

    public void updateFavorite(final FavoriteEntityBase updatedFavorite){
        addFavorite(updatedFavorite);
    }
}
