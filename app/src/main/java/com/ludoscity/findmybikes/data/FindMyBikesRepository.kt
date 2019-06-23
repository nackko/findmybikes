package com.ludoscity.findmybikes.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.ludoscity.findmybikes.data.database.bikesystem.BikeSystem
import com.ludoscity.findmybikes.data.database.bikesystem.BikeSystemDao
import com.ludoscity.findmybikes.data.database.favorite.*
import com.ludoscity.findmybikes.data.database.station.BikeStation
import com.ludoscity.findmybikes.data.database.station.BikeStationDao
import com.ludoscity.findmybikes.data.network.citybik_es.BikeSystemListAnswerRoot
import com.ludoscity.findmybikes.data.network.citybik_es.BikeSystemListNetworkDataSource
import com.ludoscity.findmybikes.data.network.citybik_es.BikeSystemStatus
import com.ludoscity.findmybikes.data.network.citybik_es.BikeSystemStatusNetworkDataSource
import com.ludoscity.findmybikes.data.network.cozy.CozyDataPipe
import com.ludoscity.findmybikes.data.network.cozy.CozyFileDescAnswerRoot
import com.ludoscity.findmybikes.data.network.cozy.RegisteredOAuthClient
import com.ludoscity.findmybikes.data.network.cozy.UserCredentialTokens
import com.ludoscity.findmybikes.data.network.twitter.TwitterNetworkDataExhaust
import com.ludoscity.findmybikes.utils.Utils
import com.nimbusds.oauth2.sdk.ResponseType
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import com.nimbusds.openid.connect.sdk.Nonce
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URI

/**
 * Created by F8Full on 2019-03-01. This file is part of #findmybikes
 *
 */
