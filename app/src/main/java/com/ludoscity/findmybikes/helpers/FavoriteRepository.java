package com.ludoscity.findmybikes.helpers;

import android.arch.lifecycle.LiveData;

import com.ludoscity.findmybikes.datamodel.FavoriteEntityBase;
import com.ludoscity.findmybikes.datamodel.FavoriteEntityStation;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Created by F8Full on 2017-12-26. This file is part of #findmybikes
 * A repo for favorites. They are fetched from Room
 */
public class FavoriteRepository {

    private static FavoriteRepository mInstance = null;

    public static FavoriteRepository getInstance()
    {
        if(mInstance == null)
        {
            mInstance = new FavoriteRepository();
        }

        return mInstance;
    }

    //private final FavoriteEntityStationDao mFavStationDao;

    //TODO: add an in memory cache ?

    //TODO: Build a cache of bikeStationList that have already been checked. Repo is perfect place !
    //This is called every time the list binds a station - There is a hidden 'favorite' button on each
    //BikeStation list element displayed in tab 'a' and tab 'B'
    //it happens a lot (every time user location is updated).
    //getFavoriteAll loads data from db
    public boolean isFavorite(String id) {
        return getFavoriteEntityStationForId(id).getValue() != null;
    }

    public @Nullable LiveData<FavoriteEntityStation> getFavoriteEntityStationForId(String favoriteId) {
        return DBHelper.getInstance().getDatabase().favoriteEntityStationDao().getForId(favoriteId);
    }

    public boolean hasAtleastNValidFavorites(String nearestBikeStationId, int n){
        Long count = DBHelper.getInstance().getDatabase().favoriteEntityStationDao().validFavoriteCount(nearestBikeStationId).getValue();

        return true;
        //return count != null && count  >= n;
    }

    public void removeFavorite(final String favIdToRemove)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DBHelper.getInstance().getDatabase().favoriteEntityStationDao().deleteOne(favIdToRemove);
            }
        }).start();
    }

    public void addOrUpdateFavorite(final FavoriteEntityBase _favoriteEntity) {


        if(_favoriteEntity instanceof FavoriteEntityStation)
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                        DBHelper.getInstance().getDatabase().favoriteEntityStationDao().insertOne((FavoriteEntityStation)_favoriteEntity);
                }
            }).start();
        }
            /*else    //_favoriteEntity instanceof FavoriteEntityPlace
            {
                if(isFavorite) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mDatabase.favoriteEntityPlaceDao().insertOne((FavoriteEntityPlace) _favoriteEntity);

                        }
                    }).start();

                }
                else
                    mDatabase.favoriteEntityPlaceDao().deleteOne(_favoriteEntity.getId());
            }*/
    }

    private FavoriteRepository(){}

    /*@Inject
    public FavoriteRepository(FavoriteEntityStationDao favStationDao){
        //this.mFavStationDao = favStationDao;
    }*/

    public void setAll(List<FavoriteEntityBase> toSet){

        List<FavoriteEntityStation> stationToSet = new ArrayList<>();

        for (FavoriteEntityBase fav:
             toSet) {
            if(fav instanceof FavoriteEntityStation){
                stationToSet.add((FavoriteEntityStation)fav);
            }
        }
        DBHelper.getInstance().getDatabase().favoriteEntityStationDao().insertAll(stationToSet);
    }

    public LiveData<List<FavoriteEntityStation>> getFavoriteStationList()
    {
        return DBHelper.getInstance().getDatabase().favoriteEntityStationDao().getAll();
    }
}
