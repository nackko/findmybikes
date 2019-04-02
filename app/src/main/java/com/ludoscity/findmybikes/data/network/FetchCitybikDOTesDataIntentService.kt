package com.ludoscity.findmybikes.data.network

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.support.v4.app.JobIntentService
import android.util.Log
import com.ludoscity.findmybikes.RootApplication
import com.ludoscity.findmybikes.utils.InjectorUtils

class FetchCitybikDOTesDataIntentService : JobIntentService() {
    override fun onHandleWork(intent: Intent) {
        Log.i(TAG, "Executing work: $intent")
        val api = (application as RootApplication).citybik_esApi

        val action = intent.action

        when (action) {
            ACTION_FETCH_SYSTEM_STATUS -> InjectorUtils.provideBikeSystemStatusNetworkDataSource(applicationContext).fetchBikeSystemStatus(api, intent.getStringExtra("system_href"))
            ACTION_FETCH_SYSTEM_LIST -> InjectorUtils.provideBikeSystemListNetworkDataSource(applicationContext).fetchBikeSystemList(api)
        }

        Log.i(TAG, "Completed service @ " + SystemClock.elapsedRealtime())
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "All work completed")
    }

    companion object {

        const val ACTION_FETCH_SYSTEM_STATUS = "systemStatus"
        const val ACTION_FETCH_SYSTEM_LIST = "systemList"


        private const val JOB_ID = 1000
        private val TAG = FetchCitybikDOTesDataIntentService::class.java.simpleName

        fun enqueueWork(ctx: Context, work: Intent){
            enqueueWork(ctx, FetchCitybikDOTesDataIntentService::class.java, JOB_ID, work)
        }
    }
}