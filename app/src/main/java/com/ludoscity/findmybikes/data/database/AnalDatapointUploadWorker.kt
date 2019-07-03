package com.ludoscity.findmybikes.data.database

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.ludoscity.findmybikes.utils.InjectorUtils
import com.ludoscity.findmybikes.utils.Utils

/**
 * Created by F8Full on 2019-06-28. This file is part of #findmybikes
 *
 */
class AnalDatapointUploadWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        // Do the work here--in this case, upload the data
        val repo = InjectorUtils.provideRepository(applicationContext)

        val gson = Gson()
        val api = Utils.getCozyCloudAPI(applicationContext)

        Log.i(AnalDatapointUploadWorker::class.java.simpleName, "About to upload analytic table")
        repo.getAnalDatapointListReadyForUpload().forEach {
            repo.uploadDatapoint(gson, api, it, ArrayList(listOf("findmybikes", "analytics")))
        }
        // Indicate whether the task finished successfully with the Result
        return Result.success()
    }
}