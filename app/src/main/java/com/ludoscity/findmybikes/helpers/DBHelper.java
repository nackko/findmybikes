package com.ludoscity.findmybikes.helpers;

import android.annotation.SuppressLint;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.ludoscity.findmybikes.FavoriteItemBase;
import com.ludoscity.findmybikes.FavoriteItemPlace;
import com.ludoscity.findmybikes.FavoriteItemStation;
import com.ludoscity.findmybikes.R;
import com.ludoscity.findmybikes.StationItem;
import com.ludoscity.findmybikes.citybik_es.model.BikeStation;
import com.ludoscity.findmybikes.citybik_es.model.NetworkDesc;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * Created by F8Full on 2015-04-02.
 * This file is part of BixiTrackExplorer
 * Helper class providing static method to save and retrieve data from storage
 * Internally, it uses both SharedPreferences and couchbase
 */
@SuppressWarnings("unchecked") //(List<QueryRow>) allDocs.get("rows");
public class DBHelper {

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

    private static boolean mAutoUpdatePaused = false;

    private DBHelper() {}

    public static void init(Context context) throws IOException, PackageManager.NameNotFoundException {
        mDatabase = Room.databaseBuilder(context, AppDatabase.class, "findmybikes-database").build();

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

            editor.putInt(SHARED_PREF_VERSION_CODE, currentVersionCode);
            editor.apply();
        }
    }

    public static boolean getAutoUpdate(Context _ctx){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(_ctx);

        return !mAutoUpdatePaused && sp.getBoolean(_ctx.getString(R.string.pref_refresh_options_key), false);
    }

    public static void pauseAutoUpdate() { mAutoUpdatePaused = true; }

    public static void resumeAutoUpdate() { mAutoUpdatePaused = false; }

    public static int getCriticalAvailabilityMax(Context _ctx) {
        SharedPreferences sp = _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        return sp.getInt(_ctx.getString(R.string.pref_critical_availability_max_key), PREF_CRITICAL_AVAILABILITY_MAX_DEFAULT);
    }

    public static int getBadAvailabilityMax(Context _ctx) {

        SharedPreferences sp = _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        return sp.getInt(_ctx.getString(R.string.pref_bad_availability_max_key), PREF_BAD_AVAILABILITY_MAX_DEFAULT);

    }

    public static void saveCriticalAvailabilityMax(Context _ctx, int _toSave) {
        _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE).edit()
                .putInt(_ctx.getString(R.string.pref_critical_availability_max_key), _toSave)
                .apply();
    }

    public static void saveBadAvailabilityMax(Context _ctx, int _toSave) {
        _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE).edit()
                .putInt(_ctx.getString(R.string.pref_bad_availability_max_key), _toSave)
                .apply();
    }

    public static long getLastUpdateTimestamp(Context ctx){

        return ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE)
                .getLong(buildNetworkSpecificKey(PREF_SUFFIX_WEBTASK_LAST_TIMESTAMP_MS, ctx), 0);
    }

    public static void saveLastUpdateTimestampAsNow(Context ctx){

        ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE).edit()
                .putLong(buildNetworkSpecificKey(PREF_SUFFIX_WEBTASK_LAST_TIMESTAMP_MS, ctx),
                        Calendar.getInstance().getTimeInMillis()).apply();
    }

    public static boolean isBikeNetworkIdAvailable(Context ctx){

        return !getBikeNetworkId(ctx).equalsIgnoreCase("");
    }

    public static String getBikeNetworkName(Context ctx){

        return ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE)
                .getString(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_NAME, ctx), "");
    }

    public static String getHashtaggableNetworkName(Context _ctx){

        String hashtagable_bikeNetworkName = getBikeNetworkName(_ctx);
        hashtagable_bikeNetworkName = hashtagable_bikeNetworkName.replaceAll("\\s","");
        hashtagable_bikeNetworkName = hashtagable_bikeNetworkName.replaceAll("[^A-Za-z0-9 ]", "");
        hashtagable_bikeNetworkName = hashtagable_bikeNetworkName.toLowerCase();

        return hashtagable_bikeNetworkName;

    }

    public static String getBikeNetworkHRef(Context ctx){

        return ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE)
                .getString(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_HREF, ctx), "/v2/networks/bixi-montreal");
    }

    public static String getBikeNetworkCity(Context _ctx) {

        return _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE)
                .getString(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_CITY, _ctx), "");
    }

    public static void saveBikeNetworkDesc(NetworkDesc networkDesc, Context ctx){

        SharedPreferences sp = ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor IdEditor = sp.edit();

        //Important to apply right away so that subsequent calls to buildNetworkSpecificKey work
        IdEditor.putString(PREF_CURRENT_BIKE_NETWORK_ID, networkDesc.id).apply();

        SharedPreferences.Editor editor = sp.edit();

        editor.putString(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_NAME, ctx), networkDesc.name);
        editor.putString(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_HREF, ctx), networkDesc.href);
        editor.putString(buildNetworkSpecificKey(PREF_SUFFIX_NETWORK_CITY, ctx), networkDesc.location.city);

        editor.apply();
    }

    public static void saveBikeNetworkBounds(LatLngBounds bounds, Context ctx){

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

    public static LatLngBounds getBikeNetworkBounds(Context _ctx, double _paddingKm){

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

    public static void notifyBeginSavingStations(Context _ctx){

        mSaving = true;

        _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE).edit()
                .putBoolean(PREF_LAST_SAVE_CORRUPTED, true).apply();

        Log.d(TAG, "Begin saving bikeStationList");
    }

    public static void notifyEndSavingStations(Context _ctx){

        _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE).edit()
                .putBoolean(PREF_LAST_SAVE_CORRUPTED, false).apply();

        mSaving = false;
        Log.d(TAG, "End saving bikeStationList");
    }

    public static boolean wasLastSavePartial(Context _ctx) {

        return !mSaving && _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE)
                .getBoolean(PREF_LAST_SAVE_CORRUPTED, true);

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //!!! DANGER ZONE !!!
    //This method cannot be completely trusted as of yet, because db update algorithm is too violent
    //TODO: Rework saving algorithm.
    //current method of dropping / recreating everything leads to some big troubles
    //NO CLIENT FOR NOW
    /*public static StationItem getStation(final String _stationId){

        StationItem toReturn = null;

        Document stationDoc = getStationFromId(_stationId);

        if (stationDoc != null && stationDoc.getProperties() != null){
            toReturn = createStationItem(stationDoc);
        }

        return toReturn;
    }


    private static Document getStationFromId(String _stationId) {

        Document toReturn = null;
        // retrieve the document from the database
        //from : http://developer.couchbase.com/documentation/mobile/current/develop/training/build-first-android-app/do-crud/index.html
        try {
            toReturn = mManager.getDatabase(mSTATIONS_DB_NAME).getDocument(_stationId);
        } catch (CouchbaseLiteException | SQLiteException e) {
            Log.d(TAG, "Couldn't retrieve a station document from id", e);
        }

        return toReturn;
    }*/

    public static void deleteAllStations() {
        mDatabase.bikeStationDao().deleteAllBikeStation();
    }

    public static ArrayList<StationItem> getStationsNetwork() {
        ArrayList<StationItem> stationsNetwork = new ArrayList<>();

        List<BikeStation> allStationList;

        allStationList = mDatabase.bikeStationDao().getAll().getValue();

        if (allStationList == null)
        {
            allStationList = new ArrayList<>();
        }


        for (BikeStation bs : allStationList)
        {
            //Épic !!
            stationsNetwork.add(new StationItem(
                    bs.getLocationHash(), bs.getName(),
                    new LatLng(bs.getLatitude(), bs.getLongitude()),
                    bs.getFreeBikes(), bs.getEmptySlots(), bs.getTimestamp(),
                    bs.getExtra().getLocked()));
        }

        return stationsNetwork;
    }

    public static void saveStationNetwork(List<StationItem> stationListNetwork) {

        ArrayList<BikeStation> toSave = new ArrayList<>(stationListNetwork.size());

        for (StationItem si : stationListNetwork)
        {
            toSave.add(new BikeStation(si));
        }

        mDatabase.bikeStationDao().insertBikeStationList(toSave);
    }

    //TODO: Add validation of IDs to handle the case were a favorite station been removed
    //Replace edit fab with red delete one
    public static ArrayList<FavoriteItemBase> getFavoriteAll(Context _ctx){
        ArrayList<FavoriteItemBase> toReturn = new ArrayList<>();

        SharedPreferences sp = _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        try {
            JSONArray favoritesJSONArray = new JSONArray(sp.getString(
                    buildNetworkSpecificKey(PREF_SUFFIX_FAVORITES_JSONARRAY, _ctx), "[]" ));

                //reverse iteration so that newly added favorites appear on top of the list
                for (int i=favoritesJSONArray.length()-1; i>=0; --i){

                    JSONObject curFav = favoritesJSONArray.optJSONObject(i);

                    if ( curFav != null){

                        if (curFav.getString(FavoriteItemBase.FAVORITE_JSON_KEY_ID).startsWith(FavoriteItemPlace.PLACE_ID_PREFIX)){
                            toReturn.add(FavoriteItemPlace.fromJSON(curFav));
                        }
                        else{
                            toReturn.add(FavoriteItemStation.fromJSON(curFav));
                        }
                    }
                }

            //check if there was no valid data in the retrieved array
            if (favoritesJSONArray.length() != 0 && toReturn.isEmpty())
                dropFavoriteAll(_ctx);

        } catch (JSONException e) {
            Log.d(TAG, "Error while loading favorites from prefs", e);
        }

        return toReturn;
    }

    public static FavoriteItemBase getFavoriteItemForId(Context _ctx, String _favoriteID){
        FavoriteItemBase toReturn = null;

        ArrayList<FavoriteItemBase> favoriteList = getFavoriteAll(_ctx);

        for (int i=0; i<favoriteList.size(); ++i){
            if (favoriteList.get(i).getId().equalsIgnoreCase(_favoriteID)) {
                toReturn = favoriteList.get(i);
                break;
            }
        }

        return toReturn;
    }

    //TODO: Build a cache of bikeStationList that have already been checked
    //This is called every time the list binds a station
    //it happens a lot (every time user location is updated).
    //getFavoriteAll loads a JSON from SharedPref
    public static boolean isFavorite(String id, Context ctx) {

        boolean toReturn = false;

        ArrayList<FavoriteItemBase> favoriteList = getFavoriteAll(ctx);

        for (int i=0; i<favoriteList.size(); ++i){
            if (favoriteList.get(i).getId().equalsIgnoreCase(id)){
                toReturn = true;
                break;
            }
        }

        return toReturn;
    }

    //counts valid favorites, an invalid favorite corresponds to the provided StationItem
    //returns true if this count >= provided parameter
    public static boolean hasAtLeastNValidFavorites(StationItem _closestBikeStation, int _n, Context _ctx) {

        int validCount = 0;

        ArrayList<FavoriteItemBase> favoriteList = getFavoriteAll(_ctx);

        if (_closestBikeStation == null)
            return favoriteList.size() >= _n;

        for (int i=0; i<favoriteList.size(); ++i){
            if (!favoriteList.get(i).getId().equalsIgnoreCase(_closestBikeStation.getId()))
                ++validCount;
        }

        return validCount >= _n;
    }

    @SuppressLint("CommitPrefEdits")
    public static void dropFavoriteAll(Context _ctx){

        SharedPreferences sp = _ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        sp.edit().remove(buildNetworkSpecificKey(PREF_SUFFIX_FAVORITES_JSONARRAY, _ctx)).commit(); //I DO want commit and not apply

    }

    public static void updateFavorite(final Boolean isFavorite, FavoriteItemBase _favorite, Context ctx) {

        SharedPreferences sp = ctx.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        //JSONArray favoriteJSONArray;
        //contains JSONObject elements
        //BikeStation favorite
        //{
        //    favorite_id: string, (a station ID)
        //    display_name: string,
        //    is_display_name_default: boolean
        //}
        //Place favorite
        //{
        //    favorite_id: string, (a place ID, prefixed with FavoriteItemPlace.PLACE_ID_PREFIX)
        //    display_name: string,
        //    latitude: double,
        //    longitude: double,
        //    attributions: string
        //}

        try{
            JSONArray favoriteJSONArray = new JSONArray(sp.getString(
                    buildNetworkSpecificKey(PREF_SUFFIX_FAVORITES_JSONARRAY, ctx), "[]" ));

            int existingIdx = -1;
            int firstNullIdx = -1;

            for (int i=0; i<favoriteJSONArray.length(); ++i){
                JSONObject curFav = favoriteJSONArray.optJSONObject(i);
                if (curFav != null && curFav.getString(FavoriteItemBase.FAVORITE_JSON_KEY_ID).equalsIgnoreCase(_favorite.getId())) {
                    existingIdx = i;
                }
                else if(curFav == null && firstNullIdx == -1){
                    firstNullIdx = i;
                }
            }

            if (isFavorite){

                if (existingIdx == -1){

                    if (firstNullIdx == -1){
                        favoriteJSONArray.put(_favorite.toJSON());
                    }
                    else{
                        favoriteJSONArray.put(firstNullIdx, _favorite.toJSON());
                    }
                }
                else{
                    favoriteJSONArray.put(existingIdx, _favorite.toJSON());
                }
            }
            else{

                int i = existingIdx+1;

                //packing all null at the end of the array
                for (; i < favoriteJSONArray.length(); ++i){
                    JSONObject curFav = favoriteJSONArray.optJSONObject(i);

                    if (curFav == null)
                        break;

                    favoriteJSONArray.put(i-1, curFav);

                }

                favoriteJSONArray.put(i-1, JSONObject.NULL);
            }

            sp.edit().putString(buildNetworkSpecificKey(PREF_SUFFIX_FAVORITES_JSONARRAY, ctx), favoriteJSONArray.toString()).apply();

        } catch (JSONException e) {
            Log.d(TAG, "Error while retrieving favorites from Preferences", e);
        }

        //Set<String> oldFavorites = sp.getStringSet(buildNetworkSpecificKey(PREF_SUFFIX_FAVORITES_SET, ctx), new HashSet<String>());

        /*http://developer.android.com/reference/android/content/SharedPreferences.html#getStringSet(java.lang.String, java.util.Set)

        Note that you must not modify the set instance returned by this call.
        The consistency of the stored data is not guaranteed if you do, nor is your ability to modify the instance at all.*/

        //Set<String> newFavorites = new HashSet<>(oldFavorites);

        //if (isFavorite)
        //    newFavorites.add(id);
        //else
        //    newFavorites.remove(id);

        //sp.edit().putStringSet(buildNetworkSpecificKey(PREF_SUFFIX_FAVORITES_SET, ctx), newFavorites).apply();

        /*Document doc = mManager.getDatabase(mSTATIONS_DB_NAME).getExistingDocument(id);


        doc.update(new Document.DocumentUpdater() {
            @Override
            public boolean update(UnsavedRevision newRevision) {
                newRevision.getProperties().put("isFavorite", isFavorite);
                return true;
            }
        });*/

    }
}
