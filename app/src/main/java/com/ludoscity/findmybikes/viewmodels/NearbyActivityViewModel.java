package com.ludoscity.findmybikes.viewmodels;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

/**
 * Created by F8Full on 2017-12-24. This file is part of #findmybikes
 * ViewModel for handling favoritelistFragment data prep for UI and business logic
 */

public class NearbyActivityViewModel extends ViewModel {
    private MutableLiveData<Boolean> mFavoriteFabShown = new MutableLiveData<>();
    private MutableLiveData<Boolean> mFavoriteSheetShown = new MutableLiveData<>();
    private MutableLiveData<Boolean> mFavoriteItemNameEditInProgress = new MutableLiveData<>();
    private MutableLiveData<Boolean> mFavoriteSheetEditInProgress = new MutableLiveData<>();
    private MutableLiveData<Boolean> mFavoriteSheetEditFabShown = new MutableLiveData<>();

    public void showFavoriteFab(){
        mFavoriteFabShown.setValue(true);
    }
    public void showFavoriteSheet(){
        mFavoriteSheetShown.setValue(true);
    }
    public void favoriteItemNameEditStart() { mFavoriteItemNameEditInProgress.setValue(true);}
    public void favoriteSheetEditStart() { mFavoriteSheetEditInProgress.setValue(true);}

    public void showFavoriteSheetEditFab(){
        mFavoriteSheetEditFabShown.setValue(true);
    }

    public void hideFavoriteFab(){
        mFavoriteFabShown.setValue(false);
    }
    public void hideFavoriteSheet(){
        mFavoriteSheetShown.setValue(false);
    }
    public void favoriteItemNameEditStop() { mFavoriteItemNameEditInProgress.setValue(false);}
    public void favoriteSheetEditStop() { mFavoriteSheetEditInProgress.setValue(false);}
    public void hideFavoriteSheetEditFab(){
        mFavoriteSheetEditFabShown.setValue(false);
    }

    public LiveData<Boolean> isFavoriteFabShown(){
        return mFavoriteFabShown;
    }
    public LiveData<Boolean> isFavoriteSheetShown(){
        if(mFavoriteSheetShown == null){
            showFavoriteFab();
        }

        return mFavoriteSheetShown;
    }

    public LiveData<Boolean> isFavoriteSheetEditfabShown(){
        if(mFavoriteSheetEditFabShown == null){
            showFavoriteSheetEditFab();
        }

        return mFavoriteSheetEditFabShown;
    }

    public LiveData<Boolean> isFavoriteSheetItemNameEditInProgress(){
        return mFavoriteItemNameEditInProgress;
    }

    public LiveData<Boolean> isFavoriteSheetEditInProgress(){
        return mFavoriteSheetEditInProgress;
    }

}
