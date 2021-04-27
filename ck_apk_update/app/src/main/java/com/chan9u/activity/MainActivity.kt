package com.chan9u.activity

import android.Manifest
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.AppUtils
import com.chan9u.R
import com.chan9u.base.BaseActivity
import com.chan9u.databinding.ActivityMainBinding
import com.chan9u.model.BasicApi
import com.chan9u.model.DownloadFile
import com.chan9u.model.K
import com.chan9u.model.VersionDto
import com.chan9u.utils.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.orhanobut.hawk.Hawk
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.*
import java.nio.charset.Charset
import java.util.zip.ZipFile

/**
 *  MainActivity
 *  by chan9u
 *  last 20.11.20
 */
class MainActivity : BaseActivity<ActivityMainBinding>() {
    // permission
    private val REQ_CODE_PERMISSION = 777

    override val layoutId: Int = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissions(
                permissions = listOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.REQUEST_INSTALL_PACKAGES,
                        Manifest.permission.FOREGROUND_SERVICE
                ),
                notGranted = {
//                    dialog("필수 권한이 없어 앱을 종료합니다.").right { finish() }
                }
        )

        initViews()
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onDestroy() {
        super.onDestroy()
    }

    // initView
    override fun initViews() {
        super.initViews()

        if (Build.VERSION.SDK_INT >= 26) {
            val packageManager: PackageManager = packageManager
            if (!packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.setData(Uri.parse("package:${packageName}"))
                startActivity(intent)
            }
        }

        binding.btnStart.setOnClickListener {
            startUpdate()
        }

        binding.btnReset.setOnClickListener {
            10000.save(K.hawk.contents_version)
            20002.save(K.hawk.apk_version)
        }

    }

    @JvmOverloads
    fun permissions(permissions: List<String>, granted: () -> Unit = {}, notGranted: () -> Unit = {}) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // 다음단계 진행
        } else {
            val notGrants = permissions
                    .filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
                    .toTypedArray()

            if (notGrants.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, notGrants, REQ_CODE_PERMISSION)
                return
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQ_CODE_PERMISSION) {
            if (grantResults.filter { it == PackageManager.PERMISSION_GRANTED }.count() == grantResults.size) {
                // 퍼미션 허용
            } else {
                // 퍼미션 거절
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun startUpdate() {
        Log.d("@@@@@@@@@", "startUpdate")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("@@@@@@@@@", "startUpdate11")
            startForegroundService(Intent(this, AutoUpdateService::class.java))
        } else {
            Log.d("@@@@@@@@@", "startUpdate22")
            startService(Intent(this, AutoUpdateService::class.java))
        }
    }

}