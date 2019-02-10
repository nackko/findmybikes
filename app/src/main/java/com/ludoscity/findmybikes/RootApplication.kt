package com.ludoscity.findmybikes

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.support.multidex.MultiDex
import android.util.Log

import com.ludoscity.findmybikes.citybik_es.Citybik_esAPI
import com.ludoscity.findmybikes.citybik_es.model.BikeStation
import com.ludoscity.findmybikes.helpers.BikeStationRepository
import com.ludoscity.findmybikes.helpers.DBHelper

import java.io.IOException

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder

/**
 * Created by F8Full on 2015-09-28.
 * This class is used to maintain global states and also safely initialize static singletons
 * See http://stackoverflow.com/questions/3826905/singletons-vs-application-context-in-android
 */
class RootApplication : Application() {

    lateinit var citybik_esApi: Citybik_esAPI
        internal set
    lateinit var twitterApi: Twitter
        internal set
    //TODO: refactor with MVC in mind. This is model

    override fun onCreate() {
        super.onCreate()

        try {
            DBHelper.getInstance().init(this)
        } catch (e: IOException) {
            Log.d(TAG, "Error initializing database", e)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "Error initializing database", e)
        }

        if (!DBHelper.getInstance().wasLastSavePartial(this)) {


            bikeNetworkStationList = BikeStationRepository.getInstance().stationList


            Log.i("RootApplication", bikeNetworkStationList!!.size.toString() + " stations loaded from DB")
        } else
            bikeNetworkStationList =  ArrayList()


        citybik_esApi = buildCitybik_esAPI()
        twitterApi = buildTwitterAPI()
    }

    private fun buildCitybik_esAPI(): Citybik_esAPI {

        val retrofit = Retrofit.Builder()
                .baseUrl(ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        return retrofit.create(Citybik_esAPI::class.java)
    }

    //They are packaged indeed, but at least they don't show up on github ^^
    private fun buildTwitterAPI(): Twitter {
        val cb = ConfigurationBuilder()
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(resources.getString(R.string.twitter_consumer_key))
                .setOAuthConsumerSecret(resources.getString(R.string.twitter_consumer_secret))
                .setOAuthAccessToken(resources.getString(R.string.twitter_access_token))
                .setOAuthAccessTokenSecret(resources.getString(R.string.twitter_access_token_secret))
        val tf = TwitterFactory(cb.build())
        return tf.instance
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    companion object {

        private val TAG = "RootApplication"

        internal val ENDPOINT = "http://api.citybik.es"
        //Station list data is kept here to survive screen orientation change. TODO: use ViewModel
        //It's built from the database on launch (if there's a complete record available) and updated in memory after data download
        lateinit var bikeNetworkStationList: List<BikeStation>
            private set

        fun addAllToBikeNetworkStationList(_bikeStationList: ArrayList<BikeStation>): List<BikeStation> {

            val newList = ArrayList<BikeStation>(_bikeStationList.size)

            for (bikeStation in _bikeStationList) {

                if (bikeStation.emptySlots == null)
                    bikeStation.emptySlots = -1
                //Some systems have empty_slots to null (like nextbike SZ-bike in Dresden, Germany)
                //-1 is used to encode this case
                newList.add(bikeStation)
            }

            bikeNetworkStationList = newList

            return bikeNetworkStationList
        }
    }
}
