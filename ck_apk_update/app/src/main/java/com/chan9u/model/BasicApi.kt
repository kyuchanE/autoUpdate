package com.chan9u.model

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

@JvmSuppressWildcards
interface BasicApi {
    // 컨텐츠 버전 체크
    @GET("/get_contents_ver.php")
    fun reqContents(): Call<VersionDto>
    // apk 파일 버전 체크
    @GET("/get_apk_ver.php")
    fun reqApk(): Call<VersionDto>

    // 컨텐츠 버전 체크
    @GET("/UploadApi/sendServiceUploadInfo")
    fun reqUploadInfo(): Call<JsonObject>

    // 컨텐츠 버전 체크
    @GET("/UploadApi/getServiceUploadInfo")
    fun sendUploadInfo(): Call<JsonObject>
}