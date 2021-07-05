package com.uplusupdate.activity

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.uplusupdate.R
import com.uplusupdate.base.BaseActivity
import com.uplusupdate.databinding.ActivityMainBinding
import com.uplusupdate.model.K
import com.uplusupdate.utils.*
import java.io.*
import java.lang.Exception

/**
 *  MainActivity
 *  by chan9u
 *  last 20.11.20
 */
class MainActivity : BaseActivity<ActivityMainBinding>() {
    // permission
    private val REQ_CODE_PERMISSION = 777

    override val layoutId: Int = R.layout.activity_main

    private var ver: Int = 0
    lateinit var fileChild: Array<File>

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

        ver = hawk(K.hawk.contents_version, 0)
        binding.tvVer.text = "version -> ${ver}"

        val code: String = hawk(K.hawk.poscode, "")
        binding.tvCode.text = "poscode -> $code"

        val name: String = hawk(K.hawk.posname, "")
        binding.tvName.text = "posname -> $name"

        // TODO chan 21.05.11
//        val isSuccess = intent.getStringExtra("success") ?: ""
//        if (isSuccess.equals("success", false)) {
//            val url = "lguplus://smarthome?path=test"
//            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//            startActivity(intent)
//        }

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
            K.isLoop = true
            val code: String = hawk(K.hawk.poscode, "")
            val name: String = hawk(K.hawk.posname, "")

            if (code.isNotEmpty() && name.isNotEmpty()) {
                startUpdate()
            } else {
                Toast.makeText(this, "poscode, posname 값을 확인해 주세요!!", Toast.LENGTH_LONG).show()
            }

        }

        binding.btnSave.setOnClickListener {

            if (!binding.etCode.text.isNullOrEmpty()) {
                var code: String = binding.etCode.text.toString()
                code.save(K.hawk.poscode)
                binding.tvCode.text = "poscode -> $code"
                binding.etCode.setText("")
            }

            if (!binding.etName.text.isNullOrEmpty()) {
                var name: String = binding.etName.text.toString()
                name.save(K.hawk.posname)
                binding.tvName.text = "posname -> $name"
                binding.etName.setText("")
            }
        }

        binding.btnStop.setOnClickListener {
            try {
                K.isLoop = false
                stopService(Intent(this, AutoUpdateService::class.java))
            } catch (e: Exception) {
                Log.d("@@@@@@@@@", "btnStop >> ${e.message}")
            }

        }

        binding.ivVer.setOnClickListener {
            ver = hawk(K.hawk.contents_version, 0)
            binding.tvVer.text = "version -> ${ver}"
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