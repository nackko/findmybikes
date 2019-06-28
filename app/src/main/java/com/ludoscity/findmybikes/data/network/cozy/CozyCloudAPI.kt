package com.ludoscity.findmybikes.data.network.cozy

import com.ludoscity.findmybikes.data.database.tracking.AnalTrackingDatapoint
import com.ludoscity.findmybikes.data.database.tracking.GeoTrackingDatapoint
import retrofit2.Call
import retrofit2.http.*

/**
 * Created by F8Full on 2019-06-20.
 * Retrofit interface to access cozy cloud API
 * see https://github.com/cozy/cozy-stack/blob/master/docs/files.md
 */
interface CozyCloudAPI {

    //https://github.com/cozy/cozy-stack/blob/master/docs/files.md#post-filesdir-id
    //https://f8full.mycozy.cloud/files/?Type=directory&Name=truc&Tags=data
    @POST("/files/{dir-id}")
    fun postFile(
            @Path("dir-id") parentDirectoryId: String = "",
            @QueryMap specifications: Map<String, String>): Call<CozyFileDescAnswerRoot>

    @POST("/files/{dir-id}")
    fun postFile(
            @Body content: AnalTrackingDatapoint,
            @Path("dir-id") parentDirectoryId: String = "",
            @QueryMap specifications: Map<String, String>,
            @Header("Content-MD5") contentMD5: String,
            @Header("Content-Type") contentType: String = "application/json"//,
    ): Call<CozyFileDescAnswerRoot>

    @POST("/files/{dir-id}")
    fun postFile(
            @Body content: GeoTrackingDatapoint,
            @Path("dir-id") parentDirectoryId: String = "",
            @QueryMap specifications: Map<String, String>,
            @Header("Content-MD5") contentMD5: String,
            @Header("Content-Type") contentType: String = "application/json"//,
    ): Call<CozyFileDescAnswerRoot>

    @GET("/files/{id}")
    fun getFileInfo(@Path("id") id: String): Call<CozyFileDescAnswerRoot>

    //https://github.com/cozy/cozy-stack/blob/master/docs/files.md#get-filesmetadata
    //https://f8full.mycozy.cloud/files/metadata?Path=%2F%23findmybikes_raw
    @GET("/files/metadata")
    fun getFileMetadata(@Query("Path") fullPathWithSlashes: String): Call<CozyFileDescAnswerRoot>
}
