package com.ludoscity.findmybikes.viewmodels;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;

import com.ludoscity.findmybikes.datamodel.FavoriteEntityBase;
import com.ludoscity.findmybikes.datamodel.FavoriteEntityStation;
import com.ludoscity.findmybikes.helpers.FavoriteRepository;

import java.util.List;

/**
 * Created by F8Full on 2017-12-24. This file is part of #findmybikes
 * ViewModel for handling favoritelistFragment data prep for UI and business logic
 */

public class FavoriteListViewModel extends ViewModel {

    //private LiveData<List<FavoriteEntityStation>> mFavoriteEntityStationList;
    //private FavoriteRepository mFavRepo;

    /*@Inject
    public FavoriteListViewModel(FavoriteRepository favRepo)
    {
        this.mFavRepo = favRepo;
        this.mFavoriteEntityStationList = favRepo.getFavoriteStationList();
    }*/

    public LiveData<List<FavoriteEntityStation>> getFavoriteEntityStationList(){
        return FavoriteRepository.getInstance().getFavoriteStationList();
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
        FavoriteRepository.getInstance().addOrUpdateFavorite(toAdd);
    }

    public void updateFavorite(final FavoriteEntityBase updatedFavorite){
        addFavorite(updatedFavorite);
    }
}
