package com.ludoscity.findmybikes.data.network.cozy

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.core.app.JobIntentService
import com.ludoscity.findmybikes.data.Result
import com.ludoscity.findmybikes.utils.InjectorUtils
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CozyDataPipeIntentService : JobIntentService() {
    private lateinit var api: CozyCloudAPI
    override fun onHandleWork(intent: Intent) {
        Log.i(TAG, "Executing work: $intent")

        when (intent.action) {
            ACTION_CREATE_DIRECTORY ->
                InjectorUtils.provideCozyNetworkDataPipe(applicationContext)
                        .postDirectory(
                                api,
                                intent.getStringExtra(EXTRA_CREATE_DIRECTORY_NAME)!!,
                                intent.getStringArrayListExtra(EXTRA_CREATE_DIRECTORY_TAG_LIST)!!)
        }

        Log.i(TAG, "Completed service @ " + SystemClock.elapsedRealtime())
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "All work completed")
    }

    override fun onCreate() {
        super.onCreate()

        //TODO: code duplicated in Utils::getCozyCloudAPI
        //see: https://www.coderdump.net/2018/04/automatic-refresh-api-token-with-retrofit-and-okhttp-authenticator.html
        val httpClientBuilder = OkHttpClient.Builder()


        //interceptor to add authorization header with token to every request
        httpClientBuilder.addInterceptor {

            var response = it.proceed(it.request().newBuilder().addHeader("Authorization",
                    "Bearer ${InjectorUtils.provideRepository(applicationContext).userCred?.accessToken}").build()
            )

            //When access token is expired, cozy replies with code 400 -- Bad request
            if (response.code() == 400) {
                if (response.body()?.string()?.equals("Expired token") == true) {

                    Log.i(TAG, "Captured 400 error Expired token - initiating token refresh")
                    val refreshResult = InjectorUtils.provideRepository(applicationContext).refreshCozyAccessToken()

                    //We're clear to retry the original request from it.request
                    if (refreshResult is Result.Success)
                        response = it.proceed(it.request().newBuilder().addHeader("Authorization", "Bearer ${refreshResult.data.accessToken}").build())
                }
            }
            response

        }

        //authenticator to grab 401 errors, refresh access token and retry the original request
        httpClientBuilder.authenticator { _, response ->

            val refreshResult = InjectorUtils.provideRepository(applicationContext).refreshCozyAccessToken()

            if (refreshResult is Result.Success)
                response.request().newBuilder().addHeader("Authorization", "Bearer ${refreshResult.data.accessToken}").build()
            else
                null
        }

        Log.d(TAG, "Building a Cozy API instance")
        val retrofit = Retrofit.Builder()
                //TODO: should auth also happen through this intent service ?
                .baseUrl(InjectorUtils.provideRepository(applicationContext).cozyOAuthClient?.stackBaseUrl!!)
                .client(httpClientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        api = retrofit.create(CozyCloudAPI::class.java)

    }

    init {

    }

    companion object {

        const val ACTION_CREATE_DIRECTORY = "mkdir"
        const val EXTRA_CREATE_DIRECTORY_NAME = "mkdir_name"
        const val EXTRA_CREATE_DIRECTORY_TAG_LIST = "mkdir_tag_list"
        const val ACTION_UPLOAD_FILE = "uploadFile"
        internal const val BASE_URL = "https://api.citybik.es"


        private const val JOB_ID = 3000
        private val TAG = CozyDataPipeIntentService::class.java.simpleName

        fun enqueueWork(ctx: Context, work: Intent) {
            enqueueWork(ctx, CozyDataPipeIntentService::class.java, JOB_ID, work)
        }
    }
}