package com.ludoscity.findmybikes.data.network.twitter

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.support.v4.app.JobIntentService
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.utils.InjectorUtils
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder

class PushToTwitterDataIntentService : JobIntentService() {

    override fun onHandleWork(intent: Intent) {
        Log.i(TAG, "Executing work: $intent")

        val action = intent.action

        @Suppress("UNCHECKED_CAST")
        when (action) {
            ACTION_PUSH_DATA -> InjectorUtils.provideTwitterNetworkDataExhaust(applicationContext).pushToTwitter(applicationContext,
                    api!!,
                    intent.getSerializableExtra(EXTRA_DATA_TO_PUSH) as ArrayList<String>,
                    LatLng(intent.getDoubleExtra(EXTRA_DATA_USER_LOC_LAT, 0.0),
                            intent.getDoubleExtra(EXTRA_DATA_USER_LOC_LNG, 0.0)))
        }

        Log.i(TAG, "Completed service @ " + SystemClock.elapsedRealtime())
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "All work completed")
    }

    companion object {

        const val ACTION_PUSH_DATA = "pushData"
        const val EXTRA_DATA_TO_PUSH = "pushData"
        const val EXTRA_DATA_USER_LOC_LAT = "userLocLat"
        const val EXTRA_DATA_USER_LOC_LNG = "userLocLng"

        private var api: Twitter? = null

        private const val JOB_ID = 2000
        private val TAG = PushToTwitterDataIntentService::class.java.simpleName

        fun enqueueWork(ctx: Context, work: Intent) {
            if (api == null) {
                Log.d(TAG, "Building a Twitter API instance")
                val cb = ConfigurationBuilder()
                cb.setDebugEnabled(true)    //TODO: remove this in release builds
                        .setOAuthConsumerKey(ctx.resources.getString(R.string.twitter_consumer_key))
                        .setOAuthConsumerSecret(ctx.resources.getString(R.string.twitter_consumer_secret))
                        .setOAuthAccessToken(ctx.resources.getString(R.string.twitter_access_token))
                        .setOAuthAccessTokenSecret(ctx.resources.getString(R.string.twitter_access_token_secret))
                val tf = TwitterFactory(cb.build())
                api = tf.instance
            }
            enqueueWork(ctx, PushToTwitterDataIntentService::class.java, JOB_ID, work)
        }
    }
}