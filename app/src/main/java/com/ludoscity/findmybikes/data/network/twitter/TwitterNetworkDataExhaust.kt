package com.ludoscity.findmybikes.data.network.twitter

import android.content.Context
import android.content.Intent
import android.support.v4.util.Pair
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.data.database.station.BikeStation
import com.ludoscity.findmybikes.ui.table.TableFragmentViewModel
import com.ludoscity.findmybikes.utils.InjectorUtils
import com.ludoscity.findmybikes.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import twitter4j.GeoLocation
import twitter4j.StatusUpdate
import twitter4j.Twitter
import twitter4j.TwitterException
import java.text.NumberFormat

class TwitterNetworkDataExhaust private constructor() {

    private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)

    fun startPushDataToTwitterService(ctx: Context, dataToPush: ArrayList<String>, userLoc: LatLng) {
        val intentToPush = Intent(ctx, PushToTwitterDataIntentService::class.java)
        intentToPush.action = PushToTwitterDataIntentService.ACTION_PUSH_DATA
        intentToPush.putExtra(PushToTwitterDataIntentService.EXTRA_DATA_TO_PUSH, dataToPush)
        intentToPush.putExtra(PushToTwitterDataIntentService.EXTRA_DATA_USER_LOC_LAT, userLoc.latitude)
        intentToPush.putExtra(PushToTwitterDataIntentService.EXTRA_DATA_USER_LOC_LNG, userLoc.longitude)

        PushToTwitterDataIntentService.enqueueWork(ctx, intentToPush)
    }

    fun pushToTwitter(ctx: Context, twitterApi: Twitter, dataToPush: ArrayList<String>, userLoc: LatLng) {

        //if only one station, call updateStatus with intended one and selected one + deduplication
        //if multiple stations, post selected one first and then all other in replies
        //////////////////////////////////////////////////////////////////////////////////////
        //FORMAT -- ROOT STATUS
        /*#findmybixibikes bike is not closest! Bikes:X BAD at IDIDIDIDIDIDIDIDIDIDIDIDIDIDIDID ~XXmin walk stationnamestationnamestation deduplicateZ
          #findmybixibikes bike is not closest! Bikes:XX AOK at IDIDIDIDIDIDIDIDIDIDIDIDIDIDIDID ~XXmin walk stationnamestationnamestatio deduplicateZ

          -- REPLIES
          #findmybixibikes discarded closer! Bikes:Y CRI at IDIDIDIDIDIDIDIDIDIDIDIDIDIDIDID stationnamestationnamestationnamestationnamestationnam
          #findmybixibikes discarded closer! Bikes:Y LCK at IDIDIDIDIDIDIDIDIDIDIDIDIDIDIDID stationnamestationnamestationnamestationnamestationnam

          */

        coroutineScopeIO.launch {

            var selectedNbBikes = -1
            var selectedBadorAok = "BAD"    //hashtagged
            var selectedStationId = ""
            var selectedProximityString = "XXmin"
            var selectedStationName = "Laurier / De Lanaudière"
            var deduplicate = "deduplicate"    //hashtagged

            var selectedStation: BikeStation? = null
            //Pair of station id and availability code (always 'CRI' as of now)
            val discardedStations = java.util.ArrayList<Pair<String, String>>().toMutableList()


            //dataToPush will contain as first element
            //   359f354466083c962d243bc238c95245_AVAILABILITY_AOK
            //OR 359f354466083c962d243bc238c95245_AVAILABILITY_BAD
            //followed by 1 or more string in the form of
            //   3c3bf5e74cb938e7d57641edaf909d24_AVAILABILITY_CRI

            var firstString = true
            val repo = InjectorUtils.provideRepository(ctx)
            val hastagCurBikeSystemName = "#findmy${repo.getHashtaggableCurBikeSystemName()}"

            dataToPush.forEach {
                if (firstString) {
                    //359f354466083c962d243bc238c95245_AVAILABILITY_BAD or
                    //359f354466083c962d243bc238c95245_AVAILABILITY_AOK

                    selectedStationId = it.substring(0, STATION_ID_LENGTH)
                    selectedBadorAok = it.substring(STATION_ID_LENGTH + TableFragmentViewModel.AVAILABILITY_POSTFIX_START_SEQUENCE.length,
                            STATION_ID_LENGTH + TableFragmentViewModel.AVAILABILITY_POSTFIX_START_SEQUENCE.length + 3) //'BAD' or 'AOK'


                    selectedStation = repo.getStationForId(selectedStationId)

                    selectedNbBikes = selectedStation?.freeBikes ?: 0


                    selectedProximityString = Utils.getWalkingProximityString(selectedStation?.location,
                            userLoc, false, NumberFormat.getInstance(), ctx) as String

                    //station name will be truncated to fit everything in a single tweet
                    //see R.string.twitter_not_closest_bike_data_format
                    val maxStationNameIdx = 138 - (deduplicate.length + " ".length
                            + " walk ".length
                            + selectedProximityString.length
                            + " ".length
                            + STATION_ID_LENGTH
                            + " at ".length
                            + selectedBadorAok.length
                            + " #".length
                            + Integer.toString(selectedNbBikes).length
                            + " bike is not closest! Bikes:".length
                            + hastagCurBikeSystemName.length)

                    selectedStationName = selectedStation?.name?.substring(0, Math.min(selectedStation?.name?.length
                            ?: 0, maxStationNameIdx)) ?: "Laurier / De Lanaudière"

                    firstString = false

                } else { //3c3bf5e74cb938e7d57641edaf909d24_AVAILABILITY_CRI
                    val discarded = Pair(it.substring(0, STATION_ID_LENGTH), it.substring(
                            STATION_ID_LENGTH + TableFragmentViewModel.AVAILABILITY_POSTFIX_START_SEQUENCE.length,
                            STATION_ID_LENGTH + TableFragmentViewModel.AVAILABILITY_POSTFIX_START_SEQUENCE.length + 3
                    ))

                    discardedStations.add(discarded)
                }

            }

            var deduplicateCounter = 0

            deduplicate += deduplicateCounter

            var newStatusString = String.format(ctx.getString(R.string.twitter_not_closest_bike_data_format),
                    hastagCurBikeSystemName, selectedNbBikes, selectedBadorAok, selectedStationId, selectedProximityString, selectedStationName, deduplicate)

            var newStatus = StatusUpdate(newStatusString)
            //noinspection ConstantConditions
            newStatus.displayCoordinates(true).location(GeoLocation(selectedStation?.location?.latitude
                    ?: 0.0, selectedStation?.location?.longitude ?: 0.0))

            var deduplicationDone = false

            while (!deduplicationDone) {

                //post status before adding replies
                try {
                    //can be interrupted here (duplicate)
                    val answerStatus = twitterApi.updateStatus(newStatus)

                    val replyToId = answerStatus.getId()

                    for (discarded in discardedStations) {
                        val discardedStationItem = repo.getStationForId(discarded.first ?: "NO_ID")

                        val replyStatusString = String.format(ctx.getString(R.string.twitter_closer_discarded_reply_data_format),
                                hastagCurBikeSystemName, discardedStationItem.freeBikes, discarded.second, discarded.first,
                                discardedStationItem.name!!.substring(0, Math.min(discardedStationItem.name!!.length, REPLY_STATION_NAME_MAX_LENGTH)))

                        val replyStatus = StatusUpdate(replyStatusString)

                        replyStatus.inReplyToStatusId(replyToId)
                                .displayCoordinates(true)
                                .location(GeoLocation(discardedStationItem.location.latitude, discardedStationItem.location.longitude))

                        //that can also raise exception
                        twitterApi.updateStatus(replyStatus)

                    }

                    deduplicationDone = true


                } catch (e: TwitterException) {
                    val errorMessage = e.errorMessage
                    if (errorMessage.contains("Status is a duplicate.")) {
                        ++deduplicateCounter

                        deduplicate = "deduplicate$deduplicateCounter"

                        newStatusString = String.format(ctx.getString(R.string.twitter_not_closest_bike_data_format),
                                hastagCurBikeSystemName, selectedNbBikes, selectedBadorAok, selectedStationId, selectedProximityString, selectedStationName, deduplicate)

                        newStatus = StatusUpdate(newStatusString)

                        newStatus.displayCoordinates(true).location(GeoLocation(selectedStation?.location?.latitude
                                ?: 0.0, selectedStation?.location?.longitude ?: 0.0))

                        Log.e("TwitterUpdate", "TwitterUpdate duplication -- deduplicating now", e)

                    } else {
                        deduplicationDone = true
                    }
                }
            }
        }
    }

    companion object {
        private val TAG = TwitterNetworkDataExhaust::class.java.simpleName
        private const val STATION_ID_LENGTH = 32
        private const val REPLY_STATION_NAME_MAX_LENGTH = 54

        // For Singleton instantiation
        private val LOCK = Any()
        private var sInstance: TwitterNetworkDataExhaust? = null

        /**
         * Get the singleton for this class
         */
        fun getInstance(): TwitterNetworkDataExhaust {
            //Log.d(TAG, "Getting bike system list network data source")
            if (sInstance == null) {
                synchronized(LOCK) {
                    sInstance = TwitterNetworkDataExhaust()
                    Log.d(TAG, "Made new bike system list network data source")
                }
            }
            return sInstance!!
        }
    }
}