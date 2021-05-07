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
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.blankj.utilcode.util.AppUtils
import com.chan9u.R
import com.chan9u.activity.MainActivity
import com.chan9u.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedInputStream
import java.io.File
import java.nio.charset.Charset
import java.text.SimpleDateFormat
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
    private var downloadUrl = ""

    // version
    private var verContents: Int = 0
    private var verApk: Int = 0

    private var currentVer: Int = 0

    private var timerTask: TimerTask? = null
    lateinit var fileChild: Array<File>

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
            timerTask?.cancel()
            timerTask = object :TimerTask(){
                override fun run() {
//                    reqContentVersion()
                    reqUploadVer()
//                    Log.d("@@@@@@@", "onStartCommand run@@@@@@@@@@@@@@@@")
                }
            }
            Timer().schedule(timerTask!!, 3000, sleepMillis)

            // 21.04.27 chan test
//            download {
//                context = this@AutoUpdateService
//                downloads = listOf(
//                    DownloadFile(
//                        zipFileNm,
//                        "https://www.dropbox.com/s/futi6fbg02eggbr/contents.zip?dl=1"
//                    )
//                )
//            }
        } catch (e: Exception) {
            Log.d("@@@@@@@@", "onStartCommand >> ${e.message}")
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            if (K.isLoop) {
                unregisterReceiver(downloadReceiver())
            } else {
                timerTask?.cancel()
            }
        } catch (e: Exception) {

        }

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
        try {
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
            zipFile.delete()

            // TODO chan 21.04.27 apk 업데이트는 이후에 작업
//        reqApkVersion()

        } catch (e: Exception) {
            Log.d("@@@@@@ ", "unZip >> ${e.message}")
        }

    }

    // 디렉토리 모든 파일 삭제
    fun setDirEmpty(dirNm: String) {
        val file = File(dirNm)
        if (file.exists()) {
            fileChild = file.listFiles()
            Log.d("@@@@@@@@@", "btnDelete >> ")
            for (child: File in fileChild) {
                if (child.isDirectory) {
                    setDirEmpty(child.absolutePath)
                } else {
                    child.delete()
                }
            }
            file.delete()
        }
    }

    // downloadReceiver
    private fun downloadReceiver(): BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.N)
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("@@@@@@@@@", "downloadReceiver() >> ")
            // 다운로드 완료
            val dirBefore = File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path}${File.separator}before")
            if (!dirBefore.exists()) {
                dirBefore.mkdir()
            }
            val beforeContents = File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path}${File.separator}contents")
            if (beforeContents.exists()) {
                beforeContents.renameTo(
                        File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path}${File.separator}before${File.separator}contents")
                )
            }

            val zipFile = File(
                    "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path}${File.separator}${zipFileNm}"
            )
            unZip(
                    zipFile,
                    "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path}"
            )

            if (zipFile.exists()) {
                // 예외적인 상황으로 압축해제 실패
                val contents = File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path}${File.separator}contents")
                if (contents.exists()) {
                    setDirEmpty("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path}${File.separator}contents")
                }
                val returnFile = File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path}${File.separator}before${File.separator}contents")
                if (returnFile.exists()) {
                    returnFile.renameTo(
                            File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path}${File.separator}contents")
                    )
                }

                // remove zip file
                zipFile.delete()
                sendUploadVer(11, hawk(K.hawk.periodic, "").toString())
            } else {
                // 압축해제 성공
                if (dirBefore.exists()) {
                    setDirEmpty("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path}${File.separator}before")
                }

                sendUploadVer(10, hawk(K.hawk.periodic, "").toString())
                currentVer.save(K.hawk.contents_version)
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
            Toast.makeText(this, "adb install Success >> ", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.d("@@@@@@@@@@ adbInstall", e.message)
            Toast.makeText(this, "adb install Fail >> ${e.message}", Toast.LENGTH_SHORT).show()
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

    /**
     * 스마트홈 업데이트 정보 가져오기
     */
    private fun reqUploadVer() {
        setRetrofit()
        retrofitService.reqUploadInfo(hawk(K.hawk.poscode, "")).enqueue(object : Callback<SendUploadInfo> {

            override fun onResponse(call: Call<SendUploadInfo>, response: Response<SendUploadInfo>) {
                Log.d("@@@@@@@@ ", "reqUploadVer onResponse >> ${response.body()}")
                try {
                    if (response.isSuccessful) {
                        downloadUrl = response.body()?.url ?: ""
                        verContents = hawk(K.hawk.contents_version, 0)

                        val serverVersion: Int =
                            response.body()?.updatever?.let { Integer.parseInt(it) } ?: 0

                        if (verContents < serverVersion) {
//                        if (true) {
                            response.body()?.updatever?.let {
                                currentVer = Integer.parseInt(it)
                            }
                            response.body()?.periodic?.let {
                                it.save(K.hawk.periodic)
                            }

                            download {
                                context = this@AutoUpdateService
                                downloads = listOf(
                                    DownloadFile(
                                        zipFileNm,
                                        downloadUrl
                                    )
                                )
                            }
                        }

                    } else {
                        Log.d("@@@@@@@@ ", "reqUploadVer else")
                        sendUploadVer(11, hawk(K.hawk.periodic, "").toString())
                    }
                } catch (e: Exception) {
                    Log.d("@@@@@@@@@@@", "reqUploadVer >> ${e.message}")
                }
            }

            override fun onFailure(call: Call<SendUploadInfo>, t: Throwable) {
                Log.d("@@@@@@@@ ", "reqUploadVer onFailure")
                sendUploadVer(11, hawk(K.hawk.periodic, "").toString())
            }

        })
    }

    /**
     * 스마트홈 업데이트 결과 전송
     *  result 성공 10, 실패 11
     */
    private fun sendUploadVer(result: Int, periodic: String) {
        Log.d("@@@@@@@@ ", "sendUploadVer >> ")
        setRetrofit()
        retrofitService.sendUploadInfo(
            getTime(),
            hawk(K.hawk.contents_version, 0).toString(),
            hawk(K.hawk.poscode, "").toString(),
            hawk(K.hawk.posname, "").toString(),
            result,
            periodic
        ).enqueue(object : Callback<JsonObject> {
            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Log.d("@@@@@@@@ ", "sendUploadVer onFailure")
            }

            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                Log.d("@@@@@@@@ ", "sendUploadVer onResponse >> ${response.isSuccessful()}")
                Log.d("@@@@@@@@ ", "sendUploadVer onResponse >> ${response.body()}")
            }

        })
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
                            installPackage("test.apk")
                            installPackage("app_${serverVersion}.apk")
//                            adbInstall("app_${serverVersion}.apk")
                        }
                    }
                } catch (e: Exception) {
                    Log.d("@@@@@@@@@@@", "retrofitService >> ${e.message}")
                }
            }
        })
    }

    private fun getTime(): String {
        var stringTime = ""
        try {
            val now: Long = System.currentTimeMillis()
            val date = Date(now)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("ko", "KR"))
            stringTime = dateFormat.format(date)
        } catch (e: java.lang.Exception) {
            Log.d("OkHttpWebSocket ", "@@@@ time error ${e.message}")
        }
        return stringTime
    }

}