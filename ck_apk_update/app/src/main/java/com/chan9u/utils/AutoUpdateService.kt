package com.chan9u.utils

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.AsyncTask
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.blankj.utilcode.util.AppUtils
import com.chan9u.R
import com.chan9u.activity.MainActivity
import com.chan9u.model.BasicApi
import com.chan9u.model.DownloadFile
import com.chan9u.model.K
import com.chan9u.model.VersionDto
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedInputStream
import java.io.File
import java.nio.charset.Charset
import java.util.*
import java.util.zip.ZipFile


/**
 *  AutoUpdateService
 *  업데이트 서비스
 *  by chan9u
 *  last 20.11.22
 */
class AutoUpdateService: Service() {
    private var serviceIntent: Intent? = null
    // 10분  600000
    private val sleepMillis: Long = 600000

    private lateinit var retrofitService: BasicApi

    private val zipFileNm = "contents.zip"

    // version
    private var verContents: Int = 0
    private var verApk: Int = 0

    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onCreate() {
        super.onCreate()
        // 포그라운드 생성
        initializeNotification()
        setRetrofit()

        val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        registerReceiver(downloadReceiver(), intentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        serviceIntent = intent
        try {
            Log.d("@@@@@@@", "onStartCommand")

            val task = object :TimerTask(){
                override fun run() {
                    reqContentVersion()
                }
            }

            Timer().schedule(task, 9000, sleepMillis)
        } catch (e: Exception) {
            Log.d("@@@@@@@@", "onStartCommand >> ${e.message}")
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadReceiver())
        Log.d("@@@@@@@", "onDestroy ")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        Log.d("@@@@@@@", "onTaskRemoved ")
    }

    // 포그라운드 서비스
    fun initializeNotification() {
        val notificationStyle: NotificationCompat.BigTextStyle
                = NotificationCompat.BigTextStyle()
        notificationStyle.bigText("자동 업데이트 서비스 동작중")
        notificationStyle.setBigContentTitle(null)
        notificationStyle.setSummaryText("서비스 동작중")

        val notificationBuilder: NotificationCompat.Builder
                = NotificationCompat.Builder(this, "1").apply {
            setSmallIcon(R.mipmap.ic_launcher_round)
            setContentText(null)
            setContentTitle(null)
            setOngoing(true)
            setStyle(notificationStyle)
            setWhen(0)
            setShowWhen(false)
        }

        val manager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            manager.createNotificationChannel(NotificationChannel("1","auto_update_service", NotificationManager.IMPORTANCE_NONE))
        startForeground(1, notificationBuilder.build())
    }

    private fun setRetrofit() {
        var gson: Gson = GsonBuilder()
                .setLenient()
                .create()

        //creating retrofit object
        var retrofit =
                Retrofit.Builder()
                        .baseUrl(K.BaseUrl)
                        .addConverterFactory(GsonConverterFactory.create(gson))
                        .build()

        //creating our api
        retrofitService = retrofit.create(BasicApi::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun unZip(zipFile: File, targetPath: String) {
        val zip = ZipFile(zipFile, Charset.forName("euc-kr"))
        val enumeration = zip.entries()
        while (enumeration.hasMoreElements()) {
            val entry = enumeration.nextElement()
            val destFilePath = File(targetPath, entry.name)
            destFilePath.parentFile.mkdirs()
            if (entry.isDirectory)
                continue
            val bufferedIs = BufferedInputStream(zip.getInputStream(entry))
            bufferedIs.use {
                destFilePath.outputStream().buffered(1024).use { bos ->
                    bufferedIs.copyTo(bos)
                }
            }
        }
        // remove zip file
        File(
                "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path}${File.separator}${zipFileNm}"
        ).delete()
        reqApkVersion()
    }

    // downloadReceiver
    private fun downloadReceiver(): BroadcastReceiver = object: BroadcastReceiver(){
        @RequiresApi(Build.VERSION_CODES.N)
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("@@@@@@@@@", "downloadReceiver() >> ")
            try {
                val zipFile = File(
                        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path}${File.separator}${zipFileNm}"
                )
                unZip(
                        zipFile, "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path}"
                )
            } catch (e: Exception) {
                Log.d("@@@@@@@@@", e.message)
            }
        }
    }

    // adb install
    private fun adbInstall(apkName: String) {
        // 슈퍼 유저 받기 앱 자체에서 불가
        // Caused by: java.io.IOException: Cannot run program "su": error=13, Permission denied
        //
//        Runtime.getRuntime().exec("su")

        try {
            // 루틴된 폰에서 테스트 가능한지 네이버 앱 스킴 호출 (네이버 앱 존재시)
//            Runtime.getRuntime().exec("adb shell am start -a android.intent.action.VIEW -d naversearchapp://host")

            // adb install 을 이용하여 루틴된 폰에서 다운  'adb install apk파일경로'
            Runtime.getRuntime().exec("adb install ${apkName}")

        } catch (e: Exception) {
            Log.d("@@@@@@@@@@ adbInstall", e.message)
        }

    }

    private fun installPackage(apkName: String){
        try {
            val apkFile = File(
                    "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path}${File.separator}${apkName}"
            )
            AppUtils.installApp(apkFile)

            // Blankj 라이브러리를 사용 안할 시 위내용 주석 후 아래 로직 사용 (같은 동작)
//           val apkFile = File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path}${File.separator}version_2.apk")
//           val list = apkFile.listFiles()
//
//           val apkUri: Uri = Uri.fromFile(apkFile)
//
//           val intent = Intent()
//           intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//           intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//           intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//           intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
//           startActivity(intent)

        } catch (e: Exception) {
            Log.d("@@@@@@@@@", e.message)
        }
    }

    fun install(context: Context, packageName: String, apkPath: String) {

        // PackageManager provides an instance of PackageInstaller
        val packageInstaller = context.packageManager.packageInstaller

        // Prepare params for installing one APK file with MODE_FULL_INSTALL
        // We could use MODE_INHERIT_EXISTING to install multiple split APKs
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(packageName)

        // Get a PackageInstaller.Session for performing the actual update
        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        // Copy APK file bytes into OutputStream provided by install Session
        val out = session.openWrite(packageName, 0, -1)
        val fis = File(apkPath).inputStream()
        fis.copyTo(out)
        session.fsync(out)
        out.close()
    }

    private fun reqContentVersion() {
        Log.d("@@@@@@@@ ", "reqContentVersion")
        setRetrofit()
        retrofitService.reqContents().enqueue(object: Callback<VersionDto> {
            override fun onFailure(call: Call<VersionDto>, t: Throwable) {
                Log.d("@@@@@@@@ ", "reqContentVersion onFailure")
            }

            override fun onResponse(call: Call<VersionDto>, response: Response<VersionDto>) {
                Log.d("@@@@@@@@ ", "reqContentVersion onResponse >> ${response.body()?.result}")
                try {
                    if (response.isSuccessful) {
                        verContents = hawk(K.hawk.contents_version, 0)
                        val serverVersion: Int = response.body()?.version?.let { Integer.parseInt(it) } ?: 0
                        if (verContents < serverVersion){
                            serverVersion.save(K.hawk.contents_version)
                            download {
                                context = this@AutoUpdateService
                                downloads = listOf(
                                        DownloadFile(
                                                zipFileNm,
                                        "http://svntest.lineheart.kr/download_contests/contents_${serverVersion}.zip"
//                                                "https://www.dropbox.com/s/5qlsq1h8k361k8k/test.zip?dl=1"
                                        )
                                )
                            }
                        }
                    }
                }catch (e: Exception){
                    Log.d("@@@@@@@@@@@", "reqContentVersion >> ${e.message}")
                }
            }
        })
    }

    private fun reqApkVersion() {
        Log.d("@@@@@@@@ ", "reqApkVersion")
        setRetrofit()
        retrofitService.reqApk().enqueue(object: Callback<VersionDto> {
            override fun onFailure(call: Call<VersionDto>, t: Throwable) {
                Log.d("@@@@@@@@ ", "reqApkVersion onFailure")
            }

            override fun onResponse(call: Call<VersionDto>, response: Response<VersionDto>) {
                Log.d("@@@@@@@@ ", "reqApkVersion onResponse >> ${response.body()?.result}")
                try {
                    if (response.isSuccessful) {
                        verApk = hawk(K.hawk.apk_version, 0)
                        val serverVersion: Int = response.body()?.version?.let { Integer.parseInt(it) } ?: 0
                        if (verApk < serverVersion) {
                            serverVersion.save(K.hawk.apk_version)
//                            installPackage("test.apk")
                            installPackage("app_${serverVersion}.apk")
                        }
                    }
                } catch (e: Exception) {
                    Log.d("@@@@@@@@@@@", "retrofitService >> ${e.message}")
                }
            }
        })
    }

}