package com.uplusupdate.utils

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import com.uplusupdate.model.DownloadFile

/**
 * 다운로드 요청 파라미터 세터
 */
class DownloadSetter {
    lateinit var context: Context                   // 컨텍스트
    var downloads: List<DownloadFile> = listOf()    // 다운로드 파일 리스트
    var title: String? = null                       // 다운로드 텍스트
}

/**
 * 파일 다운로드
 *
 * @param setter 요청 세터
 */
fun download(setter: DownloadSetter.() -> Unit) = DownloadSetter().run {
    setter()

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val downloadIdList = mutableListOf<Long>()
    downloads.forEach {
        val request = DownloadManager.Request(it.url.toUri())
                .setTitle(title ?: "${it.name}을(를) 다운로드 합니다.")
//                    .setDescription("Downloading...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, it.name)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
//                    .addRequestHeader("", "")

        downloadIdList.add(downloadManager.enqueue(request))
    }

    downloadIdList
}