class FindMyBikesRepository private constructor(
        private val currentBikeSystemDao: BikeSystemDao,
        private val stationDao: BikeStationDao,
        private val favoriteEntityPlaceDao: FavoriteEntityPlaceDao,
        private val favoriteEntityStationDao: FavoriteEntityStationDao,
        private val bikeSystemListNetworkDataSource: BikeSystemListNetworkDataSource,
        private val bikeSystemStatusNetworkDataSource: BikeSystemStatusNetworkDataSource,
        private val twitterNetworkDataExhaust: TwitterNetworkDataExhaust,
        private val cozyNetworkDataPipe: CozyDataPipe,
        private val secureSharedPref: SharedPreferences) {

    private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)

    private var mInitialized = false
    private val networkCozyDirectoryData: LiveData<Result<CozyFileDescAnswerRoot>> = cozyNetworkDataPipe.createDirectoryResult

    private val networkBikeSystemStatusData: LiveData<BikeSystemStatus> = bikeSystemStatusNetworkDataSource.data
    private val networkBikeSystemListAnswerRootData: LiveData<BikeSystemListAnswerRoot> = bikeSystemListNetworkDataSource.data

    //transient local list of all bike systems available. Model determine nearest one for DB saving with bounds of current bike system
    private val availableBikeSystemList: MutableLiveData<List<BikeSystem>> = MutableLiveData()

    //For some reason, those copies are required for LiveData from Room to correctly trigger
    //when db is updated.
    //COULD NOT RETURN THEM DIRECTLY FROM ACCESSOR
    private val curBikeSystemData: LiveData<BikeSystem> = currentBikeSystemDao.single
    private val curBikeSystemStatusData: LiveData<List<BikeStation>> = stationDao.all

    private val statusFetchErrored = MutableLiveData<Boolean>()
    val lastBikeNetworkStatusFetchErrored: LiveData<Boolean>
        get () = statusFetchErrored

    private val prevBikeSystem = MutableLiveData<BikeSystem>()
    val previousBikeSystem: LiveData<BikeSystem>
        get() = prevBikeSystem

    // in-memory cache of the registeredOAuthClient object for Cozy
    var cozyOAuthClient: RegisteredOAuthClient? = null
        private set

    var cozyDirectoryId: String? = null
        private set

    val isCozyOAuthClientRegistered: Boolean
        get() = cozyOAuthClient != null

    init {

        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
        //TODO: store credentials properly and retrieve them from DB to restore client
        cozyOAuthClient = null

        coroutineScopeIO.launch {
            prevBikeSystem.postValue(currentBikeSystemDao.singleSynchronous)

            if (secureSharedPref.contains(OAUTH_CLIENT_REG_TOKEN_PREF_KEY)) {
                Log.i(TAG, "Cozy OAuth client registration found in secure shared pref, deserializing...")
                setRegisteredOAuthClient(RegisteredOAuthClient(
                        secureSharedPref.getString(OAUTH_STACK_BASEURL_PREF_KEY, null)!!,
                        secureSharedPref.getString(OAUTH_CLIENT_ID_PREF_KEY, null)!!,
                        secureSharedPref.getString(OAUTH_CLIENT_SECRET_PREF_KEY, null)!!,
                        secureSharedPref.getString(OAUTH_CLIENT_REG_TOKEN_PREF_KEY, null)!!
                ))

                if (secureSharedPref.contains(OAUTH_ACCESS_TOKEN_PREF_KEY)) {
                    Log.i(TAG, "Cozy OAuth authorization found in secure shared pref, deserializing...")
                    setUserCredentials(UserCredentialTokens(
                            secureSharedPref.getString(OAUTH_ACCESS_TOKEN_PREF_KEY, null)!!,
                            secureSharedPref.getString(OAUTH_REFRESH_TOKEN_PREF_KEY, null)!!
                    ))
                }
            }

            if (secureSharedPref.contains(COZY_DIRECTORY_ID_PREF_KEY)) {
                Log.i(TAG, "Cozy directory Id found in secure shared pref, deserializing...")
                setCozyDirectoryId(secureSharedPref.getString(COZY_DIRECTORY_ID_PREF_KEY, null)!!)
            }
        }

        networkCozyDirectoryData.observeForever {
            it?.let { mkdirAnswerData ->

                if (mkdirAnswerData is Result.Success && mkdirAnswerData.data.data?.id != null) {
                    secureSharedPref.edit()
                            .putString(COZY_DIRECTORY_ID_PREF_KEY, mkdirAnswerData.data.data.id)
                            .apply()

                    Log.i(TAG, "saved Cozy directory Id in secure shared preferences")

                    setCozyDirectoryId(mkdirAnswerData.data.data.id)
                } else {
                    if (mkdirAnswerData is Result.Error) {
                        Log.e(TAG, mkdirAnswerData.exception.message, mkdirAnswerData.exception)
                    }
                    cozyDirectoryId = null
                }
            }
        }

        networkBikeSystemStatusData.observeForever { newStatusDataFromNetwork ->

            if (newStatusDataFromNetwork == null) {
                statusFetchErrored.value = true
            }

            newStatusDataFromNetwork?.let {

                coroutineScopeIO.launch {

                    if (prevBikeSystem.value == null || (prevBikeSystem.value?.id != getCurrentBikeSystem().value?.id))
                        prevBikeSystem.postValue(getCurrentBikeSystem().value)

                    currentBikeSystemDao.updateLastUpdateTimestamp(it.id, System.currentTimeMillis())

                    Log.d(TAG, "New data from network, begin processing of " +
                            "${newStatusDataFromNetwork.bikeStationList?.size} stations for saving in Room")

                    val boundsBuilder = LatLngBounds.Builder()

                    newStatusDataFromNetwork.bikeStationList?.forEach {

                        boundsBuilder.include(it.location)

                        it.uid = it.locationHash

                        if (it.extra != null) {
                            if (it.extra.uid != null) {
                                it.uid = "${newStatusDataFromNetwork.id}:${it.extra.uid}"
                            }

                            if (it.name == null) {
                                it.name = it.extra.name ?: "null"
                            }
                        }
                    }

                    Log.d(TAG, "Calculating bounds")
                    val bounds = boundsBuilder.build()

                    Log.d(TAG, "Saving bounds")
                    currentBikeSystemDao.updateBounds(it.id,
                            bounds.northeast.latitude,
                            bounds.northeast.longitude,
                            bounds.southwest.latitude,
                            bounds.southwest.longitude)

                    Log.d(TAG, "Processing done")
                    //TODO: smarter update algorithm that purges obsolete stations from DB without dropping the whole table
                    //deleteOldData
                    //TODO: in relation to previous : do that only when switching from a bike network to an other one
                    //BUT, even when updating in place, some strategy should be in place to detect 'orphans' and purge them
                    //especially if they are a user's favourite.
                    //For now, live with the visual glitch on launch where the UI table empties itself and refills
                    stationDao.deleteAllBikeStation()
                    Log.d(TAG, "Old station data deleted")
                    //Insert new data into database
                    stationDao.insertBikeStationList(newStatusDataFromNetwork.bikeStationList!!)
                    Log.d(TAG, "New values inserted with replace strategy")
                }
            }
        }
        networkBikeSystemListAnswerRootData.observeForever { newBikeSystemListData ->

            newBikeSystemListData?.let { newBikeSystemListFromNetwork ->
                Log.d(TAG, "New complete list of bike system available, size: ${newBikeSystemListFromNetwork.networks?.size}")

                coroutineScopeIO.launch {
                    Log.d(TAG, "Backgorund thread, turning BikeSystemListanswerRoot into List<BikeSystem>")

                    val bikeSystemList = ArrayList<BikeSystem>()
                    //TODO: behave if networks is null (that should be a test)
                    newBikeSystemListData.networks!!.forEach {

                        bikeSystemList.add(BikeSystem(
                                it.id,
                                System.currentTimeMillis(),
                                it.href,
                                it.name,
                                Utils.cleanStringForHashtagUse(it.name),
                                it.location.latitude,
                                it.location.longitude,
                                it.location.city,
                                //TODO: also have country. Bounds will be calculated when network status is available
                                "to add country here",
                                null,
                                null,
                                null,
                                null
                        ))

                    }

                    //act model observes transient full list and then update repo for current bike system
                    //(it knows user location)
                    availableBikeSystemList.postValue(bikeSystemList)
                }
            }
        }
    }

    /**
     * Creates periodic sync tasks and checks to see if an immediate sync is required. If an
     * immediate sync is required, this method will take care of making sure that sync occurs.
     */
    @Synchronized
    private fun initializeData(ctx: Context) {

        // Only perform initialization once per app lifetime. If initialization has already been
        // performed, we have nothing to do in this method.
        if (mInitialized) return
        mInitialized = true

        // This method call triggers Findmybikes to create its task to synchronize station data
        // periodically.
        //TODO: non manual refresh
        //bikeSystemStatusNetworkDataSource.scheduleRecurringFetchBikeSystemSync()

        coroutineScopeIO.launch {
            //TODO: implement real isFetchNeeded. Shall we have on for status and one for list ?
            //or should the thing cascade ?
            if (isFetchNeeded()){
                //startFetchBikeSystemStatusDataService(ctx)
                startFetchBikeSystemListDataService(ctx)
            }
        }
    }

    fun getStationForId(targetId: String): BikeStation {
        return stationDao.getStation(targetId)
    }

    fun getFavoriteStationList(): LiveData<List<FavoriteEntityStation>> {
        return favoriteEntityStationDao.getAllForBikeSystemId(currentBikeSystemDao.singleIdSynchronous)
    }

    fun getFavoritePlaceList(): LiveData<List<FavoriteEntityPlace>> {
        return favoriteEntityPlaceDao.getAllForBikeSystemId(currentBikeSystemDao.singleIdSynchronous)
    }

    fun addOrUpdateFavorite(toAddOrUpdate: FavoriteEntityBase) {
        coroutineScopeIO.launch {
            when (toAddOrUpdate) {
                is FavoriteEntityPlace -> favoriteEntityPlaceDao.insertOne(toAddOrUpdate)
                is FavoriteEntityStation -> favoriteEntityStationDao.insertOne(toAddOrUpdate)
            }

            Log.d(TAG, "Favorite added or updated, default name : ${toAddOrUpdate.defaultName}")
        }
    }

    fun updateFavoriteCustomNameByFavoriteId(idToUpdate: String, newCustomName: String) {

        coroutineScopeIO.launch {
            if (idToUpdate.startsWith(FavoriteEntityPlace.PLACE_ID_PREFIX)) {
                favoriteEntityPlaceDao.updateCustomNameByFavoriteId(idToUpdate, newCustomName)
            } else {
                favoriteEntityStationDao.updateCustomNameByFavoriteId(idToUpdate, newCustomName)
            }
        }
    }

    fun updateFavoriteUiIndexByFavoriteId(idToUpdate: String, newUiIndex: Int) {
        coroutineScopeIO.launch {
            if (idToUpdate.startsWith(FavoriteEntityPlace.PLACE_ID_PREFIX)) {
                favoriteEntityPlaceDao.updateUiIndexByFavoriteId(idToUpdate, newUiIndex)
            } else {
                favoriteEntityStationDao.updateUiIndexByFavoriteId(idToUpdate, newUiIndex)
            }
        }
    }

    fun removeFavoriteByFavoriteId(idToRemove: String) {
        coroutineScopeIO.launch {
            if (idToRemove.startsWith(FavoriteEntityPlace.PLACE_ID_PREFIX)) {
                favoriteEntityPlaceDao.deleteOne(idToRemove)
            } else {
                favoriteEntityStationDao.deleteOne(idToRemove)
            }
        }
    }

    fun getFavoriteEntityByFavoriteId(favoriteId: String?): FavoriteEntityBase? {
        if (favoriteId == null)
            return null

        return if (favoriteId.startsWith(FavoriteEntityPlace.PLACE_ID_PREFIX))
            getFavoritePlaceByFavoriteId(favoriteId)
        else
            getFavoriteStationByFavoriteId(favoriteId)
    }

    private fun getFavoriteStationByFavoriteId(favoriteId: String): FavoriteEntityStation? {
        return favoriteEntityStationDao.getForId(favoriteId)
    }

    private fun getFavoritePlaceByFavoriteId(favoriteId: String): FavoriteEntityPlace? {
        return favoriteEntityPlaceDao.getForId(favoriteId)
    }

    fun getFavoriteCount(): Int {
        return favoriteEntityPlaceDao.count + favoriteEntityStationDao.count
    }

    fun getAllFavoriteStationIdList(): List<String> {
        return favoriteEntityStationDao.allId
    }

    fun isFavoriteId(id: String?): Boolean {
        return id == null || (favoriteEntityPlaceDao.isFavoriteId(id) != 0 || favoriteEntityStationDao.isFavoriteId(id) != 0)
    }

    fun hasAtLeastNValidStationFavorites(nearestStationId: String, n: Int): Boolean {
        return favoriteEntityStationDao.validFavoriteCount(nearestStationId) >= n
    }

    fun getBikeSystemStationData(ctx: Context): LiveData<List<BikeStation>>{
        initializeData(ctx)
        return curBikeSystemStatusData
    }

    fun getBikeSystemListData(ctx: Context): LiveData<List<BikeSystem>> {
        initializeData(ctx)
        return availableBikeSystemList
    }

    fun getCurrentBikeSystem(): LiveData<BikeSystem> {
        return curBikeSystemData
    }

    fun setCurrentBikeSystem(ctx: Context, toSet: BikeSystem, alsoFetchStatus: Boolean = false) {
        Log.d(TAG, "setting current bike system to : $toSet")
        prevBikeSystem.postValue(currentBikeSystemDao.singleSynchronous)
        currentBikeSystemDao.deleteCurrentBikeSystem()
        currentBikeSystemDao.insertCurrentBikeSystem(toSet)
        if (alsoFetchStatus)
            startFetchBikeSystemStatusDataService(ctx, toSet.citybikDOTesUrl)
    }

    fun invalidateCurrentBikeSystem(ctx: Context) {
        startFetchBikeSystemListDataService(ctx)
    }

    fun invalidateBikeSystemStatus(ctx: Context, systemHRef: String) {
        startFetchBikeSystemStatusDataService(ctx, systemHRef)
    }

    private fun isFetchNeeded(): Boolean {
        return 0 == currentBikeSystemDao.countCurrentBikeSystem()
    }

    private fun isBikeSystemStatusFetchNeeded(): Boolean {
        return false
    }

    private fun startFetchBikeSystemStatusDataService(ctx: Context, systemHRef: String) {
        bikeSystemStatusNetworkDataSource.startFetchBikeSystemStatusService(ctx, systemHRef)
    }

    private fun startFetchBikeSystemListDataService(ctx: Context) {
        bikeSystemListNetworkDataSource.startFetchingBikeSystemListService(ctx)
    }

    private fun startPushTwitterDataService(ctx: Context, dataToPush: ArrayList<String>, userLoc: LatLng) {
        twitterNetworkDataExhaust.startPushDataToTwitterService(ctx, dataToPush, userLoc)
    }

    fun pushDataToTwitter(ctx: Context, dataToPush: ArrayList<String>, userLoc: LatLng) {
        startPushTwitterDataService(ctx, dataToPush, userLoc)
    }

    fun getHashtaggableCurBikeSystemName(): String {
        return currentBikeSystemDao.singleNameForHashtagUse
    }

    ///////////////////////////
    //Cozy

    @SuppressLint("ApplySharedPref")
    fun unregisterCozyOAuthClient(cozyBaseUrlString: String): Result<Boolean> {
        val result = cozyNetworkDataPipe.unregister(
                cozyBaseUrlString = cozyBaseUrlString,
                clientId = cozyOAuthClient?.clientId!!,
                masterAccessToken = cozyOAuthClient?.registrationAccessToken!!)

        //TODO: this is a problem if the client is unregistered out of the app
        if (result is Result.Success) {
            secureSharedPref.edit().remove(OAUTH_CLIENT_REG_TOKEN_PREF_KEY)
                    .remove(OAUTH_CLIENT_ID_PREF_KEY)
                    .remove(OAUTH_CLIENT_SECRET_PREF_KEY)
                    .remove(OAUTH_STACK_BASEURL_PREF_KEY)
                    .remove(OAUTH_ACCESS_TOKEN_PREF_KEY)
                    .remove(OAUTH_REFRESH_TOKEN_PREF_KEY)
                    .remove(COZY_DIRECTORY_ID_PREF_KEY)
                    .commit()

            this.cozyOAuthClient = null
            this.userCred = null
            this.cozyDirectoryId = null
        }

        return result
    }

    @SuppressLint("ApplySharedPref")
    fun registerCozyOAuthClient(cozyBaseUrlString: String): Result<RegisteredOAuthClient> {
        // handle registration
        val result = cozyNetworkDataPipe.register(cozyBaseUrlString)

        if (result is Result.Success) {
            // If user credentials will be cached in local storage, it is recommended it be encrypted
            // @see https://developer.android.com/training/articles/keystore
            //We're using androix security library
            //https://developer.android.com/jetpack/androidx/releases/security
            secureSharedPref.edit().putString(OAUTH_STACK_BASEURL_PREF_KEY, result.data.stackBaseUrl)
                    .putString(OAUTH_CLIENT_ID_PREF_KEY, result.data.clientId)
                    .putString(OAUTH_CLIENT_SECRET_PREF_KEY, result.data.clientSecret)
                    .putString(OAUTH_CLIENT_REG_TOKEN_PREF_KEY, result.data.registrationAccessToken)
                    .commit()

            setRegisteredOAuthClient(result.data)
        }

        return result
    }

    private fun setRegisteredOAuthClient(registeredClient: RegisteredOAuthClient) {
        this.cozyOAuthClient = registeredClient
    }

    private fun setCozyDirectoryId(toSet: String) {
        this.cozyDirectoryId = toSet
    }

    // in-memory cache of the authRequest State object
    private var authRequestState: State? = null
    // in-memory cache of the authRequest Nonce object
    var authRequestNonce: Nonce? = null
        private set

    // If user credentials will be cached in local storage, it is recommended it be encrypted
    // @see https://developer.android.com/training/articles/keystore
    var userCred: UserCredentialTokens? = null
        private set

    private fun setUserCredentials(userCred: UserCredentialTokens) {
        this.userCred = userCred
    }

    @SuppressLint("ApplySharedPref")
    fun refreshCozyAccessToken(): Result<UserCredentialTokens> {
        val result = cozyNetworkDataPipe.refreshAccessToken(
                cozyOAuthClient?.stackBaseUrl!!,
                userCred!!,
                cozyOAuthClient?.clientId!!,
                cozyOAuthClient?.clientSecret!!
        )

        //Repo captures data
        if (result is Result.Success) {

            // If user credentials will be cached in local storage, it is recommended it be encrypted
            // @see https://developer.android.com/training/articles/keystore
            //We're using androix security library
            //https://developer.android.com/jetpack/androidx/releases/security
            secureSharedPref.edit().putString(OAUTH_ACCESS_TOKEN_PREF_KEY, result.data.accessToken)
                    .putString(OAUTH_REFRESH_TOKEN_PREF_KEY, result.data.refreshToken)
                    .commit()

            setUserCredentials(result.data)
        }

        return result
    }

    @SuppressLint("ApplySharedPref")
    fun exchangeCozyAuthCodeForTokenCouple(
            cozyBaseUrlString: String,
            redirectIntentData: String,
            clientID: String,
            clientSecret: String):
            Result<UserCredentialTokens> {

        val result = cozyNetworkDataPipe.exchangeAuthCodeForTokenCouple(
                cozyBaseUrlString,
                redirectIntentData,
                clientID,
                clientSecret,
                authRequestState!!)

        //Repo captures data
        if (result is Result.Success) {

            // If user credentials will be cached in local storage, it is recommended it be encrypted
            // @see https://developer.android.com/training/articles/keystore
            //We're using androix security library
            //https://developer.android.com/jetpack/androidx/releases/security
            secureSharedPref.edit().putString(OAUTH_ACCESS_TOKEN_PREF_KEY, result.data.accessToken)
                    .putString(OAUTH_REFRESH_TOKEN_PREF_KEY, result.data.refreshToken)
                    .commit()

            setUserCredentials(result.data)
        }

        return result
    }

    fun buildCozyAuthenticationUri(cozyBaseUrlString: String, authClientId: RegisteredOAuthClient?): Result<URI> {

        //we just publish URI for Activity consumption
        // Generate random state string for pairing the response to the request
        authRequestState = State()
// Generate nonce
        authRequestNonce = Nonce()
// Specify scope
        //TODO: custom scope from UI
        val scope = Scope.parse("openid io.cozy.files io.cozy.oauth.clients")

// Compose the request
        val authenticationRequest = AuthenticationRequest(
                URI("$cozyBaseUrlString/auth/authorize"),
                ResponseType(ResponseType.Value.CODE),
                //TODO: from configuration file
                scope, ClientID(authClientId?.clientId), URI("findmybikes://com.f8full.findmybikes.oauth2redirect"),
                authRequestState,
                authRequestNonce
        )

        return Result.Success(authenticationRequest.toURI())

    }


    //end cozy
    /////////////////////////////////////////////

    companion object {
        private val TAG = FindMyBikesRepository::class.java.simpleName

        private const val OAUTH_STACK_BASEURL_PREF_KEY = "fmb_oauth_stack_baseurl"
        private const val OAUTH_CLIENT_ID_PREF_KEY = "fmb_oauth_client_id"
        private const val OAUTH_CLIENT_SECRET_PREF_KEY = "fmb_oauth_client_secret"
        private const val OAUTH_CLIENT_REG_TOKEN_PREF_KEY = "fmb_oauth_client_registration_token"
        private const val OAUTH_ACCESS_TOKEN_PREF_KEY = "fmb_oauth_access_token"
        private const val OAUTH_REFRESH_TOKEN_PREF_KEY = "fmb_oauth_refresh_token"

        private const val COZY_DIRECTORY_ID_PREF_KEY = "fmb_cozy_directory_id"

        // For Singleton instantiation
        private val LOCK = Any()
        private var sInstance: FindMyBikesRepository? = null

        @Synchronized
        fun getInstance(
                bikeSystemDao: BikeSystemDao,
                bikeStationDao: BikeStationDao,
                favoriteEntityPlaceDao: FavoriteEntityPlaceDao,
                favoriteEntityStationDao: FavoriteEntityStationDao,
                bikeSystemListNetworkDataSource: BikeSystemListNetworkDataSource,
                bikeSystemStatusNetworkDataSource: BikeSystemStatusNetworkDataSource,
                twitterNetworkDataExhaust: TwitterNetworkDataExhaust,
                cozyNetworkDataPipe: CozyDataPipe,
                secureSharedPref: SharedPreferences): FindMyBikesRepository {
            //Log.d(TAG, "Getting the repository")
            if (sInstance == null) {
                synchronized(LOCK) {
                    sInstance = FindMyBikesRepository(bikeSystemDao,
                            bikeStationDao,
                            favoriteEntityPlaceDao,
                            favoriteEntityStationDao,
                            bikeSystemListNetworkDataSource,
                            bikeSystemStatusNetworkDataSource,
                            twitterNetworkDataExhaust,
                            cozyNetworkDataPipe,
                            secureSharedPref)
                    Log.d(TAG, "Made new repository")
                }
            }
            return sInstance!!
        }
    }
}