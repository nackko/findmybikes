package com.ludoscity.findmybikes.data.database

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.ludoscity.findmybikes.utils.InjectorUtils
import com.ludoscity.findmybikes.utils.Utils

/**
 * Created by F8Full on 2019-06-30. This file is part of #findmybikes
 *
 */
class GeoDatapointUploadWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {
    //private var api: CozyCloudAPI = Utils.getCozyCloudAPI(appContext)

    override fun doWork(): Result {
        // Do the work here--in this case, upload the data
        val repo = InjectorUtils.provideRepository(applicationContext)

        val gson = Gson()
        val api = Utils.getCozyCloudAPI(applicationContext)

        Log.i(GeoDatapointUploadWorker::class.java.simpleName, "About to upload geolocation table")
        repo.getGeoDatapointListReadyForUpload().forEach {
            repo.uploadDatapoint(gson, api, it, ArrayList(listOf("findmybikes", "geolocation")))
        }

        // Indicate whether the task finished successfully with the Result
        return Result.success()
    }
}