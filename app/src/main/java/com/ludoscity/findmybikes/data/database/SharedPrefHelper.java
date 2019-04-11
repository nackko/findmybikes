package com.ludoscity.findmybikes.data.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.ludoscity.findmybikes.R;
import com.ludoscity.findmybikes.data.database.bikesystem.BikeSystem;
import com.ludoscity.findmybikes.data.database.favorite.FavoriteEntityBase;
import com.ludoscity.findmybikes.data.database.favorite.FavoriteEntityPlace;
import com.ludoscity.findmybikes.data.database.favorite.FavoriteEntityStation;
import com.ludoscity.findmybikes.utils.InjectorUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by F8Full on 2015-04-02.
 * This file is part of BixiTrackExplorer
 * Helper class providing static method to save and retrieve data from storage
 * Internally, it uses both SharedPreferences and couchbase
 */
@SuppressWarnings("unchecked") //(List<QueryRow>) allDocs.get("rows");
public class SharedPrefHelper {

    private static SharedPrefHelper mInstance = null;

    public static SharedPrefHelper getInstance()
    {
        if(mInstance == null)
        {
            mInstance = new SharedPrefHelper();
        }

        return mInstance;
    }

    private static final String TAG = "SharedPrefHelper";
    private static final String mTRACKS_DB_NAME = "tracksdb";
    private static final String mSTATIONS_DB_NAME = "stationsdb";

    public static final String SHARED_PREF_FILENAME = "FindMyBikes_prefs";
    private static final String SHARED_PREF_VERSION_CODE = "FindMyBikes_prefs_version_code";

    private static final String PREF_SUFFIX_FAVORITES_JSONARRAY = "_favorites";

    private static final int PREF_CRITICAL_AVAILABILITY_MAX_DEFAULT = 1;
    private static final int PREF_BAD_AVAILABILITY_MAX_DEFAULT = 4;

    private static boolean mAutoUpdatePaused = false; //TODO: should be in model

    private SharedPrefHelper() {
    }

    public void init(Context context) throws PackageManager.NameNotFoundException {

        //Check for SharedPreferences versioning
        int sharedPrefVersion = context.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE).getInt(SHARED_PREF_VERSION_CODE, 0);
        PackageInfo pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        int currentVersionCode = pinfo.versionCode;

        if (sharedPrefVersion != currentVersionCode){
            SharedPreferences settings;
            SharedPreferences.Editor editor;

            settings = context.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);
            editor = settings.edit();

            boolean cleared = false;

            /*if (sharedPrefVersion == 0 && currentVersionCode >= 8){
                //Because the way favorites are saved changed

                editor.clear();
                editor.commit(); //I do want commit and not apply
                cleared = true;

            }

            if (!cleared && sharedPrefVersion <= 15 && currentVersionCode >= 16){
                //Changed formatting of favorites JSONArray
                //Those version numbers are beta so I guess it's ok to remove all favorites
                editor.clear();
                editor.commit(); //I do want commit and not apply
                cleared = true;
            }

            if (!cleared && sharedPrefVersion <= 24 && currentVersionCode >= 26 ){
                //reworked appbar title and subtitle
                editor.remove(PREF_CURRENT_BIKE_NETWORK_ID);
                editor.remove(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_NAME, context));
                editor.remove(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_HREF, context));
                editor.remove(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_CITY, context));
                editor.apply();
            }*/

            if (currentVersionCode > 68 && settings.contains(buildNetworkSpecificKey(PREF_SUFFIX_FAVORITES_JSONARRAY, context))){

                int i = 0;
                for(FavoriteEntityBase fav : getFavoriteAll(context))
                {
                    fav.setUiIndex(i);
                    InjectorUtils.Companion.provideRepository(context).addOrUpdateFavorite(fav);

                    ++i;
                }

                editor.remove(buildNetworkSpecificKey(PREF_SUFFIX_FAVORITES_JSONARRAY, context));
                editor.apply();
            }

            editor.putInt(SHARED_PREF_VERSION_CODE, currentVersionCode);
            editor.apply();
        }
    }

    //Scheduled for removal. Used for favorites conversion from SharedPref to Room
    //TODO: Add validation of IDs to handle the case were a favorite station been removed
    //Replace edit fab with red delete one
    private static List<FavoriteEntityBase> getFavoriteAll(Context _ctx){
        List<FavoriteEntityBase> toReturn = new ArrayList<>();

        SharedPreferences sp = _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        try {
            JSONArray favoritesJSONArray = new JSONArray(sp.getString(
                    buildNetworkSpecificKey(PREF_SUFFIX_FAVORITES_JSONARRAY, _ctx), "[]" ));

            //reverse iteration so that newly added favorites appear on top of the list
            for (int i=favoritesJSONArray.length()-1; i>=0; --i){

                JSONObject curFav = favoritesJSONArray.optJSONObject(i);

                if ( curFav != null){

                    if (curFav.getString("favorite_id").startsWith(FavoriteEntityPlace.PLACE_ID_PREFIX)){
                        toReturn.add(new FavoriteEntityPlace(
                                curFav.getString("favorite_id"),
                                i,
                                curFav.getString("display_name"),
                                InjectorUtils.Companion.provideRepository(_ctx).getCurrentBikeSystem().getValue().getId(),
                                new LatLng(curFav.getDouble("latitude"), curFav.getDouble("longitude")),
                                curFav.getString("attributions")));
                    }
                    else{
                        toReturn.add(new FavoriteEntityStation(curFav.getString("favorite_id"), i, curFav.getString("display_name"),
                                InjectorUtils.Companion.provideRepository(_ctx).getCurrentBikeSystem().getValue().getId()));
                    }
                }
            }

        } catch (JSONException e) {
            Log.d(TAG, "Error while loading favorites from prefs", e);
        }

        return toReturn;
    }


    public boolean getAutoUpdate(Context _ctx){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(_ctx);

        return !mAutoUpdatePaused && sp.getBoolean(_ctx.getString(R.string.pref_refresh_options_key), false);
    }

    public void pauseAutoUpdate() { mAutoUpdatePaused = true; }

    public void resumeAutoUpdate() { mAutoUpdatePaused = false; }

    public int getCriticalAvailabilityMax(Context _ctx) {
        SharedPreferences sp = _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        return sp.getInt(_ctx.getString(R.string.pref_critical_availability_max_key), PREF_CRITICAL_AVAILABILITY_MAX_DEFAULT);
    }

    public int getBadAvailabilityMax(Context _ctx) {

        SharedPreferences sp = _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        return sp.getInt(_ctx.getString(R.string.pref_bad_availability_max_key), PREF_BAD_AVAILABILITY_MAX_DEFAULT);

    }

    public void saveCriticalAvailabilityMax(Context _ctx, int _toSave) {
        _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE).edit()
                .putInt(_ctx.getString(R.string.pref_critical_availability_max_key), _toSave)
                .apply();
    }

    public void saveBadAvailabilityMax(Context _ctx, int _toSave) {
        _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE).edit()
                .putInt(_ctx.getString(R.string.pref_bad_availability_max_key), _toSave)
                .apply();
    }

    private static String buildNetworkSpecificKey(String suffix, Context ctx){
        BikeSystem system = InjectorUtils.Companion.provideRepository(ctx).getCurrentBikeSystem().getValue();

        if (system != null) {
            return InjectorUtils.Companion.provideRepository(ctx).getCurrentBikeSystem().getValue().getId() + suffix;
        } else {
            return "null";
        }
    }


    /*public static boolean gotTracks() throws CouchbaseLiteException {
        return mGotTracks;
    }*/
}
