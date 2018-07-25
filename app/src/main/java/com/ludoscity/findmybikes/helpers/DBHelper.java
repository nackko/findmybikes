package com.ludoscity.findmybikes.helpers;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.ludoscity.findmybikes.R;
import com.ludoscity.findmybikes.citybik_es.model.BikeNetworkDesc;
import com.ludoscity.findmybikes.datamodel.FavoriteEntityBase;
import com.ludoscity.findmybikes.datamodel.FavoriteEntityPlace;
import com.ludoscity.findmybikes.datamodel.FavoriteEntityStation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.ludoscity.findmybikes.helpers.AppDatabase.MIGRATION_1_2;
import static com.ludoscity.findmybikes.helpers.AppDatabase.MIGRATION_2_3;
import static com.ludoscity.findmybikes.helpers.AppDatabase.MIGRATION_3_4;
import static com.ludoscity.findmybikes.helpers.AppDatabase.MIGRATION_4_5;

/**
 * Created by F8Full on 2015-04-02.
 * This file is part of BixiTrackExplorer
 * Helper class providing static method to save and retrieve data from storage
 * Internally, it uses both SharedPreferences and couchbase
 */
@SuppressWarnings("unchecked") //(List<QueryRow>) allDocs.get("rows");
public class DBHelper {

    private static DBHelper mInstance = null;

    public static DBHelper getInstance()
    {
        if(mInstance == null)
        {
            mInstance = new DBHelper();
        }

        return mInstance;
    }

    private  static final String TAG = "DBHelper";
    private static AppDatabase mDatabase = null;
    private static final String mTRACKS_DB_NAME = "tracksdb";

    private static final String mSTATIONS_DB_NAME = "stationsdb";

    private static final String PREF_LAST_SAVE_CORRUPTED = "last_save_corrupted";
    private static boolean mSaving = false;

    static final String SHARED_PREF_FILENAME = "FindMyBikes_prefs";
    private static final String SHARED_PREF_VERSION_CODE = "FindMyBikes_prefs_version_code";

    private static final String PREF_CURRENT_BIKE_NETWORK_ID = "current_bike_network_id";

    private static final String PREF_SUFFIX_FAVORITES_JSONARRAY = "_favorites";
    private static final String PREF_SUFFIX_WEBTASK_LAST_TIMESTAMP_MS = "_last_refresh_timestamp";
    private static final String PREF_SUFFIX_NETWORK_NAME = "_network_name";
    private static final String PREF_SUFFIX_NETWORK_HREF = "_network_href";
    private static final String PREF_SUFFIX_NETWORK_CITY = "_network_city";
    private static final String PREF_SUFFIX_NETWORK_BOUNDS_SW_LATITUDE = "_network_bounds_sw_lat";
    private static final String PREF_SUFFIX_NETWORK_BOUNDS_SW_LONGITUDE = "_network_bounds_sw_lng";
    private static final String PREF_SUFFIX_NETWORK_BOUNDS_NE_LATITUDE = "_network_bounds_ne_lat";
    private static final String PREF_SUFFIX_NETWORK_BOUNDS_NE_LONGITUDE = "_network_bounds_ne_lng";

    private static final int PREF_CRITICAL_AVAILABILITY_MAX_DEFAULT = 1;
    private static final int PREF_BAD_AVAILABILITY_MAX_DEFAULT = 4;

    private static boolean mAutoUpdatePaused = false; //TODO: should be in model

    private DBHelper() {}

