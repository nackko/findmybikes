package com.ludoscity.findmybikes.helpers;

import android.arch.lifecycle.LiveData;

import com.ludoscity.findmybikes.datamodel.FavoriteEntityBase;
import com.ludoscity.findmybikes.datamodel.FavoriteEntityPlace;
import com.ludoscity.findmybikes.datamodel.FavoriteEntityStation;

import java.util.ArrayList;
import java.util.List;

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

    public void removeFavorite(final String favIdToRemove)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (favIdToRemove.startsWith(FavoriteEntityPlace.PLACE_ID_PREFIX)){
                    DBHelper.getInstance().getDatabase().favoriteEntityPlaceDao().deleteOne(favIdToRemove);
                }
                else{
                    DBHelper.getInstance().getDatabase().favoriteEntityStationDao().deleteOne(favIdToRemove);
                }
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
        else    //_favoriteEntity instanceof FavoriteEntityPlace
        {
            new Thread(new Runnable() {
            @Override
            public void run() {
                    DBHelper.getInstance().getDatabase().favoriteEntityPlaceDao().insertOne((FavoriteEntityPlace)_favoriteEntity);
            }
            }).start();

        }
    }

    private FavoriteRepository(){}

    public void setAll(List<FavoriteEntityBase> toSet){

        List<FavoriteEntityStation> stationListToSet = new ArrayList<>();
        List<FavoriteEntityPlace> placeListToset = new ArrayList<>();

        for (FavoriteEntityBase fav:
             toSet) {
            if(fav instanceof FavoriteEntityStation){
                stationListToSet.add((FavoriteEntityStation)fav);
            }
            else{   //fav instanceof FavoriteEntityStation
                placeListToset.add((FavoriteEntityPlace)fav);
            }
        }

        DBHelper.getInstance().getDatabase().favoriteEntityStationDao().insertAll(stationListToSet);
        DBHelper.getInstance().getDatabase().favoriteEntityPlaceDao().insertAll(placeListToset);
    }

    public LiveData<List<FavoriteEntityStation>> getFavoriteStationList()
    {
        return DBHelper.getInstance().getDatabase().favoriteEntityStationDao().getAll();
    }

    public LiveData<List<FavoriteEntityPlace>> getFavoritePlaceList()
    {
        return DBHelper.getInstance().getDatabase().favoriteEntityPlaceDao().getAll();
    }
}
