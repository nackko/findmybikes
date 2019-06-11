package com.ludoscity.findmybikes.data.network.citybik_es

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.core.app.JobIntentService
import com.ludoscity.findmybikes.utils.InjectorUtils
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FetchCitybikDOTesDataIntentService : JobIntentService() {
    private val api: Citybik_esAPI
    override fun onHandleWork(intent: Intent) {
        Log.i(TAG, "Executing work: $intent")

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

    init {
        Log.d(TAG, "Building a citybik.es API instance")
        val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        api = retrofit.create(Citybik_esAPI::class.java)
    }

    companion object {

        const val ACTION_FETCH_SYSTEM_STATUS = "systemStatus"
        const val ACTION_FETCH_SYSTEM_LIST = "systemList"
        internal const val BASE_URL = "https://api.citybik.es"


        private const val JOB_ID = 1000
        private val TAG = FetchCitybikDOTesDataIntentService::class.java.simpleName

        fun enqueueWork(ctx: Context, work: Intent){
            enqueueWork(ctx, FetchCitybikDOTesDataIntentService::class.java, JOB_ID, work)
        }
    }
}