    public void init(Context context) throws IOException, PackageManager.NameNotFoundException {
        mDatabase = Room.databaseBuilder(context, AppDatabase.class, "findmybikes-database")
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
                .build();

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
                    FavoriteRepository.getInstance().addOrUpdateFavorite(fav);

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
                                curFav.getString("display_name"),
                                getBikeNetworkId(_ctx),
                                new LatLng(curFav.getDouble("latitude"), curFav.getDouble("longitude")),
                                curFav.getString("attributions")));
                    }
                    else{
                        toReturn.add(new FavoriteEntityStation(curFav.getString("favorite_id"), curFav.getString("display_name"), getBikeNetworkId(_ctx)));
                    }
                }
            }

        } catch (JSONException e) {
            Log.d(TAG, "Error while loading favorites from prefs", e);
        }

        return toReturn;
    }


    public AppDatabase getDatabase(){
        return mDatabase;
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

    public long getLastUpdateTimestamp(Context ctx){

        return ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE)
                .getLong(buildNetworkSpecificKey(PREF_SUFFIX_WEBTASK_LAST_TIMESTAMP_MS, ctx), 0);
    }

    public void saveLastUpdateTimestampAsNow(Context ctx){

        ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE).edit()
                .putLong(buildNetworkSpecificKey(PREF_SUFFIX_WEBTASK_LAST_TIMESTAMP_MS, ctx),
                        Calendar.getInstance().getTimeInMillis()).apply();
    }

    public boolean isBikeNetworkIdAvailable(Context ctx){

        return !getBikeNetworkId(ctx).equalsIgnoreCase("");
    }

    public String getBikeNetworkName(Context ctx){

        return ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE)
                .getString(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_NAME, ctx), "");
    }

    public String getHashtaggableNetworkName(Context _ctx){

        String hashtagable_bikeNetworkName = getBikeNetworkName(_ctx);
        hashtagable_bikeNetworkName = hashtagable_bikeNetworkName.replaceAll("\\s","");
        hashtagable_bikeNetworkName = hashtagable_bikeNetworkName.replaceAll("[^A-Za-z0-9 ]", "");
        hashtagable_bikeNetworkName = hashtagable_bikeNetworkName.toLowerCase();

        return hashtagable_bikeNetworkName;

    }

    public String getBikeNetworkHRef(Context ctx){

        return ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE)
                .getString(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_HREF, ctx), "/v2/networks/bixi-montreal");
    }

    public String getBikeNetworkCity(Context _ctx) {

        return _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE)
                .getString(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_CITY, _ctx), "");
    }

    public void saveBikeNetworkDesc(BikeNetworkDesc bikeNetworkDesc, Context ctx){

        SharedPreferences sp = ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor IdEditor = sp.edit();

        //Important to apply right away so that subsequent calls to buildNetworkSpecificKey work
        IdEditor.putString(PREF_CURRENT_BIKE_NETWORK_ID, bikeNetworkDesc.id).apply();

        SharedPreferences.Editor editor = sp.edit();

        editor.putString(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_NAME, ctx), bikeNetworkDesc.name);
        editor.putString(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_HREF, ctx), bikeNetworkDesc.href);
        editor.putString(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_CITY, ctx), bikeNetworkDesc.location.getCity());

        editor.apply();
    }

    public void saveBikeNetworkBounds(LatLngBounds bounds, Context ctx){

        if (!bounds.equals(getBikeNetworkBounds(ctx, 0))){

            SharedPreferences.Editor editor = ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE).edit();

            editor.putLong(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_BOUNDS_SW_LATITUDE, ctx),
                    Double.doubleToLongBits(bounds.southwest.latitude));
            editor.putLong(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_BOUNDS_SW_LONGITUDE, ctx),
                    Double.doubleToLongBits(bounds.southwest.longitude));
            editor.putLong(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_BOUNDS_NE_LATITUDE, ctx),
                    Double.doubleToLongBits(bounds.northeast.latitude));
            editor.putLong(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_BOUNDS_NE_LONGITUDE, ctx),
                    Double.doubleToLongBits(bounds.northeast.longitude));

            editor.apply();
        }
    }

    public LatLngBounds getBikeNetworkBounds(Context _ctx, double _paddingKm){

        SharedPreferences sp = _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        LatLng southwestRaw = new LatLng(
                Double.longBitsToDouble(sp.getLong(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_BOUNDS_SW_LATITUDE, _ctx), 0)),
                Double.longBitsToDouble(sp.getLong(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_BOUNDS_SW_LONGITUDE, _ctx), 0))
        );

        LatLng northeastRaw = new LatLng(
                Double.longBitsToDouble(sp.getLong(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_BOUNDS_NE_LATITUDE, _ctx), 0)),
                Double.longBitsToDouble(sp.getLong(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_BOUNDS_NE_LONGITUDE, _ctx), 0))
        );

        //http://gis.stackexchange.com/questions/2951/algorithm-for-offsetting-a-latitude-longitude-by-some-amount-of-meters
        //http://stackoverflow.com/questions/29478463/offset-latlng-by-some-amount-of-meters-in-android
        //Latitude : easy, 1 degree = 111111m (historically because of the French :D)
        //Longitude : 1 degree = 111111 * cos (latitude)m
        LatLng southwestPadded = new LatLng(southwestRaw.latitude - (_paddingKm*1000.d) / 111111.d,
                southwestRaw.longitude - (_paddingKm*1000.d) / 111111.d * Math.cos(southwestRaw.latitude)  );
        LatLng northeastPadded = new LatLng(northeastRaw.latitude + (_paddingKm*1000.d) / 111111.d,
                northeastRaw.longitude + (_paddingKm*1000.d) / 111111.d * Math.cos(northeastRaw.latitude)  );

        return new LatLngBounds(southwestPadded, northeastPadded);
    }

    private static String buildNetworkSpecificKey(String suffix, Context ctx){
        return getBikeNetworkId(ctx) + suffix;
    }

    public static String getBikeNetworkId(Context ctx){

        SharedPreferences sp = ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        return sp.getString(PREF_CURRENT_BIKE_NETWORK_ID, "");
    }

    /*public static void deleteDB() throws CouchbaseLiteException {
        //If it crashes here because getDatabase returns null, uninstall and reinstall the app
        mManager.getDatabase(mTRACKS_DB_NAME).delete();
    }*/

    /*public static boolean gotTracks() throws CouchbaseLiteException {
        return mGotTracks;
    }*/

    public void notifyBeginSavingStations(Context _ctx){

        mSaving = true;

        _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE).edit()
                .putBoolean(PREF_LAST_SAVE_CORRUPTED, true).apply();

        Log.d(TAG, "Begin saving bikeStationList");
    }

    public void notifyEndSavingStations(Context _ctx){

        _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE).edit()
                .putBoolean(PREF_LAST_SAVE_CORRUPTED, false).apply();

        mSaving = false;
        Log.d(TAG, "End saving bikeStationList");
    }

    public boolean wasLastSavePartial(Context _ctx) {

        return !mSaving && _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE)
                .getBoolean(PREF_LAST_SAVE_CORRUPTED, true);

    }
}
