package com.ludoscity.findmybikes.viewmodels;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.Nullable;

import com.ludoscity.findmybikes.datamodel.FavoriteEntityBase;
import com.ludoscity.findmybikes.datamodel.FavoriteEntityStation;
import com.ludoscity.findmybikes.helpers.DBHelper;
import com.ludoscity.findmybikes.helpers.FavoriteRepository;

import java.util.List;

/**
 * Created by F8Full on 2017-12-24. This file is part of #findmybikes
 * ViewModel for handling favoritelistFragment data prep for UI and business logic
 */

public class FavoriteListViewModel extends ViewModel {

    private MutableLiveData<List<FavoriteEntityStation>> mFavoriteStationList = new MutableLiveData<>();

    //private LiveData<List<FavoriteEntityStation>> mFavoriteEntityStationList;
    //private FavoriteRepository mFavRepo;

    //TODO: this bugs me, I hope setValue is thread safe. Revisit this
    //android app architecture guide : https://developer.android.com/topic/libraries/architecture/guide.html
    public FavoriteListViewModel(){
        FavoriteRepository.getInstance().getFavoriteStationList().observeForever(new Observer<List<FavoriteEntityStation>>() {
            @Override
            public void onChanged(@Nullable List<FavoriteEntityStation> favoriteEntityStations) {
                mFavoriteStationList.setValue(favoriteEntityStations);
            }
        });
    }

    public @Nullable LiveData<FavoriteEntityStation> getFavoriteEntityStationForId(String favoriteId) {
        return DBHelper.getInstance().getDatabase().favoriteEntityStationDao().getForId(favoriteId);
    }

    public @Nullable FavoriteEntityBase getFavoriteEntityForId(String favoriteId){
        FavoriteEntityBase toReturn = null;

        for (FavoriteEntityBase fav : mFavoriteStationList.getValue()){
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

        for (FavoriteEntityBase fav : mFavoriteStationList.getValue()){

            if (fav.getId().equalsIgnoreCase(id)){
                toReturn = true;
                break;
            }
        }

        return toReturn;
    }



    /*@Inject
    public FavoriteListViewModel(FavoriteRepository favRepo)
    {
        this.mFavRepo = favRepo;
        this.mFavoriteEntityStationList = favRepo.getFavoriteStationList();
    }*/

    /*public LiveData<List<FavoriteEntityStation>> getFavoriteEntityStationList(){
        return FavoriteRepository.getInstance().getFavoriteStationList();
    }*/

    public LiveData<List<FavoriteEntityStation>> getFavoriteEntityStationList(){
        return mFavoriteStationList;
    }

    /*public void setFavoriteEntityBaseList(List<FavoriteEntityBase> toSet){
        mFavoriteEntityBaseList.setValue(toSet);
    }*/

    //Those are are and forwarding to repo because they imply a modification of the list
    //TODO: When adding FavoriteEntityPlace, have a single LiveData<List<FavoriteEntityBase>>
    //maintained in the model and observing the two lists that will exist in the repo
    public void removeFavorite(String favIdToRemove){
        FavoriteRepository.getInstance().removeFavorite(favIdToRemove);
    }

    public void addFavorite(final FavoriteEntityBase toAdd){

        if(toAdd.getUiIndex() == -1)
            toAdd.setUiIndex(mFavoriteStationList.getValue().size());

        FavoriteRepository.getInstance().addOrUpdateFavorite(toAdd);
    }

    public void updateFavorite(final FavoriteEntityBase updatedFavorite){
        addFavorite(updatedFavorite);
    }
}
