package com.ludoscity.findmybikes.data.network.cozy

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Created by F8Full on 2019-06-20.
 * Retrofit interface to access cozy cloud API
 * see https://github.com/cozy/cozy-stack/blob/master/docs/files.md
 */
interface CozyCloudAPI {

    //https://github.com/cozy/cozy-stack/blob/master/docs/files.md#post-filesdir-id
    //https://f8full.mycozy.cloud/files/?Type=directory&Name=truc&Tags=data
    //@POST("/files/")
    //fun createFile(@QueryMap specifications: Map<String, String>): Call<CozyFileDescAnswerRoot>

    //https://github.com/cozy/cozy-stack/blob/master/docs/files.md#post-filesdir-id
    //https://f8full.mycozy.cloud/files/?Type=directory&Name=truc&Tags=data
    @POST("/files/")
    fun createFile(@Query("Type") type: String, @Query("Name") dirName: String, @Query("Tags") tagList: List<String>): Call<CozyFileDescAnswerRoot>


    @GET("/files/{id}")
    fun getFileInfo(@Path("id") id: String): Call<CozyFileDescAnswerRoot>

    //https://github.com/cozy/cozy-stack/blob/master/docs/files.md#get-filesmetadata
    //https://f8full.mycozy.cloud/files/metadata?Path=%2F%23findmybikes_raw
    @GET("/files/metadata")
    fun getFileMetadata(@Query("Path") fullPathWithSlashes: String): Call<CozyFileDescAnswerRoot>
}
