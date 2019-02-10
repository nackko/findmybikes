package com.ludoscity.findmybikes.data.network

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.support.v4.app.JobIntentService
import android.util.Log
import com.ludoscity.findmybikes.RootApplication
import com.ludoscity.findmybikes.utils.InjectorUtils

class FetchBikeSystemIntentService : JobIntentService() {
    override fun onHandleWork(intent: Intent) {
        Log.i(TAG, "Executing work: $intent")
        val api = (application as RootApplication).citybik_esApi

        InjectorUtils.provideNetworkDataSource().fetchBikeSystem(api, intent.getStringExtra("system_href"))

        Log.i(TAG, "Completed service @ " + SystemClock.elapsedRealtime())
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "All work completed")
    }

    companion object {
        private const val JOB_ID = 1000
        private val TAG = FetchBikeSystemIntentService::class.java.simpleName

        fun enqueueWork(ctx: Context, work: Intent){
            enqueueWork(ctx, FetchBikeSystemIntentService::class.java, JOB_ID, work)
        }
    }
}