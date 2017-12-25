package com.ludoscity.findmybikes.viewmodels;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import com.ludoscity.findmybikes.datamodel.FavoriteEntityBase;
import com.ludoscity.findmybikes.helpers.DBHelper;

import java.util.List;

/**
 * Created by F8Full on 2017-12-24. This file is part of #findmybikes
 * ViewModel for handling favoritelistFragment data prep for UI and business logic
 */

public class FavoriteListViewModel extends ViewModel {
    private MutableLiveData<List<FavoriteEntityBase>> mFavoriteEntityBaseList = new MutableLiveData<>();

    public LiveData<List<FavoriteEntityBase>> getFavoriteEntityBaseList(){
        return mFavoriteEntityBaseList;
    }

    public void setFavoriteEntityBaseList(List<FavoriteEntityBase> toSet){
        mFavoriteEntityBaseList.setValue(toSet);
    }

    public void removeFavorite(FavoriteEntityBase toRemove){
        DBHelper.updateFavorite(false, toRemove);
    }

    public void addFavorite(final FavoriteEntityBase toAdd){
        DBHelper.updateFavorite(true, toAdd);
    }


}
