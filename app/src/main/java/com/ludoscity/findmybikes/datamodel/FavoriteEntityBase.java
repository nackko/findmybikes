package com.ludoscity.findmybikes.datamodel;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spanned;

import com.google.android.gms.maps.model.LatLng;
import com.ludoscity.findmybikes.R;
import com.ludoscity.findmybikes.utils.Utils;

/**
 * Created by F8Full on 2017-12-23. This file is part of #findmybikes
 * A data model class to handle the concept of Favorite and save it using Room
 */

@Entity
public abstract class FavoriteEntityBase {

    @PrimaryKey
    @NonNull
    private String id;

    @ColumnInfo(name = "custom_name")
    private String customName;

    @ColumnInfo(name = "default_name")
    private /*final*/ String defaultName;

    @Ignore //so that Room don't persist it
    private boolean defaultNameWasSet = false;


    FavoriteEntityBase(@NonNull String id, String defaultName)
    {
        this.id = id;
        this.defaultName = defaultName;
        this.customName = null;
    }

    public abstract CharSequence getAttributions();
    public abstract LatLng getLocation();

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public boolean isDisplayNameDefault(){
        return customName == null;
    }

    //must be provided for Room generated code
    public String getDefaultName(){return defaultName;}
    //defaultName SHOULD be final. Can't because of Room generated code
    //so I have this tracking mechanism
    public void setDefaultName(String toSet) throws IllegalAccessException {
        if(defaultNameWasSet) {
            throw new IllegalAccessException("can't set default name more than once !!");
        }
        else{
            defaultName = toSet;
            defaultNameWasSet = true;
        }
    }

    public String getDisplayName(){
        String toReturn = customName;

        if (toReturn == null)
            toReturn = defaultName;

        return toReturn;
    }

    public Spanned getSpannedDisplayName(Context ctx, boolean favoriteDisplayNameOnly){

        Spanned toReturn = Utils.fromHtml(String.format(ctx.getString(R.string.favorite_display_name_only_italic),
                getDefaultName()));



        if (!isDisplayNameDefault()){
            if(favoriteDisplayNameOnly){
                toReturn = Utils.fromHtml(String.format(ctx.getString(R.string.favorite_display_name_only_bold),
                        getDisplayName()));
            } else {
                toReturn = Utils.fromHtml(String.format(ctx.getString(R.string.favorite_display_name_complete),
                        getDisplayName(), getDefaultName() ));
            }
        }

        return toReturn;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(@Nullable String customName) {
        this.customName = customName;
    }
}
