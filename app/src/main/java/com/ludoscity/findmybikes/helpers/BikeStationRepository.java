package com.ludoscity.findmybikes.helpers;

import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;

import com.ludoscity.findmybikes.data.database.BikeStation;
import com.ludoscity.findmybikes.datamodel.FavoriteEntityBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by F8Full on 2017-12-26. This file is part of #findmybikes
 * A repo for bike stations. They are fetched from Room
 */
//TODO: delete this whole class
public class BikeStationRepository {

    private static BikeStationRepository mInstance = null;

    private Map<String,BikeStation> cachedStationMap;

    public static BikeStationRepository getInstance()
    {
        if(mInstance == null)
        {
            mInstance = new BikeStationRepository();
        }

        return mInstance;
    }

    public void addOrUpdateStation(final FavoriteEntityBase _favoriteEntity) {


        /*if(_favoriteEntity instanceof FavoriteEntityStation)
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

    private BikeStationRepository(){
        cachedStationMap = new HashMap<>();

        DBHelper.getInstance().getDatabase().bikeStationDao().getAll().observeForever(new Observer<List<BikeStation>>() {
            @Override
            public void onChanged(@Nullable List<BikeStation> bikeStations) {

                for (BikeStation station: bikeStations ) {
                    cachedStationMap.put(station.getLocationHash(), station);
                }
            }
        });
    }

    public void setAll(List<BikeStation> toSet){
        if(toSet != null)
        {
            DBHelper.getInstance().getDatabase().bikeStationDao().insertBikeStationList(toSet);
        }
        else
        {
            deleteStationList();
        }

    }

    public BikeStation getStation(final String _stationId){
        BikeStation toReturn = null;

        if (cachedStationMap != null){
            toReturn = cachedStationMap.get(_stationId);
        }

        return toReturn;
    }

    private void deleteStationList() {
        DBHelper.getInstance().getDatabase().bikeStationDao().deleteAllBikeStation();
    }

    public List<BikeStation> getStationList()
    {
        List<BikeStation> toReturn = DBHelper.getInstance().getDatabase().bikeStationDao().getAll().getValue();

        if (toReturn == null)
        {
            toReturn = new ArrayList<>();
        }

        return toReturn;
    }
}