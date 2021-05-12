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
    fun reqUploadInfo(
        @Query("poscode") poscode: String
    ): Call<SendUploadInfo>

    // 컨텐츠 업데이트 결과
    @GET("/UploadApi/getServiceUploadInfo")
    fun sendUploadInfo(
        @Query("updatedatetime") updatedatetime: String ,
        @Query("updatever") updatever: String ,
        @Query("poscode") poscode: String ,
        @Query("posname") posname: String ,
        @Query("updateresult") updateresult: Int ,      // 성공 10, 실패 11, 통신오류 13, 버전중복 14
        @Query("periodic") periodic: String
    ): Call<JsonObject>
}